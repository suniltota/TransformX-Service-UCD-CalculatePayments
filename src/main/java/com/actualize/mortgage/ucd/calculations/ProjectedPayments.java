package com.actualize.mortgage.ucd.calculations;

import java.util.LinkedList;

import com.actualize.mortgage.mortgagemodel.CashFlowInfo;
import com.actualize.mortgage.mortgagemodel.CashFlowResult;
import com.actualize.mortgage.mortgagemodel.Environment;
import com.actualize.mortgage.mortgagemodel.Loan;
import com.actualize.mortgage.mortgagemodel.MortgageInsurance;

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
			if (end < pd.end)
				end = pd.end;
			if (highPI < pd.highPI)
				highPI = pd.highPI;
			if (lowPI > pd.lowPI)
				lowPI = pd.lowPI;
		}

		public int getStartYear() { return start/12 + 1; }
		public int getEndYear() { return (end+11)/12; }
		public double getHighPI() { return highPI; }
		public double getLowPI() { return lowPI; }
		public double getMI() { return mi; }
	}
	
	private Disclosure[] payments;
	private int maxRateFirstMonth = 0;
	private double maxRate = 0;
	private int maxPIFirstMonth = 0;
	private double maxPI = 0;
	
	public ProjectedPayments(Environment baseEnv, Environment lowEnv, Environment highEnv, Loan loan, MortgageInsurance mi) {
		LinkedList<Disclosure> disclosures = generateAllDistinctPayments(baseEnv, lowEnv, highEnv, loan, mi);
		disclosures = combineSameStartYear(disclosures);
		disclosures = consolidateThirdPayment(disclosures);
		disclosures = fixEndYears(disclosures);
		this.payments = disclosures.toArray(new Disclosure[disclosures.size()]);
	}
	
	private LinkedList<Disclosure> generateAllDistinctPayments(Environment baseEnv, Environment lowEnv, Environment highEnv, Loan loan, MortgageInsurance mi) {
		CashFlowResult base = loan.generateCashFlows(baseEnv);
		mi.addMortgageInsurance(base);
		CashFlowResult low = loan.generateCashFlows(lowEnv);
		CashFlowResult high = loan.generateCashFlows(highEnv);
		double oldMi = 0;
		int periods = base.length;
		Disclosure pd = null;
		LinkedList<Disclosure> disclosures = new LinkedList<Disclosure>();
		for (int i = 0; i < base.length-1; i++) {
			double lowPI = low.getValue(i, CashFlowInfo.PRINCIPAL_AND_INTEREST_PAYMENT);
			double highPI = high.getValue(i, CashFlowInfo.PRINCIPAL_AND_INTEREST_PAYMENT);
			double miPmt = base.getValue(i, CashFlowInfo.MORTGAGE_INSURANCE_PAYMENT);
			if (pd == null || lowPI != pd.getLowPI() || highPI != pd.getHighPI() || (oldMi!= 0 && miPmt==0)) {
				if (highPI > maxPI) {
					maxPIFirstMonth = i;
					maxPI = highPI;
				}
				if (pd != null)
					pd.end = i;
				pd = new Disclosure(i, periods, lowPI, highPI, miPmt);
				disclosures.add(pd);
			}
			oldMi = miPmt;
			double highRate = high.getValue(i, CashFlowInfo.INTEREST_RATE);
			if (highRate > maxRate) {
				maxRateFirstMonth = i;
				maxRate = highRate;
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
				pd1.end = 12*(pd2.getStartYear()-1);
				pd2.start = 12*(pd2.getStartYear()-1)+1;
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

}
