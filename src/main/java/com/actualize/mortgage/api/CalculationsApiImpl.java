package com.actualize.mortgage.api;

import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
/**
 * this class defines the apis for getting the calulations for Project payments and loan terms
 * @author sboragala
 *
 */
@RestController
@RequestMapping("/actualize/transformx/services/ucd/calculatepayments")
public class CalculationsApiImpl {
	
	private static final Logger LOGGER = Logger.getLogger(CalculationsApiImpl.class.getName());
   
	@RequestMapping(value = "/projectedpayments", method = { RequestMethod.GET })
	public String createProjectedPayments() throws Exception
	{
		return "this api should return projected payments";
	}
	
	@RequestMapping(value = "/loanterms", method = { RequestMethod.GET })
	public String createLoanCalculations() throws Exception
	{
		return "this api should return loanterms";
	}

}
