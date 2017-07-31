#include "dynet/nodes.h"
#include "dynet/dynet.h"
#include "dynet/training.h"
#include "dynet/timing.h"
#include "dynet/rnn.h"
#include "dynet/gru.h"
#include "dynet/lstm.h"
#include "dynet/dict.h"
#include "dynet/expr.h"

#include <iostream>
#include <fstream>

#include <boost/archive/text_iarchive.hpp>
#include <boost/archive/text_oarchive.hpp>
#include <boost/math/special_functions/fpclassify.hpp>
#include <boost/program_options.hpp>

#include <unordered_map>
#include <unordered_set>

using namespace std;
using namespace dynet;
namespace po = boost::program_options;

//float pdrop = 0.02;
float pdrop = 0.5;
float unk_prob = 0.1;
bool DEBUG = 0;

unsigned WORD_DIM = 100;
unsigned PRE_WORD_DIM = 100;
unsigned HIDDEN_DIM = 150;
unsigned TYPE_HIDDEN_DIM = 150;
unsigned LAYERS = 1;
unsigned VOCAB_SIZE = 0;
unsigned QUES_SIZE = 0;
unsigned TYPE_SIZE = 0;

float THRESHOLD = 0.5;
unsigned ATTENTION_HIDDEN_DIM = 100;
dynet::Dict wd;
dynet::Dict qd;
dynet::Dict td;

int kUNK; //tzy
unordered_map<unsigned, vector<float> > pretrained;
vector<float> unk_embedding;

void InitCommandLine(int argc, char** argv, po::variables_map* conf) {
  po::options_description opts("Configuration options");
  opts.add_options()
        ("training_data,T", po::value<string>(), "List of Transitions - Training corpus")
        ("dev_data,d", po::value<string>(), "Development corpus")
        ("test_data", po::value<string>(), "Test corpus")
        ("pdrop", po::value<float>()->default_value(0.5), "dropout probabilty")
	("unk_prob,u", po::value<float>()->default_value(0.1), "Probably with which to replace singletons with UNK in training data")
        ("model,m", po::value<string>(), "Load saved model from this file")
        ("word_dim", po::value<unsigned>()->default_value(100), "word embedding size")
	("pre_word_dim", po::value<unsigned>()->default_value(100), "pretrained word embedding size")
        ("hidden_dim", po::value<unsigned>()->default_value(150), "hidden dimension")
        ("tag_hidden_dim", po::value<unsigned>()->default_value(64), "tag hidden dimension")
	("layers", po::value<unsigned>()->default_value(1), "layers")
	("test,t", "Should training be run?")
        ("pretrained,w", po::value<string>(), "Pretrained word embeddings")
        ("lexicon", po::value<string>(), "Sentiment Lexicon")
	("train_methods", po::value<unsigned>()->default_value(0), "0 for simple, 1 for mon, 2 for adagrad, 3 for adam")
	("report_i", po::value<unsigned>()->default_value(100), "report i")
        ("dev_report_i", po::value<unsigned>()->default_value(10), "dev report i")
	("count_limit", po::value<unsigned>()->default_value(50), "count limit")
	("threshold", po::value<float>()->default_value(0.5),"[-threshold, threshold] is neutron")
	("debug", "Debug to output trace")
        ("help,h", "Help");
  po::options_description dcmdline_options;
  dcmdline_options.add(opts);
  po::store(parse_command_line(argc, argv, dcmdline_options), *conf);
  if (conf->count("help")) {
    cerr << dcmdline_options << endl;
    exit(1);
  }
  if (conf->count("training_data") == 0 || conf->count("dev_data") == 0 || conf->count("test_data") == 0) {
    cerr << dcmdline_options << endl;
    exit(1);
  }
}

void normalize_digital_lower(string& line){
  for(unsigned i = 0; i < line.size(); i ++){
    if(line[i] >= 'A' && line[i] <= 'Z'){
      line[i] = line[i] - 'A' + 'a';
    }
  }
}

class Instance{
public:
	vector<unsigned> raws;
	vector<unsigned> lows;
	vector<unsigned> words;

	vector<unsigned> questions;
	vector<int> labels;

	unsigned id;
	unsigned event_type;
	
