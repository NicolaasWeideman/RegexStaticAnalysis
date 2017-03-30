package analysis;

import java.util.LinkedList;
import nfa.NFAGraph;
import nfa.NFAVertexND;
import nfa.NFAEdge;

public interface NFAAnalyserInterface {	
	
	public static enum AnalysisResultsType {
		EDA,
		NO_EDA,
		IDA,
		NO_IDA,
		TIMEOUT_IN_EDA,
		TIMEOUT_IN_IDA,
		ANALYSIS_FAILED
		
	}
		
	static final class EdaAnalysisResultsNoEda extends EdaAnalysisResults {
		
		EdaAnalysisResultsNoEda(NFAGraph originalGraph) {
			super(originalGraph, EdaCases.NO_EDA);
		}
	}
	
	/* ============= Results Objects ============= */
	
	static final class EdaAnalysisResultsParallel extends EdaAnalysisResults {

		private final NFAGraph mergedScc;
		public NFAGraph getMergedScc() {
			return mergedScc;
		}

		private final NFAVertexND sourceVertex;
		public NFAVertexND getSourceVertex() {
			return sourceVertex;
		}
		
		private final NFAEdge parallelEdge;
		public NFAEdge getParallelEdge() {
			return parallelEdge;
		}

		EdaAnalysisResultsParallel(NFAGraph originalGraph, NFAGraph mergedScc, NFAVertexND sourceVertex, NFAEdge parallelEdge) {
			super(originalGraph, EdaCases.PARALLEL);
			this.mergedScc = mergedScc;
			this.sourceVertex = sourceVertex;
			this.parallelEdge = parallelEdge;
		}

	}
	
	static final class EdaAnalysisResultsESCC extends EdaAnalysisResults {


		private final NFAGraph originalScc;
		public NFAGraph getOriginalScc() {
			return originalScc;
		}

		private final NFAEdge entranceEdge;
		public NFAEdge getEntranceEdge() {
			return entranceEdge;
		}
		
		private final NFAEdge exitEdge;
		public NFAEdge getExitEdge() {
			return exitEdge;
		}
		
		EdaAnalysisResultsESCC(NFAGraph originalGraph, NFAGraph originalScc, NFAEdge startEdge, NFAEdge endEdge) {
			super(originalGraph, EdaCases.ESCC);
			this.originalScc = originalScc;
			this.entranceEdge = startEdge;
			this.exitEdge = endEdge;
		}
	}
	
	static final class EdaAnalysisResultsFilter extends EdaAnalysisResults {		
		private final NFAGraph pcScc;
		public NFAGraph getPcScc() {
			return pcScc;
		}

		private final NFAVertexND startState;
		public NFAVertexND getStartState() {
			return startState;
		}
		private final NFAVertexND endState;
		public NFAVertexND getEndState() {
			return endState;
		}

		EdaAnalysisResultsFilter(NFAGraph originalGraph, NFAGraph pcScc, NFAVertexND startState, NFAVertexND endState) {
			super(originalGraph, EdaCases.FILTER);
			this.pcScc = pcScc;
			this.startState = startState;
			this.endState = endState;
		}
	}
	
	static final class IdaAnalysisResultsNoIda extends IdaAnalysisResults {
		
		IdaAnalysisResultsNoIda(NFAGraph originalGraph) {
			super(originalGraph, IdaCases.NO_IDA);
		}
	}
	
	static final class IdaAnalysisResultsIda extends IdaAnalysisResults {
		private final int degree;
		public int getDegree() {
			return degree;
		}
		
		private final LinkedList<NFAEdge> maxPath;
		public LinkedList<NFAEdge> getMaxPath() {
			return maxPath;
		}
		
		
		IdaAnalysisResultsIda(NFAGraph originalGraph, int degree, LinkedList<NFAEdge> maxPath) {
			super(originalGraph, IdaCases.IDA);
			this.degree = degree;
			this.maxPath = maxPath;
		}
	}
	
	public AnalysisResultsType containsEDA(NFAGraph m);
	public EdaAnalysisResults getEdaAnalysisResults(NFAGraph m);
	
	public ExploitString findEDAExploitString(NFAGraph m) throws InterruptedException;
	
	public AnalysisResultsType containsIDA(NFAGraph m);
	public IdaAnalysisResults getIdaAnalysisResults(NFAGraph m);
	
	public ExploitString findIDAExploitString(NFAGraph m) throws InterruptedException;


}
