package com.actualize.mortgage.mortgagemodel;

public abstract class Payment {
	
	public boolean isReset(int period) { return period == 0; }
	public boolean resetsWithRateChange(int period) { return false; }
	public boolean resetsWithBalanceChange(int period) { return false; }
	
	public abstract double pmt(int period, double balance, double rate);
	public abstract double ipmt(int period, double pmt, double balance, double rate);
	public abstract double ppmt(int period, double pmt, double balance, double rate);
}
