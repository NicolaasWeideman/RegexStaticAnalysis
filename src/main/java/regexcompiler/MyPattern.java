package regexcompiler;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import nfa.NFAGraph;
import matcher.*;
import regexcompiler.ParseTree.TreeNode;
import regexcompiler.RegexAnchor.RegexAnchorType;
import regexcompiler.RegexEscapedSymbol.RegexEscapedSymbolType;
import regexcompiler.RegexGroup.RegexGroupType;
import regexcompiler.RegexOperator.OperatorType;
import regexcompiler.RegexOperator.RegexUnionOperator;
import regexcompiler.RegexOperator.RegexJoinOperator;
import regexcompiler.RegexQuantifiableOperator.QuantifierType;
import regexcompiler.RegexQuantifiableOperator.RegexPlusOperator;
import regexcompiler.RegexQuantifiableOperator.RegexQuestionMarkOperator;
import regexcompiler.RegexQuantifiableOperator.RegexStarOperator;
import regexcompiler.RegexSubexpression.SubexpressionType;
import regexcompiler.RegexToken.TokenType;

import analysis.AnalysisSettings.NFAConstruction;

/*
 * Known issues: does not parse a*{1,2} In the Java parser the {...} gets ignored: a?{2} matches ε and a not aa
 */

public class MyPattern {

	private NFAGraph nfaGraph;
	
	public static void main(String [] args) {
		if (args.length < 1) {
			System.out.println("Pattern should be specified as a command line argument (and possibly give an input string).");
		}
		if (args.length < 2) {
			String pattern = args[0];
			Tokeniser t = new Tokeniser(pattern);
			List<RegexToken> tokenList = t.tokenise();
			
			Parser p = new Parser(pattern, tokenList);
			ParseTree parseTree = p.parse();
			System.out.println(parseTree);
			NFAGraph resultGraph = toNFAGraph(pattern, NFAConstruction.JAVA);
			System.out.println(resultGraph);
		} else {
			String pattern = args[0];
			String inputString = args[1];
			MyPattern myPattern = MyPattern.compile(pattern, NFAConstruction.JAVA);
			MyMatcher myMatcher = myPattern.matcher(inputString);
			boolean matches = myMatcher.matches();
			System.out.println(pattern + " matches " + inputString + ": " + matches);
		}
		
	}
	

	private static final int MAX_REPETITION = Integer.MAX_VALUE;

	private MyPattern(NFAGraph nfaGraph) {
		this.nfaGraph = nfaGraph;
	}

	public static MyPattern compile(String pattern, NFAConstruction construction) {
		NFAGraph nfaGraph = toNFAGraph(pattern, construction);
		return new MyPattern(nfaGraph);
	}

	public MyMatcher matcher(String inputString) {
		if (nfaGraph == null) {
			throw new IllegalStateException("Pattern has not yet been compiled!");
		}
		return new RegexNFAMatcher(nfaGraph, inputString);
	}
	
	public static NFAGraph toNFAGraph(String pattern, NFAConstruction construction) {
		Tokeniser t = new Tokeniser(pattern);
		List<RegexToken> tokenList = t.tokenise();
		//System.out.println(tokenList);	
		Parser p = new Parser(pattern, tokenList);
		ParseTree parseTree = p.parse();
		ParseTreeToNFAConverter pttnc;
		switch (construction) {
		case THOMPSON:
			pttnc = new ThompsonParseTreeToNFAConverter();
			break;
		case JAVA:
			pttnc = new JavaParseTreeToNFAConverter();
			break;
		default:
			throw new RuntimeException("Unknown regex flavour");
		}
		
		NFAGraph resultNFA = pttnc.convertParseTree(parseTree);
		return resultNFA;
	}

	private static class Tokeniser {

		private final String pattern;
		private final char[] patternArr;
		private final int length;
		private int i;

		private List<RegexToken> tokenList;
		private Stack<List<RegexToken>> tokenListStack;
		private Stack<RegexGroupType> groupTypeStack;

		private boolean verbatimMode;

		private Tokeniser(String pattern) {
			this.pattern = pattern;
			this.patternArr = pattern.toCharArray();
			this.length = patternArr.length;
			this.verbatimMode = false;

		}

