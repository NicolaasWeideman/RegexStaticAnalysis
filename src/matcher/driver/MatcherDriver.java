package matcher.driver;

import matcher.*;
import regexcompiler.*;

import analysis.AnalysisSettings.NFAConstruction;

public class MatcherDriver {

	public static void main(String args[]) {
		if (args.length < 2) {
			System.out.println("usage: java MatcherDriver <regex> <input string>");
			System.exit(0);
		}
		String pattern = args[0];
		String inputString = args[1];
		MyPattern myPattern = MyPattern.compile(pattern, NFAConstruction.JAVA);
		MyMatcher myMatcher = myPattern.matcher(inputString);
		boolean matches = myMatcher.matches();
		System.out.println(pattern + " matches " + inputString + ": " + matches);
	}
}
