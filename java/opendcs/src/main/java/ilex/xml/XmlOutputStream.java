/*
*  $Id$
*/
package ilex.xml;

import ilex.util.StringPair;
import ilex.util.TextUtil;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;

/**
This class implements methods for writing to an XML file.
It keeps track of indentation.
It manages the escape-sequences used for special characters.
*/
public class XmlOutputStream
{
	// Set by constructor:
	private OutputStream os;
	private BufferedWriter osw;
	private String top;

	private int curIndent;  // current level of indentation

	/**
	* Character(s) that are written for each level of indentation.
	* By default this is two space characters.
	* You can modify this string after creating the XmlOutputStream.
	*/
	public String indent;

	/**
	* Maximum width of a line.
	* The outputter will attempt to break up strings that would cause a line
	* to go over this limit.
	*/
	public int width;

	/**
	* Character(s) that are written for a newline.
	* By default these are read from the system property "line.separator".
	* You can modify this string after creating the XmlOutputStream.
	*/
	public String newline = System.getProperty("line.separator","\n");

	/**
	* URI of the DTD file (if any) that is associated with the XML file
	* to be written.
	* By default this is null (meaning standalone).
	* You can modify this string after creating the XmlOutputStream.
	*/
	public String xmlDtdUri;

	/**
	* Scop of the DTD URI. Default="SYSTEM"
	*/
	public String xmlDtdScope;

	/**
	* XML Version of the file being written.
	* By default this is 1.0.
	* You can modify this string after creating the XmlOutputStream.
	*/
	public static String xmlVersion;


	/**
	* Creates an XmlOutputStream to write to the passed output stream.
	* @param os the output stream
	* @param top the name of the top element
	*/
	public XmlOutputStream( OutputStream os, String top )
	{
		this.os = os;

		// XML requires UTF-8 encoding. This is NOT the default on windoze.
		Charset utf8 = Charset.forName("UTF-8");
		this.osw = new BufferedWriter(new OutputStreamWriter(os, utf8));
		this.top= top;
		curIndent = 0;
		indent = "  ";
		width = 80;
		newline = System.getProperty("line.separator", "\n");
		xmlVersion = "1.0";
		xmlDtdUri = null;
		xmlDtdScope = "SYSTEM";
	}

	/**
	* Writes the XML file header including version, URI, and top element name.
	* @throws IOException on IO error
	*/
	public void writeXmlHeader( ) throws IOException
	{
		String h = "<?xml version=\"" + xmlVersion + "\" "
			+ "encoding=\"UTF-8\" "
			+ "standalone=\"" + (xmlDtdUri == null ? "yes" : "no") + "\"?>";
		osw.write(h);
		osw.write(newline);
		if (xmlDtdUri != null)
		{
			h = "<!DOCTYPE " + top + " " + xmlDtdScope
				 + " \"" + xmlDtdUri + "\">";
			osw.write(h);
			osw.write(newline);
		}
		osw.flush();
	}

	/**
	* Starts an element with no attributes
	* @param tag the element tag
	* @throws IOException on IO error
	*/
	public void startElement( String tag ) throws IOException
	{
		startElement(tag, null);
	}

	/**
	* Starts an element with a single attribute
	* @param tag the element tag
	* @param attName attribute name
	* @param attValue attribute value
	* @throws IOException on IO error
	*/
	public void startElement( String tag, String attName, String attValue ) throws IOException
	{
		StringPair sp[] = new StringPair[1];
		sp[0] = new StringPair(attName, attValue);
		startElement(tag, sp);
	}

	/**
	* Starts an element with 2 attributes
	* @param tag the element tag
	* @throws IOException on IO error
	* @param att1Name attribute name
	* @param att1Value attribute value
	* @param att2Name attribute name
	* @param att2Value attribute value
	*/
	public void startElement( String tag, String att1Name, String att1Value, String att2Name, String att2Value ) throws IOException
	{
		StringPair sp[] = new StringPair[2];
		sp[0] = new StringPair(att1Name, att1Value);
		sp[1] = new StringPair(att2Name, att2Value);
		startElement(tag, sp);
	}

	/**
	* Starts an element with N attributes
	* @param tag the element tag
	* @param atts array of StringPair objects containing attribute name/values.
	* @throws IOException on IO error
	*/
	public void startElement( String tag, StringPair[] atts ) throws IOException
	{
		String startTag = buildStartTag(tag, atts, false);

		outputIndent();
		osw.write(startTag);
		osw.write(newline);
		osw.flush();
		curIndent++;
	}

	/**
	* Ends an element.
	* @param tag the element tag
	* @throws IOException on IO error
	*/
	public void endElement( String tag ) throws IOException
	{
		if (curIndent > 0)
			--curIndent;
		outputIndent();

		String t = "</" + tag + ">";
		osw.write(t);
		osw.write(newline);
		osw.flush();
	}

	/**
	* Writes PCDATA within the current element.
	* @param data the data
	* @throws IOException on IO error
	*/
	public void writePCDATA( String data ) throws IOException
	{
		if (data == null)
			return;
		data = escapeERs(data);

		int pos = currentIndentPosition();
		int w = pos < width ? width-pos : 40;
		String[] lines = ilex.util.TextUtil.splitLine(data, w);
		for(int i = 0; i < lines.length; i++)
		{
			outputIndent();
			osw.write(lines[i]);
			osw.write(newline);
		}
		osw.flush();
	}

	/**
	* Writes a literal string with no substitutions.
	* @param data the literal string
	* @throws IOException on IO error
	*/
	public void writeLiteral(String data) throws IOException
	{
		osw.write(data);
		osw.flush();
	}

