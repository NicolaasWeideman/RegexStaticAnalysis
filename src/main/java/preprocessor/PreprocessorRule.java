package preprocessor;

import java.util.List;

public interface PreprocessorRule {
	
	public String process(List<ParsingPreprocessor.RegexToken> tokenStream);
	
	
}
