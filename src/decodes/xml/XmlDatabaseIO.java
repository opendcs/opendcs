/*
*  $Id$
*  
*  Open Source Software
*  
*  $Log$
*  Revision 1.5  2015/04/15 19:59:47  mmaloney
*  Fixed synchronization bugs when the same data sets are being processed by multiple
*  routing specs at the same time. Example is multiple real-time routing specs with same
*  network lists. They will all receive and decode the same data together.
*
*  Revision 1.4  2014/08/29 18:20:00  mmaloney
*  remove updateTransportId method
*
*  Revision 1.3  2014/08/22 17:23:04  mmaloney
*  6.1 Schema Mods and Initial DCP Monitor Implementation
*
*  Revision 1.2  2014/05/30 13:00:28  mmaloney
*  dev
*
*  Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
*  OPENDCS 6.0 Initial Checkin
*
*  Revision 1.17  2013/05/06 18:17:20  mmaloney
*  Bug in Platform File Name creation. DbKey is now not a number.
*
*  Revision 1.16  2013/03/21 18:27:40  mmaloney
*  DbKey Implementation
*
*/
package decodes.xml;

import ilex.util.Counter;
import ilex.util.FileCounter;
import ilex.util.Logger;
import ilex.xml.LoggerErrorHandler;
import ilex.xml.XmlObjectWriter;
import ilex.xml.XmlOutputStream;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;

import javax.xml.parsers.ParserConfigurationException;

import opendcs.dai.LoadingAppDAI;
import opendcs.dai.PlatformStatusDAI;
import opendcs.dai.ScheduleEntryDAI;

import org.xml.sax.SAXException;

import decodes.db.Constants;
import decodes.db.DataSource;
import decodes.db.DataSourceList;
import decodes.db.DataTypeSet;
import decodes.db.Database;
import decodes.db.DatabaseException;
import decodes.db.DatabaseIO;
import decodes.db.EngineeringUnitList;
import decodes.db.EnumList;
import decodes.db.EquipmentModel;
import decodes.db.EquipmentModelList;
import decodes.db.NetworkList;
import decodes.db.NetworkListList;
import decodes.db.NetworkListSpec;
import decodes.db.Platform;
import decodes.db.PlatformConfig;
import decodes.db.PlatformConfigList;
import decodes.db.PlatformList;
import decodes.db.PlatformSensor;
import decodes.db.PresentationGroup;
import decodes.db.PresentationGroupList;
import decodes.db.RoutingSpec;
import decodes.db.RoutingSpecList;
import decodes.db.Site;
import decodes.db.SiteList;
import decodes.db.SiteName;
import decodes.db.UnitConverterSet;
import decodes.sql.DbKey;
import decodes.tsdb.xml.XmlLoadingAppDAO;
import decodes.util.DecodesSettings;

/**
 * This class allows you to read database information from a
 * collection of XML files stored in a predefined directory hierarchy.
 */
public class XmlDatabaseIO extends DatabaseIO
{
	public static final String module = "XmlDatabaseIO";
	// Directory Structure for the XML Files:
	public static final String PlatformDir = "platform";
	public static final String SiteDir = "site";
	public static final String PlatformConfigDir = "config";
	public static final String RoutingSpecDir = "routing";
	public static final String EnumDir = "enum";
	public static final String EnumListFile = "EnumList.xml";
	public static final String NetworkListDir = "netlist";
	public static final String PresentationGroupDir = "presentation";
	public static final String EUDir = "eu";
	public static final String EUListFile = "EngineeringUnitList.xml";
	public static final String DataTypeDir = "datatype";
	public static final String DataTypeEquivFile = "DataTypeEquivalenceList.xml";
	public static final String PlatformListFile = "PlatformList.xml";
	public static final String PMConfigDir = "pm";
	//public static final String TimeZoneDir = "tz";
	//public static final String TimeZoneFile = "TimeZoneList.xml";
	public static final String EquipmentDir = "equipment";
	public static final String DataSourceDir = "datasource";
	public static final String ScheduleEntryDir = "schedule";
	public static final String PlatformStatusDir = "platstat";
	public static final String LoadingAppDir = "loading-app";
	
	private static String[] subdirs = { PlatformDir, SiteDir, PlatformConfigDir,
		RoutingSpecDir, EnumDir, NetworkListDir, PresentationGroupDir, EUDir,
		DataTypeDir, EquipmentDir, DataSourceDir, ScheduleEntryDir,
		PlatformStatusDir, LoadingAppDir
	};
	
	protected boolean commitAfterSelect = false;

	/**
	 * The 'root' of the tree containing the database directories
	 */
	protected String xmldir;
	protected TopLevelParser myParser;
	protected LoggerErrorHandler errorHandler;
	protected String dtdUri;
	protected FileCounter platformIdCounter;
	protected static NumberFormat platIdFormat;
	static
	{
		platIdFormat = NumberFormat.getNumberInstance();
		platIdFormat.setMaximumIntegerDigits(5);
		platIdFormat.setMinimumIntegerDigits(5);
		platIdFormat.setGroupingUsed(false);
	}

	// Switches
	public static boolean writeDependents = true;

	/**
	 * Construct with a database directory location.
	 * @param xmldir top of hierarchy of XML database
	 * @throws SAXException if can't initialize XML parsers
	 * @throws ParserConfigurationException if can't configure XML parsers
	 */
	public XmlDatabaseIO( String xmldir ) 
		throws SAXException, ParserConfigurationException
	{
		Logger.instance().log(Logger.E_DEBUG1, 
			"Creating XmlDatabaseIO for directory '" + xmldir + "'");
		this.xmldir = xmldir;
		myParser = new TopLevelParser();

		errorHandler = new LoggerErrorHandler();
		errorHandler.stopOnWarnings(false);
		errorHandler.stopOnErrors(false);
		myParser.setErrorHandler(errorHandler);
		platformIdCounter = null;

		dtdUri = null;

		if (xmldir != null && xmldir.length() > 0)
		{
			File xmlTop = new File(xmldir);
			if (!xmlTop.isDirectory())
			{
				if (!xmlTop.mkdirs())
				{
					Logger.instance().warning(module + " Top directory '" + xmldir 
						+ "' does not exist and cannot be created. Check permissions and location.");
					return;
				}
			}
			for(String subdir : subdirs)
			{
				File entdir = new File(xmlTop, subdir);
				if (!entdir.isDirectory() && !entdir.mkdir())
					Logger.instance().warning(module + " Entity directory '" + entdir.getPath() 
						+ "' does not exist and cannot be created. Check permissions and location.");
			}
		}
	}

	/**
	 * @return "XML"
	 */
	public String getDatabaseType( ) { return "XML"; }

	/**
	 * @return the top directory of the hierarchy
	 */
	public String getDatabaseName( ) { return xmldir; }

