# RegexStaticAnalysis
A tool to perform static analysis on regexes to determine whether they are vulnerable to ReDoS.

## Installation
1. To obtain the code, clone the repository with:  
   `git clone --recursive https://github.com/NicolaasWeideman/RegexStaticAnalysis.git`  
   (the --recurive option is necessary to clone submodules as well)

2. To compile the code use either  
   `make`, or
   `make exejar`  
   depending on whether you want an executable jar to run the code, or not.  
3. There are three options for running the code:
   1. `./run.sh`
   2. `./RegexStaticAnalysis.jar` (if you created the executable jar in the compilation step)
   3. `java -cp ./bin driver.Main <command line args>` (which is basically the bash script inside run.sh)
4. To learn how to use the code, you can read the usage statement that is printed when the code is run without any command line arguments.


## Motivation
For certain regexes, some regular expression matchers are vulnerable to a phenomenon known as regular expression denial of service (ReDoS).
In general terms, ReDoS occurs whenever the matching time a regex matcher takes to decide whether an input string matches a regex is inordinately long.
The indorinately long run time is caused by catastrophic backtracking, which means the matcher has to try a large number of ways to match an input string with a regex one after the other.
For example, consider the regex `(a|a)*`.
