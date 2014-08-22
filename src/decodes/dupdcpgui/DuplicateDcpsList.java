package decodes.dupdcpgui;

import ilex.cmdline.StringToken;
import ilex.cmdline.TokenOptions;
import ilex.util.EnvExpander;
import ilex.util.Logger;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Properties;

import decodes.db.Constants;
import decodes.db.Database;
import decodes.db.DatabaseException;
import decodes.db.DatabaseIO;
import decodes.db.NetworkList;
import decodes.db.NetworkListEntry;
import decodes.db.Site;
import decodes.dcpmon.DcpGroupList;
import decodes.util.CmdLineArgs;
import decodes.util.DecodesSettings;

/**
 * This class is the main class for the Duplicate DCPs GUI.
 * This GUI will find the duplicate DCPs, allow the user to 
 * assign the import district and create temporary network 
 * list .nl file for each district. These .nl file will be used when 
 * performing the pxport command on the combine-from-hub.sh 
 * script. In addition, this GUI will save the controlling 
 * district information in a text file.
 * 
 * The controlling-district.txt and the distname-TOIMPORT.nl
 * files are stored in DECODES_INSTALL_DIR/dcptoimport directory.
 * 
 * This class takes three arguments:
 * The dcpmon.conf file with its location
 * (Optional) - a pdts_compressed.txt file (used to find description for 
 * platforms that do not have one and PDT owner "agency")
 * (Optional) - a NWS file (hads.txt file) (used to find NWSHB5 code, nws 
 * description) 
 * 
 * Caveat: The way the District names and network list names are matched
 * is by subtracting the -RIVERGAGES-DAS from the network list name and
 * this gives us the District Name. If we need to implement this 
 * Combine SQL functionality in other place - We must set the District
 * names with the Network list names so that the Duplicate DCPs GUI can
 * work fine. 
 * In addition, the .nl files needs to be on the location as the 
 * dcpmon.conf file.
 * 
 * This code goes along with the combine-from-hub.sh script.
 * If this script changes we need to verify this code.
 */
public class DuplicateDcpsList
{
	private String module = "DuplicateDcpsList";
	private DuplicateDcpsListFrame dupDcpsFrame;
	private boolean packFrame = false;
	private String propFile;
	private String dcpMonConf;
	private static String pdtFile;
	private static String nwsFile;
	private ArrayList<String> groupNames;
	public static String GROUP_DELIMITER_NAME = "-RIVERGAGES-DAS";

	/** Constructor. Initialize class */
	public DuplicateDcpsList(String propFile, String dcpMonConf,
			String pdtFile, String nwsFile)
	{
		this.propFile = propFile;
		this.dcpMonConf = dcpMonConf;
		setPdtFile(pdtFile);
		setNwsFile(nwsFile);
	}

	/**
	 * Read Group names from dcpmon.conf
	 * Read Decodes Database.
	 * Create the DuplicateDcpsFrame class.
	 */
	public void init()
	{
		if (!initDatabaseObjects())
			return; //If error reading database STOP- no processing
		
		//Read Groups from dcpmon.conf file - these are the network lists
		if (!readGroupsFromDcpMonConf())
			return; //if cannot read dcpmon.conf STOP - can not do any process
					//without the DCP Mon groups
		
		dupDcpsFrame = new DuplicateDcpsListFrame(groupNames);
		// Validate frames that have preset sizes
		//Pack frames that have useful preferred size info, 
		//e.g. from their layout
		if (packFrame)
		{
			dupDcpsFrame.pack();
		} else
		{
			dupDcpsFrame.validate();
		}
		// Center the window
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		Dimension frameSize = dupDcpsFrame.getSize();
		if (frameSize.height > screenSize.height)
		{
			frameSize.height = screenSize.height;
		}
		if (frameSize.width > screenSize.width)
		{
			frameSize.width = screenSize.width;
		}
		dupDcpsFrame.setLocation((screenSize.width - frameSize.width) / 2,
				(screenSize.height - frameSize.height) / 2);
		dupDcpsFrame.setVisible(true);
	}

