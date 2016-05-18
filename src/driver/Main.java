package driver;

import java.util.HashSet;
import java.util.HashMap;
import java.util.ArrayList;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ByteArrayInputStream;
import java.io.FileReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;

import analysis.AnalysisSettings;
import analysis.AnalysisSettings.NFAConstruction;
import analysis.AnalysisSettings.PreprocessingType;
import analysis.AnalysisSettings.EpsilonLoopRemovalStrategy;
import analysis.AnalysisSettings.PriorityRemovalStrategy;
import analysis.driver.AnalysisDriverStdOut;
import util.InterfaceSettings;
import util.InterfaceSettings.InputType;


public class Main {

	private static final String SIMPLE_ANALYSIS_FLAG = "--simple";
	private static final String FULL_ANALYSIS_FLAG = "--full";

	private static final String MERGING_EPSILON_LOOPS_FLAG = "--merge";
	private static final String FLATTENING_EPSILON_LOOPS_FLAG = "--flatten";

	private static final String JAVA_NFA_CONSTRUCTION_FLAG = "--java";
	private static final String THOMPSON_NFA_CONSTRUCTION_FLAG = "--thompson";

	private static final String FILE_INPUT_FLAG = "-i"; // To match the RXXR Tool
	private static final String USER_INPUT_FLAG = "-u";
	private static final String COMMAND_LINE_INPUT_FLAG = "-c";

	private static final String TEST_IDA_SETTING = "--ida";
	private static final String IS_VERBOSE_SETTING = "--verbose";
	private static final String TEST_EXPLOIT_STRING_SETTING = "--testexploitstring";
	private static final String TIMEOUT_SETTING = "--timeout";


	/* default settings */
	private static final NFAConstruction DEFAULT_NFA_CONSTRUCTION = NFAConstruction.JAVA;
	private static final EpsilonLoopRemovalStrategy DEFAULT_EPSILON_LOOP_REMOVAL_STRATEGY = EpsilonLoopRemovalStrategy.FLATTENING;
	private static final PriorityRemovalStrategy DEFAULT_PRIORITY_REMOVAL_STRATEGY = PriorityRemovalStrategy.UNPRIORITISE;
	private static final InputType DEFAULT_INPUT_TYPE = InputType.USER_INPUT;
	private static final boolean DEFAULT_TEST_IDA = true;
	private static final boolean DEFAULT_IS_VERBOSE = true;
	private static final boolean DEFAULT_TEST_EXPLOIT_STRING = true;
	private static final int DEFAULT_TIMEOUT = 10;

	private static HashSet<String> commandLineFlags;
	private static HashMap<String, String> commandLineSettings;
	private static ArrayList<String> commandLineValues;




	public static void main(String[] args) {

		if (args.length == 0) {
			printUsage();
			System.exit(0);
		}

		commandLineFlags = new HashSet<String>();
		commandLineSettings = new HashMap<String, String>();
		commandLineValues = new ArrayList<String>();
		for (String arg : args) {
			if (arg.startsWith("-")) {
				if (arg.contains("=")) {
					int settingLastIndex = arg.indexOf("=");
					String settingName = arg.substring(0, settingLastIndex);
					String settingValue = arg.substring(settingLastIndex + 1); 
					commandLineSettings.put(settingName, settingValue);
				} else {
					commandLineFlags.add(arg);
				}
			} else {
				commandLineValues.add(arg);
			}
		}

		NFAConstruction nfaConstruction = determineNFAConstruction();
		PreprocessingType preprocessingType = PreprocessingType.NONE;
		EpsilonLoopRemovalStrategy epsilonLoopRemovalStrategy = determineEpsilonLoopRemovalStrategy();
		

		PriorityRemovalStrategy priorityRemovalStrategy = determinePriorityRemovalStrategy();

		InputType inputType = determineInputType();

		boolean shouldTestIDA = determineWhetherShouldTestIDA();

		boolean isVerbose = determineWhetherIsVerbose();
		
		boolean shouldTestExploitString = determineWhetherShouldTestExploitString();
		if (shouldTestExploitString && nfaConstruction != NFAConstruction.JAVA) {
			System.err.println("Warning: You cannot test the exploit strings for any construction other than Java. (setting test exploit string to false)");
			shouldTestExploitString = false;
		}


		int timeout = determineTimeoutValue();

		BufferedReader regexesReader = setupRegexesReader(inputType);

		
		InterfaceSettings interfaceSettings = new InterfaceSettings(inputType, isVerbose);
		AnalysisSettings analysisSettings = new AnalysisSettings(nfaConstruction, preprocessingType, epsilonLoopRemovalStrategy, priorityRemovalStrategy, shouldTestIDA, shouldTestExploitString, timeout);		
		AnalysisDriverStdOut.performAnalysis(regexesReader, interfaceSettings, analysisSettings);
	}

