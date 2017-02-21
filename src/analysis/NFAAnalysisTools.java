package analysis;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.jgrapht.alg.StrongConnectivityInspector;
import org.jgrapht.graph.DirectedSubgraph;

import nfa.NFAGraph;
import nfa.NFAVertexND;
import nfa.UPNFAState;
import nfa.NFAEdge;
import nfa.FilterEdge;
import nfa.transitionlabel.TransitionLabel;
import nfa.transitionlabel.CharacterClassTransitionLabel;
import nfa.transitionlabel.EmptyTransitionLabelException;


public class NFAAnalysisTools {

	/* ============= GENERAL TOOLS ============= */

	/**
	 * Modifies an NFA graph so that it can be used with the Mohri filter.
	 * 
	 * @param m
	 *            The NFA graph to modify
	 * @param modifyLabel
	 *            The label to assign to the current epsilon transitions.
	 * @param selfloopLabel
	 *            The label to assign to the selfloops added to all states.
	 */
	public static void prepareForFilter(NFAGraph m, String modifyLabel, String selfloopLabel) {
		for (NFAVertexND v : m.vertexSet()) {
			/* changing current epsilon transitions to modifyLabel */
			for (NFAEdge e : m.outgoingEdgesOf(v)) {
				if (e.getIsEpsilonTransition()) {
					e.setTransitionLabel(modifyLabel);
				}
			}
			/* Adding the self loop */
			try {
				m.addEdge(new NFAEdge(v, v, selfloopLabel));
			} catch (EmptyTransitionLabelException e1) {
				throw new RuntimeException("Empty transition label");
			}
		}
	}

	/**
	 * Creates an NFA graph representing the Mohri filter.
	 * 
	 * @return the NFA graph
	 */
	public static NFAGraph createFilter() {
		NFAGraph mohriFilter = new NFAGraph();
		NFAVertexND v0 = new NFAVertexND(0);
		NFAVertexND v1 = new NFAVertexND(1);
		NFAVertexND v2 = new NFAVertexND(2);

		mohriFilter.addVertex(v0); /* state 0 */
		mohriFilter.addVertex(v1); /* state 1 */
		mohriFilter.addVertex(v2); /* state 2 */

		try {
			mohriFilter.addEdge(new FilterEdge(v0, v0, "ε2", "ε1"));
			mohriFilter.addEdge(new FilterEdge(v0, v0, "x", "x"));
			mohriFilter.addEdge(new FilterEdge(v0, v1, "ε1", "ε1"));
			mohriFilter.addEdge(new FilterEdge(v0, v2, "ε2", "ε2"));

			mohriFilter.addEdge(new FilterEdge(v1, v1, "ε1", "ε1"));
			mohriFilter.addEdge(new FilterEdge(v1, v0, "x", "x"));

			mohriFilter.addEdge(new FilterEdge(v2, v2, "ε2", "ε2"));
			mohriFilter.addEdge(new FilterEdge(v2, v0, "x", "x"));
			
		} catch (EmptyTransitionLabelException e1) {
			throw new RuntimeException("Empty transition label");
		}
		

		mohriFilter.setInitialState(v0);

		mohriFilter.addAcceptingState(v0);
		mohriFilter.addAcceptingState(v1);
		mohriFilter.addAcceptingState(v2);

		return mohriFilter;
	}

	public static NFAGraph productConstructionAFB(NFAGraph a, NFAGraph b) throws InterruptedException {
		NFAGraph m1 = a.copy();
		NFAGraph m2 = b.copy();
		NFAGraph f = NFAAnalysisTools.createFilter();
		NFAAnalysisTools.prepareForFilter(m1, "ε2", "ε1");
		NFAAnalysisTools.prepareForFilter(m2, "ε1", "ε2");
		HashMap<NFAEdge, TransitionLabel> originalWords = new HashMap<NFAEdge, TransitionLabel>();
		NFAGraph af = NFAAnalysisTools.productConstruction(m1, f, originalWords);		
		return NFAAnalysisTools.productConstruction(af, m2, originalWords);
	}
	

	/**
	 * Calculates the product construction of a graph using the the Mohri
	 * filter.
	 * 
	 * @param m
	 *            The NFA to get the product construction of.
	 * @return The NFA representing the product construction.
	 */
	public static NFAGraph productConstructionAFA(NFAGraph m) throws InterruptedException {		
		return NFAAnalysisTools.productConstructionAFB(m, m);
	}
	
	public static NFAGraph productConstructionAFAFA(NFAGraph m) throws InterruptedException {
		NFAGraph m1 = m.copy();
		NFAGraph m2 = m.copy();
		NFAGraph f = NFAAnalysisTools.createFilter();
		NFAAnalysisTools.prepareForFilter(m1, "ε2", "ε1");
		NFAAnalysisTools.prepareForFilter(m2, "ε1", "ε2");
		HashMap<NFAEdge, TransitionLabel> originalWords = new HashMap<NFAEdge, TransitionLabel>();
		NFAGraph af = NFAAnalysisTools.productConstruction(m1, f, originalWords);
		NFAGraph afa = NFAAnalysisTools.productConstruction(af, m2, originalWords);
		/*
		 * Changing the existing epsilon transitions and adding the epsilon self
		 * loops
		 */
		NFAAnalysisTools.prepareForFilter(afa, "ε2", "ε1");

		NFAGraph afaf = NFAAnalysisTools.productConstruction(afa, f, originalWords);
		NFAGraph afafa = NFAAnalysisTools.productConstruction(afaf, m2, originalWords);

		return afafa;
	}

