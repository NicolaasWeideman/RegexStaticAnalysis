package analysis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.regex.Pattern;

import analysis.EdaAnalysisResults.EdaCases;
import analysis.IdaAnalysisResults.IdaCases;
import analysis.AnalysisSettings.NFAConstruction;
import analysis.AnalysisSettings.PriorityRemovalStrategy;
import analysis.NFAAnalyserInterface.EdaAnalysisResultsNoEda;
import regexcompiler.MyPattern;

import nfa.transitionlabel.TransitionLabel.TransitionType;

import nfa.NFAGraph;
import nfa.NFAVertexND;
import nfa.UPNFAState;
import nfa.NFAEdge;
import nfa.transitionlabel.TransitionLabel;
import nfa.transitionlabel.CharacterClassTransitionLabel;
import nfa.transitionlabel.EpsilonTransitionLabel;

public abstract class NFAAnalyser implements NFAAnalyserInterface {
	
	private final int MAX_IDA_DEGREE = Integer.MAX_VALUE;
	
	private final int MAX_CACHE_SIZE = 5;
	
	protected final ExploitStringBuilder exploitStringBuilder;
	protected final PriorityRemovalStrategy priorityRemovalStrategy;
	public NFAAnalyser(PriorityRemovalStrategy priorityRemovalStrategy) {
		this.exploitStringBuilder = new ExploitStringBuilder();
		this.priorityRemovalStrategy = priorityRemovalStrategy;
	}

	protected Map<NFAGraph, EdaAnalysisResults> edaResultsCache = new HashMap<NFAGraph, EdaAnalysisResults>();
	protected Map<NFAGraph, IdaAnalysisResults> idaResultsCache = new HashMap<NFAGraph, IdaAnalysisResults>();
	
	protected abstract EdaAnalysisResults calculateEdaAnalysisResults(NFAGraph originalM) throws InterruptedException;
	
	protected abstract EdaAnalysisResults calculateEdaUnprioritisedAnalysisResults(NFAGraph originalM) throws InterruptedException;
	
	protected abstract IdaAnalysisResults calculateIdaAnalysisResults(NFAGraph originalM) throws InterruptedException;
	
	protected abstract IdaAnalysisResults calculateIdaUnprioritisedAnalysisResults(NFAGraph originalM) throws InterruptedException;
	
