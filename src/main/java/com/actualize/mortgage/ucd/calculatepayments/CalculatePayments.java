package com.actualize.mortgage.ucd.calculatepayments;

import java.io.StringReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.TreeMap;

import javax.xml.namespace.NamespaceContext;
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
import com.actualize.mortgage.domainmodels.CompositePayment;
import com.actualize.mortgage.domainmodels.FixedInterestRate;
import com.actualize.mortgage.domainmodels.InterestOnlyPayment;
import com.actualize.mortgage.domainmodels.InterestRate;
import com.actualize.mortgage.domainmodels.Loan;
import com.actualize.mortgage.domainmodels.MortgageInsurance;
import com.actualize.mortgage.domainmodels.Payment;
import com.actualize.mortgage.domainmodels.PrivateMortgageInsurance;

public class CalculatePayments {
	private static final String MISMO_URL = "http://www.mismo.org/residential/2009/schemas";
	private XPath xpath = null;
	private LinkedList<CalculationError> errors = new LinkedList<CalculationError>();

	public Document calculate(String xmldoc) {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder;  
        try  
        {  
            builder = factory.newDocumentBuilder();  
            Document doc = builder.parse(new InputSource(new StringReader(xmldoc)));
            return calculate(doc);
        } catch (Exception e) {  
            // TODO dump errors
        }
        return null;
	}

