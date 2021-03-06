package com.actualize.mortgage.ucd.calculatepayments;

import java.io.StringReader;
import java.io.StringWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
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
import com.actualize.mortgage.domainmodels.CashFlowInfo;
import com.actualize.mortgage.domainmodels.CompositePayment;
import com.actualize.mortgage.domainmodels.ErrorModel;
import com.actualize.mortgage.domainmodels.ErrorsListModel;
import com.actualize.mortgage.domainmodels.FixedInterestRate;
import com.actualize.mortgage.domainmodels.InterestOnlyPayment;
import com.actualize.mortgage.domainmodels.InterestRate;
import com.actualize.mortgage.domainmodels.Loan;
import com.actualize.mortgage.domainmodels.MortgageInsurance;
import com.actualize.mortgage.domainmodels.Payment;
import com.actualize.mortgage.domainmodels.PrivateMortgageInsurance;
import com.actualize.mortgage.ucd.calculationutils.CalculationErrorType;

public class CalculatePayments {
    private static final String MISMO_URL = "http://www.mismo.org/residential/2009/schemas";
    private static final String GSE_URL = "http://www.datamodelextension.org";
    private XPath xpath = null;
    private LinkedList<CalculationError> errors = new LinkedList<CalculationError>();

    public Document calculate(String xmldoc) throws Exception {
        try  
        {  
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            Document doc = factory.newDocumentBuilder().parse(new InputSource(new StringReader(xmldoc)));
            xpath = createXPath(doc.getDocumentElement());
            doc = calculate(doc);
            if (errors.isEmpty())
                return doc;
        } catch (Exception e) {
            errors.add(new CalculationError(CalculationErrorType.INTERNAL_ERROR, "undefined calculation error"));
        }
        return marshallErrors();
    }
    
    public Document calculate(Document doc) throws NumberFormatException, XPathExpressionException {
        Node root = doc.getDocumentElement();
        String mismo = xpath.getNamespaceContext().getPrefix(MISMO_URL);
        
        // Create loan
        Loan loan = createLoan(root, mismo);

        // Create MI model
        MortgageInsurance insurance = createMortgageInsurance(root, mismo, loan.loanAmount, loan.loanTerm);

        // Run calculations
        LoanCalculations calcs = new LoanCalculations(loan, insurance, loan.interestRate.getInitialRate(), calculateLoanCosts(root, mismo), calculateAprCosts(root, mismo), calculatePrepaidInterest(root, mismo));
        PaymentChanges changes = new PaymentChanges(loan);
        ProjectedPayments projected = new ProjectedPayments(loan, insurance);

        // Update document with calculation results
        updatePaymentRule(doc, mismo, projected);
        updateInterestRateLifetimeAdjustmentRule(doc, mismo, loan, changes);
        updatePrincipalAndInterestPaymentLifetimeAdjustmentRule(doc, mismo, loan, changes);
        updatePrincipalAndInterestPaymentPerChangeAdjustmentRule(doc, mismo, loan, projected);
        updateProjectedPayment(doc, mismo, projected);
        updateFeeSummaryDetail(doc, mismo, calcs);
        updateIntegratedDisclosureDetail(doc, mismo, calcs);
        return doc;
    }

    public CalculationError[] getErrors() {
        return errors.toArray(new CalculationError[errors.size()]);
    }
    
    private double calculateLoanCosts(Node root, String mismo) {
        return getSumValue(root, addNamespace("//INTEGRATED_DISCLOSURE_SECTION_SUMMARY_DETAIL[IntegratedDisclosureSectionType='TotalLoanCosts'][IntegratedDisclosureSubsectionType='LoanCostsSubtotal']/IntegratedDisclosureSectionTotalAmount", mismo));
    }
    
