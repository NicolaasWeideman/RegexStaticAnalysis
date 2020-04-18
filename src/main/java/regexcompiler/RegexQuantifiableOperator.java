package regexcompiler;

public class RegexQuantifiableOperator extends RegexOperator {

	public enum QuantifierType {
		GREEDY(""),
		RELUCTANT("?"),
		POSSESSIVE("+");
		
		private final String symbol;
		
		QuantifierType(String symbol) {
			this.symbol = symbol;
		}
		
		@Override
		public String toString() {
			return symbol;
		}
	}
	
	private final QuantifierType quantifierType;
	public QuantifierType getQuantifierType() {
		return quantifierType;
	}
	
	public RegexQuantifiableOperator(OperatorType operatorType, QuantifierType quantifierType, int index) {
		super(operatorType, index);
		this.quantifierType = quantifierType;
	}
	
	public String toString() {
		String operator = getOperatorType().toString();
		String quantifier = getQuantifierType().toString();
		
		return operator + quantifier;
	}
	
	public static class RegexStarOperator extends RegexQuantifiableOperator {

		public RegexStarOperator(QuantifierType quantifierType, int index) {
			super(OperatorType.STAR, quantifierType, index);
		}
		
	}
	
	public static class RegexPlusOperator extends RegexQuantifiableOperator {

		public RegexPlusOperator(QuantifierType quantifierType, int index) {
			super(OperatorType.PLUS, quantifierType, index);
		}
		
	}
	
	public static class RegexQuestionMarkOperator extends RegexQuantifiableOperator {

		public RegexQuestionMarkOperator(QuantifierType quantifierType, int index) {
			super(OperatorType.QUESTION_MARK, quantifierType, index);
		}
		
	}
	

}
