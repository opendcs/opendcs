package decodes.dupdcpgui;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;

import javax.xml.parsers.ParserConfigurationException;

import lrgs.common.DcpAddress;

import org.xml.sax.SAXException;

import ilex.cmdline.StringToken;
import ilex.cmdline.TokenOptions;
import ilex.util.Logger;
import decodes.db.NetworkList;
import decodes.db.NetworkListEntry;
import decodes.util.CmdLineArgs;
import decodes.xml.TopLevelParser;

/**
 * This class is used by the combine-from-hub.sh program. 
 * This class takes as an argument a network list as XML file
 * and creates a new TOIMPORT.nl file with the DCPs from this 
 * XML Network List minus any DCP found on the controlling-district
 * file.
 *
 * This utility will be used when the combine-from-hub.sh can't find
 * a district-TOIMPORT.nl file, in which case this class will create
 * the district-TOIMPORT.nl file for the given Network List XML file.
 * This will be used when adding a new group to the combine SQL Database.
 */
public class GenerateToimportNL
{
	private String xmlFileStr;
	private String module = "GenerateToimportNL";
	private String propFile;
	private DuplicateIo dupIo;
	
	/** Constructor */
	public GenerateToimportNL(String propFile, String xmlFileStr)
	{
		this.xmlFileStr = xmlFileStr;
		this.propFile = propFile;
		dupIo = new DuplicateIo("$DECODES_INSTALL_DIR/dcptoimport",
			"controlling-districts.txt");
		createToImportNl(this.xmlFileStr);
	}
	
	/**
	 * Construct a TopLevelParser, call parse method and give it
	 * the network list xml file. The parse method will return a 
	 * Network List obj which will be used to generate a TOIMPORT.nl
	 * file in conjunction with the controlling district list.
	 * 
	 * @param xmlFileStr
	 */
	private void createToImportNl(String xmlFileStr)
	{
		try
		{
			TopLevelParser topLevelParser = new TopLevelParser();
			File xmlFile = new File(xmlFileStr);
			boolean ctrlDistOk = dupIo.readControllingDist();
			StringBuffer nlBuffer = new StringBuffer("");
			NetworkList nl = (NetworkList)topLevelParser.parse(xmlFile);
			if (nl != null)
			{
				String districtName = 
					DuplicateIo.parseDistrictName(nl.getDisplayName());
				for(Iterator<NetworkListEntry> it = nl.iterator(); it.hasNext(); )
				{
					//select all dcps that are 
					//not in the controlling dist list
					NetworkListEntry nle = it.next();
					if (nle != null)
					{	//Get the dcp address
						String dcpAddress = nle.transportId;
						//address:name description:type
						if (!ctrlDistOk)
						{	//no controlling district list found
							nlBuffer.append(dcpAddress
								+ ":" + (nle.getPlatformName() != null ? nle.getPlatformName() : "")
								+"\n");
						}
						else
						{	//add dcp if it is not in the control list
							ControllingDistrict cd =
								dupIo.getControllingDistrict(
									new DcpAddress(dcpAddress));
							if (cd == null 
							 || cd.getDistrict().equalsIgnoreCase(districtName))
								nlBuffer.append(dcpAddress 
									+ ":" + (nle.getPlatformName() != null ? nle.getPlatformName() : "")
									+"\n");
						}
					}
				}
				//Create the TOIMPORT.nl file with the nlBuffer
				dupIo.writeToImportNlFile(nlBuffer.toString(), districtName);
			}
		} catch (IOException e)
		{
			Logger.instance().failure(module + " " + e.getMessage());
		} catch (SAXException e)
		{
			Logger.instance().failure(module + " " + e.getMessage());
		} catch (ParserConfigurationException e)
		{
			Logger.instance().failure(module + " " + e.getMessage());
		}
	}
	
	/** Main method */
	public static void main(String[] args)
	{
		//Network List XML file
		StringToken xmlFile;
		
		String logname= "GenerateToimportNL.log";
		CmdLineArgs cmdLineArgs = new CmdLineArgs(false, logname);
		
		xmlFile = new StringToken("f", 
				"XML Network List file (full file system path)", "",
				TokenOptions.optRequired, 
				"");
		cmdLineArgs.addToken(xmlFile);
		
		try 
		{ 
			cmdLineArgs.parseArgs(args);
			String propFile = cmdLineArgs.getPropertiesFile();
		
			//Create utility
			GenerateToimportNL utility = new GenerateToimportNL(propFile,
													xmlFile.getValue());
		}
		catch(IllegalArgumentException ex)
		{
			Logger.instance().failure(ex.getMessage());
		}
	}
}
