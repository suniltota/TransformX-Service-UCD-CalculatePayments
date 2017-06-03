package com.actualize.mortgage.api;

import java.io.StringWriter;
import java.util.logging.Level;
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
 * this class defines the apis for getting the calulations for Project payments and loan terms
 * @author sboragala
 *
 */
@RestController
@RequestMapping("/actualize/transformx/services/ucd/calculatepayments")
public class CalculationsApiImpl {
	
	private static final Logger LOGGER = Logger.getLogger(CalculationsApiImpl.class.getName());
   
	@RequestMapping(value = "/calculatepayments", method = { RequestMethod.POST })
	public String calculatePayments(@RequestBody String xmldoc) throws Exception {
		LOGGER.log(Level.INFO, "Service call: /calculatepayments");
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

}
