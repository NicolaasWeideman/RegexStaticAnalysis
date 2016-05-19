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
