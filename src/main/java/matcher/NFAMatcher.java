package matcher;

import java.util.*;

import nfa.*;
import nfa.transitionlabel.*;

public abstract class NFAMatcher implements MyMatcher {
	
	private final NFAGraph nfaGraph;
	private final String inputString;
	private final int inputStringLength;

	protected NFAMatcher(NFAGraph nfaGraph, String inputString) {
		this.nfaGraph = nfaGraph;
		this.inputString = inputString;
		this.inputStringLength = inputString.length();
	}

	public boolean matches() {
			
		NFAVertexND initialState = nfaGraph.getInitialState();
		HashMap<NFAEdge, Integer> transitionToNumTraversedMap = new HashMap<NFAEdge, Integer>();
		return matchingDFS(initialState, 0, transitionToNumTraversedMap);
	}

	private boolean matchingDFS(NFAVertexND currentState, int inputStringPosition, HashMap<NFAEdge, Integer> transitionToNumTraversedMap) {

		if (nfaGraph.isAcceptingState(currentState) && inputStringPosition == inputStringLength) {
			return true;
		}

		Set<NFAEdge> outgoingEdges = nfaGraph.outgoingEdgesOf(currentState);
		List<NFAEdge> sortedOutgoingEdges = new LinkedList<NFAEdge>(outgoingEdges);
		Collections.sort(sortedOutgoingEdges);

		for (NFAEdge outgoingEdge : sortedOutgoingEdges) {
			NFAVertexND targetState = outgoingEdge.getTargetVertex();
			int currentTimesTraversed = 0;
			if (transitionToNumTraversedMap.containsKey(outgoingEdge)) {
				currentTimesTraversed = transitionToNumTraversedMap.get(outgoingEdge);
				
			}
			
			if (currentTimesTraversed < outgoingEdge.getNumParallel()) {
				if (outgoingEdge.getIsEpsilonTransition()) {
					transitionToNumTraversedMap.put(outgoingEdge, currentTimesTraversed + 1);
					boolean foundMatch = matchingDFS(targetState, inputStringPosition, transitionToNumTraversedMap);
					if (foundMatch) {
						return true;
					}
					/* If we didn't find a match, backtrack and remove this edge */
					transitionToNumTraversedMap.put(outgoingEdge, currentTimesTraversed);
				} else {
					TransitionLabel transitionLabel = outgoingEdge.getTransitionLabel();
					if (inputStringPosition < inputStringLength) {
						if (transitionLabel.matches("" + inputString.charAt(inputStringPosition))) {
							//System.out.println("matched: " + inputString.charAt(inputStringPosition) + " with " + transitionLabel);
							transitionToNumTraversedMap.put(outgoingEdge, currentTimesTraversed + 1);
							boolean foundMatch = matchingDFS(targetState, inputStringPosition + 1, new HashMap<NFAEdge, Integer>());
							if (foundMatch) {
								return true;
							}
							/* If we didn't find a match, backtrack and remove this edge */
							transitionToNumTraversedMap.put(outgoingEdge, currentTimesTraversed);
						}
					}
				}
			}
		}
		return false;
		
	}



}