		private List<RegexToken> tokenise() {
			tokenList = new ArrayList<RegexToken>();
			tokenListStack = new Stack<List<RegexToken>>();
			groupTypeStack = new Stack<RegexGroupType>();
			i = 0;
			while (true) {
				if (!verbatimMode) {
					switch (patternArr[i]) {
					case '^':
						RegexAnchor lineStartAnchor = new RegexAnchor(RegexAnchorType.LINESTART, i);
						tokenList.add(lineStartAnchor);
						i++;
						break;
					case '$':
						RegexAnchor lineEndAnchor = new RegexAnchor(RegexAnchorType.LINEEND, i);
						tokenList.add(lineEndAnchor);
						i++;
						break;
					case '[':
						RegexCharacterClass rcc = createTokenCharacterClass();
						tokenList.add(rcc);
						i++;
						break;
					case '(':
						RegexGroupType newGroupType = findGroupType();
						groupTypeStack.push(newGroupType);
						/* preserve token list */
						tokenListStack.push(tokenList);
						/* token list for this group */
						tokenList = new ArrayList<RegexToken>();
						
						break;
					case ')':
						RegexGroupType currentGroupType = groupTypeStack.pop();
						RegexGroup rg = new RegexGroup(tokenList, currentGroupType, i);
						tokenList = tokenListStack.pop();
						tokenList.add(rg);
						i++;
						break;
					case '\\':
						i++;
						switch (patternArr[i]) {
						case 'Q':
							this.verbatimMode = true;
							i++;
							break;
						default:
							RegexEscapedSymbol res = createTokenEscapedSymbol();
							tokenList.add(res);
						} // End switch

						break;
					case '|':
						RegexUnionOperator unionOperator = new RegexUnionOperator(i);
						tokenList.add(unionOperator);
						i++;
						break;
					case '*':
						RegexStarOperator starOperator = (RegexStarOperator) createQuantifiableOperator(OperatorType.STAR);
						tokenList.add(starOperator);
						break;
					case '+':
						RegexPlusOperator plusOperator = (RegexPlusOperator) createQuantifiableOperator(OperatorType.PLUS);
						tokenList.add(plusOperator);
						break;
					case '?':
						RegexQuestionMarkOperator questionMarkOperator = (RegexQuestionMarkOperator) createQuantifiableOperator(OperatorType.QUESTION_MARK);
						tokenList.add(questionMarkOperator);
						break;
					case '{':
						RegexCountClosureOperator countedClosureOperator;
						QuantifierType countedClosureQuantifier;
						StringBuilder countClosureOperatorBuilder = new StringBuilder();
						try {
							i++;

							while (patternArr[i] != '}') {
								countClosureOperatorBuilder.append(patternArr[i]);
								i++;
							}
							i++;
							if (i < length && patternArr[i] == '?') {
								countedClosureQuantifier = QuantifierType.RELUCTANT;
								i++;
							} else if (i < length && patternArr[i] == '+') {
								countedClosureQuantifier = QuantifierType.POSSESSIVE;
								i++;
							} else {
								countedClosureQuantifier = QuantifierType.GREEDY;
								/* Leave i for unknown token */
							}
						} catch (ArrayIndexOutOfBoundsException aiooe) {
							throw new PatternSyntaxException("Unclosed counted closure", pattern, i);
						}

						String bounds = countClosureOperatorBuilder.toString();
						Pattern boundedPattern = Pattern.compile("(\\d+),(\\d+)");
						Pattern unboundedPattern = Pattern.compile("(\\d+),");
						Pattern constantRepititionPattern = Pattern.compile("(\\d+)");

						Matcher boundedMatcher = boundedPattern.matcher(bounds);
						Matcher unboundedMatcher = unboundedPattern.matcher(bounds);
						Matcher constantRepititionMatcher = constantRepititionPattern.matcher(bounds);
						int low,
						high;
						if (boundedMatcher.find()) {
							String lowStr = boundedMatcher.group(1);
							low = Integer.parseInt(lowStr);
							String highStr = boundedMatcher.group(2);
							high = Integer.parseInt(highStr);

							if (high < low || low < 0 || high > MAX_REPETITION) {
								throw new PatternSyntaxException("Illegal repetition range", pattern, i);
							}

							countedClosureOperator = new RegexCountClosureOperator(low, high, countedClosureQuantifier, i);
							tokenList.add(countedClosureOperator);

						} else if (unboundedMatcher.find()) {
							String lowStr = unboundedMatcher.group(1);
							low = Integer.parseInt(lowStr);

							if (low < 0 || low > MAX_REPETITION) {
								throw new PatternSyntaxException("Illegal repetition range", pattern, i);
							}
							high = MAX_REPETITION;
							countedClosureOperator = new RegexCountClosureOperator(low, high, countedClosureQuantifier, i);
							tokenList.add(countedClosureOperator);

						} else if (constantRepititionMatcher.find()) {
							String lowStr = constantRepititionMatcher.group(1);
							low = Integer.parseInt(lowStr);

							if (low < 0 || low > MAX_REPETITION) {
								throw new PatternSyntaxException("Illegal repetition range", pattern, i);
							}
							high = low;
							countedClosureOperator = new RegexCountClosureOperator(low, high, countedClosureQuantifier, i);
							tokenList.add(countedClosureOperator);
						} else {
							throw new PatternSyntaxException("Illegal repetition range", pattern, i);
						}
						break;
					default:
						RegexSymbol rs = new RegexSymbol("" + patternArr[i], i);
						tokenList.add(rs);
						i++;
					} // End switch
				} else {
					// verbatimMode is true
					if (patternArr[i] == '\\') {
						i++;
						if (i < length && patternArr[i] == 'E') {
							i++;
							verbatimMode = false;
						} else {
							RegexSymbol rs = new RegexSymbol("\\", i);
							tokenList.add(rs);
							rs = new RegexSymbol("" + patternArr[i], i);
							tokenList.add(rs);
							i++;
						}
					} else {
						RegexSymbol rs = new RegexSymbol("" + patternArr[i], i);
						tokenList.add(rs);
						i++;
					}
				} // End if/else

				if (i >= length) {
					break;
				}
			}
			return tokenList;

		}

