package preprocessor;

import java.util.List;

import preprocessor.ParsingPreprocessor.EscapeFactor;
import preprocessor.ParsingPreprocessor.GroupFactor;
import preprocessor.ParsingPreprocessor.GroupFactor.GroupType;
import preprocessor.ParsingPreprocessor.RegexFactor;
import preprocessor.ParsingPreprocessor.RegexFactor.FactorType;
import preprocessor.ParsingPreprocessor.RegexToken;
import preprocessor.ParsingPreprocessor.RegexToken.TokenType;

public class EscapeSequenceExpansionRule implements PreprocessorRule {

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
				if (factorToken.getFactorType() == FactorType.ESCAPED_CHARACTER) {
					EscapeFactor escapeFactorToken = (EscapeFactor) factorToken;
					regexBuilder.append("[" + escapeFactorToken.getRepresentation() + "]");
					
					
				} else if (factorToken.getFactorType() == FactorType.GROUP) {
					GroupFactor groupFactorToken = (GroupFactor) factorToken;
					GroupType type = groupFactorToken.getGroupType();
					StringBuilder groupBuilder = new StringBuilder();
					groupBuilder.append(process(groupFactorToken.factorContent));
					switch (type) {
					case NORMAL:
						regexBuilder.append("(" + groupBuilder.toString() + ")");
						break;
					case NONCAPTURING:
						regexBuilder.append("(" + groupBuilder.toString() + ")");
						break;
					case NEGLOOKAHEAD:
						regexBuilder.append("(?!" + groupBuilder.toString() + ")");
						break;
					case NEGLOOKBEHIND:
						regexBuilder.append("(?<!" + groupBuilder.toString() + ")");
						break;
					case POSLOOKAHEAD:
						regexBuilder.append("(?=" + groupBuilder.toString() + ")");
						break;
					case POSLOOKBEHIND:
						regexBuilder.append("(?<=" + groupBuilder.toString() + ")");
						break;
					default:
						throw new RuntimeException();
					}
					
				} else {
					regexBuilder.append(factorToken.getRepresentation());
				}
			} else {
				regexBuilder.append(tokens[i].getRepresentation());
				
			}
			i++;
		}
		
		return regexBuilder.toString();
	}

}
