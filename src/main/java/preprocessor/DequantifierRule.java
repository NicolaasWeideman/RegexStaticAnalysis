package preprocessor;

import java.util.List;

import preprocessor.ParsingPreprocessor.QuantifiableOperator;
import preprocessor.ParsingPreprocessor.RegexOperator;
import preprocessor.ParsingPreprocessor.RegexToken;
import preprocessor.ParsingPreprocessor.RegexToken.TokenType;

public class DequantifierRule implements PreprocessorRule {

	@Override
	public String process(List<RegexToken> tokenStream) {
		StringBuilder regexBuilder = new StringBuilder();
		RegexToken tokens[] = new RegexToken[tokenStream.size()];
		tokens = tokenStream.toArray(tokens);
		int numTokens = tokens.length;
		int i = 0;
		while (i < numTokens) {
			
			if (tokens[i].getTokenType() == TokenType.REGEX_OPERATOR) {
				RegexOperator operatorToken = (RegexOperator) tokens[i];
				if (operatorToken.getIsQuantifiable()) {
					QuantifiableOperator quantifiableOperator = (QuantifiableOperator) operatorToken;
					QuantifiableOperator replacementOperator = new QuantifiableOperator(quantifiableOperator.getOperator(), quantifiableOperator.getOperatorType());
					regexBuilder.append(replacementOperator.getRepresentation());
				} else {
					regexBuilder.append(operatorToken.getRepresentation());
				}
			} else {
				regexBuilder.append(tokens[i].getRepresentation());
				
			}
			i++;
		}
		
		return regexBuilder.toString();
	}

	

}
