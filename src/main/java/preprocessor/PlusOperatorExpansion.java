package preprocessor;

import preprocessor.ParsingPreprocessor.QuantifiableOperator;
import preprocessor.ParsingPreprocessor.RegexOperator;
import preprocessor.ParsingPreprocessor.RegexToken;
import preprocessor.ParsingPreprocessor.RegexOperator.OperatorType;

public class PlusOperatorExpansion extends OperatorExpansionRule {

	@Override
	protected void expandOperator(StringBuilder resultBuilder, RegexToken factor, RegexToken operator) {
		resultBuilder.append(factor.getRepresentation() + factor.getRepresentation() + "*");
	}

	@Override
	protected RegexOperator getOperator() {
		return new QuantifiableOperator("+", OperatorType.PLUS);
	}

	@Override
	protected OperatorType getOperatorType() {
		return OperatorType.PLUS;
	}

}