	public static NFAGraph productConstruction(NFAGraph m1, NFAGraph m2, HashMap<NFAEdge, TransitionLabel> originalWords) throws InterruptedException {
		NFAGraph productConstruction = new NFAGraph();

		NFAVertexND m1SourceState, m2SourceState;
		m1SourceState = m1.getInitialState();
		m2SourceState = m2.getInitialState();

		int m1Dimensions = m1SourceState.getNumDimensions();
		int m2Dimensions = m2SourceState.getNumDimensions();
		NFAVertexND firstVertex = new NFAVertexND(m1SourceState, m2SourceState);

		LinkedList<NFAVertexND> toVisit = new LinkedList<NFAVertexND>();
		/* Adding the initial state */
		toVisit.add(firstVertex);
		productConstruction.addVertex(firstVertex);

		productConstruction.setInitialState(firstVertex);
		while (!toVisit.isEmpty()) {
			if (Thread.currentThread().isInterrupted()) {
				throw new InterruptedException();
			}
			NFAVertexND sourceVertex = toVisit.poll();
			m1SourceState = sourceVertex.getStateByDimensionRange(1, 1 + m1Dimensions);
			m2SourceState = sourceVertex.getStateByDimensionRange(1 + m1Dimensions, 1 + m1Dimensions + m2Dimensions);
			/* see if the current vertex is accepting */
			if (m1.isAcceptingState(m1SourceState) && m2.isAcceptingState(m2SourceState)) {
				productConstruction.addAcceptingState(sourceVertex);
			}
			
			for (NFAEdge currentM1Edge : m1.outgoingEdgesOf(m1SourceState)) {
				if (Thread.currentThread().isInterrupted()) {
					throw new InterruptedException();
				}

				int m1NumParallel = currentM1Edge.getNumParallel();

				NFAVertexND m1TargetState = currentM1Edge.getTargetVertex();
				TransitionLabel word = currentM1Edge.getTransitionLabel();
				
				TransitionLabel originalWord = word;
				if (originalWords.containsKey(currentM1Edge)) {
					/* This edge changed the word, find it's original value */
					originalWord = originalWords.get(currentM1Edge);
				}
				
				for (NFAEdge currentM2Edge : m2.outgoingEdgesOf(m2SourceState)) {
					if (Thread.currentThread().isInterrupted()) {
						throw new InterruptedException();
					}
					if (!currentM2Edge.isTransitionFor(word)) {
						/* current edge can't handle word */
						continue;
					}
					
					
					int m2NumParallel = currentM2Edge.getNumParallel();
					NFAVertexND m2TargetState = currentM2Edge.getTargetVertex();

					NFAVertexND targetVertex = new NFAVertexND(m1TargetState, m2TargetState);
					/* ensure each state is only visited once */
					if (!productConstruction.containsVertex(targetVertex)) {
						toVisit.add(targetVertex);
						productConstruction.addVertex(targetVertex);
					}
					
					NFAEdge newEdge = new NFAEdge(sourceVertex, targetVertex, originalWord);
					
					if (isFilterEdge(currentM2Edge)) {
						/*
						 * swap out the current character for the filter's
						 * output character
						 */
						FilterEdge fEdge = (FilterEdge) currentM2Edge;
						if (fEdge.getIsEpsilonTransition()) {
							/*
							 * Storing the original name of the edge in the
							 * outgoing transition character
							 */
							newEdge.setTransitionLabel(fEdge.getOutGoingTransitionCharacter());
							originalWords.put(newEdge, originalWord);
						}
					} else {
						
						if (!currentM2Edge.getIsEpsilonTransition()) {
							
							TransitionLabel tl2 = currentM2Edge.getTransitionLabel();
							TransitionLabel intersection = originalWord.intersection(tl2);
							newEdge.setTransitionLabel(intersection);
						}
					}
					newEdge.setNumParallel(m1NumParallel * m2NumParallel);
					productConstruction.addEdge(newEdge);

				}

			}
		}
		return productConstruction;
	}

	/* Trims away states not reachable form start */
	public static NFAGraph makeTrimFromStart(NFAGraph m)  throws InterruptedException {
		NFAGraph trimmed = m.copy();
		NFAVertexND mInitialVertex = m.getInitialState();
		Set<NFAVertexND> vSet = m.vertexSet();
		HashSet<NFAVertexND> usefulStates = new HashSet<NFAVertexND>();
		makeTrimFromStartDFS(m, mInitialVertex, usefulStates);

		for (NFAVertexND currentVertex : vSet) {
			if (!usefulStates.contains(currentVertex)) {
				trimmed.removeVertex(currentVertex);
			}
		}
		return trimmed;
	}

	private static void makeTrimFromStartDFS(NFAGraph m, NFAVertexND currentVertex, HashSet<NFAVertexND> usefulStates) {
		usefulStates.add(currentVertex);
		for (NFAEdge e : m.outgoingEdgesOf(currentVertex)) {
			NFAVertexND target = e.getTargetVertex();
			if (!usefulStates.contains(target)) {
				makeTrimFromStartDFS(m, target, usefulStates);
			}
		}
	}

	/**
	 * Removes all useless states from an NFA graph. This is done recursively by
	 * determining whether each vertex is connected to a useful vertex,
	 * recursively.
	 * 
	 * @param m
	 *            The NFA graph to remove all useless states from.
	 * @return The trimmed graph.
	 */
	public static NFAGraph makeTrimAlternative(NFAGraph m) throws InterruptedException {
		NFAGraph trimmed = m.copy();
		Set<NFAVertexND> vSet = m.vertexSet();
		LinkedList<NFAVertexND> toRemove = new LinkedList<NFAVertexND>();
		HashSet<NFAVertexND> usefulStates = new HashSet<NFAVertexND>();
		for (NFAVertexND acceptingState : m.getAcceptingStates()) {
			usefulStates.add(acceptingState);
		}
		for (NFAVertexND currentVertex : vSet) {
			if (Thread.currentThread().isInterrupted()) {
				throw new InterruptedException();
			}
			
			if (!NFAAnalysisTools.makeTrimIsUseful(trimmed, currentVertex, new HashSet<NFAVertexND>(), usefulStates)) {
				/* We do not want to remove the initial state */
				if (!m.getInitialState().equals(currentVertex)) {
					toRemove.add(currentVertex);
				}
			} else {
				usefulStates.add(currentVertex);
			}
		}

		for (NFAVertexND currentVertex : toRemove) {
			trimmed.removeVertex(currentVertex);
		}
		return trimmed;
	}

