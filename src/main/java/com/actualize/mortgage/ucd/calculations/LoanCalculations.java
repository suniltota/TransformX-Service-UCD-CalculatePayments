package Calculations;

import MortgageModel.AdjustableInterestRate;
import MortgageModel.AmortizingPayment;
import MortgageModel.CashFlow;
import MortgageModel.Environment;
import MortgageModel.InterestRate;
import MortgageModel.Loan;
import MortgageModel.Payment;

public class LoanCalculations {
	private double fiveYearTotalOfPayments;
	private double fiveYearPrincipal;
	private double totalOfPayments;
	private double amountFinanced;
	private double financeCharge;
	private double apr;
	private double totalInterestPercentage;
	
	static public double calculateFiveYearPrincipal(CashFlow cashFlow) {
		double value = cashFlow.getBalance(0);
		if (cashFlow.getPeriods() > 60)
			value -= cashFlow.getBalance(60);
		return value;
	}

	static public double totalOfPayments(double upfrontCosts, CashFlow cashFlow, int horizon) {
		double value = upfrontCosts; // add loan costs plus prepaid interest
		for (int i = 0; i < cashFlow.getPeriods() && i < horizon; i++)
			value += cashFlow.getPayment(i);
		return value;
	}

	static public double amountFinanced(double upfrontCosts, CashFlow cashFlow) {
		double value = -upfrontCosts; // subtract out apr included costs and prepaid interest
		return cashFlow.getBalance(0) + value;
	}
	
	static public double financeCharge(double upfrontCosts, CashFlow cashFlow) {
		double value = upfrontCosts; // add apr included costs and prepaid interest
		for (int i = 0; i < cashFlow.getPeriods(); i++)
			value += cashFlow.getInterest(i); // Add MI when available
		return value;
	}

	static public double calculateTotalInterestPercentage(CashFlow cashFlow, double prepaidInterest) {
		double value = prepaidInterest;
		for (int i = 0; i < cashFlow.getPeriods(); i++)
			value += cashFlow.getInterest(i);
		return 100*value/cashFlow.getBalance(0);
	}
	
	public LoanCalculations(Environment environment, double loanCosts, double aprCosts, double prepaidInterest, Loan loan) {
		CashFlow cashFlow = loan.generateCashFlows(environment);
		fiveYearTotalOfPayments = totalOfPayments(loanCosts + prepaidInterest, cashFlow, 60);
		fiveYearPrincipal = calculateFiveYearPrincipal(cashFlow);
		totalOfPayments = totalOfPayments(loanCosts + prepaidInterest, cashFlow, loan.getLoanTerm());
		amountFinanced = amountFinanced(aprCosts + prepaidInterest, cashFlow);
		financeCharge = financeCharge(aprCosts + prepaidInterest, cashFlow);
		apr = 100*cashFlow.apr(aprCosts);
		totalInterestPercentage = calculateTotalInterestPercentage(cashFlow, prepaidInterest);
	}
	
	public double getFiveYearTotal() { return fiveYearTotalOfPayments; }
	public double getFiveYearPrincipal() { return fiveYearPrincipal; }
	public double getTotalOfPayments() { return totalOfPayments; }
	public double getAmountFinanced() { return amountFinanced; }
	public double getFinanceCharge() { return financeCharge; }
	public double getApr() { return apr; }
	public double getTotalInterestPercentage() { return totalInterestPercentage; }
	
	public void print() {
		System.out.println(String.format("Five Year Total Costs: $%9.2f", fiveYearTotalOfPayments));
		System.out.println(String.format("Principal Paid After Five Years:  $%9.2f", fiveYearPrincipal));
		System.out.println(String.format("Total of Payments: $%9.2f", totalOfPayments));
		System.out.println(String.format("Finance Charge: $%9.2f", financeCharge));
		System.out.println(String.format("Amount Financed: $%9.2f", amountFinanced));
		System.out.println(String.format("APR: %3.3f%%", apr));
		System.out.println(String.format("TIP: %3.3f%%", totalInterestPercentage));
	}
		
	public static void main(String[] args) {
		Payment payment = new AmortizingPayment(360);
		InterestRate rate = new AdjustableInterestRate(0.025, 60, 12, 0.02, 0.02, 0.075, 0.02, 0.02, 0.0225);
		Loan loan = new Loan(337500, 360, payment, rate);
		Environment fullyIndexedRate = new Environment(0.03);
		LoanCalculations calcs = new LoanCalculations(fullyIndexedRate, 10194.38, 8785.38, 670.48, loan);
		calcs.print();
		ProjectedPayments projectedPayments = new ProjectedPayments(fullyIndexedRate, new Environment(0.0225), new Environment(0.075), loan);
		projectedPayments.print();
	}

}