    private double calculateAprCosts(Node root, String mismo) {
        if (isDocTypeLoanEstimate(root.getOwnerDocument(), mismo))
            return getSumValue(root, addNamespace("//ESCROW_ITEM/ESCROW_ITEM_DETAIL[EXTENSION/MISMO/PaymentIncludedInAPRIndicator='true']/EscrowItemEstimatedTotalAmount", mismo))
                    + getSumValue(root, addNamespace("//FEE/FEE_DETAIL[EXTENSION/MISMO/PaymentIncludedInAPRIndicator='true']/FeeEstimatedTotalAmount", mismo))
                    + getSumValue(root, addNamespace("//PREPAID_ITEM/PREPAID_ITEM_DETAIL[EXTENSION/MISMO/PaymentIncludedInAPRIndicator='true']/PrepaidItemEstimatedTotalAmount", mismo));
        return getSumValue(root, addNamespace("//ESCROW_ITEM[ESCROW_ITEM_DETAIL/EXTENSION/MISMO/PaymentIncludedInAPRIndicator='true']/ESCROW_ITEM_PAYMENTS/ESCROW_ITEM_PAYMENT[EscrowItemPaymentPaidByType='Buyer']/EscrowItemActualPaymentAmount", mismo))
                + getSumValue(root, addNamespace("//FEE[FEE_DETAIL/EXTENSION/MISMO/PaymentIncludedInAPRIndicator='true']/FEE_PAYMENTS/FEE_PAYMENT[FeePaymentPaidByType='Buyer']/FeeActualPaymentAmount", mismo))
                + getSumValue(root, addNamespace("//PREPAID_ITEM[PREPAID_ITEM_DETAIL/EXTENSION/MISMO/PaymentIncludedInAPRIndicator='true']/PREPAID_ITEM_PAYMENTS/PREPAID_ITEM_PAYMENT[PrepaidItemPaymentPaidByType='Buyer']/PrepaidItemActualPaymentAmount", mismo));
    }
    