	Instance(){clear();};
        ~Instance(){};
	void clear(){
		raws.clear();
		lows.clear();
		words.clear();
		questions.clear();
		labels.clear();
	}	
	friend ostream& operator << (ostream& out, Instance& instance){
		out<<instance.id<<" ||| ";
		for(unsigned i = 0; i < instance.raws.size(); i ++){
			out << wd.convert(instance.raws[i]) << "/"
			    << wd.convert(instance.lows[i]) << " ";
		}
		out<<"||| ";
		for(unsigned i = 0; i < instance.labels.size(); i ++){
			out << instance.labels[i]<<" ";
		}
		out<<"||| ";
		for(unsigned i = 0; i < instance.questions.size(); i++){
			out << qd.convert(instance.questions[i]) <<" ";
		}
		out << "||| "<< td.convert(instance.event_type)<<" ";
		
		out<<"\n";
		return out;
	}
	void load(const string& line){
                istringstream in(line);
                string word;
		while(in>>word) {
			if(word == "|||") break;
			id = atoi(word.c_str());
		}
                while(in>>word) {
                        if(word == "|||") break;
                        raws.push_back(wd.convert(word));
                        normalize_digital_lower(word);
                        lows.push_back(wd.convert(word));
                }
		assert(word == "|||");
		while(in>>word){
			if(word == "|||") break;
			labels.push_back(atoi(word.c_str()));
		}
                assert(word == "|||");
		while(in>>word){
			if(word == "|||") break;
			questions.push_back(qd.convert(word));
		}
		in >> word;
		event_type = td.convert(word);
        }
	unsigned size(){assert(raws.size() == lows.size()); return raws.size();}
};

struct LSTMClassifier {
    LookupParameter p_word;
    LookupParameter p_ques;

    Parameter p_start;
    Parameter p_end;

    Parameter p_q_start;
    Parameter p_q_end;

    LSTMBuilder l2rbuilder;
    LSTMBuilder r2lbuilder;

    LSTMBuilder q_l2rbuilder;
    LSTMBuilder q_r2lbuilder;

    Parameter p_attbias;
    Parameter p_input2att;
    Parameter p_question2att;
    Parameter p_att2weight;

    Parameter p_typebias;
    Parameter p_sent2type;
    Parameter p_typehidden2typediste;

    explicit LSTMClassifier(Model& model) :
        l2rbuilder(LAYERS, WORD_DIM , HIDDEN_DIM, model),
        r2lbuilder(LAYERS, WORD_DIM , HIDDEN_DIM, model),
        q_l2rbuilder(LAYERS, WORD_DIM , HIDDEN_DIM, model),
        q_r2lbuilder(LAYERS, WORD_DIM , HIDDEN_DIM, model)
    {
        p_word = model.add_lookup_parameters(VOCAB_SIZE, {WORD_DIM});
	p_ques = model.add_lookup_parameters(QUES_SIZE,{WORD_DIM});

        p_start = model.add_parameters({WORD_DIM});
	p_end = model.add_parameters({WORD_DIM});

	p_q_start = model.add_parameters({WORD_DIM});
        p_q_end = model.add_parameters({WORD_DIM});

	p_attbias = model.add_parameters({ATTENTION_HIDDEN_DIM});
	p_input2att = model.add_parameters({ATTENTION_HIDDEN_DIM, 2*HIDDEN_DIM});
	p_question2att = model.add_parameters({ATTENTION_HIDDEN_DIM, 2*HIDDEN_DIM});
	p_att2weight = model.add_parameters({2, ATTENTION_HIDDEN_DIM});
     
	p_typebias = model.add_parameters({TYPE_HIDDEN_DIM});
        p_sent2type = model.add_parameters({TYPE_HIDDEN_DIM, 2*HIDDEN_DIM});
	p_typehidden2typediste = model.add_parameters({TYPE_SIZE, TYPE_HIDDEN_DIM});

        for(auto& it : pretrained){
	    p_word.initialize(it.first, it.second);
        }
    }

