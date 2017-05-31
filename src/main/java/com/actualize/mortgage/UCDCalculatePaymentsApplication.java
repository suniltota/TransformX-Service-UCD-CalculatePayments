/**
 * 
 */
package com.actualize.mortgage;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * this class initiates the UCD
 * @author sboragala
 *
 */
@SpringBootApplication(scanBasePackages = "com.actualize.mortgage")
public class UCDCalculatePaymentsApplication {
	
	public void main(String args[])
	{
		SpringApplication.run(UCDCalculatePaymentsApplication.class, args);
	}
}