	@Override
	public AnalysisResultsType containsIDA(NFAGraph originalM) {
		IdaAnalysisResults resultsObject;
		try {
			resultsObject = (IdaAnalysisResults) searchIdaCache(originalM);
			switch (resultsObject.idaCase) {
			case IDA:
				return AnalysisResultsType.IDA;
			case NO_IDA:
				return AnalysisResultsType.NO_IDA;
			default:
				throw new RuntimeException("Unexpected IDA analysis result: " + resultsObject.idaCase);
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return AnalysisResultsType.TIMEOUT_IN_IDA;
		}
	}

	@Override
	public IdaAnalysisResults getIdaAnalysisResults(NFAGraph m) {
		if (idaResultsCache.containsKey(m)) {
			return idaResultsCache.get(m);
		} else {
			throw new IllegalStateException("No IDA Analysis results found!");
		}
	}
	
	@Override
	public AnalysisResultsType containsEDA(NFAGraph originalM) {
		EdaAnalysisResults resultsObject;
		try {

			resultsObject = (EdaAnalysisResults) searchEdaCache(originalM);

			switch (resultsObject.edaCase) {
			case ESCC:
			case FILTER:
			case PARALLEL:
				return AnalysisResultsType.EDA;
			case NO_EDA:
				return AnalysisResultsType.NO_EDA;
			default:
				throw new RuntimeException("Unexpected EDA analysis result: " + resultsObject.edaCase);
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return AnalysisResultsType.TIMEOUT_IN_EDA;
		}
		
		
	}

	@Override
	public EdaAnalysisResults getEdaAnalysisResults(NFAGraph m) {
		if (edaResultsCache.containsKey(m)) {
			return edaResultsCache.get(m);
		} else {
			throw new IllegalStateException("No EDA Analysis results found!");
		}
	}
	

	protected AnalysisResults searchEdaCache(NFAGraph originalM) throws InterruptedException {
		EdaAnalysisResults resultsObject;
		if (!edaResultsCache.containsKey(originalM)) {

			resultsObject = calculateEdaAnalysisResults(originalM);
			if (resultsObject.edaCase != EdaCases.NO_EDA) {
				switch (priorityRemovalStrategy) {
				case IGNORE:
					break;
				case UNPRIORITISE:

					resultsObject = calculateEdaUnprioritisedAnalysisResults(originalM);
	
					break;
				default:
					throw new RuntimeException("Unknown priority strategy: " + priorityRemovalStrategy);
				}
			}

			if (edaResultsCache.size() >= MAX_CACHE_SIZE) {
				edaResultsCache.clear();
			}
			edaResultsCache.put(originalM, resultsObject);	
		} else {
			resultsObject = edaResultsCache.get(originalM);
		}
		return resultsObject;
	}
	
	protected AnalysisResults searchIdaCache(NFAGraph originalM) throws InterruptedException {
		IdaAnalysisResults resultsObject;
		
		if (!edaResultsCache.containsKey(originalM)) {
			throw new IllegalStateException("An NFA must first be checked for EDA, before it can be checked for IDA.");
		} else {
			EdaAnalysisResults edaResultsObject = edaResultsCache.get(originalM);

			if (edaResultsObject.edaCase == EdaCases.NO_EDA) {
				if (!idaResultsCache.containsKey(originalM)) {
					EdaAnalysisResultsNoEda noEdaResults = (EdaAnalysisResultsNoEda) edaResultsObject;
					/* If the analysis was unpriority based, immediately check for unprioritised IDA (since the NFA might have priority ignored EDA, which will cause the IDA analysis to fail) */
					PriorityRemovalStrategy noEdaPriorityRemovalStrategy = noEdaResults.getPriorityRemovalStrategy();
					switch (noEdaPriorityRemovalStrategy) {
					case IGNORE:
						resultsObject = calculateIdaAnalysisResults(originalM);
						if (resultsObject.idaCase != IdaCases.NO_IDA) {
							if (priorityRemovalStrategy == PriorityRemovalStrategy.UNPRIORITISE) {
								resultsObject = calculateIdaUnprioritisedAnalysisResults(originalM);
							}
						}
						break;
					case UNPRIORITISE:
						resultsObject = calculateIdaUnprioritisedAnalysisResults(originalM);
						break;
					default:
						throw new RuntimeException("Unknown priority strategy: " + noEdaPriorityRemovalStrategy);
					}
					
					if (idaResultsCache.size() >= MAX_CACHE_SIZE) {
						idaResultsCache.clear();
					}
					idaResultsCache.put(originalM, resultsObject);
				} else {
					resultsObject = idaResultsCache.get(originalM);
				}
			} else {
				throw new IllegalArgumentException("NFA contains EDA and cannot be tested for IDA.");
			}
		}

		return resultsObject;
	}
	
	public ExploitString findEDAExploitString(NFAGraph originalM) throws InterruptedException {
		if (edaResultsCache.containsKey(originalM)) {
			EdaAnalysisResults resultsObject = edaResultsCache.get(originalM);
			return exploitStringBuilder.buildEdaExploitString(resultsObject);
		} else {
			throw new NoAnalysisFoundException();
		}
		
	}
	
	public ExploitString findIDAExploitString(NFAGraph originalM) throws InterruptedException {
		if (idaResultsCache.containsKey(originalM)) {
			IdaAnalysisResults resultsObject = idaResultsCache.get(originalM);
			return exploitStringBuilder.buildIdaExploitString(resultsObject);
		} else {
			throw new NoAnalysisFoundException();
		}
	}
	
	protected EdaAnalysisResults edaTestCaseParallel(NFAGraph originalM, LinkedList<NFAGraph> sccsInFlat) throws InterruptedException {
		for (NFAGraph currentSccInFlat : sccsInFlat) {
			if (isInterrupted()) {
				throw new InterruptedException();
			}
			for (NFAEdge e : currentSccInFlat.edgeSet()) {
				if (isInterrupted()) {
					throw new InterruptedException();
				}
				if (e.getNumParallel() > 1) {

					/* building the exploit string */
					NFAVertexND sourceVertex = e.getSourceVertex();
					// System.out.println("Case: Parallel edges in merged");
					EdaAnalysisResults resultsObject = new EdaAnalysisResultsParallel(originalM, currentSccInFlat, sourceVertex, e);
					return resultsObject;
				}
			}
			for (NFAVertexND sourceVertex : currentSccInFlat.vertexSet()) {
				if (isInterrupted()) {
					throw new InterruptedException();
				}
				HashSet<NFAVertexND> epsilonAdjacentVertices = new HashSet<NFAVertexND>();
				HashSet<NFAEdge> outgoingEpsilonEdges = (HashSet<NFAEdge>) currentSccInFlat.outgoingEpsilonEdgesOf(sourceVertex);
				for (NFAEdge e : outgoingEpsilonEdges) {
					if (isInterrupted()) {
						throw new InterruptedException();
					}
					NFAVertexND targetVertex = e.getTargetVertex();
					if (epsilonAdjacentVertices.contains(targetVertex)) {
						
						EdaAnalysisResults resultsObject = new EdaAnalysisResultsParallel(originalM, currentSccInFlat, sourceVertex, e);
						return resultsObject;
					} else {
						epsilonAdjacentVertices.add(targetVertex);
					}
					
				}
			}
		}
		return new EdaAnalysisResultsNoEda(originalM);
	}
	
	protected EdaAnalysisResults edaTestCaseFilter(NFAGraph originalM, NFAGraph merged) throws InterruptedException {
		NFAGraph pc = NFAAnalysisTools.productConstructionAFA(merged);
		
		if (isInterrupted()) {
			throw new InterruptedException();
		}
		
		//pc = NFAAnalysisTools.makeTrim(pc);
		if (isInterrupted()) {
			throw new InterruptedException();
		}
		List<NFAGraph> pcSCCs = NFAAnalysisTools.getStronglyConnectedComponents(pc);
		if (isInterrupted()) {
			throw new InterruptedException();
		}
		for (NFAGraph pcSCC : pcSCCs) {
			
			for (NFAVertexND pfp : pcSCC.vertexSet()) {
				
				if (isInterrupted()) {
					throw new InterruptedException();
				}
				String p1 = pfp.getStateNumberByDimension(1);
				String p2 = pfp.getStateNumberByDimension(3);
				if (p1.equals(p2)) {
					/* found (P, P) */

					for (NFAVertexND qfq : pcSCC.vertexSet()) {
						
						if (isInterrupted()) {
							throw new InterruptedException();
						}
						String q1 = qfq.getStateNumberByDimension(1);
						String q2 = qfq.getStateNumberByDimension(3);
						if (!q1.equals(q2)) {
							/* found (P', P") where P' != P" */

							/* building the exploit string */
							// System.out.println("Case: Found (P', P\") P: " +
							// pfp + " P\'P\": " + qfq);
							//System.out.println("In NFAAnalyser:edaTestCaseFilter");
							//System.out.println(originalM);
							EdaAnalysisResultsFilter resultsObject = new EdaAnalysisResultsFilter(originalM, pcSCC, pfp, qfq);
							//System.out.println("End");
							return resultsObject;
						}
					}
				}
			}
		}
		return new EdaAnalysisResultsNoEda(originalM);
	}
	
	protected IdaAnalysisResults idaTestCaseFilter(NFAGraph originalM, NFAGraph flat) throws InterruptedException {
		//System.out.println(originalM);
		//NFAGraph m1 = flat.copy();
		//NFAGraph pc = NFAAnalysisTools.productConstructionAFAFA(m1);
		NFAGraph pc = NFAAnalysisTools.productConstructionAFAFA(flat);
		
		//pc = NFAAnalysisTools.makeTrim(pc);
		
		
		String[] filterStates = {"0", "1", "2"};

		/* Adding the edges from (p, q, q) to (p, p, q) */
		NFAGraph pcWithBackEdges = pc.copy();
		for (NFAVertexND sourcePCV : pc.vertexSet()) {
			if (isInterrupted()) {
				throw new InterruptedException();
			}

			String p1 = sourcePCV.getStateNumberByDimension(1);
			String p2 = sourcePCV.getStateNumberByDimension(3);
			String q = sourcePCV.getStateNumberByDimension(5);
			
			/* found (p, p, q), p != q */
			if (p1.equals(p2) && !p1.equals(q)) {
				/*
				 * Since filter states don't matter we need to check to all
				 * combinations
				 */
				for (String f1 : filterStates) {
					if (isInterrupted()) {
						throw new InterruptedException();
					}
					for (String f2 : filterStates) {
						if (isInterrupted()) {
							throw new InterruptedException();
						}
						NFAVertexND targetPCV = new NFAVertexND(p1, f1, q, f2, q);
						/* found (p, p, q) and (p, q, q) */
						if (pc.containsVertex(targetPCV)) {
							/* Adding the edge back from (p, q, q) to (p, p, q) */
							NFAEdge newSpecialEdge = new NFAEdge(targetPCV, sourcePCV, new IdaSpecialTransitionLabel(null));
							pcWithBackEdges.addEdge(newSpecialEdge);
						}
					}
				}
			}
			
		}
		
		boolean containsIda = false;
		LinkedList<NFAGraph> sccs = NFAAnalysisTools.getStronglyConnectedComponents(pcWithBackEdges);
		LinkedList<NFAVertexND> storedPs = new LinkedList<NFAVertexND>();
		LinkedList<NFAVertexND> storedQs = new LinkedList<NFAVertexND>();
		LinkedList<LinkedList<TransitionLabel>> storedSymbols = new LinkedList<LinkedList<TransitionLabel>>();
		
		for (NFAGraph scc : sccs) {
			if (isInterrupted()) {
				throw new InterruptedException();
			}
			boolean containsSymbolTransition = false;
			for (NFAEdge e : scc.edgeSet()) {
				if (isInterrupted()) {
					throw new InterruptedException();
				}
				TransitionLabel tl = e.getTransitionLabel();				
				if (tl.getTransitionType() == TransitionType.SYMBOL) {
					containsSymbolTransition = true;
					break;
				}
			}
			if (containsSymbolTransition) {
				for (NFAEdge e : scc.edgeSet()) {
					if (isInterrupted()) {
						throw new InterruptedException();
					}
					
					TransitionLabel tl = e.getTransitionLabel();				
					if (tl.getTransitionType() == TransitionType.OTHER) {
						if (tl instanceof IdaSpecialTransitionLabel) {
							NFAVertexND eSource = e.getSourceVertex();
							NFAVertexND p = eSource.getStateByDimension(1);
							NFAVertexND q = eSource.getStateByDimension(5);	
							LinkedList<NFAEdge> pqPath = NFAAnalysisTools.shortestPathBetween(flat, p, q);
							LinkedList<TransitionLabel> pqPathTransitionLabels = new LinkedList<TransitionLabel>();	
							for (NFAEdge e2 : pqPath) {
								TransitionLabel currentTransitionLabel = e2.getTransitionLabel();
								if (currentTransitionLabel.getTransitionType() != TransitionType.EPSILON) {
									pqPathTransitionLabels.add(currentTransitionLabel);
								}
							}
							
							storedPs.add(p);
							storedQs.add(q);
							storedSymbols.add(pqPathTransitionLabels);
							containsIda = true;
						}
					}
				}
			}
		}
		if (containsIda) {
			
			/*
			NFAGraph sccMergedM = originalM.copy();
			Map<NFAVertexND, NFAGraph> mMap = NFAAnalysisTools.mergeStronglyConnectedComponents(sccMergedM, false);
			HashMap<NFAVertexND, NFAVertexND> sccMap = new HashMap<NFAVertexND, NFAVertexND>();
			for (NFAVertexND sccMergedMVertex : sccMergedM.vertexSet()) {
				if (mMap.containsKey(sccMergedMVertex)) {
					NFAGraph currentSCC = mMap.get(sccMergedMVertex);
					for (NFAVertexND currentVertex : currentSCC.vertexSet()) {
						sccMap.put(currentVertex, sccMergedMVertex);
					}
				} else {
					sccMap.put(sccMergedMVertex, sccMergedMVertex);
				}
			}
			
			
						
			NFAGraph degreeGraph = sccMergedM.copy();
			Iterator<NFAVertexND> i0 = storedPs.iterator();
			Iterator<NFAVertexND> i1 = storedQs.iterator();
			Iterator<LinkedList<TransitionLabel>> i2 = storedSymbols.iterator();
			// i0, i1 and i2 will always be of the same size
			while (i0.hasNext()) {
				NFAVertexND p = i0.next();
				NFAVertexND q = i1.next();
				LinkedList<TransitionLabel> tls = i2.next();
				NFAVertexND pScc = sccMap.get(p);
				NFAVertexND qScc = sccMap.get(q);

				degreeGraph.addEdge(new NFAEdge(pScc, qScc, new IdaSpecialTransitionLabel(tls)));				
			}

			// Calculating the degree
			NFAVertexND initialVertex = originalM.getInitialState();
			
			NFAVertexND initialSccVertex = sccMap.get(initialVertex);
			//System.out.println(sccMap);	

			LinkedList<NFAEdge> maxPath = new LinkedList<NFAEdge>();
			int d = calculateD(degreeGraph, initialSccVertex, maxPath);
			
			//System.out.println("Begin max path");	
			//for (NFAEdge e : maxPath) {
			//	System.out.println(e.getSourceVertex() + " " + e + " " + e.getTargetVertex());
			//}
			//System.out.println("End max path");
			

			*/

			NFAGraph degreeGraph = originalM.copy();
			Iterator<NFAVertexND> i0 = storedPs.iterator();
			Iterator<NFAVertexND> i1 = storedQs.iterator();
			Iterator<LinkedList<TransitionLabel>> i2 = storedSymbols.iterator();
			// i0, i1 and i2 will always be of the same size
			while (i0.hasNext()) {
				NFAVertexND p = i0.next();
				NFAVertexND q = i1.next();
				LinkedList<TransitionLabel> tls = i2.next();

				degreeGraph.addEdge(new NFAEdge(p, q, new IdaSpecialTransitionLabel(tls)));				
			}

			/* Calculating the degree */
			NFAVertexND initialVertex = degreeGraph.getInitialState();
			

			LinkedList<NFAEdge> maxPath = new LinkedList<NFAEdge>();
			int d = calculateD(degreeGraph, initialVertex, maxPath);
			
			
			return new IdaAnalysisResultsIda(originalM, d, maxPath);
		} else {
			return new IdaAnalysisResultsNoIda(originalM);			
		}
	}
	
	private int calculateD(NFAGraph originalM, NFAVertexND initialState, LinkedList<NFAEdge> maxPath) throws InterruptedException {
		return calculateDDFS(originalM, initialState, 0, -1, new LinkedList<NFAEdge>(), maxPath, new HashSet<NFAEdge>());
	}
	
	private int calculateDDFS(NFAGraph m, NFAVertexND currentVertex, int currentD, int maxD, LinkedList<NFAEdge> currentPath, LinkedList<NFAEdge> maxPath, HashSet<NFAEdge> traversed) throws InterruptedException {
		/*
		 * For an optimisation the set "traversed" can be removed completely,
		 * since originalM will always be a DAG. At the moment it is included for
		 * testing purposes.
		 */
		for (NFAEdge currentE : m.outgoingEdgesOf(currentVertex)) {

			if (isInterrupted()) {
				throw new InterruptedException();
			}
			TransitionLabel tl = currentE.getTransitionLabel();
			if (!traversed.contains(currentE)) {
				traversed.add(currentE);
				currentPath.add(currentE); /* all deeper paths will contain currentVertex */
				if (tl instanceof IdaSpecialTransitionLabel) {
					maxD = calculateDDFS(m, currentE.getTargetVertex(), currentD + 1, maxD, currentPath, maxPath, traversed);
				} else {
					maxD = calculateDDFS(m, currentE.getTargetVertex(), currentD, maxD, currentPath, maxPath, traversed);
				}
				currentPath.remove(currentE); /* since the list is passed as reference we need to remove this from the path */
				traversed.remove(currentE);
			} else {
				/* We can make this assumption if we work with the component graph, but as we have seen, the component graph is not sufficient when calculating th exploit string explicitly. */
				//throw new RuntimeException("Graph is not supposed to have cycles...");
			}

		}
		int newMax;
		if (currentD > maxD) {
			maxPath.clear();
			maxPath.addAll(currentPath);
			newMax =  currentD;
		} else {
			newMax =  maxD;
		}
		
		return newMax;
	}
		
	/* Assume NFA has no epsilon loops, only one start and one accept and every state has either epsilon transitions, or one symbol transition from it */
	protected EdaAnalysisResults edaUnprioritisedAnalysis(NFAGraph m) throws InterruptedException {
		NFAGraph unprioritisedNFAGraph = createUnprioritisedNFAGraph(m);
		//System.out.println("UnprioritisedNFAGraph:");
		//System.out.println(unprioritisedNFAGraph);
		//System.out.println("End UnpioritisedNFAGraph");
		if (isInterrupted()) {
			throw new InterruptedException();
		}
		
		NFAGraph trimmedUPNFA = NFAAnalysisTools.makeTrimUPNFA(m, unprioritisedNFAGraph);
		//trimmedUPNFA = NFAAnalysisTools.makeTrim(trimmedUPNFA);
		
		if (isInterrupted()) {
			throw new InterruptedException();
		}
		
		HashMap<NFAVertexND, UPNFAState> statesMap = new HashMap<NFAVertexND, UPNFAState>();
		NFAGraph converted = NFAAnalysisTools.convertUpNFAToNFAGraph(trimmedUPNFA, statesMap);
		if (isInterrupted()) {
			throw new InterruptedException();
		}
		//System.out.println("Trimmed UPNFA");
		//System.out.println(converted);
		//System.out.println("End");
		
		LinkedList<NFAGraph> sccsInFlat = NFAAnalysisTools.getStronglyConnectedComponents(converted);
		if (isInterrupted()) {
			throw new InterruptedException();
		}
		
		EdaAnalysisResults resultsObject = new EdaAnalysisResultsNoEda(m);
		/* Testing for parallel edges in scc in merged graph */
		resultsObject = edaTestCaseParallel(converted, sccsInFlat);
		if (isInterrupted()) {
			throw new InterruptedException();
		}
		
		if (resultsObject.edaCase == EdaCases.NO_EDA) {
			/* Testing for multiple paths in PC */	
			//System.out.println("CONVERTED");	
			//System.out.println(converted);
			//System.out.println("End");
			resultsObject = edaTestCaseFilter(converted, converted);
		}

		resultsObject.setPriorityRemovalStrategy(PriorityRemovalStrategy.UNPRIORITISE);
		return resultsObject;
	}
	
	/* Assume NFA has no epsilon loops, only one start and one accept and every state has either epsilon transitions, or one symbol transition from it */
	protected IdaAnalysisResults idaUnprioritisedAnalysis(NFAGraph m) throws InterruptedException {		
		NFAGraph unprioritisedNFAGraph = createUnprioritisedNFAGraph(m);
		NFAGraph trimmedUPNFA = NFAAnalysisTools.makeTrimUPNFA(m, unprioritisedNFAGraph);
		trimmedUPNFA = NFAAnalysisTools.makeTrim(trimmedUPNFA);
		
		if (isInterrupted()) {
			throw new InterruptedException();
		}

		HashMap<NFAVertexND, UPNFAState> statesMap = new HashMap<NFAVertexND, UPNFAState>();
		NFAGraph converted = NFAAnalysisTools.convertUpNFAToNFAGraph(trimmedUPNFA, statesMap);
		IdaAnalysisResults resultsObject = new IdaAnalysisResultsNoIda(m);
		
		/* Testing for multiple paths in PC */
		resultsObject = idaTestCaseFilter(converted, converted);
		resultsObject.setPriorityRemovalStrategy(PriorityRemovalStrategy.UNPRIORITISE);
		return resultsObject;
	}
	
	protected NFAGraph createUnprioritisedNFAGraph(NFAGraph m) throws InterruptedException {
		NFAGraph resultUPNFA = new NFAGraph();
		int i = 0;
		//System.out.println("check0")
		
		NFAVertexND newNFAInitialState = new NFAVertexND("q" + i);
		while (m.containsVertex(newNFAInitialState)) {
			//System.out.println("check1")
			newNFAInitialState = new NFAVertexND("q" + i);
			i++;
		}
		UPNFAState newInitialState = new UPNFAState(newNFAInitialState.getStates(), new HashSet<NFAVertexND>());
		resultUPNFA.addVertex(newInitialState);
		resultUPNFA.setInitialState(newInitialState);
		
		NFAVertexND mInitialState = m.getInitialState();
		
		/* Calculating the epsilon closure after all symbol transitions */
		HashMap<NFAVertexND, TransitionLabel> stateToSymbolMap = new HashMap<NFAVertexND, TransitionLabel>();
		HashMap<NFAVertexND, LinkedList<NFAVertexND>> stateToEpsilonClosureMap = new HashMap<NFAVertexND, LinkedList<NFAVertexND>>();
		for (NFAEdge e : m.edgeSet()) {
			//System.out.println("check2")
			if (e.getTransitionType() == TransitionType.SYMBOL) {
				//System.out.println("check3")
				NFAVertexND sourceState = e.getSourceVertex();
				NFAVertexND targetState = e.getTargetVertex();
				TransitionLabel symbol = e.getTransitionLabel();				
				
				LinkedList<NFAVertexND> epsilonClosure = (LinkedList<NFAVertexND>) priorityBasedEpsilonClosureDFS(m, targetState);
				stateToSymbolMap.put(sourceState, symbol);
				stateToEpsilonClosureMap.put(sourceState, epsilonClosure);
			}
		}
		//System.out.println("check4")
		LinkedList<UPNFAState> toVisit = new LinkedList<UPNFAState>();
		HashSet<UPNFAState> visited = new HashSet<UPNFAState>();
		
		LinkedList<NFAVertexND> reachableFromStart = (LinkedList<NFAVertexND>) priorityBasedEpsilonClosureDFS(m, mInitialState);
		LinkedList<UPNFAState> startStatesList = (LinkedList<UPNFAState>) constructUPStates(reachableFromStart);
		int priorityCounter = 1;
		for (UPNFAState state : startStatesList) {
			//System.out.println("check5")
			if (!resultUPNFA.containsVertex(state)) {
				//System.out.println("check6")
				resultUPNFA.addVertex(state);
				if (m.isAcceptingState(new NFAVertexND(state.getStates()))) {
					//System.out.println("check7")
					resultUPNFA.addAcceptingState(state);
				}
				if (!visited.contains(state)) {
					//System.out.println("check8")
					toVisit.add(state);
				}
				//System.out.println("check9")
			}
			NFAEdge newEdge = new NFAEdge(newInitialState, state, new EpsilonTransitionLabel("ε" + priorityCounter));
			resultUPNFA.addEdge(newEdge);
			priorityCounter++;
		}
		while (!toVisit.isEmpty()) {
			//System.out.println("check10")
			if (isInterrupted()) {
				throw new InterruptedException();
			}
			UPNFAState currentState = toVisit.removeFirst();
			
			ArrayList<String> q = currentState.getStates();
			Set<NFAVertexND> P = currentState.getP();
			
			Set<NFAEdge> outgoingEdges = m.outgoingEdgesOf(new NFAVertexND(q));
			if (!outgoingEdges.isEmpty()) {
				//System.out.println("check11")
				NFAEdge edge = outgoingEdges.iterator().next();
				if (edge.getIsEpsilonTransition()) {
					//System.out.println("check12")
					LinkedList<NFAEdge> sortedEdges = new LinkedList<NFAEdge>(outgoingEdges);
					Collections.sort(sortedEdges);
					
					Set<NFAVertexND> s = new HashSet<NFAVertexND>();
					for (NFAEdge currentEdge : sortedEdges) {
						//System.out.println("check13: " + currentEdge.getSourceVertex() + "-" + currentEdge + "->" + currentEdge.getTargetVertex())
						if (isInterrupted()) {
							throw new InterruptedException();
						}
						NFAVertexND targetVertex = currentEdge.getTargetVertex();
						
						Set<NFAVertexND> newP = new HashSet<NFAVertexND>(P);
						newP.addAll(s);
						UPNFAState newUpNfaState = new UPNFAState(targetVertex.getStates(), newP);
						if (!resultUPNFA.containsVertex(newUpNfaState)) {
							//System.out.println("check14")
							resultUPNFA.addVertex(newUpNfaState);
							if (m.isAcceptingState(new NFAVertexND(targetVertex))) {
								//System.out.println("check15")
								resultUPNFA.addAcceptingState(newUpNfaState);
							}
							if (!visited.contains(newUpNfaState)) {
								//System.out.println("check16")
								toVisit.add(newUpNfaState);
								visited.add(newUpNfaState);
							}
							//System.out.println("check17")
						}
						
						NFAEdge newEdge = new NFAEdge(currentState, newUpNfaState, currentEdge.getTransitionLabel());					
						resultUPNFA.addEdge(newEdge);
						
						
						s.add(targetVertex);
					}
				} else {
					//System.out.println("check18")
					NFAVertexND targetVertex = edge.getTargetVertex();
					CharacterClassTransitionLabel symbolTransition = (CharacterClassTransitionLabel) edge.getTransitionLabel();
					CharacterClassTransitionLabel remainingSymbols = (CharacterClassTransitionLabel) symbolTransition.copy();
					HashMap<TransitionLabel, UPNFAState> newTransitionLabelToStateMap = new HashMap<TransitionLabel, UPNFAState>();
					HashMap<TransitionLabel, LinkedList<NFAVertexND>> labelToReachableStatesMap = new HashMap<TransitionLabel, LinkedList<NFAVertexND>>();
					// This for loop builds up labelToReachableStatesMap (does this preserve the priorities of P in labelToReachableStatesMap? Does it have to?)
					for (NFAVertexND p : P) {
						//System.out.println("check19")
						if (isInterrupted()) {
							throw new InterruptedException();
						}
						if (stateToSymbolMap.containsKey(p)) {
							//System.out.println("check20: " + p)
							TransitionLabel pSymbolTransition = stateToSymbolMap.get(p);
							LinkedList<NFAVertexND> epsilonClosure = new LinkedList<NFAVertexND>(stateToEpsilonClosureMap.get(p));
							if (labelToReachableStatesMap.containsKey(pSymbolTransition)) {
								// We already have this symbol transition, so only append the reachable states
								//System.out.println("check21")
								LinkedList<NFAVertexND> reachableStates = labelToReachableStatesMap.get(pSymbolTransition);
								/* We add one by one manually to interrupt */
								for (NFAVertexND epsilonClosureState : epsilonClosure) {
									//System.out.println("check22")
									if (isInterrupted()) {
										throw new InterruptedException();
									}
									reachableStates.add(epsilonClosureState);
								}
							} else {
								// Merge the symbol transition into the others with which it intersects
								//System.out.println("check23")
								TransitionLabel remainingPSymbols = pSymbolTransition.copy();
						
								LinkedList<TransitionLabel> transitionLabelsToRemove = new LinkedList<TransitionLabel>();
								LinkedList<TransitionLabel> transitionLabelsToAdd = new LinkedList<TransitionLabel>();
								LinkedList<LinkedList<NFAVertexND>> statesToAdd = new LinkedList<LinkedList<NFAVertexND>>();
								
								for (Map.Entry<TransitionLabel, LinkedList<NFAVertexND>> kv : labelToReachableStatesMap.entrySet()) {
									//System.out.println("check24")
									TransitionLabel currentTransitionLabel = kv.getKey();
									LinkedList<NFAVertexND> currentReachableStates = kv.getValue();
									
									// If the intersection is not empty, merge
									if (!remainingPSymbols.intersection(currentTransitionLabel).isEmpty()) {
										//System.out.println("check25")
										transitionLabelsToRemove.add(currentTransitionLabel);
										
										// The transition labels only in currentTransitionLabel
										TransitionLabel tl1 = currentTransitionLabel.intersection(remainingPSymbols.complement());
										if (!tl1.isEmpty()) {
											transitionLabelsToAdd.add(tl1);
											statesToAdd.add(currentReachableStates);
										}
										
										TransitionLabel intersection = remainingPSymbols.intersection(currentTransitionLabel);
										LinkedList<NFAVertexND> reachableStatesUnion = new LinkedList<NFAVertexND>(epsilonClosure);
										reachableStatesUnion.addAll(currentReachableStates);										
										transitionLabelsToAdd.add(intersection);
										statesToAdd.add(reachableStatesUnion);
										
										TransitionLabel tl3 = remainingPSymbols.intersection(currentTransitionLabel.complement());	
										remainingPSymbols = tl3;
										if (tl3.isEmpty()) {
											break;
										}
									}
								}
								//System.out.println("check26")
								for (TransitionLabel tl : transitionLabelsToRemove) {
									//System.out.println("check27")
									labelToReachableStatesMap.remove(tl);
								}
								Iterator<TransitionLabel> i0 = transitionLabelsToAdd.iterator();
								Iterator<LinkedList<NFAVertexND>> i1 = statesToAdd.iterator();
								while (i0.hasNext()) {
									//System.out.println("check28")
									labelToReachableStatesMap.put(i0.next(), i1.next());
								}
								//System.out.println("check29")
								if (!remainingPSymbols.isEmpty()) {
									//System.out.println("check30")
									labelToReachableStatesMap.put(remainingPSymbols, epsilonClosure);
								}
								//System.out.println("check31")
							}
						}
					}
					// Shouldn't this maybe be in order of priority? ()
					for (Map.Entry<TransitionLabel, LinkedList<NFAVertexND>> kv : labelToReachableStatesMap.entrySet()) {
						if (isInterrupted()) {
							throw new InterruptedException();
						}
						TransitionLabel currentTransitionLabel = kv.getKey();
						LinkedList<NFAVertexND> currentReachableStates = kv.getValue();
						//System.out.println("check32: " + symbolTransition + " " + currentTransitionLabel)
						
						// This should maybe be !currentTransitionLabel.intersection(remainingSymbols).isEmpty()
						if (!currentTransitionLabel.intersection(remainingSymbols).isEmpty()) {
							//System.out.println("check33")
							// This should maybe be TransitionLabel newTransitionLabel = currentTransitionLabel.intersection(remainingSymbols);
							TransitionLabel newTransitionLabel = currentTransitionLabel.intersection(symbolTransition);
							Set<NFAVertexND> newP = new HashSet<NFAVertexND>(currentReachableStates);
							
							if (newTransitionLabelToStateMap.containsKey(newTransitionLabel)) {
								//System.out.println("check34")
								UPNFAState newState = newTransitionLabelToStateMap.get(newTransitionLabel);
								// This should never happen...
								newState.getP().addAll(newP);
							} else {
								//System.out.println("check35")
								UPNFAState newState = new UPNFAState(targetVertex.getStates(), newP);
								newTransitionLabelToStateMap.put(newTransitionLabel, newState);
							}
							//System.out.println("check36")
							remainingSymbols = (CharacterClassTransitionLabel) remainingSymbols.intersection(newTransitionLabel.complement());
							
						}
						//System.out.println("check37")
						
					}
					//System.out.println("check38")
					
					if (!remainingSymbols.isEmpty()) {
						//System.out.println("check39")
						HashSet<NFAVertexND> emptySet = new HashSet<NFAVertexND>();
						UPNFAState newState = new UPNFAState(targetVertex.getStates(), emptySet);
						newTransitionLabelToStateMap.put(remainingSymbols, newState);
					}
					//System.out.println("check40")
					for (Map.Entry<TransitionLabel, UPNFAState> kv : newTransitionLabelToStateMap.entrySet()) {
						//System.out.println("check41")
						if (isInterrupted()) {
							throw new InterruptedException();
						}
						UPNFAState targetState = kv.getValue();
						TransitionLabel tl = kv.getKey();
						
						if (!resultUPNFA.containsVertex(targetState)) {
							//System.out.println("check42")
							resultUPNFA.addVertex(targetState);
							if (m.isAcceptingState(new NFAVertexND(targetVertex))) {
								//System.out.println("check43")
								resultUPNFA.addAcceptingState(targetState);
							}
						}
						//System.out.println("check44")
						
						NFAEdge newEdge = new NFAEdge(currentState, targetState, tl);
						resultUPNFA.addEdge(newEdge);
						
						if (!visited.contains(targetState)) {
							//System.out.println("check45")
							toVisit.add(targetState);
							visited.add(targetState);
						}
						//System.out.println("check46")
						
					}
					//System.out.println("check47")
				}
				//System.out.println("check48")
			}
			//System.out.println("check49")
		}
		//System.out.println("UPNFA");
		//System.out.println(resultUPNFA);
		//System.out.println("END");
		return resultUPNFA;
		
	}
	
	protected boolean isInterrupted() {
		return Thread.currentThread().isInterrupted();
	}
	
	/* Assume NFA has no epsilon loops, only one start and one accept and every state has either epsilon transitions, or one symbol transition from it */
	private List<NFAVertexND> priorityBasedEpsilonClosureDFS(NFAGraph m, NFAVertexND startState) {
		LinkedList<NFAVertexND> terminalStates = new LinkedList<NFAVertexND>();
		Stack<NFAVertexND> toVisit = new Stack<NFAVertexND>();
		toVisit.push(startState);
		while (!toVisit.isEmpty()) {
			NFAVertexND currentState = toVisit.pop();
			LinkedList<NFAEdge> outgoingTransitions = new LinkedList<NFAEdge>(m.outgoingEdgesOf(currentState));
			if (!outgoingTransitions.isEmpty()) {
				NFAEdge edge = outgoingTransitions.iterator().next();
				NFAVertexND targetState = edge.getTargetVertex();
				if (edge.getIsEpsilonTransition()) {
					Collections.sort(outgoingTransitions, Collections.reverseOrder());
					for (NFAEdge e : outgoingTransitions) {
						targetState = e.getTargetVertex();
						toVisit.push(targetState);
					}
				} else if (edge.getTransitionType() == TransitionType.SYMBOL) {
					terminalStates.add(currentState);
				}

			} else if (m.isAcceptingState(currentState)) {
				terminalStates.add(currentState);
			}
		}
		return terminalStates;
	}
	
	/* Receives a list of states in order of priority and creates unprioritised states e.g. (q0, q1, q2 -> (q0, {}), (q1, {q0}), (q2, {q0, q1})) */
	private List<UPNFAState> constructUPStates(List<NFAVertexND> stateList) {
		Set<NFAVertexND> currentP = new HashSet<NFAVertexND>();
		List<UPNFAState> unprioritisedStatesList = new LinkedList<UPNFAState>();
		for (NFAVertexND q : stateList) {
			UPNFAState newUnprioritisedState = new UPNFAState(q.getStates(), currentP);
			unprioritisedStatesList.add(newUnprioritisedState);
			if (!currentP.contains(q)) {
				currentP.add(q);
			}
		}
		return unprioritisedStatesList;
		
	}

	
	/* This class is to imitate the # transition described in Theorem 5 of General Algorithms for Testing the Ambiguity of Finite Automata by Cyril Allauzen et al, on page 9. */
	public static class IdaSpecialTransitionLabel implements TransitionLabel {

		@Override
		public boolean matches(String word) {
			return false;
		}

		@Override
		public boolean matches(TransitionLabel tl) {
			return false;
		}

		@Override
		public TransitionLabel intersection(TransitionLabel tl) {
			throw new UnsupportedOperationException("The intersection operation is invalid for special transitions.");
		}
		
		@Override
		public TransitionLabel union(TransitionLabel tl) {
			throw new UnsupportedOperationException("The union operation is invalid for special transitions.");
		}

		@Override
		public TransitionLabel complement() {
			throw new UnsupportedOperationException("The complement operation is invalid for special transitions.");
		}

		@Override
		public boolean isEmpty() {
			return false;
		}

		
		@Override
		public String getSymbol() {
			throw new UnsupportedOperationException("Special transitions contain multiple symbols");
		}
		
		@Override
		public String toString() {
			return "#(" + transitionLabels + ")";
		}

		@Override
		public TransitionLabel copy() {
			return new IdaSpecialTransitionLabel(transitionLabels);
		}

		@Override
		public TransitionType getTransitionType() {
			return TransitionType.OTHER;
		}
		
		/* Represents a transition label in this scc, for when calculating the degree */
		private final LinkedList<TransitionLabel> transitionLabels;
		public LinkedList<TransitionLabel> getTransitionLabels() {
			return transitionLabels;
		}
		
		public IdaSpecialTransitionLabel(LinkedList<TransitionLabel> transitionLabels) {
			this.transitionLabels = transitionLabels;
		}
		
	}
}
