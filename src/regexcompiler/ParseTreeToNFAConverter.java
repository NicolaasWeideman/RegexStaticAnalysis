package regexcompiler;

import java.util.Iterator;

import nfa.NFAGraph;
import nfa.NFAVertexND;
import regexcompiler.ParseTree.TreeNode;
import regexcompiler.RegexQuantifiableOperator.RegexPlusOperator;
import regexcompiler.RegexQuantifiableOperator.RegexQuestionMarkOperator;
import regexcompiler.RegexQuantifiableOperator.RegexStarOperator;

public abstract class ParseTreeToNFAConverter implements NFACreator {
	
	protected static final int MAX_REPETITION = Integer.MAX_VALUE;

	public NFAGraph convertParseTree(ParseTree parseTree) {
		
		TreeNode root = parseTree.getRoot();
		NFAGraph nfaGraph = dfsBuild(root);
		return nfaGraph;
	}
	
	private NFAGraph dfsBuild(TreeNode currentNode) {
		NFAGraph newNfaGraph;
		RegexToken regexToken = currentNode.getRegexToken();
		Iterator<TreeNode> childIterator = currentNode.getChildren().iterator();
		switch (regexToken.getTokenType()) {
		case OPERATOR:
				RegexOperator regexOperator = (RegexOperator) regexToken;
				switch (regexOperator.getOperatorType()) {
				case STAR: {
					RegexStarOperator starOperator = (RegexStarOperator) regexOperator;
					TreeNode operandNode = childIterator.next();
					NFAGraph subgraph = dfsBuild(operandNode);
					newNfaGraph = starNFA(subgraph, starOperator);
					break;
				}
				case PLUS: {
					RegexPlusOperator plusOperator = (RegexPlusOperator) regexOperator;
					TreeNode operandNode = childIterator.next();
					NFAGraph subgraph = dfsBuild(operandNode);
					newNfaGraph = plusNFA(subgraph, plusOperator);
					break;
				}
				case COUNT_CLOSURE: {
					RegexCountClosureOperator countClosureOperator = (RegexCountClosureOperator) regexOperator;
					TreeNode operandNode = childIterator.next();
					NFAGraph subgraph = dfsBuild(operandNode);
					newNfaGraph = countClosureNFA(subgraph, countClosureOperator);
					break;
				}					
				case QUESTION_MARK: {
					RegexQuestionMarkOperator questionMarkOperator = (RegexQuestionMarkOperator) regexOperator;
					TreeNode operandNode = childIterator.next();
					NFAGraph subgraph = dfsBuild(operandNode);
					newNfaGraph = questionMarkNFA(subgraph, questionMarkOperator);
					break;
				}					
				case UNION: {
					TreeNode operandNode1 = childIterator.next();
					TreeNode operandNode2 = childIterator.next();
					NFAGraph subgraph1 = dfsBuild(operandNode1);
					NFAGraph subgraph2 = dfsBuild(operandNode2);
					newNfaGraph = unionNFAs(subgraph1, subgraph2);
					break;
				}
				case JOIN: {
					TreeNode operandNode1 = childIterator.next();
					TreeNode operandNode2 = childIterator.next();
					NFAGraph subgraph1 = dfsBuild(operandNode1);
					NFAGraph subgraph2 = dfsBuild(operandNode2);
					newNfaGraph = joinNFAs(subgraph1, subgraph2);
					break;
				}
				default:
					throw new RuntimeException("Unknown operator type.");
				}
			break;
		case SUBEXPRESSION:
			RegexSubexpression<?> regexSubexpression = (RegexSubexpression<?>) regexToken;
			switch (regexSubexpression.getSubexpressionType()) {
			case CHARACTER_CLASS: {
				RegexCharacterClass regexCharacterClass = (RegexCharacterClass) regexSubexpression;
				newNfaGraph = createBaseCaseSymbol(regexCharacterClass.toString());
				break;
			}
			case ESCAPED_SYMBOL: {
				RegexEscapedSymbol regexEscapedSymbol = (RegexEscapedSymbol) regexSubexpression;
				newNfaGraph = createBaseCaseSymbol(regexEscapedSymbol.toString());
				break;
			}
			case GROUP:
				RegexGroup regexGroup = (RegexGroup) regexSubexpression;
				switch (regexGroup.getGroupType()) {
				case NORMAL:					
					break;
				case NEGLOOKAHEAD:
					throw new UnsupportedOperationException("Negative Look ahead symbols not supported");
					//break;
				case NONCAPTURING:
					break;
				case NEGLOOKBEHIND:
					throw new UnsupportedOperationException("Negative Look behind symbols not supported");
					//break;
				case POSLOOKAHEAD:
					throw new UnsupportedOperationException("Positive Look ahead symbols not supported");
					//break;
				case POSLOOKBEHIND:
					throw new UnsupportedOperationException("Positive Look behind symbols not supported");
					//break;
				default:
					throw new RuntimeException("Unknown Group type.");
				} // End switch group type
				TreeNode child = childIterator.next();
				newNfaGraph = dfsBuild(child);
				break;
			case SYMBOL: {
				RegexSymbol regexSymbol = (RegexSymbol) regexSubexpression;
				String content = regexSymbol.getSubexpressionContent();
				newNfaGraph = createBaseCaseSymbol(content);
				break;
			}
			default:
				throw new RuntimeException("Unknown Subexpression type.");
				//break;
			} // End switch subexpression type
			break;
		default:
			throw new RuntimeException("Unknown Token type.");
			//break;
		} // End switch token type
		return newNfaGraph;
	}

	protected NFAVertexND deriveVertex(NFAGraph m, NFAVertexND v) {
		String newName = "q";
		int i = 0;
		while (m.containsVertex(v)) {
			v = new NFAVertexND(newName + i);
			i++;
		}
		return v;
	}
}
