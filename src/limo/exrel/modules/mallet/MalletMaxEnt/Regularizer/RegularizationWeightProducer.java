package limo.exrel.modules.mallet.MalletMaxEnt.Regularizer;

public interface RegularizationWeightProducer {
	
	public double getWeights(int featureIndex, int labelIndex);

}
