package regexcompiler;

/*
 * Operator assumptions:
 * > Only one start and accept state
 * > No outgoing transitions from the accept state
 */

import java.util.HashMap;

import nfa.transitionlabel.TransitionLabel;
import nfa.transitionlabel.TransitionLabelParserRecursive;
import regexcompiler.RegexQuantifiableOperator.QuantifierType;
import regexcompiler.RegexQuantifiableOperator.RegexPlusOperator;
import regexcompiler.RegexQuantifiableOperator.RegexQuestionMarkOperator;
import regexcompiler.RegexQuantifiableOperator.RegexStarOperator;

import nfa.NFAGraph;
import nfa.NFAVertexND;
import nfa.NFAEdge;
import nfa.transitionlabel.EpsilonTransitionLabel;

public class JavaParseTreeToNFAConverter extends ParseTreeToNFAConverter {

	@Override
	public NFAGraph createBaseCaseEmpty() {
		NFAGraph m = new NFAGraph();
		NFAVertexND q0 = nextState();
		NFAVertexND q1 = nextState();
		m.addVertex(q0);
		m.addVertex(q1);
		
		m.setInitialState(q0);
		m.addAcceptingState(q1);
		return m;
	}

	@Override
	public NFAGraph createBaseCaseLookAround(NFAVertexND lookAroundState) {
		NFAGraph m = new NFAGraph();
		NFAVertexND q0 = nextState();
		NFAVertexND q1 = nextState();
		m.addVertex(q0);
		m.addVertex(lookAroundState);
		m.addVertex(q1);
		m.addEdge(new NFAEdge(q0, lookAroundState, new EpsilonTransitionLabel("ε1")));
		m.addEdge(new NFAEdge(lookAroundState, q1, new EpsilonTransitionLabel("ε1")));
		m.setInitialState(q0);
		m.addAcceptingState(q1);
		return m;
	}

	@Override
	public NFAGraph createBaseCaseEmptyString() {
		NFAGraph m = new NFAGraph();
		NFAVertexND q0 = nextState();
		NFAVertexND q1 = nextState();
		m.addVertex(q0);
		m.addVertex(q1);
		m.addEdge(new NFAEdge(q0, q1, new EpsilonTransitionLabel("ε1")));
		m.setInitialState(q0);
		m.addAcceptingState(q1);
		return m;
	}

	@Override
	public NFAGraph createBaseCaseSymbol(String symbol) {
		TransitionLabelParserRecursive tlpr = new TransitionLabelParserRecursive(symbol);
		TransitionLabel transitionLabel = tlpr.parseTransitionLabel();
		NFAGraph m = new NFAGraph();
		NFAVertexND q0 = nextState();
		NFAVertexND q1 = nextState();
		m.addVertex(q0);
		m.addVertex(q1);
		
		if (!transitionLabel.isEmpty()) {
			m.addEdge(new NFAEdge(q0, q1, transitionLabel));
		}
		m.setInitialState(q0);
		m.addAcceptingState(q1);
		return m;
	}

