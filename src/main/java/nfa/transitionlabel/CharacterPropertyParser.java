package nfa.transitionlabel;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.net.*;

import util.RangeSet;
import util.RangeSet.Range;

public class CharacterPropertyParser {

	private final String FILE_PATH = "../data/";
	
	private final String FILE_NAME = "predef_ranges.txt";

	private final String FILE;
	
	public static final int MIN_16UNICODE = 0;
	public static final int MAX_16UNICODE = 65536;
	
	private final String regex;
	private int index;
	public void setIndex(int index) {
		this.index = index;
	}
	
	boolean dataRead;
	private final HashMap<String, HashMap<String, RangeSet>> prefixToSuffixesToRanges;
	private final HashSet<String> caseInsensitivePrefixes;
	private final HashSet<String> caseInsensitiveSuffixes;
	
	public CharacterPropertyParser(String regex, int index) {
		URL binUrl = CharacterPropertyParser.class.getClassLoader().getResource("");
		String binAbsolutePath = binUrl.getPath();
		this.FILE = binAbsolutePath + FILE_PATH + FILE_NAME;

		this.prefixToSuffixesToRanges = new HashMap<String, HashMap<String, RangeSet>>();
		this.caseInsensitivePrefixes = new HashSet<String>();
		this.caseInsensitiveSuffixes = new HashSet<String>();
		
		this.regex = regex; /* only used for exception messages */
		this.index = index; /* only used for exception messages */
		dataRead = false;
	}
	
	
	private void readData() {
		try {
			BufferedReader fileReader = new BufferedReader(new FileReader(new File(FILE)));
			try {
				while (fileReader.ready()) {
					String line = fileReader.readLine();
					String fields[] = line.split(":");
					String prefixesStr = fields[0];
					String prefixCS = fields[1];
					String suffixesStr = fields[2];
					String suffixCS = fields[3];
					String rangesStr = fields[4];
					String[] ranges = rangesStr.split(",");
					RangeSet rangeSet = new RangeSet(MIN_16UNICODE, MAX_16UNICODE);
					/* We first put the Ranges in a separate list, so we only have to union (and so merge) once */
					List<Range> rangesToAdd = new LinkedList<Range>();
					for (String range : ranges) {
						if (range.contains("-")) {
							int index = range.indexOf("-");
							String minBoundStr = range.substring(0, index);
							String maxBoundStr = range.substring(index + 1);
							int minBound = Integer.parseInt(minBoundStr);
							int maxBound = Integer.parseInt(maxBoundStr);
							rangesToAdd.add(rangeSet.createRange(minBound, maxBound + 1));
						} else {
							int rangeInt = Integer.parseInt(range);
							rangesToAdd.add(rangeSet.createRange(rangeInt));
						}
					}
					rangeSet.union(rangesToAdd);
					
					String prefixes[] = prefixesStr.split(",");
					for (String prefix : prefixes) {
						String suffixes[] = suffixesStr.split(",");
						for (String suffix : suffixes) {
							if (prefixCS.equals("false")) {
								caseInsensitivePrefixes.add(prefix);
							}
							if (suffixCS.equals("false")) {
								caseInsensitiveSuffixes.add(suffix);
							}
							
							if (prefixToSuffixesToRanges.containsKey(prefix)) {
								/* Get suffixes currently associated with prefix */
								HashMap<String, RangeSet> newSuffixesToRanges = prefixToSuffixesToRanges.get(prefix);
								if (newSuffixesToRanges.containsKey(suffix)) {
									RangeSet oldRangesSet = newSuffixesToRanges.get(suffix);
									if (!oldRangesSet.equals(rangeSet)) {
										throw new RuntimeException("Contradicting ranges for prefix and suffix");
									}
								} else {
									newSuffixesToRanges.put(suffix, rangeSet);
									prefixToSuffixesToRanges.put(prefix, newSuffixesToRanges);
								}
								
							} else {
								HashMap<String, RangeSet> newSuffixesToRanges = new HashMap<String, RangeSet>();
								newSuffixesToRanges.put(suffix, rangeSet);
								prefixToSuffixesToRanges.put(prefix, newSuffixesToRanges);
							}
						}
					}
					
					
				}
			} catch (IOException ioe) {
				ioe.printStackTrace();
			} finally {
				try {
					fileReader.close();
				} catch (IOException e) {
					
					e.printStackTrace();
				}
			}
		} catch (FileNotFoundException e) {
			System.err.println("The file " + FILE + " was not found in " + System.getProperty("user.dir"));
			e.printStackTrace();
		}
	}
	
	public RangeSet parseCharacterPropertyIterative(String characterProperty) {
		RangeSet toReturn = new RangeSet(MIN_16UNICODE, MAX_16UNICODE);
		/* We first put the Ranges in a separate list, so we only have to union (and so merge) once */
		List<Range> rangesToAdd = new LinkedList<Range>();
		String regex = "\\p{" + characterProperty + "}";
		Pattern p1 = Pattern.compile(regex);
		int i;
		int startRange = 0;
		for (i = 0; i < MAX_16UNICODE; i++) {
			String input = ("" + ((char) i));
			if (!p1.matcher(input).matches()) {
				if (startRange < i - 1) {						
					if ((startRange + 1) == (i - 1)) {
						//System.out.print((startRange + 1));
						Range r1 = toReturn.createRange((startRange + 1));
						rangesToAdd.add(r1);
					} else {
						//System.out.print((startRange + 1) + "-" + (i - 1));
						Range r1 = toReturn.createRange((startRange + 1), i);
						rangesToAdd.add(r1);
					}
				}
				startRange = i;
				
			}
		}
		toReturn.union(rangesToAdd);
		if (startRange < i - 1) {
			if ((startRange + 1) == (i - 1)) {
				//System.out.print((startRange + 1));
				Range r1 = toReturn.createRange((startRange + 1));
				toReturn.union(r1);
			} else {
				//System.out.print((startRange + 1) + "-" + (i - 1));
				Range r1 = toReturn.createRange((startRange + 1), i);
				toReturn.union(r1);
			}
		}
		return toReturn;
	}
	
