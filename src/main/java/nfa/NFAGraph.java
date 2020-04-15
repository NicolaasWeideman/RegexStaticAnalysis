package nfa;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.Iterator;

import org.jgrapht.graph.DirectedPseudograph;

import nfa.transitionlabel.TransitionLabel;
import nfa.transitionlabel.TransitionLabel.TransitionType;

/**
 * A graph representing an NFA.
 * 
 * @author N. H. Weideman
 *
 */
public class NFAGraph extends DirectedPseudograph<NFAVertexND, NFAEdge> {
	private static final long serialVersionUID = 1L;

	/* The vertex representing the initial state of the NFA */
	private NFAVertexND initialState;

	public NFAVertexND getInitialState() {
		return initialState;
	}

	public void setInitialState(NFAVertexND initialState) {
		if (!super.containsVertex(initialState)) {
			throw new IllegalArgumentException("Graph does not contain vertex: " + initialState);
		}
		this.initialState = initialState;
	}

	/* The accepting states of the NFA */
	private HashSet<NFAVertexND> acceptingStates;

	public void addAcceptingState(NFAVertexND acceptingState) {
		if (!super.containsVertex(acceptingState)) {
			throw new IllegalArgumentException("Graph does not contain vertex: " + acceptingState);
		}
		acceptingStates.add(acceptingState);
	}

	public boolean isAcceptingState(String stateNumber) {
		return acceptingStates.contains(new NFAVertexND(stateNumber));
	}

	public boolean isAcceptingState(NFAVertexND state) {
		return acceptingStates.contains(state);
	}

	public void removeAcceptingState(NFAVertexND acceptingState) {
		if (!super.containsVertex(acceptingState)) {
			throw new IllegalArgumentException("Graph does not contains accepting state: " + acceptingState);
		}
		acceptingStates.remove(acceptingState);
	}

	public Set<NFAVertexND> getAcceptingStates() {
		return acceptingStates;
	}

	public NFAGraph() {
		super(NFAEdge.class);
		acceptingStates = new HashSet<NFAVertexND>();
	}

	/**
	 * @return A new instance of a NFAGraph equal to this instance
	 */
	public NFAGraph copy() {
		NFAGraph c = new NFAGraph();
		for (NFAVertexND v : super.vertexSet()) {
			c.addVertex(v.copy());
		}
		for (NFAEdge e : super.edgeSet()) {
			c.addEdge(e.copy());
		}

		if (initialState != null) {
			c.initialState = initialState.copy();
		}
		
		for (NFAVertexND v : acceptingStates) {
			c.addAcceptingState(v.copy());
		}
		return c;
	}

	/**
	 * Adds a new edge to the NFA graph
	 * 
	 * @param newEdge
	 *            The new edge to add
	 * @return true if this graph did not already contain the specified edge
	 */
	public boolean addEdge(NFAEdge newEdge) {
		if (newEdge == null) {
			throw new NullPointerException("New edge cannot be null");
		}
		if (newEdge.getTransitionLabel().isEmpty()) {
			return false;
		}
		
		NFAVertexND s = newEdge.getSourceVertex();
		NFAVertexND t = newEdge.getTargetVertex();
		if (super.containsEdge(newEdge)) {
			/* if the edge exists increase the number of its parallel edges */
			NFAEdge e = getEdge(newEdge);
			e.incNumParallel();
		} else if (newEdge.getIsEpsilonTransition()) {
			/* check if the NFA already has an epsilon transition between these states */
			Set<NFAEdge> es = super.getAllEdges(s, t);
			for (NFAEdge currentEdge : es) {
				if (currentEdge.equals(newEdge)) {
					/* if it does, add the new edge as a parallel edge (priorities don't matter between the same states) */
					currentEdge.incNumParallel();
					return true;
				}
			}
		} else {
			/* check if the new edge overlaps the current edges */
			Set<NFAEdge> es = super.getAllEdges(s, t);
			// TODO lightly tested
			for (NFAEdge currentEdge : es) {
				/* epsilon edges cannot overlap */
				if (currentEdge.getTransitionType() == TransitionType.SYMBOL) {
					TransitionLabel tlCurrentEdge = currentEdge.getTransitionLabel();
					TransitionLabel tlNewEdge = newEdge.getTransitionLabel();
					TransitionLabel intersection = tlNewEdge.intersection(tlCurrentEdge);
					if (!intersection.isEmpty()) {
						/* overlapping edge */
						TransitionLabel currentEdgeRelabel = tlCurrentEdge.intersection(tlNewEdge.complement());
						int currentEdgeWeight = 0;
						if (!currentEdgeRelabel.isEmpty()) {
							currentEdgeWeight = currentEdge.getNumParallel();
							removeEdge(currentEdge);
							
							NFAEdge currentEdgeRelabeled = new NFAEdge(s, t, currentEdgeRelabel);
							currentEdgeRelabeled.setNumParallel(currentEdgeWeight);
							addEdge(currentEdgeRelabeled);
						}
						
						
						NFAEdge overlappingEdge = new NFAEdge(s, t, intersection);
						overlappingEdge.setNumParallel(currentEdgeWeight + newEdge.getNumParallel());
						addEdge(overlappingEdge);
						
						TransitionLabel newEdgeRelabel = tlNewEdge.intersection(tlCurrentEdge.complement());
						if (!newEdgeRelabel.isEmpty()) {
							int newEdgeWeight = newEdge.getNumParallel();
							
							NFAEdge newEdgeRelabeled = new NFAEdge(s, t, newEdgeRelabel);
							newEdgeRelabeled.setNumParallel(newEdgeWeight);
							addEdge(newEdgeRelabeled);
						}

						
						return true;
						
					}
				}
				
			}
		}
		if (!super.containsVertex(newEdge.getSourceVertex())) {
			throw new IllegalArgumentException("Graph doesn't contain vertex: " + newEdge.getSourceVertex());
		}
		if (!super.containsVertex(newEdge.getTargetVertex())) {
			throw new IllegalArgumentException("Graph doesn't contain vertex: " + newEdge.getTargetVertex());
		}
		return super.addEdge(newEdge.getSourceVertex(), newEdge.getTargetVertex(), newEdge);
	}