	@Override
	public NFAGraph unionNFAs(NFAGraph m1, NFAGraph m2) {
		NFAGraph resultNFA = new NFAGraph();
		HashMap<NFAVertexND, NFAVertexND> stateMap = new HashMap<NFAVertexND, NFAVertexND>();
		assert m1.getAcceptingStates().size() == 1 : "Construction assumes only one accept state";
		NFAVertexND m1AcceptState = m1.getAcceptingStates().iterator().next();
		/* Adding the vertices of m1 */
		for (NFAVertexND v : m1.vertexSet()) {
			resultNFA.addVertex(v);
		}
		NFAVertexND m2Acceptstate = null;
		/* Adding the vertices of m2 */
		assert m2.getAcceptingStates().size() == 1 : "Construction assumes only one accept state";
		NFAVertexND m2OriginalAcceptState = m2.getAcceptingStates().iterator().next();
		
		int i = 0;
		for (NFAVertexND v : m2.vertexSet()) {
			NFAVertexND newVertex = v;
			/* We need to keep the prefix for lookaround states (l) */
			String newName = "" + newVertex.getStateNumberByDimension(1).charAt(0);
			while (resultNFA.containsVertex(newVertex) || newVertex.equals(m2Acceptstate)) {
				newVertex = new NFAVertexND(newName + i);
				i++;
			}
			/* Do not add the accept state of m2 */
			if (!m2OriginalAcceptState.equals(v)) {
				resultNFA.addVertex(newVertex);
			} else {
				m2Acceptstate = newVertex;
			}
			
			stateMap.put(v, newVertex);
		}

		for (NFAEdge e : m1.edgeSet()) {
			NFAVertexND source = e.getSourceVertex();
			NFAVertexND target = e.getTargetVertex();
			
			resultNFA.addEdge(new NFAEdge(source, target, e.getTransitionLabel()));
		}
		for (NFAEdge e : m2.edgeSet()) {
			NFAVertexND source = stateMap.get(e.getSourceVertex());
			NFAVertexND target = stateMap.get(e.getTargetVertex());			
			
			if (!m2Acceptstate.equals(target)) {
				resultNFA.addEdge(new NFAEdge(source, target, e.getTransitionLabel()));
			} else {
				resultNFA.addEdge(new NFAEdge(source, m1AcceptState, e.getTransitionLabel()));
			}
			
		}
		
		/* Add the new initial vertex */
		NFAVertexND newInitialVertex = new NFAVertexND("q0");
		while (resultNFA.containsVertex(newInitialVertex)) {
			newInitialVertex = new NFAVertexND("q" + i);
			i++;
		}
		resultNFA.addVertex(newInitialVertex);
		resultNFA.setInitialState(newInitialVertex);
		
		/* Add the connecting edges */
		NFAVertexND m1InitialState = m1.getInitialState();
		NFAVertexND m2InitialState = stateMap.get(m2.getInitialState());
		resultNFA.addEdge(new NFAEdge(newInitialVertex, m1InitialState, new EpsilonTransitionLabel("ε1")));
		resultNFA.addEdge(new NFAEdge(newInitialVertex, m2InitialState, new EpsilonTransitionLabel("ε2")));
		
		resultNFA.addAcceptingState(m1AcceptState);
		
		return resultNFA;
	}

	@Override
	public NFAGraph joinNFAs(NFAGraph m1, NFAGraph m2) {
		NFAGraph resultNFA = new NFAGraph();
		HashMap<NFAVertexND, NFAVertexND> stateMap = new HashMap<NFAVertexND, NFAVertexND>();
		/* Adding the vertices of m1 */
		for (NFAVertexND v : m1.vertexSet()) {
			resultNFA.addVertex(v);
		}
		
		/* Adding the vertices of m2 */
		NFAVertexND m2InitialState = null;
		
		int i = 0;
		for (NFAVertexND v : m2.vertexSet()) {
			/* Do not add m2's initial state */
			
			NFAVertexND newVertex = v;
			/* We need to keep the prefix for lookaround states (l) */
			String newName = "" + v.getStateNumberByDimension(1).charAt(0);
			while (resultNFA.containsVertex(newVertex) || newVertex.equals(m2InitialState)) {
				newVertex = new NFAVertexND(newName + i);
				i++;
			}
			
			if (!m2.getInitialState().equals(v)) {
				resultNFA.addVertex(newVertex);				
			} else {
				m2InitialState = newVertex;
			}
			stateMap.put(v, newVertex);

		}
		
		for (NFAEdge e : m1.edgeSet()) {
			NFAVertexND source = e.getSourceVertex();
			NFAVertexND target = e.getTargetVertex();			
			resultNFA.addEdge(new NFAEdge(source, target, e.getTransitionLabel()));
		}
		
		for (NFAEdge e : m2.edgeSet()) {
			NFAVertexND source = stateMap.get(e.getSourceVertex());
			NFAVertexND target = stateMap.get(e.getTargetVertex());	
			if (!m2InitialState.equals(source) && !m2InitialState.equals(target)) {
				NFAEdge newEdge = new NFAEdge(source, target, e.getTransitionLabel());
				resultNFA.addEdge(newEdge);
			} else if (m2InitialState.equals(source)) {
				/* All the outgoing edges of the original initial state of m2 now come from the accepting states */
				for (NFAVertexND m1AcceptingState : m1.getAcceptingStates()) {
					NFAEdge newEdge = new NFAEdge(m1AcceptingState, target, e.getTransitionLabel());
					resultNFA.addEdge(newEdge);
				}
			} else {
				/* All the outgoing edges of the original initial state of m2 now come from the accepting states */
				for (NFAVertexND m1AcceptingState : m1.getAcceptingStates()) {
					NFAEdge newEdge = new NFAEdge(source, m1AcceptingState, e.getTransitionLabel());
					resultNFA.addEdge(newEdge);
				}
			}

		}
		
		/* Add the new initial vertex */
		NFAVertexND newInitialVertex = new NFAVertexND("q0");
		while (resultNFA.containsVertex(newInitialVertex)) {
			newInitialVertex = new NFAVertexND("q" + i);
			i++;
		}
		resultNFA.addVertex(newInitialVertex);
		resultNFA.setInitialState(newInitialVertex);
		NFAVertexND oldInitialVertex = m1.getInitialState();
		resultNFA.addEdge(new NFAEdge(newInitialVertex, oldInitialVertex, new EpsilonTransitionLabel("ε1")));
		
		/* Adding the accept states */
		for (NFAVertexND v : m2.getAcceptingStates()) {
			v = stateMap.get(v);
			resultNFA.addAcceptingState(v);
		}
		return resultNFA;
	}