		private RegexCharacterClass createTokenCharacterClass() {
			/*
			 * read until the next unescaped ] is found and add it to a
			 * character class
			 */
			StringBuilder characterClassBuilder = new StringBuilder();
			try {
				i++;
				int depthCounter = 1;
				while (true) {
					if (patternArr[i] == '[') {
						depthCounter++;
					} else if (patternArr[i] == ']') {
						depthCounter--;
						if (depthCounter == 0) {
							break;
						}
					}
					if (patternArr[i] == '\\') {
						/* do not interpret escaped character */
						i++;
						characterClassBuilder.append("\\" + patternArr[i]);
					} else {
						characterClassBuilder.append(patternArr[i]);
					}
					i++;	
				}
			} catch (ArrayIndexOutOfBoundsException aioobe) {
				throw new PatternSyntaxException("Unclosed character class", pattern, i);
			}
			return new RegexCharacterClass(characterClassBuilder.toString(), i);
		}

		private RegexEscapedSymbol createTokenEscapedSymbol() {
			RegexEscapedSymbol res;
			switch (patternArr[i]) {
			case 'x':
				StringBuilder hexStringBuilder = new StringBuilder();
				try {
					i++;
					if (patternArr[i] == '{') {
						i++;
						while (patternArr[i] != '}') {
							hexStringBuilder.append(patternArr[i]);
							i++;
						}
						i++;
					} else {
						hexStringBuilder.append(patternArr[i]);
						i++;
						hexStringBuilder.append(patternArr[i]);
						i++;
					}
				} catch (ArrayIndexOutOfBoundsException aioobe) {
					throw new PatternSyntaxException("Unclosed hexadecimal escape sequence", pattern, i);
				}
				res = new RegexEscapedSymbol(hexStringBuilder.toString(), RegexEscapedSymbolType.HEX, i);
				break;
			case '0':
				StringBuilder octStringBuilder = new StringBuilder();
				try {
					int tmpNum = 0;
					int octalDigitCounter = 0;
					i++;
					if ('0' > patternArr[i] || patternArr[i] > '7') {
						throw new PatternSyntaxException("Illegal octal escape sequence", pattern, i);
					}
					while (i < length && tmpNum < 0377 && ('0' <= patternArr[i] && patternArr[i] <= '7')
							&& octalDigitCounter < 3) {
						octStringBuilder.append(patternArr[i]);
						tmpNum = Integer.parseInt(octStringBuilder.toString(), 8);
						octalDigitCounter++;
						i++;
					}
				} catch (NumberFormatException nfe) {
					throw new PatternSyntaxException("Illegal octal escape sequence", pattern, i);
				}

				res = new RegexEscapedSymbol(octStringBuilder.toString(), RegexEscapedSymbolType.OCTAL, i);
				break;
			case 'u':
				StringBuilder unicodeStringBuilder = new StringBuilder();
				try {
					i++;
					for (int j = 0; j < 4; j++) {
						unicodeStringBuilder.append(patternArr[i]);
						i++;
					}

				} catch (ArrayIndexOutOfBoundsException aioobe) {
					throw new PatternSyntaxException("Illegal unicode escape sequence", pattern, i);
				}

				res = new RegexEscapedSymbol(unicodeStringBuilder.toString(), RegexEscapedSymbolType.UNICODE, i);
				break;
			case 'p':
				StringBuilder characterPropertyBuilder = new StringBuilder();
				try {
					i++;
					if (patternArr[i] == '{') {
						i++;

						while (patternArr[i] != '}') {
							characterPropertyBuilder.append(patternArr[i]);
							i++;
						}
						i++;
					} else {
						characterPropertyBuilder.append(patternArr[i]);
						i++;
					}
				} catch (ArrayIndexOutOfBoundsException aioobe) {
					throw new PatternSyntaxException("Unclosed character family escape sequence", pattern, i);
				}

				res = new RegexEscapedSymbol(characterPropertyBuilder.toString(), RegexEscapedSymbolType.CHARACTER_PROPERTY, i);
				break;
			default:
				String escapedChar;
				try {
					escapedChar = "" + patternArr[i];
					i++;
				} catch (ArrayIndexOutOfBoundsException aioobe) {
					throw new PatternSyntaxException("Unexpected internal error", pattern, i);
				}
				res = new RegexEscapedSymbol(escapedChar, RegexEscapedSymbolType.CHARACTER, i);
				break;
			} // End switch
			return res;
		}

