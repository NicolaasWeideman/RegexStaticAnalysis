package analysis;

public class NoAnalysisFoundException extends RuntimeException {
	private static final long serialVersionUID = 4271497904935133142L;

	public NoAnalysisFoundException() {
		super();
	}

	public NoAnalysisFoundException(String message) {
		super(message);
	}
}
