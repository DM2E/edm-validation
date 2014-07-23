package gr.ntua.ivml.edmvalidation.xsd;

import java.util.ArrayList;
import java.util.List;

import net.sf.json.JSONObject;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;


public class ReportErrorHandler implements ErrorHandler {
	private ArrayList<SAXParseException> report = new ArrayList<SAXParseException>();
	private  ArrayList<Error> errors = new ArrayList<Error>();
	
	public class Error {
		private String source;
		private int line;
		private int column;
		private String message;
		public String getSource() {
			return source;
		}
		public void setSource(String source) {
			this.source = source;
		}
		public int getLine() {
			return line;
		}
		public void setLine(int line) {
			this.line = line;
		}
		public int getColumn() {
			return column;
		}
		public void setColumn(int column) {
			this.column = column;
		}
		public String getMessage() {
			return message;
		}
		public void setMessage(String message) {
			this.message = message;
		}
		public JSONObject toJSON() {
			JSONObject result = new JSONObject();

			result.element("message", message);
			result.element("line", line);
			result.element("column", column);
			if(this.source != null) result.element("source", source);
			
			return result;
		}
	}
	
	private void handleException(SAXParseException e) {
//		report.add(e);
		
		Error error = new Error();
		error.setLine(e.getLineNumber());
		error.setColumn(e.getColumnNumber());
		error.setMessage(e.getMessage());
		error.setSource(e.getSystemId());
		
		errors.add(error);
	}

	@Override
	public void error(SAXParseException e) throws SAXException {
		handleException(e);
	}

	@Override
	public void fatalError(SAXParseException e) throws SAXException {
		handleException(e);
	}

	@Override
	public void warning(SAXParseException e) throws SAXException {
		handleException(e);
	}

	public String getReportMessage() {
		StringBuilder result = new StringBuilder();
		for( Error err: errors ) {
			result.append("EDM VALIDATION ERROR: ");
			result.append(err.getSource());
			result.append(" [" );
			result.append(err.getLine());
			result.append(" / " );
			result.append(err.getColumn());
			result.append("]" );
			result.append("\n\t" );
			result.append(err.getMessage());
			result.append("\n" );
		}
		return result.toString();
	}

	public boolean isValid() {
		return (errors.isEmpty() && report.isEmpty());
	}

	public ArrayList<SAXParseException> getReport() {
		return report;
	}
	
	public List<Error> getErrors() {
		return this.errors;
	}

	public void addError(Error error) {
		if(error != null) this.errors.add(error);
	}
	
	public void reset() {
		errors.clear();
		report.clear();
	}
	
	@Override
	public String toString() {
		return getReportMessage();
	}

}