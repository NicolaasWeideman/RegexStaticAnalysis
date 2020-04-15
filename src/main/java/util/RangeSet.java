package util;

import java.util.*;

import util.RangeSet.Range;

public class RangeSet implements Iterable<Range> {
	
	private final int rangesLowerBound; /* inclusive */
	private final int rangesUpperBound; /* exclusive */
	
	private TreeSet<Range> ranges;
	
	public RangeSet(int rangesLowerBound, int rangesUpperBound) {
		this.rangesLowerBound = rangesLowerBound;
		this.rangesUpperBound = rangesUpperBound;
		
		ranges = new TreeSet<Range>();
	}
	
	public RangeSet(RangeSet rs) {
		rangesLowerBound = rs.rangesLowerBound;
		rangesUpperBound = rs.rangesUpperBound;
		ranges = new TreeSet<Range>(rs.ranges);
	}
	
	public Range createRange(int num) {
		return new Range(num);
	}
	
	public Range createRange(int low, int high) {
		return new Range(low, high);
	}
	
	public void union(RangeSet rs) {
		ranges.addAll(rs.ranges);	
		mergeRanges();
	}
	
	public void union(Range r) {
		ranges.add(r);
		mergeRanges();	
	}
	
	public void union(List<Range> rangesToAdd) {
		ranges.addAll(rangesToAdd);	
		mergeRanges();		
	}
	
	public void intersection(RangeSet rs) {
		complement();
		rs.complement();	
		union(rs);
		complement();
	}
	
	public void complement() {
		if (ranges.isEmpty()) {
			union(new Range(rangesLowerBound, rangesUpperBound));
			return;
		}
		TreeSet<Range> rangesComplement = new TreeSet<Range>();
		Iterator<Range> i0 = ranges.iterator();
		Range currentRange = i0.next();
		if (currentRange.low != rangesLowerBound) {
			rangesComplement.add(new Range(rangesLowerBound, currentRange.low));
		}
		
		while (i0.hasNext()) {
			int newLow = currentRange.high;
			currentRange = i0.next();
			int newHigh = currentRange.low;
			Range newRange = new Range(newLow, newHigh);
			rangesComplement.add(newRange);
		}
		
		if (currentRange.high != rangesUpperBound) {
			rangesComplement.add(new Range(currentRange.high, rangesUpperBound));
		}
		ranges = rangesComplement;
	}
	
	public Set<Integer> discretize() {
		Set<Integer> values = new HashSet<Integer>();
		for (Range r : ranges) {
			for (int i = r.low; i < r.high; i++) {
				values.add(i);
			}
		}
		
		return values;
	}
	
	private void mergeRanges() {
		List<Range> l = new LinkedList<Range>(ranges);
		for (int i = 0; i < l.size() - 1; ) {
			Range r1 = l.get(i);
			Range r2 = l.get(i + 1);
			if (r1.overlapsAdjacent(r2)) {
				l.set(i, r1.merge(r2));
				l.remove(i + 1);
			} else {
				i++;
			}
		}
		ranges = new TreeSet<Range>(l);
	}
	
	public int sampleRangeSet() {
		if (ranges.isEmpty()) {
			throw new IllegalStateException("Cannot sample from empty range set.");
		}
		Range first = ranges.iterator().next();
		return first.low;
		
	}
	
	public boolean isEmpty() {
		return ranges.isEmpty();
	}
	
	@Override
	public boolean equals(Object o) {
		if (o == null) {
			return false;
		}
	    if (o == this) {
	    	return true;
	    }
	    if (!(o instanceof RangeSet)) {
	    	return false;
	    }
	    
	    RangeSet rs = (RangeSet) o;
	    /* TODO are two Range sets with different bounds, but equal ranges, equal?*/
	    if (ranges.size() != rs.ranges.size()) {
	    	return false;
	    }
	    
	    Iterator<Range> i0 = ranges.iterator();
	    Iterator<Range> i1 = rs.ranges.iterator();
	    while (i0.hasNext()) {
	    	Range r0 = i0.next();
	    	Range r1 = i1.next();
	    	if (!r0.equals(r1)) {
	    		return false;
	    	}
	    }
	    return true;
	}
	
	@Override
	public int hashCode() {
		return 17 * rangesLowerBound + 33 * rangesUpperBound + ranges.hashCode();
	}
	
	public boolean contains(int num) {
		Range numRange = new Range(num);
		for (Range r : ranges) {
			if (r.overlaps(numRange)) {
				return true;
			}
		}
		return false;
	}
	
