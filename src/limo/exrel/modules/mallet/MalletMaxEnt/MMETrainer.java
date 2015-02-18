package limo.exrel.modules.mallet.MalletMaxEnt;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.Reader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.regex.Pattern;


import cc.mallet.classify.ClassifierTrainer;
import limo.exrel.modules.mallet.MalletMaxEnt.MaxEnt;
import limo.exrel.modules.mallet.MalletMaxEnt.Regularizer.ConstantRegularizer;
import limo.exrel.modules.mallet.MalletMaxEnt.Regularizer.RegularizationWeightProducer;
import limo.exrel.modules.mallet.MalletMaxEnt.Weighter.ConstantWeighter;
import limo.exrel.modules.mallet.MalletMaxEnt.Weighter.InstanceWeightProducer;
import cc.mallet.pipe.FeatureSequence2FeatureVector;
import cc.mallet.pipe.Pipe;
import cc.mallet.pipe.SerialPipes;
import cc.mallet.pipe.Target2Label;
import cc.mallet.pipe.TokenSequence2FeatureSequence;
import cc.mallet.types.FeatureVector;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;

public class MMETrainer {
	
	HashMap<String, Double> frequency = null;
	final static int DEFAULT_CUTOFF = 4, DEFAULT_NUMBER_OF_ITERATION = 200;
	public int cutoff = DEFAULT_CUTOFF, numberOfIterations = DEFAULT_NUMBER_OF_ITERATION;
    public String dataFileName = new String(),  modelFileName = new String();
    //double gaussianPriorVariance = 1.0;
    
    ClassifierTrainer.ByOptimization<MaxEnt> trainer = null;
    
    RegularizationWeightProducer regularizer = null;
    InstanceWeightProducer weighter = null;
    
    public InstanceList trainingData = null;
    
    MaxEnt model;
    
    public final static int L1 = 1, L2 = 2;
    final static double DEFAULT_BINARY_VALUE = 1.0;
    int type = L2;
    boolean binary = true;
    double binaryValue = DEFAULT_BINARY_VALUE;
    
    HashMap<String, Double> modifiedFrequency = null;
    
    public MMETrainer(String dataFile) throws Throwable {
    	this(dataFile, L2, true, DEFAULT_BINARY_VALUE, null);
    }
    
    public MMETrainer(String dataFile, HashMap<String, Double> modifiedFrequency) throws Throwable {
    	this(dataFile, L2, true, DEFAULT_BINARY_VALUE, modifiedFrequency);
    }
    
    public MMETrainer(String dataFile, int type) throws Throwable {
    	this(dataFile, type, true, DEFAULT_BINARY_VALUE, null);
    }
    
    public MMETrainer(String dataFile, int type, boolean binary) throws Throwable {
    	this(dataFile, type, binary, DEFAULT_BINARY_VALUE, null);
    }
    
    public MMETrainer(String dataFile, int type, boolean binary, double binaryValue, HashMap<String, Double> modifiedFrequency) throws Throwable {
    	this.dataFileName = dataFile;    	
    	modelFileName =
            dataFileName.substring(0,dataFileName.lastIndexOf('.'))
            + "Model.txt";
    	
    	if (type == L1 || type == L2)
    		this.type = type;
    	
    	this.binary = binary;
    	
    	if (binaryValue > 0)
    		this.binaryValue = binaryValue;
    	
    	this.cutoff = DEFAULT_CUTOFF;
    	this.numberOfIterations = DEFAULT_NUMBER_OF_ITERATION;
    	
    	this.modifiedFrequency = modifiedFrequency;
    	
    	fillFrequency();
    }
    
    public MMETrainer(String dataFile, int type, boolean binary, RegularizationWeightProducer regularizer, InstanceWeightProducer weighter) throws Throwable {
    	this(dataFile, type, binary, DEFAULT_CUTOFF, DEFAULT_NUMBER_OF_ITERATION, regularizer, weighter, DEFAULT_BINARY_VALUE);
    }
    
    public MMETrainer(String dataFile, int type, boolean binary,
    		int cutoff, int numberOfIterations, RegularizationWeightProducer regularizer, InstanceWeightProducer weighter, double binaryValue) throws Throwable {
    	if (dataFile == null) return;
    	this.dataFileName = dataFile;
    	if (dataFileName.lastIndexOf('.') > 0)
	    	this.modelFileName =
	            dataFileName.substring(0,dataFileName.lastIndexOf('.'))
	            + "Model.txt";
    	else
    		this.modelFileName = dataFileName + "Model.txt";
    	
    	this.binary = binary;
    	if (numberOfIterations > 0)
    		this.numberOfIterations = numberOfIterations;
    	if (cutoff >= 0)
    		this.cutoff = cutoff;
    	this.regularizer = regularizer;
    	this.weighter = weighter;
    	
    	if (binaryValue > 0)
    		this.binaryValue = binaryValue;
    	
    	if (type == L1 || type == L2)
    		this.type = type;
    	
    	this.modifiedFrequency = null;
    	
    	fillFrequency();
    }
    
