# RegexStaticAnalysis
A tool to perform static analysis on regexes to determine whether they are vulnerable to ReDoS.

## Installation
1. To obtain the code, clone the repository with:  
   `git clone --recursive https://github.com/NicolaasWeideman/RegexStaticAnalysis.git`  
   (the --recursive option is necessary to clone submodules as well)

2. To compile the code use either  
   `make`, or
   `make exejar`  
   depending on whether you want an executable jar to run the code, or not.  
3. There are three options for running the code:
   1. `./run.sh <command line args>`
   2. `./RegexStaticAnalysis.jar <command line args>` (if you created the executable jar in the compilation step)
   3. `java -cp ./bin driver.Main <command line args>` (which is basically the bash script inside run.sh)
4. To learn how to use the code, you can read the usage statement that is printed when the code is run without any command line arguments.

## Utilities
### Java Pumper
The Java pumper allows you to test the matching time of input strings for regular expressions.
You can run the Java pumper with `./pumper.sh`.
In the command line arguments, you supply the regex, the prefix/first pump separator, the first pump, the second pump separator, the second pump and so on.  
Example:  
`./pumper.sh 'ab*b*cd*d*' 'a' 'b' 'c' 'd' 'e'`  
will test the matching time of the regex `ab*b*cd*d*` with the input strings:  
* 'abcde', then
*  'abbcdde', then
*  'abbbcddde' and so forth  
It will print the iteration, length of the input string and matching time with each iteration.
This regex will have quadratic matching time.


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

## Noteworthy Examples
What follows are some examples of regexes and their analysis output to aid us in improving our understanding of vulnerable regular expressions. To conserve space we will only explain the necessary output of each analysis.
* `(a|a)*.*`
   * `./run.sh -c '(a|a)*.*'`
   * Result: Does not contain EDA, or IDA.
   * Explanation: We see the similarities between the regexes `(a|a)*`, which was vulnerable to exponential backtracking and `(a|a)*.*` which is not vulnerable at all. Note the `.*` at the end of the nonvulnerable regular expression. This will consume any suffix of an input string starting with a<sup>n</sup> and therefore the matcher will never backtrack to attempt all possible ways of matching the input string with the regex.
* `(.*|(a|a)*)`
   * `./run.sh -c '(.*|(a|a)*)'`
   * Result: Does not contain EDA, or IDA.
   * Explanation: Similar to the regex `(a|a)*.*` and since `.*` matches all possible input, it is not possible to construct a suffix for this regex that will force the matcher to try all possible ways of matching the input string with the regex. In fact, this regex will match any input string.
* `((a|a)*|.*)`
   * `./run.sh -c '((a|a)*|.*)'`
   * Result: Vulnerable to exponential backtracking.
   * Explanation: Since the regexes `((a|a)*|.*)` and `(.*|(a|a)*)` look basically equivalent, one would be tempted to think that they will exhibit exactly the same matching time behaviour. This is, however, not the case. With a regex of the form `(R|S)`, the matcher first attempts to match the input string with the subexpression left of the '|' operator and then the subexpression on the right hand side. Keeping this in mind one can see that for the regex `(.*|(a|a)*)` the matcher will almost immediately accept any input string with the subexpression `.*`, but for the regex `((a|a)*|.*)` the matcher will first attempt to match the input string with the subexpression `(a|a)*` and therefore, if the input string is of the form a<sup>n</sup>x, the matching time will be exponential in n, eventhough the matcher will accept the input string eventually when attempting to match it with the `.*` subexpression, after all possible attempts to match it with `(a|a)*` have failed.
   
   ## Publications
   Some research papers on the theory behind this project: 
   * [Analyzing Catastrophic Backtracking Behavior in Practical Regular Expression Matching](https://arxiv.org/abs/1405.5599)
   * [Analyzing Matching Time Behavior of Backtracking Regular Expression Matchers by Using Ambiguity of NFA](https://link.springer.com/chapter/10.1007/978-3-319-40946-7_27)
   * [Turning Evil Regexes Harmless](https://dl.acm.org/citation.cfm?id=3129440)
   * [Static Analysis of Regular Expressions](http://hdl.handle.net/10019.1/102879)
