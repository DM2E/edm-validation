package gr.ntua.ivml.edmvalidation.cli;

import gr.ntua.ivml.edmvalidation.persistent.XmlSchema;
import gr.ntua.ivml.edmvalidation.xsd.OutputXSD;
import gr.ntua.ivml.edmvalidation.xsd.ReportErrorHandler;
import gr.ntua.ivml.edmvalidation.xsd.SchemaValidator;

import java.io.File;
import java.io.IOException;

import javax.xml.transform.TransformerException;

import net.minidev.json.parser.ParseException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

/**
 * @author Konstantin Baierer
 *
 */
public class EdmValidationCLI {
	
	private static final Logger log = LoggerFactory.getLogger(EdmValidationCLI.class);
	
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
		}	
	}
	
	public static void main(String[] args) {
		File inputFile = new File(args[0]);
		String report = validateFileAgainstEDM(inputFile);
		System.out.println(report);
	}

	private static String validateFileAgainstEDM(File inputFile) {
		ReportErrorHandler errXSD = null;
		String errSCH = null;
		try {
			errXSD = SchemaValidator.validateXSD(inputFile, xmlSchema);
			errSCH = SchemaValidator.validateSchematron(inputFile, xmlSchema);
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
		if (errXSD.isValid() && errSCH.length() == 0) {
			sb.append(" YAY VALID ");
		} else if (! errXSD.isValid()) {
			sb.append(errXSD.getReportMessage());
		} else {
			sb.append(errSCH);
		}
		return sb.toString();
	}

}
