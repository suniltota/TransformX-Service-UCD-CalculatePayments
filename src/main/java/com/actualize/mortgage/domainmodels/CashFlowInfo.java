package com.actualize.mortgage.domainmodels;

public enum CashFlowInfo {
	BALANCE(100, "%9.2f"),
	PRINCIPAL_AND_INTEREST_PAYMENT(100, "%9.2f"),
	TOTAL_PAYMENT(100, "%9.2f"),
	INTEREST_RATE(1000000, "%3.5f"),
	PRINCIPAL_PAYMENT(100, "%9.2f", PRINCIPAL_AND_INTEREST_PAYMENT, TOTAL_PAYMENT),
	INTEREST_PAYMENT(100, "%9.2f", PRINCIPAL_AND_INTEREST_PAYMENT, TOTAL_PAYMENT),
	MORTGAGE_INSURANCE_PAYMENT(100, "%9.2f", TOTAL_PAYMENT);

	public final int precision;
	public final String printFormat;
	public final CashFlowInfo[] sumTo;
	
	private CashFlowInfo(int precision, String printFormat, CashFlowInfo... sumTo) {
		this.precision = precision;
		this.printFormat = printFormat;
		this.sumTo = new CashFlowInfo[sumTo.length];
		int i = 0;
		for (CashFlowInfo s : sumTo)
			this.sumTo[i++] = s;
	}
	
}
