/**
 * $Id$
 * 
 * Copyright 2015 U.S. Army Corps of Engineers, Hydrologic Engineering Center.
 * 
 * $Log$
 * Revision 1.4  2015/09/17 17:44:55  mmaloney
 * CWMS Screening I/O and Algorithm
 *
 * Revision 1.3  2015/09/10 21:18:07  mmaloney
 * Development on Screening
 *
 * Revision 1.2  2015/05/14 13:52:17  mmaloney
 * RC08 prep
 *
 * Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
 * OPENDCS 6.0 Initial Checkin
 *
 * Revision 1.14  2013/03/21 18:27:40  mmaloney
 * DbKey Implementation
 *
 */
package decodes.cwms.validation;

import ilex.util.EnvExpander;
import ilex.util.Logger;

import java.io.File;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.FileReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.StringTokenizer;
import java.util.TimeZone;

import ilex.xml.DomHelper;
import decodes.db.Constants;
import decodes.tsdb.DbCompException;
import decodes.tsdb.IntervalCodes;
import decodes.tsdb.IntervalIncrement;
import decodes.tsdb.TimeSeriesIdentifier;
import decodes.util.DecodesSettings;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;



public class DatchkReader
{
	public String module = "DatchkValidation";
	private static DatchkReader _instance = null;

	private LoadedFile cfgFile = null;
	private HashMap<String, PathMapping> dssPath2pathMap = 
		new HashMap<String, PathMapping>();
	private ArrayList<LoadedFile> loadedFiles = 
		new ArrayList<LoadedFile>();
	private HashMap<String, GregorianCalendar> seasons
		= new HashMap<String, GregorianCalendar>();
	private ScreeningCriteria critBuffer = null;
	private transient Calendar datchkSeason = null;
	private transient String testName = null;
	private HashMap<String, ScreeningCriteria> storedTests
		= new HashMap<String, ScreeningCriteria>();
	private HashMap<String, Screening> tsidScreeningMap = new HashMap<String, Screening>();
	private HashSet<String> screenedTsids = new HashSet<String>();
	private TimeZone seasonTz = TimeZone.getTimeZone("UTC");

	private DatchkReader()
	{
		cfgFile = new LoadedFile(
			EnvExpander.expand(DecodesSettings.instance().datchkConfigFile));
		Logger.instance().info(module + " created new instance with config file '"
			+ cfgFile.getPath() + "'");
		seasonTz = TimeZone.getTimeZone(
			DecodesSettings.instance().aggregateTimeZone);
	}
	
	/**
	 * For DatchkExport, allow creation with an alternate config file.
	 * @param cfgFile the config file name.
	 */
	public DatchkReader(String cfgFileName)
	{
		cfgFile = new LoadedFile(
			EnvExpander.expand(cfgFileName));
		Logger.instance().info(module + " created new instance with config file '"
			+ cfgFile.getPath() + "'");
		seasonTz = TimeZone.getTimeZone(
			DecodesSettings.instance().aggregateTimeZone);
	}

	public static DatchkReader instance()
	{
		if (_instance == null)
			_instance = new DatchkReader();
		return _instance;
	}
	
	public Screening getScreening(TimeSeriesIdentifier tsid)
		throws DbCompException
	{
		checkConfig();
		return findScreening(tsid.getUniqueString());
	}
	
	public Screening findScreening(String tsidStr)
	{
		String uc = tsidStr.toUpperCase();
		Logger.instance().debug1(module + " looking for match to '" + tsidStr + "'");
		Screening ret = tsidScreeningMap.get(uc);
		if (ret == null)
		{
			Logger.instance().debug1("No screening defined for '"
				+ uc + "', " + tsidScreeningMap.size()
				+ " screenings defined.");
		}
		else
			Logger.instance().debug1("Found screening for '" + tsidStr + "'");
		return ret;
	}
	
	/**
	 * Checks to see if the configuration file has changed. If so it loads it
	 * and then loads all of the caches.
	 * 
	 * @throws DbCompException
	 */
	private void checkConfig() throws DbCompException
	{
		if (!cfgFile.canRead())
			throw new DbCompException(module + " Cannot open configuration '"
					+ cfgFile.getPath() + "'");
		boolean doReload = false;
		if (cfgFile.lastModified() > cfgFile.getLastRead())
		{
			doReload = true;
			Logger.instance().info(module + " must reload because config file was changed.");
		}
		else
			for (LoadedFile lf : loadedFiles)
			{
				if (lf.lastModified() > lf.getLastRead())
				{
					doReload = true;
					Logger.instance().info(module + " must reload because file '"
						+ lf.getPath() + "' was changed.");
				}
			}
		if (doReload)
		{
			loadedFiles.clear();
			dssPath2pathMap.clear();
			seasons.clear();
			tsidScreeningMap.clear();
			screenedTsids.clear();
			reloadAll();
		}
	}

