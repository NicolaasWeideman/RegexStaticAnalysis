package nfa.transitionlabel;

public class EmptyTransitionLabelException extends Exception {
	
	private static final long serialVersionUID = 1L;

	public EmptyTransitionLabelException(String label) {
		super("Empty transition label:" + label);
	}
}
