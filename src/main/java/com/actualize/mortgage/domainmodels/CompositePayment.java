package com.actualize.mortgage.domainmodels;

public class CompositePayment extends Payment {
	public class PaymentSegment {
		public final int endPeriod;
		public final Payment payment;
		
		PaymentSegment(int endPeriod, Payment payment) {
			this.endPeriod = endPeriod;
			this.payment = payment;
		}
	}
	
	public final PaymentSegment[] paymentSegments;
	
	public CompositePayment(Object... arguments) {
		int length = arguments.length/2;
		paymentSegments = new PaymentSegment[length];
		for (int i = 0; i < length; i++)
			paymentSegments[i] = new PaymentSegment((Integer)arguments[2*i], (Payment)arguments[2*i + 1]);
	}

	@Override
	public boolean isReset(int period) { return getPayment(period).isReset(adjustedPeriod(period)); }

	@Override
	public boolean resetsWithRateChange(int period) { return getPayment(period).resetsWithRateChange(adjustedPeriod(period)); }

	@Override
	public boolean resetsWithBalanceChange(int period) { return getPayment(period).resetsWithBalanceChange(adjustedPeriod(period)); }

	@Override
	public double pmt(int period, double balance, double rate) { return getPayment(period).pmt(adjustedPeriod(period), balance, rate); }

	@Override
	public double ipmt(int period, double pmt, double balance, double rate) { return getPayment(period).ipmt(adjustedPeriod(period), pmt, balance, rate); }

	@Override
	public double ppmt(int period, double pmt, double balance, double rate) { return getPayment(period).ppmt(adjustedPeriod(period), pmt, balance, rate); }
	
	@Override
	public Payment getPayment(int period) {
		for (int i = 0; i < paymentSegments.length; i++)
			if (period < paymentSegments[i].endPeriod)
				return paymentSegments[i].payment;
			else
				period -= paymentSegments[i].endPeriod;
		return null;
	}
	
	private int adjustedPeriod(int period) {
		for (int i = 0; i < paymentSegments.length; i++)
			if (period < paymentSegments[i].endPeriod)
				return period;
			else
				period -= paymentSegments[i].endPeriod;
		return period;
	}

}
