package com.actualize.mortgage.api;

import java.io.StringWriter;
import java.util.logging.Logger;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.w3c.dom.Document;

import com.actualize.mortgage.ucd.calculatepayments.CalculatePayments;

/**
 * this class defines the apis for getting the calculations for Project payments and loan terms
 * @author sboragala
 *
 */
@RestController
public class CalculationsApiImpl {
	
	private static final Logger LOG = Logger.getLogger(CalculationsApiImpl.class.getName());
	
   /**
    * this api return the mismo xml with calculated projected payments details
    * @param xmldoc
    * @return xml Returns XML with projected payments calculated values
    * @throws Exception
    */
	@RequestMapping(value = "/actualize/transformx/services/ucd/calculatepayments", method = { RequestMethod.POST })
	public String calculatePayments(@RequestBody String xmldoc) throws Exception {
		LOG.info("user used Service: Calculations Service");
		CalculatePayments calculator = new CalculatePayments();
        Document doc = calculator.calculate(xmldoc);
        
        Transformer tr = TransformerFactory.newInstance().newTransformer();
	        tr.setOutputProperty(OutputKeys.INDENT, "yes");
	        tr.setOutputProperty(OutputKeys.METHOD, "xml");
	        tr.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
	        tr.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
        StreamResult result = new StreamResult(new StringWriter());
        	tr.transform(new DOMSource(doc), result);
        
        return result.getWriter().toString();
	}
	
	/**
	 * check the status of the service
	 * @return String 
	 * @throws Exception
	 */
	@RequestMapping(value = "/ping", method = { RequestMethod.GET })
    public String checkStatus() throws Exception {
		LOG.info("user used Service: ping to Calculations Service");
        return "The service for generating calculations is running and ready to accept your requests";
    }


}