	public void reloadAll() 
		throws DbCompException
	{
		Logger.instance().info(module + " reloading all files.");
		// Load the configuration file. Throw DbCompException if error.
		Document doc;
		try
		{
			doc = DomHelper.readFile(module, cfgFile.getPath());
			cfgFile.setLastRead(System.currentTimeMillis());
		}
		catch(ilex.util.ErrorException ex)
		{
			throw new DbCompException(module + " Cannot read config file '"
				+ cfgFile.getPath() + "': " + ex);
		}

		Node topElement = doc.getDocumentElement();
		NodeList children = topElement.getChildNodes();
		if (children == null)
		{
			fileWarning(cfgFile, -1, 
				"Empty config! No mappings or datchk files specified!");
			return;
		}
		
		// First walk the tree and get path mappings and seasons
		SimpleDateFormat sdf = new SimpleDateFormat("MMM dd HH:mm");
		sdf.setTimeZone(seasonTz);
		for(int i=0; i<children.getLength(); i++)
		{
			Node node = children.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE)
			{
				Element elem = (Element)node;		
				if (node.getNodeName().equalsIgnoreCase("timezone"))
				{
					String tx = DomHelper.getTextContent(node);
					TimeZone tz = TimeZone.getTimeZone(tx);
					if (tz != null)
					{
						seasonTz = tz;
						sdf.setTimeZone(seasonTz);
					}
					else
						fileWarning(cfgFile, -1,
							"Invalid TimeZone '" + tx + "' -- ignored.");
				}
				else if (node.getNodeName().equalsIgnoreCase("season"))
				{
					String seasonName = DomHelper.findAttr(elem, "name");
					if (seasonName == null)
					{
						fileWarning(cfgFile, -1,
							"season with no 'name' attribute -- ignored.");
						continue;
					}
					String tx = DomHelper.getTextContent(node);
					try
					{
						Date d = sdf.parse(tx);
						GregorianCalendar gc = new GregorianCalendar();
						gc.setTimeZone(seasonTz);
						gc.setTime(d);
						seasons.put(seasonName, gc);
					}
					catch(ParseException ex)
					{
						fileWarning(cfgFile, -1,
							"Invalid date format for season " + seasonName
							+ " '" + tx + "' -- season ignored.");
					}
				}
				else if (node.getNodeName().equalsIgnoreCase("pathmap"))
				{
					String tx = DomHelper.getTextContent(node);
					loadPathmapFile(tx);
				}
				else if (!node.getNodeName().equalsIgnoreCase("datchk"))
				{
					fileWarning(cfgFile, -1,
						"Unrecognized element '" + node.getNodeName()
						+ " -- ignored.");
				}
			}
		}