	public Document calculate(Document doc) throws NumberFormatException, XPathExpressionException {
		Node root = doc.getDocumentElement();
		xpath = createXPath(root);
		String mismo = xpath.getNamespaceContext().getPrefix(MISMO_URL);
		
		// Obtain calculation parameters
		int ioTerm = 0;
		if ("true".equals(getStringValue(root, addNamespace("//LOAN_DETAIL/InterestOnlyIndicator", mismo)))) // REQUIRED
			ioTerm = getIntegerValue(root, addNamespace("//INTEREST_ONLY/InterestOnlyTermMonthsCount", mismo), null); // REQUIRED, if InterestOnlyIndicator = 'true'
		int amortizationTerm = getIntegerValue(root, addNamespace("//MATURITY_RULE/LoanMaturityPeriodCount", mismo), null); // REQUIRED
		int loanTerm = amortizationTerm;
		if ("true".equals(getStringValue(root, addNamespace("//LOAN_DETAIL/BalloonIndicator", mismo)))) // REQUIRED
			loanTerm = 0; // REQUIRED, if BalloonIndicator = 'true'
		double loanAmount = getDoubleValue(root, addNamespace("//TERMS_OF_LOAN/NoteAmount", mismo), null); // REQUIRED
		String amortizationType = getStringValue(root, addNamespace("//AMORTIZATION/AMORTIZATION_RULE/AmortizationType", mismo)); // REQUIRED
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
		double escrow = getSumValue(root, addNamespace("//ESCROW_ITEM_DETAIL/EscrowMonthlyPaymentAmount", mismo));
		double loanCostsTotal = getDoubleValue(root, addNamespace("//INTEGRATED_DISCLOSURE_SECTION_SUMMARY_DETAIL[IntegratedDisclosureSectionType='TotalLoanCosts'][IntegratedDisclosureSubsectionType='LoanCostsSubtotal']/IntegratedDisclosureSectionTotalAmount", mismo), null);
		double aprIncludedCostsTotal = getSumValue(root, addNamespace("//ESCROW_ITEM[ESCROW_ITEM_DETAIL/EXTENSION/MISMO/PaymentIncludedInAPRIndicator='true']/ESCROW_ITEM_PAYMENTS/ESCROW_ITEM_PAYMENT[EscrowItemPaymentPaidByType='Buyer']/EscrowItemActualPaymentAmount", mismo))
			+ getSumValue(root, addNamespace("//FEE[FEE_DETAIL/EXTENSION/MISMO/PaymentIncludedInAPRIndicator='true']/FEE_PAYMENTS/FEE_PAYMENT[FeePaymentPaidByType='Buyer']/FeeActualPaymentAmount", mismo))
			+ getSumValue(root, addNamespace("//PREPAID_ITEM[PREPAID_ITEM_DETAIL/EXTENSION/MISMO/PaymentIncludedInAPRIndicator='true']/PREPAID_ITEM_PAYMENTS/PREPAID_ITEM_PAYMENT[PrepaidItemPaymentPaidByType='Buyer']/PrepaidItemActualPaymentAmount", mismo));
		double prepaidInterest = getSumValue(root, addNamespace("//PREPAID_ITEM[PREPAID_ITEM_DETAIL/PrepaidItemType='PrepaidInterest']/PREPAID_ITEM_PAYMENTS/PREPAID_ITEM_PAYMENT[PrepaidItemPaymentPaidByType='Buyer']/PrepaidItemActualPaymentAmount", mismo));
		
		// Perform calculations
		Payment payment = null;
		if (ioTerm == 0)
			payment = new AmortizingPayment(amortizationTerm);
		else if (ioTerm < loanTerm)
			payment = new CompositePayment(ioTerm, new InterestOnlyPayment(1), amortizationTerm, new AmortizingPayment(amortizationTerm - ioTerm));
		else
			payment = new InterestOnlyPayment(amortizationTerm);
		Loan loan = new Loan(loanAmount, loanTerm, payment, rate);
		double fullyIndexedRate = loan.interestRate.getInitialRate();
		MortgageInsurance insurance = createMortgageInsurance(root, mismo, loanAmount, loanTerm);
		ProjectedPayments projected = new ProjectedPayments(loan, insurance);
		LoanCalculations calcs = new LoanCalculations(loan, insurance, fullyIndexedRate, loanCostsTotal, aprIncludedCostsTotal, prepaidInterest);

		// Insert max/min's
		if ("AdjustableRate".equals(amortizationType)) {
			Node rateLifetime = getNode(root, addNamespace("//INTEREST_RATE_LIFETIME_ADJUSTMENT_RULE", mismo));
			if (rateLifetime == null)
				errors.add(new CalculationError(CalculationErrorType.INTERNAL_ERROR, "required container 'INTEREST_RATE_LIFETIME_ADJUSTMENT_RULE' is missing"));
			replace(doc, rateLifetime, addNamespace("CeilingRatePercentEarliestEffectiveMonthsCount", mismo)).appendChild(doc.createTextNode("" + (projected.maxRateFirstMonth+1)));
		}
		if ("AdjustableRate".equals(amortizationType) || ioTerm > 0) {
			Node piLifetime = getNode(root, addNamespace("//PRINCIPAL_AND_INTEREST_PAYMENT_LIFETIME_ADJUSTMENT_RULE", mismo));
			if (piLifetime == null)
				errors.add(new CalculationError(CalculationErrorType.INTERNAL_ERROR, "required container 'PRINCIPAL_AND_INTEREST_PAYMENT_LIFETIME_ADJUSTMENT_RULE' is missing"));
			replace(doc, piLifetime, addNamespace("PrincipalAndInterestPaymentMaximumAmount", mismo)).appendChild(doc.createTextNode("" + String.format("%9.2f", projected.maxPI).trim()));
			replace(doc, piLifetime, addNamespace("PrincipalAndInterestPaymentMaximumAmountEarliestEffectiveMonthsCount", mismo)).appendChild(doc.createTextNode("" + (projected.maxPIFirstMonth+1)));
		}
		
		// Insert projected payments
		Node integratedDisclosure = getNode(root, addNamespace("//INTEGRATED_DISCLOSURE", mismo));
		if (integratedDisclosure == null)
			errors.add(new CalculationError(CalculationErrorType.INTERNAL_ERROR, "required container 'INTEGRATED_DISCLOSURE' is missing"));
		Node projectedPayments = replace(doc, integratedDisclosure, addNamespace("PROJECTED_PAYMENTS", mismo));
		for (int i = 0; i < projected.payments.length; i++) {
			Element projectedPayment = (Element)projectedPayments.appendChild(doc.createElement(addNamespace("PROJECTED_PAYMENT", mismo)));
			projectedPayment.setAttribute("SequenceNumber", ""+(i+1));
			double mi = projected.payments[i].getMi();
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
			projectedPayment.appendChild(doc.createElement(addNamespace("ProjectedPaymentMIPaymentAmount", mismo))).appendChild(doc.createTextNode(String.format("%9.2f", mi).trim()));
			projectedPayment.appendChild(doc.createElement(addNamespace("ProjectedPaymentPrincipalAndInterestMaximumPaymentAmount", mismo))).appendChild(doc.createTextNode(String.format("%9.2f", maxPI).trim()));
			if (maxPI != minPI)
				projectedPayment.appendChild(doc.createElement(addNamespace("ProjectedPaymentPrincipalAndInterestMinimumPaymentAmount", mismo))).appendChild(doc.createTextNode(String.format("%9.2f", minPI).trim()));
		}

		// Insert calculations
		Node feeInformation = getNode(root, addNamespace("//FEE_INFORMATION", mismo));
		if (feeInformation == null)
			errors.add(new CalculationError(CalculationErrorType.INTERNAL_ERROR, "required container 'FEE_INFORMATION' is missing"));
		Node feesSummary = replace(doc, feeInformation, addNamespace("FEES_SUMMARY", mismo));
		Node feesSummaryDetail = feesSummary.appendChild(doc.createElement(addNamespace("FEE_SUMMARY_DETAIL", mismo)));
		feesSummaryDetail.appendChild(doc.createElement(addNamespace("APRPercent", mismo))).appendChild(doc.createTextNode(String.format("%7.4f", calcs.apr).trim()));
		feesSummaryDetail.appendChild(doc.createElement(addNamespace("FeeSummaryTotalAmountFinancedAmount", mismo))).appendChild(doc.createTextNode(String.format("%9.2f", calcs.amountFinanced).trim()));
		feesSummaryDetail.appendChild(doc.createElement(addNamespace("FeeSummaryTotalFinanceChargeAmount", mismo))).appendChild(doc.createTextNode(String.format("%9.2f", calcs.financeCharge).trim()));
		feesSummaryDetail.appendChild(doc.createElement(addNamespace("FeeSummaryTotalInterestPercent", mismo))).appendChild(doc.createTextNode(String.format("%9.2f", calcs.totalInterestPercentage).trim()));
		feesSummaryDetail.appendChild(doc.createElement(addNamespace("FeeSummaryTotalOfAllPaymentsAmount", mismo))).appendChild(doc.createTextNode(String.format("%9.2f", calcs.totalOfPayments).trim()));
		
		return doc;
	}