		private RegexQuantifiableOperator createQuantifiableOperator(OperatorType ot) {
			i++;
			QuantifierType quantifierType;
			if (i < length) {
				if (patternArr[i] == '?') {
					i++;
					quantifierType = QuantifierType.RELUCTANT;
				} else if (patternArr[i] == '+') {
					i++;
					quantifierType = QuantifierType.POSSESSIVE;
				} else {
					/* leave i's value for next token */
					quantifierType = QuantifierType.GREEDY;
				}
			} else {
				quantifierType = QuantifierType.GREEDY;
			}
			switch (ot) {
			case PLUS:
				return new RegexPlusOperator(quantifierType, i);
			case QUESTION_MARK:
				return new RegexQuestionMarkOperator(quantifierType, i);
			case STAR:
				return new RegexStarOperator(quantifierType, i);
			default:
				throw new RuntimeException("Unkown oeprator: " + ot);
			}
		}
	
		private RegexGroupType findGroupType() {
			RegexGroupType groupType = RegexGroupType.NORMAL;
			if (i < length - 2 && patternArr[i + 1] == '?') {
				if (patternArr[i + 2] == '<') {					
					if (i < length - 3) {
						
						/* check for look behind */
						switch (patternArr[i + 3]) {
						case '=':
							groupType = RegexGroupType.POSLOOKBEHIND;
							break;
						case '!':							
							groupType = RegexGroupType.NEGLOOKBEHIND;
							break;
						default:
							throw new PatternSyntaxException("Unkown look-behind group", pattern, i);

						}
					} else {
						throw new PatternSyntaxException("Unkown look-behind group", pattern, i);
					}
					i += 4;
				} else {
					switch (patternArr[i + 2]) {
					case ':':
						groupType = RegexGroupType.NONCAPTURING;
						break;
					case '=':
						groupType = RegexGroupType.POSLOOKAHEAD;
						break;
					case '!':
						groupType = RegexGroupType.NEGLOOKAHEAD;
						break;
					default:
						throw new PatternSyntaxException("Unkown inline modifier", pattern, i);

					}
					i += 3;
				}
			} else {
				i++;
			}
			return groupType;
		}
	}

	private static class Parser {
		
		private final String pattern;
		private final List<RegexToken> tokenList;
		
		private Iterator<RegexToken> tokenIterator;
		private RegexToken currentToken;
		private int index;
		private boolean endOfStream;
		private boolean isNested;
		
		private boolean nextToken() {
			if (tokenIterator.hasNext()) {
				currentToken = tokenIterator.next();
				index = currentToken.getIndex();
				return true;
			}
			currentToken = null;
			endOfStream = true;
			return false;
			
		}
		
		public Parser(String pattern, List<RegexToken> tokenList) {
			this.isNested = false;
			this.pattern = pattern;
			this.tokenList = tokenList;
		}
		
		private Parser(boolean isNested, String pattern, List<RegexToken> tokenList) {
			this.isNested = true;
			this.pattern = pattern;
			this.tokenList = tokenList;
		}
		