    // return Expression of total loss
    Expression BuildGraph(Instance& inst, ComputationGraph& cg, vector<unsigned> *results, unsigned *type_result,  bool train) {
        const vector<unsigned>& sent = inst.words;
	const vector<unsigned>& ques = inst.questions;
	assert(inst.labels.size() == 2);
	
        int ts = inst.labels[0];
	int te = inst.labels[1];
	const unsigned slen = sent.size();
	const unsigned qlen = ques.size();

        l2rbuilder.new_graph(cg);  // reset builder for new graph
        l2rbuilder.start_new_sequence();
        r2lbuilder.new_graph(cg);  // reset builder for new graph
        r2lbuilder.start_new_sequence();

	q_l2rbuilder.new_graph(cg);  // reset builder for new graph
        q_l2rbuilder.start_new_sequence();
        q_r2lbuilder.new_graph(cg);  // reset builder for new graph
        q_r2lbuilder.start_new_sequence();

	Expression word_start = parameter(cg, p_start);
        Expression word_end = parameter(cg, p_end);

	Expression ques_start = parameter(cg, p_q_start);
        Expression ques_end = parameter(cg, p_q_end);

	Expression attbias = parameter(cg, p_attbias);
	Expression input2att = parameter(cg, p_input2att);
	Expression question2att = parameter(cg, p_question2att);
	Expression att2weight = parameter(cg, p_att2weight);

	Expression typebias = parameter(cg, p_typebias);
	Expression sent2type = parameter(cg, p_sent2type);
	Expression typehidden2typediste = parameter(cg, p_typehidden2typediste);

if(DEBUG)	cerr<<"sent size " << slen<<"\n";
        vector<Expression> i_words(slen);
        for (unsigned t = 0; t < slen; ++t) {
            i_words[t] = lookup(cg, p_word, sent[t]);
            if (train) i_words[t] = dropout(i_words[t], pdrop);
        }

if(DEBUG)	cerr<<"all input expression done\n";
	
	//words contained using "inputs"
	vector<Expression> l2r(slen);
	vector<Expression> r2l(slen);
        l2rbuilder.add_input(word_start);
        r2lbuilder.add_input(word_end);
        for (unsigned t = 0; t < slen; ++t) {
	    l2r[t] = l2rbuilder.add_input(i_words[t]);
            r2l[slen - 1 - t] = r2lbuilder.add_input(i_words[slen - 1 - t]);
        }
	Expression sentl2r = l2rbuilder.add_input(word_end);
        Expression sentr2l = r2lbuilder.add_input(word_start);
	
	vector<Expression> inputs;
	for (unsigned t = 0; t < slen; ++t) {
		inputs.push_back(concatenate({l2r[t],r2l[t]}));
	}

	//questions contained using "question"
	vector<Expression> q_l2r(slen);
	vector<Expression> q_r2l(slen);
        q_l2rbuilder.add_input(ques_start);
        q_r2lbuilder.add_input(ques_end);
        for (unsigned t = 0; t < slen; ++t) {
	    l2r[t] = l2rbuilder.add_input(i_words[t]);
            r2l[slen - 1 - t] = r2lbuilder.add_input(i_words[slen - 1 - t]);
        }
	Expression ql2r = q_l2rbuilder.add_input(ques_end);
        Expression qr2l = q_r2lbuilder.add_input(ques_start);
	Expression question = concatenate({ql2r, qr2l});
	
	//attention-like weight 0 1
	vector<Expression> atts(inputs.size());
	vector<Expression> weights(inputs.size());
        for(unsigned t = 0; t < inputs.size(); t ++){
        	atts[t] = tanh(affine_transform({attbias, input2att, inputs[t], question2att, question}));
      		weights[t] = att2weight * atts[t];
	}
	vector<Expression> log_prob;

	//for event type
	Expression sente = concatenate({sentl2r,sentr2l});
	Expression event_type_hidden = tanh(affine_transform({typebias, sent2type, sente}));
	Expression event_type_diste = log_softmax(typehidden2typediste * event_type_hidden);
	log_prob.push_back(pick(event_type_diste, inst.event_type));
	auto prob = as_vector(cg.incremental_forward(event_type_diste));
	unsigned best = 0;
	float bestp = prob[0];
	for(unsigned t = 1; t < TYPE_SIZE; t++){
		if(prob[t] > bestp){
			best = t;
			bestp = prob[t];
		}
	}
	if(type_result) *type_result = best;

	//for arguments
	for(unsigned t = 0; t < inputs.size(); t ++){
		Expression adiste = log_softmax(weights[t]);
		if(ts == -1) log_prob.push_back(pick(adiste, (unsigned)0));
		else{
			if(t >= (unsigned)ts && t <= (unsigned)te) {log_prob.push_back(pick(adiste, (unsigned)1));}
			else log_prob.push_back(pick(adiste, (unsigned)0));
		}
		auto prob = as_vector(cg.incremental_forward(adiste));
		if(prob[1] > prob[0]) if(results) results->push_back(t);
	}
	Expression output_loss = -sum(log_prob);
	return output_loss;
    }
};

