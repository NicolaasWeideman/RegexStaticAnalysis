package regexcompiler;

public class RegexEscapedSymbol extends RegexSubexpression<String> {
	
	public enum RegexEscapedSymbolType {
		CHARACTER,
		OCTAL,
		UNICODE,
		HEX,
		CHARACTER_PROPERTY
	}

	@Override
	public SubexpressionType getSubexpressionType() {
		return SubexpressionType.ESCAPED_SYMBOL;
	}	
	
	private final RegexEscapedSymbolType escapedSymbolType;
	public RegexEscapedSymbolType getRegexEscapedSymbolType() {
		return escapedSymbolType;
	}
	
	public RegexEscapedSymbol(String escapedContent, RegexEscapedSymbolType escapedSymbolType, int index) {
		super(escapedContent, index);
		this.escapedSymbolType = escapedSymbolType;
	}
	
	@Override
	public String toString() {
		switch (escapedSymbolType) {
		case CHARACTER:
			return "\\" + getSubexpressionContent();
		case OCTAL:
			return "\\0" + getSubexpressionContent();
		case UNICODE:
			return "\\u" + getSubexpressionContent();
		case HEX:
			return "\\x{" + getSubexpressionContent() + "}";
		case CHARACTER_PROPERTY:
			return "\\p{" + getSubexpressionContent() + "}";
		default:
			throw new RuntimeException("Unkown RegexEscapedSymbolType");
			
		}
	}


}
