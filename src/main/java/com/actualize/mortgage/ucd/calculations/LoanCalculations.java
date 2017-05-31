package com.actualize.mortgage.ucd.calculations;

import com.actualize.mortgage.mortgagemodel.CashFlowInfo;
import com.actualize.mortgage.mortgagemodel.CashFlowResult;
import com.actualize.mortgage.mortgagemodel.Environment;
import com.actualize.mortgage.mortgagemodel.Loan;
import com.actualize.mortgage.mortgagemodel.MortgageInsurance;

public class LoanCalculations {
	public final double fiveYearTotalOfPayments;
	public final double fiveYearPrincipal;
	public final double totalOfPayments;
	public final double amountFinanced;
	public final double financeCharge;
	public final double apr;
	public final double totalInterestPercentage;
	
	static public double calculateFiveYearPrincipal(CashFlowResult cashFlow) {
		double value = cashFlow.getValue(0, CashFlowInfo.BALANCE);
		if (cashFlow.length > 60)
			value -= cashFlow.getValue(60, CashFlowInfo.BALANCE);
		return value;
	}

	static public double totalOfPayments(double upfrontCosts, CashFlowResult cashFlow, int horizon) {
		double value = upfrontCosts; // add loan costs plus prepaid interest
		for (int i = 0; i < cashFlow.length && i < horizon; i++)
			value += cashFlow.getValue(i, CashFlowInfo.TOTAL_PAYMENT);
		return value;
	}

	static public double amountFinanced(double upfrontCosts, CashFlowResult cashFlow) {
		double value = -upfrontCosts; // subtract out apr included costs and prepaid interest
		return cashFlow.getValue(0, CashFlowInfo.BALANCE) + value;
	}
	
	static public double financeCharge(double upfrontCosts, CashFlowResult cashFlow) {
		double value = upfrontCosts; // add apr included costs and prepaid interest
		for (int i = 0; i < cashFlow.length; i++)
			value += cashFlow.getValue(i, CashFlowInfo.INTEREST_PAYMENT, CashFlowInfo.MORTGAGE_INSURANCE_PAYMENT); // Add MI when available
		return value;
	}

	static public double calculateTotalInterestPercentage(CashFlowResult cashFlow, double prepaidInterest) {
		double value = prepaidInterest;
		for (int i = 0; i < cashFlow.length; i++)
			value += cashFlow.getValue(i, CashFlowInfo.INTEREST_PAYMENT);
		return 100*value/cashFlow.getValue(0, CashFlowInfo.BALANCE);
	}
	
	public LoanCalculations(Environment environment, double loanCosts, double aprCosts, double prepaidInterest, Loan loan, MortgageInsurance mi) {
		CashFlowResult cashFlow = loan.generateCashFlows(environment);
		mi.addMortgageInsurance(cashFlow);
		fiveYearTotalOfPayments = totalOfPayments(loanCosts + prepaidInterest, cashFlow, 60);
		fiveYearPrincipal = calculateFiveYearPrincipal(cashFlow);
		totalOfPayments = totalOfPayments(loanCosts + prepaidInterest, cashFlow, loan.getLoanTerm());
		amountFinanced = amountFinanced(aprCosts + prepaidInterest, cashFlow);
		financeCharge = financeCharge(aprCosts + prepaidInterest, cashFlow);
		apr = 100*cashFlow.apr(aprCosts);
		totalInterestPercentage = calculateTotalInterestPercentage(cashFlow, prepaidInterest);
	}

}
