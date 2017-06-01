package com.actualize.mortgage.domainmodels;
/**
 * 
 * @author tim
 *
 */
public class AdjustableInterestRate extends InterestRate {
	private double initialRate;
	private int firstReset;
	private int subsequentReset;
	private double firstCap;
	private double subsequentCap;
	private double lifetimeCap;
	private double firstFloor;
	private double subsequentFloor;
	private double lifetimeFloor;
	
	public AdjustableInterestRate(double initialRate, int firstReset, int subsequentReset,
			double firstCap, double subsequentCap, double lifetimeCap,
			double firstFloor, double subsequentFloor, double lifetimeFloor) {
		this.initialRate = initialRate;
		this.firstReset = firstReset;
		this.subsequentReset = subsequentReset;
		this.firstCap = firstCap;
		this.subsequentCap = subsequentCap;
		this.lifetimeCap = lifetimeCap;
		this.firstFloor = firstFloor;
		this.subsequentFloor = subsequentFloor;
		this.lifetimeFloor = lifetimeFloor;
	}

	@Override
	public double getRate(Environment environment, int period) {
		double rate = initialRate;
		int resetMonth = firstReset;
		while (resetMonth <= period) {
			double targetRate = environment.getRate(resetMonth);
			if (targetRate < rate) {
				if (resetMonth == firstReset && targetRate+firstFloor < rate)
					rate -= firstFloor;
				else if (resetMonth != firstReset && targetRate+subsequentFloor < rate)
					rate -= subsequentFloor;
				else
					rate = targetRate;
				if (rate < lifetimeFloor)
					rate = lifetimeFloor;
			} else if (targetRate > rate) {
				if (resetMonth == firstReset && targetRate-firstCap > rate)
					rate += firstCap;
				else if (resetMonth != firstReset && targetRate-subsequentCap > rate)
					rate += subsequentCap;
				else
					rate = targetRate;
				if (rate > lifetimeCap)
					rate = lifetimeCap;
			}
			resetMonth += subsequentReset;
		}
		return rate;
	}

	@Override
	public double getInitialRate() {
		return initialRate;
	}

	@Override
	public double getMaxRate() {
		return lifetimeCap;
	}

	@Override
	public double getMinRate() {
		return lifetimeFloor;
	}

	@Override
	public boolean isReset(int period) {
		if (period == 0)
			return true;
		if (period < firstReset)
			return false;
		return (period - firstReset) % subsequentReset == 0;
	}

}