	/**
	 * Read the Decodes database.
	 *	@return true if database initializa ok, false otherwise
	 */
	private boolean initDatabaseObjects()
	{
		//Read decodes.properties
		DecodesSettings settings = DecodesSettings.instance();
		if (propFile != null)
		{
			Properties props = new Properties();
			FileInputStream fis;
			try
			{
				fis = new FileInputStream(propFile);
				props.load(fis);
				fis.close();
				settings.loadFromProperties(props);
			} catch (FileNotFoundException e)
			{
				System.out.println("Can not read Decodes.properties. "
						+ e.getMessage());
				Logger.instance().failure(module + " " + e.getMessage());
				return false;
			} catch (IOException e)
			{
				System.out.println("Can not read Decodes.properties. "
						+ e.getMessage());
				Logger.instance().failure(module + " " + e.getMessage());
				return false;
			}
		}
		//Construct the database and the interface specified by properties.
		Database db;
		db = new decodes.db.Database();
		Database.setDb(db);
		DatabaseIO dbio;
		//SqlDatabaseIO dbio;
		try
		{
			System.out.println("Please Wait Initializing Application" +
			" with All Data, this may take sometime ...");
			Logger.instance().info(module + " " + 
					"db = " + settings.editDatabaseTypeCode 
					+ ":" + settings.editDatabaseLocation);
//			if (settings.editDatabaseTypeCode != DecodesSettings.DB_SQL)
//			{
//				String msg = module + " This GUI runs only on " +
//				"SQL Database type.";
//				System.out.println(msg);
//				Logger.instance().info(msg);
//				return false;
//			}
			dbio = DatabaseIO.makeDatabaseIO(
				settings.editDatabaseTypeCode, settings.editDatabaseLocation);
			Site.explicitList = true;//do not add new sites to site list
			db.setDbIo(dbio);
			// Initialize standard collections:
			db.enumList.read();
			db.siteList.read();
			db.platformList.read();
			db.networkListList.read();
			Logger.instance().info(module + " " + "read database information");
		} 
		catch (DatabaseException e)
		{
			System.out.println("Can not initialize the Database. " + 
					e.getMessage());
			Logger.instance().failure(module + " " + e.getMessage());
			return false;
		}
		return true;
	}

	/**
	 * Read all Groups that are in the format group_00=
	 * These are the groups that are in the DECODES Database
	 * These names will match with the DECODES Network list
	 * names.
	 * Also we are going to read all the .nl files and create
	 * temporary Network List objs in memory so that we can find
	 * duplicates in here two.
	 * @return true if groups were read successfull, false otherwise
	 */
	private boolean readGroupsFromDcpMonConf()
	{
		boolean result = true;
		Properties rawProps;
		groupNames = new ArrayList<String>();
		dcpMonConf = EnvExpander.expand(dcpMonConf);

		Logger.instance().info(module +
			" Loading group names from '" + dcpMonConf + "'");

		rawProps = new Properties();

		try 
		{
			FileInputStream fis = new FileInputStream(dcpMonConf);
			rawProps.load(fis);
			fis.close();
		}
		catch(IOException ex)
		{
			Logger.instance().warning(module + 
				" Cannot open config file '" + dcpMonConf + "': " + ex
				+ " -- can not initialize DCP Duplicate GUI.");
			return false;
		}
		//The DcpGroupList will keep track of all the network list by
		//groups. For example, MVR-RIVERGAGES-DAS will be one group with 
		//all its DCPs.
		DcpGroupList dgl = DcpGroupList.instance();
		
		//Find the group names - these are the network list names
		Enumeration kenum = rawProps.keys();
		while(kenum.hasMoreElements())
		{
			String key = (String)kenum.nextElement();
			key = key.toLowerCase();
			//Find properties that start with the word group
			if (key.startsWith("group") && key.length() > 6)
			{
				String gName = rawProps.getProperty(key);
				//Get all Group Names that are not .nl network list
				//We will process the groups that are in the 
				//DECODES database they have the format: 
				//group_00=MVD-RIVERGAGES-DAS
				//The District name will be MVD
				//make sure value doesn't start with file:
				if (!gName.toLowerCase().startsWith("file:"))
				{
					groupNames.add(gName);
					//dgl keeps track of network list by groups
					dgl.addDecodesNetworkList(gName, key);
				}	
				else if (gName.toLowerCase().startsWith("file:"))
				{
					//THESE ARE NOT IN DECODES SQL Database - need to 
					//create temporary network list for them
					//Get all Group Names for the .nl files
					//The group name will be the name of the .nl file without
					//the .nl extension Example: SWT.nl will be SWT
					//Group name is basic filename minus dir and ".nl" ext.
					String fName = gName.substring(5);//this gives us SWT.nl
					//Add this .nl network list name to the groupNames
					gName = fName;
					int idx = gName.lastIndexOf(File.separatorChar);
					if (idx != -1)
						gName = gName.substring(idx);//if a file path remove it
					if (gName.toLowerCase().endsWith(".nl"))
						gName = gName.substring(0,gName.length()-3);
					groupNames.add(gName);//this gives us SWT
					//Find the file path to this .nl file - concatenate
					//the fName to the file path to the dcpmon dir
					String dcpMonDir = "";
					//Remove the dcpmon.conf and append the .nl file name
					int index = dcpMonConf.indexOf("dcpmon.conf");
					if (index != -1)
					{	//Get the file system path to the dcpmon dir
						dcpMonDir = dcpMonConf.substring(0,index);		
					}
					dcpMonDir = dcpMonDir + fName;
					File f = new File(dcpMonDir);

					//dgl keeps track of network list by groups
					dgl.addLrgsNetworkList(gName, f, key);
					
					//Create a Decodes network list in memory
					//Add it to the Decodes Network List List in memory
					NetworkList netwList = new NetworkList(gName);
					netwList.transportMediumType = Constants.medium_Goes;
					//----------------Create NL
					lrgs.common.NetworkList lnl;
					try
					{
						lnl = new lrgs.common.NetworkList(f);
						//Get all dcps and add them to the netwList
						for(Iterator it = lnl.iterator(); it.hasNext(); )
						{
							lrgs.common.NetworkListItem nli =
								(lrgs.common.NetworkListItem)it.next();
							String name = nli.name;
							if (name == null || name.trim().length() == 0)
								name = nli.addr.toString();
							name = name.toUpperCase();
						
							NetworkListEntry nle = 
								new NetworkListEntry(netwList,
										nli.addr.toString());
							nle.setDescription(nli.description);
							nle.setPlatformName(name);
							netwList.addEntry(nle);
						}
						//Now add the network list to my networkListList
						Database.getDb().networkListList.add(netwList);
					} catch (IOException ex)
					{
						Logger.instance().log(Logger.E_WARNING, module +
						" Cannot add this  network list '" + 
						f.getName() + 
						" to the in memory Decodes network list': " + ex);
					}
				}	
			}
		}
		return result;
	}
	
