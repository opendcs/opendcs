package decodes.tsdb;

import java.util.Iterator;

import decodes.db.Database;
import decodes.db.Site;

public class DeleteAllSites extends TsdbAppTemplate
{
	public DeleteAllSites(String logname)
	{
		super(logname);
	}

	@Override
	protected void runApp() throws Exception
	{
		Database db = Database.getDb();
		db.siteList.read();
		for(Iterator<Site> sit = db.siteList.iterator(); sit.hasNext(); )
		{
			Site s = sit.next();
			Database.getDb().getDbIo().deleteSite(s);
		}
	}
	
	public static void main( String[] args )
	throws Exception
	{
		DeleteAllSites app = new DeleteAllSites("util.log");
		app.execute(args);
	}
}