	public CalculationError[] getErrors() {
		return errors.toArray(new CalculationError[errors.size()]);
	}
	
	private InterestRate createAdjustableInterestRate(Node root, String mismo) {
		double initialRate = getDoubleValue(root, addNamespace("//TERMS_OF_LOAN/NoteRatePercent", mismo), null) / 100.0; // REQUIRED, unless DisclosedFullyIndexedRatePercent is present
		if (initialRate == 0)
			initialRate = getDoubleValue(root, addNamespace("//TERMS_OF_LOAN/DisclosedFullyIndexedRatePercent", mismo), null) / 100.0; // REQUIRED, if AmortizationType=AdjustableRate
		int firstResetTerm = getIntegerValue(root, addNamespace("//INTEREST_RATE_LIFETIME_ADJUSTMENT_RULE/FirstRateChangeMonthsCount", mismo), null) - 1; // REQUIRED, if AmortizationType=AdjustableRate
		int subsequentResetTerm = getIntegerValue(root, addNamespace("//INTEREST_RATE_PER_CHANGE_ADJUSTMENT_RULE[AdjustmentRuleType='First']/PerChangeRateAdjustmentFrequencyMonthsCount", mismo), null); // REQUIRED, if AmortizationType=AdjustableRate
		double firstResetCap = getDoubleValue(root, addNamespace("//INTEREST_RATE_PER_CHANGE_ADJUSTMENT_RULE[AdjustmentRuleType='First']/PerChangeMaximumIncreaseRatePercent", mismo), null) / 100.0; // REQUIRED, if AmortizationType=AdjustableRate
		double subsequentResetCap = getDoubleValue(root, addNamespace("//INTEREST_RATE_PER_CHANGE_ADJUSTMENT_RULE[AdjustmentRuleType='Subsequent']/PerChangeMaximumIncreaseRatePercent", mismo), null) / 100.0; // REQUIRED, if AmortizationType=AdjustableRate
		double lifetimeCap = getDoubleValue(root, addNamespace("//INTEREST_RATE_LIFETIME_ADJUSTMENT_RULE/CeilingRatePercent", mismo), null) / 100.0; // REQUIRED, if AmortizationType=AdjustableRate
		double lifetimeFloor = getDoubleValue(root, addNamespace("//INTEREST_RATE_LIFETIME_ADJUSTMENT_RULE/FloorRatePercent", mismo), null) / 100.0; // REQUIRED, if AmortizationType=AdjustableRate
		return new AdjustableInterestRate(initialRate, firstResetTerm, subsequentResetTerm, firstResetCap, subsequentResetCap, lifetimeCap, firstResetCap, subsequentResetCap, lifetimeFloor); // REQUIRED, if AmortizationType=AdjustableRate
	}
	
