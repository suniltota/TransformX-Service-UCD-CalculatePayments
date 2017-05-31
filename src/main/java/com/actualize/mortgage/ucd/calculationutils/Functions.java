package com.actualize.mortgage.ucd.calculationutils;

public class Functions {
	static final double LOW_RATE = 0.001;
	static final double HIGH_RATE = 1.0;
	static final double MAX_ITERATION = 1000;
	static final double PRECISION_REQ = 0.00000001;

	static public double round(double value) {
		return Math.round(value * 100.0) / 100.0;
	}
	
	static public double irr(double[] cf) {
		double oldNpv = 0.00;
		double newNpv = 0.00;
		double newguessRate = LOW_RATE;
		double guessRate = LOW_RATE;
		double lowGuessRate = LOW_RATE;
		double highGuessRate = HIGH_RATE;
		for (int i = 0; i < MAX_ITERATION; i++) {
			double npv = npv(cf, guessRate);
			if (npv >= 0 && npv < PRECISION_REQ) { // Make sure npv is non-negative as payments need to "cover" initial amount
				System.out.println("Iterations = " + i);
				break;
			}
			if (oldNpv == 0)
				oldNpv = npv;
			else
				oldNpv = newNpv;
			newNpv = npv;
			if (i > 0) {
				if (oldNpv < newNpv) {
					if (oldNpv < 0 && newNpv < 0)
						highGuessRate = newguessRate;
					else
						lowGuessRate = newguessRate;
				} else {
					if (oldNpv > 0 && newNpv > 0)
						lowGuessRate = newguessRate;
					else
						highGuessRate = newguessRate;
				}
			}
			guessRate = (lowGuessRate + highGuessRate) / 2;
			newguessRate = guessRate;
		}
		return guessRate;
	}
	
	static public double npv(double[] cf, double rate) {
		double result = 0;
		for (int j = 0; j < cf.length; j++) {
			double denom = Math.pow((1 + rate), j);
			result = result + cf[j] / denom;
		}
		return result;
	}

}