	/**
	 * A function used recursively to determine whether a state is useful.
	 * 
	 * @param m
	 *            The graph containing the state
	 * @param currentVertex
	 *            The current state being considered.
	 * @param visited
	 *            A map containing all visited states to prevent loops.
	 * @return True if the state is useful, false if not.
	 */
	static boolean makeTrimIsUseful(NFAGraph m, NFAVertexND currentVertex, HashSet<NFAVertexND> visited, HashSet<NFAVertexND> usefulStates) {

		if (usefulStates.contains(currentVertex)) {
			/* the current vertex is useful */
			return true;
		}

		if (visited.contains(currentVertex)) {
			/* The current vertex is not useful, and has been visited before. */
			return false;
		} else {
			visited.add(currentVertex);
		}
		boolean result = false;

		for (NFAEdge currentEdge : m.outgoingEdgesOf(currentVertex)) {
			/* See if any of the adjacent vertices are useful */
			result |= makeTrimIsUseful(m, currentEdge.getTargetVertex(), visited, usefulStates);
			if (result) {
				return true;
			}
		}

		return result;
	}
	
	public static NFAGraph makeTrimUPNFA(NFAGraph m, NFAGraph upnfa) throws InterruptedException {
		HashSet<NFAVertexND> usefulStates = new HashSet<NFAVertexND>();
		
		for (NFAVertexND v : upnfa.vertexSet()) {
			UPNFAState upNFAState = (UPNFAState) v;
			
			if (upNFAStateIsUseful(m, upnfa, upNFAState)) {
				usefulStates.add(upNFAState);
			}
			
		}
		
		NFAGraph trimmedUPNFA = upnfa.copy();
		for (NFAVertexND v : upnfa.vertexSet()) {
			if (!usefulStates.contains(v)) {
				if (trimmedUPNFA.isAcceptingState(v)) {
					trimmedUPNFA.removeAcceptingState(v);
				}
				trimmedUPNFA.removeVertex(v);
			}
		}
		
		return trimmedUPNFA;
	}
	
	public static boolean upNFAStateIsUseful(NFAGraph m, NFAGraph upnfa, UPNFAState upNFAState) throws InterruptedException {	
		HashSet<TransitionLabel> alphabet = new HashSet<TransitionLabel>();
		alphabet.add(CharacterClassTransitionLabel.wildcardLabel());
		HashSet<NFAVertexND> P = (HashSet<NFAVertexND>) upNFAState.getP();
		TransitionLabel higherPrioritySymbols = new CharacterClassTransitionLabel();
		boolean containsAcceptState = false;
		for (NFAVertexND p : P) {
			if (m.isAcceptingState(p)) {				
				containsAcceptState = true;
			}
			Set<NFAEdge> outgoingEdges = m.outgoingEdgesOf(p);
			for (NFAEdge e : outgoingEdges) {
				if (!e.getIsEpsilonTransition()) {
					higherPrioritySymbols = higherPrioritySymbols.union(e.getTransitionLabel());
				}						
			}
		}
		
		if (!containsAcceptState || !higherPrioritySymbols.complement().isEmpty()) {
			return true;
		} else {
			Iterator<NFAVertexND> i0 = P.iterator();
			NFAVertexND p = i0.next();
			HashSet<NFAVertexND> reachableFromStart = new HashSet<NFAVertexND>();
			reachableFromStart.addAll(P);
			NFAGraph intersectionDfa = NFAAnalysisTools.determinize(m, reachableFromStart, alphabet);
			intersectionDfa = complementDfa(intersectionDfa);
			
			/* Try to reach an accept state */
			Stack<NFAVertexND> toVisit = new Stack<NFAVertexND>();
			HashSet<NFAVertexND> visited = new HashSet<NFAVertexND>();
			toVisit.push(intersectionDfa.getInitialState());
			while (!toVisit.isEmpty()) {
				NFAVertexND currentState = toVisit.pop();
				
				if (intersectionDfa.isAcceptingState(currentState)) {
					return true;
				} else {
					Set<NFAEdge> outgoingEdges = intersectionDfa.outgoingEdgesOf(currentState);
					for (NFAEdge e : outgoingEdges) {
						NFAVertexND targetVertex = e.getTargetVertex();
						if (!visited.contains(targetVertex)) {
							toVisit.push(targetVertex);
							visited.add(targetVertex);
						}							
					}
					
				}
			}
		}
		return false;
	}
	
	public static NFAGraph complementDfa(NFAGraph dfa) {
		NFAGraph resultGraph = dfa.copy();
		/* swapping final and nonfinal states */
		for (NFAVertexND currentState : dfa.vertexSet()) {
			if (dfa.isAcceptingState(currentState)) {
				resultGraph.removeAcceptingState(currentState);
			} else {
				resultGraph.addAcceptingState(currentState);
			}
		}
		return resultGraph;
		
	}


	/**
	 * Removes all useless states from an NFA graph. This is done by reversing
	 * the graph and determining which states are reachable from the final
	 * state. Typically this method was found to be slower than the other
	 * makeTrim method.
	 * 
	 * @param m
	 *            The NFA graph to remove all useless states from.
	 * @return The trimmed graph.
	 * @throws InterruptedException 
	 */
	public static NFAGraph makeTrim(NFAGraph m) throws InterruptedException {
		NFAGraph trimmed = m.copy();
		NFAGraph reversedGraph = m.reverse();

		HashSet<NFAVertexND> usefulStates = makeTrimReachable(reversedGraph, reversedGraph.getAcceptingStates());

		for (NFAVertexND v : m.vertexSet()) {
			if (!usefulStates.contains(v)) {
				/* We do not want to remove the initial state (even if it is useless) */
				if (!m.getInitialState().equals(v)) {
					trimmed.removeVertex(v);
				}
			}
		}

		return trimmed;
	}
	
