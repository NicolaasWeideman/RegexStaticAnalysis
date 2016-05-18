package preprocessor;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import preprocessor.ParsingPreprocessor.CountClosureOperator.BoundsType;
import preprocessor.ParsingPreprocessor.EscapeFactor.EscapeType;

public abstract class ParsingPreprocessor implements Preprocessor {
	
	protected static boolean ALLOW_LOOKAROUND = false;
	protected static boolean ALLOW_LINE_BOUNDARY = false;
	protected static boolean ALLOW_ZERO_ONCE = false;
	
	private final static int MAX_REPETITION = 256;
	
	private final int MAX_REGEX_LENGTH = 1<<23;

	private LinkedList<PreprocessorRule> rules;
	
	@Override
	public String applyRules(String regex) {
		for (PreprocessorRule rule : rules) {
			List<RegexToken> tokenStream = tokenize(regex, 0);
			
			regex = rule.process(tokenStream);
			if (regex.length() > MAX_REGEX_LENGTH) {
				throw new RegexException("Regular expression length exceeded.");
			}
		}
		return regex;
	}

	private static List<RegexToken> tokenize(String regex, int currentLevel) {
		List<RegexToken> tokenStream = new LinkedList<RegexToken>();

		int i;
		char regexArr[] = regex.toCharArray();
		StringBuilder groupBuilder;
		int level;
		i = 0;
		boolean escaped = false;
		while (i < regexArr.length) {
			switch (regexArr[i]) {
			case '(':
				GroupFactor.GroupType groupType = GroupFactor.GroupType.NORMAL;

				if (i < regex.length() - 2 && regexArr[i + 1] == '?') {
					if (regexArr[i + 2] == '<') {
						checkAllowedFunctionality(ALLOW_LOOKAROUND, "Lookaround symbol");
						
						if (i < regex.length() - 3) {
							
							/* check for look behind */
							switch (regexArr[i + 3]) {
							case '=':
								groupType = GroupFactor.GroupType.POSLOOKBEHIND;
								break;
							case '!':
								
								groupType = GroupFactor.GroupType.NEGLOOKBEHIND;
								break;
							default:
								throw new PatternSyntaxException("Unkown look-behind group", regex, i);

							}
						} else {
							throw new PatternSyntaxException("Unkown look-behind group", regex, i);
						}
						i += 4;
					} else {
						/*
						 * check for noncapturing group (?: and look ahead (?=
						 * and negative look ahead (?!
						 */
						switch (regexArr[i + 2]) {
						case ':':
							groupType = GroupFactor.GroupType.NONCAPTURING;
							break;
						case '=':
							checkAllowedFunctionality(ALLOW_LOOKAROUND, "Positive lookahead");
							groupType = GroupFactor.GroupType.POSLOOKAHEAD;
							break;
						case '!':
							checkAllowedFunctionality(ALLOW_LOOKAROUND, "Negative lookahead");
							groupType = GroupFactor.GroupType.NEGLOOKAHEAD;
							break;
						default:
							throw new PatternSyntaxException("Unkown inline modifier", regex, i);

						}
						i += 3;
					}
				} else {
					i++;
				}

				groupBuilder = new StringBuilder();
				level = 1;
				do {
					if (i >= regexArr.length) {
						throw new PatternSyntaxException("Unmatched '('", regex, i);
					}

					if (regexArr[i] == '\\') {
						/* skipping the escaped character */
						groupBuilder.append(regexArr[i]);
						i++;

						escaped = true;
					} else if (regexArr[i] == '[') {
						/* ignore ( and ) in character classes */
						while (i < regexArr.length && regexArr[i] != ']') {
							groupBuilder.append(regexArr[i]);
							i++;
						}
					}

					/* increasing and decreasing levels */
					if (!escaped && regexArr[i] == '(') {
						if (level != 0) {
							groupBuilder.append("(");
						}
						level++;
					} else if (!escaped && regexArr[i] == ')') {
						level--;
						if (level != 0) {
							groupBuilder.append(")");
						}
					} else {
						groupBuilder.append(regexArr[i]);
					}

					escaped = false;
					i++;
				} while (level != 0);
				List<RegexToken> groupTokens = tokenize(groupBuilder.toString(), currentLevel + 1);
				RegexToken groupFactor = new GroupFactor(groupTokens, groupType);
				tokenStream.add(groupFactor);
				break;
			case '[':
				groupBuilder = new StringBuilder();
				level = 1;
				i++;

				while (level != 0) {
					if (i >= regexArr.length) {
						throw new PatternSyntaxException("Unclosed character class", regex, i);
					}
					if (regexArr[i] == '\\') {
						/* skipping the escaped character */
						groupBuilder.append(regexArr[i]);
						i++;
						escaped = true;
					}

					if (!escaped && regexArr[i] == '[') {
						level++;
					} else if (!escaped && regexArr[i] == ']') {
						level--;
					}

					if (level != 0) {
						groupBuilder.append(regexArr[i]);
					}

					escaped = false;
					i++;
				}
				String symbols = groupBuilder.toString();
				RegexToken characterClassFactor = new CharacterClassFactor(symbols);
				tokenStream.add(characterClassFactor);
				break;
			case '\\':
				i++;
				if (i < regexArr.length && regexArr[i] == 'Q') {
					i++;
					while (true) {
						if (i < regexArr.length) {
							if (regexArr[i] == '\\') {
								i++;
								if (i < regexArr.length && regexArr[i] == 'E') {
									
									break;
								} else if (i < regexArr.length) {
									
									String currentSymbol = "" + regexArr[i];
									RegexToken verbatimChar;
									
									if (canEscapeVerbatim(currentSymbol)) {
										
										verbatimChar = new EscapeFactor("\\" + currentSymbol, EscapeType.VERBATIM);
									} else {
										verbatimChar = new SingleFactor(currentSymbol);
									}
									tokenStream.add(verbatimChar);
								}
							} else {
								String currentSymbol = "" + regexArr[i];
								RegexToken verbatimChar;
								if (canEscapeVerbatim(currentSymbol)) {
									verbatimChar = new EscapeFactor("\\" + currentSymbol, EscapeType.VERBATIM);
								} else {
									verbatimChar = new SingleFactor(currentSymbol);

								}
								tokenStream.add(verbatimChar);
							}
						} else {
							break;
						}
						i++;

					}
					i++;
				} else if (i < regexArr.length && regexArr[i] == 'x') {
					
					groupBuilder = new StringBuilder("\\x");
					i++;
					if (i < regexArr.length && regexArr[i] == '{') {
						while (i < regexArr.length && regexArr[i] != '}') {
							groupBuilder.append(regexArr[i]);
							i++;
						}
						groupBuilder.append("}");
						i++;
					} else if (i < regexArr.length) {
						groupBuilder.append(regexArr[i]);
						i++;
						if (i < regexArr.length) {
							groupBuilder.append(regexArr[i]);
							i++;
						} else {
							throw new PatternSyntaxException("Illegal hexadecimal escape sequence", regex, i);
						}
					} else {
						throw new PatternSyntaxException("Illegal hexadecimal escape sequence", regex, i);
					}

					String escapeSequence = groupBuilder.toString();
					RegexToken escapeFactor = new EscapeFactor(escapeSequence, EscapeType.HEX);
					tokenStream.add(escapeFactor);
				} else if (i < regexArr.length && regexArr[i] == '0') {
					groupBuilder = new StringBuilder("\\0");
					String hexNumberStr = "";
					/*
					 * Read octal symbols until larger than allowed max up to a
					 * maximum of three characters
					 */
					int tmpNum = 0;
					int octalDigitCounter = 0;
					i++;
					while (i < regexArr.length && tmpNum < 0377 && ('0' <= regexArr[i] && regexArr[i] <= '7') && octalDigitCounter < 3) {
						hexNumberStr += regexArr[i];
						groupBuilder.append(regexArr[i]);
						tmpNum = Integer.parseInt(hexNumberStr, 8);
						i++;
						octalDigitCounter++;
					}

					String escapeSequence = groupBuilder.toString();
					RegexToken escapeFactor = new EscapeFactor(escapeSequence, EscapeType.OCTAL);
					tokenStream.add(escapeFactor);
				} else if (i < regexArr.length && regexArr[i] == 'u') {
					groupBuilder = new StringBuilder("\\u");
					i++;
					if (i < regexArr.length - 3) {
						for (int j = 0; j < 4; j++) {
							groupBuilder.append(regexArr[i]);
							i++;
						}

					} else {
						throw new PatternSyntaxException("Illegal unicode escape sequence", regex, i);
					}

					String escapeSequence = groupBuilder.toString();
					RegexToken escapeFactor = new EscapeFactor(escapeSequence, EscapeType.UNICODE);
					tokenStream.add(escapeFactor);
					throw new PatternSyntaxException("Illegal/unsupported escape sequence", regex, i);
				} else if (i < regexArr.length && regexArr[i] == 'p') {
					groupBuilder = new StringBuilder("\\p");
					i++;
					if (regexArr[i] == '{') {
						while (regexArr[i] != '}') {
							groupBuilder.append(regexArr[i]);
							i++;
							if (i >= regexArr.length) {
								throw new PatternSyntaxException("Unclosed character family near index", regex, i);
							}
						}
						groupBuilder.append('}');
						i++;
					} else {
						groupBuilder.append(regexArr[i]);
						i++;
					}

					String escapeSequence = groupBuilder.toString();
					RegexToken escapeFactor = new EscapeFactor(escapeSequence, EscapeType.CHARACTER_PROPERTY);
					tokenStream.add(escapeFactor);
				} else if (i < regexArr.length) {
					String escapedSequence = "\\" + regexArr[i];
					RegexToken escapeFactor = new EscapeFactor(escapedSequence, EscapeType.CHARACTER);
					tokenStream.add(escapeFactor);
					i++;
				} else {
					String escapedSequence = "\\";
					RegexToken escapeFactor = new EscapeFactor(escapedSequence, EscapeType.CHARACTER);
					tokenStream.add(escapeFactor);
					i++;
				}
				break;
			case '{':
				String operatorSymbol;
				RegexToken operatorToken;
				QuantifiableOperator.Quantifier quantifier = QuantifiableOperator.Quantifier.GREEDY;
				StringBuilder countedClosureBuilder = new StringBuilder("{");
				while (regexArr[i] != '}') {

					i++;
					if (i >= regexArr.length) {
						throw new PatternSyntaxException("Unclosed counted closure", regex, i);
					}
					countedClosureBuilder.append(regexArr[i]);
				}
				i++;
				if (i < regexArr.length) {
					switch (regexArr[i]) {
					case '?':
						quantifier = QuantifiableOperator.Quantifier.RELUCTANT;
						i++;
						break;
					case '+':
						quantifier = QuantifiableOperator.Quantifier.POSSESIVE;
						i++;
						break;
					}
					
				}

				operatorSymbol = countedClosureBuilder.toString();
				Pattern boundedPattern = Pattern.compile("\\{(\\d+),(\\d+)\\}");
				Pattern unboundedPattern = Pattern.compile("\\{(\\d+),\\}");
				Pattern constantRepititionPattern = Pattern.compile("\\{(\\d+)\\}");
				
				Matcher boundedMatcher = boundedPattern.matcher(operatorSymbol);
				Matcher unboundedMatcher = unboundedPattern.matcher(operatorSymbol);
				Matcher constantRepititionMatcher = constantRepititionPattern.matcher(operatorSymbol);
				int low, high;
				if (boundedMatcher.find()) {
					String lowStr = boundedMatcher.group(1);
					low = Integer.parseInt(lowStr);
					String highStr = boundedMatcher.group(2);
					high = Integer.parseInt(highStr);
					
					if (high < low || low < 0 || high > MAX_REPETITION) {
						throw new PatternSyntaxException("Illegal repetition range", regex, i);
					}
					
					operatorToken = new CountClosureOperator(operatorSymbol, quantifier, low, high);
					tokenStream.add(operatorToken);
					
				} else if (unboundedMatcher.find()) {
					String lowStr = unboundedMatcher.group(1);
					low = Integer.parseInt(lowStr);					
					
					if (low < 0 || low > MAX_REPETITION) {
						throw new PatternSyntaxException("Illegal repetition range", regex, i);
					}
					operatorToken = new CountClosureOperator(operatorSymbol, quantifier, low, BoundsType.UNBOUNDED);
					tokenStream.add(operatorToken);
					
				} else if (constantRepititionMatcher.find()) {
					String lowStr = constantRepititionMatcher.group(1);
					low = Integer.parseInt(lowStr);					
					
					if (low < 0 || low > MAX_REPETITION) {
						throw new PatternSyntaxException("Illegal repetition range", regex, i);
					}
					operatorToken = new CountClosureOperator(operatorSymbol, quantifier, low, BoundsType.CONSTANT_REPETITION);
					tokenStream.add(operatorToken);
				} else {
					throw new PatternSyntaxException("Illegal repetition range", regex, i);
				}
				
				
				break;
			case '*':
				i++;
				quantifier = QuantifiableOperator.Quantifier.GREEDY;
				if (i < regexArr.length) {
					switch (regexArr[i]) {
					case '?':
						quantifier = QuantifiableOperator.Quantifier.RELUCTANT;
						i++;
						break;
					case '+':
						quantifier = QuantifiableOperator.Quantifier.POSSESIVE;
						i++;
						break;
					}
					
				}
				
				operatorToken = new QuantifiableOperator("*", RegexOperator.OperatorType.STAR, quantifier);
				tokenStream.add(operatorToken);
				break;
			case '+':
				i++;
				quantifier = QuantifiableOperator.Quantifier.GREEDY;
				if (i < regexArr.length) {
					switch (regexArr[i]) {
					case '?':
						quantifier = QuantifiableOperator.Quantifier.RELUCTANT;
						i++;
						break;
					case '+':
						quantifier = QuantifiableOperator.Quantifier.POSSESIVE;
						i++;
						break;
					}
					
				}
				
				operatorToken = new QuantifiableOperator("+", RegexOperator.OperatorType.PLUS, quantifier);
				tokenStream.add(operatorToken);
				break;
			case '?':
				i++;
				quantifier = QuantifiableOperator.Quantifier.GREEDY;
				if (i < regexArr.length) {
					switch (regexArr[i]) {
					case '?':
						quantifier = QuantifiableOperator.Quantifier.RELUCTANT;
						i++;
						break;
					case '+':
						quantifier = QuantifiableOperator.Quantifier.POSSESIVE;
						i++;
						break;
					}
					
				}
				checkAllowedFunctionality(ALLOW_ZERO_ONCE, "? operator");
				operatorToken = new QuantifiableOperator("?", RegexOperator.OperatorType.QM, quantifier);
				tokenStream.add(operatorToken);
				break;
			case '|':
				i++;
				operatorToken = new RegexOperator("|", RegexOperator.OperatorType.OR);
				tokenStream.add(operatorToken);
				break;
			case '.':
				i++;
				RegexToken wildCardToken = new WildCardFactor();
				tokenStream.add(wildCardToken);
				break;
			case '^':
				if (i == 0 && currentLevel == 0) {
					i++; /* We ignore line boundaries at the start */
				} else {
					checkAllowedFunctionality(ALLOW_LINE_BOUNDARY, "Line Boundary ^");
				}
				break;
			case '$':
				if (i == regexArr.length - 1 && currentLevel == 0) {
					i++; /* We ignore line boundaries at the end */
				} else {
					checkAllowedFunctionality(ALLOW_LINE_BOUNDARY, "Line Boundary $");
				}
				break;
			default:
				String escapedSequence = "" + regexArr[i];
				RegexToken escapeFactor = new SingleFactor(escapedSequence);
				tokenStream.add(escapeFactor);
				i++;
			}
		}
		
		return tokenStream;
	}