	private InterestRate createFixedInterestRate(Node root, String mismo) {
		double rate = getDoubleValue(root, addNamespace("//TERMS_OF_LOAN/NoteRatePercent", mismo), null) / 100.0; // REQUIRED, if AmortizationType=Fixed
		return new FixedInterestRate(rate);
	}
	
	private MortgageInsurance createMortgageInsurance(Node root, String mismo, double loanAmount, int loanTerm) {
		int miAmount = getIntegerValue(root, addNamespace("//PROJECTED_PAYMENT/ProjectedPaymentMIPaymentAmount", mismo), 0); // REQUIRED, if MI exists
		if (miAmount == 0)
			return null;
		String miTerminationDate = getStringValue(root, addNamespace("//MI_DATA_DETAIL/MIScheduledTerminationDate", mismo)); // REQUIRED, if terminating MI at a scheduled date
		String closingDate = getStringValue(root, addNamespace("//CLOSING_INFORMATION_DETAIL/ClosingDate", mismo)); // REQUIRED, if terminating MI at a scheduled date
		if ("".equals(miTerminationDate) || "".equals(closingDate)) {
			double homeValue = getDoubleValue(root, addNamespace("//SALES_CONTRACT_DETAIL/SalesContractAmount", mismo), 0.0);
			if (homeValue == 0.0)
				homeValue = getDoubleValue(root, addNamespace("//SALES_CONTRACT_DETAIL/RealPropertyAmount", mismo), 0.0);
			if (homeValue == 0.0)
				homeValue = getDoubleValue(root, addNamespace("//PROPERTY_DETAIL/PropertyEstimatedValueAmount", mismo), 0.0);
			return new PrivateMortgageInsurance(homeValue, loanTerm, miAmount*12/loanAmount, 0, 0, 0, 0);
		}
		return new PrivateMortgageInsurance(calculateMIDuration(closingDate, miTerminationDate), loanTerm, miAmount*12/loanAmount, 0, 0, 0, 0);
	}
	
	private int calculateMIDuration(String closingDate, String miTerminationDate) {
		SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd");
		SimpleDateFormat yearFormatter = new SimpleDateFormat("yyyy");
		SimpleDateFormat monthFormatter = new SimpleDateFormat("MM");
		Date closingDt;
		Date miTerminationDt = null;
		int fromMonth;
		int fromYear;
		int toMonth;
		int toYear;
		try {
			closingDt = dateFormatter.parse(closingDate);
			fromMonth = Integer.parseInt(monthFormatter.format(closingDt));
			fromYear = Integer.parseInt(yearFormatter.format(closingDt));
		} catch (ParseException e) {
			errors.add(new CalculationError(CalculationErrorType.INTERNAL_ERROR, "bad closing date '" + closingDate + "'"));
			return 0;
		}
		try {
			miTerminationDt = dateFormatter.parse(miTerminationDate);
			toMonth = Integer.parseInt(monthFormatter.format(miTerminationDt));
			toYear = Integer.parseInt(yearFormatter.format(miTerminationDt));
		} catch (ParseException e) {
			errors.add(new CalculationError(CalculationErrorType.INTERNAL_ERROR, "bad closing date '" + miTerminationDt + "'"));
			return 0;
		}
		return 12*(toYear - fromYear) + toMonth - fromMonth;
	}
	
	private double getSumValue(Node node, String expression) {
		try {
			return (Double)xpath.evaluate("sum(" + expression + ")", node, XPathConstants.NUMBER);
		} catch (XPathExpressionException e) {
			errors.add(new CalculationError(CalculationErrorType.INTERNAL_ERROR, "bad xpath expression '" + expression + "'"));
			return 0;
		}
		
	}
	