	private static HashSet<NFAVertexND> makeTrimReachable(NFAGraph reversedGraph, Set<NFAVertexND> defaultUsefulStates) throws InterruptedException {
		
		HashSet<NFAVertexND> usefulStates = new HashSet<NFAVertexND>();
		LinkedList<NFAVertexND> toVisit = new LinkedList<NFAVertexND>();

		for (NFAVertexND defaultUsefulState : defaultUsefulStates) {
			toVisit.add(defaultUsefulState);
		}
		while (!toVisit.isEmpty()) {
			if (isInterrupted()) {
				throw new InterruptedException();
			}
			NFAVertexND currentVertex = toVisit.pop();
			usefulStates.add(currentVertex);
			for (NFAEdge outGoingEdge : reversedGraph.outgoingEdgesOf(currentVertex)) {
				NFAVertexND targetVertex = outGoingEdge.getTargetVertex();
				if (!usefulStates.contains(targetVertex) && !toVisit.contains(targetVertex)) {
					toVisit.push(targetVertex);

				}
			}
		}
		
		return usefulStates;
	}
	
	public static NFAGraph convertUpNFAToNFAGraph(NFAGraph m, HashMap<NFAVertexND, UPNFAState> newStateMap) {
		HashMap<UPNFAState, NFAVertexND> stateMap = new HashMap<UPNFAState, NFAVertexND>();
		
		int stateCounter = 0;
		NFAGraph resultGraph = new NFAGraph();
		for (NFAVertexND v : m.vertexSet()) {
			NFAVertexND correspondingState = new NFAVertexND("q" + stateCounter);
			stateMap.put((UPNFAState) v, correspondingState);
			newStateMap.put(correspondingState, (UPNFAState) v);
			resultGraph.addVertex(correspondingState);
			if (m.isAcceptingState(v)) {
				resultGraph.addAcceptingState(correspondingState);
			}
			stateCounter++;
		}
		resultGraph.setInitialState(stateMap.get(m.getInitialState()));
		
		for (NFAEdge e : m.edgeSet()) {
			UPNFAState sourceState = (UPNFAState) e.getSourceVertex();
			UPNFAState targetState = (UPNFAState) e.getTargetVertex();
			NFAVertexND newSource = stateMap.get(sourceState);
			NFAVertexND newTarget = stateMap.get(targetState);
			TransitionLabel transitionLabel = e.getTransitionLabel();
			NFAEdge newEdge = new NFAEdge(newSource, newTarget, transitionLabel);
			resultGraph.addEdge(newEdge);
		}
		
		return resultGraph;
	}
	
	

	/**
	 * Constructs a list of NFA graphs each representing a strongly connected
	 * component in the graph given as parameter.
	 * 
	 * @param m
	 *            The NFA graph to find the strongly connected components in.
	 * @return A list containing all the strongly connected components.
	 * @throws InterruptedException 
	 */
	public static LinkedList<NFAGraph> getStronglyConnectedComponents(NFAGraph m) throws InterruptedException {
		StrongConnectivityInspector<NFAVertexND, NFAEdge> sci = new StrongConnectivityInspector<NFAVertexND, NFAEdge>(m);
		List<DirectedSubgraph<NFAVertexND, NFAEdge>> sccs = sci.stronglyConnectedSubgraphs();
		LinkedList<NFAGraph> sccNFAs = new LinkedList<NFAGraph>();

		for (DirectedSubgraph<NFAVertexND, NFAEdge> scc : sccs) {
			if (isInterrupted()) {
				throw new InterruptedException();
			}

			/* scc's consisting of no edges are irrelevant for our purpose */
			if (scc.edgeSet().size() > 0) {

				NFAGraph currentNFAG = new NFAGraph();
				for (NFAVertexND v : scc.vertexSet()) {
					if (isInterrupted()) {
						throw new InterruptedException();
					}
					currentNFAG.addVertex(v);
				}
				for (NFAEdge e : scc.edgeSet()) {
					if (isInterrupted()) {
						throw new InterruptedException();
					}
					currentNFAG.addEdge(e);
				}

				sccNFAs.add(currentNFAG);
			}

		}
		return sccNFAs;
	}

	/**
	 * Determines whether the specified edge is a filter edge.
	 * 
	 * @param e
	 *            The edge specified.
	 * @return True if the edge is a filter edge, false if not.
	 */
	private static boolean isFilterEdge(NFAEdge e) {
		return FilterEdge.class.isAssignableFrom(e.getClass());
	}

	/* ============= EDA TOOLS ============= */

	/**
	 * Constructs a list of NFA graphs each representing a strongly connected
	 * component containing only epsilon transitions in the graph given as
	 * parameter.
	 * 
	 * @param m
	 *            The NFA graph to find the strongly connected components in.
	 * @return A list containing all the strongly connected components, with
	 *         only epsilon transitions between the states.
	 * @throws InterruptedException 
	 */
	public static LinkedList<NFAGraph> getEpsilonStronglyConnectedComponents(NFAGraph m) throws InterruptedException {
		NFAGraph epsilonGraph = m.copy();

		/* iterating over m's edge set so we can modify epsilonGraph's edges */
		for (NFAEdge e : m.edgeSet()) {
			/* removing all edges that aren't epsilon transitions */
			if (!e.getIsEpsilonTransition()) {
				epsilonGraph.removeEdge(e);
			}
		}

		return getStronglyConnectedComponents(epsilonGraph);
	}