	protected void addRule(PreprocessorRule rule) {
		rules.add(rule);
	}

	public ParsingPreprocessor() {
		rules = new LinkedList<PreprocessorRule>();

	}

	private static boolean canEscapeVerbatim(String symbol) {
		/*
		 * we can escape any characters, except letters, to get their verbatim
		 * symbol
		 */
		return !symbol.matches("[a-zA-Z]");
	}
	
	private static void checkAllowedFunctionality(boolean isHandled, String message) {
		if (!isHandled) {
			throw new RuntimeException("Unhandled Functionality: " + message);
		}
	}
	
	static interface RegexToken {

		public enum TokenType {
			REGEX_FACTOR, REGEX_OPERATOR
		}

		public TokenType getTokenType();
		
		public String getRepresentation();

	}

	static abstract class RegexFactor<FactorContentType> implements RegexToken {
		public enum FactorType {
			CHARACTER_CLASS, SINGLE_CHARACTER, ESCAPED_CHARACTER, GROUP, WILD_CARD
		}

		protected FactorContentType factorContent;

		public FactorContentType getFactorContent() {
			return factorContent;
		}

		public RegexFactor(FactorContentType factorContent) {
			this.factorContent = factorContent;
		}

		@Override
		public TokenType getTokenType() {
			return TokenType.REGEX_FACTOR;
		}

