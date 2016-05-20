import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.regex.*;
import java.lang.management.*;
public class PumperJava {

	private static final long DELAY_TIME = 0000;

	public static void main(String [] args) {

		if (args.length < 4 || args.length % 2 != 0) {
			System.out.println("usage: <regex> <prefix> <pump_0> <pumpseparator_1> <pump_1> ... <pumpseparator_n> <pump_n> <suffix>");
			System.exit(0);
		}

		String regex = new String(args[0]);
			
		Pattern p = Pattern.compile(regex);
		int numPumpSeparators = (args.length - 2) / 2;
		int numPumps = (args.length - 2) / 2;
		//System.out.println(numPumpSeparators);
		//System.out.println(numPumps);
		String prefix = makeVerbatim(args[1]);
		String[] pumpSeparators = new String[numPumpSeparators];
		String[] pumps = new String[numPumps];
		for (int i = 1; i < args.length - 1; i += 2) {
			pumpSeparators[(i - 1) / 2] = makeVerbatim(args[i]);
			pumps[(i - 1) / 2] = makeVerbatim(args[i + 1]);
		}
		/*
		for (int i = 0; i < numPumps; i++) {
			System.out.println("ps_" + i + ": " + pumps[i] + " p_" + i + ": " + pumpSeparators[i]);
		}
		*/
		String suffix = makeVerbatim(args[args.length - 1]);

		int counter = 1;
		StringBuilder sb = new StringBuilder("Trying to exploit " + regex + " with ");
		for (int i = 1; i < args.length - 1; i += 2) {
			sb.append(args[i] + args[i + 1] + "..." + args[i + 1]);
		}
		sb.append(suffix);

		System.out.println(sb.toString());
		StringBuilder[] pumpers = new StringBuilder[numPumps];
		for (int i = 0; i < numPumps; i++) {
			pumpers[i] = new StringBuilder(pumps[i]);
		}
		while (true) {
			StringBuilder exploitStringBuilder = new StringBuilder();
			for (int i = 0; i < numPumps; i++) {
				exploitStringBuilder.append(pumpSeparators[i]);
				exploitStringBuilder.append(pumpers[i].toString());
				pumpers[i].append(pumps[i]);
			}
			exploitStringBuilder.append(suffix);
			String exploitString = exploitStringBuilder.toString();
			//System.out.println(exploitString);
			long startTime = System.currentTimeMillis();
			boolean matches = Pattern.matches(args[0], exploitString);
			long endTime = System.currentTimeMillis();
			System.out.println("Iteration: " + counter + "| String length: " + exploitString.length() + "| Match time: " + (endTime - startTime));

			counter++;
			try {
				Thread.sleep(DELAY_TIME);
			} catch (InterruptedException ie) {
			}
		}		
	}

	private static String makeVerbatim(String s) {
		String toReturn = s;
		Pattern hexCharsPattern = Pattern.compile("\\\\x([0-9a-fA-F]{2})");
    	Matcher m = hexCharsPattern.matcher(s);
		while (m.find()) {
			toReturn = toReturn.replaceAll(Matcher.quoteReplacement(m.group(0)), Matcher.quoteReplacement("" + (char) Integer.parseInt(m.group(1), 16)));
		}
		return toReturn;
	}

}

/* vim: set tabstop=4
*/
