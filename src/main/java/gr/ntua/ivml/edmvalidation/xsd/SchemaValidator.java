package gr.ntua.ivml.edmvalidation.xsd;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.HashMap;

import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;

import gr.ntua.ivml.edmvalidation.persistent.XmlSchema;
import gr.ntua.ivml.edmvalidation.util.NameToStreamResolver;
import gr.ntua.ivml.edmvalidation.util.StringUtils;

public class SchemaValidator {	
	
	
	private static final Logger log = LoggerFactory.getLogger(SchemaValidator.class);
	
	private SchemaFactory factory;
	private TransformerFactory tFactory;

	private HashMap<Long, Schema> schemaCache = new HashMap<Long, Schema>();
	private HashMap<Long, Templates> schematronCache = new HashMap<Long, Templates>();
	
	private NameToStreamResolver resolver;
	
	public SchemaValidator(NameToStreamResolver r) {
		this.resolver = r;
		
		factory = org.apache.xerces.jaxp.validation.XMLSchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");
		tFactory =  TransformerFactory.newInstance("net.sf.saxon.TransformerFactoryImpl",null);
		try {
			factory.setFeature("http://apache.org/xml/features/validation/schema-full-checking", false);
			factory.setFeature("http://apache.org/xml/features/honour-all-schemaLocations", true);
			factory.setResourceResolver(this.resolver.createLsResourceResolver());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	// Full validation

	public ReportErrorHandler validate(String input, XmlSchema schema) throws SAXException, IOException, TransformerException {
		ReportErrorHandler report = new ReportErrorHandler();
		validate(input, schema, report);
		return report;
	}

	public void validate(String input, XmlSchema schema, ReportErrorHandler handler) throws SAXException, IOException, TransformerException {
		validateXSD(input, schema, handler);
		validateSchematron(input, schema, handler);
	}
		
	// XSD validation
	
	public ReportErrorHandler validateXSD(String input, XmlSchema schema) throws SAXException, IOException {
		ReportErrorHandler report = new ReportErrorHandler();
		validateXSD(input, schema, report);
		return report;
	}

	public void validateXSD(String input, XmlSchema schema, ErrorHandler handler) throws SAXException, IOException {
		byte[] bytes = input.getBytes("UTF-8");
		ByteArrayInputStream stream = new ByteArrayInputStream(bytes);
		StreamSource source = new StreamSource(stream);
		validateXSD(source, schema, handler);
	}
	
	public ReportErrorHandler validateXSD(File input, File xsd) throws SAXException, IOException {
		ReportErrorHandler report = new ReportErrorHandler();
		validateXSD(input, xsd, report);
		return report;
	}
	
	public ReportErrorHandler validateXSD(File input, XmlSchema xmlSchema) throws SAXException, IOException {
		ReportErrorHandler report = new ReportErrorHandler();
		StreamSource source = new StreamSource(new FileInputStream(input));
		validateXSD(source, xmlSchema, report);
		return report;
	}
	
	public void validateXSD(File input, File xsd, ErrorHandler handler) throws SAXException, IOException {
		StreamSource source = new StreamSource(new FileInputStream(input));
		validateXSD(source, xsd, handler);
	}

	public ReportErrorHandler validateXSD(Source source, XmlSchema schema) throws SAXException, IOException {
		ReportErrorHandler report = new ReportErrorHandler();
		validateXSD(source, schema, report);
		return report;
	}
	
	public void validateXSD(Source source, XmlSchema xmlSchema, ErrorHandler handler) throws SAXException, IOException {
		Schema schema = getSchema(xmlSchema);
		validateXSD(source, schema, handler);
	}
		
	public ReportErrorHandler validateXSD(Source source, File schemaFile) throws SAXException, IOException, TransformerException {
		ReportErrorHandler report = new ReportErrorHandler();
		validateXSD(source, schemaFile, report);
		return report;
	}
	
	public void validateXSD(Source source, File schemaFile, ErrorHandler handler) throws SAXException, IOException {
		String schemaPath = schemaFile.getAbsolutePath();
		validateXSD(source, schemaPath, handler);
	}
	
	public ReportErrorHandler validateXSD(Source source, String schemaPath) throws SAXException, IOException {
		ReportErrorHandler report = new ReportErrorHandler();
		validateXSD(source, schemaPath, null);
		return report;
	}
	
	public void validateXSD(Source source, String schemaPath, ErrorHandler handler) throws SAXException, IOException {
			Schema schema = getSchema(schemaPath);
			log.debug("getSchema: " + schema);
			Validator validator = schema.newValidator();
			log.debug("newValidator: " + validator);
			if(handler != null) {
				validator.setErrorHandler(handler);
			}
			validator.validate(source);
	}
	
	public void validateXSD(Source source, Schema schema, ErrorHandler handler ) throws SAXException, IOException {
		Validator validator = schema.newValidator();
		if(handler != null) {
			validator.setErrorHandler(handler);
		}
		validator.validate(source);
	}

	// Schematron validation

	public ReportErrorHandler validateSchematron(String input, XmlSchema schema) throws TransformerException, SAXException, IOException {
		return validateSchematron(input, schema, new ReportErrorHandler());
	}

	public ReportErrorHandler validateSchematron(String input, XmlSchema schema, ReportErrorHandler handler) throws TransformerException, SAXException, IOException {
		byte[] bytes = input.getBytes("UTF-8");
		ByteArrayInputStream stream = new ByteArrayInputStream(bytes);
		StreamSource source = new StreamSource(stream);
		return validateSchematron(source, schema, handler);
	}
	
	public ReportErrorHandler validateSchematron(File input, XmlSchema schema) throws TransformerException, SAXException, IOException {
		return validateSchematron(input, schema, new ReportErrorHandler());
	}

	public ReportErrorHandler validateSchematron(File input, XmlSchema schema, ReportErrorHandler handler) throws TransformerException, SAXException, IOException {
		StreamSource source = new StreamSource(new FileInputStream(input));
		return validateSchematron(source, schema, handler);
	}
	
	public ReportErrorHandler validateSchematron(Source source, XmlSchema schema) throws TransformerException {
		return validateSchematron(source, schema, new ReportErrorHandler());
	}
		
	public ReportErrorHandler validateSchematron(Source source, XmlSchema schema, ReportErrorHandler handler) throws TransformerException {
		
		if (handler == null) handler = new ReportErrorHandler();
		if(schema.getSchematronXSL() != null) {					
			DOMResult result = new DOMResult();
	
			Transformer transformer = getTransformer(schema);
		    transformer.transform(source, result);
		    
		    NodeList nresults = result.getNode().getFirstChild().getChildNodes();
		    boolean failed = false;
		    for(int i=0; i < nresults.getLength();i++){
		    	Node nresult = nresults.item(i);
		    	if("failed-assert".equals(nresult.getLocalName())) {
		    		failed = true;
		    		ReportErrorHandler.Error error = handler.new Error();
		    		// Not that helpful
//					error.setSource(nresult.getAttributes().getNamedItem("location").getTextContent());
		    		String textContent = nresult.getTextContent();
		    		textContent = textContent.replaceAll("\\s\\s+", " ");
		    		System.out.println("Schematron validation error message : "+textContent);
					if(textContent.indexOf('<') > -1) {
						final String res = textContent.substring(textContent.indexOf('<')+1, textContent.indexOf('>'));
						final String errMess = textContent.substring(textContent.indexOf('>') + 1);
			    		error.setMessage(errMess);
			    		error.setSource(res);
					} else if(textContent.startsWith("id: ")){
						final int indexOfIdEnd = textContent.substring("id: ".length()).indexOf(" ")+"id: ".length();
						final String res = textContent.substring("id: ".length(), indexOfIdEnd);
						final String errMess = textContent.substring(indexOfIdEnd + 1);
			    		error.setMessage(errMess);
			    		error.setSource(res);
					} else {
						// default
						error.setMessage(textContent);
						error.setSource(textContent);
					}
		    		
		    		
		    		handler.addError(error);
		    	}
		    }
		    if (failed) {
		    	log.trace("Schematron result: " + StringUtils.fromDOM(result.getNode()));
		    }
		}
		return handler;
	}
	
	private synchronized Transformer getTransformer(XmlSchema schema) throws TransformerConfigurationException {
		Templates templates = schematronCache.get(schema.getId());

		
		if(templates == null) {
			String schematronXSL = schema.getSchematronXSL();
	//	    log.debug("schematron XSL: " + schematronXSL);
			StringReader xslReader = new StringReader(schematronXSL);
			templates = tFactory.newTemplates(new StreamSource(xslReader));
			schematronCache.put(schema.getId(), templates);
		}

		return templates.newTransformer();
	}

	public synchronized Schema getSchema( String schemaPath ) throws SAXException  {
		Schema schema = factory.newSchema(new File(schemaPath));
		return schema;
	}
	
	public synchronized Schema getSchema( XmlSchema xmlSchema ) throws SAXException, FileNotFoundException  {
		Schema schema = schemaCache.get(xmlSchema.getId());
		
		if(schema == null) {
			InputStream is = this.resolver.resolveNameToInputStream(xmlSchema.getXsd());
			final StreamSource isource = new StreamSource(is);
			final String id = NameToStreamResolver.DUMMY_SYSTEMID_PREFIX + xmlSchema.getXsd();
			isource.setSystemId(id);
			schema = factory.newSchema(isource);
			schemaCache.put(xmlSchema.getId(), schema);
		}

		return schema;
	}
	
	
	
	/**
	 * Clear caches in SchemaValidator
	 */
	public synchronized void clearCaches() {
		schemaCache.clear();
		schematronCache.clear();
	}

	/**
	 * Clear cache for specific schema in SchemaValidator
	 */
	public synchronized void clearCaches(XmlSchema schema) {
		schemaCache.remove(schema.getId());
		schematronCache.remove(schema.getId());
	}
}