	/**
	 * @return false: XML database doesn't require login.
	 */
	public boolean requiresLogin( ) { return false; }

	/**
	 * Does nothing.
	 * @param user ignored
	 * @param passwd ignored
	 */
	public void doLogin( String user, String passwd ) {}

	/**
	 * Does nothing.
	 * @return true
	 */
	public boolean isLoggedIn( ) { return true; }

	/**
	 * Does nothing.
	*/
	public void close( )
	{
	}

	/**
	 * @return the TopLevelParser to handle the top element in the XML file.
	 */
	public TopLevelParser getParser( ) { return myParser; }


	// The following three methods are the low-level methods for creating
	// input/output streams and listing directories. The URL sub-class
	// will overload these. All other methods defined herein will then
	// work fine.

	/**
	 * Opens an input stream to read the passed file name.
	 * The root directory for the database is pre-pended.
	 * @param dir the directory
	 * @param name the file name
	 * @return @throws IOException if can't read file
	 */
	protected InputStream getInputStream( String dir, String name ) throws IOException
	{
		String fn = makePath(dir, name);
		Logger.instance().log(Logger.E_DEBUG1,
			"XmlDatabaseIO: Opening '" + fn + "'");
		return new FileInputStream(fn);
	}

	/**
	 * @param dir the directory
	 * @param name the file name, which may or may not end with ".xml"
	 * @return the path
	 */
	public String makePath( String dir, String name )
	{
		String fn = xmldir + File.separator + dir + File.separator + name;
		if (!fn.endsWith(".xml"))
			fn += ".xml";
		return fn;
	}


	/**
	 * Returns the last modify time for an object in this database. The
	 * read methods can use this to determine if an already-loaded copy
	 * is up-to-date.
	 * @param dir the directory
	 * @param name the file name
	 * @return msec last modify time
	 * @throws IOException if IO error.
	 */
	protected long getLastModifyTime( String dir, String name ) throws IOException
	{
		File file = new File(makePath(dir, name));
		return file.lastModified();
	}

	/** Used to select XML files */
	static FilenameFilter xmlFilenameFilter = new FilenameFilter()
		{
			public boolean accept(File dir, String name)
			{
				return name.endsWith(".xml") || name.endsWith(".XML");
			}
		};

	/**
	 * Construct a string array containing a list of files under the
	 * passed directory name.
	 * @param dir the directory within the database hierarchy
	 * @return String array of file names in that directory.
	 */
	protected String[] listDirectory( String dir ) throws IOException
	{
		File fdir = new File(xmldir + File.separatorChar + dir);
		return fdir.list(xmlFilenameFilter);
	}

	/**
	 * Open an output stream for writing to the passed file name.
	 * @param dir the directory
	 * @param name the file name
	 * @return an OutputStream
	 */
	protected OutputStream getOutputStream( String dir, String name ) throws IOException
	{
		String fn = makePath(dir, name);
		return new FileOutputStream(fn);
	}

	/**
	 * At initialization, set a URI for a DTD if there is one.
	 * @param dtdUri URI to use on output files.
	 */
	public void setDtdUri( String dtdUri ) { this.dtdUri = dtdUri; }


	/**
	 * Returns the list of PlatformConfig objects defined in this database.
	 * Parse exceptions will be added to the error handler.
	 * @param pcl object in which to store data
	 * @throws DatabaseException if can't list the directory.
	 */
	public void readConfigList( PlatformConfigList pcl ) throws DatabaseException
	{
		try
		{
			String ls[] = listDirectory(PlatformConfigDir);
			if (ls == null)
				return;

			for(int i=0; i<ls.length; i++)
			{
				InputStream is = null;
				try
				{
					is = getInputStream(PlatformConfigDir, ls[i]);
					PlatformConfig pc = (PlatformConfig)myParser.parse(is);
					pcl.add(pc);
				}
				// Catch other type (IO or bad cast) exceptions
				catch(Exception e)
				{
					String msg = "Error parsing platform config '" 
						+ ls[i] + "' " + e;
					Logger.instance().log(Logger.E_FAILURE, msg);
				}
				finally
				{
					if (is != null)
						try { is.close(); } catch(Exception e) {}
				}
			}
		}
		catch(Exception e)
		{
			throw new DatabaseException(e.toString());
		}
	}

	public PlatformConfig newPlatformConfig(PlatformConfig pc, String model, String owner) 
			throws DatabaseException	
	{
		String prefix = model.trim()+"-"+owner.trim();
		int seqNumber = -1;
		while (seqNumber == -1 ) {
			try {
				seqNumber = nextPlatformSequence(prefix);
				if ( seqNumber > 0 ) {
					String newSeq = String.format("%03d", seqNumber);
					String newName = prefix+"-"+newSeq;
					String xmlPath = xmldir+File.separator+PlatformConfigDir+File.separator+newName+".xml";
					File xmlFile = new File ( xmlPath );
					try {
						boolean noExistingfile=xmlFile.createNewFile();
						if ( noExistingfile ) {
							if ( pc == null )
								pc = new PlatformConfig(newName);
							else
								pc.configName = newName;
							try {
								writeConfig( pc );
							} catch (DatabaseException ex1) {
									throw new DatabaseException ("Could not save new platform configuration - "+newName);
							}
						} else {
							seqNumber = -1;
						}
 					} catch ( Exception ex2 ) {
						throw new DatabaseException ("Could not obtain sequence number for configuration starting with "+ prefix);
					}
				} else {
					throw new DatabaseException ("Could not obtain sequence number for configuration starting with "+ prefix);
				}
			} catch ( DatabaseException e ) {
					throw new DatabaseException (e.toString());
			}
		}
		return(pc);
	}

