package analysis;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import analysis.EdaAnalysisResults.EdaCases;
import analysis.IdaAnalysisResults.IdaCases;
import analysis.AnalysisSettings.PriorityRemovalStrategy;
import analysis.NFAAnalyserInterface.EdaAnalysisResultsNoEda;

import nfa.NFAGraph;
import nfa.NFAVertexND;
import nfa.NFAEdge;
import nfa.transitionlabel.EmptyTransitionLabelException;
import nfa.transitionlabel.EpsilonTransitionLabel;

public class NFAAnalyserFlattening extends NFAAnalyser {

	public NFAAnalyserFlattening(PriorityRemovalStrategy priorityRemovalStrategy) {
		super(priorityRemovalStrategy);
	}

	@Override
	protected EdaAnalysisResults calculateEdaAnalysisResults(NFAGraph originalM) throws InterruptedException {
		NFAGraph flatGraph = flattenNFA(originalM);
		
		if (isInterrupted()) {
			throw new InterruptedException();
		}
		//flatGraph = NFAAnalysisTools.makeTrim(flatGraph);

		if (isInterrupted()) {
			throw new InterruptedException();
		}
		
		LinkedList<NFAGraph> sccsInFlat = NFAAnalysisTools.getStronglyConnectedComponents(flatGraph);

		if (isInterrupted()) {
			throw new InterruptedException();
		}	

		EdaAnalysisResults toReturn = new EdaAnalysisResultsNoEda(originalM);
		/* We set the priorityremoval strategy here, so that the caller know that priorities were ignored */
		toReturn.setPriorityRemovalStrategy(PriorityRemovalStrategy.IGNORE);

		/* Testing for parallel edges in scc in merged graph */
		toReturn = edaTestCaseParallel(originalM, sccsInFlat);
		toReturn.setPriorityRemovalStrategy(PriorityRemovalStrategy.IGNORE);
		if (isInterrupted()) {
			throw new InterruptedException();
		}
		
		if (toReturn.edaCase != EdaCases.NO_EDA) {
			return toReturn;
		}
		/* Testing for multiple paths in PC */
		toReturn = edaTestCaseFilter(originalM, flatGraph);
		toReturn.setPriorityRemovalStrategy(PriorityRemovalStrategy.IGNORE);
		return toReturn;
	}
	
	@Override
	protected EdaAnalysisResults calculateEdaUnprioritisedAnalysisResults(NFAGraph originalM) throws InterruptedException {	
		NFAGraph flatGraph = flattenNFA(originalM);
		if (isInterrupted()) {
			throw new InterruptedException();
		}
		//flatGraph = NFAAnalysisTools.makeTrim(flatGraph);
		if (isInterrupted()) {
			throw new InterruptedException();
		}
		EdaAnalysisResults toReturn = new EdaAnalysisResultsNoEda(originalM);
		toReturn = edaUnprioritisedAnalysis(flatGraph);
		toReturn.setPriorityRemovalStrategy(PriorityRemovalStrategy.UNPRIORITISE);
		return toReturn;
	}

	@Override
	protected IdaAnalysisResults calculateIdaAnalysisResults(NFAGraph originalM)
			throws InterruptedException {

		NFAGraph flatGraph = flattenNFA(originalM);

		if (isInterrupted()) {
			throw new InterruptedException();
		}
		//flatGraph = NFAAnalysisTools.makeTrim(flatGraph);

		if (isInterrupted()) {
			throw new InterruptedException();
		}

		IdaAnalysisResults toReturn = new IdaAnalysisResultsNoIda(originalM);
		toReturn.setPriorityRemovalStrategy(PriorityRemovalStrategy.IGNORE);
		/* Testing for multiple paths in PC */
		toReturn = idaTestCaseFilter(originalM, flatGraph);

		toReturn.setPriorityRemovalStrategy(PriorityRemovalStrategy.IGNORE);
		
		return toReturn;
	}
	
