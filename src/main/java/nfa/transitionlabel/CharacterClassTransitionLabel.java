package nfa.transitionlabel;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import util.RangeSet;
import util.RangeSet.Range;

public class CharacterClassTransitionLabel implements TransitionLabel, Comparable<CharacterClassTransitionLabel> {
	
	private final char PREFERRED_CHAR1 = 'a';
	private final char PREFERRED_CHAR2 = '$';
	
	private static final boolean WILDCARD_MATCHES_NEWLINE = true;
	
	public static CharacterClassTransitionLabel wildcardLabel() {
		CharacterClassTransitionLabel wildcardLabel = new CharacterClassTransitionLabel(predefinedRangeWildcard());
		return wildcardLabel;
	}
	
	public static RangeSet predefinedRangeWildcard() {
		RangeSet ranges = new RangeSet(MIN_16UNICODE, MAX_16UNICODE);
		
		if (WILDCARD_MATCHES_NEWLINE) {
			Range wildCardRange = ranges.createRange(MIN_16UNICODE, MAX_16UNICODE);
			ranges.union(wildCardRange);
		} else {
			int newLineInt = (int) '\n';
			Range wildCardRange1 = ranges.createRange(MIN_16UNICODE, newLineInt);
			Range wildCardRange2 = ranges.createRange(newLineInt + 1, MAX_16UNICODE);
			ranges.union(wildCardRange1);
			ranges.union(wildCardRange2);
		}		
		
		return ranges;
	}
	
	public static RangeSet predefinedRangeSetDigits() {
		RangeSet ranges = new RangeSet(MIN_16UNICODE, MAX_16UNICODE);
		Range digitsRange =ranges.createRange(MIN_DIGIT, MAX_DIGIT + 1);
		ranges.union(digitsRange);
		return ranges;
	}
	
	public static RangeSet predefinedRangeSetWhiteSpaces() {
		RangeSet ranges = new RangeSet(MIN_16UNICODE, MAX_16UNICODE);
		Range spacesRange = ranges.createRange(MIN_SPACE, MAX_SPACE + 1);
		ranges.union(spacesRange);
		return ranges;
	}
	
	public static RangeSet predefinedRangeSetWordCharacters() {
		RangeSet ranges = new RangeSet(MIN_16UNICODE, MAX_16UNICODE);
		Range wordsRange1 = ranges.createRange(MIN_WORD1, MAX_WORD1 + 1);
		Range wordsRange2 = ranges.createRange(MIN_WORD2, MAX_WORD2 + 1);
		Range wordsRange3 = ranges.createRange(MIN_WORD3, MAX_WORD3 + 1);
		ranges.union(wordsRange1);
		ranges.union(wordsRange2);
		ranges.union(wordsRange3);
		return ranges;
	}
	
	public static RangeSet predefinedRangeSetVerticalTab() {
		RangeSet ranges = new RangeSet(MIN_16UNICODE, MAX_16UNICODE);
		Range verticalTabRange = ranges.createRange(VERTICAL_TAB);
		ranges.union(verticalTabRange);
		return ranges;
	}
	
	public static RangeSet predefinedRangeSetHorizontalTab() {
		RangeSet ranges = new RangeSet(MIN_16UNICODE, MAX_16UNICODE);
		Range horizontalTabRange = ranges.createRange(HORIZONTAL_TAB);
		ranges.union(horizontalTabRange);
		return ranges;
	}

	private final RangeSet ranges;
	public CharacterClassTransitionLabel() {
		ranges  = new RangeSet(MIN_16UNICODE, MAX_16UNICODE);
	}
	
	public CharacterClassTransitionLabel(String word) {
		
		if (word.length() > 1) {
			throw new IllegalArgumentException("Parameter word must be of length 1.");
		}
		
		this.ranges  = new RangeSet(MIN_16UNICODE, MAX_16UNICODE);
		Range r = this.ranges.createRange((int) word.charAt(0));
		this.ranges.union(r);
	}
	
	public CharacterClassTransitionLabel(RangeSet ranges) {
		this.ranges  = new RangeSet(ranges);
	}
	
	@Override
	public boolean matches(String word) {
		TransitionLabelParserRecursive tlpr = new TransitionLabelParserRecursive(word);
		return matches(tlpr.parseTransitionLabel());
	}
	