	/**
	 * All the edges representing an epsilon transition from a vertex
	 * 
	 * @param v
	 *            The vertex to find the epsilon transitions from.
	 * @return A set of NFA edges representing the epsilon transitions.
	 */
	public Set<NFAEdge> outgoingEpsilonEdgesOf(NFAVertexND v) {
		Set<NFAEdge> allEdges = super.outgoingEdgesOf(v);

		Set<NFAEdge> toReturn = new HashSet<NFAEdge>();

		for (NFAEdge e : allEdges) {
			if (e.getIsEpsilonTransition()) {
				toReturn.add(e);
			}
		}

		return toReturn;
	}

	@Override
	public boolean addVertex(NFAVertexND v) {
		if (containsVertex(v)) {
			throw new IllegalArgumentException("Graph already contains vertex: " + v);
		}
		return super.addVertex(v);
	}
	
	public NFAEdge getEdge(NFAEdge e) {
		if (!super.containsEdge(e)) {
			throw new IllegalArgumentException("Graph does not contain edge: " + e.getSourceVertex() + "->" + e.getTargetVertex() + ":" + e.getTransitionLabel());
		}
		Set<NFAEdge> edges = super.getAllEdges(e.getSourceVertex(), e.getTargetVertex());
		for (NFAEdge currentE : edges) {
			if (currentE.equals(e)) {
				return currentE;
			}
		}
		return null;
	}

	@Override
	public boolean equals(Object o) {
		if (!super.equals(o)) {
			return false;
		}
		
		/* testing that the amount of parallel edges are equal */
		NFAGraph n = (NFAGraph) o;
		for (NFAEdge e : n.edgeSet()) {
			Set<NFAEdge> nEdges = super.getAllEdges(e.getSourceVertex(), e.getTargetVertex());
			for (NFAEdge nEdge : nEdges) {
				if (e.equals(nEdge) && e.getNumParallel() != nEdge.getNumParallel()) {
					return false;
				}
			}
			
		}
		
		if (initialState != null && !initialState.equals(n.getInitialState())) {
			return false;
		}
		
		HashSet<NFAVertexND> myAcceptingStates = new HashSet<NFAVertexND>(acceptingStates);
		HashSet<NFAVertexND> otherAcceptingStates = new HashSet<NFAVertexND>(n.getAcceptingStates());

		boolean condition1 = myAcceptingStates.size() == otherAcceptingStates.size();
		boolean condition2 = myAcceptingStates.containsAll(otherAcceptingStates);
		boolean condition3 = otherAcceptingStates.containsAll(myAcceptingStates);
		/* first condition might be redundant */
		return condition1 && condition2 && condition3 ;

	}
	
	public NFAGraph reverse() {
		NFAGraph reversedGraph = this.copy();
		
		for (NFAEdge e : edgeSet()) {
			NFAVertexND newSource = e.getTargetVertex();
			NFAVertexND newTarget = e.getSourceVertex();
			NFAEdge reversedEdge = new NFAEdge(newSource, newTarget, e.getTransitionLabel());
			reversedGraph.removeEdge(e);
			reversedGraph.addEdge(reversedEdge);
		}
		
		return reversedGraph;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("I:" + initialState + " A:");
		if (!acceptingStates.isEmpty()) {
			for (NFAVertexND a : acceptingStates) {
				sb.append(a + ";");
			}
		} else {
			sb.append("No Accepting states;");
		}

		
		return sb.toString() + " " + super.toString();
	}
			
	private String nameState(NFAVertexND v) {
			StringBuilder sb = new StringBuilder("\"");
			ArrayList<String> states = v.getStates();
			Collections.sort(states);
			Iterator<String> stateIterator = states.iterator();
			while (stateIterator.hasNext()) {
				sb.append(stateIterator.next());
				if (stateIterator.hasNext()) {
					sb.append(",");
				}
			}
			sb.append("\"");
			return sb.toString();
	}
}