	private double getDoubleValue(Node node, String expression, Double dflt) {
		try {
			return Double.parseDouble(getStringValue(node, expression));
		} catch (NumberFormatException e) {
			if (dflt != null)
				return dflt;
			errors.add(new CalculationError(CalculationErrorType.MISSING_DATA, "required datapoint '" + expression + "' is missing"));
			return 0;
		}
	}
	
	private int getIntegerValue(Node node, String expression, Integer dflt) {
		try {
			return Integer.parseInt(getStringValue(node, expression));
		} catch (NumberFormatException e) {
			if (dflt != null)
				return dflt;
			errors.add(new CalculationError(CalculationErrorType.MISSING_DATA, "required datapoint '" + expression + "' is missing"));
			return 0;
		}
	}
	
	private String getStringValue(Node node, String expression) {
		try {
			return (String)xpath.evaluate(expression, node, XPathConstants.STRING);
		} catch (XPathExpressionException e) {
			errors.add(new CalculationError(CalculationErrorType.INTERNAL_ERROR, "bad xpath expression '" + expression + "'"));
			return "";
		}
	}
	
	private Node getNode(Node node, String expression) {
		NodeList nodeList = getNodeList(node, expression);
		if (nodeList.getLength() > 0)
			return nodeList.item(0);
		return null;
	}
	
	private NodeList getNodeList(Node node, String expression) {
		try {
			return (NodeList)xpath.evaluate(expression, node, XPathConstants.NODESET);
		} catch (XPathExpressionException e) {
			errors.add(new CalculationError(CalculationErrorType.INTERNAL_ERROR, "bad xpath expression '" + expression + "'"));
			return null;
		}
	}
	
	private static Node findLocation(Node node, Node child) {
		for (Node n = node.getFirstChild(); n != null; n = n.getNextSibling())
			if (child.getNodeName().compareTo(n.getNodeName()) < 0)
				return n;
		return null;
	}
	
	private static XPath createXPath(Node root) {
		XPath xpath = XPathFactory.newInstance().newXPath();
		NamedNodeMap attributes = root.getAttributes();
		TreeMap<String, String> nsPrefixToURI = new TreeMap<String, String>();
		TreeMap<String, String> nsURIToPrefix = new TreeMap<String, String>();
		for (int i = 0; i < attributes.getLength(); i++) {
			Node attr = attributes.item(i);
			String prefix = attr.getNodeName();
			if (attr.getNodeType() == Node.ATTRIBUTE_NODE && prefix.startsWith("xmlns")) {
				prefix = prefix.indexOf(':')==-1 ? "" : prefix.substring(6);
				nsPrefixToURI.put(prefix, attr.getNodeValue());
				nsURIToPrefix.put(attr.getNodeValue(), prefix);
			}
		}
		xpath.setNamespaceContext(new NamespaceContext() {
			private TreeMap<String, String> toURI = nsPrefixToURI;
			private TreeMap<String, String> toPrefix = nsURIToPrefix;

			@Override
			public String getNamespaceURI(String prefix) {
				return toURI.get(prefix);
			}

			@Override
			public String getPrefix(String namespaceURI) {
				return toPrefix.get(namespaceURI);
			}

			@SuppressWarnings("rawtypes")
			@Override
			public Iterator getPrefixes(String namespaceURI) {
				throw new IllegalAccessError("Not implemented!");
			}
		});
		return xpath;
	}
	
	private static String addNamespace(String path, String namespace) {
		if ("".equals(namespace))
			return path;
		String[] outer = path.split("/");
		for (int i = 0; i < outer.length; i++) {
			String[] inner = outer[i].split("\\[");
			for (int j = 0; j < inner.length; j++)
				if (!"".equals(inner[j]) && inner[j].indexOf(':') == -1)
					inner[j] = namespace + ":" + inner[j];
			outer[i] = String.join("[", inner);
		}
		return String.join("/", outer);
	}
	
	private Node replace(Document doc, Node parent, String name) {
		Node oldNode = getNode(parent, name);
		Node newNode = doc.createElement(name);
		if (oldNode == null)
			parent.insertBefore(newNode, findLocation(parent, newNode));
		else
			parent.replaceChild(newNode, oldNode);
		return newNode;
	}
}