	private int nextPlatformSequence(String prefix) {
		int seqNo;
		int maxSeq = 0;
		int nextSeq = 0;
		PlatformConfig pc = null;
		try
		{
		  String ls[] = listDirectory(PlatformConfigDir);
		  if (ls == null)
		    return (0);
		  for(int i=0; i<ls.length; i++)
		  {
		    InputStream is = null;
		    try
		    {
		      is = getInputStream(PlatformConfigDir, ls[i]);
		      pc = (PlatformConfig)myParser.parse(is);
					String name = pc.getName();
					if ( name.substring(0,prefix.length()).equals(prefix) ) {
						String seq = name.substring(prefix.length()+1);
						seqNo = Integer.parseInt(seq);
						if ( seqNo > maxSeq )
							maxSeq = seqNo;
		    	}
				}
		    // Catch other type (IO or bad cast) exceptions
		    catch(Exception e)
		    {
		      String msg = "Error parsing platform config '"
		        + ls[i] + "' " + e;
		      Logger.instance().log(Logger.E_FAILURE, msg);
		    }
		    finally
		    {
		      if (is != null)
		        try { is.close(); } catch(Exception e) {}
		    }
		  }
			maxSeq++;
			int[] sequenceNumber = new int[maxSeq];
			for (int l=0; l < maxSeq; l++ )
				sequenceNumber[l] = 0;
		  for(int i=0; i<ls.length; i++)
		  {
		    InputStream is = null;
		    try
		    {
		      is = getInputStream(PlatformConfigDir, ls[i]);
		      pc = (PlatformConfig)myParser.parse(is);
					String name = pc.getName();
					if ( name.substring(0,prefix.length()).equals(prefix) ) {
						String seq = name.substring(prefix.length()+1);
						seqNo = Integer.parseInt(seq) - 1;
						sequenceNumber[seqNo] = 1;
		    	}
				}
		    // Catch other type (IO or bad cast) exceptions
		    catch(Exception e)
		    {
		      String msg = "Error parsing platform config '"
		        + ls[i] + "' " + e;
		      Logger.instance().log(Logger.E_FAILURE, msg);
		    }
		    finally
		    {
		      if (is != null)
		        try { is.close(); } catch(Exception e) {}
		    }
		  }
			nextSeq = maxSeq;
			for (int l=0; l < maxSeq; l++ ) {
				if ( sequenceNumber[l] == 0 ) {
					nextSeq = l + 1;
					break;
				}
			}
		} catch ( Exception e ) {
		  return(0);	
		}
		return nextSeq;
	}

	/**
	 * Returns the list of DataSource objects defined in this database.
	 * Objects in this list may be only partially populated (key values
	 * and primary display attributes only).
	 * @param dsl object in which to store data
	 * @throws DatabaseException if can't list the directory.
	 */
	public void readDataSourceList( DataSourceList dsl ) throws DatabaseException
	{
		try
		{
			String ls[] = listDirectory(DataSourceDir);
			if (ls == null)
				return;
			for(int i=0; i<ls.length; i++)
			{
				InputStream is = null;
				try
				{
					is = getInputStream(DataSourceDir, ls[i]);
					DataSource ob = (DataSource)myParser.parse(is);
					// Don't need to explicitly add, parser will do it.
					//dsl.add(ob);
				}
				// Catch other type (IO or bad cast) exceptions
				catch(Exception e)
				{
					Logger.instance().log(Logger.E_FAILURE,
						"Error parsing data source '" + ls[i] + "' " + e);
				}
				finally
				{
					if (is != null)
						try { is.close(); } catch(Exception e) {}
				}
			}
		}
		catch(Exception e)
		{
			throw new DatabaseException(e.toString());
		}
	}


	/**
	 * Reads the set of known data-type objects in this database.
	 * Objects in this collection are complete.
	 * @param dts object in which to store data
	 * @throws DatabaseException
	 */
	public void readDataTypeSet( DataTypeSet dts ) throws DatabaseException
	{
		Database oldDb = Database.getDb();
		// Make sure correct database is in effect.
		Database.setDb(dts.getDatabase());
		InputStream is = null;
		try
		{
			long lmt = getLastModifyTime(DataTypeDir, DataTypeEquivFile);
			if (dts.getTimeLastRead() < lmt)
			{
				is = getInputStream(DataTypeDir, DataTypeEquivFile);
				myParser.parse(is, dts);
				dts.setTimeLastRead();
			}
		}
		catch(Exception e)
		{
			throw new DatabaseException(e.toString());
		}
		finally
		{
			if (is != null)
				try { is.close(); } catch(Exception e) {}
			Database.setDb(oldDb);
		}
	}

	/**
	 * Writes the DataTypeSet to the database.
	 * @param dts set to write
	 * @throws DatabaseException
	 */
	public void writeDataTypeSet( DataTypeSet dts ) throws DatabaseException
	{
		Database oldDb = Database.getDb();
		// Make sure correct database is in effect.
		Database.setDb(dts.getDatabase());

		try
		{
			String fn = xmldir + File.separatorChar + DataTypeDir
				+ File.separatorChar + DataTypeEquivFile;
			XmlObjectWriter xow = new DataTypeEquivalenceListParser();
			writeDatabaseObject(fn, xow);
		}
		catch (DatabaseException e)
		{
			throw e;
		}
		finally
		{
			Database.setDb(oldDb);
		}
	}


	/**
	 * Returns the list of EngineeringUnit objects defined in this database.
	 * Objects in this collection are complete.
	 * @param eul object in which to store data
	 */
	public void readEngineeringUnitList( EngineeringUnitList eul ) throws DatabaseException
	{
		Database oldDb = Database.getDb();
		// Make sure correct database is in effect.
		Database.setDb(eul.getDatabase());
		InputStream is = null;
		try
		{
			long lmt = getLastModifyTime(EUDir, EUListFile);
			if (eul.getTimeLastRead() < lmt)
			{
				is = getInputStream(EUDir, EUListFile);
				myParser.parse(is, eul);
				eul.setTimeLastRead();
			}
		}
		catch(Exception e)
		{
			e.printStackTrace(System.out);
			throw new DatabaseException(e.toString());
		}
		finally
		{
			if (is != null)
				try { is.close(); } catch(Exception e) {}
			Database.setDb(oldDb);
		}
	}



	/**
	 * Returns the set of known enumeration objects in this database.
	 * Objects in this list may be only partially populated (key values
	 * and primary display attributes only).
	 * @param top object in which to store data
	 */
	public void readEnumList( EnumList top ) throws DatabaseException
	{
		Database oldDb = Database.getDb();
		// Make sure correct database is in effect.
		Database.setDb(top.getDatabase());
		InputStream is = null;
		try
		{
			long lmt = getLastModifyTime(EnumDir, EnumListFile);
			if (top.getTimeLastRead() < lmt)
			{
				is = getInputStream(EnumDir, EnumListFile);
				myParser.parse(is, top);
				top.setTimeLastRead();
			}
		}
		catch(Exception e)
		{
			throw new DatabaseException(e.toString());
		}
		finally
		{
			if (is != null)
				try { is.close(); } catch(Exception e) {}
			Database.setDb(oldDb);
		}

	}


	/**
	 * Returns the list of EquipmentModel objects defined in this database.
	 * Objects in this collection are complete.
	 * @param eml object in which to store data
	 */
	public void readEquipmentModelList( EquipmentModelList eml ) throws DatabaseException
	{
		try
		{
			String ls[] = listDirectory(EquipmentDir);
			if (ls == null)
				return;
			for(int i=0; i<ls.length; i++)
			{
				InputStream is = null;
				try
				{
					is = getInputStream(EquipmentDir, ls[i]);
					EquipmentModel ob = (EquipmentModel)myParser.parse(is);
					eml.add(ob);
				}
				// Catch other type (IO or bad cast) exceptions
				catch(Exception e)
				{
					Logger.instance().log(Logger.E_FAILURE,
						"Error parsing equipment model '" + ls[i] + "' " + e);
				}
				finally
				{
					if (is != null)
						try { is.close(); } catch(Exception e) {}
				}
			}
		}
		catch(Exception e)
		{
			throw new DatabaseException(e.toString());
		}
	}


