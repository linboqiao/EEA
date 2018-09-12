package limo.exrel.slots;

public class DoubleSlot extends Slot<Double> {
	
	public DoubleSlot(double defValue) {
		super();
		set(defValue);
	}
	
	@Override
	protected Double evaluate() {
		return Double.parseDouble(toString());
	}

	@Override
	protected void addDefaultChecks() {}
}
