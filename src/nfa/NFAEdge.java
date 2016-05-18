package nfa;

import org.jgrapht.graph.DefaultEdge;

import nfa.transitionlabel.TransitionLabel;
import nfa.transitionlabel.CharacterClassTransitionLabel;
import nfa.transitionlabel.TransitionLabel.TransitionType;
import nfa.transitionlabel.EpsilonTransitionLabel;
import nfa.transitionlabel.EmptyTransitionLabelException;
import nfa.transitionlabel.TransitionLabelParserRecursive;

/**
 * An edge used to represent one of the transitions in an NFA.
 * 
 * @author N. H. Weideman
 *
 */
public class NFAEdge extends DefaultEdge implements Comparable<NFAEdge> {
	/* required from super class */
	private static final long serialVersionUID = 1L;

	/* the vertex this edge is coming from */
	private final NFAVertexND sourceVertex;

	public NFAVertexND getSourceVertex() {
		return sourceVertex;
	}

	/* the vertex this edge is going to */
	private final NFAVertexND targetVertex;

	public NFAVertexND getTargetVertex() {
		return targetVertex;
	}
	
	/* number of parallel edges */
	private int numParallel;
	
	public int getNumParallel() {
		return numParallel;
	}
	
	public void setNumParallel(int numParallel) {
		this.numParallel = numParallel;
	}
	
	public void incNumParallel() {
		this.numParallel++;
	}

	/* The word associated with this transition in the NFA */
	private TransitionLabel transitionLabel;
	public TransitionLabel getTransitionLabel() {
		return transitionLabel;
	}

	public String getATransitionCharacter() {
		return transitionLabel.getSymbol();
	}

	public void setTransitionLabel(String transitionLabelString) {
		TransitionLabelParserRecursive tlp = new TransitionLabelParserRecursive(transitionLabelString);
		this.transitionLabel = tlp.parseTransitionLabel();
	}
	
	public void setTransitionLabel(TransitionLabel transitionLabel) {
		this.transitionLabel = transitionLabel;
		
	}

	private final boolean isEpsilonTransition;

	public boolean getIsEpsilonTransition() {
		return isEpsilonTransition;
	}
	
	public TransitionType getTransitionType() {
		return transitionLabel.getTransitionType();
	}

	public NFAEdge(NFAVertexND sourceVertex, NFAVertexND targetVertex,
			String transitionLabelString) throws EmptyTransitionLabelException {
		
		if (sourceVertex == null || targetVertex == null
				|| transitionLabelString == null) {
			throw new NullPointerException("Null parameters are not allowed.");
		}

		this.sourceVertex = sourceVertex;
		this.targetVertex = targetVertex;
		
		TransitionLabelParserRecursive tlpr = new TransitionLabelParserRecursive(transitionLabelString);
		this.transitionLabel = tlpr.parseTransitionLabel();
		if (transitionLabel.isEmpty()) {
			throw new EmptyTransitionLabelException(transitionLabelString);
		}
		this.numParallel = 1;

		this.isEpsilonTransition = isEpsilonCharacter(transitionLabel.getSymbol());
	}
	
	public NFAEdge(NFAVertexND sourceVertex, NFAVertexND targetVertex,
			TransitionLabel transitionLabel) {

		if (sourceVertex == null || targetVertex == null
				|| transitionLabel == null) {
			throw new NullPointerException("Null parameters are not allowed.");
		}
		
		if (transitionLabel.isEmpty()) {
			throw new IllegalArgumentException("The transition label of an edge cannot be empty.");
		}

		this.sourceVertex = sourceVertex;
		this.targetVertex = targetVertex;
		
		this.transitionLabel = transitionLabel;
		this.numParallel = 1;

		this.isEpsilonTransition = transitionLabel.getTransitionType() == TransitionType.EPSILON;
	}

	private boolean isEpsilonCharacter(String transitionLabelString) {
		return transitionLabelString.matches("ε\\d*");
	}

	/**
	 * @return A new instance of an NFAEdge equal to this instance.
	 */
	public NFAEdge copy() {
		NFAEdge newEdge = new NFAEdge(sourceVertex, targetVertex, transitionLabel);
		newEdge.setNumParallel(numParallel);
		return newEdge;
	}

	@Override
	public boolean equals(Object o) {
		if (o == null) {
			return false;
		}
		if (!o.getClass().isAssignableFrom(this.getClass())) {
			return false;
		}
		NFAEdge n = (NFAEdge) o;
		boolean test1 = sourceVertex.equals(n.getSourceVertex());
		boolean test2 = targetVertex.equals(n.getTargetVertex());
		boolean test3 = transitionLabel.equals(n.getTransitionLabel());
		return test1 && test2 && test3;

	}

	@Override
	public int hashCode() {
		return sourceVertex.hashCode() + targetVertex.hashCode()
				+ transitionLabel.hashCode();
	}

	@Override
	public String toString() {
		return transitionLabel + "*" + numParallel;
	}
	
	public boolean isTransitionFor(String word) {
		/* this method distinguishes between epsilon transitions, if this is unwanted rather use getIsEpsilonTransition on both */
		return transitionLabel.matches(word);		
	}
	
	public boolean isTransitionFor(TransitionLabel tl) {
		/* this method distinguishes between epsilon transitions, if this is unwanted rather use getIsEpsilonTransition on both */
		return transitionLabel.matches(tl);		
	}

	@Override
	public int compareTo(NFAEdge o) {
		if (!getIsEpsilonTransition() && o.getIsEpsilonTransition()) {
			return -1;
		} else if (getIsEpsilonTransition() && !o.getIsEpsilonTransition()) {
			return 1;
		} else if (getIsEpsilonTransition() && o.getIsEpsilonTransition()) {
			EpsilonTransitionLabel etl = (EpsilonTransitionLabel) transitionLabel;
			EpsilonTransitionLabel oetl = (EpsilonTransitionLabel) o.transitionLabel;
			return etl.compareTo(oetl);
		} else {
			CharacterClassTransitionLabel cctl = (CharacterClassTransitionLabel) transitionLabel;
			CharacterClassTransitionLabel occtl = (CharacterClassTransitionLabel) o.transitionLabel;
			return cctl.compareTo(occtl);
		}


	}
}
