package decodes.tsdb.test;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.TimeZone;

import decodes.db.Constants;
import decodes.db.Database;
import decodes.db.DbEnum;
import decodes.db.EnumValue;
import decodes.decoder.Season;
import decodes.tsdb.TsdbAppTemplate;
import ilex.util.Logger;

public class SeasonTest extends TsdbAppTemplate
{

	public SeasonTest()
	{
		super("util.log");
		
		// TODO Auto-generated constructor stub
	}

	@Override
	protected void runApp() throws Exception
	{
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd-HH:mm");
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
	
		DbEnum seasonEnum = Database.getDb().enumList.getEnum(Constants.enum_Season);
		if (seasonEnum == null)
		{
			System.err.println("No season enum.");
			System.exit(1);
		}
		ArrayList<Season> seasons = new ArrayList<Season>();
		for(EnumValue ev : seasonEnum.values())
			seasons.add(new Season(ev));
		
		System.out.println("Enter date/times in the format yyyy/MM/dd-HH:mm");
		String line;
		while((line = System.console().readLine()) != null)
		{
			try
			{
				Date d = sdf.parse(line);
				for(Season s : seasons)
					System.out.println("\t" + d
						+ (s.isInSeason(d) ? " IS" : "IS NOT")
						+ " in season " + s.getAbbr() + "-" + s.getName());
				System.out.println();
			}
			catch(Exception ex)
			{
				System.err.println(ex.toString());
			}
		}
	}

	public static void main(String[] args)
		throws Exception
	{
		// TODO Auto-generated method stub
		SeasonTest seasonTest = new SeasonTest();
		seasonTest.execute(args);
	}

}
