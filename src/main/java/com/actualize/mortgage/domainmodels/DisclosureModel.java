/**
 * 
 */
package com.actualize.mortgage.domainmodels;

import java.io.Serializable;

/**
 * @author sboragala
 *
 */
public class DisclosureModel implements Serializable {

	private static final long serialVersionUID = 8757797245576026825L;

	private int start;
	private int end;
	private double highPI;
	private double lowPI;
	private double mi;
		
	public DisclosureModel(int start, int end, double lowPI, double highPI, double mi) {
		this.start = start;
		this.end = end;
		this.highPI = highPI;
		this.lowPI = lowPI;
		this.mi = mi;
	}

	public void combine(DisclosureModel pd) {
		if (end < pd.end)
			end = pd.end;
		if (highPI < pd.highPI)
			highPI = pd.highPI;
		if (lowPI > pd.lowPI)
			lowPI = pd.lowPI;
	}

	/**
	 * @return the start
	 */
	public int getStart() {
		return start/12 + 1;
	}

	/**
	 * @param start the start to set
	 */
	public void setStart(int start) {
		this.start = start;
	}

	/**
	 * @return the end
	 */
	public int getEnd() {
		return end/12 + 1;
	}

	/**
	 * @param end the end to set
	 */
	public void setEnd(int end) {
		this.end = end;
	}

	/**
	 * @return the highPI
	 */
	public double getHighPI() {
		return highPI;
	}

	/**
	 * @param highPI the highPI to set
	 */
	public void setHighPI(double highPI) {
		this.highPI = highPI;
	}

	/**
	 * @return the lowPI
	 */
	public double getLowPI() {
		return lowPI;
	}

	/**
	 * @param lowPI the lowPI to set
	 */
	public void setLowPI(double lowPI) {
		this.lowPI = lowPI;
	}

	/**
	 * @return the mi
	 */
	public double getMi() {
		return mi;
	}

	/**
	 * @param mi the mi to set
	 */
	public void setMi(double mi) {
		this.mi = mi;
	}

}
