package com.actualize.mortgage.ucd.calculations;

import static org.junit.Assert.*;

import org.junit.Test;

import com.actualize.mortgage.domainmodels.AdjustableInterestRate;
import com.actualize.mortgage.domainmodels.AmortizingPayment;
import com.actualize.mortgage.domainmodels.Environment;
import com.actualize.mortgage.domainmodels.InterestRate;
import com.actualize.mortgage.domainmodels.Loan;
import com.actualize.mortgage.domainmodels.MortgageInsurance;
import com.actualize.mortgage.domainmodels.Payment;
import com.actualize.mortgage.domainmodels.PrivateMortgageInsurance;
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
		
		ProjectedPayments projected = new ProjectedPayments(fullyIndexedEnv, new Environment(lifetimeFloorRate), new Environment(lifetimeCapRate), loan, mi);
		System.out.println(String.format("Max interest rate starting month: %d", projected.maxRateFirstMonth));
		System.out.println(String.format("Max interest rate: %2.3f%%", 100*projected.maxRate));
		System.out.println(String.format("Max principal and interest starting month: %d", projected.maxPIFirstMonth));
		System.out.println(String.format("Max principal and interest: $%6.2f", projected.maxPI));
		for (int i = 0; i < projected.payments.length; i++)
			System.out.println(String.format("%d\t%d\t%d\t%9.2f\t%9.2f\t%9.2f\t", i+1,
					projected.payments[i].getStart(), projected.payments[i].getEnd(), projected.payments[i].getLowPI(),
					projected.payments[i].getHighPI(), projected.payments[i].getMi()));

		assertTrue("Success", true);
	}
	
	public static void main(String[] args) {
		ProjectedPaymentsTest test = new ProjectedPaymentsTest();
		test.test();
	}

}
