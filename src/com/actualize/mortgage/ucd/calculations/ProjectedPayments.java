package Calculations;

import java.util.LinkedList;

import MortgageModel.AdjustableInterestRate;
import MortgageModel.AmortizingPayment;
import MortgageModel.CashFlow;
import MortgageModel.CompositePayment;
import MortgageModel.Environment;
import MortgageModel.InterestOnlyPayment;
import MortgageModel.InterestRate;
import MortgageModel.Loan;
import MortgageModel.Payment;
import utils.Functions;

public class ProjectedPayments {
	public class Disclosure {
		private int start;
		private int end;
		private double highPI;
		private double lowPI;
		private double mi;
		
		Disclosure(int start, int end, double lowPI, double highPI, double mi) {
			this.start = start;
			this.end = end;
			this.highPI = highPI;
			this.lowPI = lowPI;
			this.mi = mi;
		}

		void combine(Disclosure pd) {
			if (end < pd.getEnd())
				end = pd.end;
			if (highPI < pd.getHighPI())
				highPI = pd.getHighPI();
			if (lowPI > pd.getLowPI())
				lowPI = pd.getLowPI();
		}

		int getStart() { return start; }
		void setStart(int start) { this.start = start; }
		int getStartYear() { return start/12 + 1; }
		int getEnd() { return end; }
		int getEndYear() { return (end+11)/12; }
		void setEnd(int end) { this.end = end; }
		double getHighPI() { return highPI; }
		double getLowPI() { return lowPI; }
		double getMI() { return mi; }
	}
	
	private Disclosure[] payments;
	private int maxRateFirstMonth = 0;
	private double maxRate = 0;
	private int maxPIFirstMonth = 0;
	private double maxPI = 0;
	
	public ProjectedPayments(Environment baseEnv, Environment lowEnv, Environment highEnv, Loan loan) {
		LinkedList<Disclosure> disclosures = generateAllDistinctPayments(baseEnv, lowEnv, highEnv, loan);
		disclosures = combineSameStartYear(disclosures);
		disclosures = consolidateThirdPayment(disclosures);
		disclosures = fixEndYears(disclosures);
		this.payments = disclosures.toArray(new Disclosure[disclosures.size()]);
	}
	
	private LinkedList<Disclosure> generateAllDistinctPayments(Environment baseEnv, Environment lowEnv, Environment highEnv, Loan loan) {
		CashFlow base = loan.generateCashFlows(baseEnv);
		CashFlow low = loan.generateCashFlows(lowEnv);
		CashFlow high = loan.generateCashFlows(highEnv);
		int periods = base.getPeriods();
		Disclosure pd = null;
		LinkedList<Disclosure> disclosures = new LinkedList<Disclosure>();
		for (int i = 0; i < periods-1; i++) {
			double lowPI = Functions.round(low.getPrincipal(i) + low.getInterest(i));
			double highPI = Functions.round(high.getPrincipal(i) + high.getInterest(i));
			double mi = 0;
			if (pd == null || lowPI != pd.getLowPI() || highPI != pd.getHighPI() || mi != pd.getMI()) {
				if (high.getRate(i) > maxRate) {
					maxRateFirstMonth = i;
					maxRate = high.getRate(i);
				}
				if (highPI > maxPI) {
					maxPIFirstMonth = i;
					maxPI = highPI;
				}
				if (pd != null)
					pd.setEnd(i);
				pd = new Disclosure(i, periods, lowPI, highPI, mi);
				disclosures.add(pd);
			}
		}
		return disclosures;
	}
	
	LinkedList<Disclosure> combineSameStartYear(LinkedList<Disclosure> paymentDisclosures) {
		int current = 0;
		int next = 1;
		while (next < paymentDisclosures.size()) {
			Disclosure pd1 = paymentDisclosures.get(current);
			Disclosure pd2 = paymentDisclosures.get(next);
			if (pd1.getStartYear() == pd2.getStartYear()) {
				pd1.combine(pd2);
				paymentDisclosures.remove(next);
			} else {
				current++;
				next = current + 1;
			}
			
		}
		return paymentDisclosures;
	}
	
	LinkedList<Disclosure> consolidateThirdPayment(LinkedList<Disclosure> paymentDisclosures) {
		while (paymentDisclosures.size() > 4) {
			Disclosure pd1 = paymentDisclosures.get(2);
			Disclosure pd2 = paymentDisclosures.get(3);
			pd1.combine(pd2);
			paymentDisclosures.remove(3);
		}
		return paymentDisclosures;
	}
	
	LinkedList<Disclosure> fixEndYears(LinkedList<Disclosure> paymentDisclosures) {
		for (int i = 0; i < paymentDisclosures.size() - 1; i++) {
			Disclosure pd1 = paymentDisclosures.get(i);
			Disclosure pd2 = paymentDisclosures.get(i+1);
			if (pd1.getEndYear() == pd2.getStartYear()) {
				pd1.setEnd(12*(pd2.getStartYear()-1));
				pd2.setStart(12*(pd2.getStartYear()-1)+1);
				pd2.combine(pd1);
			}
		}
		return paymentDisclosures;
	}
	
	public Disclosure[] getProjectedPayments() { return payments; }
	
	public int getMaxRateFirstMonth() { return maxRateFirstMonth; }

	public double getMaxRate() { return maxRate; }

	public int getMaxPIFirstMonth() { return maxPIFirstMonth; }

	public double getMaxPI() { return maxPI; }

	public void print() {
		System.out.println(String.format("Max interest rate starting month: %d", getMaxRateFirstMonth()));
		System.out.println(String.format("Max interest rate: %2.3f%%", 100*getMaxRate()));
		System.out.println(String.format("Max principal and interest starting month: %d", getMaxPIFirstMonth()));
		System.out.println(String.format("Max principal and interest: $%6.2f", getMaxPI()));
		for (int i = 0; i < payments.length; i++)
			System.out.println(String.format("%d\t%d\t%d\t%9.2f\t%9.2f\t%9.2f\t", i+1,
					payments[i].getStartYear(), payments[i].getEndYear(), payments[i].getLowPI(), payments[i].getHighPI(), payments[i].getMI()));
	}
	
	public static void main(String[] args) {
		Payment payment1 = new InterestOnlyPayment(0.5);
		Payment payment2 = new AmortizingPayment(360);
		Payment compositePayment = new CompositePayment(13, payment1, 360, payment2);
		InterestRate rate = new AdjustableInterestRate(0.05125, 60, 12, 0.02, 0.01, 0.10125, 0.02, 0.01, 0.0125);
		Loan loan = new Loan(100000, 13+360, compositePayment, rate);
		ProjectedPayments projectedPayments = new ProjectedPayments(new Environment(0.05125), new Environment(0.02125), new Environment(0.2), loan);
		projectedPayments.print();
	}
}
