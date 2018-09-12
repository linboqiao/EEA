package limo.exrel.modules.mallet.MalletMaxEnt;

import cc.mallet.types.Labeling;

	public interface GeneralMaxEntClassifier {
	
	public String eval (String predicates);
	public String eval (String predicates, boolean real);
	public double getBestOutComeScore (String predicates);
	public double getBestOutComeScore (String predicates, boolean real);
	public double[] getOutComeProbabilities(String predicates);
	public String getAllPredications (String predicates, boolean real);
	public abstract String allOutComeText(String predicates);
	
	public double getAccuracy (String testingFile) throws Throwable;
	public double getPrecision (String testingFile, int index) throws Throwable;
	public double getPrecision (String testingFile, Labeling labeling) throws Throwable;
	public double getPrecision (String testingFile, Object labelEntry) throws Throwable;
	public double getRecall (String testingFile, int index) throws Throwable;
	public double getRecall (String testingFile, Labeling labeling) throws Throwable;
	public double getRecall (String testingFile, Object labelEntry) throws Throwable;
	public double getF1 (String testingFile, int index) throws Throwable;
	public double getF1 (String testingFile, Labeling labeling) throws Throwable;
	public double getF1 (String testingFile, Object labelEntry) throws Throwable;
	public double getAverageRank (String testingFile) throws Throwable;
}
