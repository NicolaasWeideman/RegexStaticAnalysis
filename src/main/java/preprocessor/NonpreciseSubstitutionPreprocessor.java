package preprocessor;

public class NonpreciseSubstitutionPreprocessor extends ParsingPreprocessor {
	public NonpreciseSubstitutionPreprocessor() {		
		super();
		ALLOW_LOOKAROUND = true;
		DequantifierRule dr = new DequantifierRule(); /* we first need to remove the quantifying symbols */
		addRule(dr);
		EscapeSequenceExpansionRule eer = new EscapeSequenceExpansionRule();
		addRule(eer);
		WildCardExpansionRule wcer = new WildCardExpansionRule();
		addRule(wcer);
		PlusOperatorExpansion poe = new PlusOperatorExpansion();
		addRule(poe);
		QuestionMarkOperatorExpansion qmoe = new QuestionMarkOperatorExpansion();
		addRule(qmoe);
		//CountClosureOperatorExpansion ccoe = new CountClosureOperatorExpansion();
		//addRule(ccoe);
		NonpreciseCountClosureOperatorExpansion nlccoe = new NonpreciseCountClosureOperatorExpansion();
		addRule(nlccoe);
		NonpreciseLookaroundExpansion nlae = new NonpreciseLookaroundExpansion();
		addRule(nlae);
	}
}
