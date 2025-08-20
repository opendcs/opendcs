
package decodes.tsdb.algoedit;

import java.io.LineNumberReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.StringTokenizer;

import ilex.util.Logger;
import decodes.tsdb.algo.AWAlgoType;

public class AlgoReader
{
	public static String NL = System.getProperty("line.separator");
	private enum ParseState {
		SCANNING,   // Scan for start of special section
		JAVADOC_START,
		IN_JAVADOC,
		JAVADOC_END,
		INPUTS,
		IMPORTS,
		LOCALVARS,
		OUTPUTS,
		PROPERTIES,
		INIT,
		USERINIT,
		BEFORE_TIMESLICES,
		TIMESLICE,
		AFTER_TIMESLICES
	};

	private LineNumberReader myreader;
	private String inputName;
	private AlgoData algoData;
	private String origLine;
	private String expectedEndTag = "xyz";
	private StringBuilder errBuf = new StringBuilder();
	private ParseState state = ParseState.SCANNING;
	private int numParseErrors;

	public AlgoReader()
	{
		myreader = null;
		algoData = null;
		origLine = null;
		inputName = null;
		numParseErrors = 0;
	}

	public void clearAlgoData()
	{
		algoData.setAlgorithmName("");
		algoData.setAlgorithmType(AWAlgoType.TIME_SLICE);
		algoData.setJavaClassName("");
		algoData.setJavaPackage("");
		algoData.setComment("");
		algoData.clearInputTimeSeries();
		algoData.clearOutputTimeSeries();
		algoData.clearAlgoProps();
		algoData.setImportsCode("");
		algoData.setLocalVarsCode("");
		algoData.setOneTimeInitCode("");
		algoData.setBeforeIterCode("");
		algoData.setTimeSliceCode("");
		algoData.setAfterIterCode("");
		algoData.setAggPeriodOutput("");
	}

	/**
	 * Reads the passed file into the passed data-object.
	 * @param strm the input stream to read
	 * @param nm the name of the stream for error messages.
	 * @param algoData the object to store the info into.
	 */
	public void readAlgo(InputStream strm, String nm, AlgoData algoData)
	{
		myreader = null;
		this.algoData = algoData;
		inputName = nm;
		clearAlgoData();
		StringBuilder content = new StringBuilder();
		errBuf.setLength(0);
		numParseErrors = 0;
		try
		{
			myreader = new LineNumberReader(new InputStreamReader(strm));
			state = ParseState.SCANNING;
			String line;
			while ((line = getNextLine()) != null)
			{
				switch(state)
				{
				case SCANNING:
					scan(line);
					break;
				case JAVADOC_START:
					if (line.startsWith("/**"))
						state = ParseState.IN_JAVADOC;
					else 
						checkEndTag(line);
					break;
				case IN_JAVADOC:
					if (line.contains("*/"))
					{
						state = ParseState.JAVADOC_END;
						expectedEndTag = "//AW:JAVADOC_END";
						algoData.setComment(content.toString());
						content.setLength(0);
					}
					else
						content.append(origLine + NL);
					break;
				case JAVADOC_END:
					checkEndTag(line);
					break;
				case INPUTS:
					inputs(line);
					break;
				case IMPORTS:
					if (checkEndTag(line))
					{
						algoData.setImportsCode(content.toString());
						content.setLength(0);
					}
					else
						content.append(line + NL);
					break;
				case LOCALVARS:
					if (checkEndTag(line))
					{
						algoData.setLocalVarsCode(content.toString());
						content.setLength(0);
					}
					else
					{
						if (origLine.startsWith("\t"))
							origLine = origLine.substring(1);
						content.append(origLine + NL);
					}
					break;
				case OUTPUTS:
					outputs(line);
					break;
				case PROPERTIES:
					properties(line);
					break;
				case INIT:
					init(line);
					break;
				case USERINIT:
					if (checkEndTag(line))
					{
						algoData.setOneTimeInitCode(content.toString());
						content.setLength(0);
					}
					else
					{
						if (origLine.startsWith("\t\t"))
							origLine = origLine.substring(2);
						content.append(origLine + NL);
					}
					break;
				case BEFORE_TIMESLICES:
					if (checkEndTag(line))
					{
						algoData.setBeforeIterCode(content.toString());
						content.setLength(0);
					}
					else
					{
						if (origLine.startsWith("\t\t"))
							origLine = origLine.substring(2);
						content.append(origLine + NL);
					}
					break;
				case TIMESLICE:
					if (checkEndTag(line))
					{
						algoData.setTimeSliceCode(content.toString());
						content.setLength(0);
					}
					else
					{
						if (origLine.startsWith("\t\t"))
							origLine = origLine.substring(2);
						content.append(origLine + NL);
					}
					break;
				case AFTER_TIMESLICES:
					if (checkEndTag(line))
					{
						algoData.setAfterIterCode(content.toString());
						content.setLength(0);
					}
					else
					{
						if (origLine.startsWith("\t\t"))
							origLine = origLine.substring(2);
						content.append(origLine + NL);
					}
					break;
				}
			}
		}
		catch(Exception ex)
		{
			warning("Exception reading '" + nm + "': " + ex);
		}
		finally
		{
			if (myreader != null)
			{
				try { myreader.close(); }
				catch(Exception ex) {}
			}
		}
	}

