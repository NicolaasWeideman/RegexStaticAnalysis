package regexcompiler;

import java.util.List;

public class RegexGroup extends RegexSubexpression<List<RegexToken>> {

	public enum RegexGroupType {
		NORMAL(""),
		NONCAPTURING("?:"),
		POSLOOKAHEAD("?="),
		POSLOOKBEHIND("?<="),
		NEGLOOKAHEAD("?!"),
		NEGLOOKBEHIND("?<!");
		
		private final String symbol;
		
		RegexGroupType(String symbol) {
			this.symbol = symbol;
		}
		
		public String toString() {
			return symbol;
		}
	}
	
	private final RegexGroupType groupType;
	public RegexGroupType getGroupType() {
		return groupType;
	}
	
	public RegexGroup(List<RegexToken> subexpressionContent, RegexGroupType groupType, int index) {
		super(subexpressionContent, index);
		this.groupType = groupType;
	}

	@Override
	public SubexpressionType getSubexpressionType() {
		return SubexpressionType.GROUP;
	}

	@Override
	public String toString() {
		return '(' + groupType.toString() + getSubexpressionContent().toString() + ')';
	}

}