	@Override
	public NFAGraph starNFA(NFAGraph m, RegexStarOperator starOperator) {
		QuantifierType quantifierType = starOperator.getQuantifierType();
		TransitionLabel loopBackTransitionLabel;
		TransitionLabel finishTransitionLabel;
		switch (quantifierType) {
		case GREEDY:
			loopBackTransitionLabel = new EpsilonTransitionLabel("ε1");
			finishTransitionLabel = new EpsilonTransitionLabel("ε2");
			break;
		case RELUCTANT:
			loopBackTransitionLabel = new EpsilonTransitionLabel("ε2");
			finishTransitionLabel = new EpsilonTransitionLabel("ε1");
			break;
		case POSSESSIVE:
			throw new UnsupportedOperationException("Possessive quantifiers not implemented.");
			//break;
		default:
			throw new RuntimeException("Unkown quantifier: " + quantifierType);
		}
		 
		
		NFAGraph resultNFA = new NFAGraph();
		/* Adding the vertices of m1 */
		for (NFAVertexND v : m.vertexSet()) {
			resultNFA.addVertex(v);
		}
		
		for (NFAEdge e : m.edgeSet()) {
			NFAVertexND source = e.getSourceVertex();
			NFAVertexND target = e.getTargetVertex();
			
			resultNFA.addEdge(new NFAEdge(source, target, e.getTransitionLabel()));
		}
		
		NFAVertexND mInitialState = m.getInitialState();
		
		/* Setting the new initial state */
		if (m.getAcceptingStates().size() != 1) {
			throw new RuntimeException("Star construction only allows for one accept state");
		}
		NFAVertexND mAcceptState = m.getAcceptingStates().iterator().next();
		resultNFA.setInitialState(mAcceptState);
		
		/* Adding the loop back connecting edge */
		resultNFA.addEdge(new NFAEdge(mAcceptState, mInitialState, loopBackTransitionLabel));
		
		/* Add the new accepting vertex */
		String newName = "q";
		int i = 0;
		NFAVertexND newAcceptVertex = new NFAVertexND("q" + resultNFA.vertexSet().size());
		while (resultNFA.containsVertex(newAcceptVertex)) {
			newAcceptVertex = new NFAVertexND(newName + i);
			i++;
		}
		resultNFA.addVertex(newAcceptVertex);
		resultNFA.addAcceptingState(newAcceptVertex);
		
		/* Adding the finish connecting edge */
		resultNFA.addEdge(new NFAEdge(mAcceptState, newAcceptVertex, finishTransitionLabel));
		return resultNFA;
	}