		public abstract FactorType getFactorType();

	}

	static class RegexOperator implements RegexToken {

		protected final String operatorSequence;

		public String getOperator() {
			return operatorSequence;
		}

		public RegexOperator(String operatorSequence, OperatorType operatorType) {
			this.operatorSequence = operatorSequence;
			this.operatorType = operatorType;
		}

		public boolean getIsQuantifiable() {
			return false;
		}

		public enum OperatorType {
			PLUS, STAR, QM, COUNT, OR
		}

		private final OperatorType operatorType;
		public OperatorType getOperatorType() {
			return operatorType;
		}
		
		@Override
		public TokenType getTokenType() {
			return TokenType.REGEX_OPERATOR;
		}
		
		@Override
		public String toString() {
			return "O( " + operatorSequence + " )";
		}

		@Override
		public String getRepresentation() {
			return operatorSequence;
		}
		
		@Override
		public boolean equals(Object o) {
			if (o == null) {
				return false;
			}
			
			if (o == this) {
				return true;
			}
			
			if (!(o instanceof RegexOperator)) {
				return false;
			}
			
			RegexOperator ro = (RegexOperator) o;
			return ro.operatorSequence.equals(operatorSequence);
		}

		@Override
		public int hashCode() {
			return operatorSequence.hashCode();
		}
	}

