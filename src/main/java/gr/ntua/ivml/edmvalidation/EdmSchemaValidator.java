package gr.ntua.ivml.edmvalidation;

import gr.ntua.ivml.edmvalidation.persistent.XmlSchema;
import gr.ntua.ivml.edmvalidation.util.StringUtils;
import gr.ntua.ivml.edmvalidation.xsd.OutputXSD;
import gr.ntua.ivml.edmvalidation.xsd.ReportErrorHandler;
import gr.ntua.ivml.edmvalidation.xsd.ReportErrorHandler.Error;
import gr.ntua.ivml.edmvalidation.xsd.SchemaValidator;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;

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
	private static final String DEFAULT_SCHEMA_PATH = "/schemas/edm/EDM.xsd";
	
	private OutputXSD xsd;
	
	private XmlSchema xmlSchema;
	
	/**
	 * Creates an EdmSchemaValidator with a default schema path
	 */
	public EdmSchemaValidator() {
		this(DEFAULT_SCHEMA_PATH);
	}
	
	/**
	 * Creates an EdmSchemaValidator with the given path to the EDM schema (either in classpath or disk)
	 * @param schemaPath path to EDM.xsd, either in classpath or as a file reference
	 */
	public EdmSchemaValidator(String schemaPath) {
		this.xmlSchema = new XmlSchema();
		this.xmlSchema.setXsd(schemaPath);
		this.xmlSchema.setId(new Long(0));
		this.xsd = new OutputXSD(xmlSchema);
		try {
			this.xsd.processSchema(xmlSchema);
		} catch (IOException | ParseException e) {
			log.error("Couldn't initialize EDM schema: ", e);
			e.printStackTrace();
		}	
	}
	
	public static boolean shouldBeValidated(File f) {
		return (f.isFile() && f.getName().endsWith("xml"));
	}
	
	public ReportErrorHandler validateAgainstEdm(String fname, boolean validateXsd, boolean validateSchematron) {
		return validateAgainstEdm(StringUtils.resolveNameToInputStream(fname), validateXsd, validateSchematron);
	}

	public ReportErrorHandler validateAgainstEdm(File file, boolean validateXsd, boolean validateSchematron) {
		return validateAgainstEdm(new StreamSource(file), validateXsd, validateSchematron);
	}

	public ReportErrorHandler validateAgainstEdm(InputStream is, boolean validateXsd, boolean validateSchematron) {
		return validateAgainstEdm(new StreamSource(is), validateXsd, validateSchematron);
	}

	public ReportErrorHandler validateAgainstEdm(String fname) {
		return validateAgainstEdm(StringUtils.resolveNameToInputStream(fname), true, true);
	}

	public ReportErrorHandler validateAgainstEdm(File file) {
		return validateAgainstEdm(new StreamSource(file), true, true);
	}

	public ReportErrorHandler validateAgainstEdm(InputStream is) {
		return validateAgainstEdm(new StreamSource(is), true, true);
	}

	public ReportErrorHandler validateAgainstEdm(Source s, boolean validateXsd, boolean validateSchematron) {
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
		for (Error err : errXSD.getErrors()) errReturn.addError(err);
		for (Error err : errSCH.getErrors()) errReturn.addError(err);
		return errReturn;
	}
	
	public static void main(String[] args) {
		HashSet<File> files = new HashSet<File>();
		for (String thingum : args) {
			File f = new File(thingum);
			// non-recursive
			if (f.isDirectory()) {
				for (File ff : f.listFiles()) {
					if (shouldBeValidated(ff)) {
						files.add(ff);
					}
				}
			} else if (shouldBeValidated(f)) {
				files.add(f);
			}
		}
		
		EdmSchemaValidator validator = new EdmSchemaValidator();
		for (File file : files) {
			ReportErrorHandler report = validator.validateAgainstEdm(file);
			if (report.isValid()) {
				// Don't write reports for valid data
				continue;
			}
			Path outputFile = Paths.get(file.getAbsolutePath() + ".edm-validation.txt");
			BufferedWriter out;
			try {
				out = Files.newBufferedWriter(outputFile, Charset.forName("UTF-8"), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
				out.write(report.getReportMessage());
				out.close();
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(128);
			}
		}
	}

}