	/**
	 * Creates a single state for each strongly connected component.
	 * 
	 * @param m
	 *            The NFA graph to merge the epsilon strongly connected
	 *            components in.
	 * @param epsilon
	 *            True if only epsilon strongly connected components should be
	 *            merged, false if all strongly connected components should be
	 *            merged.
	 * @return A HashMap containing the merged states as key and the original
	 *         escc as value.
	 * @throws InterruptedException 
	 */
	public static Map<NFAVertexND, NFAGraph> mergeStronglyConnectedComponents(NFAGraph m, boolean epsilon) throws InterruptedException {
		Map<NFAVertexND, NFAGraph> mergedStates = new HashMap<NFAVertexND, NFAGraph>();

		LinkedList<NFAGraph> sccs;
		if (epsilon) {
			sccs = getEpsilonStronglyConnectedComponents(m);
		} else {
			sccs = getStronglyConnectedComponents(m);
		}
		for (NFAGraph scc : sccs) {
			NFAVertexND mergedState = null;
			boolean isAccepting = false;
			boolean isInitial = false;
			LinkedList<NFAEdge> edgesToRestore = new LinkedList<NFAEdge>();
			for (NFAVertexND v : scc.vertexSet()) {
				if (isInterrupted()) {
					throw new InterruptedException();
				}
				if (mergedState == null) {
					mergedState = new NFAVertexND(v.getStateNumberByDimension(1));
				}
				/* to make the merged state also accepting */
				if (m.isAcceptingState(v)) {
					isAccepting = true;
					m.removeAcceptingState(v);
				}
				/* to make the merged state also initial */
				if (m.getInitialState() != null && m.getInitialState().equals(v)) {
					isInitial = true;
				}

				for (NFAEdge e : m.edgesOf(v)) {
					/*
					 * if the edge doesn't come from another vertex in the escc
					 * or if it's a non-epsilon transition. Note that symbol transitions between vertices in the escc will become self loops on the merged state
					 */
					if (!scc.containsEdge(e)) {
						/*
						 * A necessary check for non-epsilon transitions in the
						 * escc
						 */
						NFAVertexND sourceVertex = scc.containsVertex(e.getSourceVertex()) ? mergedState : e.getSourceVertex();

						/*
						 * A necessary check for non-epsilon transitions in the
						 * escc
						 */
						NFAVertexND targetVertex = scc.containsVertex(e.getTargetVertex()) ? mergedState : e.getTargetVertex();

						NFAEdge newEdge = new NFAEdge(sourceVertex, targetVertex, e.getTransitionLabel());
						newEdge.setNumParallel(e.getNumParallel());
						edgesToRestore.add(newEdge);
					}
				}
				m.removeVertex(v);
			}
			m.addVertex(mergedState);
			if (isInitial) {
				m.setInitialState(mergedState);
			}
			if (isAccepting) {
				m.addAcceptingState(mergedState);
			}
			for (NFAEdge e : edgesToRestore) {
				m.addEdge(e);
			}
			mergedStates.put(mergedState, scc);

		}

		return mergedStates;
	}

	/**
	 * Determines the number of walks between a vertex and all other vertices in
	 * a given graph.
	 * 
	 * @param m
	 *            The graph to count the walks in.
	 * @param s
	 *            The starting vertex.
	 * @return A HashMap containing every destination vertex as key and the
	 *         number of walks to this vertex as value.
	 */
	public static HashMap<NFAVertexND, Integer> numWalksFrom(NFAGraph m, NFAVertexND s) {
		HashMap<NFAVertexND, Integer> paths = new HashMap<NFAVertexND, Integer>();
		/* initialise all paths */
		for (NFAVertexND v : m.vertexSet()) {
			paths.put(v, 0);
		}
		HashMap<NFAEdge, Integer> visitedEdges = new HashMap<NFAEdge, Integer>();
		/* set the number of times each edge has been visited to 0 */
		for (NFAEdge e : m.edgeSet()) {
			visitedEdges.put(e, 0);
		}
		NFAAnalysisTools.numWalksFromSearch(m, s, visitedEdges, paths);
		return paths;
	}

	/**
	 * A function to recursively determine the number of walks between a vertex
	 * and all other vertices in a given graph.
	 * 
	 * @param m
	 *            The graph to count the walks in.
	 * @param current
	 *            The starting vertex.
	 * @param visitedEdges
	 *            A HashMap containing all the edges as key and the current
	 *            number of times they have been visited as value.
	 * @param paths
	 *            A HashMap containing all the vertices as key and the current
	 *            number of walks to them as value.
	 */
	static void numWalksFromSearch(NFAGraph m, NFAVertexND current, HashMap<NFAEdge, Integer> visitedEdges, HashMap<NFAVertexND, Integer> paths) {
		/* update the number of paths to the current vertex */
		paths.put(current, paths.get(current) + 1);

		for (NFAEdge e : m.outgoingEdgesOf(current)) {
			int currentNumVisit = visitedEdges.get(e);
			/* update the number of times this edge has been visited */
			visitedEdges.put(e, currentNumVisit + 1);
			/* for the amount of times the edge can be visited again */
			for (int i = currentNumVisit; i < e.getNumParallel(); i++) {
				/* search from the new vertex */
				numWalksFromSearch(m, e.getTargetVertex(), visitedEdges, paths);
			}
			/* unvisit the current edge */
			visitedEdges.put(e, currentNumVisit);
		}
	}

	public static Set<TransitionLabel> getAlphabet(NFAGraph n) {
		Set<TransitionLabel> regexAlphabet = new HashSet<TransitionLabel>();
		for (NFAEdge e : n.edgeSet()) {
			if (!e.getIsEpsilonTransition()) {
				TransitionLabel tl = e.getTransitionLabel();
				if (tl instanceof CharacterClassTransitionLabel) {
					CharacterClassTransitionLabel cctl = (CharacterClassTransitionLabel) tl;
					if (!regexAlphabet.contains(cctl)) {
						regexAlphabet.add(cctl);
					}
					
				}				
			}
		}
		return regexAlphabet;
	}
	
	/**
	 * This function finds the shortest path from the initial state in the NFA
	 * to a certain finish state.
	 * 
	 * @param m
	 *            The graph representing the NFA.
	 * @param finish
	 *            The state to search to.
	 * @return A linked list containing the edges in the path.
	 */
	public static LinkedList<NFAEdge> shortestPathTo(NFAGraph m, NFAVertexND finish) {
		return shortestPathBetween(m, m.getInitialState(), finish);
	}

