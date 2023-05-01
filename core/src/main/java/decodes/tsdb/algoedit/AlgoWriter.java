package decodes.tsdb.algoedit;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.File;
import java.io.PrintWriter;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.StringTokenizer;

import ilex.util.Logger;
import decodes.tsdb.algo.AWAlgoType;


/**
Merges an algorithm's data with the template and writes the output file.
*/
public class AlgoWriter
{
	private String templateFile;
	public static String NL = System.getProperty("line.separator");
	private int indent = 2;

	public AlgoWriter(String templateFile)
	{
		this.templateFile = templateFile;
	}

	/**
	 * Merges the data with the template and saves the file.
	 * @throws AlgoIOException on failure.
	 */
	public void saveToTheFile(File theFile, AlgoData algoData)
		throws AlgoIOException
	{
		String doingWhat = "Opening template '" + templateFile + "'";
		URL myurl = ClassLoader.getSystemResource(templateFile);

		StringBuilder image = new StringBuilder();
		LineNumberReader myreader = null;
		BufferedWriter mywriter = null;
		try
		{
			myreader = new LineNumberReader(new InputStreamReader(
				myurl.openStream()));
			doingWhat = "merging with template";
			boolean overwrite = false;
			String endString = "xxx";
			while (myreader.ready())
			{
				String line = myreader.readLine();
				String trimline = line.trim();

				// If I'm in a section of the template that's being overwritten
				// with this algorithm's data...
				if (overwrite)
				{
					if (trimline.equals(endString))
					{
						image.append(line + NL);
						overwrite = false;
					}
					continue;
				}

				if (trimline.startsWith("package"))
				{
					image.append("package "
						+ algoData.getJavaPackage() + ";" + NL);
					continue;
				}
				else if (trimline.startsWith("public class"))
				{
					image.append("public class "
						+ algoData.getJavaClassName() + NL
						+ "\textends " + algoData.getExtends());
					String imp = algoData.getImplements();
					if (imp != null && imp.length() > 0)
						image.append(NL + "\timplements " + imp);
					image.append(NL);
					continue;
				}

				image.append(line + NL);

				if (trimline.equals("//AW:JAVADOC"))
				{
					image.append("/**" + NL);
					image.append(algoData.getComment());
					image.append(NL);
					image.append(" */" + NL);
					overwrite = true;
					endString = "//AW:JAVADOC_END";
				}
				else if (trimline.equals("//AW:INPUTS"))
				{
					overwrite = true;
					endString = "//AW:INPUTS_END";

					ArrayList<InputTimeSeries> inputs = 
						algoData.getAllInputTimeSeries();
					if (inputs.size() == 0)
						continue;

					for(InputTimeSeries its : inputs)
						image.append("\tpublic " + its.javaType + " " + its.roleName
							+ ";	//AW:TYPECODE=" + its.roleTypeCode + NL);

					image.append("\tString _inputNames[] = { ");
					int n = 0;
					for(InputTimeSeries its : inputs)
					{
						if (n > 0)
							image.append(", ");
						image.append("\"" + its.roleName + "\"");
						n++;
					}
					image.append(" };" + NL);
				}
				else if (trimline.equals("//AW:LOCALVARS"))
				{
					overwrite = true;
					endString = "//AW:LOCALVARS_END";
					indent = 1;
					outputCode(algoData.getLocalVarsCode(), image);
					indent = 2;
					image.append(NL);
				}
				else if (line.equals("//AW:OUTPUTS"))
				{
					overwrite = true;
					endString = "//AW:OUTPUTS_END";

					ArrayList<String> outputs = 
						algoData.getAllOutputTimeSeries();
//					if (outputs.size() == 0)
//						continue;

					for(String name : outputs)
						image.append("\tpublic NamedVariable " + name
							+ " = new NamedVariable(\"" + name
							+ "\", 0);" + NL);

					image.append("\tString _outputNames[] = { ");
					int n = 0;
					for(String name : outputs)
					{
						if (n > 0)
							image.append(", ");
						image.append("\"" + name + "\"");
						n++;
					}
					image.append(" };" + NL);
				}
				else if (line.equals("//AW:PROPERTIES"))
				{
					overwrite = true;
					endString = "//AW:PROPERTIES_END";

					ArrayList<AlgoProp> props = algoData.getAllAlgoProps();

					for(AlgoProp prop : props)
						image.append("\tpublic " + prop.javaType + " " + prop.name
							+ " = " + prop.defaultValue + ";" + NL);

					image.append("\tString _propertyNames[] = { ");
					int n = 0;
					for(AlgoProp prop : props)
					{
						if (n > 0)
							image.append(", ");
						image.append("\"" + prop.name + "\"");
						n++;
					}
					image.append(" };" + NL);
				}
				else if (line.equals("//AW:INIT"))
				{
					overwrite = true;
					endString = "//AW:INIT_END";

					AWAlgoType algoType =
						algoData.getAlgorithmType();
					image.append("\t\t_awAlgoType = AWAlgoType."
						+ algoType.toString() + ";" + NL);

					String aggOutput = algoData.getAggPeriodOutput();
					if (algoType == AWAlgoType.AGGREGATING
					 && aggOutput != null)
						image.append("\t\t_aggPeriodVarRoleName = \""
							+ aggOutput + "\";" + NL);
				}
				else if (line.equals("//AW:IMPORTS"))
				{
					overwrite = true;
					endString = "//AW:IMPORTS_END";
					indent = 0;
					outputCode(algoData.getImportsCode(), image);
					indent = 2;
				}
				else if (line.equals("//AW:USERINIT"))
				{
					overwrite = true;
					endString = "//AW:USERINIT_END";
					outputCode(algoData.getOneTimeInitCode(), image);
				}
				else if (line.equals("//AW:BEFORE_TIMESLICES"))
				{
					overwrite = true;
					endString = "//AW:BEFORE_TIMESLICES_END";
					outputCode(algoData.getBeforeIterCode(), image);
				}
				else if (line.equals("//AW:TIMESLICE"))
				{
					overwrite = true;
					endString = "//AW:TIMESLICE_END";
					outputCode(algoData.getTimeSliceCode(), image);
				}
				else if (line.equals("//AW:AFTER_TIMESLICES"))
				{
					overwrite = true;
					endString = "//AW:AFTER_TIMESLICES_END";
					outputCode(algoData.getAfterIterCode(), image);
				}
			}

			// System.out.println(image);
			doingWhat = "writing output '" + theFile.getName() + "'";
			mywriter = new BufferedWriter(new FileWriter(theFile));
			mywriter.write(image.toString());
			return;
		}
		catch (Exception ex)
		{
			String msg = "Error " + doingWhat + ": " + ex;
			System.err.println(msg);
			ex.printStackTrace(System.err);
			throw new AlgoIOException(msg);
		}
		finally
		{
			if (myreader != null)
			{
				try { myreader.close(); }
				catch(Exception ex) {}
			}
			if (mywriter != null)
			{
				try { mywriter.close(); }
				catch(Exception ex) {}
			}
		}
	}

	private void outputCode(String section, StringBuilder image)
	{
		StringTokenizer st = new StringTokenizer(section,"\r\n");
		while(st.hasMoreTokens())
		{
			for(int i=0; i<indent; i++)
				image.append('\t');
			image.append(st.nextToken() + NL);
		}
	}
}