	private void scan(String line)
	{
		if (line.startsWith("package "))
		{
			line = line.substring(8);
			if (line.endsWith(";"))
				line = line.substring(0, line.length()-1);
			algoData.setJavaPackage(line);
		}
		else if (line.startsWith("public class "))
		{
			line = line.substring(13);
			StringTokenizer st = new StringTokenizer(line);
			if (st.hasMoreTokens())
			{
				String clsnm = st.nextToken();
				algoData.setAlgorithmName(clsnm);
				algoData.setJavaClassName(clsnm);
			}
			if (st.hasMoreTokens() && st.nextToken().equals("extends")
			 && st.hasMoreTokens())
			{
				algoData.setExtends(st.nextToken());
			}
		}
		else if (line.startsWith("extends"))
		{
			StringTokenizer st = new StringTokenizer(line);
			st.nextToken();
			if (st.hasMoreTokens())
				algoData.setExtends(st.nextToken());
			if (st.hasMoreTokens() && st.nextToken().equals("implements")
			 && st.hasMoreTokens())
			{
				algoData.setImplements(st.nextToken());
			}
		}
		else if (line.startsWith("implements"))
		{
			StringTokenizer st = new StringTokenizer(line);
			st.nextToken();
			if (st.hasMoreTokens())
				algoData.setImplements(st.nextToken());
		}
		else if (line.equals("//AW:JAVADOC"))
		{
			state = ParseState.JAVADOC_START;
			expectedEndTag = "//AW:JAVADOC_END";
		}
		else if (line.equals("//AW:INPUTS"))
		{
			state = ParseState.INPUTS;
			expectedEndTag = "//AW:INPUTS_END";
		}
		else if (line.equals("//AW:IMPORTS"))
		{
			state = ParseState.IMPORTS;
			expectedEndTag = "//AW:IMPORTS_END";
		}
		else if (line.equals("//AW:LOCALVARS"))
		{
			state = ParseState.LOCALVARS;
			expectedEndTag = "//AW:LOCALVARS_END";
		}
		else if (line.equals("//AW:OUTPUTS"))
		{
			state = ParseState.OUTPUTS;
			expectedEndTag = "//AW:OUTPUTS_END";
		}
		else if (line.equals("//AW:PROPERTIES"))
		{
			state = ParseState.PROPERTIES;
			expectedEndTag = "//AW:PROPERTIES_END";
		}
		else if (line.equals("//AW:INIT"))
		{
			state = ParseState.INIT;
			expectedEndTag = "//AW:INIT_END";
		}
		else if (line.equals("//AW:USERINIT"))
		{
			state = ParseState.USERINIT;
			expectedEndTag = "//AW:USERINIT_END";
		}
		else if (line.equals("//AW:BEFORE_TIMESLICES"))
		{
			state = ParseState.BEFORE_TIMESLICES;
			expectedEndTag = "//AW:BEFORE_TIMESLICES_END";
		}
		else if (line.equals("//AW:TIMESLICE"))
		{
			state = ParseState.TIMESLICE;
			expectedEndTag = "//AW:TIMESLICE_END";
		}
		else if (line.equals("//AW:AFTER_TIMESLICES"))
		{
			state = ParseState.AFTER_TIMESLICES;
			expectedEndTag = "//AW:AFTER_TIMESLICES_END";
		}
		else if (line.startsWith("//AW:"))
		{
			warning("Unexpected tag '" + line + "' -- skipped.");
		}
	}

	private void inputs(String line)
	{
		// Input var declaraction should be of the form:
		// [public] <type> varname;  //AW:TYPECODE=<typecode>

		if (line.startsWith("public "))
			line = line.substring(7);

		if ((line.startsWith("double")
		 || line.startsWith("long")
		 || line.startsWith("String"))
			&& !line.contains("_inputNames"))
		{
			StringTokenizer st = new StringTokenizer(line, " \t;=");
			int ntokens = st.countTokens();
			if (ntokens < 2)
			{
				warning("Bad syntax on input var declaration"
					+ " line has " + ntokens + " tokens.");
				return;
			}
			String jtype = st.nextToken();
			String varname = st.nextToken();
			String typcode = "i";
			if (ntokens >= 4)
			{
				st.nextToken(); // throw away "//AW:TYPECODE"
				typcode = st.nextToken();
			}
			algoData.addInputTimeSeries(
				new InputTimeSeries(varname, jtype, typcode));
		}
		else 
			checkEndTag(line);
	}

