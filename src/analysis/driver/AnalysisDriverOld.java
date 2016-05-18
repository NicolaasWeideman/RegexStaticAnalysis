package analysis;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader; 
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.jgrapht.graph.UnmodifiableDirectedGraph;

import analysis.*;
import analysis.NFAAnalyserInterface.AnalysisResultsType;
import analysis.AnalysisSettings.PreprocessingType;
import analysis.AnalysisSettings.PriorityRemovalStrategy;
import analysis.AnalysisSettings.EpsilonLoopRemovalStrategy;
import analysis.AnalysisSettings.NFAConstruction;

import regexcompiler.MyPattern;


import preprocessor.Preprocessor;
import preprocessor.PreciseSubstitutionPreprocessor;
import preprocessor.NonpreciseSubstitutionPreprocessor;

import util.Constants;
import util.InterfaceSettings;
import util.InterfaceSettings.InputType;
import util.InterruptibleMatchingString;


import nfa.NFAGraph;

public class AnalysisDriverOld {

	private final static boolean TEST_IDA = true;
	
	private static enum ToTest {
		USER_INPUT, 
		TEST_EDA
	}

	/* Interface Settings */
	private static InputType inputType;
	private static boolean isVerbose;

	/* Analysis Settings */
	private static NFAConstruction nfaConstruction;
	private static PreprocessingType preprocessingType;
	private static EpsilonLoopRemovalStrategy epsilonLoopRemovalStrategy;
	private static PriorityRemovalStrategy priorityRemovalStrategy;
	private static boolean shouldTestExploitString;
	private static int timeout;
	private static boolean timeoutEnabled;


	private final static ToTest testing = ToTest.USER_INPUT;
	
	public static void performAnalysis(AnalysisSettings analysisSettings) {

		nfaConstruction = analysisSettings.getNFAConstruction();
		preprocessingType = analysisSettings.getPreprocessingType();
		epsilonLoopRemovalStrategy = analysisSettings.getEpsilonLoopRemovalStrategy();
		priorityRemovalStrategy = analysisSettings.getPriorityRemovalStrategy();
		shouldTestExploitString = analysisSettings.getShouldTestExploitString();
		timeout = analysisSettings.getTimeout();

		
		NFAAnalyserInterface analyser;
		switch (epsilonLoopRemovalStrategy) {
		case MERGING:
			analyser = new NFAAnalyserMerging(priorityRemovalStrategy);
			break;
		case FLATTENING:
			analyser = new NFAAnalyserFlattening(priorityRemovalStrategy);
			break;
		default:
			throw new RuntimeException("Unkown Strategy");
		}
		switch (testing) {
		case USER_INPUT:
			userInput(analyser);
			break;
		case TEST_EDA:
			testEDA(analyser);
			break;
		}
		System.exit(0);
	}

