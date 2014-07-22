package gr.ntua.ivml.edmvalidation.cli;

import gr.ntua.ivml.edmvalidation.persistent.XmlSchema;
import gr.ntua.ivml.edmvalidation.util.StringUtils;
import gr.ntua.ivml.edmvalidation.xsd.OutputXSD;
import gr.ntua.ivml.edmvalidation.xsd.ReportErrorHandler;
import gr.ntua.ivml.edmvalidation.xsd.ReportErrorHandler.Error;
import gr.ntua.ivml.edmvalidation.xsd.SchemaValidator;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamSource;

import net.minidev.json.parser.ParseException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

/**
 * @author Konstantin Baierer
 *
 */
public class EdmSchemaValidator {
	
	private static final Logger log = LoggerFactory.getLogger(EdmSchemaValidator.class);
	
	private static final OutputXSD xsd;
	private static final String schemaPath = "/schemas/edm/EDM.xsd";
	private static final XmlSchema xmlSchema;
	
	static {
		xmlSchema = new XmlSchema();
		xmlSchema.setXsd(schemaPath);
		xmlSchema.setId(new Long(0));
		xsd = new OutputXSD(xmlSchema);
		try {
			xsd.processSchema(xmlSchema);
		} catch (IOException | ParseException e) {
			log.error("Couldn't initialize EDM schema: ", e);
			e.printStackTrace();
		}	
	}
	
	public static void main(String[] args) {
		ReportErrorHandler report = validateAgainstEdm(args[0]);
		System.out.println(report);
	}
	
	public static ReportErrorHandler validateAgainstEdm(String fname, boolean validateXsd, boolean validateSchematron) {
		return validateAgainstEdm(StringUtils.resolveNameToInputStream(fname), validateXsd, validateSchematron);
	}

	public static ReportErrorHandler validateAgainstEdm(File file, boolean validateXsd, boolean validateSchematron) {
		return validateAgainstEdm(new StreamSource(file), validateXsd, validateSchematron);
	}

	public static ReportErrorHandler validateAgainstEdm(InputStream is, boolean validateXsd, boolean validateSchematron) {
		return validateAgainstEdm(new StreamSource(is), validateXsd, validateSchematron);
	}

	public static ReportErrorHandler validateAgainstEdm(String fname) {
		return validateAgainstEdm(StringUtils.resolveNameToInputStream(fname), true, true);
	}

	public static ReportErrorHandler validateAgainstEdm(File file) {
		return validateAgainstEdm(new StreamSource(file), true, true);
	}

	public static ReportErrorHandler validateAgainstEdm(InputStream is) {
		return validateAgainstEdm(new StreamSource(is), true, true);
	}

	public static ReportErrorHandler validateAgainstEdm(Source s, boolean validateXsd, boolean validateSchematron) {
		ReportErrorHandler errReturn = new ReportErrorHandler();
		ReportErrorHandler errXSD = null;
		ReportErrorHandler errSCH = null;
		try {
			errXSD = SchemaValidator.validateXSD(s, xmlSchema);
			errSCH = SchemaValidator.validateSchematron(s, xmlSchema);
		} catch (TransformerException | SAXException | IOException e) {
			log.error("Validation failed (bug): ", e);
		}
		if (errXSD == null) {
			log.error("XSD validation failed (bug).");
			System.exit(1);
		}
		if (errSCH == null) {
			log.error("Schematron validation failed (bug).");
			System.exit(1);
		}
		StringBuilder sb = new StringBuilder();
		sb.append("*** EDM VALIDATION REPORT ***");
		for (Error err : errXSD.getErrors()) errReturn.addError(err);
		for (Error err : errSCH.getErrors()) errReturn.addError(err);
		return errReturn;
	}

}
