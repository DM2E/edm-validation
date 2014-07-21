package gr.ntua.ivml.edmvalidation.persistent;

import gr.ntua.ivml.edmvalidation.util.JSONUtils;
import gr.ntua.ivml.edmvalidation.xsd.XSDParser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.io.Reader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.swing.JFileChooser;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.ParseException;


//import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

public class TargetConfigurationFactory {
	private static final Logger log = Logger.getLogger(TargetConfigurationFactory.class);
	private static final String TARGET_DEFINITION_VERSION = "2.0";

	private XSDParser parser;
	private JSONObject configuration;
	
	public static void main(String [] args) {
		if(args.length > 0) {
			String xsd = args[0];
			
			@SuppressWarnings("unused")
			TargetConfigurationFactory factory = new TargetConfigurationFactory(xsd);
		} else {
			try {
				JFileChooser chooser = new JFileChooser();
				if(chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
//				{					
					File selected = chooser.getSelectedFile();
					String path = selected.getAbsolutePath();
//					String output = selected.getParent() + "/template.json";

//					String path = "/Users/mith/EDMSchemaV8.xsd";
//					String output = "/Users/mith/template.json";
					TargetConfigurationFactory factory = new TargetConfigurationFactory(path);
	
					StringBuffer buffer = new StringBuffer();
					
					try {
					File confFile = new File(path + ".conf");
					if(confFile.exists()) {
						String line;
						BufferedReader in = new BufferedReader(new FileReader(confFile));
						while((line = in.readLine()) != null) {
							buffer.append(line);
						}
						
						factory.setConfiguration(buffer.toString());
						in.close();
					}
					} catch(Exception ex) {
						ex.printStackTrace();
					}
				}
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	public TargetConfigurationFactory() {
		this.parser = null;
	}
	
	public TargetConfigurationFactory(String file) {
		log.debug("factory for xsd: " + file);
		this.parser = new XSDParser(file);
	}
	
	public TargetConfigurationFactory(InputStream stream) {
		this.parser = new XSDParser(stream);
	}
	
	public TargetConfigurationFactory(Reader reader) {
		this.parser = new XSDParser(reader);
	}
	
	public void setParser(String file) {
		this.parser = new XSDParser(file);
		if(this.configuration != null) setParserNamespacesFromConfiguration();
	}
	
	public void setParser(InputStream stream) {
		this.parser = new XSDParser(stream);
		if(this.configuration != null) setParserNamespacesFromConfiguration();
	}
	
	public void setParser(Reader reader) {
		this.parser = new XSDParser(reader);
		if(this.configuration != null) setParserNamespacesFromConfiguration();
	}
	
	public XSDParser getParser() {
		return this.parser;
	}
	
	public JSONObject generateConfiguration() {
		JSONObject target = new JSONObject();
		
		// basic structure
		target.put("version", TARGET_DEFINITION_VERSION);
		target.put("groups", new JSONArray());
		
		
		// namespaces
		JSONObject namespaces = new JSONObject();
		Map<String, String> map = parser.getNamespaces();
		Iterator<String> i = map.keySet().iterator();
		while(i.hasNext()) {
			String key = i.next();
			String value = map.get(key);
			
			namespaces.put(key, value);
		}
		
		target.put("namespaces", namespaces);
		
		// set item level
		JSONObject root = parser.getAnyRootElementDescription();

		if(root != null) {
			JSONObject item = new JSONObject();
			
			item.put("element", root.get("name").toString());
			if(root.containsKey("prefix")) {
				item.put("prefix", root.get("prefix").toString());
			}
				
			target.put("item", item);
		}
		
		//set default group
		JSONObject group = new JSONObject();
		group.put("name", root.get("name").toString());
		group.put("element", root.get("name").toString());
		((JSONArray) target.get("groups")).add(group);
		
		this.setConfiguration(target);
		return target;
	}
	
	public JSONObject getConfiguration(boolean generate) {
		if(this.configuration == null && generate) {
			this.generateConfiguration();
		}
		
		return this.configuration;
	}
	
	public void setConfiguration(String configuration) {
		try {
			this.setConfiguration(JSONUtils.parse(configuration));
		} catch (ParseException e) {
			e.printStackTrace();
		}
	}
	
	public void setConfiguration(JSONObject configuration) {
		this.configuration = configuration;
		log.debug("initialize namespaces");
        // initialize namespaces

		if(this.parser != null) setParserNamespacesFromConfiguration();
	}
	
	private void setParserNamespacesFromConfiguration() {
        if(this.configuration.containsKey("namespaces")) {
            JSONObject object = (JSONObject) this.configuration.get("namespaces");
            HashMap<String, String> map = new HashMap<String, String>();
            for(Object entry : object.keySet()) {
                    String key = (String) entry;
                    String value = object.get(key).toString();
                    map.put(value, key);
            }

            this.parser.setNamespaces(map);
        }
	}
	

	
	public JSONObject getDocumentation() {
		return this.parser.getDocumentation();
	}
	
	public Set<String> getSchematronRules() {
		return this.parser.getSchematronRules();
	}
}
