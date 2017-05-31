package com.actualize.mortgage.mortgagemodel;

import com.actualize.mortgage.ucd.calculationutils.Functions;

public class InterestOnlyPayment extends Payment {

	private double factor;
	
	public InterestOnlyPayment(double factor) {
		this.factor = factor;
	}
	
	@Override
	public boolean resetsWithRateChange(int period) { return true; }

	@Override
	public boolean resetsWithBalanceChange(int period) { return true; }

	@Override
	public double pmt(int period, double balance, double rate) {
		rate = rate/12.0;
		return Functions.round(factor * rate * balance);
	}

	@Override
	public double ipmt(int period, double pmt, double balance, double rate) { return pmt(period, balance, rate); }

	@Override
	public double ppmt(int period, double pmt, double balance, double rate) { return 0; }

}