	public static LinkedList<NFAEdge> shortestPathBetween(NFAGraph m, NFAVertexND start, NFAVertexND finish) {

		HashMap<NFAVertexND, LinkedList<NFAEdge>> pathToMap = new HashMap<NFAVertexND, LinkedList<NFAEdge>>();
		HashSet<NFAEdge> traversed = new HashSet<NFAEdge>();

		LinkedList<NFAVertexND> queue = new LinkedList<NFAVertexND>();
		NFAVertexND firstVertex = start;
		LinkedList<NFAEdge> emptyPath = new LinkedList<NFAEdge>();
		queue.add(firstVertex);
		pathToMap.put(firstVertex, emptyPath);
		
		while (!queue.isEmpty()) {
			NFAVertexND currentVertex = queue.removeLast();
			LinkedList<NFAEdge> currentPath = pathToMap.get(currentVertex);
			if (currentVertex.equals(finish)) {
				return currentPath;
			}	
			for (NFAEdge e : m.outgoingEdgesOf(currentVertex)) {
				if (!traversed.contains(e)) {
					traversed.add(e);

					NFAVertexND target = e.getTargetVertex();
					LinkedList<NFAEdge> newPath = new LinkedList<NFAEdge>(currentPath);
					newPath.add(e);
					pathToMap.put(target, newPath);

					queue.addFirst(target);
				}
			}
		}

		return null;
	}
	
	public static HashSet<NFAVertexND> reachableWithEpsilon(NFAGraph n, NFAVertexND v) {
		HashSet<NFAVertexND> visited = new HashSet<NFAVertexND>();
		LinkedList<NFAVertexND> toVisit = new LinkedList<NFAVertexND>();
		toVisit.add(v);
		while (!toVisit.isEmpty()) {
			
			NFAVertexND currentVertex = toVisit.removeLast();
			visited.add(currentVertex);

			for (NFAEdge e : n.outgoingEdgesOf(currentVertex)) {
				if (e.getIsEpsilonTransition()) {
					NFAVertexND targetVertex = e.getTargetVertex();
					if (!visited.contains(targetVertex)) {
						toVisit.add(targetVertex);
					}
				}

			}
		}

		return visited;
	}
	
