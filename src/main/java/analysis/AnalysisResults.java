package analysis;

import nfa.NFAGraph;

public abstract class AnalysisResults {
	
	protected final NFAGraph originalGraph;
	public NFAGraph getOriginalGraph() {
		return originalGraph;
	}
	
	public AnalysisResults(NFAGraph originalGraph) {
		this.originalGraph = originalGraph;
	}

}