		public ParseTree parse() {
			tokenIterator = tokenList.iterator();
			endOfStream = false;
			nextToken();
			TreeNode root = parseRegex();
			ParseTree pt = new ParseTree();
			pt.newRoot(root);	
			
			return pt;
		}
		
		public TreeNode parseRegex() {
			//System.out.println("Parse Regex");
			if (currentToken.getTokenType() == TokenType.ANCHOR) {
				RegexAnchor regexAnchorToken = (RegexAnchor) currentToken;
				if (regexAnchorToken.getAnchorType() == RegexAnchorType.LINESTART && !isNested) {
					nextToken();
					/* Since we assume line based matching, we ignore the caret at the start */
				} else {
					throw new UnimplementedFunctionalityException("Anchor at invalid position: " + regexAnchorToken.getAnchorType() + " at " + regexAnchorToken.getIndex());
				}
			}
			TreeNode root = parseTerm();
			while (checkEndOfTerm()) {
				TreeNode operatorNode = new TreeNode(currentToken);
				operatorNode.addChild(root);
				if(nextToken()) {
					TreeNode nextTermNode = parseTerm();
					operatorNode.addChild(nextTermNode);
					root = operatorNode;
				} else {
					/* Create an empty factor */
				}
				
			}
			if (!endOfStream && currentToken.getTokenType() == TokenType.ANCHOR) {
				RegexAnchor regexAnchorToken = (RegexAnchor) currentToken;
				nextToken();
				if (regexAnchorToken.getAnchorType() == RegexAnchorType.LINEEND && !isNested) {
					/* Since we assume line based matching, we ignore the dollar at the line end */
				} else {
					throw new UnimplementedFunctionalityException("Anchor at invalid position: " + regexAnchorToken.getAnchorType() + " at " + regexAnchorToken.getIndex());
				}
			}
			if (!endOfStream) {
				throw new PatternSyntaxException("Dangling meta character '" + currentToken + "'", pattern, currentToken.getIndex());
			}
			//System.out.println("END Parse Regex");
			return root;
			
		}
		
		public TreeNode parseTerm() {
			//System.out.println("Parse Term");
			TreeNode root;
			if (currentToken.getTokenType() == TokenType.SUBEXPRESSION) {
				root = parseFactor();
			} else if (currentToken.getTokenType() == TokenType.OPERATOR) {
				/* empty factor */
				RegexOperator operatorToken = (RegexOperator) currentToken;
				if (operatorToken.getIsBinaryOperator()) {
					return new TreeNode(new RegexSymbol("", index));
				} else {
					throw new PatternSyntaxException("Dangling meta character '" + operatorToken + "'", pattern, operatorToken.getIndex());
				}
				
			} else if (currentToken.getTokenType() == TokenType.ANCHOR) {
				RegexAnchor regexAnchorToken = (RegexAnchor) currentToken;
				throw new UnimplementedFunctionalityException("Anchor at invalid position: " + regexAnchorToken.getAnchorType() + " at " + regexAnchorToken.getIndex());
			} else {
				throw new RuntimeException("Unknown token type: " + currentToken.getTokenType());
			}
			while (!endOfStream && currentToken.getTokenType() == TokenType.SUBEXPRESSION) {
				TreeNode subexpressionNode = parseFactor();
				/* TODO implement lookaround here */
				TreeNode joinOperatorNode = new TreeNode(new RegexJoinOperator(index));
				joinOperatorNode.addChild(root);
				joinOperatorNode.addChild(subexpressionNode);
				root = joinOperatorNode;
			}
			//System.out.println("END Parse Term");
			return root;
		}
		