	/*
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		int previous = rangesLowerBound;
		for (Range r : ranges) {
			for (int i = previous; i < r.low; i++) {
				sb.append(i + ",");
			}
			sb.append(r + ",");
			previous = r.high;
		}
		for (int i = previous; i < rangesUpperBound; i++) {
			sb.append(i + ",");
		}
		return sb.toString();
	}
	*/
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (Range r : ranges) {
			sb.append("[" + r.low + "," + r.high + ") ");
			
		}
		return sb.toString();
	}

	public class Range implements Comparable<Range> {
		public final int low; /* inclusive */
		public final int high; /* exclusive */
		
		public Range(int low, int high) {
			if (low < rangesLowerBound || high > rangesUpperBound + 1) {
				throw new IllegalArgumentException("Range exceeds bounds.");
			}
			if (low >= high) {
				throw new IllegalArgumentException("Low must less than high.");
			}
			this.low = low;
			this.high = high;
		}

		public Range(int num) {
			if (num < rangesLowerBound || num > rangesUpperBound + 1) {
				throw new IllegalArgumentException("Range exceeds bounds.");
			}
			this.low = num;
			this.high = num + 1;
		}
		
		public boolean overlaps(Range r) {
			return low < r.high && r.low < high;
		}
		
		public boolean overlapsAdjacent(Range r) {
			return low <= r.high && r.low <= high;
		}
		
		
		
		public Range merge(Range r) {
			int newLow = 0;
			int newHigh = 0;
			if (!overlapsAdjacent(r)) {
				throw new RuntimeException("Cannot merge non-overlapping ranges");
			}
			
			if (low <= r.low) {
				newLow = low;
			} else {
				newLow = r.low;
			}
			
			if (high >= r.high) {
				newHigh = high;
			} else {
				newHigh = r.high;
			}
			
			return new Range(newLow, newHigh);
		}
		
		public RangeSet complement() {
			if (low == rangesLowerBound && high == rangesUpperBound + 1) {
				/* empty range set */
				return new RangeSet(rangesLowerBound, rangesUpperBound);
			}
			if (low == rangesLowerBound) {
				/* only one range */
				Range rComplement = new Range(high, rangesUpperBound + 1);
				RangeSet rs = new RangeSet(rangesLowerBound, rangesUpperBound);
				rs.union(rComplement);
				return rs;
			}
			if (high == rangesUpperBound + 1) {
				/* only one range */
				Range rComplement = new Range(rangesLowerBound, low);
				RangeSet rs = new RangeSet(rangesLowerBound, rangesUpperBound);
				rs.union(rComplement);
				return rs;
			}
			
			Range r1 = new Range(rangesLowerBound, low);
			Range r2 = new Range(high, rangesUpperBound + 1);
			/* TODO there might be a better way to do this... */
			RangeSet comp1 = new RangeSet(rangesLowerBound, rangesUpperBound);
			comp1.union(r1);
			RangeSet comp2 = new RangeSet(rangesLowerBound, rangesUpperBound);
			comp2.union(r2);
			comp1.union(comp2);
			return comp1;
		}

		@Override
		public int compareTo(Range r) {
			if (low != r.low) {
				return low - r.low;
			}
			return high - r.high;
		}
		
		@Override
		public String toString() {
			return "[" + low + ", " + (high - 1) + "]";
		}
		
		@Override
		public boolean equals(Object o) {
			if (o == null) {
				return false;
			}
		    if (o == this) {
		    	return true;
		    }
		    if (!(o instanceof Range)) {
		    	return false;
		    }
		    
		    Range r = (Range) o;
		    return low == r.low && high == r.high;
		}
		
		@Override
		public int hashCode() {
			return 5 * low + 7 * high;
		}
	}

	
	public static void main(String [] args) {
		RangeSet rs1 =  new RangeSet(0, 256);
		Range r11 = rs1.createRange(0, 10);
		rs1.union(r11);
		Range r12 = rs1.createRange(20, 30);
		rs1.union(r12);
		Range r13 = rs1.createRange(40, 50);
		rs1.union(r13);
		
		RangeSet rs2 =  new RangeSet(0, 256);
		Range r21 = rs2.createRange(10, 20);
		rs2.union(r21);
		Range r22 = rs2.createRange(30, 40);
		rs1.union(r22);
		Range r23 = rs2.createRange(50, 60);
		rs2.union(r23);
		
		//rs1.union(rs2);
		
		System.out.println(rs1);
		System.out.println(rs2);
		rs1.intersection(rs2);
		System.out.println(rs1);
		
		RangeSet rs3 =  new RangeSet(0, 256);
		Range r31 = rs3.createRange(0, 256);
		rs3.union(r31);
		System.out.println(rs3);
		rs3.complement();
		System.out.println(rs3);
		rs3.complement();
		System.out.println(rs3);
	}

	@Override
	public Iterator<Range> iterator() {
		return ranges.iterator();
	}

}
