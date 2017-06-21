package com.actualize.mortgage.domainmodels;

import com.actualize.mortgage.ucd.calculationutils.CalculationErrorType;

public class CalculationError {

	private final CalculationErrorType type;
	private final String info;
	
	public CalculationError(CalculationErrorType type, String info) {
		this.type = type;
		this.info = info;
	}

	/**
	 * @return the type
	 */
	public CalculationErrorType getType() {
		return type;
	}

	/**
	 * @return the info
	 */
	public String getInfo() {
		return info;
	}

	
}