	private static PriorityRemovalStrategy determinePriorityRemovalStrategy() {
		boolean containsSimpleAnalysisFlag = commandLineFlags.contains(SIMPLE_ANALYSIS_FLAG);
		boolean containsFullAnalysisFlag = commandLineFlags.contains(FULL_ANALYSIS_FLAG);
		if (containsSimpleAnalysisFlag && containsFullAnalysisFlag) {
			System.err.println("Contradicting flags: " + SIMPLE_ANALYSIS_FLAG + " " + FULL_ANALYSIS_FLAG);
			printUsage();
			System.exit(0);
		} else if (containsSimpleAnalysisFlag) {
			return PriorityRemovalStrategy.IGNORE;
		} else if (containsFullAnalysisFlag) {
			return PriorityRemovalStrategy.UNPRIORITISE;
		}
		return DEFAULT_PRIORITY_REMOVAL_STRATEGY;
	}

	private static EpsilonLoopRemovalStrategy determineEpsilonLoopRemovalStrategy() {
		boolean containsMergingEpsilonLoopsFlag = commandLineFlags.contains(MERGING_EPSILON_LOOPS_FLAG);
		boolean containsFlatteningEpsilonLoopsFlag = commandLineFlags.contains(FLATTENING_EPSILON_LOOPS_FLAG);
		if (containsMergingEpsilonLoopsFlag && containsFlatteningEpsilonLoopsFlag) {
			System.err.println("Contradicting flags: " + MERGING_EPSILON_LOOPS_FLAG + " " + FLATTENING_EPSILON_LOOPS_FLAG);
			printUsage();
			System.exit(0);
		} else if (containsMergingEpsilonLoopsFlag) {
			return EpsilonLoopRemovalStrategy.MERGING;
		} else if (containsFlatteningEpsilonLoopsFlag) {
			return EpsilonLoopRemovalStrategy.FLATTENING;
		}
		return DEFAULT_EPSILON_LOOP_REMOVAL_STRATEGY;
	}

	private static NFAConstruction determineNFAConstruction() {
		boolean containsJavaNFAConstructionFlag = commandLineFlags.contains(JAVA_NFA_CONSTRUCTION_FLAG);
		boolean containsThompsonNFAConstructionFlag = commandLineFlags.contains(THOMPSON_NFA_CONSTRUCTION_FLAG);
		if (containsJavaNFAConstructionFlag && containsThompsonNFAConstructionFlag) {
			System.err.println("Contradicting flags: " + JAVA_NFA_CONSTRUCTION_FLAG + " " + THOMPSON_NFA_CONSTRUCTION_FLAG);
			printUsage();
			System.exit(0);
		} else if (containsJavaNFAConstructionFlag) {
			return NFAConstruction.JAVA;
		} else if (containsThompsonNFAConstructionFlag) {
			return NFAConstruction.THOMPSON;
		}
		return DEFAULT_NFA_CONSTRUCTION;
	}

	private static InputType determineInputType() {
		boolean containsUserInputFlag = commandLineFlags.contains(USER_INPUT_FLAG);
		boolean containsFileInputFlag = commandLineFlags.contains(FILE_INPUT_FLAG);
		boolean containsCommandLineInputFlag = commandLineFlags.contains(COMMAND_LINE_INPUT_FLAG);

		int numFlags = 0;
		numFlags = containsUserInputFlag ? numFlags + 1 : numFlags;
		numFlags = containsFileInputFlag ? numFlags + 1 : numFlags;
		numFlags = containsCommandLineInputFlag ? numFlags + 1 : numFlags;
		if (numFlags > 1) {
			System.err.println("Contradicting flags for input method.");
			printUsage();
			System.exit(0);
		} else if (containsUserInputFlag) {
			return InputType.USER_INPUT;
		} else if (containsFileInputFlag) {
			return InputType.FILE_INPUT;
		} else if (containsCommandLineInputFlag) {
			return InputType.COMMAND_LINE_INPUT;
		}
		return DEFAULT_INPUT_TYPE;
	}

	private static boolean determineWhetherShouldTestIDA() {
		/* we assume that if the user enters the flag without setting it to true or false, they want it true */
		boolean containsTestIDAFlag = commandLineFlags.contains(TEST_IDA_SETTING);
		if (containsTestIDAFlag) {
			return true;
		}
		boolean containsTestIDASetting = commandLineSettings.containsKey(TEST_IDA_SETTING);
		if (containsTestIDASetting) {
			String testIDAValueString = commandLineSettings.get(TEST_IDA_SETTING);
			if (testIDAValueString.equalsIgnoreCase("true")) {
				return true;
			} else if (testIDAValueString.equalsIgnoreCase("false")) {
				return false;
			} else {
				System.err.println("Test IDA should be true or false.");
				printUsage();
				System.exit(0);
			}
		}
		return DEFAULT_TEST_IDA;
	}

	private static boolean determineWhetherIsVerbose() {
		/* we assume that if the user enters the flag without setting it to true or false, they want it true */
		boolean containsIsVerboseFlag = commandLineFlags.contains(IS_VERBOSE_SETTING);
		if (containsIsVerboseFlag) {
			return true;
		}
		boolean containsIsVerboseSetting = commandLineSettings.containsKey(IS_VERBOSE_SETTING);
		if (containsIsVerboseSetting) {
			String isVerboseValueString = commandLineSettings.get(IS_VERBOSE_SETTING);
			if (isVerboseValueString.equalsIgnoreCase("true")) {
				return true;
			} else if (isVerboseValueString.equalsIgnoreCase("false")) {
				return false;
			} else {
				System.err.println("Is verbose should be true or false.");
				printUsage();
				System.exit(0);
			}
		}
		return DEFAULT_IS_VERBOSE;
	}

