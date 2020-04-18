package regexcompiler;

public class RegexCharacterClass extends RegexSubexpression<String> {

	public RegexCharacterClass(String subexpressionContent, int index) {
		super(subexpressionContent, index);
	}

	@Override
	public SubexpressionType getSubexpressionType() {
		return SubexpressionType.CHARACTER_CLASS;
	}
	
	@Override
	public String toString() {
		return '[' + getSubexpressionContent() + ']';
	}

}
