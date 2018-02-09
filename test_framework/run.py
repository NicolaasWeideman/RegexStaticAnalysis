import subprocess
import re
import os
import sys
import time
import datetime
import operator

PATH_TO_ROOT = '../'
PATH_TO_TEST_RESULTS_DIR = './test_results/'

JAVA = '/usr/bin/java'
MEMORY_SETTINGS = '-Xms2048m'
CLASS_PATH = '/bin/'

PATH_TO_TESTS = PATH_TO_ROOT + 'tests/'

DEFAULT_TIMEOUT = "10"
timeout = DEFAULT_TIMEOUT


TIME_FORMAT = "%H:%M:%S, %9A, %d-%m-%Y"

def main(argv):
	global timeout
	if len(argv) < 2:
		print "usage: python ./" + argv[0] + " test_file1 test_file2 ..."
		return
	test_files = []
	for arg in argv[1:]:
		match = re.search(r'^--timeout=(\d+)', arg)
		if match:
			timeout = match.group(1)
		else:
			test_files.append(arg)
			
	for test_file in test_files:
		print "Test file: " + test_file
		analyse_regexes_file(test_file)
		print "========================"


def analyse_regexes_file(test_file_name):
	global_start_time = time.localtime()
	#f = open(PATH_TO_TESTS + TEST_FILE_NAME, 'r')
	f = open(test_file_name, 'r')
	all_regexes = f.readlines()
	num_test_cases = len(all_regexes)
	time_str = "{:%d%b%Y_%H:%M:%S}".format(datetime.datetime.now())
	results_directory = PATH_TO_TEST_RESULTS_DIR + os.path.splitext(os.path.basename(test_file_name))[0] + "_" + time_str
	os.makedirs(results_directory)
	test_regexes_file = open(results_directory + '/test_regexes.txt', 'w')
	for regex in all_regexes:
		test_regexes_file.write(regex)
	test_regexes_file.close()
	f.close()

	# Running simple analysis
	simple_analysis_results = run_simple_analysis(test_file_name, num_test_cases)	

	simple_analysis_results_file = open(results_directory + '/simple_analysis_results.txt', 'w')
	simple_analysis_results_file.write(simple_analysis_results)
	simple_analysis_results_file.close()

	# Remove the summary at the end of the simple_analysis_results
	simple_analysis_summary_removed = re.sub(re.compile(r'Construction: .*?Total running time: \d*.*', re.DOTALL), '', simple_analysis_results)

	# Open files to keep potentially_evil regexes (and seperate eda, ida and fda  evil regexes)
	eda_regexes_file = open(results_directory + '/simple_analysis_eda_regexes.txt', 'w')
	eda_regexes_numbered_file = open(results_directory + '/simple_analysis_eda_regexes_numbered.txt', 'w')
	ida_regexes_file = open(results_directory + '/simple_analysis_ida_regexes.txt', 'w')
	ida_regexes_numbered_file = open(results_directory + '/simple_analysis_ida_regexes_numbered.txt', 'w')
	fda_regexes_file = open(results_directory + '/simple_analysis_fda_regexes.txt', 'w')
	fda_regexes_numbered_file = open(results_directory + '/simple_analysis_fda_regexes_numbered.txt', 'w')

	simple_analysis_results_set = {}
	num_eda_regexes = 0
	num_ida_regexes = 0
	num_fda_regexes = 0
	potentially_evil_regexes_matches = re.findall(r'^(\d+): ([^\n]*)\n(EDA|IDA_(\d+|\?)|NO IDA)', simple_analysis_summary_removed, re.MULTILINE)
	for potentially_evil_regex_match in potentially_evil_regexes_matches:
		potentially_evil_regex_number = potentially_evil_regex_match[0]
		potentially_evil_regex = potentially_evil_regex_match[1]
		potentially_evil_regex_result = potentially_evil_regex_match[2]
		if potentially_evil_regex_result ==  'EDA':
			num_eda_regexes += 1
			eda_regexes_file.write(potentially_evil_regex + "\n")
			eda_regexes_numbered_file.write(potentially_evil_regex_number + ": " + potentially_evil_regex + "\n")
		elif re.match(r'IDA_(\d+|\?)', potentially_evil_regex_result):
			num_ida_regexes += 1
			ida_regexes_file.write(potentially_evil_regex + "\n")
			ida_regexes_numbered_file.write(potentially_evil_regex_number + ": " + potentially_evil_regex + "\n")
		elif potentially_evil_regex_result == 'NO IDA':
			num_fda_regexes += 1
			fda_regexes_file.write(potentially_evil_regex + "\n")
			fda_regexes_numbered_file.write(potentially_evil_regex_number + ": " + potentially_evil_regex + "\n")
		else:
			raise ValueError('Unknown simple analysis result: ' + potentially_evil_regex_result)
		simple_analysis_results_set[potentially_evil_regex] = (potentially_evil_regex_number, potentially_evil_regex_result)
	num_potentially_evil_regexes = len(potentially_evil_regexes_matches)
	eda_regexes_numbered_file.close()
	eda_regexes_file.close()
	ida_regexes_numbered_file.close()
	ida_regexes_file.close()
	fda_regexes_numbered_file.close()
	fda_regexes_file.close()

	simple_analysis_timeout_regexes_file = open(results_directory + '/simple_analysis_timeout_' + str(timeout) + 's_regexes.txt', 'w')
	simple_analysis_timeout_regexes_numbered_file = open(results_directory + '/simple_analysis_timeout_' + str(timeout) + 's_regexes_numbered.txt', 'w')
	timeout_regexes_matches = re.findall(r'(\d+): ([^\n]*)\nTIMEOUT in (?:E|I)DA', simple_analysis_summary_removed, re.MULTILINE)
	for timeout_regexes_match in timeout_regexes_matches:
		timeout_regex_number = timeout_regexes_match[0]
		timeout_regex = timeout_regexes_match[1]
		simple_analysis_timeout_regexes_file.write(timeout_regex + "\n")
		simple_analysis_timeout_regexes_numbered_file.write(timeout_regex_number + ": " + timeout_regex + "\n")
	simple_analysis_timeout_regexes_file.close()
	simple_analysis_timeout_regexes_numbered_file.close()
	

	# Running full analysis for simple analysis eda regexes
	full_analysis_eda_results = run_full_analysis(results_directory + '/simple_analysis_eda_regexes.txt', num_eda_regexes)
	full_analysis_eda_results_file = open(results_directory + '/full_analysis_eda_results.txt', 'w')
	full_analysis_eda_results_file.write(full_analysis_eda_results)
	full_analysis_eda_results_file.close()

	# Running full analysis for simple analysis ida regexes
	full_analysis_ida_results = run_full_analysis(results_directory + '/simple_analysis_ida_regexes.txt', num_ida_regexes)
	full_analysis_ida_results_file = open(results_directory + '/full_analysis_ida_results.txt', 'w')
	full_analysis_ida_results_file.write(full_analysis_ida_results)
	full_analysis_ida_results_file.close()

	# Running full analysis for simple analysis fda regexes
	full_analysis_fda_results = run_full_analysis(results_directory + '/simple_analysis_fda_regexes.txt', num_fda_regexes)
	full_analysis_fda_results_file = open(results_directory + '/full_analysis_fda_results.txt', 'w')
	full_analysis_fda_results_file.write(full_analysis_fda_results)
	full_analysis_fda_results_file.close()

	global_end_time = time.localtime()

	global_end_time_datetime = datetime.datetime.fromtimestamp(time.mktime(global_end_time))
	global_start_time_datetime = datetime.datetime.fromtimestamp(time.mktime(global_start_time))
	global_run_time_datetime = global_end_time_datetime - global_start_time_datetime
	seconds = int(global_run_time_datetime.seconds)
	days, seconds = divmod(seconds, 86400)
	hours, seconds = divmod(seconds, 3600)
	minutes, seconds = divmod(seconds, 60)
	global_run_time_str = "{0} days, {1} hours, {2:02d} minutes and {3:02d} seconds.".format(days, hours, minutes, seconds)

	# Extracting Timeout regexes from full analyses
	full_analysis_timeout_regexes_file = open(results_directory + '/full_analysis_timeout_' + str(timeout) + 's_regexes.txt', 'w')
	full_analysis_timeout_regexes_numbered_file = open(results_directory + '/full_analysis_timeout_' + str(timeout) + 's_regexes_numbered.txt', 'w')
	timeout_regexes_matches_eda = re.findall(r'(\d+): ([^\n]*)\nTIMEOUT in (?:E|I)DA', full_analysis_eda_results, re.MULTILINE)
	timeout_regexes_matches_ida = re.findall(r'(\d+): ([^\n]*)\nTIMEOUT in (?:E|I)DA', full_analysis_ida_results, re.MULTILINE)
	timeout_regexes_matches_fda = re.findall(r'(\d+): ([^\n]*)\nTIMEOUT in (?:E|I)DA', full_analysis_fda_results, re.MULTILINE)
	timeout_regexes_matches = timeout_regexes_matches_eda + timeout_regexes_matches_ida + timeout_regexes_matches_fda
	for timeout_regexes_match in timeout_regexes_matches:
		timeout_regex_number = timeout_regexes_match[0]
		timeout_regex = timeout_regexes_match[1]
		full_analysis_timeout_regexes_file.write(timeout_regex + "\n")
		full_analysis_timeout_regexes_numbered_file.write(timeout_regex_number + ": " + timeout_regex + "\n")
	full_analysis_timeout_regexes_file.close()
	full_analysis_timeout_regexes_numbered_file.close()

	# Creating a summary
	summary_file = open(results_directory + '/summary.txt', 'w')
	summary_file.write("Timeout: " + str(timeout) + '\n')
	summary_file.write("Start time: " + time.strftime(TIME_FORMAT, global_start_time) + '\n')
	summary_file.write("End time:   " + time.strftime(TIME_FORMAT, global_end_time) + '\n')
	summary_file.write("Duration:   " + global_run_time_str + '\n')
	summary_file.write("=====Simple Analysis (total: " + str(num_test_cases) + "):=====\n")
	simple_analysis_eda_matches = re.findall(r'(\d+): ([^\n]*)\nEDA', simple_analysis_summary_removed, re.MULTILINE)
	simple_analysis_num_eda = len(simple_analysis_eda_matches)
	
	simple_analysis_ida_degrees = {}
	simple_analysis_ida_matches = re.findall(r'(\d+): ([^\n]*)\nIDA_(\d+|\?)', simple_analysis_summary_removed, re.MULTILINE)
	for simple_analysis_ida_match in simple_analysis_ida_matches:
		degree = simple_analysis_ida_match[2]
		if degree in simple_analysis_ida_degrees:
			simple_analysis_ida_degrees[degree] = simple_analysis_ida_degrees[degree] + 1
		else:
			simple_analysis_ida_degrees[degree] = 1
	simple_analysis_num_ida = len(simple_analysis_ida_matches)
	
	simple_analysis_no_ida_matches = re.findall(r'(\d+): ([^\n]*)\nNO IDA', simple_analysis_summary_removed, re.MULTILINE)
	simple_analysis_num_no_ida = len(simple_analysis_no_ida_matches)
	
	simple_analysis_skipped_matches = re.findall(r'(\d+): ([^\n]*)\nSKIPPED', simple_analysis_summary_removed, re.MULTILINE)
	simple_analysis_num_skipped = len(simple_analysis_skipped_matches)
	
	simple_analysis_timeout_in_eda_matches = re.findall(r'(\d+): ([^\n]*)\nTIMEOUT in EDA', simple_analysis_summary_removed, re.MULTILINE)
	simple_analysis_num_timeout_in_eda_matches = len(simple_analysis_timeout_in_eda_matches)
	
	simple_analysis_timeout_in_ida_matches = re.findall(r'(\d+): ([^\n]*)\nTIMEOUT in IDA', simple_analysis_summary_removed, re.MULTILINE)
	simple_analysis_num_timeout_in_ida_matches = len(simple_analysis_timeout_in_ida_matches)

	summary_file.write("EDA: " + str(simple_analysis_num_eda) + '\n')
	summary_file.write("IDA: " + str(simple_analysis_num_ida) + '\n')
	if simple_analysis_num_ida > 0:
		sorted_simple_analysis_ida_degrees = sorted(simple_analysis_ida_degrees.items(), key=operator.itemgetter(0))
		for k, v in sorted_simple_analysis_ida_degrees:
			degree = k
			num_with_degree = v
			summary_file.write("\t\tWith degree " + str(degree) + ": " + str(num_with_degree) + '\n')
	summary_file.write("No IDA: " + str(simple_analysis_num_no_ida) + '\n')
	summary_file.write("Skipped: " + str(simple_analysis_num_skipped) + '\n')
	summary_file.write("Timeout in EDA: " + str(simple_analysis_num_timeout_in_eda_matches) + '\n')
	summary_file.write("Timeout in IDA: " + str(simple_analysis_num_timeout_in_ida_matches) + '\n')

	summary_file.write("=====Full Analysis (total: " + str(num_potentially_evil_regexes) + "):=====\n")
	# Remove the summary at the end of the full analysis results
	full_analysis_eda_exp = []
	full_analysis_ida_exp = []
	full_analysis_fda_exp = [] # Finite degree of ambiguity
	full_analysis_eda_pol = []
	full_analysis_ida_pol = []
	full_analysis_fda_pol = [] # Finite degree of ambiguity
	full_analysis_eda_lin = []
	full_analysis_ida_lin = []
	full_analysis_fda_lin = [] # Finite degree of ambiguity

	# These regexes which had eda in the simple analysis, which have exp matching time according to full analysis
	full_analysis_eda_summary_removed = re.sub(re.compile(r'Construction: .*?Total running time: \d*.*', re.DOTALL), '', full_analysis_eda_results)
	eda_exploit_string_test_num_vuln = 0
	eda_exploit_string_test_num_not_vuln = 0
	eda_exploit_string_test_num_timeout = 0
	full_analysis_exp_matches = re.findall(r'(\d+): ([^\n]*)\n(EDA) ([^\n]*)', full_analysis_eda_summary_removed, re.MULTILINE)
	for full_analysis_exp_match in full_analysis_exp_matches:
		regex = full_analysis_exp_match[1]
		result = full_analysis_exp_match[2]
		exploit_string_test_result = full_analysis_exp_match[3]
		if exploit_string_test_result == 'MATCHER_CONFIRMED_EXP_TIME':
			eda_exploit_string_test_num_vuln += 1
		elif exploit_string_test_result == 'MATCHER_DID_NOT_DISPLAY_EXP_TIME':
			eda_exploit_string_test_num_not_vuln += 1
		elif exploit_string_test_result == 'NO_EXPLOIT_STRING_CONSTRUCTED':
			eda_exploit_string_test_num_timeout += 1
		else:
			raise ValueError('Unknown exploit string test result: ' + exploit_string_test_result)
		full_analysis_eda_exp.append(regex) 
	# These regexes which had eda in the simple analysis, which have pol matching time according to full analysis
	full_analysis_eda_ida_degrees = {}
	full_analysis_pol_matches = re.findall(r'(\d+): ([^\n]*)\n(IDA_(\d+|\?))', full_analysis_eda_summary_removed, re.MULTILINE)
	for full_analysis_pol_match in full_analysis_pol_matches:
		regex = full_analysis_pol_match[1]
		result = full_analysis_pol_match[2]
		degree = full_analysis_pol_match[3]
		if degree in full_analysis_eda_ida_degrees:
			full_analysis_eda_ida_degrees[degree] = full_analysis_eda_ida_degrees[degree] + 1
		else:
			full_analysis_eda_ida_degrees[degree] = 1
		full_analysis_eda_pol.append(regex)
	# These regexes which had eda in the simple analysis, which have lin matching time according to full analysis
	full_analysis_lin_matches = re.findall(r'(\d+): ([^\n]*)\n(NO IDA)', full_analysis_eda_summary_removed, re.MULTILINE)
	for full_analysis_lin_match in full_analysis_lin_matches:
		regex = full_analysis_lin_match[1]
		result = full_analysis_lin_match[2]
		full_analysis_eda_lin.append(regex)
	full_analysis_num_eda_exp = len(full_analysis_eda_exp)
	full_analysis_num_eda_pol = len(full_analysis_eda_pol)
	full_analysis_num_eda_lin = len(full_analysis_eda_lin)
	
	full_analysis_eda_skipped_matches = re.findall(r'(\d+): ([^\n]*\nSKIPPED)', full_analysis_eda_summary_removed, re.MULTILINE)
	full_analysis_eda_num_skipped = len(full_analysis_eda_skipped_matches)

	full_analysis_eda_timeout_in_eda_matches = re.findall(r'(\d+): ([^\n]*\nTIMEOUT in EDA)', full_analysis_eda_summary_removed, re.MULTILINE)
	full_analysis_eda_num_timeout_in_eda = len(full_analysis_eda_timeout_in_eda_matches)
	
	full_analysis_eda_timeout_in_ida_matches = re.findall(r'(\d+): ([^\n]*\nTIMEOUT in IDA)', full_analysis_eda_summary_removed, re.MULTILINE)
	full_analysis_eda_num_timeout_in_ida = len(full_analysis_eda_timeout_in_ida_matches)
	

	# These regexes which had ida in the simple analysis, which have exp matching time according to full analysis
	full_analysis_ida_summary_removed = re.sub(re.compile(r'Construction: .*?Total running time: \d*.*', re.DOTALL), '', full_analysis_ida_results)
	ida_exploit_string_test_num_vuln = 0
	ida_exploit_string_test_num_not_vuln = 0
	ida_exploit_string_test_num_timeout = 0
	full_analysis_exp_matches = re.findall(r'(\d+): ([^\n]*)\n(EDA) ([^\n]*)', full_analysis_ida_summary_removed, re.MULTILINE)
	for full_analysis_exp_match in full_analysis_exp_matches:
		regex = full_analysis_exp_match[1]
		result = full_analysis_exp_match[2]
		exploit_string_test_result = full_analysis_exp_match[3]
		if exploit_string_test_result == 'MATCHER_CONFIRMED_EXP_TIME':
			ida_exploit_string_test_num_vuln += 1
		elif exploit_string_test_result == 'MATCHER_DID_NOT_DISPLAY_EXP_TIME':
			ida_exploit_string_test_num_not_vuln += 1
		elif exploit_string_test_result == 'NO_EXPLOIT_STRING_CONSTRUCTED':
			ida_exploit_string_test_num_timeout += 1
		else:
			raise ValueError('Unknown exploit string test result: ' + exploit_string_test_result)
		full_analysis_ida_exp.append(regex)
	# These regexes which had ida in the simple analysis, which have pol matching time according to full analysis
	full_analysis_ida_ida_degrees = {}
	full_analysis_pol_matches = re.findall(r'(\d+): ([^\n]*)\n(IDA_(\d+|\?))', full_analysis_ida_summary_removed, re.MULTILINE)
	for full_analysis_pol_match in full_analysis_pol_matches:
		regex = full_analysis_pol_match[1]
		result = full_analysis_pol_match[2]
		degree = full_analysis_pol_match[3]
		if degree in full_analysis_ida_ida_degrees:
			full_analysis_ida_ida_degrees[degree] = full_analysis_ida_ida_degrees[degree] + 1
		else:
			full_analysis_ida_ida_degrees[degree] = 1
		full_analysis_ida_pol.append(regex)
	# These regexes which had ida in the simple analysis, which have lin matching time according to full analysis
	full_analysis_lin_matches = re.findall(r'(\d+): ([^\n]*)\n(NO IDA)', full_analysis_ida_summary_removed, re.MULTILINE)
	for full_analysis_lin_match in full_analysis_lin_matches:
		regex = full_analysis_lin_match[1]
		result = full_analysis_lin_match[2]
		full_analysis_ida_lin.append(regex)
	full_analysis_num_ida_exp = len(full_analysis_ida_exp)
	full_analysis_num_ida_pol = len(full_analysis_ida_pol)
	full_analysis_num_ida_lin = len(full_analysis_ida_lin)

	full_analysis_ida_skipped_matches = re.findall(r'(\d+): ([^\n]*\nSKIPPED)', full_analysis_ida_summary_removed, re.MULTILINE)
	full_analysis_ida_num_skipped = len(full_analysis_ida_skipped_matches)

	full_analysis_ida_timeout_in_eda_matches = re.findall(r'(\d+): ([^\n]*\nTIMEOUT in EDA)', full_analysis_ida_summary_removed, re.MULTILINE)
	full_analysis_ida_num_timeout_in_eda = len(full_analysis_ida_timeout_in_eda_matches)
	
	full_analysis_ida_timeout_in_ida_matches = re.findall(r'(\d+): ([^\n]*\nTIMEOUT in IDA)', full_analysis_ida_summary_removed, re.MULTILINE)
	full_analysis_ida_num_timeout_in_ida = len(full_analysis_ida_timeout_in_ida_matches)


	# These regexes which had fda in the simple analysis, which have exp matching time according to full analysis
	full_analysis_fda_summary_removed = re.sub(re.compile(r'Construction: .*?Total running time: \d*.*', re.DOTALL), '', full_analysis_fda_results)
	fda_exploit_string_test_num_vuln = 0
	fda_exploit_string_test_num_not_vuln = 0
	fda_exploit_string_test_num_timeout = 0
	full_analysis_exp_matches = re.findall(r'(\d+): ([^\n]*)\n(EDA) ([^\n]*)', full_analysis_fda_summary_removed, re.MULTILINE)
	for full_analysis_exp_match in full_analysis_exp_matches:
		regex = full_analysis_exp_match[1]
		result = full_analysis_exp_match[2]
		exploit_string_test_result = full_analysis_exp_match[3]
		if exploit_string_test_result == 'MATCHER_CONFIRMED_EXP_TIME':
			fda_exploit_string_test_num_vuln += 1
		elif exploit_string_test_result == 'MATCHER_DID_NOT_DISPLAY_EXP_TIME':
			fda_exploit_string_test_num_not_vuln += 1
		elif exploit_string_test_result == 'NO_EXPLOIT_STRING_CONSTRUCTED':
			fda_exploit_string_test_num_timeout += 1
		else:
			raise ValueError('Unknown exploit string test result: ' + exploit_string_test_result)
		full_analysis_fda_exp.append(regex)
	# These regexes which had fda in the simple analysis, which have pol matching time according to full analysis
	full_analysis_fda_ida_degrees = {}
	full_analysis_pol_matches = re.findall(r'(\d+): ([^\n]*)\n(IDA_(\d+|\?))', full_analysis_fda_summary_removed, re.MULTILINE)
	for full_analysis_pol_match in full_analysis_pol_matches:
		regex = full_analysis_pol_match[1]
		result = full_analysis_pol_match[2]
		degree = full_analysis_pol_match[3]
		if degree in full_analysis_fda_ida_degrees:
			full_analysis_fda_ida_degrees[degree] = full_analysis_fda_ida_degrees[degree] + 1
		else:
			full_analysis_fda_ida_degrees[degree] = 1
		full_analysis_fda_pol.append(regex)
	# These regexes which had fda in the simple analysis, which have lin matching time according to full analysis
	full_analysis_lin_matches = re.findall(r'(\d+): ([^\n]*)\n(NO IDA)', full_analysis_fda_summary_removed, re.MULTILINE)
	for full_analysis_lin_match in full_analysis_lin_matches:
		regex = full_analysis_lin_match[1]
		result = full_analysis_lin_match[2]
		full_analysis_fda_lin.append(regex)
	full_analysis_num_fda_exp = len(full_analysis_fda_exp)
	full_analysis_num_fda_pol = len(full_analysis_fda_pol)
	full_analysis_num_fda_lin = len(full_analysis_fda_lin)	

	full_analysis_fda_skipped_matches = re.findall(r'(\d+): ([^\n]*\nSKIPPED)', full_analysis_fda_summary_removed, re.MULTILINE)
	full_analysis_fda_num_skipped = len(full_analysis_fda_skipped_matches)

	full_analysis_fda_timeout_in_eda_matches = re.findall(r'(\d+): ([^\n]*\nTIMEOUT in EDA)', full_analysis_fda_summary_removed, re.MULTILINE)
	full_analysis_fda_num_timeout_in_eda = len(full_analysis_fda_timeout_in_eda_matches)
	
	full_analysis_fda_timeout_in_ida_matches = re.findall(r'(\d+): ([^\n]*\nTIMEOUT in IDA)', full_analysis_fda_summary_removed, re.MULTILINE)
	full_analysis_fda_num_timeout_in_ida = len(full_analysis_fda_timeout_in_ida_matches)

	summary_file.write("EDA: " + str(simple_analysis_num_eda) + '\n')
	summary_file.write("\tExponential: " + str(full_analysis_num_eda_exp) + '\n')
	if full_analysis_num_eda_exp > 0:
		summary_file.write("\t\tExploit strings (ES):" + '\n')
		summary_file.write("\t\tVulnerable:            " + str(eda_exploit_string_test_num_vuln) + '\n')
		summary_file.write("\t\tNot vulnerable:        " + str(eda_exploit_string_test_num_not_vuln) + '\n')
		summary_file.write("\t\tTO in ES construction: " + str(eda_exploit_string_test_num_timeout) + '\n')
	summary_file.write("\tPolynomial:  " + str(full_analysis_num_eda_pol) + '\n')
	if full_analysis_num_eda_pol > 0:
		sorted_full_analysis_eda_ida_degrees = sorted(full_analysis_eda_ida_degrees.items(), key=operator.itemgetter(0))
		for k, v in sorted_full_analysis_eda_ida_degrees:
			degree = k
			num_with_degree = v
			summary_file.write("\t\tWith degree " + str(degree) + ": " + str(num_with_degree) + '\n')
	summary_file.write("\tLinear:      " + str(full_analysis_num_eda_lin) + '\n')
	summary_file.write("\tSkipped:     " + str(full_analysis_eda_num_skipped) + '\n')
	summary_file.write("\tTO in EDA:   " + str(full_analysis_eda_num_timeout_in_eda) + '\n')
	summary_file.write("\tTO in IDA:   " + str(full_analysis_eda_num_timeout_in_ida) + '\n')
	summary_file.write("IDA: " + str(simple_analysis_num_ida) + '\n')
	summary_file.write("\tExponential: " + str(full_analysis_num_ida_exp) + ' (should be 0)\n')
	if full_analysis_num_ida_exp > 0:
		summary_file.write("\t\tExploit strings (ES):" + '\n')
		summary_file.write("\t\tVulnerable:            " + str(ida_exploit_string_test_num_vuln) + '\n')
		summary_file.write("\t\tNot vulnerable:        " + str(ida_exploit_string_test_num_not_vuln) + '\n')
		summary_file.write("\t\tTO in ES construction: " + str(ida_exploit_string_test_num_timeout) + '\n')
	summary_file.write("\tPolynomial:  " + str(full_analysis_num_ida_pol) + '\n')
	if full_analysis_num_ida_pol > 0:
		sorted_full_analysis_ida_ida_degrees = sorted(full_analysis_ida_ida_degrees.items(), key=operator.itemgetter(0))
		for k, v in sorted_full_analysis_ida_ida_degrees:
			degree = k
			num_with_degree = v
			summary_file.write("\t\tWith degree " + str(degree) + ": " + str(num_with_degree) + '\n')
	summary_file.write("\tLinear:      " + str(full_analysis_num_ida_lin) + '\n')
	summary_file.write("\tSkipped:     " + str(full_analysis_ida_num_skipped) + '\n')
	summary_file.write("\tTO in EDA:   " + str(full_analysis_ida_num_timeout_in_eda) + '\n')
	summary_file.write("\tTO in IDA:   " + str(full_analysis_ida_num_timeout_in_ida) + '\n')
	summary_file.write("FDA: " + str(simple_analysis_num_no_ida) + '\n')
	summary_file.write("\tExponential: " + str(full_analysis_num_fda_exp) + ' (should be 0)\n')
	if full_analysis_num_fda_exp > 0:
		summary_file.write("\t\tExploit strings (ES):" + '\n')
		summary_file.write("\t\tVulnerable:            " + str(fda_exploit_string_test_num_vuln) + '\n')
		summary_file.write("\t\tNot vulnerable:        " + str(fda_exploit_string_test_num_not_vuln) + '\n')
		summary_file.write("\t\tTO in ES construction: " + str(fda_exploit_string_test_num_timeout) + '\n')
	summary_file.write("\tPolynomial:  " + str(full_analysis_num_fda_pol) + ' (should be 0)\n')
	if full_analysis_num_fda_pol > 0:
		sorted_full_analysis_fda_ida_degrees = sorted(full_analysis_fda_ida_degrees.items(), key=operator.itemgetter(0))
		for k, v in sorted_full_analysis_fda_ida_degrees:
			degree = k
			num_with_degree = v
			summary_file.write("\t\tWith degree " + str(degree) + ": " + str(num_with_degree) + '\n')
	summary_file.write("\tLinear:      " + str(full_analysis_num_fda_lin) + '\n')
	summary_file.write("\tSkipped:     " + str(full_analysis_fda_num_skipped) + '\n')
	summary_file.write("\tTO in EDA:   " + str(full_analysis_fda_num_timeout_in_eda) + '\n')
	summary_file.write("\tTO in IDA:   " + str(full_analysis_fda_num_timeout_in_ida) + '\n')

