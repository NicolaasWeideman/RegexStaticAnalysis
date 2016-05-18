package analysis.driver;

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

public class AnalysisDriverStdOut {

	private static final boolean DEBUG = false;
	
	/* Interface Settings */
	private static InputType inputType;
	private static boolean isVerbose;

	/* Analysis Settings */
	private static NFAConstruction nfaConstruction;
	private static PreprocessingType preprocessingType;
	private static EpsilonLoopRemovalStrategy epsilonLoopRemovalStrategy;
	private static PriorityRemovalStrategy priorityRemovalStrategy;
	private static boolean shouldTestIDA;
	private static boolean shouldTestExploitString;
	private static int timeout;
	private static boolean timeoutEnabled;

	public static void performAnalysis(BufferedReader regexesReader, InterfaceSettings interfaceSettings, AnalysisSettings analysisSettings) {
		inputType = interfaceSettings.getInputType();
		isVerbose = interfaceSettings.getIsVerbose();	

		nfaConstruction = analysisSettings.getNFAConstruction();
		preprocessingType = analysisSettings.getPreprocessingType();
		epsilonLoopRemovalStrategy = analysisSettings.getEpsilonLoopRemovalStrategy();
		priorityRemovalStrategy = analysisSettings.getPriorityRemovalStrategy();
		shouldTestIDA = analysisSettings.getShouldTestIDA();
		shouldTestExploitString = analysisSettings.getShouldTestExploitString();
		timeout = analysisSettings.getTimeout();
		if (timeout > 0) {
			timeoutEnabled = true;
		}

		int counter = 0;
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


		/* Printing settings */
		if (isVerbose) {
			System.out.println("---Interface settings:---");
			System.out.println("Input type:\t\t\t" + inputType);
			System.out.println("Is Verbose:\t\t\t" + isVerbose);
			System.out.println("---Analysis settings:---");
			System.out.println("NFA Construction:\t\t" + nfaConstruction);
			System.out.println("Preprocessing type:\t\t" + preprocessingType);
			System.out.println("Epsilon loop removal:\t\t" + epsilonLoopRemovalStrategy);
			System.out.println("Priority removal:\t\t" + priorityRemovalStrategy);
			System.out.println("Testing for IDA:\t\t" + shouldTestIDA);
			System.out.println("Testing exploit strings:\t" + shouldTestExploitString);
			if (timeout > 0) {
				System.out.println("Timeout:\t\t\t" + timeout + "s");
			} else {
				System.out.println("Timeout:\t\t\tDISABLED");
			}
			System.out.println("------------------------");
		}

		NFAAnalyserInterface analyser = getCorrectNFAAnalyser(epsilonLoopRemovalStrategy);
		
		Pattern slashesRegex = Pattern.compile("^/(.*)/[a-zA-Z]*$");
		if (isVerbose && inputType == InputType.USER_INPUT) {
			System.out.println("Enter a regular expression to analyze:");
		}
		String pattern;
		try {
			long startTime = System.currentTimeMillis();
			while ((pattern = regexesReader.readLine()) != null) {
				/* To allow for the convention of writing regular expressions as / ... /, we simply take that in ... */
				Matcher slashMatcher = slashesRegex.matcher(pattern);
				if (slashMatcher.find()) {
					pattern = slashMatcher.group(1);
				}
				if (isVerbose) {
					System.out.println((counter + 1) + ". pattern = \"" + pattern + "\"");
				} else {
					System.out.println((counter + 1) + ": " + pattern);
				}

				try {
					
					String finalPattern = preprocessToFinalPattern(pattern);
					if (isVerbose && !pattern.equals(finalPattern)) {
						System.out.println("preprocessed pattern = \"" + finalPattern + "\"");
					}
					
					AnalysisRunner ar = new AnalysisRunner(finalPattern, analyser);					
					
					final Thread AnalysisRunnerThread = new Thread(ar);
					Thread sleepThread = new Thread() {
						public void run() {
							try {
								if (timeoutEnabled) {
									Thread.sleep(timeout * Constants.MILLISECONDS_IN_SECOND);
									AnalysisRunnerThread.interrupt();
								}
							} catch (InterruptedException e) {
								Thread.currentThread().interrupt();
							}
						}
					};
					
					AnalysisRunnerThread.start();
					sleepThread.start();
					AnalysisRunnerThread.join();
					sleepThread.interrupt();
					
					NFAGraph analysisGraph;
					AnalysisResultsType results = ar.getAnalysisResultsType();
					switch (results) {
					case EDA:
						analysisGraph = ar.getAnalysisGraph();
						ExploitString edaResult = analyser.findEDAExploitString(analysisGraph);
						if (isVerbose) {
							/* We only construct the exploit string if the user asks for it */
							System.out.println("NFA constructed in: " + ar.getNfaConstructionTime() + "ms");
							System.out.println("EDA analysis performed in: " + ar.getEdaAnalysisTime() + "ms");
							System.out.println("Contains EDA with: " + edaResult);
							System.out.println("\tPrefix:\t\"" + edaResult.getPrefixVisual() + "\"");
							System.out.println("\tPump:\t\"" + edaResult.getPumpByDegreeVisual(0) + "\"");
							System.out.println("\tSuffix:\t\"" + edaResult.getSuffixVisual() + "\"");
							System.out.println("Total analysis time: " + ar.getTotalAnalysisTime());
						} else {
							System.out.print("EDA ");
						}
						if (shouldTestExploitString) {
							testWithMatcher(edaResult, pattern);
						} else {
							System.out.println();
						}
						numVulnerable++;
						numEda++;
						edaVulnerableNumbers.add(counter + 1);
						numAnalysed++;
						break;
					case NO_EDA:
						if (isVerbose) {
							System.out.println("NFA constructed in: " + ar.getNfaConstructionTime() + "ms");
							System.out.println("EDA analysis performed in: " + ar.getEdaAnalysisTime() + "ms");
							System.out.println("Does not contain EDA");
							System.out.println("Total analysis time: " + ar.getTotalAnalysisTime());
						} else {
							System.out.println("NO EDA");
						}
						numSafe++;
						numAnalysed++;
						break;
					case IDA:
						/* We need to construct the exploit string to calculate the degree */
						analysisGraph = ar.getAnalysisGraph();
						ExploitString idaResult = analyser.findIDAExploitString(analysisGraph);
						int degree = idaResult.getDegree();
						if (isVerbose) {
							System.out.println("NFA constructed in: " + ar.getNfaConstructionTime() + "ms");
							System.out.println("EDA analysis performed in: " + ar.getEdaAnalysisTime() + "ms");
							System.out.println("Does not contain EDA");
							System.out.println("IDA analysis performed in: " + ar.getIdaAnalysisTime() + "ms");
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
							System.out.println("Total analysis time: " + ar.getTotalAnalysisTime());
						} else {
							System.out.println("IDA_" + degree);
							//System.out.println("IDA");
						}
						numVulnerable++;
						numIda++;
						idaVulnerableNumbers.add(counter + 1);
						numAnalysed++;
						break;
					case NO_IDA:
						if (isVerbose) {
							System.out.println("NFA constructed in: " + ar.getNfaConstructionTime() + "ms");
							System.out.println("EDA analysis performed in: " + ar.getEdaAnalysisTime() + "ms");
							System.out.println("Does not contain EDA");
							System.out.println("IDA analysis performed in: " + ar.getIdaAnalysisTime() + "ms");
							System.out.println("Does not contain IDA");
							System.out.println("Total analysis time: " + ar.getTotalAnalysisTime());
						} else {
							System.out.println("NO IDA");
						}
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
				} catch (PatternSyntaxException pse){
					if (DEBUG) {
						pse.printStackTrace();
					}			
					System.out.println((counter + 1) + ": SKIPPED: " + pse.getDescription());
					numSkipped++;
				} catch (Exception e) {
						if (DEBUG) {
							e.printStackTrace();
						}
						System.out.println((counter + 1) + ": SKIPPED: " + e.getMessage());
						numSkipped++;
				} catch (OutOfMemoryError oome) {
					if (DEBUG) {
						oome.printStackTrace();
					}
					System.out.println((counter + 1) + ": SKIPPED: " + oome.getMessage());
					numSkipped++;
				}
				counter++;

				if (isVerbose && inputType == InputType.USER_INPUT) {
					System.out.println("Enter a regular expression to analyze:");
				}
			}

			long endTime = System.currentTimeMillis();
			if (!isVerbose) {
				/* All the settings have already been printed */
				System.out.println("Construction: " + nfaConstruction);
				System.out.println("ε-loop removing strategy: " + epsilonLoopRemovalStrategy);
			}
			System.out.println("Analysed:\t" + numAnalysed + "/" + counter);
			System.out.println("\tSafe:\t\t" + numSafe + "/" + counter);
			System.out.println("\tVulnerable:\t" + numVulnerable + "/" + counter);
			System.out.println("\t\tEDA:\t\t" + numEda + "/" + counter);
			if (shouldTestIDA) {
				System.out.println("\t\tIDA:\t\t" + numIda + "/" + counter);
			}
			System.out.println("\tVulnerable EDA:\t" + edaVulnerableNumbers);
			if (shouldTestIDA) {
				System.out.println("\tVulnerable IDA:\t" + idaVulnerableNumbers);
			}
			System.out.println("Skipped:\t" + numSkipped + "/" + counter);
			System.out.println("Timeout:\t" + numTimeout + "/" + counter);
			System.out.println("\t\tEDA:\t" + numTimeoutInEda + "/" + counter);
			if (shouldTestIDA) {
				System.out.println("\t\tIDA:\t" + numTimeoutInIda + "/" + counter);
			}
			
			System.out.println("Total running time: " + (endTime - startTime));
		} catch (IOException ioe) {
			System.err.println("Error while reading pattern.");
			System.exit(0);
		}

		
	}

	private static NFAAnalyser getCorrectNFAAnalyser(EpsilonLoopRemovalStrategy epsilonLoopRemovalStrategy) {
		NFAAnalyser analyser;
		switch (epsilonLoopRemovalStrategy) {
		case MERGING:
			analyser = new NFAAnalyserMerging(priorityRemovalStrategy);
			break;
		case FLATTENING:
			analyser = new NFAAnalyserFlattening(priorityRemovalStrategy);
			break;
		default:
			throw new RuntimeException("Unkown Strategy: " + epsilonLoopRemovalStrategy);
		}
		return analyser;
	}

	private static String preprocessToFinalPattern(String pattern) {
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
		return finalPattern;
	}

	private static void testWithMatcher(ExploitString es, String regex) {
		int max_tries = 500;
		Pattern pattern = Pattern.compile(regex, Pattern.DOTALL);
		int pumpLength = es.getPumpByDegree(0).length();
		int i = 0;
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
				if (DEBUG) {
					e.printStackTrace();
				}
				if (isVerbose) {
					System.out.println("\t\t\tVulnerable:");
					System.out.println("\t\t\t" + String.format("%1$-" + pumpLength + "s", ExploitString.visualiseString(exploitStringShort)) + " Time: " + shortTime);
					System.out.println("\t\t\t" + ExploitString.visualiseString(exploitStringLong) + " Time: (timeout) >(" + shortTime + " * 10)");
				} else {
					System.out.println("MATCHER_CONFIRMED_EXP_TIME");
				}
				return;
			}
			long longTime = (longEndTime - longStartTime);

			if (longTime > 2 * shortTime) {
				if (isVerbose) {
					System.out.println("\t\t\tVulnerable:");
					System.out.println("\t\t\t" + String.format("%1$-" + pumpLength + "s", ExploitString.visualiseString(exploitStringShort)) + " Time: " + shortTime);
					System.out.println("\t\t\t" + ExploitString.visualiseString(exploitStringLong) + " Time: " + longTime);
				} else {
					System.out.println("MATCHER_CONFIRMED_EXP_TIME");
				}
				return;
			}
			
		}

