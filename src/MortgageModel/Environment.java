package MortgageModel;

public class Environment {
	double rate = 0;
	
	public Environment() {
	}
	
	public Environment(double r) {
		rate = r;
	}
	
	double getRate(int period) {
		return rate;
	}
}
