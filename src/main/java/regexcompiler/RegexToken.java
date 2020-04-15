package regexcompiler;

public interface RegexToken {
	public enum TokenType {
		SUBEXPRESSION,
		OPERATOR, 
		ANCHOR;
	}
	
	public TokenType getTokenType();
	
	public int getIndex();
}
