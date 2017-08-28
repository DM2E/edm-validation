package gr.ntua.ivml.edmvalidation.xsd;


import gr.ntua.ivml.edmvalidation.persistent.TargetConfigurationFactory;
import gr.ntua.ivml.edmvalidation.persistent.XmlSchema;
import gr.ntua.ivml.edmvalidation.schematron.SchematronXSLTProducer;
import gr.ntua.ivml.edmvalidation.util.JSONUtils;
import gr.ntua.ivml.edmvalidation.util.NameToStreamResolver;
import gr.ntua.ivml.edmvalidation.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.Set;

import net.minidev.json.JSONObject;
import net.minidev.json.parser.ParseException;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OutputXSD  {
	  
	//private static final long serialVersionUID = 1L;
	  
	private static final Logger log = LoggerFactory.getLogger(OutputXSD.class);
		
	private String id = null;
	private XmlSchema xmlschema;

	private long sourceSchemaId;
	private String newName;
	
	private String textdata = "";
	
	// handle on the SchemaValidator to update caches
	private SchemaValidator schemaValidator;
	
	private NameToStreamResolver resolver;
	
	public OutputXSD(XmlSchema xmlSchema, SchemaValidator schemaValidator, NameToStreamResolver resolver) {
		this.schemaValidator = schemaValidator;
		this.resolver = resolver;
		setXmlschema(xmlSchema);
	}
	
	public String getId()
	{
		return id;
	}
	
	public void setId(String id)
	{
		this.id = id;
	}
	
	public XmlSchema getXmlschema()
	{
		return xmlschema;
	}
	
	public void setXmlschema(XmlSchema xmlschema) {
		this.xmlschema = xmlschema;
	}
	
	
	public void setSourceSchemaId(String id) {
		this.sourceSchemaId = Long.parseLong(id);
	}
	
	public String getSourceSchemaId() {
		return "" + this.sourceSchemaId;
	}
	

	public void processSchema(XmlSchema schema) throws IOException, ParseException {
		this.processSchema(schema, true);
	}
	
	private void processSchema(XmlSchema schema, boolean reparse) throws IOException, ParseException {
		log.debug("Processing schema: " + schema);

		String confFilename = schema.getXsd() + ".conf";
		
		InputStream confFileIS = this.resolver.resolveNameToInputStream(confFilename);
		if(confFileIS != null) {
			log.debug("Found configuration: " + confFilename);
			StringWriter sw = new StringWriter();
			IOUtils.copy(confFileIS, sw);
			schema.setJsonConfig(sw.toString());
		} else {
			schema.setJsonConfig(null);
		}

		InputStream xsdIS = this.resolver.resolveNameToInputStream(schema.getXsd());
		if (xsdIS == null) {
			log.error("Couldn't resolve schema " + schema);
		}
		TargetConfigurationFactory factory = null;
		
		try {
			factory = new TargetConfigurationFactory();
		} catch(Exception ex) {
			ex.printStackTrace();
			return;
		}
		
		log.debug("Build schema factory for: " + schema.getXsd());
		
		// create configuration or use one provided if it exists
		JSONObject configuration = null;
		if(schema.getJsonConfig() == null || schema.getJsonConfig().length() == 0) {
			log.debug("Generating default configuration");
			if(factory.getParser() == null) factory.setParser(new XSDParser(xsdIS, this.resolver));
			configuration = factory.getConfiguration(true);
			configuration.put("xsd", schema.getXsd());
			schema.setJsonConfig(configuration.toString());
		} else {
			configuration = JSONUtils.parse(schema.getJsonConfig());
			factory.setConfiguration(schema.getJsonConfig());
			log.debug("Using provided configuration");
		}
		
		if(schema.getJsonOriginal() == null || reparse) {
			// generate mapping template
			if(factory.getParser() == null) factory.setParser(new XSDParser(xsdIS, this.resolver));
			log.debug("Get schematron rules...");
			Set<String> fsr = factory.getSchematronRules();
			schema.setSchematronRules(fsr);
//			log.debug("-- schematron rules: " + schema.getSchematronRules());
			log.debug("Get documentation");
			schema.setDocumentation(factory.getDocumentation().toString());
		} 
		
		log.debug("Looking schematron rules");
		String externalSchematron = null;
		String schematronFilename = null;
		if(configuration.containsKey("schematron")) {
			if(OutputXSD.class.getResource(configuration.get("schematron").toString()) != null) {
				schematronFilename = OutputXSD.class.getResource(configuration.get("schematron").toString()).getFile();
			} else {
				schematronFilename = configuration.get("schematron").toString();
			}
			
			// schematronFilename = OutputXSD.class.getResource(configuration.get("schematron").toString()).getFile();					
		} else {
			if(OutputXSD.class.getResource(schema.getXsd()) != null) {
				schematronFilename = OutputXSD.class.getResource(schema.getXsd()).getFile() + ".sch";
			} else {
				schematronFilename = schema.getXsd()+".sch";
			}
			// schematronFilename = OutputXSD.class.getResource(schema.getXsd()).getFile() + ".sch";
		}
		log.debug("-- schematron file: " + schematronFilename);

		File schematronFile = new File(schematronFilename);
		if(schematronFile.exists()) {
			try {
				log.debug("Loading external schematron rules...");
				externalSchematron = StringUtils.xmlContents(schematronFile);
				log.debug("-- external schematron rules: " + externalSchematron);
			} catch (Exception e) {
				externalSchematron = null;
				log.debug("Could not load external schematron file: " + schematronFile.getAbsolutePath());
				e.printStackTrace();
			}
		}

		/*
		 generate schematron XSL
		 - use external schematron by default
		 - if xml schema rules exists attempt to merge
		 - if no external schematron exists wrap rules and generate
		*/
		
		String schematronRules = externalSchematron;
		String schematronXSL = null;
		if(externalSchematron != null && schema.getSchematronRules() != null) {
			try {
				log.debug("Merging schematron rules...");
				schematronRules = SchematronXSLTProducer.getInstance().mergeSchematronRules(externalSchematron, schema.getSchematronRules());
				log.debug("-- merged schematron rules: " + schematronRules);
			} catch (Exception e) {
				log.info("Failed to merge schematron rules, fall back to xml schema rules");
				e.printStackTrace();
			}					
		}
		
		if(schematronRules == null && schema.getSchematronRules() != null) {
			log.debug("Generate schematron XSL from xml schema rules...");
			schematronRules = schema.getSchematronRules();
		}

		if(schematronRules != null) {
			log.debug("Generate schematron XSL...");
			String wrapped = SchematronXSLTProducer.getInstance().wrapRules(schematronRules, null);
			log.debug("Wrapped schematron XSL...");
			schematronXSL = SchematronXSLTProducer.getInstance().getXSL(wrapped);		
			log.debug("Generated schematron XSL.");
		}
		schema.setSchematronXSL(schematronXSL);

		

		// extract item level, label & id if they exist
		if(configuration.containsKey("paths")) {
			JSONObject paths = (JSONObject) configuration.get("paths");

			if(paths.containsKey("item")) {
				schema.setItemLevelPath(paths.get("item").toString());
			}
			
			if(paths.containsKey("label")) {
				schema.setItemLabelPath(paths.get("label").toString());				
			}
			
			if(paths.containsKey("id")) {
				schema.setItemIdPath(paths.get("id").toString());				
			}
		}
		
		// Clear any cached objects in SchemaValidator
		log.debug("Clear cached objects.");
		this.schemaValidator.clearCaches(schema);
	}
	
	public void setTextdata(String s) {
		this.textdata = s;
	}
	
	public String getTextdata() {
		return this.textdata;
	}

	public String getNewName() {
		return newName;
	}

	public void setNewName(String newName) {
		this.newName = newName;
	}
}