	public boolean matches(TransitionLabel tl) {
		
		if (tl instanceof CharacterClassTransitionLabel) {
			CharacterClassTransitionLabel cctl = (CharacterClassTransitionLabel) tl;
			
			RangeSet rs1 = new RangeSet(ranges);
			RangeSet rs2 = new RangeSet(cctl.ranges);
			rs1.intersection(rs2);
			return !rs1.isEmpty();
		} else {
			return false;
		}
	}
	
	@Override
	public TransitionLabel intersection(TransitionLabel tl) {
		if (tl instanceof CharacterClassTransitionLabel) {
			CharacterClassTransitionLabel cctl = (CharacterClassTransitionLabel) tl;
			
			RangeSet rs1 = new RangeSet(ranges);
			RangeSet rs2 = new RangeSet(cctl.ranges);
			rs1.intersection(rs2);
			return new CharacterClassTransitionLabel(rs1);
		} else {
			throw new IllegalArgumentException("Invalid TransitionLabel type.");
		}
	}
	
	@Override
	public TransitionLabel union(TransitionLabel tl) {
		if (tl instanceof CharacterClassTransitionLabel) {
			CharacterClassTransitionLabel cctl = (CharacterClassTransitionLabel) tl;
			
			RangeSet rs1 = new RangeSet(ranges);
			RangeSet rs2 = new RangeSet(cctl.ranges);
			rs1.union(rs2);
			return new CharacterClassTransitionLabel(rs1);
		} else {
			throw new IllegalArgumentException("Invalid TransitionLabel type.");
		}
	}
	
	@Override
	public TransitionLabel complement() {
		RangeSet rs = new RangeSet(ranges);
		rs.complement();
		return new CharacterClassTransitionLabel(rs);
	}
	
	public boolean isEmpty() {
		return ranges.isEmpty();
	}

	@Override
	public String getSymbol() {
		if (ranges.contains((int) PREFERRED_CHAR1)) {
			return "" + PREFERRED_CHAR1;
		} else if (ranges.contains((int) PREFERRED_CHAR2)) {
			return "" + PREFERRED_CHAR2;
		}
		return "" + ((char) ranges.sampleRangeSet());
	}
	
	@Override
	public TransitionLabel copy() {
		CharacterClassTransitionLabel toReturn = new CharacterClassTransitionLabel(ranges);
		assert equals(toReturn) : this + " " + toReturn;
		return toReturn;
	}
	
	@Override
	public boolean equals(Object o) {
		
		if (o == null) {
			return false;
		}
	    if (o == this) {
	    	return true;
	    }
	    if (!(o instanceof CharacterClassTransitionLabel)) {
	    	return false;
	    }
	    
	    CharacterClassTransitionLabel cctl = (CharacterClassTransitionLabel) o;
	    return cctl.ranges.equals(ranges);
	}
	

	public Set<String> getSymbols() {
		Set<Integer> symbolCodes = ranges.discretize();
		Set<String> symbols = new HashSet<String>();
		for (int sc : symbolCodes) {
			symbols.add("" + ((char) sc));
		}
		return symbols;
	}
	
	@Override
	public int hashCode() {
		return ranges.hashCode();
	}
	
	@Override
	public String toString() {
		if (ranges.isEmpty()) {
			return "[]";
		}
		StringBuilder sb = new StringBuilder("[");
		Iterator<Range> i0 = ranges.iterator();
		while (i0.hasNext()) {
			Range r = i0.next();
			String lowString;
			if ('!' <= r.low && r.low <= '~') {
				lowString = "" + ((char) + r.low);
			} else {
				lowString = String.format("\\x{%02x}", r.low);
			}
			String highString;
			if ('!' <= r.high && r.high <= '~') {
				highString = "" + (char) + (r.high - 1);
			} else {
				highString = String.format("\\x{%02x}", r.high - 1);
			}
			if (r.low == r.high - 2) {
				/* Print adjacent characters as [ab] (remember high is exclusive) */
				sb.append(lowString + highString);
			} else if (r.low != r.high - 1) {
				/* Print non adjacent characters as [<low>-<high>] */
				sb.append(lowString+ "-" + highString);

			} else {
				sb.append(lowString);
			}
			
		}
		sb.append("]");
		return sb.toString();
	}

	@Override
	public TransitionType getTransitionType() {
		return TransitionType.SYMBOL;
	}

	@Override
	public int compareTo(CharacterClassTransitionLabel cctl) {

		return ranges.sampleRangeSet() - cctl.ranges.sampleRangeSet();
	}

}
