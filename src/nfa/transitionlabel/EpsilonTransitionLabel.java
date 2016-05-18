package nfa.transitionlabel;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EpsilonTransitionLabel implements TransitionLabel, Comparable<EpsilonTransitionLabel> {

	private final String transitionLabel;
	
	private final int priority;
	
	public EpsilonTransitionLabel(String transitionLabel) {
		Pattern pattern = Pattern.compile("ε(\\d*)");
		Matcher matcher = pattern.matcher(transitionLabel);
		if (!matcher.matches()) {
			throw new IllegalArgumentException("Transition label must be of the format ε\\d*");
		} else {
			priority = Integer.parseInt(matcher.group(1));
		}
		this.transitionLabel = transitionLabel;
	}
	
	@Override
	public boolean matches(String word) {
		return transitionLabel.equals(word);
	}

	@Override
	public boolean matches(TransitionLabel tl) {
		if (tl instanceof CharacterClassTransitionLabel) {
			return false;
		}
		EpsilonTransitionLabel etl = (EpsilonTransitionLabel) tl;
		return matches(etl.transitionLabel);
	}

	@Override
	public TransitionLabel intersection(TransitionLabel tl) {
		throw new UnsupportedOperationException("The intersection operation is invalid for epsilon transitions.");
	}
	
	@Override
	public TransitionLabel union(TransitionLabel tl) {
		throw new UnsupportedOperationException("The union operation is invalid for epsilon transitions.");
	}

	@Override
	public TransitionLabel complement() {
		throw new UnsupportedOperationException("The complement operation is invalid for epsilon transitions.");

	}

	@Override
	public boolean isEmpty() {
		return false; /* Note: Empty in this case means something such as [A&&B] */
	}

	@Override
	public String getSymbol() {
		return transitionLabel;
	}

	@Override
	public TransitionLabel copy() {
		return new EpsilonTransitionLabel(transitionLabel);
	}
	
	@Override
	public boolean equals(Object o) {
		if (o == null) {
			return false;
		}
		if (this == o) {
			return true;
		}
		
		if (!(o instanceof EpsilonTransitionLabel)) {
			return false;
		}
		
		EpsilonTransitionLabel etl = (EpsilonTransitionLabel) o;
		return transitionLabel.equals(etl.transitionLabel);
	}
	
	@Override
	public String toString() {
		return transitionLabel;
	}
	
	@Override
	public int hashCode() {
		return transitionLabel.hashCode();
	}

	@Override
	public TransitionType getTransitionType() {
		return TransitionType.EPSILON;
	}


	@Override
	public int compareTo(EpsilonTransitionLabel o) {
		return priority - o.priority;
	}

}
