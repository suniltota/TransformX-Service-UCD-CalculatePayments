package MortgageModel;

public class PrivateMortgageInsurance extends MortgageInsurance {
	int firstTerm = 0;
	double firstFactor = 0;
	int secondTerm = 0;
	double secondFactor = 0;
	int thirdTerm = 0;
	double thirdFactor = 0;
	
	public PrivateMortgageInsurance(int firstTerm, double firstFactor, int secondTerm, double secondFactor, int thirdTerm, double thirdFactor) {
		this.firstTerm = firstTerm;
		this.firstFactor = firstFactor;
		this.secondTerm = secondTerm;
		this.secondFactor = secondFactor;
		this.thirdTerm = thirdTerm;
		this.thirdFactor = thirdFactor;		
	}
	
	@Override
	CashFlow addMortgageInsurance(CashFlow baseCashFlow) {
		CashFlow cashFlow = new CashFlow(baseCashFlow.getPeriods());
		for (int i = 0; i < baseCashFlow.getPeriods(); i++) {
			// TODO
		}
		return cashFlow;
	}
}
