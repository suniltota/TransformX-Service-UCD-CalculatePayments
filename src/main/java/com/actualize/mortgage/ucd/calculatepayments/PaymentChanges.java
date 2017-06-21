package com.actualize.mortgage.ucd.calculatepayments;

import com.actualize.mortgage.domainmodels.CashFlowInfo;
import com.actualize.mortgage.domainmodels.CashFlowResult;
import com.actualize.mortgage.domainmodels.Environment;
import com.actualize.mortgage.domainmodels.Loan;

public class PaymentChanges {
	public final int maxPIFirstMonth;
	public final double maxPI;
	public final int maxRateFirstMonth;
	public final double maxRate;
	public final int firstChangeMonth;

	public PaymentChanges(Loan loan) {
		Environment highEnv = new Environment(loan.interestRate.getMaxRate()); 
		CashFlowResult high = loan.generateCashFlows(highEnv);

		int maxPIFirstMonth = 0;
		double maxPI = -1;
		int maxRateFirstMonth = 0;
		double maxRate = -1;
		double firstPI = 0;
		int firstChangeMonth = 0;
		for (int i = 0; i < high.length-1; i++) {
			double pi = high.getValue(i, CashFlowInfo.PRINCIPAL_AND_INTEREST_PAYMENT);
			if (pi > maxPI) {
				maxPIFirstMonth = i;
				maxPI = pi;
			}
			double rate = high.getValue(i, CashFlowInfo.INTEREST_RATE);
			if (rate > maxRate) {
				maxRateFirstMonth = i;
				maxRate = rate;
			}
			if (i == 0)
				firstPI = pi;
			else if (firstChangeMonth == 0 && firstPI != pi)
				firstChangeMonth = i;
		}

		this.firstChangeMonth = firstChangeMonth;
		this.maxRateFirstMonth = maxRateFirstMonth;
		this.maxRate = maxRate;
		this.maxPIFirstMonth = maxPIFirstMonth;
		this.maxPI = maxPI;
	}
}
