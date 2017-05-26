package MortgageModel;
public class FixedRateLoan extends Loan {
	public FixedRateLoan(double amount, int amortizationTerm, int loanTerm, double rate) {
		super(amount, loanTerm, new AmortizingPayment(amortizationTerm), new FixedInterestRate(rate));
	}
	
	public static void main(String[] args) {
		FixedRateLoan loan = new FixedRateLoan(100000, 360, 360, 0.05125);
		CashFlow cashFlow = loan.generateCashFlows(new Environment());
		cashFlow.print();
	}
}
