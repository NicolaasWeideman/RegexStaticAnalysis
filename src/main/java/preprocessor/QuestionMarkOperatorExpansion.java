package preprocessor;

import preprocessor.ParsingPreprocessor.RegexOperator.OperatorType;
import preprocessor.ParsingPreprocessor.*;

public class QuestionMarkOperatorExpansion extends OperatorExpansionRule {

	@Override
	protected void expandOperator(StringBuilder resultBuilder, RegexToken factor, RegexToken operator) {
		resultBuilder.append("("  + "\\l|" + factor.getRepresentation() + ")");
	}

	@Override
	protected RegexOperator getOperator() {
		return new QuantifiableOperator("?", OperatorType.QM);
	}

	@Override
	protected OperatorType getOperatorType() {
		return OperatorType.QM;
	}

}
