package test.com.actualize.mortgage.ucd.calculations;

import static org.junit.Assert.*;

import org.junit.Test;

import com.actualize.mortgage.mortgagemodel.AdjustableInterestRate;
import com.actualize.mortgage.mortgagemodel.AmortizingPayment;
import com.actualize.mortgage.mortgagemodel.CashFlow;
import com.actualize.mortgage.mortgagemodel.Environment;
import com.actualize.mortgage.mortgagemodel.InterestRate;
import com.actualize.mortgage.mortgagemodel.Loan;
import com.actualize.mortgage.mortgagemodel.Payment;

public class CashFlowTest {

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
		double rate = 0.1;
		
		Payment payment = new AmortizingPayment(loanTerm);
		InterestRate interestRate = new AdjustableInterestRate(initialRate, firstResetMonthsCount, subsequentResetMonthsCount,
				firstResetCapRate, subsequentResetCapRate, lifetimeCapRate, firstResetCapRate, subsequentResetCapRate, lifetimeFloorRate);
		Loan loan = new Loan(loanAmount, loanTerm, payment, interestRate);
		Environment environment = new Environment(rate);
		CashFlow cf = loan.generateCashFlows(environment);
		
		System.out.println(String.format("APR = %2.4f%%", 100.0*cf.apr(0)));
		System.out.println("");
		System.out.println("Period\t  Balance \t  Payment \t Principal\t Rate    \t Interest");
		System.out.println("------\t----------\t----------\t----------\t-------  \t----------");
		for (int i = 0; i < cf.getPeriods(); i++)
			System.out.println(String.format("%d\t%10.2f\t%10.2f\t%10.2f\t%2.4f%%  \t%10.2f",
					i+1, cf.getBalance(i), cf.getPayment(i), cf.getPrincipal(i), 100.0*cf.getRate(i), cf.getInterest(i)));
		
		assertTrue("Success", true);
	}

}
