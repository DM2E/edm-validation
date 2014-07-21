package mint.xsd;


import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Set;

import mint.persistent.TargetConfigurationFactory;
import mint.persistent.XmlSchema;
import mint.schematron.SchematronXSLTProducer;
import mint.util.JSONUtils;
import mint.util.StringUtils;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.ParseException;

import org.apache.log4j.Logger;
import org.apache.xerces.xs.datatypes.XSDouble;

public class OutputXSD  {
	  
	//private static final long serialVersionUID = 1L;
	  
	protected final Logger log = Logger.getLogger(getClass());
		
	private String id = null;
	private XmlSchema xmlschema;

	private long sourceSchemaId;
	private String newName;
	
	private String textdata = "";
	
	
	public OutputXSD(XmlSchema xmlSchema) {
		setXmlschema(xmlSchema);
	}
	
//	public List<String> getAvailablexsd(String path) {
//		List<String> result = new ArrayList<String>();
//
//		try {
//			File schemaDir = new File(path);
//			File[] contents = schemaDir.listFiles();
//			for(int i = 0; i < contents.length; i++) {
//				File file = contents[i];
//				String filename = file.getAbsolutePath();
//				
//				if(file.isDirectory()) {
//					result.addAll(this.getAvailablexsd(file.getAbsolutePath()));
//				} else if(filename.toLowerCase().endsWith(".xsd")) {
//					result.add(filename);
//				}
//			}
//		} catch(Exception ex) {
//			ex.printStackTrace();
//		}
//
//		return result;
//	}
//	
//	public List<String> getAvailablexsd() {
//		List<String> result = new ArrayList<String>();
//		List<String> filenames = new ArrayList<String>();
//		
//		String schemaDir = Config.getSchemaDir().getAbsolutePath();
//		result = this.getAvailablexsd(schemaDir);
//		
//		Iterator<String> i = result.iterator();
//		while(i.hasNext()) {
//			String path = i.next();
//			String replaced = path.replace(schemaDir, "");
//			filenames.add(replaced);
//		}
//		
//		return filenames;
//	}
//	
//	public List<String> getAvailableXSL()
//	{
//		List<String> result = new ArrayList<String>();
//
//		try {
//			File schemaDir = Config.getSchemaDir();
//			String[] contents = schemaDir.list();
//			for(int i = 0; i < contents.length; i++) {
//				String filename = contents[i];
//				if(filename.toLowerCase().endsWith(".xsl")) {
//					result.add(filename);
//				}
//			}
//		} catch(Exception ex) {
//			ex.printStackTrace();
//		}
//
//		return result;
//	}

  
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
		
		URL confFile = OutputXSD.class.getResource(confFilename);
		if(confFile != null) {
			log.debug("Found configuration: " + confFilename);
			StringBuffer confcontents = StringUtils.fileContents(new File(confFile.getFile()));
			schema.setJsonConfig(confcontents.toString());
		} else {
			schema.setJsonConfig(null);
		}

		String xsdBaseName = OutputXSD.class.getResource(schema.getXsd()).getFile();
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
			if(factory.getParser() == null) factory.setParser(xsdBaseName);
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
			if(factory.getParser() == null) factory.setParser(xsdBaseName);
			log.debug("Get schematron rules...");
			Set<String> fsr = factory.getSchematronRules();
			schema.setSchematronRules(fsr);
			log.debug("-- schematron rules: " + schema.getSchematronRules());
			log.debug("Get documentation");
			schema.setDocumentation(factory.getDocumentation().toString());
		} 
		
		log.debug("Looking schematron rules");
		String externalSchematron = null;
		String schematronFilename = null;
		if(configuration.containsKey("schematron")) {
			schematronFilename = OutputXSD.class.getResource(configuration.get("schematron").toString()).getFile();					
		} else {
			schematronFilename = OutputXSD.class.getResource(schema.getXsd()).getFile() + ".sch";
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
			schematronXSL = SchematronXSLTProducer.getInstance().getXSL(wrapped);		
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
		SchemaValidator.clearCaches(schema);
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
