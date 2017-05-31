package com.actualize.mortgage.domainmodels;

public class FixedInterestRate extends InterestRate {
	private double rate;
	
	public FixedInterestRate(double rate) {
		this.rate = rate;
	}

	@Override
	public double getRate(Environment environment, int period) {
		return rate;
	}

	@Override
	public boolean isReset(int period) {
		return period == 0;
	}

}
