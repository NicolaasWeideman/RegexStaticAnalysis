package regexcompiler;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import regexcompiler.RegexSubexpression.SubexpressionType;
import regexcompiler.RegexToken.TokenType;

public class ParseTree {
		
	private TreeNode root;
	public TreeNode getRoot() {
		return root;
	}
	
	public ParseTree() {
	}
	
	public void addTreeNode(TreeNode child) {
		root.addChild(child);
	}
	
	public void newRoot(TreeNode child) {
		if (root != null) {
			child.addChild(root);
		}		
		root = child;
	}
	
	@Override
	public String toString() {
		StringBuilder treeStringBuilder = new StringBuilder();
		dfsTree(root, treeStringBuilder);
		return treeStringBuilder.toString();
	}
	
	private void dfsTree(TreeNode currentNode, StringBuilder sb) {
		
		if (!currentNode.getChildren().isEmpty()) {
			sb.append(currentNode);
			sb.append(getBracketType(currentNode, true));
			Iterator<TreeNode> childIterator = currentNode.getChildren().iterator();
			while (childIterator.hasNext()) {
				TreeNode child = childIterator.next();
				dfsTree(child, sb);
				if (childIterator.hasNext()) {
					sb.append(", ");
				}
				
			}
			sb.append(getBracketType(currentNode, false));
		} else {
			sb.append("'" + currentNode + "'");
		}
	}
	
	private String getBracketType(TreeNode node, boolean isOpen) {
		RegexToken token = node.getRegexToken();
		if (token.getTokenType() == TokenType.OPERATOR) {
			RegexOperator operatorToken = (RegexOperator) token;
			if (isOpen) {
				switch (operatorToken.getOperatorType()) {
				case JOIN:
					return "[";
				case UNION:
					return "{";
				default:
					return "<";
				}
			} else {
				switch (operatorToken.getOperatorType()) {
				case JOIN:
					return "]";
				case UNION:
					return "}";
				default:
					return ">";
				}
			}

		} else {
			if (isOpen) {
				return "(";
			} else {
				return ")";
			}
		}
	}
	
	
	
	public static class TreeNode {
		
		private List<TreeNode> children;
		public List<TreeNode> getChildren() {
			return children;
		}
		
		public void addChild(TreeNode child) {
			children.add(child);
		}
		
		private final RegexToken regexToken;
		public RegexToken getRegexToken() {
			return regexToken;
		}

		private int getRegexIndex() {
			return regexToken.getIndex();
		}
		
		public TreeNode(RegexToken regexToken) {
			this.regexToken = regexToken;
			
			children = new LinkedList<TreeNode>();
		}
		
		@Override
		public String toString() {
			if (regexToken.getTokenType() == TokenType.SUBEXPRESSION) {
				RegexSubexpression<?> subexpressionToken = (RegexSubexpression<?>) regexToken;
				if (subexpressionToken.getSubexpressionType() == SubexpressionType.GROUP) {
					RegexGroup groupToken = (RegexGroup) subexpressionToken;
					return groupToken.getGroupType().toString();
				}
			}
			return regexToken.toString();
		}
	}
}
