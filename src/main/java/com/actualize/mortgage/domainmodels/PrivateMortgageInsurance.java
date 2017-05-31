package com.actualize.mortgage.domainmodels;

import com.actualize.mortgage.ucd.calculationutils.Functions;

public class PrivateMortgageInsurance extends MortgageInsurance {
	private final int endPeriod;
	private final double homeValue;
	private final int firstTerm;
	private final double firstFactor;
	private final int secondTerm;
	private final double secondFactor;
	private final int thirdTerm;
	private final double thirdFactor;
	
	public PrivateMortgageInsurance(int endPeriod, int firstTerm, double firstFactor, int secondTerm, double secondFactor, int thirdTerm, double thirdFactor) {
		this.endPeriod = endPeriod;
		this.homeValue = 0;
		this.firstTerm = firstTerm;
		this.firstFactor = firstFactor;
		this.secondTerm = secondTerm;
		this.secondFactor = secondFactor;
		this.thirdTerm = thirdTerm;
		this.thirdFactor = thirdFactor;		
	}

	public PrivateMortgageInsurance(double homeValue, int firstTerm, double firstFactor, int secondTerm, double secondFactor, int thirdTerm, double thirdFactor) {
		this.endPeriod = 0;
		this.homeValue = homeValue;
		this.firstTerm = firstTerm;
		this.firstFactor = firstFactor;
		this.secondTerm = secondTerm;
		this.secondFactor = secondFactor;
		this.thirdTerm = thirdTerm;
		this.thirdFactor = thirdFactor;		
	}

	@Override
	public CashFlowResult addMortgageInsurance(CashFlowResult baseCashFlow) {
		if (homeValue > 0)
			addMortgageInsuraceToLTV(baseCashFlow);
		else
			addMortgageInsuraceToPeriod(baseCashFlow);
		return baseCashFlow;
	}
	
	private CashFlowResult addMortgageInsuraceToLTV(CashFlowResult baseCashFlow) {
		double loanAmount = baseCashFlow.getValue(0, CashFlowInfo.BALANCE);
		for (int i = 0; i < baseCashFlow.length; i++) {
			if (baseCashFlow.getValue(0, CashFlowInfo.BALANCE) / homeValue < 0.8)
				break;
			baseCashFlow.addValue(i, CashFlowInfo.MORTGAGE_INSURANCE_PAYMENT, getFactor(i) * loanAmount);
		}
		return baseCashFlow;
	}
	
	private CashFlowResult addMortgageInsuraceToPeriod(CashFlowResult baseCashFlow) {
		double loanAmount = baseCashFlow.getValue(0, CashFlowInfo.BALANCE);
		for (int i = 0; i < endPeriod && i < baseCashFlow.length; i++)
			baseCashFlow.addValue(i, CashFlowInfo.MORTGAGE_INSURANCE_PAYMENT, Functions.round(getFactor(i) * loanAmount / 12.0));
		return baseCashFlow;
	}

	private double getFactor(int period) {
		if (period < firstTerm)
			return firstFactor;
		if (period < secondTerm)
			return secondFactor;
		if (period < thirdTerm)
			return thirdFactor;
		return 0;
	}
}
