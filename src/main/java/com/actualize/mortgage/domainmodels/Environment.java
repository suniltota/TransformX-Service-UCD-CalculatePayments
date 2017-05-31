package com.actualize.mortgage.domainmodels;

import java.io.Serializable;

/**
 * 
 * @author tim
 *
 */
public class Environment implements Serializable{
	private static final long serialVersionUID = 1L;
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
