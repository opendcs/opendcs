/*
* Where Applicable, Copyright 2025 OpenDCS Consortium and/or its contributors
* 
* Licensed under the Apache License, Version 2.0 (the "License"); you may not
* use this file except in compliance with the License. You may obtain a copy
* of the License at
* 
*   http://www.apache.org/licenses/LICENSE-2.0
* 
* Unless required by applicable law or agreed to in writing, software 
* distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
* License for the specific language governing permissions and limitations 
* under the License.
*/
package decodes.dupdcpgui;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import javax.xml.parsers.ParserConfigurationException;

import lrgs.common.DcpAddress;

import org.xml.sax.SAXException;

import ilex.cmdline.StringToken;
import ilex.cmdline.TokenOptions;
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
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	private String xmlFileStr;
	private String module = "GenerateToimportNL";
	private DuplicateIo dupIo;
	
	/** Constructor */
	public GenerateToimportNL(String propFile, String xmlFileStr)
	{
		this.xmlFileStr = xmlFileStr;
		dupIo = new DuplicateIo("$DCSTOOL_USERDIR/dcptoimport",
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
		}
		catch (IOException | SAXException | ParserConfigurationException ex)
		{
			log.atError().setCause(ex).log("Unable to create Import NetList file.");
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
			log.atError().setCause(ex).log("Invalid arguments provided.");
			System.exit(1);
		}
	}
}
