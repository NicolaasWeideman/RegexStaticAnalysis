package regexcompiler;

public class RegexSymbol extends RegexSubexpression<String> {
	
	public RegexSymbol(String subexpressionContent, int index) {
		super(subexpressionContent, index);
	}

	@Override
	public SubexpressionType getSubexpressionType() {
		return SubexpressionType.SYMBOL;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (char c : getSubexpressionContent().toCharArray()) {
			if ('!' <= c && c <= '~') {
				sb.append(c);
			} else {
				sb.append(String.format("\\x{%02x}", (int) c));
			}
		}
		
		return sb.toString();
	}

}
