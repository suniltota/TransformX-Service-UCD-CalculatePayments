package com.actualize.mortgage.api;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.w3c.dom.Document;

import com.actualize.mortgage.ucd.calculatepayments.CalculatePayments;
/**
 * this class defines the apis for getting the calulations for Project payments and loan terms
 * @author sboragala
 *
 */
@RestController
@RequestMapping("/actualize/transformx/services/ucd/calculatepayments")
public class CalculationsApiImpl {
	
	private static final Logger LOGGER = Logger.getLogger(CalculationsApiImpl.class.getName());
   
	@RequestMapping(value = "/calculatepayments", method = { RequestMethod.GET })
	public String calculatePayments(@RequestBody String xmldoc) throws Exception {
		LOGGER.log(Level.INFO, "Service call: /calculatepayments");
		CalculatePayments calculator = new CalculatePayments();
        Document doc = calculator.calculate(xmldoc);
        // TODO: convert doc to string and return?
        return null;
	}

}
