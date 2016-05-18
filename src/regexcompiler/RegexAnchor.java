package regexcompiler;

public class RegexAnchor implements RegexToken {
	
	public enum RegexAnchorType {
		LINESTART("^"),
		LINEEND("$");
		
		private final String symbol;
		
		RegexAnchorType(String symbol) {
			this.symbol = symbol;
		}
		
		public String toString() {
			return symbol;
		}
	}
	
	private final RegexAnchorType anchorType;
	public RegexAnchorType getAnchorType() {
		return anchorType;
	}
	
	private final int index;
	@Override
	public int getIndex() {
		return index;
	}

	public RegexAnchor(RegexAnchorType anchorType, int index) {
		this.index = index;
		this.anchorType = anchorType;
	}

	@Override
	public TokenType getTokenType() {
		return TokenType.ANCHOR;
	}

}
