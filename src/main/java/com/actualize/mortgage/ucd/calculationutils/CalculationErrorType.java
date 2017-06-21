/**
 * 
 */
package com.actualize.mortgage.ucd.calculationutils;

/**
 * ENUM class to define type of exceptions
 * @author sboragala
 *
 */
public enum CalculationErrorType {
	
	INTERNAL_ERROR("internal error"),
	MISSING_DATA("calculation is missing required input data"),
	NOT_IMPLEMENTED("calculation not implemented"),
	OTHER("other calculation error");
	
	public final String msg;
	
	private CalculationErrorType(String msg) {
		this.msg = msg;
	}

}
