package com.actualize.mortgage.calculatepayments.service;

import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class Controller {
	public static final String MORTGAGE_CADENCE_MAP = "MortgageCadenceMap.xml";
	private static final Logger LOGGER = Logger.getLogger(Controller.class.getName());

    @Autowired
    private UCDTransformerService ucdTransformerService;

    @Autowired
    private FileService fileService;

    @RequestMapping(value = {"/actualize/transformx/services/ucd/cd/calculatepayments"}, name = "TRIDen Toolkit", method = { RequestMethod.POST }, consumes = "application/xml", produces = "application/xml")
    public UCDXMLResult transformMortgageCadenceJSONtoMISMO(@RequestBody MortgageCadenceJSON mortgageCadenceJSONObject) throws Exception {
		LOGGER.log(Level.INFO, "Service call: /ucdxml");
        InputStream mappingFileStream;
        if(null!=fileService.getFilename() && !"".equalsIgnoreCase(fileService.getFilename()) && !"MortgageCadenceMap.xml".equalsIgnoreCase(fileService.getFilename())) {
            mappingFileStream = new FileInputStream(fileService.getFilename());
        } else {
            mappingFileStream = getClass().getClassLoader().getResourceAsStream("MortgageCadenceMap.xml");
        }
        
        IntermediateXMLData intermediateXMLData = ucdTransformerService.generateIntermediateXML(mappingFileStream, mortgageCadenceJSONObject);
        MESSAGE message = ucdTransformerService.generateMasterXML(intermediateXMLData);
        return ucdTransformerService.generateUCDXML(message);
    }

}