def run_simple_analysis(test_file_name, num_test_cases):
	return run_analysis('--simple', test_file_name, 'false', 'false', 'false', num_test_cases)

def run_full_analysis(test_file_name, num_test_cases):
	return run_analysis('--full', test_file_name, 'true', 'true', 'false', num_test_cases)

def run_analysis(analysis_type, test_file_name, construct_eda_exploit_string, test_eda_exploit_string, construct_ida_exploit_string, num_test_cases):
	analysis_results = ""
	current_test_case_num = 0
	current_num_eda = 0
	current_num_ida = 0
	current_num_no_ida = 0
	current_num_skipped = 0
	current_num_to_in_eda = 0
	current_num_to_in_ida = 0
	updated = False
	proc = subprocess.Popen([JAVA, MEMORY_SETTINGS, '-cp', PATH_TO_ROOT + CLASS_PATH, 'driver.Main', analysis_type, '--construct-eda-exploit-string=' + construct_eda_exploit_string, '--test-eda-exploit-string=' + test_eda_exploit_string, '--construct-ida-exploit-string=' + construct_ida_exploit_string, '--timeout=' + timeout, '--verbose=false', '--if=' + test_file_name], stdout=subprocess.PIPE)
	while True:
		line = proc.stdout.readline()
		if line != '':
			if re.search('^\d+: ', line):
				current_test_case_num += 1
			elif re.search('^EDA', line):
				current_num_eda += 1
				updated = True
			elif re.search('^IDA_(\d+|\?)', line):
				current_num_ida += 1
				updated = True
			elif re.search('^NO IDA', line):
				current_num_no_ida += 1
				updated = True
			elif re.search('^SKIPPED', line):
				current_num_skipped += 1
				updated = True
			elif re.search('^TIMEOUT in EDA', line):
				current_num_to_in_eda += 1
				updated = True
			elif re.search('^TIMEOUT in IDA', line):
				current_num_to_in_ida += 1
				updated = True

			if updated:
				print_progress(current_test_case_num, num_test_cases, current_num_eda, current_num_ida, current_num_no_ida, current_num_skipped, current_num_to_in_eda, current_num_to_in_ida)
				updated = False

			analysis_results += line.rstrip() + '\n'
		else:
			break
	print
	print analysis_results
	return analysis_results

def print_progress(current, total, current_num_eda, current_num_ida, current_num_no_ida, current_num_skipped, current_num_to_in_eda, current_num_to_in_ida):
	print ' {0}/{1} EDA: {2} IDA: {3} NoIDA: {4} SKIP: {5} TOEDA: {6} TOIDA: {7} @ {8}\r'.format(current, total, current_num_eda, current_num_ida, current_num_no_ida, current_num_skipped, current_num_to_in_eda, current_num_to_in_ida, time.strftime(TIME_FORMAT)),
	


if __name__ == "__main__":
	main(sys.argv)
