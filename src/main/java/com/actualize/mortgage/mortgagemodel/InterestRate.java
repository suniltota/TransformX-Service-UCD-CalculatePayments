package MortgageModel;

public abstract class InterestRate {
	public abstract double getRate(Environment environment, int period);
	public abstract boolean isReset(int period);
}
