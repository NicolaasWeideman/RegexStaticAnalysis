package preprocessor;



import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.Scanner;

public class PreciseSubstitutionPreprocessor extends ParsingPreprocessor {
		
	public PreciseSubstitutionPreprocessor() {		
		super();
		DequantifierRule dr = new DequantifierRule(); /* we first need to remove the quantifying symbols */
		addRule(dr);
		EscapeSequenceExpansionRule eer = new EscapeSequenceExpansionRule();
		addRule(eer);
		WildCardExpansionRule wcer = new WildCardExpansionRule();
		addRule(wcer);
		PlusOperatorExpansion poe = new PlusOperatorExpansion();
		addRule(poe);
		QuestionMarkOperatorExpansion qmoe = new QuestionMarkOperatorExpansion();
		addRule(qmoe);
		CountClosureOperatorExpansion ccoe = new CountClosureOperatorExpansion();
		addRule(ccoe);
	}
	
	public static void main(String [] args) {
		//testingDequantifier();
		//testingNonCapturingGroups();
		//testingQuestionMark();
		//testingPlus();
		testingCount();
		//testingVulnerable();
		//driver("../snort-raw.txt");
		//driver("../regexlib-manual-processed.txt");
		//driver("../regex_test.txt");
		
	}
	
	@SuppressWarnings("unused")
	private static void testingVulnerable() {
		/* /^([a-zA-Z0-9_\.\-])+\@(([a-zA-Z0-9\-])+\.)+([a-zA-Z0-9]{2,4})+$/ */
		String orinigalRegex = " <[a-zA-Z][^>]*\\son\\w+=(\\w+|'[^']*'|\"[^\"]*\")[^>]*> ";
		Preprocessor p = new PreciseSubstitutionPreprocessor();
		String newRegex =  p.applyRules(orinigalRegex);
		System.out.println(newRegex);
	}
	
	@SuppressWarnings("unused")
	private static void testingPlus() {
		String originalRegexs[] = {"(a)+?", "[a]+", "b+", "a\\+", "[ba+]", "(ab+)+", "abc+", "\\Qabc+\\E+", "[\\Qabc\\E]+", "\\++", "\\(+", "\\x{FFF}+", "\\uffff+", "\\077+", "\\s+", "(abc){0,1}+", "[\\[]+"};
		String correctRegexs[] = {"(a)(a)*", "[a][a]*", "bb*", "a\\+", "[ba+]", "(abb*)(abb*)*", "abcc*", "\\Qabc+\\E\\+*", "[\\Qabc\\E][\\Qabc\\E]*", "\\+\\+*", "\\(\\(*", "\\x{FFF}", "\\uffff\\uffff*", "\\077\\077*", "\\s\\s*", "(abc){0,1}", "[\\[][\\[]*"};
		Preprocessor p = new PreciseSubstitutionPreprocessor();
		for (int i = 0; i < originalRegexs.length; i++) {
			String originalRegex = originalRegexs[i];
			String correctRegex = correctRegexs[i];
			String newRegex =  p.applyRules(originalRegex);
			if (!newRegex.equals(correctRegex)) {
				System.out.println("Original: " + originalRegex + "\tReplace: " + newRegex + "\tCorrect: " + correctRegex);
			}
			
		}
	}
	
	private static void testingCount() {
		String originalRegexs[] = {"a{1,2}", "(a){0,3}", "(abc){2,5}", "a{3,}", "a{3}"};
		String correctRegexs[] = {"(a(\\l|a))", "((\\l|(a)|(a)(a)|(a)(a)(a)))", "((abc)(abc)(\\l|(abc)|(abc)(abc)|(abc)(abc)(abc)))", "(aaaa*)", "(aaa)"};
		Preprocessor p = new PreciseSubstitutionPreprocessor();
		for (int i = 0; i < originalRegexs.length; i++) {
			String originalRegex = originalRegexs[i];
			String correctRegex = correctRegexs[i];
			String newRegex =  p.applyRules(originalRegex);
			if (!newRegex.equals(correctRegex)) {
				System.out.println("Original: " + originalRegex + "\tReplace: " + newRegex + "\tCorrect: " + correctRegex);
			}
			
		}
	}
	
	@SuppressWarnings("unused")
	private static void testingNonCapturingGroups() {
		String originalRegexs[] = {"(?:a)", "(?:)", "(a)", "(?:(?:a))"};
		String correctRegexs[] = {"(a)", "()", "(a)", "((a))"};
		Preprocessor p = new PreciseSubstitutionPreprocessor();
		for (int i = 0; i < originalRegexs.length; i++) {
			String originalRegex = originalRegexs[i];
			String correctRegex = correctRegexs[i];
			String newRegex =  p.applyRules(originalRegex);
			if (!newRegex.equals(correctRegex)) {
				System.out.println("Original: " + originalRegex + "\tReplace: " + newRegex + "\tCorrect: " + correctRegex);
			}
			
		}
	}
	
	@SuppressWarnings("unused")
	private static void testingDequantifier() {
		String originalRegexs[] = {"[a]++", "([a++]*?)", "a+?b*+", "\\d*?"};
		String correctRegexs[] = {"[a]+", "([a++]*)", "a+b*", "\\d*"};
		Preprocessor p = new PreciseSubstitutionPreprocessor();
		for (int i = 0; i < originalRegexs.length; i++) {
			String originalRegex = originalRegexs[i];
			String correctRegex = correctRegexs[i];
			String newRegex =  p.applyRules(originalRegex);
			if (!newRegex.equals(correctRegex)) {
				System.out.println("Original: " + originalRegex + "\tReplace: " + newRegex + "\tCorrect: " + correctRegex);
			}
			
		}
	}
	
	@SuppressWarnings("unused")
	private static void testingQuestionMark() {
		String originalRegexs[] = {"a?+", "(abc)?", "\\Qabc\\E?", "a{0,1}?", "a|[[b\\[]]?"};
		String correctRegexs[] = {"(\\l|a)", "(\\l|(abc))", "\\Qab\\E(\\l|c)", "a{0,1}", "a|(\\l|[[b\\[]])"};
		Preprocessor p = new PreciseSubstitutionPreprocessor();
		for (int i = 0; i < originalRegexs.length; i++) {
			String originalRegex = originalRegexs[i];
			String correctRegex = correctRegexs[i];
			String newRegex =  p.applyRules(originalRegex);
			if (!newRegex.equals(correctRegex)) {
				System.out.println("Original: " + originalRegex + "\tReplace: " + newRegex + "\tCorrect: " + correctRegex);
			}
			
		}
	}
	
	@SuppressWarnings("unused")
	private static void driver(String inputFileName) {
		
		try {
			Scanner inputReader = new Scanner(new FileReader(inputFileName));
			PrintWriter outputWriter = new PrintWriter("output.txt");
			int counter = 1;
			int skipCounter = 0;
			Preprocessor p = new PreciseSubstitutionPreprocessor();
			while (inputReader.hasNextLine()) {
				String line = inputReader.nextLine();
				try {
					String processedLine = p.applyRules(line);
					System.out.println(counter + " " + line);
					System.out.println(counter + " " + processedLine);
					System.out.println();
					outputWriter.println(processedLine);
					System.out.flush();
					
				} catch (Exception e) {
					System.out.println("SKIPPING: " + counter + " " + line);
					System.out.println();
					skipCounter++;
					System.out.flush();
				}
				counter++;
				
			}
			System.out.println("Amount skipped: " + skipCounter);
			inputReader.close();
			outputWriter.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

}
