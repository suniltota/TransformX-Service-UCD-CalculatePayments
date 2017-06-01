package com.actualize.mortgage.ucd.calculatepayments;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.StringReader;
import java.util.LinkedList;
import java.util.Scanner;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.actualize.mortgage.domainmodels.AdjustableInterestRate;
import com.actualize.mortgage.domainmodels.AmortizingPayment;
import com.actualize.mortgage.domainmodels.CalculationError;
import com.actualize.mortgage.domainmodels.CalculationError.CalculationErrorType;
import com.actualize.mortgage.domainmodels.FixedInterestRate;
import com.actualize.mortgage.domainmodels.InterestRate;
import com.actualize.mortgage.domainmodels.Loan;
import com.actualize.mortgage.domainmodels.MortgageInsurance;
import com.actualize.mortgage.domainmodels.Payment;

public class CalculatePayments {
	private static final String MISMO_URL = "http://www.mismo.org/residential/2009/schemas";
	private static final XPath XPATH = XPathFactory.newInstance().newXPath();
	private LinkedList<CalculationError> errors = new LinkedList<CalculationError>();

	public Document calculate(String xmldoc) {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();  
        DocumentBuilder builder;  
        try  
        {  
            builder = factory.newDocumentBuilder();  
            Document doc = builder.parse(new InputSource(new StringReader(xmldoc)));
            return calculate(doc);
        } catch (Exception e) {  
            e.printStackTrace();  
        }
        return null;
	}