	/** Return the PDT file full path */
	public static String getPdtFilePath()
	{
		return pdtFile;
	}
	
	/** Set the PDT file full path */
	public static void setPdtFile(String pdtFileIn)
	{
		pdtFile = pdtFileIn;
	}
	
	/** Return the NWS file full path */
	public static String getNwsFile()
	{
		return nwsFile;
	}

	/** Set the NWS file full path */
	public static void setNwsFile(String nwsFile)
	{
		DuplicateDcpsList.nwsFile = nwsFile;
	}
	
	/** Main method */
	public static void main(String[] args)
	{
		//Need to supply the dcpmon.conf file with full path as an argument
		//This file is needed to find out all the groups that we are
		//going to check for duplicates - Also this conf file will gives
		//us the group names to display in the Controlling Dist combo boxes
		StringToken dcpMonConf;
		//PDT File used in case there is no description when displaying
		//the records in the table
		StringToken pdtFile;
		//NWS File used to get information for the dcps
		StringToken nwsFile;
		
		String logname= "DuplicateDcpsList.log";
		CmdLineArgs cmdLineArgs = new CmdLineArgs(false, logname);
		
		dcpMonConf = new StringToken("f", 
				"dcpmon.conf (full file system path)", "",
				TokenOptions.optRequired, 
				"$DECODES_INSTALL_DIR/dcpmon/dcpmon.conf");
		pdtFile = new StringToken("t", 
				"pdt file (full file system path, " +
				"fill descriptions if empty and PDT Owner Column)", "",
				TokenOptions.optSwitch, 
				"$LRGSHOME/pdt");
		nwsFile = new StringToken("w", 
				"nwshb5 file (full file system path, " +
				"fill NWSHB5 name column and NWS description Column)", "",
				TokenOptions.optSwitch, 
				"$DECODES_INSTALL_DIR/hads");
		
		cmdLineArgs.addToken(dcpMonConf);
		cmdLineArgs.addToken(pdtFile);
		cmdLineArgs.addToken(nwsFile);
		
		try 
		{ 
			cmdLineArgs.parseArgs(args);
			String propFile = cmdLineArgs.getPropertiesFile();
		
			//Create GUI Application
			DuplicateDcpsList dupDcp = new DuplicateDcpsList(propFile,
													dcpMonConf.getValue(),
													pdtFile.getValue(),
													nwsFile.getValue());
			dupDcp.init();
		}
		catch(IllegalArgumentException ex)
		{
			Logger.instance().failure(ex.getMessage());
			System.exit(1);
		}
	}
}
