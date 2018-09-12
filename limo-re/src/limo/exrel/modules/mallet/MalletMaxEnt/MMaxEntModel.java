package limo.exrel.modules.mallet.MalletMaxEnt;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import cc.mallet.classify.Classification;
import cc.mallet.pipe.Pipe;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import cc.mallet.types.SparseVector;

//import AceJet.Datum;
import limo.exrel.modules.mallet.MalletMaxEnt.Regularizer.ConstantRegularizer;

public class MMaxEntModel {
	
	String featureFileName;
	String modelFileName;
	BufferedWriter featureWriter = null;
	MaxEnt model = null;
	Pipe pipe = null;
	int cutoff = 4;
	int iterations = 500;
	
	private static long numberOfInstances = 0;
	
	/**
	 *  creates a new maximum entropy model.
	 */

	public MMaxEntModel () {
		numberOfInstances = 0;
	}

	/**
	 *  creates a new maximum entropy model, specifying files for both
	 *  the features and the resulting model.
	 *
	 *  @param featureFileName  the name of the file in which features will be
	 *                          stored during training
	 *  @param modelFileName    the name of the file in which the max ent
	 *                          model will be stored
	 */

	public MMaxEntModel (String featureFileName, String modelFileName) {
		this.featureFileName = featureFileName;
		this.modelFileName = modelFileName;
		numberOfInstances = 0;
	}
	
	public void initializeForTraining (String featureFileName) {
		this.featureFileName = featureFileName;
		initializeForTraining ();
	}
	
	public void initializeForTraining () {
		if (featureFileName == null) {
			System.out.println ("MaxEntModel.initializeForTraining: no featureFileName specified");
		} else {
			try {
				featureWriter = new BufferedWriter (new FileWriter (featureFileName));
			} catch (IOException e) {
				System.out.print("Unable to create feature file: ");
				System.out.println(e);
			}
		}
	}
	
	/**
	 *  invoked during training to add one training Datum <CODE>d</CODE> to the
	 *  training set.
	 */

//	public void addEvent (Datum d) {
//		if (featureWriter == null)
//			initializeForTraining ();
//		if (d == null) return;
//		try {
//			numberOfInstances++;
//			featureWriter.write(datum2MString(d, numberOfInstances) + "\n");
//		}
//		catch (IOException ex) {
//			System.out.print("Unable to write to feature file: ");
//			System.out.println(ex);
//		}
//	}
//	
//	private String datum2MString(Datum d, long instanceId) {
//		if (d == null) return null;
//		String ins = d.toString();
//		return instanceId + "\t" + ins.substring(ins.lastIndexOf(" ")+1) + "\t" + ins.substring(0, ins.lastIndexOf(" "));
//	}
//	
//	private String datum2Predicates(Datum d) {
//		if (d == null) return null;
//		String ins = d.toString();
//		return ins.substring(0, ins.lastIndexOf(" "));
//	}
	
	/**
	 *  sets the feature cutoff.  Features occurring fewer than <CODE>cutoff</CODE>
	 *  times in the training set are ignored.  Default value is 4.
	 */

	public void setCutoff (int cutoff) {
		if (cutoff >= 0)
			this.cutoff = cutoff;
	}

	public void setIterations (int iterations) {
		if (iterations > 0)
			this.iterations = iterations;
	}
	
	public void buildModel() {
		buildModel(1.0, MMETrainer.L2, true, 1.0);
	}
	
	public void buildModelL1() {
		buildModel(1.0, MMETrainer.L1, true, 1.0);
	}
	
	public void buildModel(double reg) {
		buildModel(reg, MMETrainer.L2, true, 1.0);
	}
	
	public void buildModelL1(double reg) {
		buildModel(reg, MMETrainer.L1, true, 1.0);
	}
	
	public void buildModel (double reg, int type, boolean binary, double binaryValue) {
//		boolean USE_SMOOTHING = false;
//		double SMOOTHING_OBSERVATION = 0.1;
//		boolean PRINT_MESSAGES = true;
		try {
			if (featureWriter != null)
				featureWriter.close();
			//MMETrainer trainer = new MMETrainer(featureFileName);
			MMETrainer trainer = new MMETrainer(featureFileName, type, binary, binaryValue, null);
			//trainer.setType(type);
			reg = (reg < 0.0) ? 1.0 : reg;
			trainer.setRegularizer(new ConstantRegularizer(reg));
			trainer.setCutoff(this.cutoff);
			trainer.setNumberOfIterations(this.iterations);
			//trainer.setBinary(binary);
			//trainer.setBinaryValue(binaryValue);
			model = trainer.train(false);
			pipe = model.getInstancePipe();
			
//			FileReader datafr = new FileReader(new File(featureFileName));
//			EventStream es =
//				new BasicEventStream(new PlainTextByLineDataStream(datafr));
//			GIS.SMOOTHING_OBSERVATION = SMOOTHING_OBSERVATION;
//			model = GIS.trainModel(es, iterations, cutoff, USE_SMOOTHING, PRINT_MESSAGES);
		} catch (Throwable e) {
			System.out.print("Unable to create model due to exception: ");
			System.out.println(e);
		}
	}
	
