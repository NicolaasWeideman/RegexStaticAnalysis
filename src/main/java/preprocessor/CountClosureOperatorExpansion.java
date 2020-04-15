package preprocessor;

import preprocessor.ParsingPreprocessor.CountClosureOperator;
import preprocessor.ParsingPreprocessor.RegexOperator;
import preprocessor.ParsingPreprocessor.RegexOperator.OperatorType;
import preprocessor.ParsingPreprocessor.RegexToken;

public class CountClosureOperatorExpansion extends OperatorExpansionRule {

	@Override
	protected void expandOperator(StringBuilder resultBuilder, RegexToken factor, RegexToken operator) {
		CountClosureOperator cco = (CountClosureOperator) operator;
		switch (cco.getBoundsType()) {
		case BOUNDED:
			expandBounded(resultBuilder, factor, cco);
			break;
		case UNBOUNDED:
			expandUnbounded(resultBuilder, factor, cco);
			break;
		case CONSTANT_REPETITION:
			expandConstantRepitition(resultBuilder, factor, cco);
			break;

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