	@Override
	protected IdaAnalysisResults calculateIdaUnprioritisedAnalysisResults(NFAGraph originalM) throws InterruptedException {
		NFAGraph flatGraph = flattenNFA(originalM);

		if (isInterrupted()) {
			throw new InterruptedException();
		}
		
		//flatGraph = NFAAnalysisTools.makeTrim(flatGraph);

		if (isInterrupted()) {
			throw new InterruptedException();
		}

		IdaAnalysisResults toReturn = new IdaAnalysisResultsNoIda(originalM);
		toReturn = idaUnprioritisedAnalysis(flatGraph);


		toReturn.setPriorityRemovalStrategy(PriorityRemovalStrategy.UNPRIORITISE);
		return toReturn;
	}

	public static NFAGraph flattenNFA2(NFAGraph m) throws InterruptedException {
		NFAGraph flatGraph = new NFAGraph();
		/* Adding the vertices */
		for (NFAVertexND v : m.vertexSet()) {
			if (Thread.currentThread().isInterrupted()) {
				throw new InterruptedException();
			}
			flatGraph.addVertex(v);
		}

		flatGraph.setInitialState(m.getInitialState());
		for (NFAVertexND v : m.getAcceptingStates()) {
			if (Thread.currentThread().isInterrupted()) {
				throw new InterruptedException();
			}
			flatGraph.addAcceptingState(v);
		}

		/* Adding the non-epsilon edges */
		for (NFAEdge e : m.edgeSet()) {
			if (Thread.currentThread().isInterrupted()) {
				throw new InterruptedException();
			}
			if (!e.getIsEpsilonTransition()) {
				flatGraph.addEdge(e);
			}
		}

		for (NFAVertexND source : m.vertexSet()) {
			if (Thread.currentThread().isInterrupted()) {
				throw new InterruptedException();
			}
			HashMap<NFAVertexND, Integer> numWalksMap = numWalksFrom(m, source);
			for (Map.Entry<NFAVertexND, Integer> kv : numWalksMap.entrySet()) {
				if (Thread.currentThread().isInterrupted()) {
					throw new InterruptedException();
				}
				NFAVertexND destination = kv.getKey();
				int weight = kv.getValue();
				if (weight > 0) {
					NFAEdge newEdge;
					try {
						newEdge = new NFAEdge(source, destination, "ε0");
					} catch (EmptyTransitionLabelException e1) {
						throw new RuntimeException("Empty transition label");
					}
					newEdge.setNumParallel(weight);
					flatGraph.addEdge(newEdge);
				}
			}
		}
		return flatGraph;
	}

	public static NFAGraph flattenNFA(NFAGraph m) throws InterruptedException {
		NFAGraph flatGraph = new NFAGraph();

		NFAVertexND mInitialState = m.getInitialState();

		HashSet<NFAVertexND> searchFromVertices = new HashSet<NFAVertexND>();
		searchFromVertices.add(mInitialState);

		/* Adding the non-epsilon edges and their vertices */
		for (NFAEdge e : m.edgeSet()) {
			if (Thread.currentThread().isInterrupted()) {
				throw new InterruptedException();
			}
			if (!e.getIsEpsilonTransition()) {
				NFAVertexND sourceVertex = e.getSourceVertex();
				NFAVertexND targetVertex = e.getTargetVertex();
				if (!flatGraph.containsVertex(sourceVertex)) {
					flatGraph.addVertex(sourceVertex);
				}
				if (!flatGraph.containsVertex(targetVertex)) {
					flatGraph.addVertex(targetVertex);
				}

				flatGraph.addEdge(e);

				if (!searchFromVertices.contains(targetVertex)) {
					searchFromVertices.add(targetVertex);
				}

			}
		}
		

		/* Adding the initial state */
		if (!flatGraph.containsVertex(mInitialState)) {
			flatGraph.addVertex(mInitialState);
		}
		flatGraph.setInitialState(mInitialState);

		/* Adding the accepting states */
		for (NFAVertexND v : m.getAcceptingStates()) {
			if (Thread.currentThread().isInterrupted()) {
				throw new InterruptedException();
			}
			if (!flatGraph.containsVertex(v)) {
				flatGraph.addVertex(v);
			}
			flatGraph.addAcceptingState(v);
		}

		for (NFAVertexND sourceState : searchFromVertices) {
			if (Thread.currentThread().isInterrupted()) {
				throw new InterruptedException();
			}
			LinkedList<NFAVertexND> reachableFromSource = dfsFlatten(m,	sourceState);
			int priorityCounter = 1;
			for (NFAVertexND targetState : reachableFromSource) {
				if (Thread.currentThread().isInterrupted()) {
					throw new InterruptedException();
				}
				if (!sourceState.equals(targetState)) {
					NFAEdge newEdge = new NFAEdge(sourceState, targetState,	new EpsilonTransitionLabel("ε" + priorityCounter));
					flatGraph.addEdge(newEdge);
					priorityCounter++;
				}

			}
		}
		return flatGraph;
	}