	@Override
	public NFAGraph plusNFA(NFAGraph m, RegexPlusOperator plusOperator) {
		QuantifierType quantifierType = plusOperator.getQuantifierType();
		TransitionLabel loopBackTransitionLabel;
		TransitionLabel finishTransitionLabel;
		switch (quantifierType) {
		case GREEDY:
			loopBackTransitionLabel = new EpsilonTransitionLabel("ε1");
			finishTransitionLabel = new EpsilonTransitionLabel("ε2");
			break;
		case RELUCTANT:
			loopBackTransitionLabel = new EpsilonTransitionLabel("ε2");
			finishTransitionLabel = new EpsilonTransitionLabel("ε1");
			break;
		case POSSESSIVE:
			throw new UnsupportedOperationException("Possessive quantifiers not implemented.");
			//break;
		default:
			throw new RuntimeException("Unkown quantifier: " + quantifierType);
		}
		 
		
		NFAGraph resultNFA = new NFAGraph();
		/* Adding the vertices of m */
		for (NFAVertexND v : m.vertexSet()) {
			resultNFA.addVertex(v);
		}
		
		for (NFAEdge e : m.edgeSet()) {
			NFAVertexND source = e.getSourceVertex();
			NFAVertexND target = e.getTargetVertex();
			
			resultNFA.addEdge(new NFAEdge(source, target, e.getTransitionLabel()));
		}
		
		NFAVertexND mInitialState = m.getInitialState();
		
		/* Adding the new initial state */
		NFAVertexND newInitialState = deriveVertex(resultNFA, new NFAVertexND("q0"));
		resultNFA.addVertex(newInitialState);
		resultNFA.setInitialState(newInitialState);
		/* Adding the initial connecting edge */
		resultNFA.addEdge(new NFAEdge(newInitialState, mInitialState, new EpsilonTransitionLabel("ε1")));
		
		if (m.getAcceptingStates().size() != 1) {
			throw new RuntimeException("Plus construction only allows for one accept state");
		}
		NFAVertexND mAcceptState = m.getAcceptingStates().iterator().next();
		/* Adding the loop back connecting edge */
		resultNFA.addEdge(new NFAEdge(mAcceptState, newInitialState, loopBackTransitionLabel));
		
		/* Add the new accepting vertex */
		NFAVertexND newAcceptVertex = deriveVertex(resultNFA, new NFAVertexND("q" + resultNFA.vertexSet().size()));
		resultNFA.addVertex(newAcceptVertex);
		resultNFA.addAcceptingState(newAcceptVertex);
		
		/* Adding the finish connecting edge */
		resultNFA.addEdge(new NFAEdge(mAcceptState, newAcceptVertex, finishTransitionLabel));
		return resultNFA;
	}

	@Override
	public NFAGraph countClosureNFA(NFAGraph m, RegexCountClosureOperator countClosureOperator) {
		QuantifierType quantifierType = countClosureOperator.getQuantifierType();
		int cmin = countClosureOperator.getLow();
		int cmax = countClosureOperator.getHigh();
		boolean bounded = cmax < MAX_REPETITION;
		TransitionLabel continueTransitionLabel;
		TransitionLabel finishTransitionLabel;
		switch (quantifierType) {
		case GREEDY:
			continueTransitionLabel = new EpsilonTransitionLabel("ε1");
			finishTransitionLabel = new EpsilonTransitionLabel("ε2");
			break;
		case RELUCTANT:
			continueTransitionLabel = new EpsilonTransitionLabel("ε2");
			finishTransitionLabel = new EpsilonTransitionLabel("ε1");
			break;
		case POSSESSIVE:
			throw new UnsupportedOperationException("Possessive quantifiers not implemented.");
		default:
			throw new RuntimeException("Unkown quantifier: " + quantifierType);
		}		 
		
		NFAGraph resultNFA = new NFAGraph();
		/* Add the new initial vertex */
		NFAVertexND newInitialVertex = deriveVertex(resultNFA, new NFAVertexND("q0"));
		resultNFA.addVertex(newInitialVertex);
		resultNFA.setInitialState(newInitialVertex);
		
		/* Add the new accept vertex */
		NFAVertexND newAcceptVertex = deriveVertex(resultNFA, new NFAVertexND("q" + resultNFA.vertexSet().size()));
		resultNFA.addVertex(newAcceptVertex);
		resultNFA.addAcceptingState(newAcceptVertex);
		if (cmin == 0) {
			if (cmax > 0) {
				NFAEdge newEdge = new NFAEdge(newInitialVertex, newAcceptVertex, finishTransitionLabel);
				resultNFA.addEdge(newEdge);
			} else {
				/* if cmin=0 and cmax=0 have an epsilon transition to the accept state */
				NFAEdge newEdge = new NFAEdge(newInitialVertex, newAcceptVertex, new EpsilonTransitionLabel("ε1"));
				resultNFA.addEdge(newEdge);
			}
			
		}
		
		int numRepetitions = bounded ? cmax : cmin;
		/* Having no E transitions on an unbounded count closure makes no sense */
		if (!bounded && cmin == 0) {
			numRepetitions = 1;
		}
		NFAVertexND lastConnectingVertex = newInitialVertex;
		for (int i = 1; i <= numRepetitions; i++) {
			/* Adding the vertices of m */
			NFAVertexND repetitionInitialVertex = null;
			NFAVertexND repetitionAcceptVertex = null;
			HashMap<NFAVertexND, NFAVertexND> stateMap = new HashMap<NFAVertexND, NFAVertexND>();
			for (NFAVertexND originalVertex : m.vertexSet()) {
				NFAVertexND newVertex = deriveVertex(resultNFA, originalVertex);
				stateMap.put(originalVertex, newVertex);
				resultNFA.addVertex(newVertex);
				if (m.getInitialState().equals(originalVertex)) {
					repetitionInitialVertex = newVertex;
				}
				if (m.isAcceptingState(originalVertex)) {
					repetitionAcceptVertex = newVertex;
				}				
			}
			assert repetitionInitialVertex != null && repetitionAcceptVertex != null : "NFA must have an initial and accept state.";
			
			/* Adding the edges of m */
			for (NFAEdge e : m.edgeSet()) {
				NFAVertexND source = stateMap.get(e.getSourceVertex());
				NFAVertexND target = stateMap.get(e.getTargetVertex());
				NFAEdge newEdge = new NFAEdge(source, target, e.getTransitionLabel());
				resultNFA.addEdge(newEdge);
			}
			
			/* Adding connecting edges */
			NFAEdge newEdge = new NFAEdge(lastConnectingVertex, repetitionInitialVertex, continueTransitionLabel);
			resultNFA.addEdge(newEdge);
			if (i >= cmin) {
				if (!bounded) {
					newEdge = new NFAEdge(repetitionAcceptVertex, lastConnectingVertex, continueTransitionLabel);
					resultNFA.addEdge(newEdge);					
				}
				if ((bounded || cmin != 0) && i == numRepetitions) {
					/* The only transition will be to the accept vertex so it must have highest priority. */
					newEdge = new NFAEdge(repetitionAcceptVertex, newAcceptVertex, new EpsilonTransitionLabel("ε1"));
					resultNFA.addEdge(newEdge);	
				} else if (i < numRepetitions) {
					newEdge = new NFAEdge(repetitionAcceptVertex, newAcceptVertex, finishTransitionLabel);
					resultNFA.addEdge(newEdge);	
				}
							
			}
			lastConnectingVertex = repetitionAcceptVertex;
			
		}		

		return resultNFA;
	}