void output(vector<Instance>& instances, LSTMClassifier& lstmClassifier)
{
    float num_correct = 0;
    float loss = 0;
    bool pred;
    float acc;
    ofstream out("OUT.txt");
    for (auto& sent : instances) {
        ComputationGraph cg;
	vector<unsigned> results;
	unsigned type_result;
        Expression nll = lstmClassifier.BuildGraph(sent, cg, &results, &type_result, false);
        loss += as_scalar(cg.incremental_forward(nll));
	
	out << sent.id <<" ||| ";
	for(unsigned i = 0; i < sent.raws.size(); i ++){
		out<<wd.convert(sent.raws[i])<<" ";
	}
	out<<"||| ";
	if(results.size() == 0) out << "-1 -1 ";
	else{
		for(unsigned i = 0; i < results.size(); i ++){
			out << results[i] << " ";
		}
	}
	out<<"||| ";
	for(unsigned i = 0; i < sent.questions.size(); i ++){
		out<<qd.convert(sent.questions[i])<<" ";
	}
	out<<"||| ";
	out<<td.convert(type_result)<<"\n";
    }
    out.flush();
    out.close();
    cerr<<"Loss:"<< loss/ instances.size() << " ";
}

void evaluate(vector<Instance>& instances, LSTMClassifier& lstmClassifier, float& aacc, float& eacc)
{
    output(instances, lstmClassifier);
    std::string command = "python evaluation.py dev.dat OUT.txt > OUT.eval";
    const char* cmd = command.c_str();
    system(cmd);
    ifstream ifs("OUT.eval");
    ifs>>aacc;
    ifs>>eacc;
    ifs.close();
    cerr<<"argument F1: "<<aacc<<" " << "event type accuracy: "<<eacc<<"\n";
}