	public Document calculate(Document doc) throws NumberFormatException, XPathExpressionException {
		Node root = doc.getDocumentElement();
		String mismo = getNameSpaceFromURL(root, MISMO_URL);
		
		// Obtain calculation parameters
		// TODO:  LOAN_DETAIL/{ BalloonIndicator, InterestOnlyIndicator }
		int loanTerm = getIntegerValue(root, addNamespace("//MATURITY/MATURITY_RULE/LoanMaturityPeriodCount", mismo), null);
		double loanAmount = getDoubleValue(root, addNamespace("//TERMS_OF_LOAN/NoteAmount", mismo), null);
		String amortizationType = getStringValue(root, addNamespace("//AMORTIZATION/AMORTIZATION_RULE/AmortizationType", mismo), "");
		InterestRate rate = null;
		switch (amortizationType) {
			case "Fixed":
				rate = createFixedInterestRate(root, mismo);
				break;
			case "AdjustableRate":
				rate = createAdjustableInterestRate(root, mismo);
				break;
			default:
				errors.add(new CalculationError(CalculationErrorType.NOT_IMPLEMENTED, "amortization type '" + amortizationType + "' not supported"));
		}
		double loanCostsTotal = 0; //getDoubleValue(root, addNamespace("//INTEGRATED_DISCLOSURE_SECTION_SUMMARY_DETAIL[IntegratedDisclosureSectionType=TotalLoanCosts][IntegratedDisclosureSubsectionType=LoanCostsSubtotal]/IntegratedDisclosureSectionTotalAmount", mismo), null);
		double aprIncludedCostsTotal = 0; // Sum-> prepaids, escrow, costs where PaidByType=Buyer and PaymentIncludedInAPRIndicator=false
		double prepaidInterest = 0; // Sum-> PREPAID_ITEM/[PREPAID_ITEM_DETAIL/PrepaidItemType=PrepaidInterest]/PREPAID_ITEM_PAYMENTS/PREPAID_ITEM_PAYMENT[PrepaidItemPaymentPaidByType=Buyer]/PrepaidItemActualPaymentAmount
		
		// Perform calculations
		Payment payment = new AmortizingPayment(loanTerm);
		Loan loan = new Loan(loanAmount, loanTerm, payment, rate);
		double fullyIndexedRate = loan.interestRate.getInitialRate();
		MortgageInsurance insurance = null; // TODO: need to find datapoints to pass info
		ProjectedPayments projected = new ProjectedPayments(loan, insurance);
		LoanCalculations calcs = new LoanCalculations(loan, insurance, fullyIndexedRate, loanCostsTotal, aprIncludedCostsTotal, prepaidInterest);

		// Insert results
		Node integratedDisclosure = getNode(root, "//INTEGRATED_DISCLOSURE");
		Node projectedPayments = getNode(integratedDisclosure, "//PROJECTED_PAYMENTS");
		if (projectedPayments != null)
			integratedDisclosure.removeChild(projectedPayments);
		projectedPayments = integratedDisclosure.appendChild(doc.createElement(addNamespace("PROJECTED_PAYMENTS", mismo)));
		for (int i = 0; i < projected.payments.length; i++) {
			Element projectedPayment = (Element)projectedPayments.appendChild(doc.createElement(addNamespace("PROJECTED_PAYMENT", mismo)));
			projectedPayment.setAttribute("SequenceNumber", ""+(i+1));
			double escrow = 0; // TODO
			double mi = 0; // TODO
			double maxPI = projected.payments[i].getHighPI();
			double minPI = projected.payments[i].getLowPI();
			projectedPayment.appendChild(doc.createElement(addNamespace("PaymentFrequencyType", mismo))).appendChild(doc.createTextNode("Monthly"));
			projectedPayment.appendChild(doc.createElement(addNamespace("ProjectedPaymentCalculationPeriodEndNumber", mismo))).appendChild(doc.createTextNode("" + projected.payments[i].getEnd()));
			projectedPayment.appendChild(doc.createElement(addNamespace("ProjectedPaymentCalculationPeriodStartNumber", mismo))).appendChild(doc.createTextNode("" + projected.payments[i].getStart()));
			projectedPayment.appendChild(doc.createElement(addNamespace("ProjectedPaymentCalculationPeriodTermType", mismo))).appendChild(doc.createTextNode("Yearly"));
			projectedPayment.appendChild(doc.createElement(addNamespace("ProjectedPaymentEstimatedEscrowPaymentAmount", mismo))).appendChild(doc.createTextNode(String.format("%9.2f", escrow).trim()));
			projectedPayment.appendChild(doc.createElement(addNamespace("ProjectedPaymentEstimatedTotalMaximumPaymentAmount", mismo))).appendChild(doc.createTextNode(String.format("%9.2f", maxPI + mi + escrow).trim()));
			if (maxPI != minPI)
				projectedPayment.appendChild(doc.createElement(addNamespace("ProjectedPaymentEstimatedTotalMinimumPaymentAmount", mismo))).appendChild(doc.createTextNode(String.format("%9.2f", minPI + mi + escrow).trim()));
			projectedPayment.appendChild(doc.createElement(addNamespace("ProjectedPaymentMIPaymentAmount", mismo))).appendChild(doc.createTextNode(String.format("%9.2f", mi)));
			projectedPayment.appendChild(doc.createElement(addNamespace("ProjectedPaymentPrincipalAndInterestMaximumPaymentAmount", mismo))).appendChild(doc.createTextNode(String.format("%9.2f", maxPI).trim()));
			if (maxPI != minPI)
				projectedPayment.appendChild(doc.createElement(addNamespace("ProjectedPaymentPrincipalAndInterestMinimumPaymentAmount", mismo))).appendChild(doc.createTextNode(String.format("%9.2f", minPI).trim()));
		}
		
		return doc;
	}
	
	public CalculationError[] getErrors() {
		return errors.toArray(new CalculationError[errors.size()]);
	}
	
