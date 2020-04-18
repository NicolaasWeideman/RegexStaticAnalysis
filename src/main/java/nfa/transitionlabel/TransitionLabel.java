package nfa.transitionlabel;

import java.util.regex.Pattern;

public interface TransitionLabel {
	
	public static final int MIN_16UNICODE = 0;
	public static final int MAX_16UNICODE = 65536;
	
	public static final int MIN_DIGIT = 48;
	public static final int MAX_DIGIT = 57;
	
	public static final int MIN_SPACE = 9;
	public static final int MAX_SPACE = 13;
	
	public static final int MIN_WORD1 = 65;
	public static final int MAX_WORD1 = 90;
	
	public static final int MIN_WORD2 = 95;
	public static final int MAX_WORD2 = 95;
	
	public static final int MIN_WORD3 = 97;
	public static final int MAX_WORD3 = 122;
	
	public static final int HORIZONTAL_TAB = 9;
	
	public static final int VERTICAL_TAB = 11;
	
	public enum TransitionType {
		EPSILON,
		SYMBOL,
		OTHER
	}
	
	public TransitionType getTransitionType();
	
	public abstract boolean matches(String word);
	
	public abstract boolean matches(TransitionLabel tl);
	
	public abstract TransitionLabel intersection(TransitionLabel tl);
	
	public abstract TransitionLabel union(TransitionLabel tl);
	
	public abstract TransitionLabel complement();
	
	public abstract boolean isEmpty();
	
	public abstract String getSymbol();

	public abstract TransitionLabel copy();
	
	public static void main(String [] args) {
		@SuppressWarnings("unused")
		Pattern p = Pattern.compile("\\p{ALPHA}");
	}
	
}