	@Override
	public NFAGraph questionMarkNFA(NFAGraph m, RegexQuestionMarkOperator questionMarkOperator) {
		NFAGraph resultNFA = new NFAGraph();
		QuantifierType quantifierType = questionMarkOperator.getQuantifierType();
		TransitionLabel continueTransitionLabel;
		TransitionLabel finishTransitionLabel;
		switch (quantifierType) {
		case GREEDY:
			continueTransitionLabel = new EpsilonTransitionLabel("ε1");
			finishTransitionLabel = new EpsilonTransitionLabel("ε2");
			break;
		case RELUCTANT:
			continueTransitionLabel = new EpsilonTransitionLabel("ε2");
			finishTransitionLabel = new EpsilonTransitionLabel("ε1");
			break;
		case POSSESSIVE:
			throw new UnsupportedOperationException("Possessive quantifiers not implemented.");
		default:
			throw new RuntimeException("Unkown quantifier: " + quantifierType);
		}
		assert m.getAcceptingStates().size() == 1 : "Construction assumes only one accept state";
		NFAVertexND mAcceptState = m.getAcceptingStates().iterator().next();
		/* Adding the vertices of m1 */
		for (NFAVertexND v : m.vertexSet()) {
			resultNFA.addVertex(v);
		}
		
		for (NFAEdge e : m.edgeSet()) {
			NFAVertexND source = e.getSourceVertex();
			NFAVertexND target = e.getTargetVertex();
			
			resultNFA.addEdge(new NFAEdge(source, target, e.getTransitionLabel()));
		}

		
		/* Add the new initial vertex */
		String newName = "q";
		int i = 0;
		NFAVertexND newInitialVertex = new NFAVertexND("q0");
		while (resultNFA.containsVertex(newInitialVertex)) {
			newInitialVertex = new NFAVertexND(newName + i);
			i++;
		}
		resultNFA.addVertex(newInitialVertex);
		resultNFA.setInitialState(newInitialVertex);
		
		/* Add the connecting edges */
		NFAVertexND m1InitialState = m.getInitialState();
		resultNFA.addEdge(new NFAEdge(newInitialVertex, m1InitialState, continueTransitionLabel));
		resultNFA.addEdge(new NFAEdge(newInitialVertex, mAcceptState, finishTransitionLabel));
		
		resultNFA.addAcceptingState(mAcceptState);
		
		return resultNFA;
	}
}
