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
The inordinately long run time is caused by catastrophic backtracking, which means the matcher has to try a large number of ways to match an input string with a regex one after the other.
For example, consider the regex `(a|a)*`, which will match any input string of the form a<sup>n</sup> (i.e. n repetitions of 'a').
However, every 'a' can be matched by either one of the two a's in the regex. Therefore, everytime n is increased by 1, the number of ways the input string can be matched with the regex, doubles.
In other words, the number of ways the input string can match the regex is exponential in the length of the input string.
Should we change the input string in such a way that it no longer matches the regex, for example we change a<sup>n</sup> to a<sup>n</sup>x, the matcher will be forced to try and match the input string in all the possible ways.
Therefore, the matching time is exponential in the length of the input string.

In this project we created a tool to perform static analysis on regexes to determine whether catastrophic backtracking could occur.
Two types of catastrophic backtracking are detected by this project, namely exponential backtracking and polynomial backtracking.
Polynomial backtracking occurs when the matching time is polynomial in the length of the input string, for example consider the regex `a*a*`, for which the matching time will be quadratic for input strings of the form a<sup>n</sup>x.
Similarly for the regex `a*a*a*`, the matching time will be cubic for input strings of the form a<sup>n</sup>x.
In general for the regex `a*...a*` for k repetitions of `a*`, the degree of the polynomial of the matching time for input strings of the form a<sup>n</sup>x, will be k.
One could then think of the regex `(a*)*` as a regex with infinite degree polynomial matching time for input strings of the form a<sup>n</sup>x.
Therefore the matching time is exponential in the length of the input string, similarly to the previous example regex `(a|a)*`.

Without going in too much detail, the analysis relies on inspecting the underlying NFA of a regex for exponential degree of ambiguity (EDA) and infinite degree of amgibuity (IDA) to determine whether the regex is vulnerable to exponential backtracking, or polynomial backtracking, respectively.