    public void setType(int type) {
    	if (type == L1 || type == L2)
    		this.type = type;
    }
    
    public void setRegularizer(RegularizationWeightProducer regularizer) {
    	this.regularizer = regularizer;
    }
    
    public void setWeighter(InstanceWeightProducer weighter) {
    	this.weighter = weighter;
    }
    
    public void setNumberOfIterations(int numberOfIterations) {
    	if (numberOfIterations > 0)
    		this.numberOfIterations = numberOfIterations;
    }
    
    public void setCutoff(int cutoff) {
    	if (cutoff >= 0)
    		this.cutoff = cutoff;
    }
    
    public void setBinary(boolean binary) {
    	this.binary = binary;
    }
    
    public boolean getBinary() { return binary;}
    
    public void setBinaryValue(double binaryValue) {
    	this.binaryValue = binaryValue;
    }
    
    public MaxEnt getModel() {
    	return model;
    }
	
	private void fillFrequency() throws Throwable {
		
		if (frequency == null) frequency = new HashMap<String, Double>();
		
		BufferedReader reader = new BufferedReader(new FileReader(dataFileName));
		
		String line = "";
		String[] features = null;
		double count = 0.0;
		while ((line = reader.readLine()) != null) {
			features = line.split("\t")[2].split(" ");
			
			for (String key : features) {
				if (!binary && key.contains("=="))
					key = key.substring(0, key.lastIndexOf("=="));
				
				if (modifiedFrequency != null && !modifiedFrequency.containsKey(key)) continue;
				
				count = 0.0;
				if (frequency.containsKey(key))
					count = frequency.get(key).doubleValue();
				count++;
				frequency.put(key, count);
			}
		}
		
		reader.close();
	}
	
	private Pipe createPile() {
		System.out.println("Creating pipeline ...");
		if (binary)
			return new SerialPipes(new Pipe[] {
					new CharSequence2TokenSequenceSimple(Pattern.compile("\\S*")),
					new TokenSequenceSimpleRemoveRareTokens(frequency, cutoff, true),
					new Target2Label(),
					new TokenSequence2FeatureSequence(),
					new FeatureSequence2FeatureVector()
				});
		
		return new SerialPipes(new Pipe[] {
				new CharSequence2TokenSequenceSimple(Pattern.compile("\\S*")),
				new TokenSequenceSimpleRemoveRareTokens(frequency, cutoff, false),
				new Target2Label(),
				new TokenSequence2FeatureSequenceValues(binaryValue),
				new FeatureSequenceValues2FeatureVector()
			});
	}
	
	private void createInstanceList() throws Throwable {
		
		Pipe pipe = createPile();
		
		System.out.println("Creating data instance list ...");
		trainingData = new InstanceList(pipe);
		
		Reader fileReader = new InputStreamReader(new FileInputStream(new File(dataFileName)), "UTF-8");
		trainingData.addThruPipe(new MCsvIterator (fileReader, Pattern.compile("^(\\S*)[\\s,]*(\\S*)[\\s,]*(.*)$"), 3, 2, 1));
		
		cleanData(trainingData);
		
		populateInstanceWeights();
	}
	
	
	//remove instances with no features
	private void cleanData(InstanceList ilist) {
		//ArrayList<Integer> toRemoved = new ArrayList<Integer>();
		Instance carrier = null;
		int size = ilist.size();
		for (int i = size-1; i >= 0; i--) {
			carrier = ilist.get(i);
			if (((FeatureVector) carrier.getData()).getIndices().length == 0) {
				ilist.remove(i);
			}
		}
//		System.out.println("Removed: " + count);
//		if (1==1) return;
//		for (int i = 0; i < ilist.size(); i++) {
//			carrier = ilist.get(i);
//			if (((FeatureVector) carrier.getData()).getIndices().length == 0)
//				toRemoved.add(i);
//		}
//		
//		Collections.sort(toRemoved, Collections.reverseOrder());
//		
//		for (Integer idx : toRemoved) {
//			ilist.remove(idx);
//		}
	}
	
