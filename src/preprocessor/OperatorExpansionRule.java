package preprocessor;

import java.util.List;

import preprocessor.ParsingPreprocessor.GroupFactor;
import preprocessor.ParsingPreprocessor.RegexFactor;
import preprocessor.ParsingPreprocessor.RegexOperator;
import preprocessor.ParsingPreprocessor.RegexOperator.OperatorType;
import preprocessor.ParsingPreprocessor.RegexToken;
import preprocessor.ParsingPreprocessor.RegexFactor.FactorType;
import preprocessor.ParsingPreprocessor.RegexToken.TokenType;

public abstract class OperatorExpansionRule implements PreprocessorRule {
	
	protected abstract void expandOperator(StringBuilder resultBuilder, RegexToken factor, RegexToken operator);
	
	protected abstract RegexOperator getOperator();
	
	protected abstract OperatorType getOperatorType();
	

	@Override
	public String process(List<RegexToken> tokenStream) {
		StringBuilder regexBuilder = new StringBuilder();
		RegexToken tokens[] = new RegexToken[tokenStream.size()];
		tokens = tokenStream.toArray(tokens);
		int numTokens = tokens.length;
		int i = 0;
		while (i < numTokens) {
			
			if (tokens[i].getTokenType() == TokenType.REGEX_FACTOR) {
				RegexFactor<?> factorToken = (RegexFactor<?>) tokens[i];
				if (factorToken.getFactorType() == FactorType.GROUP) {
					GroupFactor groupFactor = (GroupFactor) factorToken;
					
					String processedContent = process(groupFactor.factorContent);
					GroupFactor processedGroup = new GroupFactor(processedContent, groupFactor.getGroupType(), groupFactor.getLevel());
					factorToken = processedGroup;
					
				}
				if (i < numTokens - 1) {
					i++;
					if (tokens[i].getTokenType() == TokenType.REGEX_OPERATOR) {
						RegexOperator operatorToken = (RegexOperator) tokens[i];
						i++;
						if (operatorToken.getOperatorType() == getOperatorType()) {
							expandOperator(regexBuilder, factorToken, operatorToken);
							
						} else {
							regexBuilder.append(factorToken.getRepresentation());
							regexBuilder.append(operatorToken.getRepresentation());
						}
					} else {
						regexBuilder.append(factorToken.getRepresentation());
					}
				} else {
					regexBuilder.append(factorToken.getRepresentation());
					i++;
				}
			} else {
				regexBuilder.append(tokens[i].getRepresentation());
				i++;
			}

		}
		
		return regexBuilder.toString();
	}

}
