package decodes.platstat;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.TimeZone;

import lrgs.gui.DecodesInterface;

import opendcs.dai.PlatformStatusDAI;
import ilex.cmdline.BooleanToken;
import ilex.cmdline.StringToken;
import ilex.cmdline.TokenOptions;
import ilex.util.TextUtil;
import decodes.db.Database;
import decodes.db.NetworkList;
import decodes.db.Platform;
import decodes.db.PlatformStatus;
import decodes.tsdb.TsdbAppTemplate;
import decodes.util.CmdLineArgs;

public class ShowPlatformStatus 
	extends TsdbAppTemplate
{
	private static final String module = "ShowPlatformStatus";
	private StringToken netlistArg = new StringToken("n", "Network List Name", "", 
		TokenOptions.optSwitch, "");
	private StringToken errorArg = new StringToken("e", "Error #hours or 'current'", "", 
		TokenOptions.optSwitch, "");
	private StringToken tzArg = new StringToken("z", "Time Zone", "", 
		TokenOptions.optSwitch, "");
	private StringToken sortArg = new StringToken("s", "Sort option (n=name, c=last contact, "
		+ "m=last message, e=last error", "", TokenOptions.optSwitch, "n");
	private BooleanToken reverseSortArg = new BooleanToken("r", "Reverse sort order from ascending to "
		+ "descending.", "", TokenOptions.optSwitch, false);


	public ShowPlatformStatus()
	{
		super(module);
	}

	@Override
	protected void runApp() 
		throws Exception
	{
		SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy-HH:mm:ss");
		if (tzArg.getValue().length() > 0)
			sdf.setTimeZone(TimeZone.getTimeZone(tzArg.getValue()));
		
		NetworkList netlist = null;
		if (netlistArg.getValue().length() > 0)
			netlist = Database.getDb().networkListList.find(netlistArg.getValue());
		
		PlatformStatusDAI platformStatusDAO = Database.getDb().getDbIo().makePlatformStatusDAO();
		
		boolean currentErrorsOnly = false;
		int errorHours = -1;
		if (errorArg.getValue().length() > 0)
		{
			if (errorArg.getValue().equalsIgnoreCase("current"))
				currentErrorsOnly = true;
			else
			{
				try { errorHours = Integer.parseInt(errorArg.getValue()); }
				catch(NumberFormatException ex)
				{
					System.err.println("Invalid -e argument. Should be # of hours or 'current'");
					errorHours = -1;
					System.exit(1);
				}
			}
		}
		
		ArrayList<PlatformStatus> pslist = platformStatusDAO.listPlatformStatus();
		Collections.sort(pslist, new PlatStatComparator(sortArg.getValue().charAt(0),
			reverseSortArg.getValue()));
		System.out.println("   Platform Name         Last Contact         Last Message      FailCode      Last Error       Annotation");
		System.out.println("====================  ===================  ===================  ========  ===================  ==========");
		for(PlatformStatus ps : pslist)
		{
			StringBuilder line = new StringBuilder();
			Platform platform = Database.getDb().platformList.getById(ps.getPlatformId());
			if (netlist != null && !netlist.contains(platform))
				continue;
			
			if (ps.getLastContactTime() == null)
				continue;
			
			// Current errors only. Skip stations with no errors and stations where the
			// last error was before the last contact.
			if (currentErrorsOnly &&
				(ps.getLastErrorTime() == null || ps.getLastErrorTime().before(ps.getLastContactTime())))
				continue;
			else if (errorHours > 0)
			{
				// Only want stations with errors in last N hours
				if (ps.getLastErrorTime() == null)
					continue;
				long d = System.currentTimeMillis() - ps.getLastErrorTime().getTime();
				if (d / 3600000L >= errorHours)
					continue;
			}
			
			String pname = platform == null ? "unknown" : platform.getDisplayName();
			line.append(TextUtil.setLengthLeftJustify(pname, 20));
			line.append(", ");
			line.append(ps.getLastContactTime() == null ? "never              "
				: sdf.format(ps.getLastContactTime()));
			line.append(", ");
			line.append(ps.getLastMessageTime() == null ? "never              "
				: sdf.format(ps.getLastMessageTime()));
			line.append(", ");
			line.append(ps.getLastFailureCodes() == null ? "        " :
				TextUtil.setLengthLeftJustify(ps.getLastFailureCodes(), 8));
			line.append(", ");
			line.append(ps.getLastErrorTime() == null ? "never              "
				: sdf.format(ps.getLastErrorTime()));
			line.append(", ");
			line.append(ps.getAnnotation() == null ? "" : ps.getAnnotation());
			System.out.println(line);
		}
		platformStatusDAO.close();
	}

	@Override
	protected void addCustomArgs(CmdLineArgs cmdLineArgs)
	{
		cmdLineArgs.addToken(netlistArg);
		cmdLineArgs.addToken(errorArg);
		cmdLineArgs.addToken(tzArg);
		cmdLineArgs.addToken(sortArg);
		cmdLineArgs.addToken(reverseSortArg);
	}

	public static void main(String args[])
		throws Exception
	{
		TsdbAppTemplate tp = new ShowPlatformStatus();
		DecodesInterface.silent = true;
		tp.execute(args);
	}
	
	@Override
	public boolean tryConnect()
	{
		// This app doesn't need the TSDB and must operate with just DECODES even
		// under XML.
		return true;
	}
	
	@Override
	public void closeDb()
	{
	}

	@Override
	public synchronized void createDatabase() {}
}

class PlatStatComparator implements Comparator<PlatformStatus> 
{
	char sortArg = 'n';
	boolean reverse = false;
	PlatStatComparator(char sortArg, boolean reverse)
	{
		if ("scne".indexOf(sortArg) < 0)
			sortArg = 'm';
		
		this.sortArg = sortArg;
		this.reverse = reverse;
		
	}
	
	@Override
	public int compare(PlatformStatus ps1, PlatformStatus ps2)
	{
		int r = 0;
		Platform p1 = Database.getDb().platformList.getById(ps1.getPlatformId());
		Platform p2 = Database.getDb().platformList.getById(ps2.getPlatformId());
		switch(sortArg)
		{
		case 'n':
			r = p1.getDisplayName().compareTo(p2.getDisplayName());
			return r;
			// else fall through and sort by contact time.
		case 'c':
		  {
			Date d1 = ps1.getLastContactTime();
			Date d2 = ps2.getLastContactTime();
			if (d1 != null)
				r = d2 != null ? d1.compareTo(d2) : -1;
			else
				r = d2 != null ? 1 : 0;
			if (r == 0)
				r = p1.getDisplayName().compareTo(p2.getDisplayName());
			break;
		  }
		case 'm':
		  {
			Date d1 = ps1.getLastMessageTime();
			Date d2 = ps2.getLastMessageTime();
			if (d1 != null)
				r = d2 != null ? d1.compareTo(d2) : -1;
			else
				r = d2 != null ? 1 : 0;
			if (r == 0)
				r = p1.getDisplayName().compareTo(p2.getDisplayName());
			break;
		  }
		case 'e':
		  {
			Date d1 = ps1.getLastErrorTime();
			Date d2 = ps2.getLastErrorTime();
			if (d1 != null)
				r = d2 != null ? d1.compareTo(d2) : -1;
			else
				r = d2 != null ? 1 : 0;
			if (r == 0)
				r = p1.getDisplayName().compareTo(p2.getDisplayName());
			break;
		  }
		}
		return reverse ? -r : r;
	}
	
}