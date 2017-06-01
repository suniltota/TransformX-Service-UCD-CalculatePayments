package com.actualize.mortgage.domainmodels;

public class CalculationError {
	public enum CalculationErrorType {
		MISSING_DATA("calculation is missing required input data"),
		NOT_IMPLEMENTED("calculation not implemented"),
		OTHER("other calculation error");
		
		public final String msg;
		
		private CalculationErrorType(String msg) {
			this.msg = msg;
		}
	}
	
	public final CalculationErrorType type;
	public final String info;
	
	public CalculationError(CalculationErrorType type, String info) {
		this.type = type;
		this.info = info;
	}
}
