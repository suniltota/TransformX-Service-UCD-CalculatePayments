package com.actualize.mortgage.ucd.calculations;

import static org.junit.Assert.*;

import org.junit.Test;

import com.actualize.mortgage.mortgagemodel.AdjustableInterestRate;
import com.actualize.mortgage.mortgagemodel.AmortizingPayment;
import com.actualize.mortgage.mortgagemodel.Environment;
import com.actualize.mortgage.mortgagemodel.InterestRate;
import com.actualize.mortgage.mortgagemodel.Loan;
import com.actualize.mortgage.mortgagemodel.Payment;
import com.actualize.mortgage.ucd.calculations.LoanCalculations;

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
		Environment fullyIndexedEnv = new Environment(fullyIndexedRate);
		
		LoanCalculations calcs = new LoanCalculations(fullyIndexedEnv, loanCostsTotal, aprIncludedCostsTotal, prepaidInterest, loan);
		System.out.println(String.format("Five Year Total Costs: $%9.2f", calcs.getFiveYearTotal()));
		System.out.println(String.format("Principal Paid After Five Years:  $%9.2f", calcs.getFiveYearPrincipal()));
		System.out.println(String.format("Total of Payments: $%9.2f", calcs.getTotalOfPayments()));
		System.out.println(String.format("Finance Charge: $%9.2f", calcs.getAmountFinanced()));
		System.out.println(String.format("Amount Financed: $%9.2f", calcs.getFinanceCharge()));
		System.out.println(String.format("APR: %3.3f%%", calcs.getApr()));
		System.out.println(String.format("TIP: %3.3f%%", calcs.getTotalInterestPercentage()));
		
		assertTrue("Success", true);
	}
	
	public static void main(String[] args) {
		LoanCalculationsTest test = new LoanCalculationsTest();
		test.test();
	}

}