	/**
	 * Returns the list of NetworkList objects defined in this database.
	 * Objects in this list may be only partially populated (key values
	 * and primary display attributes only).
	 * @param nll object in which to store data
	 */
	public void readNetworkListList( NetworkListList nll ) 
		throws DatabaseException
	{
		try
		{
			String ls[] = listDirectory(NetworkListDir);
			if (ls == null)
				return;
			for(int i=0; i<ls.length; i++)
			{
				InputStream is = null;
				try
				{
					is = getInputStream(NetworkListDir, ls[i]);
					NetworkList ob = (NetworkList)myParser.parse(is);
					nll.add(ob);
				}
				// Catch other type (IO or bad cast) exceptions
				catch(Exception e)
				{
					Logger.instance().log(Logger.E_FAILURE,
						"Error parsing network list '" + ls[i] + "' " + e);
				}
				finally
				{
					if (is != null)
						try { is.close(); } catch(Exception e) {}
				}
			}
		}
		catch(java.io.IOException e) { }
	}
	
	/**
	 * Non-cached, stand-alone method to read the list of network list 
	 * specs currently defined in the database.
	 * @return ArrayList of currently defined network list specs.
	 */
	public ArrayList<NetworkListSpec> getNetlistSpecs()
		throws DatabaseException
	{
		// In XML this will require parsing every network list file.
		NetworkListList nll = new NetworkListList();
		readNetworkListList(nll);
		ArrayList<NetworkListSpec> ret = new ArrayList<NetworkListSpec>();
		for(NetworkList nl : nll.getList())
			ret.add(
				new NetworkListSpec(Constants.undefinedId,
					nl.name, nl.transportMediumType,
					nl.siteNameTypePref, nl.lastModifyTime, nl.size()));
		return ret;
	}


	/**
	 * Read the platform list cross reference file and populate the passed
	 * PlatformList object.
	 * @param top object in which to store data
	 */
	public void readPlatformList( PlatformList top ) 
		throws DatabaseException
	{
		Database oldDb = Database.getDb();
		// Make sure correct database is in effect.
		Database.setDb(top.getDatabase());
		InputStream is = null;
		try
		{
			long lmt = getLastModifyTime(PlatformDir, PlatformListFile);
			if (top.getTimeLastRead() < lmt)
			{
				is = getInputStream(PlatformDir, PlatformListFile);
				myParser.parse(is, top);
				top.setTimeLastRead();
			}
		}
		catch(Exception e)
		{
			e.printStackTrace(System.err);
			throw new DatabaseException(e.toString());
		}
		finally
		{
			if (is != null)
				try { is.close(); } catch(Exception e) {}
			Database.setDb(oldDb);
		}
	}

	public Date getPlatformListLMT()
	{
		try
		{
			return new Date(
				getLastModifyTime(PlatformDir, PlatformListFile));
		}
		catch(IOException ex)
		{
			return new Date(0L);
		}
	}

	public DbKey lookupPlatformId(String mediumType, String mediumId,
		Date timeStamp)
		throws DatabaseException
	{
		
		PlatformList platList = Database.getDb().platformList;
        if (platList.getTimeLastRead() == 0L)
        {
        	readPlatformList(platList);
        }
        Platform p = platList.findPlatform(mediumType, mediumId, timeStamp);
       	if (p == null)
       	{
Logger.instance().debug3("XmlDatabaseIO: lookup - No platform matching " + mediumType + ":" + mediumId);
       		return Constants.undefinedId;
       	}
Logger.instance().debug3("XmlDatabaseIO: lookup - platformID = " + p.getId());
       	return p.getId();
	}
	
	public synchronized DbKey lookupCurrentPlatformId(SiteName sn, 
		String designator, boolean useDesignator)
		throws DatabaseException
	{
		PlatformList platList = Database.getDb().platformList;
        if (platList.getTimeLastRead() == 0L)
        {
        	readPlatformList(platList);
        }

        if (sn.getSite() == null)
        	return Constants.undefinedId;
        Platform plat = platList.findPlatform(sn.getSite(), designator);
        if (plat == null)
        	return Constants.undefinedId;
        return plat.getId();
	}
	
	/**
	 * Reads the list of all Platform objects defined in this database.
	 * Objects in this list may be only partially populated (key values
	 * and primary display attributes only).
	 * @param pl object in which to store data
	 */
	public void readAllPlatforms( PlatformList pl ) throws DatabaseException
	{
		try
		{
			String ls[] = listDirectory(PlatformDir);
			if (ls == null)
				return;
			for(int i=0; i<ls.length; i++)
			{
				InputStream is = null;
				try
				{
					is = getInputStream(PlatformDir, ls[i]);
					Platform ob = (Platform)myParser.parse(is);
					if (ob.lastModifyTime == null)
						ob.lastModifyTime = new Date(
							getLastModifyTime(PlatformDir, ls[i]));
					pl.add(ob);
				}
				// Catch other type (IO or bad cast) exceptions
				catch(Exception e)
				{
					e.printStackTrace(System.out);
					Logger.instance().log(Logger.E_FAILURE,
						"Error parsing platform '" + ls[i] + "' " + e);
				}
				finally
				{
					if (is != null)
						try { is.close(); } catch(Exception e) {}
				}
			}
		}
		catch(Exception e)
		{
			e.printStackTrace(System.err);
			throw new DatabaseException(e.toString());
		}
	}


//	/**
//	  Returns the list of PMConfig objects defined in this database.
//	  Objects in this list may be only partially populated (key values
//	  and primary display attributes only).
//	*/
//	public void readPMConfigList(PMConfigList pmcl)
//		throws DatabaseException
//	{
//		try
//		{
//			String ls[] = listDirectory(PMConfigDir);
//			if (ls == null)
//				return;
//			for(int i=0; i<ls.length; i++)
//			{
//				InputStream is = null;
//				try
//				{
//					is = getInputStream(PMConfigDir, ls[i]);
//					PMConfig ob = (PMConfig)myParser.parse(is);
//					// No need to add -- PMConfig ctor will do it.
//					//pmcl.add(ob);
//				}
//				// Catch other type (IO or bad cast) exceptions
//				catch(Exception e)
//				{
//					Logger.instance().log(Logger.E_FAILURE,
//						"Error parsing perf measurements '" + ls[i] + "' " + e);
//				}
//				finally
//				{
//					if (is != null)
//						try { is.close(); } catch(Exception e) {}
//				}
//			}
//		}
//		catch(Exception e)
//		{
//			throw new DatabaseException(e.toString());
//		}
//	}