	public void saveModel () {
		if (modelFileName == null) {
			System.out.println ("MMaxEntModel.saveModel:  no modelFileName specified");
		} else {
			saveModel (modelFileName);
		}
	}
	
	public void saveModel (String modelFileName) {
		try {
			ObjectOutputStream objOut = new ObjectOutputStream(new FileOutputStream(new File(modelFileName)));
			objOut.writeObject(model);
			objOut.close();
		} catch (IOException e) {
			System.out.print("Unable to save model: ");
			System.out.println(e);
		}
	}
	
	public void saveModel (ObjectOutputStream objOut) {
		try {
			objOut.writeObject(model);
		} catch (IOException e) {
			System.out.print("Unable to save model: ");
			System.out.println(e);
		}
	}
	
	public void loadModel () {
		if (modelFileName == null) {
			System.out.println ("MaxEntModel.loadModel:  no modelFileName specified");
		} else {
			loadModel (modelFileName);
		}
	}

	public void loadModel (String modelFileName) {
		try {
			ObjectInputStream objIn = new ObjectInputStream(new FileInputStream(new File(modelFileName)));
			model = (MaxEnt) objIn.readObject();
			pipe = model.getInstancePipe();
			pipe.getDataAlphabet().stopGrowth();
			pipe.getTargetAlphabet().stopGrowth();
			objIn.close();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}
	}

	public void loadModel (ObjectInputStream objIn) {
		try {
			model = (MaxEnt) objIn.readObject();
			pipe = model.getInstancePipe();
			pipe.getDataAlphabet().stopGrowth();
			pipe.getTargetAlphabet().stopGrowth();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}
	}
	
	public boolean isLoaded () {
		return model != null && pipe != null;
	}
	
	/**
	 *  (for a trained model) returns the probability that the Datum
	 *  <CODE>d</CODE> is classified as <CODE>value</CODE>.
	 */

//	public double prob (Datum d, String value) {
//		if (d == null || value == null) return -1.0;
//		return prob(datum2Predicates(d), value);
//	}
	
	public double prob (String predicates, String value) {
		if (predicates == null || value == null) return -1.0;
		return ((SparseVector) classify(predicates).getLabeling()).value(pipe.getTargetAlphabet().lookupIndex(value, false));
	}
	
	/**
	 *  (for a trained model) returns the most likely outcome for Datum
	 *  <CODE>d</CODE>.
	 */

//	public String bestOutcome (Datum d) {
//		if (d == null) return null;
//		return bestOutcome(datum2Predicates(d));
//	}
	
	public String bestOutcome(String predicates) {
		return (String) classify(predicates).getLabeling().getBestLabel().getEntry();
	}

	public int getNumOutcomes () {
		return pipe.getTargetAlphabet().size();
	}

	public String getOutcome (int i) {
		if (i < 0) return null;
		return (String)pipe.getTargetAlphabet().lookupObject(i);
	}

//	public double[] getOutcomeProbabilities (Datum d) {
//		if (d == null) return null;
//		return getOutcomeProbabilities(datum2Predicates(d));
//	}
	
	public double[] getOutcomeProbabilities (String predicates) {
		if (predicates == null) return null;
		return ((SparseVector) classify(predicates).getLabeling()).getValues();
	}
	
	private Classification classify (Instance instance) {
		if (model == null) return null;
		return model.classify(instance);
	}
	
	private Classification classify(String predicates) {
		if (predicates == null) return null;
		String dump = (String) pipe.getTargetAlphabet().toArray()[0];
		
		Instance carrier = new Instance(predicates, dump, null, predicates);
		
		InstanceList ilist = new InstanceList(pipe);
		ilist.addThruPipe(carrier);
		
		return classify(ilist.get(0));
	}
	
	public double getBestOutComeScore (String predicates) {
		return classify(predicates).getLabeling().getBestValue();
	}
	
}
