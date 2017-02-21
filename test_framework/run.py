import subprocess
import re
import datetime
import os
import sys
import time

PATH_TO_ROOT = '../'
PATH_TO_TEST_RESULTS_DIR = './test_results/'

JAVA = '/usr/bin/java'
MEMORY_SETTINGS = '-Xms2048m'
CLASS_PATH = '/bin/'

PATH_TO_TESTS = PATH_TO_ROOT + 'tests/'

TIMEOUT = "10"

TIME_FORMAT = "%H:%M:%S, %A, %d %B %Y"

def main(argv):
	if len(argv) < 2:
		print "usage: python ./" + argv[0] + " test_file1 test_file2 ..."
		return
	for test_file in argv[1:]:
		print "Test file: " + test_file
		analyse_regexes_file(test_file)
		print "========================"


def analyse_regexes_file(test_file_name):
	#f = open(PATH_TO_TESTS + TEST_FILE_NAME, 'r')
	f = open(test_file_name, 'r')
	all_regexes = f.readlines()
	num_test_cases = len(all_regexes)
	time_str = "{:%H:%M:%S_%d%b%Y}".format(datetime.datetime.now())
	results_directory = PATH_TO_TEST_RESULTS_DIR + os.path.splitext(os.path.basename(test_file_name))[0] + "_" + time_str
	os.makedirs(results_directory)
	test_regexes_file = open(results_directory + '/test_regexes.txt', 'w')
	for regex in all_regexes:
		test_regexes_file.write(regex)
	test_regexes_file.close()
	f.close()
	results = ""
	current_test_case_num = 0
	proc = subprocess.Popen([JAVA, MEMORY_SETTINGS, '-cp', PATH_TO_ROOT + CLASS_PATH, 'driver.Main', '--simple', '--testexploitstring=false', '--timeout=' + TIMEOUT, '--verbose=false', '-i', test_file_name], stdout=subprocess.PIPE)
	while True:
		line = proc.stdout.readline()
		if line != '':
			if re.search('^\d+: ', line):
				current_test_case_num += 1
				print ' {0}/{1} @ {2}\r'.format(current_test_case_num, num_test_cases, time.strftime(TIME_FORMAT)),
			results += line.rstrip() + '\n'
		else:
			break
	print
	print results
	simple_analysis_results_file = open(results_directory + '/simple_analysis_results.txt', 'w')
	simple_analysis_results_file.write(results)
	simple_analysis_results_file.close()
	# Remove the summary at the end of the results
	summary_removed = re.sub(re.compile(r'Construction: .*Total running time: \d*.*', re.DOTALL), '', results)
	# Filter out regexes which where skipped.
	#skipped_removed = re.sub(re.compile(r'^\d+: [^\n]*\nSKIPPED\n', re.MULTILINE), '', summary_removed)
	# Filter out regexes which timed out.
	#timeout_removed = re.sub(re.compile(r'^\d+: [^\n]*\nTIMEOUT in (EDA|IDA)\n', re.MULTILINE), '', skipped_removed)
	# Filter out regexes for which the simple analysis indicated EDA or IDA
	#high_degree_ambiguous_regexes = re.sub(re.compile(r'^\d+: [^\n]*\nNO IDA\n', re.MULTILINE), '', timeout_removed)
	#problem_regexes_only = (re.sub(re.compile(r'^\d+: ([^\n]*)\n(EDA|IDA_\d+)', re.MULTILINE), r'\1', high_degree_ambiguous_regexes)).rstrip()
	potential_evil_regexes = open(results_directory + '/simple_analysis_potentially_evil_regexes.txt', 'w')
	print "Problem regexes:"
	problem_regexes_matches = re.findall(r'^\d+: ([^\n]*)\n(EDA|IDA_(\d+|\?))', summary_removed,re.MULTILINE)
	for problem_regex_match in problem_regexes_matches:
		problem_regex = problem_regex_match[0]
		potential_evil_regexes.write(problem_regex + "\n")
		print problem_regex
	#potential_evil_regexes.write(problem_regexes_only)
	potential_evil_regexes.close()	
	print "End"

	full_analysis_results = ""
	current_test_case_num = 0
	# Send the problem regexes through the full analysis
	proc = subprocess.Popen([JAVA, MEMORY_SETTINGS, '-cp', PATH_TO_ROOT + CLASS_PATH, 'driver.Main', '--full', '--testexploitstring=true', '--timeout=' + TIMEOUT, '--verbose=false', '-i', results_directory + '/simple_analysis_potentially_evil_regexes.txt'], stdout=subprocess.PIPE)
	while True:
		line = proc.stdout.readline()
		if line != '':
			if re.search('^\d+: ', line):
				current_test_case_num += 1
				print ' {0}/{1} @ {2}\r'.format(current_test_case_num, num_test_cases, time.strftime(TIME_FORMAT)),
			full_analysis_results += line.rstrip() + '\n'
		else:
			break
	print
	print full_analysis_results
	full_analysis_results_file = open(results_directory + '/full_analysis_results.txt', 'w')
	full_analysis_results_file.write(full_analysis_results)
	full_analysis_results_file.close()


	
	


if __name__ == "__main__":
	main(sys.argv)
