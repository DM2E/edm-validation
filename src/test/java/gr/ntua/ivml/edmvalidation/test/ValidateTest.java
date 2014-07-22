package gr.ntua.ivml.edmvalidation.test;


import static org.fest.assertions.Assertions.*;
import gr.ntua.ivml.edmvalidation.persistent.XmlSchema;
import gr.ntua.ivml.edmvalidation.xsd.OutputXSD;
import gr.ntua.ivml.edmvalidation.xsd.ReportErrorHandler;
import gr.ntua.ivml.edmvalidation.xsd.SchemaValidator;

import java.io.File;
import java.io.IOException;

import javax.xml.transform.TransformerException;

import net.minidev.json.parser.ParseException;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

public class ValidateTest {
	
	private static final Logger log = LoggerFactory.getLogger(ValidateTest.class);
	private final String schemaPath = "/schemas/edm/EDM.xsd";
	private OutputXSD xsd;
	private XmlSchema	xmlSchema;

	public ValidateTest() throws Exception {
		xmlSchema = new XmlSchema();
		xmlSchema.setXsd(schemaPath);
		xsd = new OutputXSD(xmlSchema);
		xsd.processSchema(xmlSchema);	
	}
	/**
	 * @param args
	 * @throws ParseException 
	 * @throws IOException 
	 */
	@Test
	public void testValid() throws Exception {
		final File folder = new File(ValidateTest.class.getResource("/expect-valid/.dummy").getFile()).getParentFile();
		for (File inputFile : folder.listFiles()) {
			if (! inputFile.isFile() || inputFile.length() == 0) continue;
			ReportErrorHandler err1 = SchemaValidator.validateXSD(inputFile, xmlSchema);
			assertThat(err1.isValid()).overridingErrorMessage(inputFile + " is NOT VALID according to XSD: " + err1.getReportMessage()).isTrue();
			ReportErrorHandler err2 = SchemaValidator.validateSchematron(inputFile, xmlSchema);
			assertThat(err2.isValid()).overridingErrorMessage(inputFile + " is NOT VALID according to Schematron: " + err2).isTrue();
		}	
	}
	
	@Test
	public void testInvalidByXSD() throws Exception {
		final File folder = new File(ValidateTest.class.getResource("/expect-invalid-xsd/.dummy").getFile()).getParentFile();
		for (File inputFile : folder.listFiles()) {
			if (! inputFile.isFile() || inputFile.length() == 0) continue;
			ReportErrorHandler err1 = SchemaValidator.validateXSD(inputFile, xmlSchema);
			assertThat(err1.isValid()).overridingErrorMessage(inputFile + " IS VALID according to XSD!").isFalse();
		}	
	}

	@Test
	public void testInvalidBySchematron() throws Exception {
		final File folder = new File(ValidateTest.class.getResource("/expect-invalid-schematron/.dummy").getFile()).getParentFile();
		for (File inputFile : folder.listFiles()) {
			if (! inputFile.isFile() || inputFile.length() == 0) continue;
			ReportErrorHandler err2 = SchemaValidator.validateSchematron(inputFile, xmlSchema);
			assertThat(err2.isValid()).overridingErrorMessage(inputFile + " IS VALID according to Schematron!").isFalse();
			log.debug(err2.toString());
		}	
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			//File inputFile = new File("src/test/inputFiles/athenaplus_edm1.xml");
			final File folder = new File(ValidateTest.class.getResource("/inputFiles/athenaplus_edm1.xml").getFile()).getParentFile();
			String schemaPath = "/schemas/edm/EDM.xsd";
			XmlSchema xmlSchema = new XmlSchema();
			xmlSchema.setXsd(schemaPath);
			xmlSchema.setId(new Long(0));
			OutputXSD xsd = new OutputXSD(xmlSchema);
			xsd.processSchema(xmlSchema);
			for (File inputFile : folder.listFiles()) {
				if (inputFile.isFile()) {
					ReportErrorHandler err1 = SchemaValidator.validateXSD(inputFile, xmlSchema);
					if (err1.isValid()) {
						System.out.println("Input file " + inputFile.getName() + " is valid according to schema.");
						ReportErrorHandler err2 = SchemaValidator.validateSchematron(inputFile, xmlSchema);
						if (err2.isValid())
							System.out.println("Input file " + inputFile.getName() + " is valid according to schematron rules.");
						else
							System.out.println("Input file " + inputFile.getName() + " is invalid due to schematron rules:\n" + err2);
					}
					else {
						System.out.println("Input file " + inputFile.getName() + " is invalid due to schema:\n" + err1.getReportMessage());
					}
				}   
			}	
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		} catch (TransformerException e) {
			e.printStackTrace();
		}
	}

}
