package preprocessor;

import preprocessor.ParsingPreprocessor.CountClosureOperator;
import preprocessor.ParsingPreprocessor.RegexOperator;
import preprocessor.ParsingPreprocessor.RegexOperator.OperatorType;
import preprocessor.ParsingPreprocessor.RegexToken;

public class NonpreciseCountClosureOperatorExpansion extends OperatorExpansionRule {
	
	private final int CONSTANT_CUTOFF = 32;
	private final int BOUND_DIFF_CUTOFF = Integer.MAX_VALUE;

	@Override
	protected void expandOperator(StringBuilder resultBuilder, RegexToken factor, RegexToken operator) {
		CountClosureOperator cco = (CountClosureOperator) operator;
		int low = cco.getLow();
		int high = cco.getHigh();
		if (low > CONSTANT_CUTOFF) {
			/* approximate with plus */
			StringBuilder expansionBuilder = new StringBuilder("(");
			expansionBuilder.append(factor.getRepresentation());
			expansionBuilder.append(factor.getRepresentation() + "*");
			expansionBuilder.append(")");
			resultBuilder.append(expansionBuilder);
		} else if ((high - low) >= BOUND_DIFF_CUTOFF) {
			/* factor out and approximate with star */
			expandUnbounded(resultBuilder, factor, cco);
		} else {
			switch (cco.getBoundsType()) {
			case CONSTANT_REPETITION:
				expandConstantRepitition(resultBuilder, factor, cco);
				break;
			case BOUNDED:
				expandBounded(resultBuilder, factor, cco);
				break;
			case UNBOUNDED:
				expandUnbounded(resultBuilder, factor, cco);
				break;
			}
		}
		
	}

	private void expandBounded(StringBuilder resultBuilder, RegexToken factor, CountClosureOperator cco) {
		int low = cco.getLow();
		int high = cco.getHigh();
		StringBuilder expansionBuilder = new StringBuilder("(");
		StringBuilder options = new StringBuilder();
		for (int i = 0; i < low - 1; i++) {
			expansionBuilder.append(factor.getRepresentation());
		}
		expansionBuilder.append("(");
		for (int i = low; i <= high; i++) {
			if (low == 0 && i == 0) {
				expansionBuilder.append("\\l");
			} else {
				options.append(factor.getRepresentation());
				expansionBuilder.append(options);
			}

			if (i < high) {
				expansionBuilder.append("|");
			}

		}
		expansionBuilder.append(")");

		expansionBuilder.append(")");
		resultBuilder.append(expansionBuilder);
	}

	private void expandUnbounded(StringBuilder resultBuilder, RegexToken factor, CountClosureOperator cco) {
		int low = cco.getLow();
		StringBuilder expansionBuilder = new StringBuilder("(");
		for (int i = 0; i < low; i++) {
			expansionBuilder.append(factor.getRepresentation());
		}
		expansionBuilder.append(factor.getRepresentation() + "*");
		expansionBuilder.append(")");
		resultBuilder.append(expansionBuilder);
	}

	private void expandConstantRepitition(StringBuilder resultBuilder, RegexToken factor, CountClosureOperator cco) {
		int low = cco.getLow();
		StringBuilder expansionBuilder = new StringBuilder("(");
		for (int i = 0; i < low; i++) {
			expansionBuilder.append(factor.getRepresentation());
		}
		expansionBuilder.append(")");
		resultBuilder.append(expansionBuilder);
	}

	@Override
	protected RegexOperator getOperator() {
		throw new UnsupportedOperationException();
	}

	@Override
	protected OperatorType getOperatorType() {
		return OperatorType.COUNT;
	}

}
