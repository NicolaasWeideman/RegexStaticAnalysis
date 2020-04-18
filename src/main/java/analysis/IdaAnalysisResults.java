package analysis;

import analysis.AnalysisSettings.PriorityRemovalStrategy;

import nfa.NFAGraph;

public abstract class IdaAnalysisResults extends AnalysisResults {
	enum IdaCases {IDA, NO_IDA}
	public final IdaCases idaCase;
	
	private PriorityRemovalStrategy priorityRemovalStrategy;
	public PriorityRemovalStrategy getPriorityRemovalStrategy() {
		return priorityRemovalStrategy;
	}
	public void setPriorityRemovalStrategy(PriorityRemovalStrategy priorityRemovalStrategy) {
		this.priorityRemovalStrategy = priorityRemovalStrategy;
	}
	
	protected IdaAnalysisResults(NFAGraph originalGraph, IdaCases idaCase) {
		super(originalGraph);
		this.idaCase = idaCase;
	}

}
