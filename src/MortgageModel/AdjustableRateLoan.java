package MortgageModel;

public class AdjustableRateLoan extends Loan {

	public AdjustableRateLoan(double loanAmount, int amortizationTerm, int loanTerm,
			double initialRate, int firstReset, int subsequentReset,
			double firstCap, double subsequentCap, double lifetimeCap,
			double firstFloor, double subsequentFloor, double lifetimeFloor) {
		super(loanAmount, loanTerm,  new AmortizingPayment(amortizationTerm),
			new AdjustableInterestRate(initialRate, firstReset, subsequentReset,
				firstCap, subsequentCap, lifetimeCap,
				firstFloor, subsequentFloor, lifetimeFloor));
	}
	
	public static void main(String[] args) {
		AdjustableRateLoan loan = new AdjustableRateLoan(100000, 360, 84, 0.05125, 60, 12, 0.02, 0.01, 0.10125, 0.02, 0.01, 0.0125);
		CashFlow cashFlow = loan.generateCashFlows(new Environment(0.05125));
		cashFlow.print();
	}

}