	public static NFAGraph determinize(NFAGraph input, Set<NFAVertexND> reachableFromStart, Set<TransitionLabel> alphabet) throws InterruptedException {
		NFAGraph dfa = new NFAGraph();
		
		/* http://www.cse.unsw.edu.au/~rvg/pub/nfadfa.pdf */
		//NFAVertexND startState = input.getInitialState();
		//HashSet<Integer> reachableFromStart = reachableWithEpsilon(input, startState);
		LinkedList<NFAVertexND> toVisit = new LinkedList<NFAVertexND>();		
		
		LinkedList<NFAVertexND> sortedReachableFromStart = new LinkedList<NFAVertexND>(reachableFromStart);
		Collections.sort(sortedReachableFromStart);
		StringBuilder labelBuilder = new StringBuilder();
		Iterator<NFAVertexND> i0 = sortedReachableFromStart.iterator();
		while (i0.hasNext()) {
			if (isInterrupted()) {
				throw new InterruptedException();
			}
			NFAVertexND startState = i0.next();
			List<String> subStates = startState.getStates();
			if (subStates.size() == 1) {
				String currentLabel = subStates.iterator().next();
				labelBuilder.append(currentLabel);
				
			} else {
				Iterator<String> i1 = subStates.iterator();
				labelBuilder.append("(");
				while (i1.hasNext()) {
					String currentLabel = i1.next();
					labelBuilder.append(currentLabel);
					if (i1.hasNext()) {
						labelBuilder.append(", ");
					}
				}
				labelBuilder.append(")");

			}
			if (i0.hasNext()) {
				labelBuilder.append(", ");
			}
			
		}
		
		
		/* note dfa states are not multidimensional */
		NFAVertexND dfaStartState = new NFAVertexND(labelBuilder.toString());
		toVisit.add(dfaStartState);
		HashMap<NFAVertexND, Set<NFAVertexND>> dfaStateToSubStatesMap = new HashMap<NFAVertexND, Set<NFAVertexND>>();
		dfaStateToSubStatesMap.put(dfaStartState, reachableFromStart);
		
		dfa.addVertex(dfaStartState);
		dfa.setInitialState(dfaStartState);
		for (NFAVertexND i : reachableFromStart) {
			if (isInterrupted()) {
				throw new InterruptedException();
			}
			if (input.isAcceptingState(i)) {
				dfa.addAcceptingState(dfaStartState);
			}
			
		}
		
		NFAVertexND emptyState = new NFAVertexND(0);
		
		
		while (!toVisit.isEmpty()) {
			if (isInterrupted()) {
				throw new InterruptedException();
			}
			
			NFAVertexND P = toVisit.removeLast();
			/* with the label in TransitionLabel, P can get to the states in HashSet<NFAvertexND> */
			HashMap<TransitionLabel, HashSet<NFAVertexND>> newStates = new HashMap<TransitionLabel, HashSet<NFAVertexND>>();
			Set<NFAVertexND> subStates = dfaStateToSubStatesMap.get(P);
			for (NFAVertexND currentSubState : subStates) {
				if (isInterrupted()) {
					throw new InterruptedException();
				}
				for (NFAEdge e : input.outgoingEdgesOf(currentSubState)) {
					if (isInterrupted()) {
						throw new InterruptedException();
					}
					if (!e.getIsEpsilonTransition()) {
						TransitionLabel label = e.getTransitionLabel();
						NFAVertexND targetState = e.getTargetVertex();
						//HashSet<NFAVertexND> newState = newStates.getOrDefault(label, new HashSet<NFAVertexND>());
						HashSet<NFAVertexND> newState;
						if (newStates.containsKey(label)) {
							newState = newStates.get(label);
						} else {
							newState = new HashSet<NFAVertexND>();
						}


						if (!newState.contains(targetState)) {
							newState.add(targetState);
						}
						
						/* If there isn't another outgoing label exactly like this one, check for other overlapping labels */
						if (!newStates.containsKey(label)) {
							/* Copying entries, to avoid concurrent modification errors */
							Set<Map.Entry<TransitionLabel, HashSet<NFAVertexND>>> entries = new HashSet<Map.Entry<TransitionLabel, HashSet<NFAVertexND>>>(newStates.entrySet());
							for (Map.Entry<TransitionLabel, HashSet<NFAVertexND>> kv : entries) {
								if (isInterrupted()) {
									throw new InterruptedException();
								}
								TransitionLabel tl1 = kv.getKey();
							
								TransitionLabel intersection = tl1.intersection(label);
								if (!intersection.isEmpty()) {
									//System.out.println(tl1 + " " + symbol + " " + intersection);
									/* the transition labels over lap. For a DFA we need to 
									 * ensure that all character class labels are disjoint.
									 * We can do this by ensuring the original class (tl1Copy) without the intersecting part of 
									 * the new class (symbol) goes to the original states
									 * the new class (symbol) without the intersecting part of the orignal class (tl1Copy) 
									 * goes to the new states.
									 * the intersection goes to both the classes.
									 *  */
									
									HashSet<NFAVertexND> tmpVertices =  newStates.remove(kv.getKey());
									TransitionLabel uniqueTl1 = tl1.intersection(label.complement());
									if (!uniqueTl1.isEmpty()) {
										//System.out.println("1: " + uniqueTl1 + " " + tmpVertices);
										newStates.put(uniqueTl1, tmpVertices);
									}									
									
								
									HashSet<NFAVertexND> unionStates = new HashSet<NFAVertexND>(tmpVertices);
									unionStates.addAll(newState);
									//System.out.println("2: " + intersection + " " + unionStates);
									/* We know intersection is not empty */
									newStates.put(intersection, unionStates);
									label = label.intersection(tl1.complement());
								}
							
							}
						}
						if (!label.isEmpty()) {
							//System.out.println("3: " + symbol + " " + newState);
							newStates.put(label, newState);
						}
						
					}
				}
			}
			//System.out.println(P + "\t\t" + newStates);
			/* Finding all the ranges in the alphabet not accounted for */
			for (TransitionLabel s : alphabet) {
				if (isInterrupted()) {
					throw new InterruptedException();
				}
				TransitionLabel toEmptyState = s;
				/* for each range accounted for, remove it from the current alphabet range */
				for (TransitionLabel tl : newStates.keySet()) {
					if (isInterrupted()) {
						throw new InterruptedException();
					}
					toEmptyState = toEmptyState.intersection(tl.complement());
					if (toEmptyState.isEmpty()) {
						break;
					}
				}
				if (!toEmptyState.isEmpty()) {
					//System.out.println("check: " + toEmptyState);
					//System.out.println(toEmptyState);
					if (!dfa.containsVertex(emptyState)) {
						dfa.addVertex(emptyState);
						for (TransitionLabel s2 : alphabet) {
							
							dfa.addEdge(new NFAEdge(emptyState, emptyState, s2));
						}
					}
					dfa.addEdge(new NFAEdge(P, emptyState, toEmptyState));
				}
			}
			
			/*for (TransitionLabel s : alphabet) {
				if (!newStates.containsKey(s)) {
					if (!dfa.containsVertex(emptyState)) {
						dfa.addVertex(emptyState);
						for (TransitionLabel s2 : alphabet) {
							dfa.addEdge(new NFAEdge(emptyState, emptyState, s2));
						}
					}
					dfa.addEdge(new NFAEdge(P, emptyState, s));
				}
			}*/
			
			for (Map.Entry<TransitionLabel, HashSet<NFAVertexND>> kv : newStates.entrySet()) {
				if (isInterrupted()) {
					throw new InterruptedException();
				}
				TransitionLabel label = kv.getKey();
				HashSet<NFAVertexND> reachableViaSymbolEpsilon = new HashSet<NFAVertexND>();
				
				for (NFAVertexND v : kv.getValue()) {
					if (isInterrupted()) {
						throw new InterruptedException();
					}
					reachableViaSymbolEpsilon.add(v);
					HashSet<NFAVertexND> reachableViaEpsilon = reachableWithEpsilon(input, v);
					reachableViaSymbolEpsilon.addAll(reachableViaEpsilon);
				}
				/* we sort the sub states, so that states with the same sub states are equal (since order matters in the PC, but not here) */
				LinkedList<NFAVertexND> sortedReachableViaSymbolEpsilon = new LinkedList<NFAVertexND>(reachableViaSymbolEpsilon);
				Collections.sort(sortedReachableViaSymbolEpsilon);
				
				labelBuilder = new StringBuilder();
				Iterator<NFAVertexND> i1 = sortedReachableViaSymbolEpsilon.iterator();
				while (i1.hasNext()) {
					if (isInterrupted()) {
						throw new InterruptedException();
					}
					NFAVertexND currentSubState = i1.next();
					List<String> labelSubStates = currentSubState.getStates();
					if (labelSubStates.size() == 1) {
						String currentLabel = labelSubStates.iterator().next();
						labelBuilder.append(currentLabel);
						
					} else {
						/* TODO determinizing a multidimensional NFA is untested */
						Iterator<String> i2 = labelSubStates.iterator();
						labelBuilder.append("(");
						while (i2.hasNext()) {
							if (isInterrupted()) {
								throw new InterruptedException();
							}
							String currentLabel = i2.next();
							labelBuilder.append(currentLabel);
							if (i2.hasNext()) {
								labelBuilder.append(", ");
							}
						}
						labelBuilder.append(")");

					}
					if (i1.hasNext()) {
						labelBuilder.append(", ");
					}
					
				}

				
				NFAVertexND stateToAdd = new NFAVertexND(labelBuilder.toString()); /* << build a label from sortedReachableViaSymbolEpsilon and store in map from stateToAdd to sortedReachableViaSymbolEpsilon */
				dfaStateToSubStatesMap.put(stateToAdd, reachableViaSymbolEpsilon);
				if (!dfa.containsVertex(stateToAdd)) {
					toVisit.add(stateToAdd);
					dfa.addVertex(stateToAdd);
					for (NFAVertexND i : reachableViaSymbolEpsilon) {
						if (input.isAcceptingState(i)) {
							dfa.addAcceptingState(stateToAdd);
						}
						
					}
				}
				//System.out.println(P + " " + " " + stateToAdd + " " + symbol);
				dfa.addEdge(new NFAEdge(P, stateToAdd, label));				
			}
		}
		
		//System.out.println(dfa);
		return dfa;
	}
	
