package regexcompiler;

import nfa.*;
import regexcompiler.RegexQuantifiableOperator.RegexPlusOperator;
import regexcompiler.RegexQuantifiableOperator.RegexQuestionMarkOperator;
import regexcompiler.RegexQuantifiableOperator.RegexStarOperator;

public interface NFACreator {
	
	public NFAGraph createBaseCaseEmpty();

	public NFAGraph createBaseCaseLookAround(NFAVertexND lookAroundState);
	
	public NFAGraph createBaseCaseEmptyString();
	
	public NFAGraph createBaseCaseSymbol(String symbol);
	
	public NFAGraph unionNFAs(NFAGraph m1, NFAGraph m2);
	
	public NFAGraph joinNFAs(NFAGraph m1, NFAGraph m2);
	
	public NFAGraph starNFA(NFAGraph m, RegexStarOperator starOperator);
	
	public NFAGraph plusNFA(NFAGraph m, RegexPlusOperator plusOperator);
	
	public NFAGraph countClosureNFA(NFAGraph m, RegexCountClosureOperator countClosureOperator);
	
	public NFAGraph questionMarkNFA(NFAGraph m, RegexQuestionMarkOperator questionMarkOperator);
	
	
}
