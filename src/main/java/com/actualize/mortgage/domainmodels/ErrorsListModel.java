/**
 * 
 */
package com.actualize.mortgage.domainmodels;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

/**
 * defines the list of exceptions in response
 * @author sboragala
 *
 */
@XmlRootElement(name="exceptions")
@XmlAccessorType(XmlAccessType.FIELD)
public class ErrorsListModel {
	
	@XmlElement(name="exception")
	private List<ErrorModel> error ;

	/**
	 * @return the error
	 */
	public List<ErrorModel> getError() {
		return error;
	}

	/**
	 * @param error the error to set
	 */
	public void setError(List<ErrorModel> error) {
		this.error = error;
	} 

	
}