	private void populateInstanceWeights() {
		if (weighter == null)
			weighter = new ConstantWeighter(1.0);
		Iterator<Instance> iter = trainingData.iterator();
		while (iter.hasNext()) {
			Instance instance = iter.next();
			trainingData.setInstanceWeight(instance, weighter.getWeight((String) instance.getSource()));
		}
	}
	
//	private MaxEnt train(InstanceList trainingData, int numberOfIterations) throws Throwable {
//		//MaxEntTrainer maxEntTrainer = new MaxEntTrainer();
//		//maxEntTrainer.setGaussianPriorVariance(gaussianPriorVariance);
//		createTrainer();
//		MaxEnt maxEntClassifier = trainer.train(trainingData, numberOfIterations);
//		
//		//ObjectOutputStream objOut = new ObjectOutputStream(new FileOutputStream(new File(modelFileName)));
//		//maxEntClassifier.writeObject(objOut);
//		
//		return maxEntClassifier;
//	}
	
//	public void setFequencyBasedRegularizer(ArrayList<String> sourceFiles, ArrayList<String> targetFiles, String featureFile, 
//			double upper, double lower, double lastFeature, double threshold) throws Throwable {
//		setRegularizer(new CommonThresholdFrequencyBasedRegularizer(this, sourceFiles, targetFiles, featureFile, upper, lower, threshold, lastFeature));
//	}
	
//	public void setFequencyBasedRegularizer(ArrayList<String> sourceFiles, ArrayList<String> targetFiles, String featureFile, double[] params) throws Throwable {
//		setRegularizer(new CommonThresholdFrequencyBasedRegularizer(this, sourceFiles, targetFiles, featureFile, params[0], params[1], params[2], params[3]));
//	}
	
//	public void setDistictionFrequencyBasedRegularizer(ArrayList<String> sourceFiles, ArrayList<String> targetFiles, String featureFile, double[] params) throws Throwable {
//		setRegularizer(new FeatureDistinctionFrequencyBasedRegularizer(this, sourceFiles, targetFiles, featureFile, params[0], params[1], params[2], params[3]));
//	}
	
	private void createTrainer() {
    	if (type == L1)
    		trainer = new MMaxEntTrainerL1(regularizer);
    	else if (type == L2)
    		trainer = new MMaxEntTrainerL2(regularizer);
    }
	
	public void writeModel(String out) throws Throwable {
		ObjectOutputStream objOut = new ObjectOutputStream(new FileOutputStream(new File(out)));
		objOut.writeObject(model);
		objOut.close();
	}
	
	public MaxEnt train(boolean write) throws Throwable {
		createInstanceList();
		createTrainer();
		model = trainer.train(trainingData, numberOfIterations);
		if (write)
			writeModel(modelFileName);
		return model;
	}
	
//	public MaxEnt trainWithCommonThresholdRegularizer(ArrayList<String> sourceFiles, ArrayList<String> targetFiles, String featureFile, double[] params) throws Throwable {
//		createInstanceList();
//		setFequencyBasedRegularizer(sourceFiles, targetFiles, featureFile, params);
//		createTrainer();
//		model = trainer.train(trainingData, numberOfIterations);
//		writeModel(modelFileName);
//		return model;
//	}
	
//	public MaxEnt trainWithDistinctionFrequncyBasedRegularizer(ArrayList<String> sourceFiles, ArrayList<String> targetFiles, String featureFile, double[] params) throws Throwable {
//		createInstanceList();
//		setDistictionFrequencyBasedRegularizer(sourceFiles, targetFiles, featureFile, params);
//		createTrainer();
//		model = trainer.train(trainingData, numberOfIterations);
//		writeModel(modelFileName);
//		return model;
//	}
	
	public MaxEnt train() throws Throwable {
		return train(true);
	}
	
	public MaxEnt train(int numOfIteration, int cutoff) throws Throwable {
		if (cutoff > 0)
			this.cutoff = cutoff;
		if (numOfIteration > 0)
			this.numberOfIterations = numOfIteration;
		return train();
	}
	
	
	
//	public void setGaussianPriorVariance(double gaussianPriorVariance) {
//		this.gaussianPriorVariance = gaussianPriorVariance;
//	}
	
	private static void convertToMalletForm(String input, String output, int number1, int number2) throws Throwable {
		if (number1 >= number2) return;
		BufferedReader reader = new BufferedReader(new FileReader(input));
		BufferedWriter writer = new BufferedWriter(new FileWriter(output));
		String line = "", label = "";
		int count = 0;
		
		if (number1 < 0) number1 = 0;
		
		while ((line = reader.readLine()) != null) {
			count++;
			if (count <= number1) continue;
			if (count > number2) break;
			System.out.println("Getting instance # " + count);
			label = line.substring(line.lastIndexOf(" ")+1);
			line = line.substring(0, line.lastIndexOf(" "));
			writer.write("Instance#" + count + "\t" + label + "\t" + line + "\n");
		}
		
		reader.close();
		writer.close();
	}

	public static void main(String[] args) throws Throwable {
		String raw = "/Users/thien/workspace/REJet/Kdd_Thien/re/feature_files/" + 
				"relationClassifier.list1.train_cleanlist2.test.word_parse_dependency_entity_semantic.maxent";
		String input = "/Users/thien/workspace/REJet/Kdd_Thien/re/systemFile/test/features.txt";
		String testing = "/Users/thien/workspace/REJet/Kdd_Thien/re/systemFile/test/testing.txt";
		//String output = "/Users/thien/workspace/REJet/Kdd_Thien/re/systemFile/model.txt";
		
		//convertToMalletForm(raw, input, 0, 300);
		//convertToMalletForm(raw, testing, 600, 1000);
		MMETrainer trainer = new MMETrainer(input);
		trainer.setType(L1);
		trainer.setRegularizer(new ConstantRegularizer(0.5));
		trainer.train(200, 4);
	}
}
