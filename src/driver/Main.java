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

	private static final String HELP_FLAG = "--help";

	private static final String SIMPLE_ANALYSIS_FLAG = "--simple";
	private static final String FULL_ANALYSIS_FLAG = "--full";

	private static final String MERGING_EPSILON_LOOPS_FLAG = "--merge";
	private static final String FLATTENING_EPSILON_LOOPS_FLAG = "--flatten";

	private static final String JAVA_NFA_CONSTRUCTION_FLAG = "--java";
	private static final String THOMPSON_NFA_CONSTRUCTION_FLAG = "--thompson";

	private static final String TEST_IDA_SETTING = "--ida";
	private static final String IS_VERBOSE_SETTING = "--verbose";
	private static final String CONSTRUCT_EDA_EXPLOIT_STRING_SETTING = "--construct-eda-exploit-string";
	private static final String TEST_EDA_EXPLOIT_STRING_SETTING = "--test-eda-exploit-string";
	private static final String CONSTRUCT_IDA_EXPLOIT_STRING_SETTING = "--construct-ida-exploit-string";
	private static final String TIMEOUT_SETTING = "--timeout";
	private static final String FILE_INPUT_SETTING = "--if";
	private static final String COMMAND_LINE_INPUT_SETTING = "--regex";


	/* default settings */
	private static final NFAConstruction DEFAULT_NFA_CONSTRUCTION = NFAConstruction.JAVA;
	private static final EpsilonLoopRemovalStrategy DEFAULT_EPSILON_LOOP_REMOVAL_STRATEGY = EpsilonLoopRemovalStrategy.FLATTENING;
	private static final PriorityRemovalStrategy DEFAULT_PRIORITY_REMOVAL_STRATEGY = PriorityRemovalStrategy.UNPRIORITISE;
	private static final InputType DEFAULT_INPUT_TYPE = InputType.USER_INPUT;
	private static final boolean DEFAULT_TEST_IDA = true;
	private static final boolean DEFAULT_IS_VERBOSE = true;
	private static final boolean DEFAULT_CONSTRUCT_EDA_EXPLOIT_STRING = true;
	private static final boolean DEFAULT_TEST_EDA_EXPLOIT_STRING = true;
	private static final boolean DEFAULT_CONSTRUCT_IDA_EXPLOIT_STRING = true;
	private static final int DEFAULT_TIMEOUT = 10;

	private static HashSet<String> commandLineFlags;
	private static HashMap<String, String> commandLineSettings;
	private static ArrayList<String> commandLineValues;




	public static void main(String[] args) {

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

		if (commandLineFlags.contains(HELP_FLAG)) {
			printUsage();
			System.exit(0);
		}

		NFAConstruction nfaConstruction = determineNFAConstruction();
		PreprocessingType preprocessingType = PreprocessingType.NONE;
		EpsilonLoopRemovalStrategy epsilonLoopRemovalStrategy = determineEpsilonLoopRemovalStrategy();
		

		PriorityRemovalStrategy priorityRemovalStrategy = determinePriorityRemovalStrategy();

		InputType inputType = determineInputType();

		boolean shouldTestIDA = determineWhetherShouldTestIDA();

		boolean isVerbose = determineWhetherIsVerbose();

		boolean shouldConstructEdaExploitString = determineWhetherShouldConstructEdaExploitString();
		
		boolean shouldTestEdaExploitString;
		if (shouldConstructEdaExploitString) {
			shouldTestEdaExploitString = determineWhetherShouldTestEdaExploitString();
		} else {
			shouldTestEdaExploitString = false;
		}
		if (shouldTestEdaExploitString && nfaConstruction != NFAConstruction.JAVA) {
			System.err.println("Warning: You cannot test the exploit strings for any construction other than Java. (setting test exploit string to false)");
			shouldTestEdaExploitString = false;
		}

		boolean shouldConstructIdaExploitString = determineWhetherShouldConstructIdaExploitString();


		int timeout = determineTimeoutValue();

		BufferedReader regexesReader = setupRegexesReader(inputType);

		
		InterfaceSettings interfaceSettings = new InterfaceSettings(inputType, isVerbose);
		AnalysisSettings analysisSettings = new AnalysisSettings(nfaConstruction, 
						preprocessingType, 
						epsilonLoopRemovalStrategy, 
						priorityRemovalStrategy, 
						shouldTestIDA, 
						shouldConstructEdaExploitString,
						shouldTestEdaExploitString, 
						shouldConstructIdaExploitString,
						timeout);		
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
		boolean containsFileInputSetting = commandLineSettings.containsKey(FILE_INPUT_SETTING);
		boolean containsCommandLineInputSetting = commandLineSettings.containsKey(COMMAND_LINE_INPUT_SETTING);

		int numSettings = 0;
		numSettings = containsFileInputSetting ? numSettings + 1 : numSettings;
		numSettings = containsCommandLineInputSetting ? numSettings + 1 : numSettings;
		if (numSettings > 1) {
			System.err.println("Contradicting settings for input method.");
			printUsage();
			System.exit(0);
		} else if (containsFileInputSetting) {
			return InputType.FILE_INPUT;
		} else if (containsCommandLineInputSetting) {
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

	private static boolean determineWhetherShouldConstructEdaExploitString() {
		/* we assume that if the user enters the flag without setting it to true or false, they want it true */
		boolean containsConstructEdaExploitStringFlag = commandLineFlags.contains(CONSTRUCT_EDA_EXPLOIT_STRING_SETTING);
		if (containsConstructEdaExploitStringFlag) {
			return true;
		}
		boolean containsConstructEdaExploitStringSetting = commandLineSettings.containsKey(CONSTRUCT_EDA_EXPLOIT_STRING_SETTING);
		if (containsConstructEdaExploitStringSetting) {
			String shouldConstructEdaExploitStringValueString = commandLineSettings.get(CONSTRUCT_EDA_EXPLOIT_STRING_SETTING);
			if (shouldConstructEdaExploitStringValueString.equalsIgnoreCase("true")) {
				return true;
			} else if (shouldConstructEdaExploitStringValueString.equalsIgnoreCase("false")) {
				return false;
			} else {
				System.err.println("Construct exploitstring should be true or false.");
				printUsage();
				System.exit(0);
			}
		}
		return DEFAULT_CONSTRUCT_EDA_EXPLOIT_STRING;
	}

	private static boolean determineWhetherShouldConstructIdaExploitString() {
		/* we assume that if the user enters the flag without setting it to true or false, they want it true */
		boolean containsConstructIdaExploitStringFlag = commandLineFlags.contains(CONSTRUCT_IDA_EXPLOIT_STRING_SETTING);
		if (containsConstructIdaExploitStringFlag) {
			return true;
		}
		boolean containsConstructIdaExploitStringSetting = commandLineSettings.containsKey(CONSTRUCT_IDA_EXPLOIT_STRING_SETTING);
		if (containsConstructIdaExploitStringSetting) {
			String shouldConstructIdaExploitStringValueString = commandLineSettings.get(CONSTRUCT_IDA_EXPLOIT_STRING_SETTING);
			if (shouldConstructIdaExploitStringValueString.equalsIgnoreCase("true")) {
				return true;
			} else if (shouldConstructIdaExploitStringValueString.equalsIgnoreCase("false")) {
				return false;
			} else {
				System.err.println("Construct exploitstring should be true or false.");
				printUsage();
				System.exit(0);
			}
		}
		return DEFAULT_CONSTRUCT_IDA_EXPLOIT_STRING;
	}

	private static boolean determineWhetherShouldTestEdaExploitString() {
		/* we assume that if the user enters the flag without setting it to true or false, they want it true */
		boolean containsTestEdaExploitStringFlag = commandLineFlags.contains(TEST_EDA_EXPLOIT_STRING_SETTING);
		if (containsTestEdaExploitStringFlag) {
			return true;
		}
		boolean containsTestEdaExploitStringSetting = commandLineSettings.containsKey(TEST_EDA_EXPLOIT_STRING_SETTING);
		if (containsTestEdaExploitStringSetting) {
			String shouldTestEdaExploitStringValueString = commandLineSettings.get(TEST_EDA_EXPLOIT_STRING_SETTING);
			if (shouldTestEdaExploitStringValueString.equalsIgnoreCase("true")) {
				return true;
			} else if (shouldTestEdaExploitStringValueString.equalsIgnoreCase("false")) {
				return false;
			} else {
				System.err.println("Test exploitstring should be true or false.");
				printUsage();
				System.exit(0);
			}
		}
		return DEFAULT_TEST_EDA_EXPLOIT_STRING;
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
			String fileName = commandLineSettings.get(FILE_INPUT_SETTING);
			try {
			regexesReader = new BufferedReader(new FileReader(fileName));
			} catch (FileNotFoundException fnfe) {
				System.err.println("File " + fileName + " not found.");
				System.exit(0);
			}
			break;
		case COMMAND_LINE_INPUT:
			/* Find regex in first command line value */
			String regex = commandLineSettings.get(COMMAND_LINE_INPUT_SETTING);
			InputStream inputStreamReader = new ByteArrayInputStream(regex.getBytes());
			regexesReader = new BufferedReader(new InputStreamReader(inputStreamReader));
			break;
		default:
			throw new RuntimeException("Unknown input type: " + inputType);
		}
		return regexesReader;
	}

	private static void printUsage() {
		System.out.println("usage: java -cp ./bin Main [--simple|--full] [--merge|--flatten] [--java|--thompson] [--if='inputfile.txt'|--regex='regex' |] [--ida=true|false] [--verbose=true|false] [--test-eda-exploit-string=true|false] [--timeout=d]");
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
		System.out.println("\tif='inputfile.txt':");
		System.out.println("\t\tAnalyse the regexes read from an input file named inputfile.txt.");
		System.out.println("\tregex='regex to analyse':");
		System.out.println("\t\tAnalyse the one regex specified in the command line argument 'regex'.");
		System.out.println("\tida=[true|false]:");
		System.out.println("\t\tTrue: Test for IDA aswell as EDA.");
		System.out.println("\t\tFalse: Only test for EDA.");
		System.out.println("\tverbose=[true|false]:");
		System.out.println("\t\tTrue: Print verbose output.");
		System.out.println("\t\tFalse: Do not print verbose output.");
		System.out.println("\ttest-eda-exploit-string=[true|false]:");
		System.out.println("\t\tTrue: Test the generated exploit strings on the corresponding regexes for exponential behaviour using the Java matcher (only valid when using Java construction (--java)), testing regexes for polynomial behaviour is not yet implented.");
		System.out.println("\t\tFalse: Do not test the generated exploit strings.");
		System.out.println("\ttimeout=d:");
		System.out.println("\t\tSet the timeout to d miliseconds. If d <= 0, timeout is disabled.");



	}
}