	/**
	 * Returns the list of PresentationGroup objects defined in this database.
	 * Objects in this list may be only partially populated (key values
	 * and primary display attributes only).
	 * @param pgl object in which to store data
	 */
	public void readPresentationGroupList( PresentationGroupList pgl ) throws DatabaseException
	{
		try
		{
			String ls[] = listDirectory(PresentationGroupDir);
			if (ls == null)
				return;
			for(int i=0; i<ls.length; i++)
			{
				InputStream is = null;
				try
				{
					is = getInputStream(PresentationGroupDir, ls[i]);
					PresentationGroup ob = (PresentationGroup)
						myParser.parse(is);
					pgl.add(ob);
				}
				// Catch other type (IO or bad cast) exceptions
				catch(Exception e)
				{
					String msg = "Error parsing presentation group '" 
						+ ls[i] + "' " +e;
					Logger.instance().log(Logger.E_FAILURE, msg);
System.err.println(msg);
e.printStackTrace();
				}
				finally
				{
					if (is != null)
						try { is.close(); } catch(Exception e) {}
				}
			}
		}
		catch(Exception e)
		{
			throw new DatabaseException(e.toString());
		}
	}


	/**
	 * Returns the list of RoutingSpec objects defined in this database.
	 * Objects in this list may be only partially populated (key values
	 * and primary display attributes only).
	 * @param rsl object in which to store data
	 * @throws DatabaseException
	 */
	public void readRoutingSpecList( RoutingSpecList rsl ) throws DatabaseException
	{
		try
		{
			String ls[] = listDirectory(RoutingSpecDir);
			if (ls == null)
				return;
			for(int i=0; i<ls.length; i++)
			{
				InputStream is = null;
				try
				{
					is = getInputStream(RoutingSpecDir, ls[i]);
					RoutingSpec ob = (RoutingSpec)myParser.parse(is);
					rsl.add(ob);
				}
				// Catch other type (IO or bad cast) exceptions
				catch(Exception e)
				{
					Logger.instance().log(Logger.E_FAILURE,
						"Error parsing routing spec '" + ls[i] + "' " + e);
				}
				finally
				{
					if (is != null)
						try { is.close(); } catch(Exception e) {}
				}
			}
		}
		catch(Exception e)
		{
			throw new DatabaseException(e.toString());
		}
	}


	/**
	 * Returns the list of Site objects defined in this database.
	 * Objects in this list may be only partially populated (key values
	 * and primary display attributes only).
	 * @param sl object in which to store data
	 * @throws DatabaseException
	 */
	public void readSiteList( SiteList sl ) throws DatabaseException
	{
//System.out.println("Reading Site List");
//try { throw new Exception("x"); }
//catch(Exception ex) { ex.printStackTrace(); }
		try
		{
			String ls[] = listDirectory(SiteDir);
			if (ls == null)
				return;
			for(int i=0; i<ls.length; i++)
			{
				InputStream is = null;
				try
				{
					is = getInputStream(SiteDir, ls[i]);
					Site ob = (Site)myParser.parse(is);
					sl.addSite(ob);
					ob.filename = makePath(SiteDir, ls[i]);
				}
				// Catch other type (IO or bad cast) exceptions
				catch(Exception e)
				{
					Logger.instance().log(Logger.E_FAILURE,
						"Error parsing site '" + ls[i] + "' " + e);
				}
				finally
				{
					if (is != null)
						try { is.close(); } catch(Exception e) {}
				}
			}
		}
		catch(Exception e)
		{
			throw new DatabaseException(e.toString());
		}
	}


	/**
	 * Returns the list of UnitConverter objects defined in this database.
	 * Objects in this list may be only partially populated (key values
	 * and primary display attributes only).
	 * @param ucs object in which to store data
	 * @throws DatabaseException
	 */
	public void readUnitConverterSet( UnitConverterSet ucs ) throws DatabaseException
	{
		// In XML, the unit conversions are stored in the single
		// EU list file. Hence loading the EU list will also load
		// the conversions.
		readEngineeringUnitList(ucs.getDatabase().engineeringUnitList);
	}


	/**
	 * Writes the entire collection of engineering units to the database.
	 * @param top object to write
	 */
	public void writeEngineeringUnitList( EngineeringUnitList top ) throws DatabaseException
	{
		// List is pulled implicitely from current DB. Make sure correct
		// database is in effect.
		Database oldDb = Database.getDb();
		Database.setDb(top.getDatabase());

		try
		{
			XmlObjectWriter xow = new EngineeringUnitListParser();
			String fn = xmldir + File.separatorChar + EUDir
				+ File.separatorChar + EUListFile;
			writeDatabaseObject(fn, xow);
		}
		catch(DatabaseException e)
		{
			throw e;
		}
		finally
		{
			Database.setDb(oldDb);
		}
	}

	//=============== Object-level Read/Write Functions ============

	/**
	 * Reads site information. The passed Site object may only be partially
	 * populated (e.g. from a site list containing names only).
	 * <p>
	 * This method searches for a file matching any of the SiteNames assigned
	 * to this site.
	 * @param site object in which to store data
	 */
	public void readSite( Site site ) throws DatabaseException
	{
		for(Iterator<SiteName> it = site.getNames(); it.hasNext(); )
		{
			SiteName nm = it.next();
			String fn = this.makePath(SiteDir, nm.makeFileName());
			File file = new File(fn);
			if (file.canRead())
			{
				try
				{
					myParser.parse(file, site);
					site.filename = fn;
					return;
				}
				catch(Exception e)
				{
					throw new DatabaseException("Cannot read " + fn + ": " + e);
				}
			}
		}
	}


