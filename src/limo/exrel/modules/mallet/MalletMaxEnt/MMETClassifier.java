package limo.exrel.modules.mallet.MalletMaxEnt;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.regex.Pattern;

import cc.mallet.classify.Classification;
import limo.exrel.modules.mallet.MalletMaxEnt.MaxEnt;
import cc.mallet.classify.Trial;
import cc.mallet.pipe.Pipe;
import cc.mallet.pipe.iterator.CsvIterator;
import cc.mallet.types.Alphabet;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import cc.mallet.types.Labeling;
import cc.mallet.types.SparseVector;
import cc.mallet.classify.Classifier;

public class MMETClassifier extends Classifier implements GeneralMaxEntClassifier {

	MaxEnt _model;
	String modelFileName = new String();
	Pipe pipe;
	
	public MMETClassifier(MaxEnt model) {
		_model = model;
		if (model != null) {
			pipe = _model.getInstancePipe();
		}
		pipe.getDataAlphabet().stopGrowth();
		pipe.getTargetAlphabet().stopGrowth();
	}
	
	public MMETClassifier(String modelFile) throws Throwable {
		this.modelFileName = modelFile;
		ObjectInputStream objIn = new ObjectInputStream(new FileInputStream(new File(modelFileName)));
		_model = (MaxEnt) objIn.readObject();
		pipe = _model.getInstancePipe();
		pipe.getDataAlphabet().stopGrowth();
		pipe.getTargetAlphabet().stopGrowth();
		objIn.close();
	}
	
	public Pipe getPipeline() {
		return pipe;
	}
	
	public Classification classify (Instance instance) {
		return _model.classify(instance);
	}
	
	private Classification classify(String predicates) {
		String dump = (String) pipe.getTargetAlphabet().toArray()[0];
		
		Instance carrier = new Instance(predicates, dump, null, predicates);
		
		InstanceList ilist = new InstanceList(pipe);
		ilist.addThruPipe(carrier);
		
		return classify(ilist.get(0));
	}
	
	private InstanceList createInstanceList(String testingFile) throws Throwable {
		InstanceList ret = new InstanceList(pipe);
		
		Reader fileReader = new InputStreamReader(new FileInputStream(new File(testingFile)), "UTF-8");
		ret.addThruPipe(new MCsvIterator (fileReader, Pattern.compile("^(\\S*)[\\s,]*(\\S*)[\\s,]*(.*)$"), 3, 2, 1));
		
		cleanData(ret);
		return ret;
	}
	
	// Various evaluation methods
	
	public double getAccuracy (String testingFile) throws Throwable { return _model.getAccuracy(createInstanceList(testingFile)); }
	public double getPrecision (String testingFile, int index) throws Throwable { return _model.getPrecision(createInstanceList(testingFile), index); }
	public double getPrecision (String testingFile, Labeling labeling) throws Throwable { return _model.getPrecision(createInstanceList(testingFile), labeling); }
	public double getPrecision (String testingFile, Object labelEntry) throws Throwable { return _model.getPrecision(createInstanceList(testingFile), labelEntry); }
	public double getRecall (String testingFile, int index) throws Throwable { return _model.getRecall(createInstanceList(testingFile), index); }
	public double getRecall (String testingFile, Labeling labeling) throws Throwable { return _model.getRecall(createInstanceList(testingFile), labeling); }
	public double getRecall (String testingFile, Object labelEntry) throws Throwable { return _model.getRecall(createInstanceList(testingFile), labelEntry); }
	public double getF1 (String testingFile, int index) throws Throwable { return _model.getF1(createInstanceList(testingFile), index); }
	public double getF1 (String testingFile, Labeling labeling) throws Throwable { return _model.getF1(createInstanceList(testingFile), labeling); }
	public double getF1 (String testingFile, Object labelEntry) throws Throwable { return _model.getF1(createInstanceList(testingFile), labelEntry); }
	public double getAverageRank (String testingFile) throws Throwable { return _model.getAverageRank(createInstanceList(testingFile)); }
	
	public int getNumberOfOutCome() {return pipe.getTargetAlphabet().size();}
	
	private void cleanData(InstanceList ilist) {
		ArrayList<Integer> toRemoved = new ArrayList<Integer>();
		Instance carrier = null;
		for (int i = 0; i < ilist.size(); i++) {
			carrier = ilist.get(i);
			if (carrier.getTarget() == null)
				toRemoved.add(i);
		}
		
		Collections.sort(toRemoved, Collections.reverseOrder());
		
		for (Integer idx : toRemoved) {
			ilist.remove(idx);
		}
	}
	
	public String eval(String predicates) {
		return (String) classify(predicates).getLabeling().getBestLabel().getEntry();
	}
	
	public String eval (String predicates, boolean real) {
		return eval(predicates);
	}
	
	public double getBestOutComeScore (String predicates) {
		return classify(predicates).getLabeling().getBestValue();
	}
	
	public double getBestOutComeScore (String predicates, boolean real) {
		return getBestOutComeScore(predicates);
	}
	
	public double[] getOutComeProbabilities(String predicates) {
		return ((SparseVector) classify(predicates).getLabeling()).getValues();
	}
	
	public String allOutComeText(String predicates) {
		double[] scores = getOutComeProbabilities(predicates);
		Alphabet alphabet = pipe.getTargetAlphabet();
		String ret = "", label = "";
		for (int i = 0; i  < scores.length; i++) {
			label = (String) alphabet.lookupObject(i);
			ret += label + "[" + scores[i] + "] ";
		}
		return ret.trim();
	}
	
	public String getAllPredications (String predicates, boolean real) {
		return allOutComeText(predicates);
	}
	
	public static void main(String[] args) throws Throwable {
		String fileModel = "/Users/thien/workspace/REJet/Kdd_Thien/re/systemFile/test/featuresModel.txt";
		String input = "/Users/thien/workspace/REJet/Kdd_Thien/re/systemFile/test/features.txt";
		String testing = "/Users/thien/workspace/REJet/Kdd_Thien/re/systemFile/test/testing.txt";
		
		BufferedReader reader = new BufferedReader(new FileReader(input));
		String feature = "";
		int only = 5, count = 0;
		MMETClassifier mmet = new MMETClassifier(fileModel);
		String prediction = "";
		while ((feature = reader.readLine()) != null) {
			count++;
			if (count > only) break;
			feature = feature.substring(feature.lastIndexOf("\t")+1);
			//System.out.println(feature);
			//prediction = mmet.eval(feature);
			//System.out.println("Instance#" + count + ": " + prediction);
			//System.out.println("Instance#" + count + ": \n" + mmet.eval(feature) + " " + mmet.getBestOutComeScore(feature));
			System.out.println("Instance#" + count + " : " + mmet.allOutComeText(feature));
		}
		
//		System.out.println("Accuracy = " + mmet.getAccuracy(testing));
//		int numberOutcomes = mmet.getNumberOfOutCome();
//		System.out.println("Precision:");
//		for (int i = 0; i < numberOutcomes; i++)
//			System.out.println(mmet.getPipeline().getTargetAlphabet().lookupObject(i) + " : " + mmet.getPrecision(testing, i));
//		System.out.println("Recal:");
//		for (int i = 0; i < numberOutcomes; i++)
//			System.out.println(mmet.getPipeline().getTargetAlphabet().lookupObject(i) + " : " + mmet.getRecall(testing, i));
//		System.out.println("F1:");
//		for (int i = 0; i < numberOutcomes; i++)
//			System.out.println(mmet.getPipeline().getTargetAlphabet().lookupObject(i) + " : " + mmet.getF1(testing, i));
		
		reader.close();
	}
}
