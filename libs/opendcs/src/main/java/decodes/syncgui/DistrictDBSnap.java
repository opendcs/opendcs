package decodes.syncgui;

import java.util.Date;
import java.io.InputStream;
import java.io.LineNumberReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.text.SimpleDateFormat;

import decodes.syncgui.District;
import decodes.syncgui.FileList;
import decodes.syncgui.PlatList;
import ilex.util.Logger;

/**
 * A district database archived at a known time.
 */
public class DistrictDBSnap
	implements DownloadReader
{
	/** Formatter used to print and parse the directory names containing
	   snapshots. */
	//public static SimpleDateFormat dateFormat
	//	= new SimpleDateFormat("yyyy-MM-dd.HHmm");

	/**
	 * The district owning this database
	 */
	private District myDistrict;

	/** The snapshot directory name */
	private String dirName;

	/** The archive date */
	private String archiveDate;
	
	/** Contains names of site files. */
	private FileList siteFiles;
	
	/** List of Platform Config Files.  */
	private FileList configFiles;
	
	/** List of data source files.  */
	private FileList dataSourceFiles;
	
	/** List of equipment model files. */
	private FileList equipmentFiles;
	
	/** List of network list files. */
	private FileList netlistFiles;
	
	/** List of presentation group files. */
	private FileList presentationFiles;
	
	/** List of routing spec files. */
	private FileList routingFiles;
	
	/** List of platforms. */
	private PlatList platList;

	/** expansion flag */
	private boolean _isExpanded;

	/**
	 * Constructor
	 * @param dist the District
	 * @param archiveDate the archive Date
	 */
	public DistrictDBSnap( District dist, String dirName, String archiveDate )
	{
		this.myDistrict = dist;
		this.dirName = dirName;
		this.archiveDate = archiveDate;
		siteFiles = new FileList(this, "site"); 
		configFiles =  new FileList(this, "config");
		dataSourceFiles =  new FileList(this, "datasource");
		equipmentFiles =  new FileList(this, "equipment");
		netlistFiles =  new FileList(this, "netlist");
		presentationFiles =  new FileList(this, "presentation");
		routingFiles =  new FileList(this, "routing");
		platList = new PlatList(this);
		_isExpanded = false;
	}

	/**
	 * @return formatted date.
	 */
	public String toString( )
	{
		return dirName + " (" + archiveDate + ")";
	}

	/** @return the directory name containing this snapshot */
	public String getDirName()
	{
		return dirName;
	}

	/** @return a full name with the district prefix */
	public String getFullName()
	{
		return myDistrict.getName() + " " + toString();
	}

	/** @return the district */
	public District getDistrict() { return myDistrict; }

	/**
	 * Return the file list with the matching type, or null if no match.
	 */
	public FileList getFileList( String type )
	{
		if (type.equalsIgnoreCase("site"))
			return siteFiles;
		else if (type.equalsIgnoreCase("config"))
			return configFiles;
		else if (type.equalsIgnoreCase("datasource"))
			return dataSourceFiles;
		else if (type.equalsIgnoreCase("equipment"))
			return equipmentFiles;
		else if (type.equalsIgnoreCase("netlist"))
			return netlistFiles;
		else if (type.equalsIgnoreCase("presentation"))
			return presentationFiles;
		else if (type.equalsIgnoreCase("routing"))
			return routingFiles;
		else
			return null;
	}

	/** @return the PlatList */
	public PlatList getPlatList() { return platList; }

	/**
	 * @return true if the file list has previously been read.
	 */
	public boolean isExpanded() { return _isExpanded; }
	
	/**
	 * Reads the file list produced by "tar cvf ..." in the snapshot
	 * directory. Each line will be the name of a file prefaced by the
	 * directory name.
	 * @param strm the input stream.
	 */
	public void readFileList( InputStream strm )
		throws IOException
	{
		Logger.instance().debug1("Reading file list for " + toString());

		LineNumberReader lnr = new LineNumberReader(
			new InputStreamReader(strm));

		siteFiles.clear();
		configFiles.clear();
		dataSourceFiles.clear();
		equipmentFiles.clear();
		netlistFiles.clear();
		presentationFiles.clear();
		routingFiles.clear();

		String line;
		while((line = lnr.readLine()) != null)
		{
			line = line.trim();
			if (line.length() == 0)
				continue;

			if (!line.endsWith(".xml"))
				continue;
			if (line.startsWith("./"))
				line = line.substring(2);
			int idx = line.indexOf("/");
			if (idx == -1)
				continue;
			String dir = line.substring(0, idx);
			String name = line.substring(idx+1);
			if (dir.equals("site"))
				siteFiles.addName(name);
			else if (dir.equals("config"))
				configFiles.addName(name);
			else if (dir.equals("datasource"))
				dataSourceFiles.addName(name);
			else if (dir.equals("equipment"))
				equipmentFiles.addName(name);
			else if (dir.equals("netlist"))
				netlistFiles.addName(name);
			else if (dir.equals("presentation"))
				presentationFiles.addName(name);
			else if (dir.equals("routing"))
				routingFiles.addName(name);
		}

/*
System.out.println("Finished reading " + siteFiles.size() + " sites, "
+ configFiles.size() + " configs, "
+ dataSourceFiles.size() + " sources, "
+ equipmentFiles.size() + " equips, "
+ netlistFiles.size() + " netlists,"
+ presentationFiles.size() + " PGs, "
+ routingFiles.size() + "RS's");
*/
		_isExpanded = true;
	}

	/** from DownloadReader interface */
	public void readFile(String relpath, InputStream strm)
		throws IOException
	{
		if (relpath.endsWith(".xml"))
			readPlatList(strm);
		else
			readFileList(strm);
	}

	/** from DownloadReader interface */
	public void readFailed(String relpath, Exception ex)
	{
		SyncGuiFrame.instance().showError("Cannot download " + relpath
			+ ": " + ex);
	}

	/**
	  Reads the plat list.
	  @param strm the input stream
	*/
	public void readPlatList(InputStream strm)
		throws IOException
	{
		platList.clear();
		platList.read(strm);
	}

	/** Dumps the file list to stdout for testing. */
	public void dump()
	{
		if (!isExpanded())
			return;
		System.out.println("\tFiles for "+myDistrict.getName()+":"+toString());
		siteFiles.dump();
		configFiles.dump();
		dataSourceFiles.dump();
		equipmentFiles.dump();
		netlistFiles.dump();
		presentationFiles.dump();
		routingFiles.dump();
		platList.dump();
	}
}