	/**
	 * Writes site information back to the database.
	 * A file will be written in the sites subdirectory containing this
	 * object's information. The filename will be constructed from the
	 * preferred naming standard (as defined in your decodes.properties)
	 * file. If no name of that type exists, the first name assigned to
	 * this site will be used.
	 * @param site the object to write
	 * @throws DatabaseException if no name is assigned to this object or
	 * if the write fails.
	 */
	public void writeSite( Site site ) throws DatabaseException
	{
		String type = DecodesSettings.instance().siteNameTypePreference;
		SiteName sn = site.getName(type);
		if (sn == null)
		{
			for(Iterator<SiteName> it = site.getNames(); it.hasNext(); )
			{
				sn = it.next();
				break;
			}
			if (sn == null) // No names of any type defined!
				throw new DatabaseException("Cannot save site with no name");
		}
		String pfn = sn.makeFileName();
		String fn = xmldir + File.separator + SiteDir + File.separator + pfn;

		/*
		  If writing a site with a different name, remove the old filename.
		  This can happen in the editor if the preferred site name is changed.
		*/
		if (site.filename != null && !fn.equals(site.filename))
		{
			Logger.instance().log(Logger.E_DEBUG1,
				"XmlDatabaseIO.writeSite: Deleting old site file '"
				+ site.filename + "'");
			try
			{
				File oldFile = new File(site.filename);
				oldFile.delete();
			}
			catch(Exception ex)
			{
				Logger.instance().log(Logger.E_WARNING,
					"XmlDatabaseIO.writeSite: Cannot remove old site file '" 
					+ site.filename + "'");
			}
		}

		site.filename = fn;
		writeDatabaseObject(fn, new SiteParser(site));

		if (writeDependents)
		{
			// Rewrite any platforms that contain this site.
			int numPlatformsWritten = 0;
			for(Iterator<Platform> it = Database.getDb().platformList.iterator();
				it.hasNext(); )
			{
				Platform p = it.next();
				boolean writeThis = false;
				if (p.getSite() == site)
					writeThis = true;
				else if (p.getSite() != null)
				{
					SiteName sn2 = p.getSite().getPreferredName();
					if (sn2 != null)
					{
						String pfn2 = sn2.makeFileName();
						if (pfn2.equals(pfn))
							writeThis = true;
					}
				}
				for(Iterator<PlatformSensor> sit = p.getPlatformSensors(); sit.hasNext(); )
				{
					PlatformSensor ps = sit.next();
					if (ps.site == site)
					{
						writeThis = true;
						break;
					}
				}
				if (writeThis)
				{
					if (!p.isComplete())
						p.read();
					p.setSite(site);
					p.write();
					numPlatformsWritten++;
				}
			}
			if (numPlatformsWritten > 0)
				Database.getDb().platformList.write();
		}
	}


	/**
	 * Deletes a site from the database.
	 * @param site the site to write
	 */
	public void deleteSite( Site site ) throws DatabaseException
	{
		// Attempt to delete all possible file names.
		for(Iterator<SiteName> it = site.getNames(); it.hasNext(); )
		{
			SiteName sn = it.next();
			String fn = xmldir + File.separator + SiteDir
				+ File.separator + sn.makeFileName();
			try { tryDelete(fn); }
			catch(Exception e) {}
		}
	}
	
	public Site getSiteBySiteName(SiteName sn)
		throws DatabaseException
	{
		Site site = new Site();
		site.addName(sn);
		readSite(site);
		return site;
	}


	/**
	 * Reads a complete platform from the database.
	 * This uses this object's platformId member to
	 * uniquely identify the record in the database.
	 * <p>
	 * The resulting platform object will be populated with links to sites,
	 * platform configs, platform sensors, and transport media.
	 * </p>
	 * @param p object in which to store data
	 */
	public void readPlatform( Platform p ) throws DatabaseException
	{
		String fn = makePath(PlatformDir,
			"p" + platIdFormat.format(p.getId().getValue()));

		Logger.instance().log(Logger.E_DEBUG1,
			"XmlDatabaseIO: Reading '" + fn + "'");

		File file = new File(fn);
		if (file.canRead())
		{
			p.lastModifyTime = new Date(file.lastModified());
			try { myParser.parse(file, p); }
			catch(Exception ex)
			{
				String msg = "Error reading '" + fn + "': " + ex;
				System.err.println(msg);
				ex.printStackTrace();
				throw new DatabaseException(msg);
			}
			Logger.instance().debug1("XML readPlatform, fileLMT="
				+ myParser.getFileLMT() + ", platformLMT=" + p.lastModifyTime);

			return;
		}
		throw new DatabaseException(
			"Cannot read platform from file '" + fn + "'");
	}


	/**
	 * Writes a complete platform back to the database.
	 * This uses this object's platformId member to
	 * uniquely identify the record in the database.
	 * @param p the platform to write
	 * @throws DatabaseException
	 */
	public void writePlatform( Platform p ) throws DatabaseException
	{
		if (!p.idIsSet())
			p.setId(DbKey.createDbKey(getPlatformIdCounter().getNextValue()));

		String fn = makePath(PlatformDir,
			"p" + platIdFormat.format(p.getId().getValue()));

		writeDatabaseObject(fn, new PlatformParser(p));
	}

	public static String makeFileName(Platform p)
	{
		return "p" + platIdFormat.format(p.getId().getValue()) + ".xml";
	}

	/**
	 * Returns Date object representing the last modify time for this
	 * platform in the database.
	 * Returns null if the platform no longer exists in the database.
	 * @param p to check
	 * @return last modify time as a Java Date object
	 */
	public Date getPlatformLMT( Platform p ) throws DatabaseException
	{
		String fn = makePath(PlatformDir, "p" + platIdFormat.format(p.getId().getValue()));
		File f = new File(fn);
		if (!f.exists())
		{
			String msg = "Platform '" + p.makeFileName() + "' with file name '"
				+ fn + "' has been deleted.";
			Logger.instance().log(Logger.E_WARNING, msg);
			return null;
		}
		return new Date(f.lastModified());
	}

	/**
	 * Writes the 'platform list' to the database.
	 * This list is an XML file containing abbreviated entries for each
	 * platform. Just enough to populate the Platform List tab in the editor.
	 * @param pl the platform list
	 */
	public void writePlatformList( PlatformList pl ) throws DatabaseException
	{
		String fn = xmldir + File.separator + PlatformDir
			+ File.separator + PlatformListFile;
		writeDatabaseObject(fn, new PlatformListParser(pl));
	}


	/**
	 * Deletes a platform from the database, including its transport
	 * media. It's configuration is not deleted.
	 * @param p the platform to delete
	 */
	public void deletePlatform( Platform p ) throws DatabaseException
	{
		String fn = makePath(PlatformDir,
			"p" + platIdFormat.format(p.getId().getValue()));

		try { tryDelete(fn); }
		catch(Exception e) {}
	}

	/**
	 * Reads a PlatformConfig object from the database.
	 * This uses the object's configName member to uniquely identify
	 * it in the database (not its ID number).
	 * <p>
	 * The resulting PlatformConfig will be complete with links to config-
	 * sensors, decodes scripts (and subordinate script data), and equipment
	 * model.
	 * </p>
	 * @param pc object in which to store data
	 * @throws DatabaseException
	 */
	public void readConfig( PlatformConfig pc ) throws DatabaseException
	{
		String fn = "";
		try
		{
			fn = makePath(PlatformConfigDir, pc.makeFileName());
			myParser.parse(new File(fn), pc);
		}
		catch(Exception e)
		{
			throw new DatabaseException("Error reading '" + fn + "': " + e);
		}
	}

	/**
	 * Writes a PlatformConfig back to the database.
	 * This uses the object's configName member to uniquely identify
	 * it in the database (not its ID number).
	 * @param ob object to write
	 * @throws DatabaseException
	 */
	public void writeConfig( PlatformConfig ob ) throws DatabaseException
	{
		String fn = xmldir + File.separator + PlatformConfigDir
					+ File.separator + ob.makeFileName();
		writeDatabaseObject(fn, new PlatformConfigParser(ob));
	}

