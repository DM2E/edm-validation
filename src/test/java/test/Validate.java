package test;


import java.io.File;
import java.io.IOException;

import javax.xml.transform.TransformerException;

import net.minidev.json.parser.ParseException;

import org.xml.sax.SAXException;

import mint.persistent.XmlSchema;
import mint.xsd.OutputXSD;
import mint.xsd.ReportErrorHandler;
import mint.xsd.SchemaValidator;

public class Validate {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			//File inputFile = new File("src/test/inputFiles/athenaplus_edm1.xml");
			final File folder = new File(Validate.class.getResource("/inputFiles/athenaplus_edm1.xml").getFile()).getParentFile();
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
						String err2 = SchemaValidator.validateSchematron(inputFile, xmlSchema);
						if (err2 == null || err2.length() == 0)
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