		// Walk the DOM tree again
		// Load each DATCHK file with the optional season.
		// Issue warnings for read & parse errors but don't throw.
		for(int i=0; i<children.getLength(); i++)
		{
			Node node = children.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE)
			{
				Element elem = (Element)node;
				if (node.getNodeName().equalsIgnoreCase("datchk"))
				{
					String filename = DomHelper.getTextContent(node);
					GregorianCalendar gc = null;
					String seasonName = DomHelper.findAttr(elem, "season");
					if (seasonName != null)
					{
						gc = seasons.get(seasonName);
						if (gc == null)
							fileWarning(cfgFile, -1,
								"Unrecognzied season name '"
								+ seasonName + "' for datchk file "
								+ filename + " -- ignored.");
						continue;
					}
					loadDatchkFile(filename, gc);
				}
			}
		}
		
		Logger.instance().info(module + " Loaded " + tsidScreeningMap.size()
			+ " time-series screenings.");
	}
	
	private void loadPathmapFile(String filename)
	{
		LineNumberReader lnr = null;
		LoadedFile file = new LoadedFile(filename);
		file.setLastRead(System.currentTimeMillis());
		loadedFiles.add(file);

		try
		{
			lnr = new LineNumberReader(new FileReader(file));
			String line;
			while((line = lnr.readLine()) != null)
			{
				line = line.trim();
				if (line.length() == 0 || line.charAt(0) == '#')
					continue;
				StringTokenizer st = new StringTokenizer(line, ";");
				String mapping = st.nextToken();
				int idx = mapping.indexOf('=');
				if (idx == -1)
				{
					fileWarning(file, lnr.getLineNumber(), 
						"Path assignment with no equal sign '" + mapping
						+ "' -- ignored.");
					continue;
				}
				else if (idx >= mapping.length()-1)
				{
					fileWarning(file, lnr.getLineNumber(),
						"Missing DSS Path in '" + mapping
						+ "' -- ignored.");
					continue;
				}
				String cwmsPath = mapping.substring(0,idx).trim();
				if (cwmsPath.length() == 0)
				{
					fileWarning(file, lnr.getLineNumber(),
						"Missing CWMS Time Series ID in '" + mapping
						+ "' -- ignored.");
					continue;
				}
				String dssPath = mapping.substring(idx+1).trim();
				if (dssPath.length() == 0)
				{
					fileWarning(file, lnr.getLineNumber(),
						"Missing DSS Path in '" + mapping
						+ "' -- ignored.");
					continue;
				}
				String units = null;
				while(st.hasMoreTokens())
				{
					String t = st.nextToken();
					if (t.toLowerCase().startsWith("units"))
					{
						idx = t.indexOf('=');
						if (idx == -1 || idx>=t.length()-1)
						{
							fileWarning(file, lnr.getLineNumber(),
								"Bad units assignment '" + t
								+ "' -- ignored.");
							continue;
						}
						units = t.substring(idx+1).trim();
						if (units.length() == 0)
						{
							fileWarning(file, lnr.getLineNumber(),
								"Missing units assignment '" + t
								+ "' -- ignored.");
							units = null;
						}
					}
				}
				dssPath2pathMap.put(dssPath, new PathMapping(dssPath, cwmsPath, units));
			}
		}
		catch (IOException ex)
		{
			Logger.instance().warning(module + " Error reading pathmap file '"
				+ filename + "': " + ex);
		}
		finally
		{
			if (file != null)
				file.setLastRead(System.currentTimeMillis());
			if (lnr != null)
			{
				try {lnr.close(); } catch(Exception ex) {}
			}
		}
	}
	
	private void fileWarning(File file, int linenum, String msg)
	{
		String w = module + " file " + file.getPath() + " ";
		if (linenum >= 0)
			w = w + "line=" + linenum + " ";
		w = w + msg;
		Logger.instance().warning(w);	}

	/**
	 * Loads a datchk file optionally to a season
	 * @param filename datchk file name containing the validations.
	 * @param season season name or null if these are all-time checks.
	 */
	private void loadDatchkFile(String filename, Calendar season)
	{
		LineNumberReader lnr = null;
		LoadedFile file = new LoadedFile(filename);
		file.setLastRead(System.currentTimeMillis());
		loadedFiles.add(file);
		datchkSeason = season;
		
		Logger.instance().debug1("Loading DATCHK file '" + filename + "'");

		try
		{
			flushCriteria();
			lnr = new LineNumberReader(new FileReader(file));
			String line;
			DatchkKeyword prevKeyword = null;
			while((line = lnr.readLine()) != null)
			{
				line = line.trim();
				if (line.length() == 0 || line.charAt(0) == '*')
					continue;
				
				StringTokenizer st = new StringTokenizer(line, " ");
				String kw = st.nextToken();
				DatchkKeyword keyword = DatchkKeyword.token2keyword(kw);
				if (keyword == null)
				{
					fileWarning(file, lnr.getLineNumber(),
						"Unrecognized keyword '" + kw + "' -- ignored.");
					continue;
				}
				if (keyword != DatchkKeyword.DATA
				 && prevKeyword == DatchkKeyword.DATA)
					flushCriteria();
						
				int lineNum = lnr.getLineNumber();
				switch(keyword)
				{
				case DEFINE:
					startNamedTest(st, file, lineNum);
					break;
				case END:
					endNamedTest();
					break;
				case TEST:
					recallNamedTest(st, file, lineNum);
					break;
				case CRITERIA:
					specifyCriteria(st, file, lineNum);
					break;
				case DATA:
					specifyData(line, file, lineNum);
					break;
				case CRITFILE:
				case TIME:
				case CONTEXT:
					break;
				
				default:
					fileWarning(file, lnr.getLineNumber(), 
						"Keyword '"
						+ keyword.name() + "' ignored.");
				}
				prevKeyword = keyword;

			}
		}
		catch (IOException ex)
		{
			Logger.instance().warning(module + " Error reading datchk file '"
				+ filename + "': " + ex);
		}
		finally
		{
			if (file != null)
				file.setLastRead(System.currentTimeMillis());
			if (lnr != null)
			{
				try {lnr.close(); } catch(Exception ex) {}
			}
		}
	
	}
	
	private void flushCriteria()
	{
		critBuffer = new ScreeningCriteria(datchkSeason);
		testName = null;
	}

	private void startNamedTest(StringTokenizer st, File file, int lineNum)
	{
		flushCriteria();
		if (st.hasMoreTokens())
			testName = st.nextToken();
		else
			fileWarning(file, lineNum, "DEFINE without name -- ignored");
	}
	
	private void endNamedTest()
	{
		if (testName != null)
			storedTests.put(testName, critBuffer);
	}
	
	private void recallNamedTest(StringTokenizer st, File file, int lineNum)
	{
		flushCriteria();
		if (st.hasMoreTokens())
		{
			String name = st.nextToken();
			critBuffer = storedTests.get(name);
			if (critBuffer == null)
			{
				fileWarning(file, lineNum, "TEST with unknown name '"
					+ name + "' -- ignored.");
				flushCriteria();
			}
		}
	}
	
	private void specifyCriteria(StringTokenizer st, File file, int lineNum)
	{
		if (!st.hasMoreTokens())
		{
			fileWarning(file, lineNum, "CRITERIA with no type field -- ignored.");
			return;
		}
		String type = st.nextToken().toUpperCase();
		if (!st.hasMoreTokens())
		{
			fileWarning(file, lineNum, "CRITERIA " + type + " with no arguments.");
			return;
		}
		String tok = st.nextToken().toUpperCase();
		char qflag = tok.charAt(0);
		if (qflag != 'R' && qflag != 'Q' && qflag != 'M')
		{
			fileWarning(file, lineNum, "CRITERIA " + type
				+ " with unrecognized qflag '" + qflag + " -- ignored.");
			return;
		}
		if (type.startsWith("ABS"))
		{
			// absolute magnitude test args: qflag min max
			if (st.countTokens() < 2)
			{
				fileWarning(file, lineNum, "CRITERIA " + type
					+ " missing range variables -- ignored.");
				return;
			}
			double min = Double.NEGATIVE_INFINITY;
			double max = Double.POSITIVE_INFINITY;
			try 
			{
				min = Double.parseDouble(st.nextToken());
				max = Double.parseDouble(st.nextToken());
			}
			catch(Exception ex)
			{
				fileWarning(file, lineNum, "CRITERIA " + type
					+ " non-numeric range variables -- ignored.");
				return;
			}
			critBuffer.addAbsCheck(new AbsCheck(qflag, min, max));
		}
		else if (type.startsWith("DUR"))
		{
			// duration magnitude test args: qflag min max duration
			if (st.countTokens() < 3)
			{
				fileWarning(file, lineNum, "CRITERIA " + type
					+ " missing variables -- ignored.");
				return;
			}
			double min = Double.NEGATIVE_INFINITY;
			double max = Double.POSITIVE_INFINITY;
			try 
			{
				min = Double.parseDouble(st.nextToken());
				max = Double.parseDouble(st.nextToken());
			}
			catch(Exception ex)
			{
				fileWarning(file, lineNum, "CRITERIA " + type
					+ " non-numeric range variables -- ignored.");
				return;
			}
			tok = st.nextToken();
			String duration = dur2IntervalCode(tok);
			critBuffer.addDurCheckPeriod(
				new DurCheckPeriod(qflag, duration, min, max));
		}
		else if (type.startsWith("CONST"))
		{
			// constant value test args: qflag duration [min [tolerance [maxmissing]]]
			if (!st.hasMoreTokens())
			{
				fileWarning(file, lineNum, "CRITERIA " + type
					+ " missing duration -- ignored.");
				return;
			}
			tok = st.nextToken();
			String duration = dur2IntervalCode(tok);
			double min = 0.0;
			double tolerance = 0.0;
			int nmissing = 0;
			IntervalIncrement maxGap = null;
			if (st.hasMoreTokens())
			{
				try 
				{
					min = Double.parseDouble(st.nextToken());
					if (st.hasMoreTokens())
					{
						tolerance = Double.parseDouble(st.nextToken());
						if (st.hasMoreTokens())
						{
							String missingArg = st.nextToken();
							try { nmissing = Integer.parseInt(missingArg); }
							catch(NumberFormatException ex)
							{
								maxGap = IntervalCodes.getIntervalCalIncr(missingArg);
								if (maxGap == null)
									fileWarning(file, lineNum, "Invalid nmissing argument '" 
										+ missingArg + "' -- ignored.");
							}
						}
					}
				}
				catch(Exception ex)
				{
					fileWarning(file, lineNum, "CRITERIA " + type
						+ " non-numeric min and/or tolerance.");
					return;
				}
			}
			ConstCheck cc = new ConstCheck(qflag, duration, min, tolerance, nmissing);
			cc.setMaxGap(maxGap);
			critBuffer.addConstCheck(cc);
			
		}
		else if (type.startsWith("RATE"))
		{
			// rate of change test args: qflag neg-rate positive-rate
			if (st.countTokens() < 2)
			{
				fileWarning(file, lineNum, "CRITERIA " + type
					+ " missing variables -- ignored.");
				return;
			}
			double neg = 0.0;
			double pos = 0.0;
			try 
			{
				neg = Double.parseDouble(st.nextToken());
				pos = Double.parseDouble(st.nextToken());
			}
			catch(Exception ex)
			{
				fileWarning(file, lineNum, "CRITERIA " + type
					+ " non-numeric range variables -- ignored.");
				return;
			}
			critBuffer.addRocPerHourCheck(
				new RocPerHourCheck(qflag, neg, pos));
		}
//		else if (type.startsWith("REL"))
//		{
//			// relative magnitude test args: qflag min-expr, max-expr
//			//    [action duration]
//		}
//		else if (type.startsWith("DIS"))
//		{
//			// distribution test args: qflag duration significance-lev
//			//    base1 base2
//		}
		else
		{
			fileWarning(file, lineNum, "CRITERIA " + type
				+ " not implemented.");
		}
	}
	
	private void specifyData(String line, File file, int lineNum)
	{
		// Isolate the DSS path on the line
		int colon = line.indexOf(':');
		int dssPathStart = colon >= 0 ? colon + 1 : line.indexOf('/');
		if (dssPathStart == -1 || colon >= line.length())
		{
			fileWarning(file, lineNum, "DATA line with no DSS path -- ignored.");
			return;
		}
		int dssPathEnd = line.indexOf(';', dssPathStart);
		String dssPath = 
			dssPathEnd != -1 ? line.substring(dssPathStart, dssPathEnd) :
				line.substring(dssPathStart);

		// Lookup the mapping to the CWMS path
		PathMapping pm = dssPath2pathMap.get(dssPath);
		if (pm == null)
		{
			fileWarning(file, lineNum, "No mapping for DSS path '"
				+ dssPath + "' -- ignored.");
			return;
		}
		
		// Get the screening for this TSID, or create one if none yet exists.
		String cwmsTSID = pm.getCwmsPath();
		String tsid_uc = cwmsTSID.toUpperCase();
		Screening screening = tsidScreeningMap.get(tsid_uc);
		if (screening == null)
		{
			screening = new Screening(Constants.undefinedId,
				pm.getCwmsPath(),
				"Read from " + file.getPath(),
				pm.getDssUnitsAbbr());
			tsidScreeningMap.put(tsid_uc, screening);
			screenedTsids.add(cwmsTSID);
			Logger.instance().debug2(module + " Created new screening for '" +
				tsid_uc + "'");
		}
		
		// Add the accumulated criteria checks to this screening.
		screening.add(critBuffer);
		Logger.instance().debug3(module + " Added new criteria for '" +
			tsid_uc + "' read from file '" + file.getPath() + "'");
	}

	// Convert a datchk duration into a CWMS interval/duration code
	private String dur2IntervalCode(String tok)
	{
		int idx = 0;
		while(idx < tok.length() && Character.isDigit(tok.charAt(idx)))
			idx++;
		if (idx == 0)
			return IntervalCodes.int_cwms_zero;
		int n = Integer.parseInt(tok.substring(0, idx));
		char u = idx == tok.length() ? 'H' : 
			Character.toUpperCase(tok.charAt(idx));
		if (u == 'M')
			switch(n)
			{
			case 1: return IntervalCodes.int_one_minute;
			case 2: return IntervalCodes.int_two_minutes;
			case 3: return IntervalCodes.int_three_minutes;
			case 4: return IntervalCodes.int_four_minutes;
			case 5: return IntervalCodes.int_five_minutes;
			case 6: return IntervalCodes.int_six_minutes;
			case 10: return IntervalCodes.int_ten_minutes;
			case 12: return IntervalCodes.int_twelve_minutes;
			case 15: return IntervalCodes.int_fifteen_minutes;
			case 20: return IntervalCodes.int_twenty_minutes;
			case 30: return IntervalCodes.int_thirty_minutes;
			}
		else if (u == 'H')
			switch(n)
			{
			case 1: return IntervalCodes.int_one_hour;
			case 2: return IntervalCodes.int_two_hours;
			case 3: return IntervalCodes.int_three_hours;
			case 4: return IntervalCodes.int_four_hours;
			case 6: return IntervalCodes.int_six_hours;
			case 8: return IntervalCodes.int_eight_hours;
			case 12: return IntervalCodes.int_twelve_hours;
			}
		else if (u == 'D')
			switch(n)
			{
			case 1: return IntervalCodes.int_one_day;
			case 2: return IntervalCodes.int_two_days;
			case 3: return IntervalCodes.int_three_days;
			case 4: return IntervalCodes.int_four_days;
			case 5: return IntervalCodes.int_five_days;
			case 6: return IntervalCodes.int_six_days;
			}
		return IntervalCodes.int_one_hour;
	}
	
	/**
	 * Reads all the input files specified in the config and prints a 
	 * report containing all screenings.
	 * @param args <configfile> [<seasonTimeZone>]
	 */
	public static void main(String args[])
		throws Exception
	{
		DatchkReader datchkReader = DatchkReader.instance();
		datchkReader.cfgFile = new LoadedFile(args[0]);
		
		datchkReader.checkConfig();
		
		System.out.println("Files Loaded:");
		for(LoadedFile lf : datchkReader.loadedFiles)
			System.out.println("\t" + lf.getPath());
		
		SimpleDateFormat sdf = new SimpleDateFormat("MMM dd HH:mm");
		sdf.setTimeZone(datchkReader.seasonTz);
		System.out.println("Seasons Defined:");
		for(String seasonName : datchkReader.seasons.keySet())
		{
			GregorianCalendar cal = datchkReader.seasons.get(seasonName);
			Date d = cal.getTime();
			System.out.println("\t" + seasonName + " = " + sdf.format(d));
		}
		
		System.out.println("DSS - CWMS Path Mappings:");
		for(PathMapping pmap : datchkReader.dssPath2pathMap.values())
			System.out.println(pmap.getDssPath() + " = " + pmap.getCwmsPath()
				+ " DSS Units=" + pmap.getDssUnitsAbbr());
		
		System.out.println("Screenings by Time Series Identifier:");
		for(String tsid : datchkReader.tsidScreeningMap.keySet())
		{
			Screening scr = datchkReader.tsidScreeningMap.get(tsid);
			System.out.println("\t" + tsid);
			printScreening(scr);
		}
		if (args.length > 1)
		{
			System.out.println("Screening searches from command line:");
		}
		for(int idx = 1; idx < args.length; idx++)
		{
			System.out.println("Searching for '" + args[idx] + "'");
			Screening scr = datchkReader.tsidScreeningMap.get(args[idx]);
			if (scr == null)
				System.out.println("\tNo screening found.");
		}
	}
	
	public static void printScreening(Screening scr)
	{
		System.out.println("Screening Name: " + scr.getScreeningName());
		System.out.println("Units: " + scr.getCheckUnitsAbbr());
		SimpleDateFormat sdf = new SimpleDateFormat("MMM dd HH:mm");
		DatchkReader datchkReader = DatchkReader.instance();
		sdf.setTimeZone(datchkReader.seasonTz);

		for(ScreeningCriteria crit : scr.criteriaSeasons)
		{
			Calendar cal = crit.seasonStart;
			if (cal != null)
			{
				Date d = cal.getTime();
				System.out.println("\t\tSeason start: " + sdf.format(d));
			}

			for(AbsCheck chk : crit.absChecks)
				System.out.println("\t\t" + chk);
			for(ConstCheck chk : crit.constChecks)
				System.out.println("\t\t" + chk);
			for(RocPerHourCheck chk : crit.rocPerHourChecks)
				System.out.println("\t\t" + chk);
			for(DurCheckPeriod chk : crit.durCheckPeriods)
				System.out.println("\t\t" + chk);
		}
	}

	public HashSet<String> getScreenedTsids()
	{
		return screenedTsids;
	}
	
}