	/**
	 * Deletes a platform configuration from the database.
	 * This uses the object's configName member to uniquely identify
	 * it in the database (not its ID number).
	 * @param ob the object to delete
	 */
	public void deleteConfig( PlatformConfig ob ) throws DatabaseException
	{
		String fn = xmldir + File.separator + PlatformConfigDir
					+ File.separator + ob.makeFileName();
		tryDelete(fn);
	}

	/**
	 * Read (or re-read) a single EquipmentModel from the database.
	 * This uses the EquipmentModel's name (not it's ID number) to
	 * uniquely identify the record in the database.
	 * @param em object in which to store data
	 * @throws DatabaseException
	 */
	public void readEquipmentModel( EquipmentModel em ) throws DatabaseException
	{
		String fn = "";
		try
		{
			fn = makePath(EquipmentDir, em.makeFileName());
			myParser.parse(new File(fn), em);
		}
		catch(Exception e)
		{
			throw new DatabaseException("Error reading '" + fn + "': " + e);
		}
	}

	/**
	 * Write an EquipmentModel to the database.
	 * This uses the EquipmentModel's name (not it's ID number) to
	 * uniquely identify the record in the database.
	 * @param ob object to write
	 * @throws DatabaseException
	 */
	public void writeEquipmentModel( EquipmentModel ob ) throws DatabaseException
	{
		String fn = xmldir + File.separator + EquipmentDir
					+ File.separator + ob.makeFileName();
		writeDatabaseObject(fn, new EquipmentModelParser(ob));
	}


	/**
	 * Deletes an EquipmentModel object from the database.
	 * @param ob the object to delete
	 */
	public void deleteEquipmentModel( EquipmentModel ob ) throws DatabaseException
	{
		String fn = xmldir + File.separator + EquipmentDir
					+ File.separator + ob.makeFileName();
		tryDelete(fn);
	}

//	public void readEquationSpec( EquationSpec ob )
//		throws DatabaseException
//	{
//		String fn = "";
//		try
//		{
//			fn = makePath(EquationDir, ob.makeFileName());
//			myParser.parse(new File(fn), ob);
//		}
//		catch(Exception e)
//		{
//			throw new DatabaseException("Error reading '" + fn + "': " + e);
//		}
//	}
//
//
//	public void writeEquationSpec( EquationSpec ob )
//		throws DatabaseException
//	{
//		String fn = xmldir + File.separator + EquationDir
//					+ File.separator + ob.makeFileName();
//		writeDatabaseObject(fn, new EquationSpecParser(ob));
//	}
//
//	public void deleteEquationSpec( EquationSpec ob )
//		throws DatabaseException
//	{
//		String fn = xmldir + File.separator + EquationDir
//					+ File.separator + ob.makeFileName();
//		tryDelete(fn);
//	}

	/**
	 * This reads the PresentationGroup object from the XML database,
	 * using it's groupName to identify the record in the database.
	 * @param ob object in which to store data
	 */
	public void readPresentationGroup( PresentationGroup ob ) throws DatabaseException
	{
		String fn = "";
		try
		{
			fn = makePath(PresentationGroupDir, ob.makeFileName());
			myParser.parse(new File(fn), ob);
		}
		catch(Exception e)
		{
			throw new DatabaseException("Error reading '" + fn + "': " + e);
		}
	}

	/**
	 * Writes a presentation group and all of its DataPresentation elements
	 * out to the database.
	 * @param ob object to write
	 */
	public void writePresentationGroup( PresentationGroup ob ) throws DatabaseException
	{
		String fn = makePath(PresentationGroupDir, ob.makeFileName());
		writeDatabaseObject(fn, new PresentationGroupParser(ob));
	}

	/**
	 * Deletes a presentation group and all of its DataPresentation elements
	 * from the database.
	 * @param ob the object to delete
	 */
	public void deletePresentationGroup( PresentationGroup ob ) throws DatabaseException
	{
		String fn = makePath(PresentationGroupDir, ob.makeFileName());
		tryDelete(fn);
	}

	/**
	 * Returns Date object representing the last modify time for this
	 * presentation group in the database.
	 * Returns null if the presentation group no longer exists in the database.
	 * @param pg the PresentationGroup to check
	 * @return last modify time as a Date object
	 */
	public Date getPresentationGroupLMT( PresentationGroup pg ) throws DatabaseException
	{
		String fn = makePath(PresentationGroupDir, pg.makeFileName());
		File f = new File(fn);
		if (!f.exists())
		{
			String msg = "Presentation Group '" + fn + "' has been deleted.";
			Logger.instance().log(Logger.E_WARNING, msg);
			return null;
		}
		return new Date(f.lastModified());
	}

	/**
	 * Reads a routing spec from the database.
	 * @param ob object in which to store data
	 */
	public void readRoutingSpec( RoutingSpec ob ) throws DatabaseException
	{
		String fn = "";
		try
		{
			fn = makePath(RoutingSpecDir, ob.makeFileName());
			myParser.parse(new File(fn), ob);
		}
		catch(Exception e)
		{
			throw new DatabaseException("Error reading '" + fn + "': " + e);
		}
	}


	/**
	 * Writes a routing spec to the database.
	 * @param ob object to write
	 */
	public void writeRoutingSpec( RoutingSpec ob ) throws DatabaseException
	{
		String fn = makePath(RoutingSpecDir, ob.makeFileName());
		writeDatabaseObject(fn, new RoutingSpecParser(ob));
	}

	/**
	 * Deletes a RoutingSpec from the database
	 * @param ob the object to delete
	 * @throws DatabaseException
	 */
	public void deleteRoutingSpec( RoutingSpec ob ) throws DatabaseException
	{
		String fn = makePath(RoutingSpecDir, ob.makeFileName());
		tryDelete(fn);
	}

	/**
	 * Returns Date object representing the last modify time for this
	 * routing spec in the database.
	 * Returns null if the routing spec no longer exists in the database.
	 * @param rs the RoutingSpec to check
	 * @return last-modify time as a Date object
	 */
	public Date getRoutingSpecLMT( RoutingSpec rs ) throws DatabaseException
	{
		String fn = makePath(RoutingSpecDir, rs.makeFileName());
		File f = new File(fn);
		if (!f.exists())
		{
			String msg = "Routing Spec '" + fn + "' has been deleted.";
			Logger.instance().log(Logger.E_WARNING, msg);
			return null;
		}
		return new Date(f.lastModified());
	}

