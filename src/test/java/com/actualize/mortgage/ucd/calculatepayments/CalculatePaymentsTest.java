package com.actualize.mortgage.ucd.calculatepayments;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

import org.junit.Test;

import com.actualize.mortgage.ucd.calculatepayments.CalculatePayments;

public class CalculatePaymentsTest {

	@Test
	public void test() throws FileNotFoundException {
		String filename = "C:/Users/tmcuckie/Dropbox (Personal)/TransformX/ucd-sample-xml-files-appendix-g/NonSeller_ARM_033117.xml";
	//		String filename = "C:/Users/tmcuckie/Dropbox (Personal)/USBank Code/Code_2016_11_03/Actualize/Data/CD_2017208111.xml";
	//		String filename = "C:/Users/tmcuckie/Dropbox (Personal)/USBank Code/Code_2016_11_03/Actualize/Data/CD_6830011666.xml";
		File file = new File(filename);
		@SuppressWarnings("resource")
		String content = new Scanner(file).useDelimiter("\\Z").next();
		CalculatePayments calculator = new CalculatePayments();
		calculator.calculate(content);

		assertTrue("Success", true);
	}
}
