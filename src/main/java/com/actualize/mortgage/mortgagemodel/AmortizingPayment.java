package com.actualize.mortgage.mortgagemodel;

import com.actualize.mortgage.ucd.calculationutils.Functions;

public class AmortizingPayment extends Payment {
	
	int term;
	
	public AmortizingPayment(int term) {
		this.term = term;
	}
	
	@Override
	public boolean resetsWithRateChange(int period) { return true; }

	@Override
	public double pmt(int period, double balance, double rate) {
		int n = term - period;
		rate = rate/12.0;
		return Functions.round(balance * rate * Math.pow(1+rate, n) / (Math.pow(1+rate, n) - 1));
	}

	@Override
	public double ipmt(int period, double pmt, double balance, double rate) { return Functions.round(balance * rate/12.0); }

	@Override
	public double ppmt(int period, double pmt, double balance, double rate) {
		double value = Functions.round(pmt - ipmt(period, pmt, balance, rate));
		if (value > balance)
			value = balance;
		return value;
	}

}
