package com.actualize.mortgage.ucd.calculations;

import static org.junit.Assert.*;

import org.junit.Test;

import com.actualize.mortgage.mortgagemodel.AdjustableInterestRate;
import com.actualize.mortgage.mortgagemodel.AmortizingPayment;
import com.actualize.mortgage.mortgagemodel.Environment;
import com.actualize.mortgage.mortgagemodel.InterestRate;
import com.actualize.mortgage.mortgagemodel.Loan;
import com.actualize.mortgage.mortgagemodel.MortgageInsurance;
import com.actualize.mortgage.mortgagemodel.Payment;
import com.actualize.mortgage.mortgagemodel.PrivateMortgageInsurance;
import com.actualize.mortgage.ucd.calculations.ProjectedPayments;

public class ProjectedPaymentsTest {

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
		
		Payment payment = new AmortizingPayment(loanTerm);
		InterestRate rate = new AdjustableInterestRate(initialRate, firstResetMonthsCount, subsequentResetMonthsCount,
				firstResetCapRate, subsequentResetCapRate, lifetimeCapRate, firstResetCapRate, subsequentResetCapRate, lifetimeFloorRate);
		Loan loan = new Loan(loanAmount, loanTerm, payment, rate);
		Environment fullyIndexedEnv = new Environment(fullyIndexedRate);
		MortgageInsurance mi = new PrivateMortgageInsurance(140, 120, .006, 240, .004, 360, .002);
		
		ProjectedPayments payments = new ProjectedPayments(fullyIndexedEnv, new Environment(lifetimeFloorRate), new Environment(lifetimeCapRate), loan, mi);
		System.out.println(String.format("Max interest rate starting month: %d", payments.getMaxRateFirstMonth()));
		System.out.println(String.format("Max interest rate: %2.3f%%", 100*payments.getMaxRate()));
		System.out.println(String.format("Max principal and interest starting month: %d", payments.getMaxPIFirstMonth()));
		System.out.println(String.format("Max principal and interest: $%6.2f", payments.getMaxPI()));
		for (int i = 0; i < payments.getProjectedPayments().length; i++)
			System.out.println(String.format("%d\t%d\t%d\t%9.2f\t%9.2f\t%9.2f\t", i+1,
					payments.getProjectedPayments()[i].getStartYear(), payments.getProjectedPayments()[i].getEndYear(),
					payments.getProjectedPayments()[i].getLowPI(), payments.getProjectedPayments()[i].getHighPI(), payments.getProjectedPayments()[i].getMI()));
	
		
		assertTrue("Success", true);
	}
	
	public static void main(String[] args) {
		ProjectedPaymentsTest test = new ProjectedPaymentsTest();
		test.test();
	}

}
