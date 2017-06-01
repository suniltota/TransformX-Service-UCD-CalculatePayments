package com.actualize.mortgage.ucd.calculations;

import static org.junit.Assert.*;

import org.junit.Test;

import com.actualize.mortgage.domainmodels.AdjustableInterestRate;
import com.actualize.mortgage.domainmodels.AmortizingPayment;
import com.actualize.mortgage.domainmodels.InterestRate;
import com.actualize.mortgage.domainmodels.Loan;
import com.actualize.mortgage.domainmodels.MortgageInsurance;
import com.actualize.mortgage.domainmodels.Payment;
import com.actualize.mortgage.domainmodels.PrivateMortgageInsurance;
import com.actualize.mortgage.ucd.calculatepayments.LoanCalculations;

public class LoanCalculationsTest {

	@Test
	public void test() {
		int loanTerm = 360;
		double loanAmount = 337500;
		double initialRate = 0.025;
		int firstResetMonthsCount = 60;
		int subsequentResetMonthsCount = 12;
		double firstResetCapRate = 0.02;
		double subsequentResetCapRate = 0.02;
		double lifetimeCapRate = 0.075;
		double lifetimeFloorRate = 0.0225;
		double fullyIndexedRate = 0.03;
		double loanCostsTotal = 10194.38;
		double aprIncludedCostsTotal = 8785.38;
		double prepaidInterest = 670.48;
		
		Payment payment = new AmortizingPayment(loanTerm);
		InterestRate rate = new AdjustableInterestRate(initialRate, firstResetMonthsCount, subsequentResetMonthsCount,
				firstResetCapRate, subsequentResetCapRate, lifetimeCapRate, firstResetCapRate, subsequentResetCapRate, lifetimeFloorRate);
		Loan loan = new Loan(loanAmount, loanTerm, payment, rate);
		MortgageInsurance mi = new PrivateMortgageInsurance(140, 120, .006, 240, .004, 360, .002);
		
		LoanCalculations calcs = new LoanCalculations(loan, mi, fullyIndexedRate, loanCostsTotal, aprIncludedCostsTotal, prepaidInterest);
		System.out.println(String.format("Five Year Total Costs: $%9.2f", calcs.fiveYearTotalOfPayments));
		System.out.println(String.format("Principal Paid After Five Years:  $%9.2f", calcs.fiveYearPrincipal));
		System.out.println(String.format("Total of Payments: $%9.2f", calcs.totalOfPayments));
		System.out.println(String.format("Finance Charge: $%9.2f", calcs.amountFinanced));
		System.out.println(String.format("Amount Financed: $%9.2f", calcs.financeCharge));
		System.out.println(String.format("APR: %3.3f%%", calcs.apr));
		System.out.println(String.format("TIP: %3.3f%%", calcs.totalInterestPercentage));
		
		assertTrue("Success", true);
	}
	
	public static void main(String[] args) {
		LoanCalculationsTest test = new LoanCalculationsTest();
		test.test();
	}

}