	/**
	 * Reads (or re-reads) the DataSource object from the XML database.
	 * This uses the argument's name member to identify the record in the
	 * database.
	 * @param ob object in which to store data
	 */
	public void readDataSource( DataSource ob ) throws DatabaseException
	{
		String fn = "";
		try
		{
			fn = makePath(DataSourceDir, ob.makeFileName());
			Logger.instance().log(Logger.E_DEBUG1,
				"XmlDatabaseIO: Reading DataSource '" + fn + "'");
			myParser.parse(new File(fn), ob);
		}
		catch(Exception e)
		{
			throw new DatabaseException("Error reading '" + fn + "': " + e);
		}
	}


	/**
	 * Writes a DataSource to the database.
	 * @param ob object to write
	 * @throws DatabaseException
	 */
	public void writeDataSource( DataSource ob ) throws DatabaseException
	{
		String fn = xmldir + File.separator + DataSourceDir
					+ File.separator + ob.makeFileName();
		writeDatabaseObject(fn, new DataSourceParser(ob));
	}

	/**
	 * Deletes a DataSource in the database.
	 * @param ob the object to delete
	 * @throws DatabaseException
	 */
	public void deleteDataSource( DataSource ob ) throws DatabaseException
	{
		String fn = xmldir + File.separator + DataSourceDir
					+ File.separator + ob.makeFileName();
		tryDelete(fn);
	}

	/**
	 * Reads (or re-reads) a NetworkList from the database.  This uses
	 * the object's name member (not its ID) to uniquely identify the
	 * record in the database.
	 * @param ob object in which to store data
	 */
	public void readNetworkList( NetworkList ob ) throws DatabaseException
	{
		String fn = "";
		try
		{
			fn = makePath(NetworkListDir, ob.makeFileName());
			myParser.parse(new File(fn), ob);
		}
		catch(Exception e)
		{
			throw new DatabaseException("Error reading '" + fn + "': " + e);
		}
	}

	/**
	 * Writes a NetworkList to the database.  This uses
	 * the object's name member (not its ID) to uniquely identify the
	 * record in the database.
	 * @param ob object to write
	 * @throws DatabaseException
	 */
	public void writeNetworkList( NetworkList ob ) throws DatabaseException
	{
		String fn = makePath(NetworkListDir, ob.makeFileName());
		writeDatabaseObject(fn, new NetworkListParser(ob));
	}

	/**
	 * Deletes a NetworkList from the database.
	 * @param ob the object to delete
	 * @throws DatabaseException
	 */
	public void deleteNetworkList( NetworkList ob ) throws DatabaseException
	{
		String fn = makePath(NetworkListDir, ob.makeFileName());
		tryDelete(fn);
	}

	/**
	 * Returns Date object representing the last modify time for this
	 * network list in the database.
	 * Returns null if the network list no longer exists in the database.
	 * @param nl the NetworkList to check
	 * @return Last modify time as a Date object
	 */
	public Date getNetworkListLMT( NetworkList nl ) throws DatabaseException
	{
		if (nl == NetworkList.dummy_all || nl == NetworkList.dummy_production)
			return this.getPlatformListLMT();

		String fn = makePath(NetworkListDir, nl.makeFileName());
		File f = new File(fn);
		if (!f.exists())
		{
			String msg = "Network List '" + fn + "' has been deleted.";
			Logger.instance().log(Logger.E_WARNING, msg);
			return null;
		}
		return new Date(f.lastModified());
	}

	/**
	 * Writes EnumList to the database.
	 * @param ob object to write
	 */
	public void writeEnumList( EnumList ob ) throws DatabaseException
	{
		String fn = xmldir + File.separator + EnumDir
					+ File.separator + EnumListFile;
		writeDatabaseObject(fn, new EnumListParser(ob.getDatabase()));
	}


	/**
	 * Local helper method to write objects & catch IO exceptions.
	 * @param fn the file name
	 * @param xow the XmlObjectWriter pre-configured.
	 */
	private void writeDatabaseObject( String fn, XmlObjectWriter xow ) throws DatabaseException
	{
		FileOutputStream os = null;
		if (!fn.endsWith(".xml"))
			fn = fn + ".xml";
		try
		{
			Logger.instance().log(Logger.E_DEBUG1,
				"XmlDatabaseIO: Writing '" + fn + "'");
			os = new FileOutputStream(new File(fn));
			XmlOutputStream xos = new XmlOutputStream(os, xow.myName());
			xos.xmlDtdUri = dtdUri;
			xos.writeXmlHeader();
			xow.writeXml(xos);
		}
		catch(Exception e)
		{
e.printStackTrace();
			throw new DatabaseException("Error writing '" + fn + "': " + e);
		}
		finally
		{
			if (os != null)
				try { os.close(); } catch(Exception e){}
		}
	}

	/**
	 * Convenience method to delete a file and handle exceptions.
	 * @param fn the file name.
	 * @throws DatabaseException if file cannot be deleted.
	 */
	private void tryDelete( String fn ) throws DatabaseException
	{
		if (!fn.endsWith(".xml"))
			fn = fn + ".xml";
		File file = new File(fn);
		if (file.exists())
		{
			try
			{
				Logger.instance().log(Logger.E_DEBUG1,
					"Deleting '" + fn + "' ");
				file.delete();
			}
			catch (Exception e)
			{
				String err = "Cannot delete '" + fn + "': " + e;
				Logger.instance().log(Logger.E_FAILURE, err);
				throw new DatabaseException(err);
			}
		}
		else
		{
			Logger.instance().debug1(
				"Could not delete " + fn + ": file doesn't exist!");
		}
	}

	/**
	 * Returns counter to use to generate new Platform IDs.
	 * @return Counter object
	 */
	public Counter getPlatformIdCounter( )
	{
		if (platformIdCounter == null)
		{
			String fn = xmldir + File.separator + PlatformDir
					+ File.separator + "PlatformIdCounter";

			try { platformIdCounter = new FileCounter(fn); }
			catch(IOException e)
			{
				Logger.instance().log(Logger.E_FAILURE,
					"Cannot get platform ID file counter at '" + fn + "': "+e);
			}
		}
		return platformIdCounter;
	}

    public boolean commitAfterSelectStatus()
    {
      return(commitAfterSelect);
    }
    public void setCommitAfterSelect(boolean status)
    {
      commitAfterSelect=status;
    }

	/* (non-Javadoc)
	 * @see decodes.db.DatabaseIO#readNetworkListName(java.lang.String)
	 */
	@Override
	public ArrayList<String> readNetworkListName(String transportId)
			throws DatabaseException 
	{
		return null;
	}

	@Override
	public LoadingAppDAI makeLoadingAppDAO()
	{
		return new XmlLoadingAppDAO(xmldir);
	}
	
	@Override
	public ScheduleEntryDAI makeScheduleEntryDAO()
	{
		return new XmlScheduleEntryDAO(this);
	}
	
	@Override
	public PlatformStatusDAI makePlatformStatusDAO()
	{
		return new XmlPlatformStatusDAO(this);
	}

}
