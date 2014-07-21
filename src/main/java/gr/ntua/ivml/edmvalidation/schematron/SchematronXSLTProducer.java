/**
 * 
 */
package gr.ntua.ivml.edmvalidation.schematron;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Map;

import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.URIResolver;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.ParsingException;
import nu.xom.ValidityException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author geomark
 *
 */
public class SchematronXSLTProducer {
	
	private static final Logger log = LoggerFactory.getLogger(SchematronXSLTProducer.class);

	private static SchematronXSLTProducer INSTANCE;
	private static TransformerFactory tFactory;
	private static String iso_dsdl_include = "/schematron/iso_dsdl_include.xsl";
	private static String iso_abstract_expand = "/schematron/iso_abstract_expand.xsl";
	private static String iso_svrl_for_xslt2 = "/schematron/iso_svrl_for_xslt2.xsl";
//	private static String iso_schematron_skeleton_for_saxon = "iso_schematron_skeleton_for_saxon.xsl";
	
	
	/**
	 * Default private constructor (instantiate via factory method)
	 */
	private SchematronXSLTProducer(){
		
	}
	
	
	/**
	 * @return
	 */
	public static SchematronXSLTProducer getInstance(){
		if(INSTANCE == null){
			tFactory = TransformerFactory.newInstance();
			tFactory.setURIResolver(new URIResolver() {
				@Override
				public Source resolve(String href, String base) throws TransformerException {
					href = "/schematron/" + href;
					log.debug("Resolving XSL import '" + href + "'.");
					InputStream is = SchematronXSLTProducer.class.getResourceAsStream(href);
			        StreamSource jarFileSS = new StreamSource();
			        jarFileSS.setInputStream(is);
			        return jarFileSS;
				}
			});
			
			INSTANCE = new SchematronXSLTProducer();
		}

		return INSTANCE;
	}
	
	/**
	 * Generate the schematron XSL from a string that contains a schematron document
	 * @param schematron
	 * @return
	 */
	public String getXSL(String schematron) {				
		String step1 = performTransformation(schematron, SchematronXSLTProducer.class.getResourceAsStream(iso_dsdl_include));
		String step2 = performTransformation(step1,SchematronXSLTProducer.class.getResourceAsStream(iso_abstract_expand));
		String finalstep = performTransformation(step2,SchematronXSLTProducer.class.getResourceAsStream(iso_svrl_for_xslt2));
		return finalstep;
	}
	
	/**
	 * Form a complete schematron document from schematron rules by adding root element and XmlSchema's namespace declarations 
	 * @param schematronRules
	 * @param namespaces
	 * @return
	 */
	public String wrapRules(String schematronRules, Map<String, String> namespaces) {
		StringBuffer sb = new StringBuffer();
		sb.append("<schema xmlns=\"http://purl.oclc.org/dsdl/schematron\">");
		
		if(namespaces != null) {
			for(String prefix: namespaces.keySet()) {
				String uri = namespaces.get(prefix);
				sb.append("<ns prefix=\"" + prefix + "\" uri=\"" + uri + "\"/>");
			}
		}
		
	    sb.append(schematronRules);
		sb.append("</schema>");
		
		return sb.toString();
	}
		
	public String wrapRules(String schematronRules){
		return this.wrapRules(schematronRules, null);
	}
	
	/**
	 * @param xml
	 * @param xslt
	 * @return
	 */
	private String performTransformation(String xml, InputStream xslt){
		try {
		    StringReader reader = new StringReader(xml);
		    StringWriter writer = new StringWriter();

		    Transformer transformer = tFactory.newTransformer(
		            new StreamSource(xslt));

		    transformer.transform(
		            new StreamSource(reader), 
		            new StreamResult(writer));

		    String result = writer.toString();
		    return result;
		} catch (Exception e) {
		    e.printStackTrace();
		    return null;
		}
	}

	/**
	 * Merge schematron rules with an existing schematron document
	 * 
	 * @param Schematron string with contents of the original schematron document
	 * @param schematronRules the rules to merge
	 * @return the merged schematron document
	 * @throws IOException 
	 * @throws ParsingException 
	 * @throws ValidityException 
	 */
	public String mergeSchematronRules(String schematron, String schematronRules) throws ValidityException, ParsingException, IOException {
		Builder builder = new Builder();
		Document document = builder.build(new ByteArrayInputStream(schematron.getBytes()));
		return this.mergeSchematron(document, schematronRules);
	}
	
	/**
	 * Merge schematron rules with an existing schematron document
	 * 
	 * @param Schematron the original schematron document
	 * @param schematronRules the rules to merge
	 * @return the merged schematron document
	 * @throws IOException 
	 * @throws ParsingException 
	 * @throws ValidityException 
	 */
	public String mergeSchematron(Document schematron, String schematronRules) throws ValidityException, ParsingException, IOException {
		Builder builder = new Builder();
		Document rules = builder.build(new ByteArrayInputStream(this.wrapRules(schematronRules).getBytes()));
		for(int i = 0; i < rules.getRootElement().getChildCount(); i++) {
			schematron.getRootElement().appendChild(rules.getRootElement().getChild(i).copy());			
		}
		return schematron.toXML();
	}
}