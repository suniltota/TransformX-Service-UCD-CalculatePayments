package com.actualize.mortgage.ucd.calculatepayments;

import java.util.LinkedList;

import com.actualize.mortgage.domainmodels.CashFlowInfo;
import com.actualize.mortgage.domainmodels.CashFlowResult;
import com.actualize.mortgage.domainmodels.DisclosureModel;
import com.actualize.mortgage.domainmodels.Environment;
import com.actualize.mortgage.domainmodels.Loan;
import com.actualize.mortgage.domainmodels.MortgageInsurance;
/**
 * 
 * @author tim
 *
 */
public class ProjectedPayments {
	
	public final DisclosureModel[] payments;

	/**
	 * parameterized constructor to evaluate projected payments 
	 * @param baseEnv
	 * @param lowEnv
	 * @param highEnv
	 * @param loan
	 * @param mi
	 */
	public ProjectedPayments(Loan loan, MortgageInsurance mi) {
		LinkedList<DisclosureModel> disclosureModels = generateAllDistinctPayments(loan, mi);
		disclosureModels = combineSameStartYear(disclosureModels);
		disclosureModels = consolidateThirdPayment(disclosureModels);
		disclosureModels = fixEndYears(disclosureModels);
		this.payments = disclosureModels.toArray(new DisclosureModel[disclosureModels.size()]);
	}
	
	/**
	 * 
	 * @param baseEnv
	 * @param lowEnv
	 * @param high
	 * @param loan
	 * @param mi
	 * @return
	 */
	private LinkedList<DisclosureModel> generateAllDistinctPayments(Loan loan, MortgageInsurance mi) {
		Environment baseEnv = new Environment(loan.interestRate.getInitialRate());
		Environment lowEnv = new Environment(loan.interestRate.getMinRate());
		Environment highEnv = new Environment(loan.interestRate.getMaxRate()); 
		CashFlowResult base = loan.generateCashFlows(baseEnv);
		if (mi != null)
			mi.addMortgageInsurance(base);
		CashFlowResult low = loan.generateCashFlows(lowEnv);
		CashFlowResult high = loan.generateCashFlows(highEnv);
		double oldMi = 0;
		int periods = base.length;
		DisclosureModel pd = null;
		LinkedList<DisclosureModel> disclosureModels = new LinkedList<DisclosureModel>();
		for (int i = 0; i < base.length-1; i++) {
			double lowPI = low.getValue(i, CashFlowInfo.PRINCIPAL_AND_INTEREST_PAYMENT);
			double highPI = high.getValue(i, CashFlowInfo.PRINCIPAL_AND_INTEREST_PAYMENT);
			double miPmt = base.getValue(i, CashFlowInfo.MORTGAGE_INSURANCE_PAYMENT);
			if (pd == null || lowPI != pd.getLowPI() || highPI != pd.getHighPI() || (oldMi!= 0 && miPmt==0)) {
				if (pd != null)
					pd.setEnd(i);
				pd = new DisclosureModel(i, periods, lowPI, highPI, miPmt);
				disclosureModels.add(pd);
			}
			oldMi = miPmt;
		}
		return disclosureModels;
	}
	
	/**
	 * gets the start years
	 * @param paymentDisclosures
	 * @return list of Disclosure Models
	 */
	LinkedList<DisclosureModel> combineSameStartYear(LinkedList<DisclosureModel> paymentDisclosures) {
		int current = 0;
		int next = 1;
		while (next < paymentDisclosures.size()) {
			DisclosureModel pd1 = paymentDisclosures.get(current);
			DisclosureModel pd2 = paymentDisclosures.get(next);
			if (pd1.getStart() == pd2.getStart()) {
				pd1.combine(pd2);
				paymentDisclosures.remove(next);
			} else {
				current++;
				next = current + 1;
			}
			
		}
		return paymentDisclosures;
	}
	
	/**
	 * consolidates the Third Payment
	 * @param paymentDisclosures
	 * @return list of Disclosure Models
	 */
	LinkedList<DisclosureModel> consolidateThirdPayment(LinkedList<DisclosureModel> paymentDisclosures) {
		while (paymentDisclosures.size() > 4) {
			DisclosureModel pd1 = paymentDisclosures.get(2);
			DisclosureModel pd2 = paymentDisclosures.get(3);
			pd1.combine(pd2);
			paymentDisclosures.remove(3);
		}
		return paymentDisclosures;
	}
	
	/**
	 * calculates the end years
	 * @param paymentDisclosures
	 * @return list of disclosure models
	 */
	LinkedList<DisclosureModel> fixEndYears(LinkedList<DisclosureModel> paymentDisclosures) {
		for (int i = 0; i < paymentDisclosures.size() - 1; i++) {
			DisclosureModel pd1 = paymentDisclosures.get(i);
			DisclosureModel pd2 = paymentDisclosures.get(i+1);
			if (pd1.getEnd() == pd2.getStart()) {
				pd1.setEnd(12*(pd2.getStart()-1));
				pd2.setStart(12*(pd2.getStart()-1)+1);
				pd2.combine(pd1);
			}
		}
		return paymentDisclosures;
	}

}
