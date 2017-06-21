/**
 * 
 */
package com.actualize.mortgage.domainmodels;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * defines exception in response
 * @author sboragala
 *
 */
@XmlType(name = "MESSAGE", propOrder = {
	    "type",
	    "message"
	})
@XmlAccessorType(XmlAccessType.FIELD)
public class ErrorModel {
	
	@XmlElement 
	private String type;
	@XmlElement 
	private String message;
	
	/**
	 * @return the type
	 */
	public String getType() {
		return type;
	}
	/**
	 * @param type the type to set
	 */
	public void setType(String type) {
		this.type = type;
	}
	/**
	 * @return the message
	 */
	public String getMessage() {
		return message;
	}
	/**
	 * @param message the message to set
	 */
	public void setMessage(String message) {
		this.message = message;
	}
	
	
}