    private double calculatePrepaidInterest(Node root, String mismo) {
        if (isDocTypeLoanEstimate(root.getOwnerDocument(), mismo))
            return getSumValue(root, addNamespace("//PREPAID_ITEM/PREPAID_ITEM_DETAIL[PrepaidItemType='PrepaidInterest']/PrepaidItemEstimatedTotalAmount", mismo));
        return getSumValue(root, addNamespace("//PREPAID_ITEM[PREPAID_ITEM_DETAIL/PrepaidItemType='PrepaidInterest']/PREPAID_ITEM_PAYMENTS/PREPAID_ITEM_PAYMENT[PrepaidItemPaymentPaidByType='Buyer']/PrepaidItemActualPaymentAmount", mismo));
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

    private boolean isDocTypeLoanEstimate(Document doc, String mismo) {
        NodeList nodes = getNodeList(doc.getDocumentElement(), addNamespace("//DOCUMENT_CLASSIFICATION/DOCUMENT_CLASSES/DOCUMENT_CLASS", mismo));
        for (int i = 0; i < nodes.getLength(); i++) {
            String doctype = getStringValue(nodes.item(i), addNamespace("DocumentTypeOtherDescription", mismo));
            if (doctype.startsWith("ClosingDisclosure:"))
                return false;
            else if (doctype.startsWith("LoanEstimate:"))
                return true;
        }
        return false;
    }
    
    private InterestRate createAdjustableInterestRateModel(Node root, String mismo) {
        double initialRate = getDoubleValue(root, addNamespace("//TERMS_OF_LOAN/DisclosedFullyIndexedRatePercent", mismo), null) / 100.0; // REQUIRED, if AmortizationType=AdjustableRate
        int firstResetTerm = getIntegerValue(root, addNamespace("//INTEREST_RATE_LIFETIME_ADJUSTMENT_RULE/FirstRateChangeMonthsCount", mismo), null); // REQUIRED, if AmortizationType=AdjustableRate
        int subsequentResetTerm = getIntegerValue(root, addNamespace("//INTEREST_RATE_PER_CHANGE_ADJUSTMENT_RULE[AdjustmentRuleType='First']/PerChangeRateAdjustmentFrequencyMonthsCount", mismo), null); // REQUIRED, if AmortizationType=AdjustableRate
        double firstResetCap = getDoubleValue(root, addNamespace("//INTEREST_RATE_PER_CHANGE_ADJUSTMENT_RULE[AdjustmentRuleType='First']/PerChangeMaximumIncreaseRatePercent", mismo), null) / 100.0; // REQUIRED, if AmortizationType=AdjustableRate
        double subsequentResetCap = getDoubleValue(root, addNamespace("//INTEREST_RATE_PER_CHANGE_ADJUSTMENT_RULE[AdjustmentRuleType='Subsequent']/PerChangeMaximumIncreaseRatePercent", mismo), null) / 100.0; // REQUIRED, if AmortizationType=AdjustableRate
        double lifetimeCap = getDoubleValue(root, addNamespace("//INTEREST_RATE_LIFETIME_ADJUSTMENT_RULE/CeilingRatePercent", mismo), null) / 100.0; // REQUIRED, if AmortizationType=AdjustableRate
        double lifetimeFloor = getDoubleValue(root, addNamespace("//INTEREST_RATE_LIFETIME_ADJUSTMENT_RULE/FloorRatePercent", mismo), null) / 100.0; // REQUIRED, if AmortizationType=AdjustableRate
        return new AdjustableInterestRate(initialRate, firstResetTerm, subsequentResetTerm, firstResetCap, subsequentResetCap, lifetimeCap, firstResetCap, subsequentResetCap, lifetimeFloor); // REQUIRED, if AmortizationType=AdjustableRate
    }
    
    private InterestRate createFixedInterestRateModel(Node root, String mismo) {
        double rate = getDoubleValue(root, addNamespace("//TERMS_OF_LOAN/NoteRatePercent", mismo), null) / 100.0; // REQUIRED, if AmortizationType=Fixed
        return new FixedInterestRate(rate);
    }
    
    private Loan createLoan(Node root, String mismo) {
        String gse = xpath.getNamespaceContext().getPrefix(GSE_URL);
    
        // Validate input fields
        if ("true".equals(getStringValue(root, addNamespace("//LOAN_DETAIL/SeasonalPaymentFeatureIndicator", mismo))))
            errors.add(new CalculationError(CalculationErrorType.NOT_IMPLEMENTED, "seasonal payment feature not supported"));
        if (!"".equals(getStringValue(root, addNamespace("//TotalStepPaymentCount", gse))))
            errors.add(new CalculationError(CalculationErrorType.NOT_IMPLEMENTED, "step payment feature not supported"));
        if ("true".equals(getStringValue(root, addNamespace("//PAYMENT/PAYMENT_RULE/PaymentOptionIndicator", mismo))))
            errors.add(new CalculationError(CalculationErrorType.NOT_IMPLEMENTED, "optional payment feature not supported"));
        
        // Obtain loan amount
        double loanAmount = getDoubleValue(root, addNamespace("//TERMS_OF_LOAN/NoteAmount", mismo), null); // REQUIRED
    
        // Get io term
        int ioTerm = 0;
        if ("true".equals(getStringValue(root, addNamespace("//LOAN_DETAIL/InterestOnlyIndicator", mismo)))) // REQUIRED
            ioTerm = getIntegerValue(root, addNamespace("//INTEREST_ONLY/InterestOnlyTermMonthsCount", mismo), null); // REQUIRED, if InterestOnlyIndicator = 'true'
        
        // Obtain loan and amortization terms
        int loanTerm = getIntegerValue(root, addNamespace("//MATURITY_RULE/LoanMaturityPeriodCount", mismo), null); // REQUIRED
        int amortizationTerm = loanTerm;
        if ("true".equals(getStringValue(root, addNamespace("//LOAN_DETAIL/BalloonIndicator", mismo)))) { // REQUIRED
            amortizationTerm = getIntegerValue(root, addNamespace("//AMORTIZATION_RULE/LoanAmortizationPeriodCount", mismo), null); // REQUIRED, if BalloonIndicator = 'true'
            String amortizationPeriodType = getStringValue(root, addNamespace("//AMORTIZATION_RULE/LoanAmortizationPeriodType", mismo));
            switch (amortizationPeriodType) {
            case "Month":
                break;
            case "Year":
                amortizationTerm *= 12;
                break;
            default:
                errors.add(new CalculationError(CalculationErrorType.NOT_IMPLEMENTED, "amortization period type '" + amortizationPeriodType + "' not supported"));
            }
        }
        
        // Create payment model
        Payment payment = null;
        if (ioTerm == 0)
            payment = new AmortizingPayment(amortizationTerm);
        else if (ioTerm < loanTerm)
            payment = new CompositePayment(ioTerm, new InterestOnlyPayment(1), amortizationTerm, new AmortizingPayment(amortizationTerm - ioTerm));
        else
            payment = new InterestOnlyPayment(amortizationTerm);
        
        // Create interest rate model
        String amortizationType = getStringValue(root, addNamespace("//AMORTIZATION_RULE/AmortizationType", mismo)); // REQUIRED
        InterestRate rate = null;
        switch (amortizationType) {
            case "Fixed":
                rate = createFixedInterestRateModel(root, mismo);
                break;
            case "AdjustableRate":
                rate = createAdjustableInterestRateModel(root, mismo);
                break;
            default:
                errors.add(new CalculationError(CalculationErrorType.NOT_IMPLEMENTED, "amortization type '" + amortizationType + "' not supported"));
        }
    
        return new Loan(loanAmount, loanTerm, payment, rate);
    }

    private MortgageInsurance createMortgageInsurance(Node root, String mismo, double loanAmount, int loanTerm) {
        int duration1 = getIntegerValue(root, addNamespace("//MI_DATA/MI_PREMIUMS/MI_PREMIUM/MI_PREMIUM_DETAIL[MIPremiumPeriodType='First']/MIPremiumRateDurationMonthsCount", mismo), 0);
        double factor1 = getDoubleValue(root, addNamespace("//MI_DATA/MI_PREMIUMS/MI_PREMIUM/MI_PREMIUM_DETAIL[MIPremiumPeriodType='First']/MIPremiumRatePercent", mismo), 0.0)/1200;
        int duration2 = getIntegerValue(root, addNamespace("//MI_DATA/MI_PREMIUMS/MI_PREMIUM/MI_PREMIUM_DETAIL[MIPremiumPeriodType='Second']/MIPremiumRateDurationMonthsCount", mismo), 0);
        double factor2 = getDoubleValue(root, addNamespace("//MI_DATA/MI_PREMIUMS/MI_PREMIUM/MI_PREMIUM_DETAIL[MIPremiumPeriodType='Second']/MIPremiumRatePercent", mismo), 0.0)/1200;
        int duration3 = getIntegerValue(root, addNamespace("//MI_DATA/MI_PREMIUMS/MI_PREMIUM/MI_PREMIUM_DETAIL[MIPremiumPeriodType='Third']/MIPremiumRateDurationMonthsCount", mismo), 0);
        double factor3 = getDoubleValue(root, addNamespace("//MI_DATA/MI_PREMIUMS/MI_PREMIUM/MI_PREMIUM_DETAIL[MIPremiumPeriodType='Third']/MIPremiumRatePercent", mismo), 0.0)/1200;
        if (duration1 == 0) {
            double miAmount = getDoubleValue(root, addNamespace("//PROJECTED_PAYMENT/ProjectedPaymentMIPaymentAmount", mismo), 0.0); // REQUIRED, if MI exists
            factor1 = miAmount/loanAmount;
            duration1 = loanTerm;
        }
        String miTerminationDate = getStringValue(root, addNamespace("//MI_DATA_DETAIL/MIScheduledTerminationDate", mismo)); // REQUIRED, if terminating MI at a scheduled date
        String closingDate = getStringValue(root, addNamespace("//CLOSING_INFORMATION_DETAIL/ClosingDate", mismo)); // REQUIRED, if terminating MI at a scheduled date
        if ("".equals(miTerminationDate) || "".equals(closingDate)) {
            double homeValue = getDoubleValue(root, addNamespace("//SALES_CONTRACT_DETAIL/SalesContractAmount", mismo), 0.0);
            if (homeValue == 0.0)
                homeValue = getDoubleValue(root, addNamespace("//SALES_CONTRACT_DETAIL/RealPropertyAmount", mismo), 0.0);
            if (homeValue == 0.0)
                homeValue = getDoubleValue(root, addNamespace("//PROPERTY_DETAIL/PropertyEstimatedValueAmount", mismo), 0.0);
            return new PrivateMortgageInsurance(homeValue, duration1, factor1, duration2, factor2, duration3, factor3);
        }
        return new PrivateMortgageInsurance(calculateMIDuration(closingDate, miTerminationDate), duration1, factor1, duration2, factor2, duration3, factor3);
    }
    
    private void updatePaymentRule(Document doc, String mismo, ProjectedPayments projected) {
        Node paymentRule = constructNodePath(doc.getDocumentElement(), addNamespace("//LOAN/PAYMENT/PAYMENT_RULE", mismo));
        if (paymentRule == null)
            errors.add(new CalculationError(CalculationErrorType.INTERNAL_ERROR, "data point 'PAYMENT_RULE' can't be inserted"));
        if (projected.payments.length == 0)
            errors.add(new CalculationError(CalculationErrorType.INTERNAL_ERROR, "no projected payments generated"));
        replaceNode(doc, paymentRule, addNamespace("InitialPrincipalAndInterestPaymentAmount", mismo)).appendChild(doc.createTextNode(String.format("%9.2f", projected.payments[0].getHighPI()).trim()));
    }

    private void updateInterestRateLifetimeAdjustmentRule(Document doc, String mismo, Loan loan, PaymentChanges changes) {
    	String interestRateType = loan.interestRate.getClass().getName();
        if (interestRateType.contains("AdjustableInterestRate")) {
            Node interestRateLifetimeAdjustment = constructNodePath(doc.getDocumentElement(), addNamespace("//INTEREST_RATE_LIFETIME_ADJUSTMENT_RULE", mismo));
            if (interestRateLifetimeAdjustment == null)
                errors.add(new CalculationError(CalculationErrorType.INTERNAL_ERROR, "data point 'INTEREST_RATE_LIFETIME_ADJUSTMENT_RULE' can't be inserted"));
            Node earliestCeilingRate = replaceNode(doc, interestRateLifetimeAdjustment, addNamespace("CeilingRatePercentEarliestEffectiveMonthsCount", mismo));
            earliestCeilingRate.appendChild(doc.createTextNode("" + (changes.maxRateFirstMonth+1)));
        }
    }

    private void updatePrincipalAndInterestPaymentLifetimeAdjustmentRule(Document doc, String mismo, Loan loan, PaymentChanges changes) {
    	String interestRateType = loan.interestRate.getClass().getName();
        if (interestRateType.contains("AdjustableInterestRate")) {
            int firstReset = ((AdjustableInterestRate)loan.interestRate).firstReset;
            Node piLifetime = constructNodePath(doc.getDocumentElement(), addNamespace("//LOAN/ADJUSTMENT/PRINCIPAL_AND_INTEREST_PAYMENT_ADJUSTMENT/PRINCIPAL_AND_INTEREST_PAYMENT_LIFETIME_ADJUSTMENT_RULE", mismo));
            if (piLifetime == null)
                errors.add(new CalculationError(CalculationErrorType.INTERNAL_ERROR, "required container 'PRINCIPAL_AND_INTEREST_PAYMENT_LIFETIME_ADJUSTMENT_RULE' is missing and can't be inserted"));
            piLifetime = replaceNode(doc, piLifetime.getParentNode(), addNamespace("PRINCIPAL_AND_INTEREST_PAYMENT_LIFETIME_ADJUSTMENT_RULE", mismo));
            piLifetime.appendChild(doc.createElement(addNamespace("FirstPrincipalAndInterestPaymentChangeMonthsCount", mismo))).appendChild(doc.createTextNode("" + (firstReset+1)));
            piLifetime.appendChild(doc.createElement(addNamespace("PrincipalAndInterestPaymentMaximumAmount", mismo))).appendChild(doc.createTextNode(String.format("%9.2f", changes.maxPI).trim()));
            piLifetime.appendChild(doc.createElement(addNamespace("PrincipalAndInterestPaymentMaximumAmountEarliestEffectiveMonthsCount", mismo))).appendChild(doc.createTextNode("" + (changes.maxPIFirstMonth+1)));
        }
    }

    private void updatePrincipalAndInterestPaymentPerChangeAdjustmentRule(Document doc, String mismo, Loan loan, ProjectedPayments projected) {
    	String interestRateType = loan.interestRate.getClass().getName();
    	String initialPaymentType = loan.payment.getPayment(0).getClass().getName();
        if (interestRateType.contains("AdjustableInterestRate")) {
            int firstReset = ((AdjustableInterestRate)loan.interestRate).firstReset;
            int subsequentReset = ((AdjustableInterestRate)loan.interestRate).subsequentReset;
            double maxPI = projected.high.getValue(firstReset, CashFlowInfo.PRINCIPAL_AND_INTEREST_PAYMENT);
            double minPI = projected.low.getValue(firstReset, CashFlowInfo.PRINCIPAL_AND_INTEREST_PAYMENT);
            Node pAndIAdjustment = getNode(doc.getDocumentElement(), addNamespace("//LOAN/ADJUSTMENT/PRINCIPAL_AND_INTEREST_PAYMENT_ADJUSTMENT", mismo));
            if (pAndIAdjustment == null)
                errors.add(new CalculationError(CalculationErrorType.INTERNAL_ERROR, "required container 'PRINCIPAL_AND_INTEREST_PAYMENT_ADJUSTMENT' is missing and can not be inserted"));
            Node pAndIPerChangeRules = replaceNode(doc, pAndIAdjustment, addNamespace("PRINCIPAL_AND_INTEREST_PAYMENT_PER_CHANGE_ADJUSTMENT_RULES", mismo));
            Node firstPAndIAdjustment = pAndIPerChangeRules.appendChild(doc.createElement(addNamespace("PRINCIPAL_AND_INTEREST_PAYMENT_PER_CHANGE_ADJUSTMENT_RULE", mismo)));
            firstPAndIAdjustment.appendChild(doc.createElement(addNamespace("AdjustmentRuleType", mismo))).appendChild(doc.createTextNode("First"));
            firstPAndIAdjustment.appendChild(doc.createElement(addNamespace("PerChangeMaximumPrincipalAndInterestPaymentAmount", mismo))).appendChild(doc.createTextNode(String.format("%9.2f", maxPI).trim()));
            if (maxPI != minPI)
                firstPAndIAdjustment.appendChild(doc.createElement(addNamespace("PerChangeMinimumPrincipalAndInterestPaymentAmount", mismo))).appendChild(doc.createTextNode(String.format("%9.2f", minPI).trim()));
            firstPAndIAdjustment.appendChild(doc.createElement(addNamespace("PerChangePrincipalAndInterestPaymentAdjustmentFrequencyMonthsCount", mismo))).appendChild(doc.createTextNode("" + subsequentReset));
            Node subsequentPAndIAdjustment = pAndIPerChangeRules.appendChild(doc.createElement(addNamespace("PRINCIPAL_AND_INTEREST_PAYMENT_PER_CHANGE_ADJUSTMENT_RULE", mismo)));
            subsequentPAndIAdjustment.appendChild(doc.createElement(addNamespace("AdjustmentRuleType", mismo))).appendChild(doc.createTextNode("Subsequent"));
            subsequentPAndIAdjustment.appendChild(doc.createElement(addNamespace("PerChangePrincipalAndInterestPaymentAdjustmentFrequencyMonthsCount", mismo))).appendChild(doc.createTextNode("" + subsequentReset));
        }
        if (initialPaymentType.contains("InterestOnlyPayment")) {
            int ioTerm = ((CompositePayment)loan.payment).paymentSegments[0].endPeriod;
            double maxPI = projected.high.getValue(ioTerm, CashFlowInfo.PRINCIPAL_AND_INTEREST_PAYMENT);
            Node pAndIAdjustment = constructNodePath(doc.getDocumentElement(), addNamespace("//LOAN/ADJUSTMENT/PRINCIPAL_AND_INTEREST_PAYMENT_ADJUSTMENT", mismo));
            if (pAndIAdjustment == null)
                errors.add(new CalculationError(CalculationErrorType.INTERNAL_ERROR, "required container 'PRINCIPAL_AND_INTEREST_PAYMENT_ADJUSTMENT' is missing and can not be inserted"));
            Node pAndIPerChangeRules = replaceNode(doc, pAndIAdjustment, addNamespace("PRINCIPAL_AND_INTEREST_PAYMENT_PER_CHANGE_ADJUSTMENT_RULES", mismo));
            Node firstPAndIAdjustment = pAndIPerChangeRules.appendChild(doc.createElement(addNamespace("PRINCIPAL_AND_INTEREST_PAYMENT_PER_CHANGE_ADJUSTMENT_RULE", mismo)));
            firstPAndIAdjustment.appendChild(doc.createElement(addNamespace("AdjustmentRuleType", mismo))).appendChild(doc.createTextNode("First"));
            firstPAndIAdjustment.appendChild(doc.createElement(addNamespace("PerChangeMaximumPrincipalAndInterestPaymentAmount", mismo))).appendChild(doc.createTextNode(String.format("%9.2f", maxPI).trim()));
            firstPAndIAdjustment.appendChild(doc.createElement(addNamespace("PerChangePrincipalAndInterestPaymentAdjustmentFrequencyMonthsCount", mismo))).appendChild(doc.createTextNode("" + ioTerm));
        }
    
    }

    private void updateProjectedPayment(Document doc, String mismo, ProjectedPayments projected) {
        Node root = doc.getDocumentElement();
        double escrow = getSumValue(root, addNamespace("//ESCROW_ITEM_DETAIL/EscrowMonthlyPaymentAmount", mismo));
        Node integratedDisclosure = constructNodePath(root, addNamespace("//LOAN/DOCUMENT_SPECIFIC_DATA_SETS/DOCUMENT_SPECIFIC_DATA_SET/INTEGRATED_DISCLOSURE", mismo));
        if (integratedDisclosure == null)
            errors.add(new CalculationError(CalculationErrorType.INTERNAL_ERROR, "required container 'INTEGRATED_DISCLOSURE' is missing and can not be inserted"));
        Node projectedPayments = replaceNode(doc, integratedDisclosure, addNamespace("PROJECTED_PAYMENTS", mismo));
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
    }

    private void updateFeeSummaryDetail(Document doc, String mismo, LoanCalculations calcs) {
        boolean isLE = isDocTypeLoanEstimate(doc, mismo);
        Node feesSummary = constructNodePath(doc.getDocumentElement(), addNamespace("//LOAN/FEE_INFORMATION/FEES_SUMMARY", mismo));
        if (feesSummary == null)
            errors.add(new CalculationError(CalculationErrorType.INTERNAL_ERROR, "required container 'FEE_SUMMARY' is missing and can't be added"));
        Node feesSummaryDetail = replaceNode(doc, feesSummary, addNamespace("FEE_SUMMARY_DETAIL", mismo));
        feesSummaryDetail.appendChild(doc.createElement(addNamespace("APRPercent", mismo))).appendChild(doc.createTextNode(String.format("%7.4f", calcs.apr).trim()));
        if (!isLE) {
            feesSummaryDetail.appendChild(doc.createElement(addNamespace("FeeSummaryTotalAmountFinancedAmount", mismo))).appendChild(doc.createTextNode(String.format("%9.2f", calcs.amountFinanced).trim()));
            feesSummaryDetail.appendChild(doc.createElement(addNamespace("FeeSummaryTotalFinanceChargeAmount", mismo))).appendChild(doc.createTextNode(String.format("%9.2f", calcs.financeCharge).trim()));
        }
        feesSummaryDetail.appendChild(doc.createElement(addNamespace("FeeSummaryTotalInterestPercent", mismo))).appendChild(doc.createTextNode(String.format("%9.2f", calcs.totalInterestPercentage).trim()));
        if (!isLE)
            feesSummaryDetail.appendChild(doc.createElement(addNamespace("FeeSummaryTotalOfAllPaymentsAmount", mismo))).appendChild(doc.createTextNode(String.format("%9.2f", calcs.totalOfPayments).trim()));
    }

    private void updateIntegratedDisclosureDetail(Document doc, String mismo, LoanCalculations calcs) {
        if (isDocTypeLoanEstimate(doc, mismo)) {
            Node integratedDisclosureDetail = constructNodePath(doc.getDocumentElement(), addNamespace("//LOAN/DOCUMENT_SPECIFIC_DATA_SETS/DOCUMENT_SPECIFIC_DATA_SET/INTEGRATED_DISCLOSURE/INTEGRATED_DISCLOSURE_DETAIL", mismo));
            replaceNode(doc, integratedDisclosureDetail, addNamespace("FiveYearTotalOfPaymentsComparisonAmount", mismo)).appendChild(doc.createTextNode(String.format("%9.2f", calcs.fiveYearTotalOfPayments).trim()));
            replaceNode(doc, integratedDisclosureDetail, addNamespace("FiveYearPrincipalReductionComparisonAmount", mismo)).appendChild(doc.createTextNode(String.format("%9.2f", calcs.fiveYearPrincipal).trim()));
        }
    }

    private Document marshallErrors()  throws Exception {
	    ErrorsListModel errorsList = new ErrorsListModel();
	    List<ErrorModel> errorList = new LinkedList<ErrorModel>();
	    errors.forEach(errorEntity-> {
	        ErrorModel errorModel = new ErrorModel();
	        errorModel.setType(errorEntity.getType().msg);
	        errorModel.setMessage(errorEntity.getInfo());
	        errorList.add(errorModel);
	    });
	    errorsList.setError(errorList);
	    StringWriter sw = new StringWriter();
	    JAXBContext jaxbContext = JAXBContext.newInstance(ErrorsListModel.class);
	    Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
	    jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
	    jaxbMarshaller.marshal(errorsList, sw);
	    String xmlString = sw.toString();
	    DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
	    InputSource is = new InputSource();
	    is.setCharacterStream(new StringReader(xmlString));
	    return db.parse(is);
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
    
    private Node constructNodePath(Node node, String nodePathToAdd) {
        String nodeName = "";
        String[] nodes = nodePathToAdd.split("/");
        for (int i = 0; i < nodes.length; i++)
            if ("".equals(nodes[i]))
                nodeName += "/";
            else {
                nodeName += nodes[i];
                Node n = getNode(node, nodeName);
                if (n == null)
                    errors.add(new CalculationError(CalculationErrorType.INTERNAL_ERROR, "required container " + nodeName + " is missing and can't be inserted"));
                else
                    return traverseContainerPath(n, Arrays.copyOfRange(nodes, i+1, nodes.length));
                break;
            }
        return null;
    }
    
    private Node traverseContainerPath(Node node, String[] nodesToAdd) {
        for (int i = 0; i < nodesToAdd.length; i++) {
            Node n = getNode(node, nodesToAdd[i]);
            if (n == null)
                return insertContainerPath(node, Arrays.copyOfRange(nodesToAdd, i, nodesToAdd.length));
            node = n;
        }
        return node;
    }
    
    private Node replaceNode(Document doc, Node parent, String name) {
        Node oldNode = getNode(parent, name);
        Node newNode = doc.createElement(name);
        if (oldNode == null)
            return parent.insertBefore(newNode, findLocation(parent, name));
        parent.replaceChild(newNode, oldNode);
        return newNode;
    }
    
    private static Node findLocation(Node node, String child) {
        for (Node n = node.getFirstChild(); n != null; n = n.getNextSibling())
            if (child.compareTo(n.getNodeName()) < 0)
                return n;
        return null;
    }
    
    static private Node insertContainerPath(Node node, String[] nodesToAdd) {
        Document doc = node.getOwnerDocument();
        Node newNode = doc.createElement(nodesToAdd[0]);
        node = node.insertBefore(newNode, findLocation(node, nodesToAdd[0]));
        for (int i = 1; i < nodesToAdd.length; i++)
            node = node.appendChild(doc.createElement(nodesToAdd[i]));
        return node;
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
}