		public TreeNode parseFactor() {
			//System.out.println("Parse Factor");
			TreeNode root;
			if (currentToken.getTokenType() == TokenType.SUBEXPRESSION) {
				RegexSubexpression<?> subexpressionToken = (RegexSubexpression<?>) currentToken;
				
				if (subexpressionToken.getSubexpressionType() == SubexpressionType.GROUP) {
					RegexGroup groupToken = (RegexGroup) subexpressionToken;
					RegexGroupType groupTokenType = groupToken.getGroupType();
					/* TODO if this works, remove switch and handle all groups equally */
					switch (groupTokenType) {
					case NEGLOOKAHEAD: {
						//throw new UnimplementedFunctionalityException("Negative lookahead has not yet been implemented.");
						Parser p = new Parser(true, pattern, groupToken.getSubexpressionContent());
						ParseTree pt = p.parse();
						/* We add a group node so that the info on which type of group (?: or ?<= Etc) does not go missing  */
						TreeNode groupNode = new TreeNode(groupToken);
						groupNode.addChild(pt.getRoot());
						root = groupNode;
						break;
					}
					case NEGLOOKBEHIND: {
						//throw new UnimplementedFunctionalityException("Negative lookbehind has not yet been implemented.");
						Parser p = new Parser(true, pattern, groupToken.getSubexpressionContent());
						ParseTree pt = p.parse();
						/* We add a group node so that the info on which type of group (?: or ?<= Etc) does not go missing  */
						TreeNode groupNode = new TreeNode(groupToken);
						groupNode.addChild(pt.getRoot());
						root = groupNode;
						break;
					}
					case NONCAPTURING: {
						/* we do not perform capturing, so handle noncapturing groups as normal groups */
						Parser p = new Parser(true, pattern, groupToken.getSubexpressionContent());
						ParseTree pt = p.parse();
						/* We add a group node so that the info on which type of group (?: or ?<= Etc) does not go missing  */
						TreeNode groupNode = new TreeNode(groupToken);
						groupNode.addChild(pt.getRoot());
						root = groupNode;
						break;
					}
					case NORMAL: {
						Parser p = new Parser(true, pattern, groupToken.getSubexpressionContent());
						ParseTree pt = p.parse();
						/* We add a group node so that the info on which type of group (?: or ?<= Etc) does not go missing  */
						TreeNode groupNode = new TreeNode(groupToken);
						groupNode.addChild(pt.getRoot());
						root = groupNode;
						break;
					}
					case POSLOOKAHEAD: {
						//throw new UnimplementedFunctionalityException("Positive lookahead has not yet been implemented.");
						Parser p = new Parser(true, pattern, groupToken.getSubexpressionContent());
						ParseTree pt = p.parse();
						/* We add a group node so that the info on which type of group (?: or ?<= Etc) does not go missing  */
						TreeNode groupNode = new TreeNode(groupToken);
						groupNode.addChild(pt.getRoot());
						root = groupNode;
						break;
					}
					case POSLOOKBEHIND: {
						//throw new UnimplementedFunctionalityException("Positive lookbehind has not yet been implemented.");
						Parser p = new Parser(true, pattern, groupToken.getSubexpressionContent());
						ParseTree pt = p.parse();
						/* We add a group node so that the info on which type of group (?: or ?<= Etc) does not go missing  */
						TreeNode groupNode = new TreeNode(groupToken);
						groupNode.addChild(pt.getRoot());
						root = groupNode;
						break;
					}
					default:
						throw new RuntimeException("Unknown group type: " + groupTokenType);
						//break;
					
					}
				} else {
					TreeNode subexpressionNode = new TreeNode(currentToken);
					root = subexpressionNode;
				}
				
				/* search for unary operator */
				if (nextToken()) {
					if (currentToken.getTokenType() == TokenType.OPERATOR) {
						RegexOperator currentOperator = (RegexOperator) currentToken;
						if (!currentOperator.getIsBinaryOperator()) {
							TreeNode unaryOperatorNode = new TreeNode(currentOperator);
							unaryOperatorNode.addChild(root);
							root = unaryOperatorNode;
							nextToken();
						} else {
							/* leave for End of term */
						}
					} else {
						/* leave for next parseFactor */
					}
				}
				
			} else if (currentToken.getTokenType() == TokenType.ANCHOR) {
				RegexAnchor regexAnchorToken = (RegexAnchor) currentToken;
				throw new UnimplementedFunctionalityException("Anchor at invalid position: " + regexAnchorToken.getAnchorType() + " at " + regexAnchorToken.getIndex());
			} else {
				if (checkEndOfTerm()) {
					/* return empty factor */
					return new TreeNode(new RegexSymbol("", index));
				} else {
					/* error dangling meta character */
					throw new PatternSyntaxException("Dangling meta character", pattern, index);
				}
			}
			//System.out.println("END Parse Factor");
			return root;
		}
		
		private boolean checkEndOfTerm() {
			return !endOfStream && (currentToken.getTokenType() == TokenType.OPERATOR && ((RegexOperator) currentToken).getIsBinaryOperator());
		}
		
	}

	static class RegexNFAMatcher extends NFAMatcher {
		private RegexNFAMatcher(NFAGraph nfaGraph, String inputString) {
			super(nfaGraph, inputString);
		}
	}
}