	static class QuantifiableOperator extends RegexOperator {

		public enum Quantifier {
			GREEDY, POSSESIVE, RELUCTANT
		}

		private Quantifier quantifier;
		public Quantifier getOperatorQuantifier() {
			return quantifier;
		}

		public QuantifiableOperator(String operatorSequence, OperatorType operatorType, Quantifier quantifier) {
			super(operatorSequence, operatorType);
			
			this.quantifier = quantifier;
		}
		
		public QuantifiableOperator(String operatorSequence, OperatorType operatorType) {
			super(operatorSequence, operatorType);
			
			this.quantifier = Quantifier.GREEDY;
		}
		
		@Override
		public boolean getIsQuantifiable() {
			return true;
		}

		public void setOperatorQuantifier(Quantifier quantifier) {
			this.quantifier = quantifier;
		}
		
		@Override
		public String toString() {
			String q = "";
			switch (quantifier) {
			case GREEDY:
				q = "G";
				break;
			case POSSESIVE:
				q = "P";
				break;
			case RELUCTANT:
				q = "R";
				break;
			}
			return q + "QO( " + operatorSequence + " )";
		}

	}
	
	static class CountClosureOperator extends QuantifiableOperator {
		
		public enum BoundsType {
			BOUNDED, UNBOUNDED, CONSTANT_REPETITION
		}

