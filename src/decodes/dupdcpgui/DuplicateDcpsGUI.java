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

import opendcs.dai.LoadingAppDAI;
import decodes.db.Constants;
import decodes.db.Database;
import decodes.db.DatabaseException;
import decodes.db.DatabaseIO;
import decodes.db.NetworkList;
import decodes.db.NetworkListEntry;
import decodes.db.Site;
import decodes.dcpmon.DcpMonitorConfig;
import decodes.tsdb.CompAppInfo;
import decodes.tsdb.DbIoException;
import decodes.tsdb.NoSuchObjectException;
import decodes.tsdb.TsdbAppTemplate;
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
public class DuplicateDcpsGUI 
	extends TsdbAppTemplate
{
	private String module = "DuplicateDcpsList";
	private DuplicateDcpsFrame dupDcpsFrame;
	private String dcpMonConf;
	private ArrayList<NetworkList> groups = new ArrayList<NetworkList>();
	public static final String GROUP_DELIMITER_NAME = "-RIVERGAGES-DAS";
	
	//PDT File used in case there is no description when displaying
	//the records in the table
	private StringToken pdtFileArg = new StringToken("T", 
		"pdt file (full file system path, " +
		"fill descriptions if empty and PDT Owner Column)", "",
		TokenOptions.optSwitch, 
		"$LRGSHOME/pdt");
	//NWS File used to get information for the dcps
	private StringToken nwsFileArg = new StringToken("w", 
		"nwshb5 file (full file system path, " +
		"fill NWSHB5 name column and NWS description Column)", "",
		TokenOptions.optSwitch, 
		"$DECODES_INSTALL_DIR/hads");
	private boolean _shutdown = false;

	private DcpMonitorConfig dcpmonConfig = new DcpMonitorConfig();



	/** Constructor. Initialize class */
	public DuplicateDcpsGUI()
	{
		super("dupdcps.log");
	}

	/**
	 * Read Group names from dcpmon.conf
	 * Read Decodes Database.
	 * Create the DuplicateDcpsFrame class.
	 */
	public void init()
	{
		LoadingAppDAI loadingAppDAO = Database.getDb().getDbIo().makeLoadingAppDAO();
		try
		{
			CompAppInfo dcpmonAppInfo = loadingAppDAO.getComputationApp("dcpmon");
			dcpmonConfig.loadFromProperties(dcpmonAppInfo.getProperties());
			groups.clear();
			for(Enumeration en = dcpmonAppInfo.getPropertyNames(); en.hasMoreElements(); )
			{
				String nm = (String)en.nextElement();
				if (nm.toLowerCase().startsWith("grp:"))
				{
					String listName = nm.substring(4);
					String displayName = dcpmonAppInfo.getProperty(nm);
					NetworkList netlist = Database.getDb().networkListList.find(listName);
					if (netlist == null)
					{
						System.err.println("AppInfo specifies non-existent network list '" + nm + "'");
						continue;
					}
					groups.add(netlist);
				}
			}
		}
		catch (Exception e)
		{
			System.err.println("Error in init: " + e);
			e.printStackTrace();
		}
		finally
		{
			loadingAppDAO.close();
		}
		
		dupDcpsFrame = new DuplicateDcpsFrame(this);
		
		// TODO - use my status files to track changes to size & position.
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
	}

	
	/** Return the PDT file full path */
	public String getPdtFilePath()
	{
		return EnvExpander.expand(pdtFileArg.getValue());
	}
	
	
	/** Return the NWS file full path */
	public String getNwsFilePath()
	{
		return EnvExpander.expand(nwsFileArg.getValue());
	}

	/** Main method */
	public static void main(String[] args)
		throws Exception
	{
		DuplicateDcpsGUI dupDcp = new DuplicateDcpsGUI();
		dupDcp.execute(args);
	}

	@Override
	protected void runApp() throws Exception
	{
		init();
		dupDcpsFrame.setVisible(true);
		while(!_shutdown)
		{
			try { Thread.sleep(1000L); } catch(Exception ex) {}
		}
	}

	@Override
	protected void addCustomArgs(CmdLineArgs cmdLineArgs)
	{
		cmdLineArgs.addToken(pdtFileArg);
		cmdLineArgs.addToken(nwsFileArg);
	}

	public ArrayList<NetworkList> getGroups()
	{
		return groups;
	}

	public void shutdown()
	{
		this._shutdown = true;
	}
}