		if (isVerbose) {
			System.out.println("Java matcher did not display exponential matching time...");
		} else {
			System.out.println("MATCHER_DID_NOT_DISPLAY_EXP_TIME");
		}
	}

	private static class AnalysisRunner implements Runnable {
				
		private final String pattern;
		private final NFAAnalyserInterface analyser;
		
		private AnalysisRunner(String pattern, NFAAnalyserInterface analyser) {
			this.pattern = pattern;
			this.analyser = analyser;
		}

		private NFAGraph analysisGraph;
		public NFAGraph getAnalysisGraph() {
			return analysisGraph;
		}

		private long totalAnalysisTime;
		public long getTotalAnalysisTime() {
			return totalAnalysisTime;
		}

		private long nfaConstructionTime;
		public long getNfaConstructionTime() {
			return nfaConstructionTime;
		}

		private long edaAnalysisTime;
		public long getEdaAnalysisTime() {
			return edaAnalysisTime;
		}

		private long idaAnalysisTime;
		public long getIdaAnalysisTime() {
			return idaAnalysisTime;
		}
		
		private AnalysisResultsType analysisResultsType;
		public AnalysisResultsType getAnalysisResultsType() {
			return analysisResultsType;
		}

		@Override
		public void run() {

			boolean finishedEdaAnalysis = false;
			try {
				long totalAnalysisStartTime = System.currentTimeMillis();
				analysisGraph = MyPattern.toNFAGraph(pattern, nfaConstruction);	

				nfaConstructionTime = System.currentTimeMillis() - totalAnalysisStartTime;
				long edaAnalysisStartTime = System.currentTimeMillis();
				analysisResultsType = analyser.containsEDA(analysisGraph);

				edaAnalysisTime = System.currentTimeMillis() - edaAnalysisStartTime;
				totalAnalysisTime += nfaConstructionTime + edaAnalysisTime;
				finishedEdaAnalysis = true;
				switch (analysisResultsType) {
				case NO_EDA:
					if (shouldTestIDA) {
						long idaAnalysisStartTime = System.currentTimeMillis();
						analysisResultsType = analyser.containsIDA(analysisGraph);
						idaAnalysisTime = System.currentTimeMillis() - idaAnalysisStartTime;
						totalAnalysisTime += idaAnalysisTime;
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
				if (DEBUG) {
					e.printStackTrace();
				}
				Thread.currentThread().interrupt();
				analysisResultsType = AnalysisResultsType.ANALYSIS_FAILED;
			} catch (OutOfMemoryError oome) {
				if (DEBUG) {
					oome.printStackTrace();
				}
				Thread.currentThread().interrupt();
				analysisResultsType = AnalysisResultsType.ANALYSIS_FAILED;
			}

			
		}
	}

}


