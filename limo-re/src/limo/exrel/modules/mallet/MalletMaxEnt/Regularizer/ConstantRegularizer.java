package limo.exrel.modules.mallet.MalletMaxEnt.Regularizer;


public class ConstantRegularizer implements RegularizationWeightProducer {
	
	public double constant = 1.0;
	
	public ConstantRegularizer(double constant) {
		this.constant = constant;
	}
	
	public double getConstant() {
		return constant;
	}
	
	public void setConstant(double constant) {
		this.constant = constant;
	}
	
	public double getWeights(int featureIndex, int labelIndex) {
		return constant;
	}

}
