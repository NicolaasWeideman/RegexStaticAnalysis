package nfa;

import nfa.transitionlabel.TransitionLabel;
import nfa.transitionlabel.TransitionLabelParserRecursive;
import nfa.transitionlabel.CharacterClassTransitionLabel;
import nfa.transitionlabel.EpsilonTransitionLabel;
import nfa.transitionlabel.EmptyTransitionLabelException;

/**
 * An NFA edge specifically used in the Mohri Filter.
 * 
 * @author N. H. Weideman
 *
 */

public class FilterEdge extends NFAEdge {

	private static final long serialVersionUID = 1L;
	/* the transition character passed to m2 */
	private TransitionLabel outGoingTransitionCharacter;

	public TransitionLabel getOutGoingTransitionCharacter() {
		return outGoingTransitionCharacter;
	}

	public void setOutGoingTransitionCharacter(String outGoingTransitionCharacter) {
		TransitionLabelParserRecursive tlpr = new TransitionLabelParserRecursive(outGoingTransitionCharacter);
		this.outGoingTransitionCharacter = tlpr.parseTransitionLabel();
	}

	public FilterEdge(NFAVertexND sourceVertex, NFAVertexND targetVertex, String transitionCharacter, String outGoingTransitionCharacter) throws EmptyTransitionLabelException {
		super(sourceVertex, targetVertex, transitionCharacter);
		TransitionLabelParserRecursive tlpr = new TransitionLabelParserRecursive(outGoingTransitionCharacter);
		this.outGoingTransitionCharacter = tlpr.parseTransitionLabel();
	}
	
	public FilterEdge(NFAVertexND sourceVertex, NFAVertexND targetVertex, TransitionLabel transitionCharacter, TransitionLabel outGoingTransitionCharacter) {
		super(sourceVertex, targetVertex, transitionCharacter);
		this.outGoingTransitionCharacter = outGoingTransitionCharacter;
	}

	@Override
	public NFAEdge copy() {
		return new FilterEdge(super.getSourceVertex(), super.getTargetVertex(), super.getTransitionLabel(), outGoingTransitionCharacter);
	}

	@Override
	public boolean isTransitionFor(String word) {
		if (super.getIsEpsilonTransition()) {
			/*
			 * if this is an epsilon transition, it is only a transition for the
			 * word if they both represent the same epsilon transition
			 */
			return super.isTransitionFor(word);
		} else {
			/*
			 * if this is not an epsilon transition, it is only a transition for
			 * the word if the word also is not an epsilon transition
			 */
			return !word.matches("ε\\d*");
		}
	}
	
	@Override
	public boolean isTransitionFor(TransitionLabel tl) {
		if (super.getIsEpsilonTransition()) {
			if (tl instanceof CharacterClassTransitionLabel) {
				/* CharacterClassTransitionLabels cannot match epsilon transtions */
				return false;
			}
			EpsilonTransitionLabel stl = (EpsilonTransitionLabel) tl;
			String word = stl.getSymbol();
			/*
			 * if this is an epsilon transition, it is only a transition for the
			 * word if they both represent the same epsilon transition
			 */
			return super.isTransitionFor(word);
		} else {
			
			if (tl instanceof CharacterClassTransitionLabel) {
				/* we assume character classes cannot be empty transitions */
				return true;
			} else {
				return false;
			}

			
		}
	}

	@Override
	public boolean equals(Object o) {
		if (o == null) {
			return false;
		}
		if (!o.getClass().isAssignableFrom(this.getClass())) {
			return false;
		}
		FilterEdge n = (FilterEdge) o;
		boolean condition1 = super.getSourceVertex().equals(n.getSourceVertex());
		boolean condition2 = super.getTargetVertex().equals(n.getTargetVertex());
		boolean condition3 = super.getTransitionLabel().equals(n.getTransitionLabel());
		boolean condition4 = outGoingTransitionCharacter.equals(n.getOutGoingTransitionCharacter());
		return condition1 && condition2 && condition3 && condition4;

	}

	@Override
	public String toString() {
		return super.getTransitionLabel() + ":" + outGoingTransitionCharacter;
	}

	@Override
	public int hashCode() {
		return super.getSourceVertex().hashCode() + super.getTargetVertex().hashCode() + super.getTransitionLabel().hashCode()
				+ outGoingTransitionCharacter.hashCode();
	}

}
