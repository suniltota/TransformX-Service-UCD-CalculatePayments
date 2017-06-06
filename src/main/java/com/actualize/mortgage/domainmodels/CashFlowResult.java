package com.actualize.mortgage.domainmodels;

import java.util.TreeMap;

import com.actualize.mortgage.ucd.calculationutils.Functions;
/**
 * 
 * @author tim
 *
 */
public class CashFlowResult {
	private final Object[] data;
	public final int length;

	public CashFlowResult(int periods) {
		this.data = new Object[periods];
		this.length = periods;
	}

	public double apr(double upfrontCosts) {
		double[] cf = new double[length + 1];
		cf[0] = -getValue(0, CashFlowInfo.BALANCE) + upfrontCosts;
		for (int i = 1; i < cf.length; i++)
			cf[i] = getValue(i-1, CashFlowInfo.TOTAL_PAYMENT);
		return 12.0 * Functions.irr(cf);
	}

	public CashFlowResult addValue(int month, CashFlowInfo info, double value) {
		addBase(month, info, value);
		for (CashFlowInfo cfi : info.sumTo)
			addBase(month, cfi, value);
		return this;
	}
	
	public double getValue(int month, CashFlowInfo... info) {
		if (data[month] == null) {
			data[month] = new TreeMap<CashFlowInfo, Double>();
			return 0;
		}
		double accum = 0;
		@SuppressWarnings("unchecked")
		TreeMap<CashFlowInfo, Double> periodData = ((TreeMap<CashFlowInfo, Double>)data[month]);
		for (CashFlowInfo cfi : info) {
			Double value = periodData.get(cfi);
			if (value != null)
				accum += value;
		}
		return Math.round(info[0].precision*accum)/info[0].precision;
	}
	
	@SuppressWarnings("unchecked")
	private void addBase(int month, CashFlowInfo info, double value) {
		value += getValue(month, info);
		((TreeMap<CashFlowInfo, Double>)data[month]).put(info, value);
	}
	
}
