package regexcompiler;

public abstract class RegexSubexpression<SubexpressionContentType> implements RegexToken {
	
	public enum SubexpressionType {
		SYMBOL,
		ESCAPED_SYMBOL,
		CHARACTER_CLASS,
		GROUP,
	}

	@Override
	public TokenType getTokenType() {
		return TokenType.SUBEXPRESSION;
	}
	
	public abstract SubexpressionType getSubexpressionType();
	
	private final SubexpressionContentType subexpressionContent;
	public SubexpressionContentType getSubexpressionContent() {
		return subexpressionContent;
	}
	
	private final int index;
	@Override
	public int getIndex() {
		return index;
	}
	
	public RegexSubexpression(SubexpressionContentType subexpressionContent, int index) {
		this.subexpressionContent = subexpressionContent;
		this.index = index;
	}

}
