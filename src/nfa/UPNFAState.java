package nfa;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class UPNFAState extends NFAVertexND {
	
	private final Set<NFAVertexND> P;
	public Set<NFAVertexND> getP() {
		return P;
	}
	
	public UPNFAState(Collection<String> q, Set<NFAVertexND> P) {
		super(q);
		this.P = new HashSet<NFAVertexND>(P);
	}
	
	@Override
	public UPNFAState copy() {
		return new UPNFAState(super.getStates(), P);
	}
	
	@Override
	public boolean equals(Object o) {
		boolean superEquals = super.equals(o);
		if (!superEquals) {
			return false;
		}
		if (!(o instanceof UPNFAState)) {
			return false;
		}
		UPNFAState p = (UPNFAState) o;
		return P.equals(p.P);
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("({");
		Iterator<String> i0 = getStates().iterator();
		while (i0.hasNext()) {
			String state = i0.next();
			sb.append(state);
			if (i0.hasNext()) {
				sb.append(", ");
			}
		}
		sb.append("}, {");
		Iterator<NFAVertexND> i1 = P.iterator();
		while (i1.hasNext()) {
			NFAVertexND state = i1.next();
			sb.append(state);
			if (i1.hasNext()) {
				sb.append(", ");
			}
		}
		sb.append("}}");
		
		return sb.toString();
	}
}
