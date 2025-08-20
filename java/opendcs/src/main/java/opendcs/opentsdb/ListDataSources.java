package opendcs.opentsdb;

import java.util.ArrayList;

import decodes.tsdb.TsdbAppTemplate;

public class ListDataSources extends TsdbAppTemplate
{

	public ListDataSources()
	{
		super("util.log");
	}

	@Override
	protected void runApp() throws Exception
	{
		if (!theDb.isOpenTSDB())
		{
			System.err.println("This utility only runs on OpenTSDB.");
			System.exit(1);
		}
		
		OpenTimeSeriesDAO tsDAO = (OpenTimeSeriesDAO)theDb.makeTimeSeriesDAO();
		try
		{
			System.out.println("SourceId, AppId, AppName, ModuleName");
			ArrayList<TsDataSource> list = tsDAO.listDataSources();
			for(TsDataSource tds : list)
				System.out.println("" + tds.getSourceId() + ", " + tds.getAppId()
					+ ", " + tds.getAppName() + ", " + tds.getAppModule());
		}
		finally
		{
			tsDAO.close();
		}

	}

	public static void main(String[] args)
		throws Exception
	{
		ListDataSources app = new ListDataSources();
		app.execute(args);
	}

}
