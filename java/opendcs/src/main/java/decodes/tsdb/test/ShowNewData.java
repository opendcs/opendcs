/*
*  $Id: ShowNewData.java,v 1.3 2020/02/14 15:16:47 mmaloney Exp $
*/
package decodes.tsdb.test;

import java.io.PrintStream;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import ilex.var.TimedVariable;
import opendcs.dai.TimeSeriesDAI;
import decodes.db.DataType;
import decodes.db.SiteName;
import decodes.sql.DbKey;
import decodes.tsdb.*;

/**
Stays in a loop showing new data on the screen as it arrives.
Reads data from the tasklist table.
*/
public class ShowNewData extends TestProg
{
	private PrintStream out = System.out;
	
	public ShowNewData()
	{
		super(null);
	}

	public static void main(String args[])
		throws Exception
	{
		TestProg tp = new ShowNewData();
		tp.execute(args);
	}

	protected void runTest()
		throws Exception
	{
		out.println("Getting new data for app ID=" + appId);
//		while(true)
//		{
		TimeSeriesDAI tsDao = theDb.makeTimeSeriesDAO();
		
			DataCollection dc = tsDao.getNewData(appId);
			List<CTimeSeries> tsl = dc.getAllTimeSeries();
			for(CTimeSeries ts : tsl)
			{
				out.println("");
				out.println("Time Series  SDI="
					+ ts.getSDI() 
					+ " tabsel=" + ts.getTableSelector()
					+ " interval=" + ts.getInterval()
					+ " modelId=" + ts.getModelId()
					+ " modelRunId=" + ts.getModelRunId()
					+ " compId=" + ts.getComputationId());
				TimeSeriesIdentifier tsid = ts.getTimeSeriesIdentifier();
				if (tsid.getSite() != null)
					for(Iterator<SiteName> snit = tsid.getSite().getNames(); snit.hasNext(); )
					{
						SiteName sn = snit.next();
						out.println("Site Name: " + sn);
					}
				DataType dt = tsid.getDataType();
				out.println("Data Type: " + dt);
				out.println("Number of values: " + ts.size());
				for(int i=0; i<ts.size(); i++)
				{
					TimedVariable tv = ts.sampleAt(i);
					out.println(
						(VarFlags.wasAdded(tv) ? "Add: " :
						 VarFlags.wasDeleted(tv) ? "Del: " : "???: ")
						+ tv.toString() + " " + tv.getFlags());
				}
			}
			theDb.releaseNewData(dc, tsDao);
			try 
			{
				Thread.sleep(1000L); 
				out.print(".");
				out.flush();
			}
			catch(InterruptedException ex) {}
//		}
		tsDao.close();
	}

	private static CTimeSeries makeTimeSeries(String x)
		throws Exception
	{
		StringTokenizer st = new StringTokenizer(x, ":");
		DbKey sdi = DbKey.createDbKey(Long.parseLong(st.nextToken()));
		String intv = st.nextToken();
		String tabsel = st.nextToken();
		CTimeSeries ret = new CTimeSeries(sdi, intv, tabsel);
		if (st.hasMoreTokens())
		{
			int modid = Integer.parseInt(st.nextToken());
			ret.setModelRunId(modid);
		}
		return ret;
	}

	public void setOut(PrintStream out)
	{
		this.out = out;
	}
}
