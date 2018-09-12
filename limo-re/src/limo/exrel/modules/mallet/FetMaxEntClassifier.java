package limo.exrel.modules.mallet;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.ArrayList;

import limo.exrel.data.Classification;
import limo.exrel.data.ClassificationScores;
import limo.exrel.modules.AbstractModule;
import limo.exrel.modules.mallet.MalletMaxEnt.MMaxEntModel;
import limo.exrel.slots.DoubleSlot;
import limo.exrel.slots.FileSlot;
import limo.exrel.slots.InputDirSlot;
import limo.exrel.slots.OutputFileSlot;

public class FetMaxEntClassifier extends AbstractModule {
	
	public InputDirSlot modelsDir = new InputDirSlot(true);
	public FileSlot examplesIdxFile = new FileSlot(true);
	public OutputFileSlot outScoresIdxFile = new OutputFileSlot(true);
	public OutputFileSlot outPropositionsIdxFile = new OutputFileSlot(true);
	
	public DoubleSlot detectionThreshold = new DoubleSlot(0.7);
	public DoubleSlot classificationThreshold = new DoubleSlot(0.9);
	
	public FetMaxEntClassifier(String moduleId, String configId) {
		super(moduleId,configId);
	}
	
	public MMaxEntModel getDetectorModel() {
		message("Loading MaxEnt detector model ...");
		String dir = modelsDir.get().getAbsolutePath() + File.separator + "detector.model";
		MMaxEntModel tagger = new MMaxEntModel();
		tagger.loadModel(dir);
		return tagger;
	}
	
	public MMaxEntModel getClassifierModel() {
		message("Loading MaxEnt classifier model ...");
		String dir = modelsDir.get().getAbsolutePath() + File.separator + "classifier.model";
		MMaxEntModel tagger = new MMaxEntModel();
		tagger.loadModel(dir);
		return tagger;
	}
	
	@Override
	protected void _run() throws Exception {
		
		double dT = detectionThreshold.get().doubleValue(), cT = classificationThreshold.get().doubleValue();
		
		message("Tagging traning data ...");
		message("Detection Threshold = " + dT + ", Classification Threshold = " + cT);
		
		MMaxEntModel detector = getDetectorModel();
		MMaxEntModel classifier = getClassifierModel();
		
		int numOfClass = classifier.getNumOutcomes();
		String[] classes = new String[numOfClass];
		for (int i = 0; i < numOfClass; i++)
			classes[i] = classifier.getOutcome(i);
		
		BufferedReader reader = new BufferedReader(new FileReader(examplesIdxFile.get()));
		PrintWriter scorePrinter = new PrintWriter(outScoresIdxFile.get());
		PrintWriter predictionPrinter = new PrintWriter(outPropositionsIdxFile.get());
		
		String line = "", features = "", id = "", relationClass = "";
		String[] data = null;
		ClassificationScores scores = null;
		boolean isRelation = false;
		double[] ds = null;
		double detectorScore = 0.0, classifierScore = 0.0;
		while ((line = reader.readLine()) != null) {
			data = line.split("\t", 4);
			features = data[2].substring(data[2].indexOf(" ")+1).trim();
			id = data[0];
			scores = new ClassificationScores();
			
			detectorScore = detector.prob(features, "1");
			
			if (detectorScore >= dT) {
				relationClass = classifier.bestOutcome(features);
				
//				if (relationClass.equals("NONE"))
//					System.out.println("----------------------NONE class in classifier");
				
				ds = classifier.getOutcomeProbabilities(features);
				
				if (ds.length != classes.length) {
					throw new Exception("Mismatch between the number of classes and scores: FetMaxEntClassifier");
				}
				
				for (int i = 0; i < ds.length; i++) {
					scores.add(new Classification(classes[i], ds[i]));
				}
			}
			else {
				
				relationClass = "NONE";
				
				for (int i = 0; i < classes.length; i++) {
					if (classes[i].equals("NONE"))
						scores.add(new Classification(classes[i], 1.0));
					else
						scores.add(new Classification(classes[i], 0.0));
				}
				
			}
			
//			isRelation = detector.bestOutcome(features).equals("1");
//			
//			if (isRelation) {
//				relationClass = classifier.bestOutcome(features);
//				
//				ds = classifier.getOutcomeProbabilities(features);
//				
//				if (ds.length != classes.length) {
//					throw new Exception("Mismatch between the number of classes and scores: FetMaxEntClassifier");
//				}
//				
//				for (int i = 0; i < ds.length; i++) {
//					scores.add(new Classification(classes[i], ds[i]));
//				}
//			}
//			else {
//				//extra effort
//				detectorScore = detector.getBestOutComeScore(features);
//				classifierScore = classifier.getBestOutComeScore(features);
//				if (detectorScore < dT && classifierScore >= cT) {
//					relationClass = classifier.bestOutcome(features);
//					
//					ds = classifier.getOutcomeProbabilities(features);
//					
//					if (ds.length != classes.length) {
//						throw new Exception("Mismatch between the number of classes and scores (Extra Effort): FetMaxEntClassifier");
//					}
//					
//					for (int i = 0; i < ds.length; i++) {
//						scores.add(new Classification(classes[i], ds[i]));
//					}
//				}
//				else {
//					relationClass = "NONE";
//					
//					for (int i = 0; i < classes.length; i++) {
//						if (classes[i].equals("NONE"))
//							scores.add(new Classification(classes[i], 1.0));
//						else
//							scores.add(new Classification(classes[i], 0.0));
//					}
//				}
//				
////				relationClass = "NONE";
////				
////				for (int i = 0; i < classes.length; i++) {
////					if (classes[i].equals("NONE"))
////						scores.add(new Classification(classes[i], 1.0));
////					else
////						scores.add(new Classification(classes[i], 0.0));
////				}
//			}
			
			scorePrinter.println(id + "\t" + scores.toString());
			predictionPrinter.println(id + "\t" + relationClass);
		}
		
		reader.close();
		scorePrinter.close();
		predictionPrinter.close();
	}
}
