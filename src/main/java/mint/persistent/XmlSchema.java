package mint.persistent;

import java.util.Date;
import java.util.Set;

import javax.xml.validation.Schema;

import mint.xsd.SchemaValidator;

import org.xml.sax.SAXException;
//import gr.ntua.ivml.mint.mapping.model.SchemaConfiguration;

public class XmlSchema {
	public class Parameter {
		private String name;
		private String type;
		private String value;
		
		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
		
		public String getType() {
			return type;
		}
		
		public void setType(String type) {
			this.type = type;
		}
		
		public String getValue() {
			return value;
		}
		
		public void setValue(String value) {
			this.value = value;
		}
	}

	Long id;
	String name;
	String xsd;
	String itemLevelPath, itemLabelPath, itemIdPath;
	String jsonConfig, jsonTemplate, jsonOriginal;
	String documentation;
	String schematronRules;
	String schematronXSL;
	Date created;
	Date lastModified;
		
	// helper functions
	
	
	//
	//  Getter Setter boilerplate code
	//
	
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getXsd() {
		return xsd;
	}
	public void setXsd(String xsd) {
		this.xsd = xsd;
	}
	public String getItemLevelPath() {
		return itemLevelPath;
	}
	public void setItemLevelPath(String itemLevelPath) {
		this.itemLevelPath = itemLevelPath;
	}
	public String getItemLabelPath() {
		return itemLabelPath;
	}
	public void setItemLabelPath(String itemLabelPath) {
		this.itemLabelPath = itemLabelPath;
	}
	public String getItemIdPath() {
		return itemIdPath;
	}
	public void setItemIdPath(String itemIdPath) {
		this.itemIdPath = itemIdPath;
	}
	public String getJsonConfig() {
		return jsonConfig;
	}
	public void setJsonConfig(String jsonConfig) {
		this.jsonConfig = jsonConfig;
	}
	public String getJsonTemplate() {
		return jsonTemplate;
	}
	public void setJsonTemplate(String jsonTemplate) {
		this.jsonTemplate = jsonTemplate;
	}
	public Date getCreated() {
		return created;
	}
	public void setCreated(Date created) {
		this.created = created;
	}

	public Date getLastModified() {
		return lastModified;
	}

	public void setLastModified(Date lastModified) {
		this.lastModified = lastModified;
	}
	
	public String getJsonOriginal() {
		return jsonOriginal;
	}

	public void setJsonOriginal(String jsonOriginal) {
		this.jsonOriginal = jsonOriginal;
	}

	public void setDocumentation(String documentation) {
		this.documentation = documentation;
	}
	
	public String getDocumentation() {
		return this.documentation;
	}
	
	/**
	 * @return the schematronRules
	 */
	public String getSchematronRules() {
		return schematronRules;
	}

	/**
	 * @param schematronRules Set of schematron rules. Contents will be concatenated and saved in schematronRules
	 */
	public void setSchematronRules(Set<String> schematronRules) {
		String rules = "";
		
		for(String rule: schematronRules) {
			rules += rule;
		}
		
		this.schematronRules = rules;
	}

	/**
	 * @param schematronRules the schematronRules to set
	 */
	public void setSchematronRules(String schematronRules) {
		this.schematronRules = schematronRules;
	}

	public String getSchematronXSL() {
		return schematronXSL;
	}
	
	public void setSchematronXSL(String xsl) {
		this.schematronXSL = xsl;
	}
	

	
	
	public Schema getSchema() throws SAXException {
		return SchemaValidator.getSchema(this);
	}
	
	
	public String toString() {
		if(this.name != null && this.name.length() > 0) {
			return this.name;
		} else if(this.xsd != null && this.xsd.length() > 0) {
			return "[" + this.xsd + "]";
		}
		
		return "XmlSchema: " + this.id;
	}
	

	
}
