package preprocessor;

import java.util.List;

import preprocessor.ParsingPreprocessor.GroupFactor;
import preprocessor.ParsingPreprocessor.RegexFactor;
import preprocessor.ParsingPreprocessor.GroupFactor.GroupType;
import preprocessor.ParsingPreprocessor.RegexFactor.FactorType;
import preprocessor.ParsingPreprocessor.RegexToken.TokenType;
import preprocessor.ParsingPreprocessor.RegexToken;

public class NonpreciseLookaroundExpansion implements PreprocessorRule {

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
					GroupType groupType = groupFactor.getGroupType();

					if (!(groupType == GroupType.POSLOOKAHEAD || groupType == GroupType.NEGLOOKAHEAD || groupType == GroupType.POSLOOKBEHIND || groupType == GroupType.NEGLOOKBEHIND)) {
						regexBuilder.append(tokens[i].getRepresentation());
					}

				} else {
					regexBuilder.append(tokens[i].getRepresentation());
				}
			} else {
				regexBuilder.append(tokens[i].getRepresentation());
			}
			i++;
		}

		return regexBuilder.toString();
	}

}