	private void outputs(String line)
	{
		if (line.startsWith("public "))
			line = line.substring(7);
		if (line.startsWith("NamedVariable "))
		{
			line = line.substring(14);
			if (line.length() == 0)
				return;
			int idx = line.indexOf(' ');
			if (idx == -1)
				idx = line.indexOf('=');
			if (idx > 0)
				line = line.substring(0, idx);
			algoData.addOutputTimeSeries(line);
		}
		else 
			checkEndTag(line);
	}

	private void properties(String line)
	{
		// Input var declaraction should be of the form:
		// [public] <type> varname;  //AW:TYPECODE=<typecode>

		if (line.startsWith("public "))
			line = line.substring(7);

		if ((line.startsWith("double")
		 || line.startsWith("long")
		 || line.startsWith("String")
		 || line.startsWith("boolean"))
			&& !line.contains("_propertyNames"))
		{
			StringTokenizer st = new StringTokenizer(line, " \t;=");
			int ntokens = st.countTokens();
			if (ntokens < 2)
			{
				warning("Bad syntax on input var declaration"
					+ " line has " + ntokens + " tokens.");
				return;
			}
			String jtype = st.nextToken();
			String name = st.nextToken();
			String defaultValue = "";
			if (st.hasMoreTokens())
				defaultValue = st.nextToken();
			if (jtype.equals("String") && defaultValue.startsWith("\""))
			{
				int start = line.indexOf('"');
				int end = start + 1;
				boolean escaped = false;
				for(; end < line.length(); end++)
				{
					if (!escaped && line.charAt(end) == '"')
						break;
					if (line.charAt(end) == '\\')
						escaped = true;
					else
						escaped = false;
				}
				defaultValue = line.substring(start, 
					(end < line.length() ? end+1 : end));
			}

			algoData.addAlgoProp(
				new AlgoProp(name, jtype, defaultValue));
		}
		else 
			checkEndTag(line);
	}


	private void init(String line)
	{
		if (checkEndTag(line))
			return;

		StringTokenizer st = new StringTokenizer(line, " \t;=\"");
		int ntokens = st.countTokens();
		if (ntokens == 2)
		{
			String nm = st.nextToken();
			String v = st.nextToken();

			if (nm.equals("_awAlgoType"))
			{
				if (v.contains("AGGREGATING"))
					algoData.setAlgorithmType(AWAlgoType.AGGREGATING);
				else if (v.contains("RUNNING"))
					algoData.setAlgorithmType(AWAlgoType.RUNNING_AGGREGATE);
				else
					algoData.setAlgorithmType(AWAlgoType.TIME_SLICE);
			}
			else if (nm.equals("_aggPeriodVarRoleName"))
				algoData.setAggPeriodOutput(nm);
		}
		else
		{
			warning("Bad syntax in INIT '" + line + "' -- skipped.");
			return;
		}
	}

	/**
	 * Checks for a specak "//AW:" tag signifying a switch in state.
	 * If this is not the expected tag, print a warning message.
	 * @return true if state is changed.
	 */
	private boolean checkEndTag(String line)
	{
		if (line.startsWith("//AW:"))
		{
			state = ParseState.SCANNING;
			if (!line.startsWith(expectedEndTag))
				warning("Garbled file. Expected '" + expectedEndTag
					+ "' got '" + line + "' continuing scan ...");
			if (!line.endsWith("END"))
				scan(line);
			return true;
		}
		else
			return false;
	}

	/**
	 * Get next line from the file. 
	 * Trims trailing double-slash comment.
	 * Trims leading and trailing space from the line.
	 * Skips blank and double-slash comment lines.
	 * @return the line of text or null if EOF.
	 */
	private String getNextLine()
		throws IOException
	{
		if ((origLine = myreader.readLine()) != null)
			return origLine.trim();

		// Fell through means EOF
		return null;
	}

	private void warning(String msg)
	{
		numParseErrors++;
		errBuf.append(inputName + " ");
		if (myreader != null)
			errBuf.append("(" + myreader.getLineNumber() + " ");
		errBuf.append(msg + NL);
	}

	public int getNumParseErrors()
	{
		return numParseErrors;
	}

	public String getParseErrors()
	{
		return errBuf.toString();
	}
}
