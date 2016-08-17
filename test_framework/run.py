import subprocess
import re
import datetime
import os

PATH_TO_ROOT = '../'
PATH_TO_TEST_RESULTS_DIR = './test_results/'

JAVA = '/usr/bin/java'
MEMORY_SETTINGS = '-Xms2048m'
CLASS_PATH = '/bin/'

PATH_TO_TESTS = PATH_TO_ROOT + 'tests/'
TEST_FILE_NAME = 'small.txt'

def main():
	f = open(PATH_TO_TESTS + TEST_FILE_NAME, 'r')
	all_regexes = f.readlines()
	num_test_cases = len(all_regexes)
	time_str = "{:%H:%M:%S_%d%b%Y}".format(datetime.datetime.now())
	results_directory = PATH_TO_TEST_RESULTS_DIR + os.path.splitext(TEST_FILE_NAME)[0] + "_" + time_str
	os.makedirs(results_directory)
	test_regexes_file = open(results_directory + '/test_regexes.txt', 'w')
	for regex in all_regexes:
		test_regexes_file.write(regex)
	test_regexes_file.close()
	f.close()
	results = ""
	current_test_case_num = 0
	proc = subprocess.Popen([JAVA, MEMORY_SETTINGS, '-cp', PATH_TO_ROOT + CLASS_PATH, 'driver.Main', '--simple', '--testexploitstring=false', '--verbose=false', '-i', PATH_TO_TESTS + TEST_FILE_NAME], stdout=subprocess.PIPE)
	while True:
		line = proc.stdout.readline()
		if line != '':
			if re.search('^\d+: ', line):
				current_test_case_num += 1
				print ' {0}/{1}\r'.format(current_test_case_num, num_test_cases),
			results += line.rstrip() + '\n'
		else:
			break
	print results
	simple_analysis_results_file = open(results_directory + '/simple_analysis_results.txt', 'w')
	simple_analysis_results_file.write(results)
	simple_analysis_results_file.close()
	# Remove the summary at the end of the results
	summary_removed = re.sub(re.compile(r'Construction: .*Total running time: \d*.*', re.DOTALL), '', results)
	# Filter out regexes for which the simple analysis indicated EDA or IDA
	high_degree_ambiguous_regexes = re.sub(re.compile(r'^\d+: [^\n]*\nNO IDA\n', re.MULTILINE), '', summary_removed)
	problem_regexes_only = (re.sub(re.compile(r'^\d+: ([^\n]*)\n(EDA|IDA_\d+)', re.MULTILINE), r'\1', high_degree_ambiguous_regexes)).rstrip()
	potential_evil_regexes = open(results_directory + '/simple_analysis_potentially_evil_regexes.txt', 'w')
	potential_evil_regexes.write(problem_regexes_only)
	potential_evil_regexes.close()
	print "Problem regexes:"
	print problem_regexes_only.rstrip()
	print "End"

	full_analysis_results = ""
	current_test_case_num = 0
	# Send the problem regexes through the full analysis
	proc = subprocess.Popen([JAVA, MEMORY_SETTINGS, '-cp', PATH_TO_ROOT + CLASS_PATH, 'driver.Main', '--full', '--testexploitstring=true', '--verbose=false', '-i', results_directory + '/simple_analysis_potentially_evil_regexes.txt'], stdout=subprocess.PIPE)
	while True:
		line = proc.stdout.readline()
		if line != '':
			if re.search('^\d+: ', line):
				current_test_case_num += 1
				print ' {0}/{1}\r'.format(current_test_case_num, num_test_cases),
			full_analysis_results += line.rstrip() + '\n'
		else:
			break
	print full_analysis_results
	full_analysis_results_file = open(results_directory + '/full_analysis_results.txt', 'w')
	full_analysis_results_file.write(full_analysis_results)
	full_analysis_results_file.close()


	
	


if __name__ == "__main__":
	main()
