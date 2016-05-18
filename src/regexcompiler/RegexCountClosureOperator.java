package regexcompiler;

public class RegexCountClosureOperator extends RegexQuantifiableOperator {
	
	
	private int low;
	public int getLow() {
		return low;
	}
	
	private int high;
	public int getHigh() {
		return high;
	}

	public RegexCountClosureOperator(int low, int high, QuantifierType quantifierType, int index) {
		super(OperatorType.COUNT_CLOSURE, quantifierType, index);
		this.low = low;
		this.high = high;
	}
	
	@Override
	public String toString() {
		return "{" + low + "," + high + "}" + getQuantifierType().toString();
	}

}