	public static LinkedList<NFAVertexND> dfsFlatten(NFAGraph m, NFAVertexND startVertex) throws InterruptedException {
		LinkedList<NFAVertexND> endVertices = new LinkedList<NFAVertexND>();
		dfsFlatten(m, startVertex, new HashSet<NFAEdge>(), endVertices);
		return endVertices;
	}

	/*
	 * Assumes one start and accept state and either epsilon transitions, or a
	 * symbol transitions from each state
	 */
	private static void dfsFlatten(NFAGraph m, NFAVertexND currentVertex, HashSet<NFAEdge> visitedEdges, LinkedList<NFAVertexND> endVertices) throws InterruptedException {
		if (Thread.currentThread().isInterrupted()) {
			throw new InterruptedException();
		}

		Set<NFAEdge> outgoingEdges = m.outgoingEdgesOf(currentVertex);
		if (!outgoingEdges.isEmpty()) {
			NFAEdge edge = outgoingEdges.iterator().next();
			if (edge.getIsEpsilonTransition()) {
				LinkedList<NFAEdge> sortedEdges = new LinkedList<NFAEdge>(outgoingEdges);
				Collections.sort(sortedEdges);
				for (NFAEdge e : sortedEdges) {
					if (!visitedEdges.contains(e)) {
						visitedEdges.add(e);
						dfsFlatten(m, e.getTargetVertex(), visitedEdges, endVertices);
						visitedEdges.remove(e);
					}

				}
			} else {
				endVertices.add(currentVertex);
			}

		} else if (m.isAcceptingState(currentVertex)) {
			endVertices.add(currentVertex);
		}

	}

	public static HashMap<NFAVertexND, Integer> numWalksFrom(NFAGraph m,
			NFAVertexND s) {
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

		numWalksFromSearch(m, s, visitedEdges, paths);
		return paths;
	}

	static void numWalksFromSearch(NFAGraph m, NFAVertexND current,
			HashMap<NFAEdge, Integer> visitedEdges,
			HashMap<NFAVertexND, Integer> paths) {
		/* update the number of paths to the current vertex */

		for (NFAEdge e : m.outgoingEdgesOf(current)) {
			if (e.getIsEpsilonTransition()) {
				int currentNumVisit = visitedEdges.get(e);
				/* update the number of times this edge has been visited */
				visitedEdges.put(e, currentNumVisit + 1);
				/* for the amount of times the edge can be visited again */
				for (int i = currentNumVisit; i < e.getNumParallel(); i++) {
					/* search from the new vertex */
					paths.put(e.getTargetVertex(),
							paths.get(e.getTargetVertex()) + 1);
					numWalksFromSearch(m, e.getTargetVertex(), visitedEdges,
							paths);
				}
				/* unvisit the current edge */
				visitedEdges.put(e, currentNumVisit);
			}
		}
	}


}