	public RangeSet parseCharacterProperty(String characterProperty) {
		return parseCharacterPropertyStored(characterProperty);
		//return parseCharacterPropertyIterative(characterProperty);
	}
	
	public RangeSet parseCharacterPropertyStored(String characterProperty) {
		if (!dataRead) {
			readData();
			dataRead = true;
		}

		RangeSet toReturn = null;
		
		boolean found = false;
		for (String prefix : prefixToSuffixesToRanges.keySet()) {
			if (characterProperty.startsWith(prefix) || (caseInsensitivePrefixes.contains(prefix) && characterProperty.toLowerCase().startsWith(prefix))) {
				String suffix = characterProperty.substring(prefix.length());
				HashMap<String, RangeSet> suffixesToRanges = prefixToSuffixesToRanges.get(prefix);
				if (suffixesToRanges.containsKey(suffix)) {
					toReturn = suffixesToRanges.get(suffix);
					found = true;
				} else if ((caseInsensitiveSuffixes.contains(suffix.toUpperCase()) && suffixesToRanges.containsKey(suffix.toUpperCase()))) {
					toReturn = suffixesToRanges.get(suffix.toUpperCase());
					found = true;
				}
			}
		}
		
		if (!found) {
			throw new PatternSyntaxException("Unknown character property name {" + characterProperty + "}", regex, index);
		}
		
		return toReturn;
	}
	
	public static void main(String [] args) {
		/*String charProperty = "IsLetter";
		CharacterPropertyParser cpp = new CharacterPropertyParser("[\\p{" + charProperty + "}]", 3);
		
		RangeSet fileRS = cpp.parseCharacterProperty(charProperty);
		RangeSet iterativeRS = cpp.parseCharacterProperty(charProperty);
		if (!fileRS.equals(iterativeRS)) {
			System.out.println("File:\t" + fileRS);
			System.out.println("Itr:\t" + iterativeRS);
		} else {
			System.out.println(fileRS);
		}*/
		testAll();
	}
	
	private static void testAll() {
		CharacterPropertyParser cpp = new CharacterPropertyParser("\\p{...}", 3);
		cpp.readData();
		for (Map.Entry<String, HashMap<String, RangeSet>> kv : cpp.prefixToSuffixesToRanges.entrySet()) {
			String prefixOriginal = kv.getKey();
			List<String> prefixes = new LinkedList<String>();
			prefixes.add(prefixOriginal);
			if (cpp.caseInsensitivePrefixes.contains(prefixOriginal)) {
				int possibleRandomCasesLeft = (1 << prefixOriginal.length()) - 1;
				int maxToAdd = 3;
				int numToAdd = possibleRandomCasesLeft > maxToAdd ? maxToAdd : possibleRandomCasesLeft;
				prefixes.addAll(randomiseCase(prefixOriginal, numToAdd));
			}
			for (String prefix : prefixes) {
				int prefixCounter = 0;
				HashMap<String, RangeSet> suffixToRangeSet = kv.getValue();
				for (Map.Entry<String, RangeSet> kv2 : suffixToRangeSet.entrySet()) {
					String suffixOriginal = kv2.getKey();
					List<String> suffixes = new LinkedList<String>();
					suffixes.add(suffixOriginal);
					if (cpp.caseInsensitiveSuffixes.contains(suffixOriginal)) {
						int possibleRandomCasesLeft = (1 << suffixOriginal.length()) - 1;
						int maxToAdd = 3;
						int numToAdd = possibleRandomCasesLeft > maxToAdd ? maxToAdd : possibleRandomCasesLeft;
						suffixes.addAll(randomiseCase(suffixOriginal, numToAdd));
					}
					for (String suffix : suffixes) {
						RangeSet rangeSet = kv2.getValue();
						String charProperty = prefix + suffix;
						
						RangeSet correctRangeSet = cpp.parseCharacterPropertyIterative(charProperty);
						if (!rangeSet.equals(correctRangeSet)) {
							System.err.println("\tFile:\t" + rangeSet);
							System.err.println("\tItr:\t" + correctRangeSet);
						} else {
							//System.out.println(counter + " Correct: " + regex);
						}
						prefixCounter++;
					}					
				}
				System.out.println(prefix + " total: " + prefixCounter);
			}
			
		}
	}
	
	private static List<String> randomiseCase(String original, int numToAdd) {
		Set<String> generated = new HashSet<String>();
		int breakCounter = 0;
		int breakMax = 10000;
		int i = 0;
		Random r = new Random();
		while (i < numToAdd) {
			StringBuilder sb = new StringBuilder();
			for (char c : original.toCharArray()) {
				double randomDouble = r.nextDouble();
				if (randomDouble >= 0.5) {
					c = Character.toUpperCase(c);
				} else {
					c = Character.toLowerCase(c);
				}
				sb.append(c);
			}
			String toAdd = sb.toString();
			if (!generated.contains(toAdd) && !original.equals(toAdd)) {
				generated.add(toAdd);
				breakCounter = 0;
				i++;
			} else {
				breakCounter++;
				if (breakCounter > breakMax) {
					throw new IllegalArgumentException("Cannot generate " + numToAdd + " unique from " + original);
				}
			}

		}
		
		return new LinkedList<String>(generated);
	}
	
}
