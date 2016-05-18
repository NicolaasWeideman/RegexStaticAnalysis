package regexcompiler;

public abstract class RegexOperator implements RegexToken {
	
	public enum OperatorType {
		STAR("*", false),
		PLUS("+", false),
		COUNT_CLOSURE("{...}", false),
		QUESTION_MARK("?", false),
		UNION("|", true),
		JOIN("○", true);
		
		private final String symbol;
		
		private final boolean isBinaryOperator;
		public boolean getIsBinaryOperator() {
			return isBinaryOperator;
		}
		
		OperatorType(String symbol, boolean isBinaryOperator) {
			this.symbol = symbol;
			this.isBinaryOperator = isBinaryOperator;
		}
		

		@Override
		public String toString() {
			return symbol;
		}
	}

	@Override
	public TokenType getTokenType() {
		return TokenType.OPERATOR;
	}
	
	private final OperatorType operatorType;
	public OperatorType getOperatorType() {
		return operatorType;
	}
	
	public boolean getIsBinaryOperator() {
		return operatorType.getIsBinaryOperator();
	}
	
	private final int index;
	@Override
	public int getIndex() {
		return index;
	}
	
	public RegexOperator(OperatorType operatorType, int index) {
		this.operatorType = operatorType;
		this.index = index;
	}
	
	@Override
	public String toString() {
		return operatorType.toString();
	}

	public static class RegexUnionOperator extends RegexOperator {

		public RegexUnionOperator(int index) {
			super(OperatorType.UNION, index);
		}
		
	}
	
	public static class RegexJoinOperator extends RegexOperator {

		public RegexJoinOperator(int index) {
			super(OperatorType.JOIN, index);
		}
		
	}
}
