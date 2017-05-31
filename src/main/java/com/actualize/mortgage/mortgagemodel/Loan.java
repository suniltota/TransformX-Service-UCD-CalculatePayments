package com.actualize.mortgage.mortgagemodel;

public class Loan {
	private double loanAmount;
	private int loanTerm;
	private Payment payment;
	private InterestRate interestRate;
	
	public Loan(double loanAmount, int loanTerm, Payment payment, InterestRate interestRate) {
		this.loanAmount = loanAmount;
		this.loanTerm = loanTerm;
		this.payment = payment;
		this.interestRate = interestRate;
	}

	public CashFlowResult generateCashFlows(Environment environment) {
		CashFlowResult cashFlow = new CashFlowResult(loanTerm);
		double balance = loanAmount;
		boolean balanceChange = false;
		double rate = 0;
		double pmt = 0;
		for (int i = 0; i < loanTerm; i++) {
			
			// Capture rate change
			boolean rateChange = false;
			if (interestRate.isReset(i)) {
				double newRate = interestRate.getRate(environment, i);
				if (newRate != rate) {
					rateChange = true;
					rate = newRate;
				}
			}
			
			// Recalculate payment (if needed)
			if (payment.isReset(i) || (rateChange && payment.resetsWithRateChange(i)) || (balanceChange && payment.resetsWithBalanceChange(i)))
				pmt = payment.pmt(i, balance, rate);

			// Calculate P&I
			double interest = payment.ipmt(i, pmt, balance, rate);
			double principal = payment.ppmt(i, pmt, balance, rate);

			// Store flows
			cashFlow.addValue(i, CashFlowInfo.BALANCE, balance);
			cashFlow.addValue(i, CashFlowInfo.PRINCIPAL_PAYMENT, principal);
			cashFlow.addValue(i, CashFlowInfo.INTEREST_RATE, rate);
			cashFlow.addValue(i, CashFlowInfo.INTEREST_PAYMENT, interest);
			balance -= principal;
		}
		return cashFlow;
	}
	
	public int getLoanTerm() { return loanTerm; }
}