	public static NFAGraph dfaIntersection(NFAGraph m1, NFAGraph m2) {
		NFAGraph intersectionGraph = new NFAGraph();
		NFAVertexND intersectionGraphInitialstate = new NFAVertexND(m1.getInitialState(), m2.getInitialState());
		
		int stateCounter = 0;
		/* We want the intersection DFA to be one dimensional, map is 2D to 1D */
		HashMap<NFAVertexND, NFAVertexND> stateMap = new HashMap<NFAVertexND, NFAVertexND>();
		NFAVertexND mappedinitialState = new NFAVertexND("q" + stateCounter);
		stateCounter++;
		stateMap.put(intersectionGraphInitialstate, mappedinitialState);
		intersectionGraph.addVertex(mappedinitialState);
		boolean isAcceptingState = m1.isAcceptingState(m1.getInitialState()) && m2.isAcceptingState(m2.getInitialState());
		if (isAcceptingState) {
			intersectionGraph.addAcceptingState(mappedinitialState);
		}
		intersectionGraph.setInitialState(mappedinitialState);
		
		Stack<NFAVertexND> toVisit = new Stack<NFAVertexND>();
		toVisit.push(intersectionGraphInitialstate);
		
		boolean containsSinkState = false;
		NFAVertexND sinkState = null;
		
		
		while (!toVisit.isEmpty()) {
			NFAVertexND currentIntersectionState = toVisit.pop();
			NFAVertexND m1SourceState = currentIntersectionState.getStateByDimension(1);
			NFAVertexND m2SourceState = currentIntersectionState.getStateByDimension(2);
			NFAVertexND currentMappedState = stateMap.get(currentIntersectionState);
			
			TransitionLabel accountedSymbols = new CharacterClassTransitionLabel();
			for (NFAEdge e1 : m1.outgoingEdgesOf(m1SourceState)) {
				for (NFAEdge e2 : m2.outgoingEdgesOf(m2SourceState)) {
					TransitionLabel e1TransitionLabel = e1.getTransitionLabel();
					TransitionLabel e2TransitionLabel = e2.getTransitionLabel();
					TransitionLabel intersectionTransitionLabel = e1TransitionLabel.intersection(e2TransitionLabel);
					if (!intersectionTransitionLabel.isEmpty()) {
						NFAVertexND m1TargetState = e1.getTargetVertex();
						NFAVertexND m2TargetState = e2.getTargetVertex();
						NFAVertexND newIntersectionState = new NFAVertexND(m1TargetState, m2TargetState);
						
						NFAVertexND targetMappedState;
						if (!stateMap.containsKey(newIntersectionState)) {
							targetMappedState = new NFAVertexND("q" + stateCounter);
							stateCounter++;
							stateMap.put(newIntersectionState, targetMappedState);
							intersectionGraph.addVertex(targetMappedState);
							toVisit.push(newIntersectionState);
							isAcceptingState = m1.isAcceptingState(m1TargetState) && m2.isAcceptingState(m2TargetState);
							if (isAcceptingState) {
								intersectionGraph.addAcceptingState(targetMappedState);
							}
						} else {
							targetMappedState = stateMap.get(newIntersectionState);
						}
						intersectionGraph.addEdge(new NFAEdge(currentMappedState, targetMappedState, intersectionTransitionLabel));
						accountedSymbols = accountedSymbols.union(intersectionTransitionLabel);
					}
				}
			}
			TransitionLabel unaccountedSymbols = accountedSymbols.complement();
			if (!unaccountedSymbols.isEmpty()) {
				if (containsSinkState) {
					intersectionGraph.addEdge(new NFAEdge(currentIntersectionState, sinkState, unaccountedSymbols));
				} else {
					sinkState = new NFAVertexND("q" + stateCounter);
					stateCounter++;
					stateMap.put(sinkState, sinkState);
					intersectionGraph.addVertex(sinkState);
					containsSinkState = true;
					NFAEdge wildcardLoop = new NFAEdge(sinkState, sinkState, CharacterClassTransitionLabel.wildcardLabel());
					intersectionGraph.addEdge(wildcardLoop);				
				}
			}
		}
		
		return intersectionGraph;
	}
	

	/**
	 * A function that uses Kahn's algorithm to find the topological order of the vertices in
	 * the graph.
	 * 
	 * @param originalM
	 *            The NFA graph to find the topological order for.
	 * @return A map to find the position of each vertex in the topological
	 *         order.
	 */
	public static HashMap<NFAVertexND, Integer> topologicalSort(NFAGraph originalM) {
		NFAGraph m = originalM.copy();

		LinkedList<NFAVertexND> toVisit = new LinkedList<NFAVertexND>();
		HashMap<NFAVertexND, Integer> oldNewMap = new HashMap<NFAVertexND, Integer>();
		int orderCounter = 0;
		toVisit.addLast(m.getInitialState());

		while (!toVisit.isEmpty()) {
			NFAVertexND n = toVisit.removeLast();
			oldNewMap.put(n, orderCounter++);
			for (NFAEdge e : originalM.outgoingEdgesOf(n)) {
				NFAVertexND targetVertex = e.getTargetVertex();
				m.removeEdge(e);
				if (m.inDegreeOf(targetVertex) == 0) {
					toVisit.addLast(targetVertex);
				}
			}
		}
		if (!m.edgeSet().isEmpty()) {
			throw new RuntimeException("G5 cannot have cycles.");
		}

		return oldNewMap;
	}
	
	protected static boolean isInterrupted() {
		return Thread.currentThread().isInterrupted();
	}

}