	/**
	* Writes an element with no attributes.
	* If 'data' is null, an empty element is written with an elided end-tag.
	* @param tag the element tag
	* @param data the String content
	* @throws IOException on IO error
	*/
	public void writeElement( String tag, String data ) throws IOException
	{
		writeElement(tag, (StringPair[])null, data);
	}

	/**
	* Writes an element with one attribute.
	* If 'data' is null, an empty element is written with an elided end-tag.
	* @param tag the element tag
	* @param data the String content
	* @param attName attribute name
	* @param attValue attribute value
	* @throws IOException on IO error
	*/
	public void writeElement( String tag, String attName, String attValue, String data ) throws IOException
	{
		StringPair sp[] = new StringPair[1];
		sp[0] = new StringPair(attName, attValue);
		writeElement(tag, sp, data);
	}

	/**
	* Writes an element with two attributes.
	* If 'data' is null, an empty element is written with an elided end-tag.
	* @param tag the element tag
	* @param data the String content
	* @param att1Name attribute name
	* @param att1Value attribute value
	* @param att2Name attribute name
	* @param att2Value attribute value
	* @throws IOException on IO error
	*/
	public void writeElement( String tag, String att1Name, String att1Value, String att2Name, String att2Value, String data ) throws IOException
	{
		StringPair sp[] = new StringPair[2];
		sp[0] = new StringPair(att1Name, att1Value);
		sp[1] = new StringPair(att2Name, att2Value);
		writeElement(tag, sp, data);
	}

	/**
	* Writes an element with an arbitrary number of attributes.
	* If 'data' is null, an empty element is written with an elided end-tag.
	* @param tag the element tag
	* @param data the String content
	* @param atts array of StringPair objects containing name/value pairs.
	* @throws IOException on IO error
	*/
	public void writeElement( String tag, StringPair[] atts, String data ) throws IOException
	{
		String startTag = buildStartTag(tag, atts, data == null);
		boolean oneLine = (data == null)
			|| (currentIndentPosition() + startTag.length() + 
				data.length() + 3 + tag.length() <= width);

		if (!oneLine)
		{
			startElement(tag, atts);
			writePCDATA(data);
			endElement(tag);
		}
		else // Everything will fit on one line.
		{
			outputIndent();
			osw.write(startTag);

			if (data != null)
			{
				osw.write(escapeERs(data));
			
				String s = "</" + tag + ">";
				osw.write(s);
			}
			osw.write(newline);
		}
	}

	/**
	* @throws IOException
	*/
	private void outputIndent( ) throws IOException
	{
		for(int i = 0; i < curIndent; i++)
			osw.write(indent);
	}

	/**
	* @return
	*/
	private int currentIndentPosition( )
	{
		int inc = 0;
		for(int i = 0; i < indent.length(); i++)
			if (indent.charAt(i) == '\t')
				inc += 4;
			else
				inc++;
	
		return inc * curIndent;
	}

	/**
	* Internal utility: builds the start tag (without indentation)
	* If 'empty' is true, the tag will be terminated with "/>".
	* @param tag
	* @param atts
	* @param empty
	* @return
	*/
	private String buildStartTag( String tag, StringPair[] atts, boolean empty )
	{
		StringBuffer sb = new StringBuffer("<"+tag);
		if (atts != null)
			for(int i = 0; i<atts.length; i++)
			{
				if (atts[i] == null)
					continue;
				sb.append(' ');
				sb.append(atts[i].first);
				sb.append("=\"");
				sb.append(escapeERs(atts[i].second));
				sb.append('"');
			}
		sb.append( empty ? "/>" : ">");
		return sb.toString();
	}

	/**
	* Escapes entity references contained in the output data.
	* @param s
	* @return
	*/
	public static String escapeERs( String s )
	{
		StringBuilder sb = new StringBuilder(s);
		for(int i=0; i<sb.length(); i++)
		{
			char c = sb.charAt(i);
			if (c == '&')
			{
				sb.setCharAt(i, '&');
				sb.insert(++i, "amp;");
				i+=3;
			}
			else if (c == '<')
			{
				sb.setCharAt(i, '&');
				sb.insert(++i, "lt;");
				i+=2;
			}
			else if (c == '>')
			{
				sb.setCharAt(i, '&');
				sb.insert(++i, "gt;");
				i+=2;
			}
			else if (c == '"')
			{
				sb.setCharAt(i, '&');
				sb.insert(++i, "quot;");
				i+=4;
			}
			else if (c == '\'')
			{
				sb.setCharAt(i, '&');
				sb.insert(++i, "apos;");
				i+=4;
			}
			else if (c == '\\')
			{
				sb.setCharAt(i, '&');
				sb.insert(++i, "#92;");
				i+=3;
			}
			else if (c > 126
			     || (c < '\u0020' && c != '\n' && c != '\r' && c != '\t'))
			{
				sb.setCharAt(i, '&');
				String ins = "#" + (int)c + ";";
				sb.insert(++i, ins);
				i += ins.length();
			}
		}
		return sb.toString();
	}

	/*
	  Utility method to escape any linefeed '\n' and carriage-return '\r' 
	  characters with the hexadecimal equivalent. This way, they will survive 
	  being written to, and read from, and XML file.
	*/
//	public String escapeNLs(String s)
//	{
//		StringBuffer sb = new StringBuffer();
//		int len=s.length(); 
//		for(int i=0; i<len; i++)
//		{
//			char c = s.charAt(i);
//			if (c == '\n')
//				sb.append("\\n");
//			else if (c == '\r')
//				sb.append("\\r");
//			else
//				sb.append(c);
//		}
//		return sb.toString();
//	}
}