	private static void userInput(NFAAnalyserInterface analyser) {
		BufferedReader inputReader = new BufferedReader(new InputStreamReader(System.in));
		try {
			while (true) {
				System.out.println("Enter a regular expression to analyze or '#' to exit:");

				String pattern = inputReader.readLine();
				if (pattern == null || pattern.equals("#")) {
					System.out.println("Goodbye");
					System.exit(0);
				}

				/* To allow for the convention of writing regular expressions as / ... /, we simply take that in ... */
				Pattern slashes = Pattern.compile("^/(.*)/[a-zA-Z]*$");
				Matcher slashMatcher = slashes.matcher(pattern);
				if (slashMatcher.find()) {
					pattern = slashMatcher.group(1);
				}
				System.out.println("pattern = \"" + pattern + "\"");
				try {
					Preprocessor preprocessor;
					String finalPattern;
					switch (preprocessingType) {
					case NONE:
						finalPattern = pattern;
						break;
					case PRECISE:
						preprocessor = new PreciseSubstitutionPreprocessor();
						finalPattern =  preprocessor.applyRules(pattern);
						break;
					case NONPRECISE:
						preprocessor = new NonpreciseSubstitutionPreprocessor();
						finalPattern =  preprocessor.applyRules(pattern);
						break;
					default:
						throw new RuntimeException("Unknown preprocessing type: " + preprocessingType);
					}
					if (!pattern.equals(finalPattern)) {
							System.out.println("preprocessed pattern = \"" + finalPattern + "\"");
					}
					
					NFAGraph analysisGraph = MyPattern.toNFAGraph(finalPattern, nfaConstruction);					
					long totalAnalysisDuration = 0;
					long edaStartTime = System.currentTimeMillis();
					AnalysisResultsType containsEda = analyser.containsEDA(analysisGraph);
					long edaEndTime = System.currentTimeMillis();
					long edaDuration = edaEndTime - edaStartTime;
					totalAnalysisDuration += edaDuration;
					System.out.println("EDA analysis performed in: " + edaDuration + "ms");
					switch (containsEda) {
					case EDA:
						ExploitString edaResult = analyser.findEDAExploitString(analysisGraph);
						System.out.println("Contains EDA with: " + edaResult);
						System.out.println("\tPrefix:\t\"" + edaResult.getPrefixVisual() + "\"");
						System.out.println("\tPump:\t\"" + edaResult.getPumpByDegreeVisual(0) + "\"");
						System.out.println("\tSuffix:\t\"" + edaResult.getSuffixVisual() + "\"");

						if (shouldTestExploitString) {
							testWithMatcher(edaResult, pattern);
						}
						break;
					case NO_EDA:
						System.out.println("Does not contain EDA");
						if (TEST_IDA) {
							long idaStartTime = System.currentTimeMillis();
							AnalysisResultsType containsIda = analyser.containsIDA(analysisGraph);
							long idaEndTime = System.currentTimeMillis();
							long idaDuration = idaEndTime - idaStartTime;
							totalAnalysisDuration += idaDuration;
							System.out.println("IDA analysis performed in: " + idaDuration + "ms");
							switch (containsIda) {
							case IDA:
								ExploitString idaResult = analyser.findIDAExploitString(analysisGraph);
								int degree = idaResult.getDegree();
								System.out.println("Contains IDA, degree " + degree + ", with: " + idaResult);
								for (int i = 0; i < degree; i++) {
									if (i == 0) {
										System.out.println("\tPrefix:\t\t\"" + idaResult.getSeparatorByDegreeVisual(i) + "\"");
									} else {
										System.out.println("\tSeparator " + i + ":\t\"" + idaResult.getSeparatorByDegreeVisual(i) + "\"");
									}								
									System.out.println("\tPump " + i + ":\t\t\"" + idaResult.getPumpByDegreeVisual(i) + "\"");
								}
								
								System.out.println("\tSuffix:\t\t\"" + idaResult.getSuffixVisual() + "\"");
								break;
							case NO_IDA:
								System.out.println("Does not contain IDA");
								break;
							case TIMEOUT_IN_IDA:
								System.out.println("TIMEOUT in IDA");
								break;
							default:
								throw new RuntimeException("Unexpected AnalysisResultsType: " + containsIda);
							}
						}						
						System.out.println("Total analysis time: " + totalAnalysisDuration);
						break;
					case TIMEOUT_IN_EDA:
						System.out.println("TIMEOUT");
						break;
					default:
						throw new RuntimeException("Unexpected AnalysisResultsType: " + containsEda);
					}
					System.out.println();
				} catch (IllegalStateException ise) {
					System.err.println("Not a valid regular expression.");
				} catch (PatternSyntaxException pe) {
					System.err.println("Invalid regular expression: " + pe.getDescription());
				} catch (Exception e) {
					System.err.println("Invalid regular expression");
					e.printStackTrace();
				}
			}
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}

	private static void testEDA(NFAAnalyserInterface analyser) {
		
		BufferedReader inputReader = new BufferedReader(new InputStreamReader(System.in));
		String pattern;
		int numAnalysed = 0;
		int numVulnerable = 0;
		int numEda = 0;
		int numIda = 0;
		int numSafe = 0;
		LinkedList<Integer> edaVulnerableNumbers = new LinkedList<Integer>();
		LinkedList<Integer> idaVulnerableNumbers = new LinkedList<Integer>();
		int numSkipped = 0;
		int numTimeout = 0;
		int numTimeoutInEda = 0;
		int numTimeoutInIda = 0;
		try {
			int counter = 0;
			long startTime = System.currentTimeMillis();
			while ((pattern = inputReader.readLine()) != null) {
				System.out.println((counter + 1) + ": " + pattern);
				try {
					/* To allow for the convention of writing regular expressions as / ... /, we simply take that in ... */
					Pattern slashes = Pattern.compile("^/(.*)/[a-zA-Z]*$");
					Matcher slashMatcher = slashes.matcher(pattern);
					if (slashMatcher.find()) {
						pattern = slashMatcher.group(1);
					}
					
					Preprocessor preprocessor;
					String finalPattern;
					switch (preprocessingType) {
						case NONE:
							finalPattern = pattern;
							break;
						case PRECISE:
							preprocessor = new PreciseSubstitutionPreprocessor();
							finalPattern =  preprocessor.applyRules(pattern);
							break;
						case NONPRECISE:
							preprocessor = new NonpreciseSubstitutionPreprocessor();
							finalPattern =  preprocessor.applyRules(pattern);
							break;
						default:
							throw new RuntimeException("Unknown preprocessing type: " + preprocessingType);
					}

					AnalysisRunner ar = new AnalysisRunner(finalPattern, analyser);					
					
					final Thread AnalysisRunnerThread = new Thread(ar);
					Thread sleepThread = new Thread() {
						public void run() {
							try {
								Thread.sleep(timeout * Constants.MILLISECONDS_IN_SECOND);
								AnalysisRunnerThread.interrupt();
							} catch (InterruptedException e) {
								Thread.currentThread().interrupt();
							}
						}
					};
					
					AnalysisRunnerThread.start();
					sleepThread.start();
					AnalysisRunnerThread.join();
					sleepThread.interrupt();
					
					AnalysisResultsType results = ar.getAnalysisResultsType();
					switch (results) {
					case EDA:
						System.out.println("EDA");
						numVulnerable++;
						numEda++;
						edaVulnerableNumbers.add(counter + 1);
						numAnalysed++;
						break;
					case NO_EDA:
						System.out.println("NO EDA");
						numSafe++;
						numAnalysed++;
						break;
					case IDA:
						System.out.println("IDA");
						numVulnerable++;
						numIda++;
						idaVulnerableNumbers.add(counter + 1);
						numAnalysed++;
						break;
					case NO_IDA:
						System.out.println("NO IDA");
						numSafe++;
						numAnalysed++;
						break;
					case TIMEOUT_IN_EDA:
						System.out.println("TIMEOUT in EDA");
						numTimeout++;
						numTimeoutInEda++;
						break;
					case TIMEOUT_IN_IDA:
						System.out.println("TIMEOUT in IDA");
						numTimeout++;
						numTimeoutInIda++;
						break;
					case ANALYSIS_FAILED:
						System.out.println("SKIPPED");
						numSkipped++;
						break;
					}
				
				} catch (java.util.regex.PatternSyntaxException pse){
					
					System.out.println((counter + 1) + ": SKIPPED: " + pse.getDescription());
					numSkipped++;
				} catch (Exception e) {
						System.out.println((counter + 1) + ": SKIPPED: " + e.getMessage());
						numSkipped++;
				} catch (OutOfMemoryError oome) {
					System.out.println((counter + 1) + ": SKIPPED: " + oome.getMessage());
					numSkipped++;
				}
				counter++;
			}
			long endTime = System.currentTimeMillis();
			System.out.println("ε-loop removing strategy: " + epsilonLoopRemovalStrategy);
			System.out.println("Analysed:\t" + numAnalysed + "/" + counter);
			System.out.println("\tSafe:\t" + numSafe + "/" + counter);
			System.out.println("\tVulnerable:\t" + numVulnerable + "/" + counter);
			System.out.println("\t\tEDA:\t" + numEda + "/" + counter);
			if (TEST_IDA) {
				System.out.println("\t\tIDA:\t" + numIda + "/" + counter);
			}
			System.out.println("\tVulnerable EDA:\t" + edaVulnerableNumbers);
			if (TEST_IDA) {
				System.out.println("\tVulnerable IDA" + idaVulnerableNumbers);
			}
			System.out.println("Skipped:\t" + numSkipped + "/" + counter);
			System.out.println("Timeout:\t" + numTimeout + "/" + counter);
			System.out.println("\t\tEDA:\t" + numTimeoutInEda + "/" + counter);
			if (TEST_IDA) {
				System.out.println("\t\tIDA:\t" + numTimeoutInIda + "/" + counter);
			}
			
			System.out.println("Total running time: " + (endTime - startTime));
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
		
		
	}

	private static boolean testWithMatcher(ExploitString es, String regex) {
		int max_tries = 500;
		Pattern pattern = Pattern.compile(regex, Pattern.DOTALL);
		int pumpLength = es.getPumpByDegree(0).length();
		int i  = 0;
		int shortPumpIterations = 1;
		while (i < max_tries) {
			i++;			

			String exploitStringShort = es.getPrefix();
			for (int j = 0; j < shortPumpIterations; j++) {
				exploitStringShort += es.getPumpByDegree(0);
			}
			exploitStringShort += es.getSuffix();

			
			Matcher matcher = pattern.matcher(exploitStringShort);
			long shortStartTime = System.currentTimeMillis();
			matcher.matches();
			long shortEndtTime = System.currentTimeMillis();
			final long shortTime = (shortEndtTime - shortStartTime);
			
			int longPumpIterations = shortPumpIterations + 1;
			/* keep pumping until we have a significant matching time */
			if (shortTime < 250) {
				shortPumpIterations++;
				continue;
			}
			

			StringBuilder exploitBuilder = new StringBuilder(es.getPrefix());
			
			for (int j = 0; j < longPumpIterations; j++) {
				exploitBuilder.append(es.getPumpByDegree(0));
			}
			exploitBuilder.append(es.getSuffix());
			String exploitStringLong = exploitBuilder.toString();
			InterruptibleMatchingString matchingString = new InterruptibleMatchingString(exploitStringLong);

			final Thread parentThread = Thread.currentThread();
			/* timing thread to monitor the runtime of the matcher */
			Thread thread = new Thread() {
				public void run() {
					try {
						/* wait and see if it finishes */
						sleep(shortTime * 10);
						parentThread.interrupt();
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					}
				}
			};

			long longStartTime = 0, longEndTime = 0;
			try {
				
				thread.start();
				matcher = pattern.matcher(matchingString);
				longStartTime = System.currentTimeMillis();
				matcher.matches();
				thread.interrupt();
				longEndTime = System.currentTimeMillis();
			} catch (Exception e) {
				System.out.println("\t\t\tVulnerable:");
				System.out.println("\t\t\t" + ExploitString.visualiseString(exploitStringShort) + addSpaces(pumpLength) + " Time: " + shortTime);
				System.out.println("\t\t\t" + ExploitString.visualiseString(exploitStringLong) + " Time: (timeout) >(" + shortTime + " * 10)");
				return true;
			}
			long longTime = (longEndTime - longStartTime);

			if (longTime > 2 * shortTime) {
				
				System.out.println("\t\t\tVulnerable:");
				System.out.println("\t\t\t" + ExploitString.visualiseString(exploitStringShort) + addSpaces(pumpLength) + " Time: " + shortTime);
				System.out.println("\t\t\t" + ExploitString.visualiseString(exploitStringLong) + " Time: " + longTime);
				return true;
			}
			
		}

		System.out.println("Java Matcher didn't display exponential growth in matching time...");

		return false;
	}

	private static String addSpaces(int numSpaces) {
		StringBuilder s = new StringBuilder();
		for (int i = 0; i < numSpaces; i++) {
			s.append(" ");
		}
		return s.toString();
	}
	
	private static class AnalysisRunner implements Runnable {
				
		private final String pattern;
		private final NFAAnalyserInterface analyser;
		
		private AnalysisRunner(String pattern, NFAAnalyserInterface analyser) {
			this.pattern = pattern;
			this.analyser = analyser;
		}
		
		private AnalysisResultsType analysisResultsType;
		public AnalysisResultsType getAnalysisResultsType() {
			return analysisResultsType;
		}

		@Override
		public void run() {

			boolean finishedEdaAnalysis = false;
			try {
				
				NFAGraph analysisGraph = MyPattern.toNFAGraph(pattern, nfaConstruction);
				analysisResultsType = analyser.containsEDA(analysisGraph);
				finishedEdaAnalysis = true;
				switch (analysisResultsType) {
				case NO_EDA:
					if (TEST_IDA) {
						analysisResultsType = analyser.containsIDA(analysisGraph);
					}
					break;
				default:
					break;
				}
				
			} catch (InterruptedException ie) {
				Thread.currentThread().interrupt();
				if (finishedEdaAnalysis) {
					analysisResultsType = AnalysisResultsType.TIMEOUT_IN_IDA;
				} else {
					analysisResultsType = AnalysisResultsType.TIMEOUT_IN_EDA;
				}
				
			} catch (Exception e) {
				Thread.currentThread().interrupt();
				analysisResultsType = AnalysisResultsType.ANALYSIS_FAILED;
			} catch (OutOfMemoryError oome) {
				Thread.currentThread().interrupt();
				analysisResultsType = AnalysisResultsType.ANALYSIS_FAILED;
			}

			
		}
	}

}
