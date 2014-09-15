package decodes.dcpmon;

import java.util.ArrayList;
import java.util.Date;

import lrgs.common.DcpMsg;
import opendcs.dai.XmitRecordDAI;
import opendcs.dao.XmitRecordDAO;
import ilex.cmdline.StringToken;
import ilex.cmdline.TokenOptions;
import decodes.db.Database;
import decodes.db.NetworkList;
import decodes.util.CmdLineArgs;

/**
 * Read transmissions by date and network list.
 * Usage: decj decodes.tsdb.ReadByNetlistTest [-y YYYYMMDD] netlist-name
 */
public class ReadByNetlistTest extends ReadAllXmitsTest
{
	protected StringToken netlistArg = new StringToken("", "netlist-name", "",
		TokenOptions.optArgument |TokenOptions.optRequired, "");

	public ReadByNetlistTest()
	{
		super();
	}

	@Override
	protected void addCustomArgs(CmdLineArgs cmdLineArgs)
	{
		super.addCustomArgs(cmdLineArgs);
		cmdLineArgs.addToken(netlistArg);
	}
	
	

	@Override
	protected void runApp() throws Exception
	{
		Date d = getDay();
		XmitRecordDAI xmitRecordDAO = theDb.makeXmitRecordDao(31);
		int dayNum = XmitRecordDAO.msecToDay(d.getTime());
		System.out.println("dayNum=" + dayNum);
		NetworkList netlist = Database.getDb().networkListList.getNetworkList(netlistArg.getValue());
		if (netlist == null)
		{
			System.err.println("No such netlist '" + netlistArg.getValue() + "'");
			System.exit(1);
		}
		ArrayList<DcpMsg> msgs = new ArrayList<DcpMsg>();
		xmitRecordDAO.readXmitsByGroup(msgs, dayNum, netlist);
		System.out.println(msgs.size() + " messages read.");
		for(DcpMsg msg : msgs)
			printMsg(msg);
		System.exit(0);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args)
		throws Exception
	{
		ReadByNetlistTest prog = new ReadByNetlistTest();
		prog.execute(args);
	}

}
