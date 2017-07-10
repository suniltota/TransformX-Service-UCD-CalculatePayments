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
//	public final double firstChangeMaxPI;
//	public final double firstChangeMinPI;

	public PaymentChanges(Loan loan) {
		CashFlowResult high = loan.generateCashFlows(new Environment(loan.interestRate.getMaxRate()));
//		CashFlowResult low = loan.generateCashFlows(new Environment(loan.interestRate.getMinRate()));

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

		this.maxRateFirstMonth = maxRateFirstMonth;
		this.maxRate = maxRate;
		this.maxPIFirstMonth = maxPIFirstMonth;
		this.maxPI = maxPI;
		this.firstChangeMonth = firstChangeMonth;
//		this.firstChangeMaxPI = high.getValue(firstChangeMonth, CashFlowInfo.PRINCIPAL_AND_INTEREST_PAYMENT);
//		this.firstChangeMinPI = low.getValue(firstChangeMonth, CashFlowInfo.PRINCIPAL_AND_INTEREST_PAYMENT);
	}
}