int main(int argc, char** argv) {
    DynetParams dynet_params = extract_dynet_params(argc, argv);
    dynet_params.random_seed = 1989121013;
    dynet::initialize(dynet_params);
  
    cerr << "COMMAND:";
    for (unsigned i = 0; i < static_cast<unsigned>(argc); ++i) cerr << ' ' << argv[i];
    cerr << endl;

    po::variables_map conf;
    InitCommandLine(argc, argv, &conf);

    WORD_DIM = conf["word_dim"].as<unsigned>();
    PRE_WORD_DIM = conf["pre_word_dim"].as<unsigned>();
    HIDDEN_DIM = conf["hidden_dim"].as<unsigned>();
    LAYERS = conf["layers"].as<unsigned>();
    unk_prob = conf["unk_prob"].as<float>();
    pdrop = conf["pdrop"].as<float>();

    DEBUG = conf.count("debug");

    assert(unk_prob >= 0.); assert(unk_prob <= 1.);
    assert(pdrop >= 0.); assert(pdrop <= 1.);

    kUNK = wd.convert("*UNK*");

    vector<Instance> training,dev,test;
    string line;
  
    //reading pretrained
    if(conf.count("pretrained")){
      cerr << "Loading from " << conf["pretrained"].as<string>() << " as pretrained embedding with" << WORD_DIM << " dimensions ... ";
      ifstream in(conf["pretrained"].as<string>().c_str());
      string word;
      while(in>>word){
        vector<float> v(PRE_WORD_DIM);
        for(unsigned i = 0; i < PRE_WORD_DIM; i++) {in>>v[i];}
        pretrained[wd.convert(word)] = v;
      }
      cerr << pretrained.size() << " ok\n";
    }
    
    //reading training data
    cerr << "Loading from " << conf["training_data"].as<string>() << "as training data : ";
    {
      ifstream in(conf["training_data"].as<string>().c_str());
      assert(in);
      while(getline(in, line)) {
        Instance instance;
        instance.load(line);
        training.push_back(instance);
      }
      cerr<<training.size()<<"\n";
    }

    //couting
    set<unsigned> training_vocab;
    set<unsigned> singletons;
    {
      map<unsigned, unsigned> counts;
      for (auto& sent : training){
	const vector<unsigned>& raws = sent.raws;
        const vector<unsigned>& lows = sent.lows;
        vector<unsigned>& words = sent.words;
	words.resize(raws.size());
	for (unsigned i = 0; i < sent.size(); ++i){
	  if(pretrained.size() > 0){
	    if(pretrained.count(raws[i])) words[i] = raws[i];
	    else if(pretrained.count(lows[i])) words[i] = lows[i];
	  }
          training_vocab.insert(words[i]); counts[words[i]]++;
	}
      }
      for (auto wc : counts)
        if (wc.second == 1) singletons.insert(wc.first);
      
      cerr<<"the training word dict size is " << training_vocab.size()
	     << " where The singletons have " << singletons.size() << "\n";
    }

    //replace unk 
    {
      int unk = 0;
      int total = 0;
      for(auto& sent : training){
        for(auto& w : sent.words){
          if(singletons.count(w) && dynet::rand01() < unk_prob){
	  	w = kUNK;
		unk += 1;
 	  }
          total += 1;
        }
      }
      cerr << "the number of word is: "<< total << ", where UNK is: "<<unk<<"("<<unk*1.0/total<<")\n";
    }

    //reading dev data 
    if(conf.count("dev_data")){
      cerr << "Loading from " << conf["dev_data"].as<string>() << "as dev data : ";
      ifstream in(conf["dev_data"].as<string>().c_str());
      string line;
      while(getline(in,line)){
        Instance inst;
        inst.load(line);
        dev.push_back(inst);
      }
      cerr<<dev.size()<<"\n";
    }

    //replace unk
    {
      int unk = 0;
      int total = 0;
      for(auto& sent : dev){
        const vector<unsigned>& raws = sent.raws;
        const vector<unsigned>& lows = sent.lows;
        vector<unsigned>& words = sent.words;
	words.resize(raws.size());
	for(unsigned i = 0; i < sent.size(); i ++){
          if(pretrained.count(raws[i])) words[i] = raws[i];
	  else if(pretrained.count(lows[i])) words[i] = lows[i];
	  else if(training_vocab.count(raws[i])) words[i] = raws[i];
	  else{
	  	words[i] = kUNK;
		unk += 1;
	  }
          total += 1;
        }
      }
      cerr << "the number of word is: "<< total << ", where UNK is: "<<unk<<"("<<unk*1.0/total<<")\n";
    }
 
    //reading test data
    if(conf.count("test_data")){
      cerr << "Loading from " << conf["test_data"].as<string>() << "as test data : ";
      ifstream in(conf["test_data"].as<string>().c_str());
      string line;
      while(getline(in,line)){
        Instance inst;
        inst.load(line);
        test.push_back(inst);
      }
      cerr<<test.size()<<"\n";
    }

    //replace unk
    {
      int unk = 0;
      int total = 0;
      for(auto& sent : test){
        const vector<unsigned>& raws = sent.raws;
        const vector<unsigned>& lows = sent.lows;
        vector<unsigned>& words = sent.words;
	words.resize(raws.size());
        for(unsigned i = 0; i < sent.size(); i ++){
          if(pretrained.count(raws[i])) words[i] = raws[i];
          else if(pretrained.count(lows[i])) words[i] = lows[i];
          else if(training_vocab.count(raws[i])) words[i] = raws[i];
          else{
                words[i] = kUNK;
                unk += 1;
          }
          total += 1;
        }
      }
      cerr << "the number of word is: "<< total << ", where UNK is: "<<unk<<"("<<unk*1.0/total<<")\n";
    }

if(DEBUG){
    for(unsigned i = 0; i < training.size(); i ++){
    	cout << training[i];
    }

    for(unsigned i = 0; i < dev.size(); i ++){
        cout << dev[i];
    }

    for(unsigned i = 0; i < test.size(); i ++){
        cout << test[i];
    }
}
    VOCAB_SIZE = wd.size();
    QUES_SIZE = qd.size();
    TYPE_SIZE = td.size();

    ostringstream os;
    os << "lstmclassifier"
       << '_' << WORD_DIM
       << '_' << HIDDEN_DIM
       << '_' << LAYERS
       << "-pid" << getpid() << ".params";
    const string fname = os.str();
    cerr << "Parameter will be written to: " << fname << endl;
    float best = 0;
    float bestf1 = 0;
    Model model;
    Trainer* sgd = nullptr;
    unsigned method = conf["train_methods"].as<unsigned>();
    if(method == 0)
  	sgd = new SimpleSGDTrainer(model,0.1, 0.1);
    else if(method == 1)
	sgd = new MomentumSGDTrainer(model,0.01, 0.9, 0.1);
    else if(method == 2){
	sgd = new AdagradTrainer(model);
	sgd->clipping_enabled = false;	
    }
    else if(method == 3){
	sgd = new AdamTrainer(model);
  	sgd->clipping_enabled = false;
    } 
    LSTMClassifier lstmClassifier(model);
	if (conf.count("model")) {
    string fname = conf["model"].as<string>();
    ifstream in(fname);
    boost::archive::text_iarchive ia(in);
    ia >> model;
    }
if(conf.count("test") == 0){
if(DEBUG)	cerr<<"begin\n";
    unsigned report_every_i = conf["report_i"].as<unsigned>();
    unsigned dev_report_every_i = conf["dev_report_i"].as<unsigned>();
    unsigned si = training.size();
    vector<unsigned> order(training.size());
    for (unsigned i = 0; i < order.size(); ++i) order[i] = i;
    bool first = true;
    int report = 0;
    unsigned lines = 0;
    int exceed_count = 0;
    unsigned count = 0;
    while(count < conf["count_limit"].as<unsigned>()) {
        Timer iteration("completed in");
        float loss = 0;
        unsigned ttags = 0;
        for (unsigned i = 0; i < report_every_i; ++i) {
            if (si == training.size()) {
                si = 0;
                if (first) {
                    first = false;
                }
                else {
                    sgd->update_epoch();
                    if (1) {
                        float aacc = 0.f;
			float eacc = 0.f;
                        cerr << "\n***DEV [epoch=" << (lines / (float)training.size()) << "] ";
                        evaluate(dev, lstmClassifier, aacc, eacc);
                        if (aacc > best || (fabs(aacc - best) <= 0.0000001)) {
                            best = aacc;
                            cerr<< "Exceed" << " ";
                            float taacc = 0;
			    float teacc = 0;
                            evaluate(test, lstmClassifier, taacc, teacc);
                            
			    ostringstream part_os;
                            part_os << "lstmclassifier"
                                << '_' << WORD_DIM
                                << '_' << HIDDEN_DIM
                                << '_' << LAYERS
                                << "-pid" << getpid()
                                << "-part" << (lines/(float)training.size()) << ".params";

                            const string part = part_os.str();
                            ofstream out("model/"+part);
                            boost::archive::text_oarchive oa(out);
                            oa << model;
                        }
			cerr<<"\n";
                    }
                }
                cerr << "**SHUFFLE\n";
                shuffle(order.begin(), order.end(), *rndeng);
                count++;
            }

            ComputationGraph cg;
            auto& sentx_y = training[order[si]];
	    float num_correct = 0;
            Expression nll= lstmClassifier.BuildGraph(sentx_y, cg, NULL, NULL, true);
	    loss += as_scalar(cg.incremental_forward(nll));
            cg.backward(nll);
            sgd->update(1.0);
            ++si;
            ++lines;
            ++ttags;
        }
        sgd->status();
        cerr << " E = " << (loss / ttags) <<" "<<loss << "/"<<ttags<<" ";

        // show score on dev data?
        report++;
        if ( report % dev_report_every_i == 0 ) {
	    float aacc;
	    float eacc;
	    cerr << "\n***DEV [epoch=" << (lines / (float)training.size()) << "] ";
            evaluate(dev, lstmClassifier, aacc, eacc);
	    exit(1);
	    if (aacc > best || (fabs(aacc - best) <= 0.0000001)) {
         	best = aacc;
            	cerr<< "Exceed" << " ";
            	float taacc = 0;
           	float teacc = 0;
               	evaluate(test, lstmClassifier, taacc, teacc);
	        ostringstream part_os;
                part_os << "lstmclassifier"
                 	<< '_' << WORD_DIM
                    	<< '_' << HIDDEN_DIM
                      	<< '_' << LAYERS
                     	<< "-pid" << getpid()
                    	<< "-part" << (lines/(float)training.size()) << ".params";

              	const string part = part_os.str();
             	ofstream out("model/"+part);
               	boost::archive::text_oarchive oa(out);
              	oa << model;
            }
        }
    }
    delete sgd;

	}
    else{
        output(test, lstmClassifier);
    }
}


