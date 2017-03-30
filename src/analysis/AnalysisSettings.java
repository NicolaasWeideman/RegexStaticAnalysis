package analysis;


public class AnalysisSettings {

	public static enum NFAConstruction {
		THOMPSON,
		JAVA;
	}

	public static enum PreprocessingType {
		NONE,
		PRECISE,
		NONPRECISE
	}
	
	public static enum EpsilonLoopRemovalStrategy {
		MERGING,
		FLATTENING
	}

	public enum PriorityRemovalStrategy {
		IGNORE,
		UNPRIORITISE
	}

	private final NFAConstruction nfaConstruction;
	public NFAConstruction getNFAConstruction() {
		return nfaConstruction;
	}

	private final PreprocessingType preprocessingType;
	public PreprocessingType getPreprocessingType() {
		return preprocessingType;
	}

	private final EpsilonLoopRemovalStrategy epsilonLoopRemovalStrategy;
	public EpsilonLoopRemovalStrategy getEpsilonLoopRemovalStrategy() {
		return epsilonLoopRemovalStrategy;
	}
	
	private final PriorityRemovalStrategy priorityRemovalStrategy;
	public PriorityRemovalStrategy getPriorityRemovalStrategy() {
		return priorityRemovalStrategy;
	}

	private final boolean shouldTestIDA;
	public boolean getShouldTestIDA() {
		return shouldTestIDA;
	}

	private final boolean shouldConstructEdaExploitString;
	public boolean getShouldConstructEdaExploitString() {
		return shouldConstructEdaExploitString;
	}

	private final boolean shouldTestEdaExploitString;
	public boolean getShouldTestExploitString() {
		return shouldTestEdaExploitString;
	}

	private final boolean shouldConstructIdaExploitString;
	public boolean getShouldConstructIdaExploitString() {
		return shouldConstructIdaExploitString;
	}

	private final int timeout;
	public int getTimeout() {
		return timeout;
	}

	public AnalysisSettings(NFAConstruction nfaConstruction, 
					PreprocessingType preprocessingType, 
					EpsilonLoopRemovalStrategy epsilonLoopRemovalStrategy, 
					PriorityRemovalStrategy priorityRemovalStrategy, 
					boolean shouldTestIDA, 
					boolean shouldConstructEdaExploitString,
					boolean shouldTestEdaExploitString, 
					boolean shouldConstructIdaExploitString,
					int timeout) {
		this.nfaConstruction = nfaConstruction;
		this.preprocessingType = preprocessingType;
		this.epsilonLoopRemovalStrategy = epsilonLoopRemovalStrategy;
		this.priorityRemovalStrategy = priorityRemovalStrategy;
		this.shouldTestIDA = shouldTestIDA;
		this.shouldConstructEdaExploitString = shouldConstructEdaExploitString;
		this.shouldTestEdaExploitString = shouldTestEdaExploitString;
		this.shouldConstructIdaExploitString = shouldConstructIdaExploitString;
		this.timeout = timeout;
	}
	
}