		private final int low;
		public int getLow() {
			return low;
		}
		
		private final int high;
		public int getHigh() {
			return high;
		}
		
		private final BoundsType boundsType;
		public BoundsType getBoundsType() {
			return boundsType;
		}
		
		public CountClosureOperator(String operatorSequence, Quantifier quantifier, int low, int high) {
			super(operatorSequence, OperatorType.COUNT, quantifier);
			this.low = low;
			this.high = high;
			
			this.boundsType = BoundsType.BOUNDED;
		}
		
		public CountClosureOperator(String operatorSequence, Quantifier quantifier, int low, BoundsType boundsType) {
			super(operatorSequence, OperatorType.COUNT, quantifier);
			this.low = low;
			
			switch (boundsType) {
			case UNBOUNDED:
				high = Integer.MAX_VALUE;
				break;
			case CONSTANT_REPETITION:
				high = low;
				break;
			default:
				throw new IllegalArgumentException("Upper bounds needs to be specified.");
			}
			this.boundsType = boundsType;
		}
		
	}
	
	static class WildCardFactor extends RegexFactor<String> {
		public WildCardFactor() {
			super(".");
		}

		@Override
		public FactorType getFactorType() {
			return FactorType.WILD_CARD;
		}

		@Override
		public String toString() {
			return factorContent;
		}

		@Override
		public String getRepresentation() {
			return factorContent;
		}
	}

	static class CharacterClassFactor extends RegexFactor<String> {

		public CharacterClassFactor(String factorContent) {
			super(factorContent);
		}

		@Override
		public FactorType getFactorType() {
			return FactorType.CHARACTER_CLASS;
		}

		@Override
		public String toString() {
			return "[" + factorContent + "]";
		}

		@Override
		public String getRepresentation() {
			return "[" + factorContent + "]";
		}
	}

	static class GroupFactor extends RegexFactor<List<RegexToken>> {
		
		
		private int level;
		public int getLevel() {
			return level;
		}

