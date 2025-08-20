package decodes.platstat;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.TimeZone;

import lrgs.gui.DecodesInterface;
import opendcs.dai.ScheduleEntryDAI;
import ilex.cmdline.IntegerToken;
import ilex.cmdline.StringToken;
import ilex.cmdline.TokenOptions;
import ilex.cmdline.BooleanToken;
import ilex.util.TextUtil;
import decodes.db.Database;
import decodes.db.RoutingSpec;
import decodes.db.ScheduleEntry;
import decodes.db.ScheduleEntryStatus;
import decodes.tsdb.TsdbAppTemplate;
import decodes.util.CmdLineArgs;

/**
 * -z timezone
 * -h <hours> default=12 only show run statuses within this many hours.
 * [names of se's] if none provided list status all schedule entries
 */
public class ShowScheduleStatus 
	extends TsdbAppTemplate
{
	private static final String module = "ShowScheduleStatus";
	
	private StringToken tzArg = new StringToken("z", "Time Zone", "", 
		TokenOptions.optSwitch, "");
	private IntegerToken hoursArg = new IntegerToken("h", "Number of Hours", "",
		TokenOptions.optSwitch, 12);
	private StringToken seNameArg = new StringToken("", "Schedule Entry Name(s)", "",
		TokenOptions.optArgument|TokenOptions.optMultiple, "");
	private SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy-HH:mm:ss");
	private BooleanToken omitManualArg = new BooleanToken("M", "Omit Manual entries (default=false)",
		"", TokenOptions.optSwitch, false);

	public ShowScheduleStatus()
	{
		super(module);
	}

	@Override
	protected void addCustomArgs(CmdLineArgs cmdLineArgs)
	{
		cmdLineArgs.addToken(tzArg);
		cmdLineArgs.addToken(hoursArg);
		cmdLineArgs.addToken(omitManualArg);
		cmdLineArgs.addToken(seNameArg);
	}

	public static void main(String args[])
		throws Exception
	{
		TsdbAppTemplate tp = new ShowScheduleStatus();
		DecodesInterface.silent = true;
		tp.execute(args);
	}

	@Override
	protected void runApp() 
		throws Exception
	{
		if (tzArg.getValue().length() > 0)
			sdf.setTimeZone(TimeZone.getTimeZone(tzArg.getValue()));

		ScheduleEntryDAI scheduleEntryDAO = Database.getDb().getDbIo().makeScheduleEntryDAO();
		ArrayList<ScheduleEntry> schedEntries = null;
		if (seNameArg.NumberOfValues() > 0 && seNameArg.getValue(0).length() > 0)
		{
			schedEntries = new ArrayList<ScheduleEntry>();
			for(int idx = 0; idx < seNameArg.NumberOfValues(); idx++)
				schedEntries.add(scheduleEntryDAO.readScheduleEntry(seNameArg.getValue(idx)));
		}
		else
			schedEntries = scheduleEntryDAO.listScheduleEntries(null);
		
		Collections.sort(schedEntries,
			new Comparator<ScheduleEntry>()
			{
				@Override
				public int compare(ScheduleEntry o1, ScheduleEntry o2)
				{
					return o1.getName().compareTo(o2.getName());
				}
			});
		
		Date cutoff = new Date(System.currentTimeMillis() - hoursArg.getValue() * 3600000L);
		for(ScheduleEntry se : schedEntries)
		{
			if (se == null) { System.out.println("se is null, # entries=" + schedEntries.size()); continue;}

			if (omitManualArg.getValue() && se.getName().toLowerCase().endsWith("-manual"))
				continue;
			
			RoutingSpec rs = Database.getDb().routingSpecList.getById(se.getRoutingSpecId());
			if (rs == null)
				rs = Database.getDb().routingSpecList.find(se.getRoutingSpecName());
			System.out.println(se.getName() + " routing spec=" + 
				(rs == null ? "null" : rs.getName()));
			
			ArrayList<ScheduleEntryStatus> seStati = scheduleEntryDAO.readScheduleStatus(se);
			if (seStati == null || seStati.size() == 0)
			{
				System.out.println("\tNo status records");
				continue;
			}
			if (hoursArg.getValue() == 0)
				displayStatus(seStati.get(seStati.size()-1));
			else
				for(ScheduleEntryStatus ses : seStati)
					if (ses.getLastModified().after(cutoff))
						displayStatus(ses);
		}
		scheduleEntryDAO.close();

	}
	
	@Override
	public void tryConnect()
	{
		// This app doesn't need the TSDB and must operate with just DECODES even
		// under XML.
	}
	
	@Override
	public void closeDb()
	{
	}

	@Override
	public synchronized void createDatabase() {}

	private void displayStatus(ScheduleEntryStatus ses)
	{
		System.out.println("\tstart=" + sdf.format(ses.getRunStart())
			+ ", stop=" 
			+ (ses.getRunStop() == null ? "                   " : sdf.format(ses.getRunStop()))
			+ ", status=" + TextUtil.setLengthLeftJustify(""+ses.getRunStatus(), 14)
			+ ", #msgs=" + ses.getNumMessages()
			+ ", #errs=" + ses.getNumDecodesErrors()
			+ ", #plat=" + ses.getNumPlatforms()
			+ ", lastModified=" + sdf.format(ses.getLastModified()));
	}


}
