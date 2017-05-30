package gr.ntua.ivml.edmvalidation;

import static org.fest.assertions.Assertions.*;
import gr.ntua.ivml.edmvalidation.EdmSchemaValidator;
import gr.ntua.ivml.edmvalidation.xsd.ReportErrorHandler;

import java.io.File;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class EdmSchemaValidatorTest {
	
	private static final Logger log = LoggerFactory.getLogger(EdmSchemaValidatorTest.class);

	@Test
	public void testValid() throws Exception {
		EdmSchemaValidator validator = new EdmSchemaValidator();
		final File folder = new File(EdmSchemaValidatorTest.class.getResource("/expect-valid/.dummy").getFile()).getParentFile();
		for (File inputFile : folder.listFiles()) {
			if (! EdmSchemaValidator.shouldBeValidated(inputFile)) continue;
			{
				ReportErrorHandler err = validator.validateAgainstEdm(inputFile, true, false);
				assertThat(err.isValid()).overridingErrorMessage(inputFile + " is NOT VALID according to XSD: " + err.getReportMessage()).isTrue();
			}
			{
				ReportErrorHandler err = validator.validateAgainstEdm(inputFile, false, true);
				assertThat(err.isValid()).overridingErrorMessage(inputFile + " is NOT VALID according to Schematron: " + err).isTrue();
			}
			{
				ReportErrorHandler err = validator.validateAgainstEdm(inputFile);
				assertThat(err.isValid()).overridingErrorMessage(inputFile + " is NOT VALID according to XSD/Schematron: " + err).isTrue();
			}
		}	
	}
	
	@Test
	public void testInvalidByXSD() throws Exception {
		EdmSchemaValidator validator = new EdmSchemaValidator();
		final File folder = new File(EdmSchemaValidatorTest.class.getResource("/expect-invalid-xsd/.dummy").getFile()).getParentFile();
		for (File inputFile : folder.listFiles()) {
			if (! EdmSchemaValidator.shouldBeValidated(inputFile)) continue;
			ReportErrorHandler err1 = validator.validateAgainstEdm(inputFile, true, false);
			assertThat(err1.isValid()).overridingErrorMessage(inputFile + " IS VALID according to XSD!").isFalse();
			log.debug(err1.toString());
		}	
	}

	@Test
	public void testInvalidBySchematron() throws Exception {
		EdmSchemaValidator validator = new EdmSchemaValidator();
		final File folder = new File(EdmSchemaValidatorTest.class.getResource("/expect-invalid-schematron/.dummy").getFile()).getParentFile();
		for (File inputFile : folder.listFiles()) {
			if (! EdmSchemaValidator.shouldBeValidated(inputFile)) continue;
			ReportErrorHandler err2 = validator.validateAgainstEdm(inputFile, false, true);
			assertThat(err2.isValid()).overridingErrorMessage(inputFile + " IS VALID according to Schematron!").isFalse();
			log.debug(err2.toString());
		}	
	}
}