	private InterestRate createAdjustableInterestRate(Node root, String mismo) {
		double initialRate = getDoubleValue(root, addNamespace("//TERMS_OF_LOAN/DisclosedFullyIndexedRatePercent", mismo), null) / 100.0;
		int firstResetTerm = getIntegerValue(root, addNamespace("//INTEREST_RATE_LIFETIME_ADJUSTMENT_RULE/FirstRateChangeMonthsCount", mismo), null) - 1;
		int subsequentResetTerm = getIntegerValue(root, addNamespace("//INTEREST_RATE_PER_CHANGE_ADJUSTMENT_RULE[AdjustmentRuleType=First]/PerChangeRateAdjustmentFrequencyMonthsCount", mismo), null);
		double firstResetCap = getDoubleValue(root, addNamespace("//INTEREST_RATE_PER_CHANGE_ADJUSTMENT_RULE[AdjustmentRuleType=First]/PerChangeMaximumIncreaseRatePercent", mismo), null) / 100.0;
		double subsequentResetCap = getDoubleValue(root, addNamespace("//INTEREST_RATE_PER_CHANGE_ADJUSTMENT_RULE[AdjustmentRuleType=Subsequent]/PerChangeMaximumIncreaseRatePercent", mismo), null) / 100.0;
		double lifetimeCap = getDoubleValue(root, addNamespace("//INTEREST_RATE_LIFETIME_ADJUSTMENT_RULE/CeilingRatePercent", mismo), null) / 100.0;
		double lifetimeFloor = getDoubleValue(root, addNamespace("//INTEREST_RATE_LIFETIME_ADJUSTMENT_RULE/FloorRatePercent", mismo), null) / 100.0;
		return new AdjustableInterestRate(initialRate, firstResetTerm, subsequentResetTerm, firstResetCap, subsequentResetCap, lifetimeCap, firstResetCap, subsequentResetCap, lifetimeFloor);
	}
	
	private InterestRate createFixedInterestRate(Node root, String mismo) {
		double rate = getDoubleValue(root, addNamespace("//TERMS_OF_LOAN/NoteRatePercent", mismo), null);
		return new FixedInterestRate(rate);
	}
	
	private double getDoubleValue(Node node, String expression, Double dflt) {
		try {
			return Double.parseDouble(getStringValue(node, expression, dflt==null ? null : dflt.toString()));
		} catch (XPathExpressionException e) {
			return 0;
		}
	}
	
	private int getIntegerValue(Node node, String expression, Integer dflt) {
		try {
			return Integer.parseInt(getStringValue(node, expression, dflt==null ? null : dflt.toString()));
		} catch (XPathExpressionException e) {
			return 0;
		}
	}
	
	private String getStringValue(Node node, String expression, String dflt) throws XPathExpressionException {
		try {
			return (String)XPATH.evaluate(expression, node, XPathConstants.STRING);
		} catch (XPathExpressionException e) {
			if (dflt != null)
				return dflt;
			errors.add(new CalculationError(CalculationErrorType.MISSING_DATA, "bad xpath expression '" + expression + "'"));
			throw e;
		}
	}
	
	private Node getNode(Node node, String expression) throws XPathExpressionException {
		NodeList nodeList = getNodeList(node, expression);
		if (nodeList.getLength() > 0)
			return nodeList.item(0);
		return null;
	}
	
	private NodeList getNodeList(Node node, String expression) throws XPathExpressionException {
		try {
			return (NodeList)XPATH.evaluate(expression, node, XPathConstants.NODESET);
		} catch (XPathExpressionException e) {
			errors.add(new CalculationError(CalculationErrorType.MISSING_DATA, "bad xpath expression '" + expression + "'"));
			throw e;
		}
	}
	
	private static String addNamespace(String path, String namespace) {
		String[] nodes = path.split("/");
		for (int i = 0; i < nodes.length; i++)
//			nodes[i] = "*[local-name()='" + nodes[i] + "']";
			if (!"".equals(namespace) && !"".equals(nodes[i]) && nodes[i].indexOf(':') == -1)
				nodes[i] = namespace + ":" + nodes[i];		
		return String.join("/", nodes);
	}
	
	private static String getNameSpaceFromURL(Node node, String url) {
		NamedNodeMap attributes = node.getAttributes();
		for (int i = 0; i < attributes.getLength(); i++) {
			Node attr = attributes.item(i);
			if (attr.getNodeType() == Node.ATTRIBUTE_NODE && url.equals(attr.getNamespaceURI()))
					return attr.getNodeName();
		}
		return "";
	}
	
	public static void main(String[] args) {
		try {
			String filename = "C:/Users/tmcuckie/Dropbox (Personal)/USBank Code/Code_2016_11_03/Actualize/Data/CD_6830011666.xml";
			File file = new File(filename);
			@SuppressWarnings("resource")
			String content = new Scanner(file).useDelimiter("\\Z").next();
			CalculatePayments calculator = new CalculatePayments();
			calculator.calculate(content);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
}