	private static boolean determineWhetherShouldTestExploitString() {
		/* we assume that if the user enters the flag without setting it to true or false, they want it true */
		boolean containsTestExploitStringFlag = commandLineFlags.contains(TEST_EXPLOIT_STRING_SETTING);
		if (containsTestExploitStringFlag) {
			return true;
		}
		boolean containsTestExploitStringSetting = commandLineSettings.containsKey(TEST_EXPLOIT_STRING_SETTING);
		if (containsTestExploitStringSetting) {
			String shouldTestExploitStringValueString = commandLineSettings.get(TEST_EXPLOIT_STRING_SETTING);
			if (shouldTestExploitStringValueString.equalsIgnoreCase("true")) {
				return true;
			} else if (shouldTestExploitStringValueString.equalsIgnoreCase("false")) {
				return false;
			} else {
				System.err.println("Test exploitstring should be true or false.");
				printUsage();
				System.exit(0);
			}
		}
		return DEFAULT_TEST_EXPLOIT_STRING;
	}

	private static int determineTimeoutValue() {
		boolean containsTimeoutSetting = commandLineSettings.containsKey(TIMEOUT_SETTING);
		if (containsTimeoutSetting) {
			String timeoutValueString = commandLineSettings.get(TIMEOUT_SETTING);
			try {
				int timeoutValue = Integer.parseInt(timeoutValueString);
				return timeoutValue;
			} catch (NumberFormatException nfe) {
				System.err.println("Timeout should be an integer value.");
				printUsage();
				System.exit(0);
			}
		}
		return DEFAULT_TIMEOUT;
	}

	private static BufferedReader setupRegexesReader(InputType inputType) {
		BufferedReader regexesReader = null;
		switch (inputType) {
		case USER_INPUT:
			regexesReader = new BufferedReader(new InputStreamReader(System.in));
			break;
		case FILE_INPUT:
			/* Find file name in first command line value */
			String fileName = commandLineValues.get(0);
			try {
			regexesReader = new BufferedReader(new FileReader(fileName));
			} catch (FileNotFoundException fnfe) {
				System.err.println("File " + fileName + " not found.");
				System.exit(0);
			}
			break;
		case COMMAND_LINE_INPUT:
			/* Find regex in first command line value */
			String regex = commandLineValues.get(0);
			InputStream inputStreamReader = new ByteArrayInputStream(regex.getBytes());
			regexesReader = new BufferedReader(new InputStreamReader(inputStreamReader));
			break;
		default:
			throw new RuntimeException("Unknown input type: " + inputType);
		}
		return regexesReader;
	}

	private static void printUsage() {
		System.out.println("usage: java -cp ./bin Main [--simple|--full] [--merge|--flatten] [--java|--thompson] [-i 'inputfile.txt'|-c 'regex' |-u] [--ida=true|false] [--verbose=true|false] [--textexploitstring=true|false] [--timeout=d]");
		System.out.println("\tsimple:");
		System.out.println("\t\tPerform the simple analysis.");
		System.out.println("\tfull:");
		System.out.println("\t\tPerform the full analysis.");
		System.out.println("\tmerge:");
		System.out.println("\t\tRemove the epsilon loops by merging them.");
		System.out.println("\tflatten:");
		System.out.println("\t\tRemove the epsilon loops by flattening them.");
		System.out.println("\tjava:");
		System.out.println("\t\tConstruct the pNFAs to approximate Java behaviour.");
		System.out.println("\tthompson:");
		System.out.println("\t\tConstruct the pNFAs using Thompson construction.");
		System.out.println("\ti 'inputfile.txt':");
		System.out.println("\t\tAnalyse the regexes read from an input file named inputfile.txt.");
		System.out.println("\tc 'regex':");
		System.out.println("\t\tAnalyse the one regex specified in the command line argument 'regex'.");
		System.out.println("\tu:");
		System.out.println("\t\tAnalyse the regexes read from stdin.");
		System.out.println("\tida=[true|false]:");
		System.out.println("\t\tTrue: Test for IDA aswell as EDA.");
		System.out.println("\t\tFalse: Only test for EDA.");
		System.out.println("\tverbose=[true|false]:");
		System.out.println("\t\tTrue: Print verbose output.");
		System.out.println("\t\tFalse: Do not print verbose output.");
		System.out.println("\ttestexploitstring=[true|false]:");
		System.out.println("\t\tTrue: Test the generated exploit strings on the corresponding regexes for exponential behaviour using the Java matcher (only valid when using Java construction (--java)), testing regexes for polynomial behaviour is not yet implented.");
		System.out.println("\t\tFalse: Do not test the generated exploit strings.");
		System.out.println("\ttimeout=d:");
		System.out.println("\t\tSet the timeout to d miliseconds. If d <= 0, timeout is disabled.");



	}
}
