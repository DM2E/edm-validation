package gr.ntua.ivml.edmvalidation.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.apache.xerces.dom.DOMInputImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Resolves file/resource names either as classpath resources or as file resources, in a directory that can be given in the constructor;
 * The default schema root/directory is "/schema/edm/".
 * 
 * @author thomas.francart@sparna.fr
 *
 */
public class NameToStreamResolver {

	private static final Logger log = LoggerFactory.getLogger(NameToStreamResolver.class);
	
	public static final String DEFAULT_SCHEMA_ROOT = "/schemas/edm/";
	public static final String DUMMY_SYSTEMID_PREFIX = "http://DUMMY/";
	
	public String schemaRoot;

	public NameToStreamResolver() {
		this(DEFAULT_SCHEMA_ROOT);
	}
	
	public NameToStreamResolver(String schemaRoot) {
		super();
		this.schemaRoot = schemaRoot;
	}

	
	
	/**
	 * Try to resolve a filename first to a file in the classpath, then to a file on disk.
	 * TODO: Make the SCHEMA_PREFIX configurable. Problem is that this is called in static contexts
	 * @param fname
	 * @return InputStream of the classpath resource/file or null.
	 */
	public InputStream resolveNameToInputStream(String fname) {
		InputStream is = null;
		log.debug("Searching for '" + fname + "' in classpath.");
		is = NameToStreamResolver.class.getResourceAsStream(fname);
		if (is == null) {
			try {
				log.debug("Searching for '" + fname + "' as a file.");
				is = new FileInputStream(new File(fname));
			} catch (FileNotFoundException e) {
				// deliberately don't handle this
			}
		}
		if (is == null) {
			String fnameWithRoot = this.schemaRoot + fname;
			
			log.debug("Searching for '" + fnameWithRoot + "' in classpath.");
			is = NameToStreamResolver.class.getResourceAsStream(fnameWithRoot);
			
			if (is == null) {
				try {
					log.debug("Searching for '" + fnameWithRoot + "' as a file.");
					is = new FileInputStream(new File(fnameWithRoot));
				} catch (FileNotFoundException e) {
					// deliberately don't handle this
				}
			}
		}
		
		if(is == null) {
			log.warn("Could not resolve resource name '"+ fname +"' either as a classpath resource or file in '"+ this.schemaRoot +"'");
		}
		
		return is;
	}
	
	public EntityResolver createEntityResolver() {
		return new EntityResolver() {

			@Override
			public InputSource resolveEntity(String publicID, String systemID) throws SAXException, IOException {
				log.debug("Resolving: " + publicID + " => " + systemID);
				
				final InputSource isource = new InputSource();
				final String name = systemID.replaceFirst(NameToStreamResolver.DUMMY_SYSTEMID_PREFIX, "");
				final InputStream is = resolveNameToInputStream(name);
				if (null != is) {
					isource.setByteStream(is);
					isource.setSystemId(NameToStreamResolver.DUMMY_SYSTEMID_PREFIX + name);
				} else {
					log.error("Can't resolve '" + name + "' in classpath or on disk.");
				}
				return isource;
			}
			
		};
	}
	
	public LSResourceResolver createLsResourceResolver() {
		return new LSResourceResolver() {
			
			@Override
			public LSInput resolveResource(String type, String namespaceURI, String publicId, String systemId, String baseURI) { 

				final LSInput input = new DOMInputImpl();
				final String name = systemId.replaceFirst(DUMMY_SYSTEMID_PREFIX, "");
				final InputStream is = resolveNameToInputStream(name);
				if (null != is) {
					input.setByteStream(is);
					input.setSystemId(DUMMY_SYSTEMID_PREFIX + name);
				} else {
					log.error("Can't resolve '" + name + "' in classpath or on disk.");
				}
				return input;
			}
		};
	}
	
}
