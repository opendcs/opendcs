package decodes.aesrd;

import ilex.cmdline.StringToken;
import ilex.cmdline.TokenOptions;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import lrgs.gui.DecodesInterface;
import decodes.db.Database;
import decodes.db.DatabaseException;
import decodes.db.Platform;
import decodes.db.TransportMedium;
import decodes.polling.Parity;
import decodes.tsdb.TsdbAppTemplate;
import decodes.util.CmdLineArgs;
import decodes.util.DecodesException;

public class ImportModemTMs extends TsdbAppTemplate
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	public static final String module = "ImportModemTMs";

	private StringToken topDirArg = new StringToken("",
        "top dir of legacy dacq, corresponding to /usr1 on collect", "",
        TokenOptions.optArgument | TokenOptions.optRequired, null);

	private File topdir = null;

	public ImportModemTMs()
	{
		super(module);
	}



	@Override
	protected void runApp() throws Exception
	{
		// From topdirArg make sure topdir exists & set variable
		topdir = new File(topDirArg.getValue());
		if (!topdir.isDirectory())
			throw new Exception("required arg is top dir containing lstfiles and confiles subdirs.");

		File phonelistfile = new File(topdir.getPath() + "/lstfiles/Phone.txt");
		if (!phonelistfile.canRead())
			throw new Exception("Cannot read '" + phonelistfile.getPath() + "'");
		LineNumberReader lnr = new LineNumberReader(new FileReader(phonelistfile));
		String line;
		while((line = lnr.readLine()) != null)
		{
			line = line.trim();
			if (line.length() == 0)
				continue;
			int period = line.indexOf('.');
			if (period == -1)
				continue;
			String station = line.substring(0,period);
			processStation(station);
		}
		lnr.close();
	}

	private void processStation(String stationName)
	{
		File inFile = new File(topdir.getPath() + "/lstfiles/" + stationName + ".in");
		if (!inFile.canRead())
		{
			log.warn("Can't read '{}' -- skipping station {}", inFile.getPath(), stationName);
			return;
		}

		// open lstfiles/$STATION.in, read the one line in the file
		BufferedReader br = null;
		String username = null;
		String loggerType = null;
		try
		{
			br = new BufferedReader(new FileReader(inFile));
			String line1 = br.readLine();
			if (line1 == null)
			{
				log.warn("File '{}' is empty. Skipping {}", inFile.getPath(), stationName);
				return;
			}
			// First field is username (only used for campbells)
			// send field is 2 char code indicating loggertype
			String fields[] = line1.split("\\s+");
			if (fields.length < 2)
			{
				log.warn("File '{}' not enough fields. Skipping {}", inFile.getPath(), stationName);
				return;
			}
			username = fields[0];
			loggerType = code2loggerType(fields[1]);
			if (loggerType == null)
			{
				log.warn("File '{}' unrecognized logger type '{}'. Skipping {}",
						 inFile.getPath(), fields[1], stationName);
				return;
			}
		}
		catch (IOException ex)
		{
			log.atError().setCause(ex).log("Error reading '{}' Skipping ", inFile.getPath(), stationName);
			return;
		}
		finally { if (br != null) try { br.close(); } catch(Exception ex) {} }

		Platform platform = Database.getDb().platformList.getBySiteNameValue(stationName);
		if (platform == null)
		{
			log.warn("No platform for station name '{}' -- Skipped.", stationName);
			return;
		}

		TransportMedium pmtm = platform.getTransportMedium("polled-modem");
		boolean wasNew = false;
		if (pmtm == null)
		{
			pmtm = new TransportMedium(platform, "polled-modem", "");
			wasNew = true;
		}

		// open confiles/$STATION.con
		// parse the modem params.
		File conFile = new File(topdir.getPath() + "/confiles/" + stationName + ".con");
		// open lstfiles/$STATION.in, read the one line in the file
		br = null;
		try
		{
			br = new BufferedReader(new FileReader(conFile));
			String line = null;
			while((line = br.readLine()) != null)
			{
				line = line.trim();
				String words[] = line.split("\\s+");
				if (words.length < 2)
					continue;
				log.info("{} : '{}' '{}'", conFile.getName(), words[0], words[1]);
				if (words[0].equalsIgnoreCase("call"))
					pmtm.setMediumId(words[1]);
				else if (words[0].equalsIgnoreCase("baud"))
				{
					try { pmtm.setBaud(Integer.parseInt(words[1])); }
					catch(NumberFormatException ex)
					{
						log.atWarn().setCause(ex).log("Bad baud in {} -- default to 1200", conFile.getPath());
						pmtm.setBaud(1200);
					}
				}
				else if (words[0].equalsIgnoreCase("parity"))
					pmtm.setParity(Parity.fromString(words[1]).getCode());
				else if (words[0].equalsIgnoreCase("wordlen"))
				{
					try { pmtm.setDataBits(Integer.parseInt(words[1])); }
					catch(NumberFormatException ex)
					{
						pmtm.setDataBits(pmtm.getParity() == Parity.None.getCode()
							|| pmtm.getParity() == Parity.Unknown.getCode() ? 8 : 7);
						log.atWarn()
						   .setCause(ex)
						   .log("Bad wordlen in {} -- default to {} with parity {}",
							    conFile.getPath(), pmtm.getDataBits(), pmtm.getParity());
					}
				}
				else if (words[0].equalsIgnoreCase("stopbits"))
				{
					try { pmtm.setStopBits(Integer.parseInt(words[1])); }
					catch(NumberFormatException ex)
					{
						pmtm.setStopBits(1);
						log.atWarn()
						   .setCause(ex)
						   .log("Bad stopbits in {} -- default to 1", conFile.getPath());
					}
				}
			}
			if (pmtm.getMediumId() == null || pmtm.getMediumId().length() == 0)
			{
				log.error("No 'call' statement found. Required for TMID. Skipping {}", stationName);
				return;
			}
			log.info("New TM: type={}, id={}", pmtm.getMediumType(), pmtm.getMediumId());
			pmtm.setLoggerType(loggerType);
			if (loggerType.equalsIgnoreCase("campbell"))
				pmtm.setUsername(username);
			if (wasNew)
				platform.transportMedia.add(pmtm);

			platform.write();
		}
		catch (IOException ex)
		{
			log.atError().setCause(ex).log("Error reading '{}' Skipping {}" , conFile.getPath(), stationName);
			return;
		}
		catch (DatabaseException ex)
		{
			log.atError().setCause(ex).log("Error saving platform to database. Station={}", stationName);
			return;
		}
		finally { if (br != null) try { br.close(); } catch(Exception ex) {} }
	}

	private String code2loggerType(String code)
	{
		code = code.toUpperCase();
		if (code.equals("CB")) return "campbell";
		else if (code.equals("H5")) return "h555";
		else if (code.equals("AM")) return "amasser";
		else if (code.equals("V2")) return "vedasii";
		else if (code.equals("ST")) return "sutron";
		else if (code.equals("TS")) return "fts";
		else return null;
	}

	public static void main(String[] args)
		throws Exception
	{
		ImportModemTMs theApp = new ImportModemTMs();
		theApp.execute(args);
	}

	@Override
	protected void addCustomArgs(CmdLineArgs cmdLineArgs)
	{
		cmdLineArgs.addToken(topDirArg);
	}



	@Override
	public void initDecodes() throws DecodesException
	{
		super.initDecodes();
		DecodesInterface.initializeForDecoding();
	}


}
