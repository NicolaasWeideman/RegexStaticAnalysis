package analysis;

import analysis.AnalysisSettings.PriorityRemovalStrategy;

import nfa.NFAGraph;

abstract class EdaAnalysisResults extends AnalysisResults {
	enum EdaCases {PARALLEL, ESCC, FILTER, NO_EDA}
	public final EdaCases edaCase;
	
	private PriorityRemovalStrategy priorityRemovalStrategy;
	public PriorityRemovalStrategy getPriorityRemovalStrategy() {
		return priorityRemovalStrategy;
	}
	public void setPriorityRemovalStrategy(PriorityRemovalStrategy priorityRemovalStrategy) {
		this.priorityRemovalStrategy = priorityRemovalStrategy;
	}
	
	protected EdaAnalysisResults(NFAGraph originalGraph, EdaCases edaCase) {
		super(originalGraph);
		this.edaCase = edaCase;
	}
	
}