		public GroupFactor(List<RegexToken> factorContent, GroupType groupType) {
			super(factorContent);
			this.groupType = groupType;
		}

		public GroupFactor(String processedContent, GroupType groupType, int level) {
			super(ParsingPreprocessor.tokenize(processedContent, level + 1));
			this.level = level;
			this.groupType = groupType;
		}

		@Override
		public FactorType getFactorType() {
			return FactorType.GROUP;
		}

		private GroupType groupType;

		public GroupType getGroupType() {
			return groupType;
		}
		
		private String groupPrefix() {
			switch (groupType) {
			case NORMAL:
				return "";
			case NONCAPTURING:
				return "?:";
			case POSLOOKAHEAD:
				return "?=";
			case POSLOOKBEHIND:
				return "?<=";
			case NEGLOOKAHEAD:
				return "?!";
			case NEGLOOKBEHIND:
				return "?<!";
			default:
				throw new AssertionError("Unknown group type.");
			}
		}
		
		@Override
		public String toString() {
			String type = groupPrefix();
			return "Group( " + type + " " + factorContent + " )";
		}

		public enum GroupType {
			NORMAL, NONCAPTURING, POSLOOKAHEAD, POSLOOKBEHIND, NEGLOOKAHEAD, NEGLOOKBEHIND
		}

		@Override
		public String getRepresentation() {
			String type = groupPrefix();
			StringBuilder contentBuilder = new StringBuilder();
			for (RegexToken rt : factorContent) {
				contentBuilder.append(rt.getRepresentation());
			}
			return "(" + type + contentBuilder.toString() + ")";
		}
		
		@Override
		public boolean equals(Object o) {
			if (o == null) {
				return false;
			}
			
			if (o == this) {
				return true;
			}
			
			if (!(o instanceof GroupFactor)) {
				return false;
			}
			
			GroupFactor ro = (GroupFactor) o;
			return ro.factorContent.equals(factorContent);
		}

		@Override
		public int hashCode() {
			return groupType.hashCode() * 13 + factorContent.hashCode();
		}
	}

	static class EscapeFactor extends RegexFactor<String> {
		
		public enum EscapeType {
			CHARACTER, OCTAL, UNICODE, HEX, VERBATIM, CHARACTER_PROPERTY
		}
		
		private final EscapeType type;
		public EscapeType getEscapeType() {
			return type;
		}

		public EscapeFactor(String factorContent, EscapeType type) {
			super(factorContent);
			this.type = type;
		}

		@Override
		public FactorType getFactorType() {
			return FactorType.ESCAPED_CHARACTER;
		}

		@Override
		public String toString() {
			return factorContent;
		}
		
		@Override
		public String getRepresentation() {
			return factorContent;
		}
		
		@Override
		public boolean equals(Object o) {
			if (o == null) {
				return false;
			}
			
			if (o == this) {
				return true;
			}
			
			if (!(o instanceof EscapeFactor)) {
				return false;
			}
			
			EscapeFactor ef = (EscapeFactor) o;
			return ef.factorContent.equals(factorContent);
		}

		@Override
		public int hashCode() {
			return type.hashCode() * 13 + factorContent.hashCode();
		}
	}

	static class SingleFactor extends RegexFactor<String> {

		public SingleFactor(String factorContent) {
			super(factorContent);
		}

		@Override
		public FactorType getFactorType() {
			return FactorType.SINGLE_CHARACTER;
		}

		@Override
		public String toString() {
			return factorContent;
		}
		
		@Override
		public String getRepresentation() {
			return factorContent;
		}
		
		@Override
		public boolean equals(Object o) {
			if (o == null) {
				return false;
			}
			
			if (o == this) {
				return true;
			}
			
			if (!(o instanceof SingleFactor)) {
				return false;
			}
			
			SingleFactor sf = (SingleFactor) o;
			return sf.factorContent.equals(factorContent);
		}

		@Override
		public int hashCode() {
			return factorContent.hashCode() * 13;
		}
		
	}
	
	public static void main(String [] args) {
		List<RegexToken> tokenStream = tokenize("\\x{FFFF}+", 0);
		System.out.println(tokenStream);
		for (RegexToken rt : tokenStream) {
			System.out.print(rt.getRepresentation());
		}
	}
	
	private static class RegexException extends RuntimeException {
		
		private static final long serialVersionUID = 1L;
		public RegexException(String message) {
			super(message);
		}
		
	}

}
