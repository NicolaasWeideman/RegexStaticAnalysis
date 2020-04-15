package preprocessor;

import java.util.HashMap;
import java.util.Map;

public abstract class SubstitutionPreprocessor implements Preprocessor {
	
	
	private HashMap<String, String> rules;
	
	public abstract String[][] getRuleStrings();
	
	public SubstitutionPreprocessor() {
		rules = new HashMap<String, String>();
		
	}
	
	public String applyRules(String regex) {
		for (Map.Entry<String, String> findReplace : rules.entrySet()) {
			String findString = findReplace.getKey();
			String replaceString = findReplace.getValue();
			
			regex = regex.replaceAll(findString, replaceString);
			
		}
		return regex;
	}
	
	protected void addRule(String findString, String replaceString) {
		if (rules.containsKey(findString)) {
			throw new IllegalArgumentException("Rule for " + findString + " already exists.");
		}
		rules.put(findString, replaceString);
	}
	
	protected void removeRule(String findString) {
		rules.remove(findString);
	}
	
	

}
