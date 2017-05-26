package MortgageModel;

public class CashFlow {
	static final double LOW_RATE = 0.001;
	static final double HIGH_RATE = 1.0;
	static final double MAX_ITERATION = 1000;
	static final double PRECISION_REQ = 0.00000001;
	
	public enum CashFlowType {
		BALANCE(0),
		PAYMENT(1),
		PRINCIPAL(2),
		INTERESTRATE(3),
		INTEREST(4);
		
		private int index;
		
	    CashFlowType(int i) {
	    	index = i;
	    }
	    
	    public int getIndex() { return index; }
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

	private int periods = 0;
	private double[][] cashFlows = null;
	
	public CashFlow(int periods) {
		this.periods = periods;
		cashFlows = new double[5][periods];
	}
	
	public double apr(double upfrontCosts) {
		double[] cf = new double[periods + 1];
		cf[0] = -getBalance(0) + upfrontCosts;
		for (int i = 1; i < cf.length; i++)
			cf[i] = getPayment(i-1);
		return 12.0 * irr(cf);
	}

	public void print() {
		System.out.println(String.format("APR = %2.4f%%", 100.0*apr(0)));
		System.out.println("");
		System.out.println("Period\t  Balance \t  Payment \t Principal\t Rate    \t Interest");
		System.out.println("------\t----------\t----------\t----------\t-------  \t----------");
		for (int i = 0; i < cashFlows[0].length && getBalance(i) > 0; i++)
			System.out.println(String.format("%d\t%10.2f\t%10.2f\t%10.2f\t%2.4f%%  \t%10.2f", i+1, getBalance(i), getPayment(i), getPrincipal(i), 100.0*getRate(i), getInterest(i)));
	}
	
	public int getPeriods() {
		return periods;
	}
	
	public double getBalance(int period) {
		return cashFlows[CashFlowType.BALANCE.getIndex()][period];
	}
	
	public void setBalance(int period, double value) {
		cashFlows[CashFlowType.BALANCE.getIndex()][period] = value;
	}
	
	public double getPayment(int period) {
		return cashFlows[CashFlowType.PAYMENT.getIndex()][period];
	}
	
	public void setPayment(int period, double value) {
		cashFlows[CashFlowType.PAYMENT.getIndex()][period] = value;
	}
	
	public double getPrincipal(int period) {
		return cashFlows[CashFlowType.PRINCIPAL.getIndex()][period];
	}
	
	public void setPrincipal(int period, double value) {
		cashFlows[CashFlowType.PRINCIPAL.getIndex()][period] = value;
	}
	
	public double getRate(int period) {
		return cashFlows[CashFlowType.INTERESTRATE.getIndex()][period];
	}
	
	public void setRate(int period, double value) {
		cashFlows[CashFlowType.INTERESTRATE.getIndex()][period] = value;
	}
	
	public double getInterest(int period) {
		return cashFlows[CashFlowType.INTEREST.getIndex()][period];
	}
	
	public void setInterest(int period, double value) {
		cashFlows[CashFlowType.INTEREST.getIndex()][period] = value;
	}
}
