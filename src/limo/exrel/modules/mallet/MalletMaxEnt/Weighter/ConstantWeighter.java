package limo.exrel.modules.mallet.MalletMaxEnt.Weighter;

import cc.mallet.types.Instance;

public class ConstantWeighter implements InstanceWeightProducer {

	double constant = 1.0;
	
	public ConstantWeighter(double constant) {
		this.constant = constant;
	}
	
	public double getConstant() {
		return constant;
	}
	
	public void setConstant(double constant) {
		this.constant = constant;
	}
	
	public double getWeight(Instance carrier) {
		return constant;
	}
	
	public double getWeight(String predicates) {
		return constant;
	}
}
