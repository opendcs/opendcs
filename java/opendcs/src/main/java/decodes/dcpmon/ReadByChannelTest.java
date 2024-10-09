package decodes.dcpmon;

import java.util.ArrayList;
import java.util.Date;

import lrgs.common.DcpMsg;
import opendcs.dai.XmitRecordDAI;
import opendcs.dao.XmitRecordDAO;
import decodes.util.CmdLineArgs;
import ilex.cmdline.IntegerToken;
import ilex.cmdline.TokenOptions;

/**
 * Usage: decj decodes.tsdb.ReadByChannelTest [-y YYYYMMDD] channel-num
 */
public class ReadByChannelTest
	extends ReadAllXmitsTest
{
	protected IntegerToken chanArg = new IntegerToken("", "channelNum", "",
		TokenOptions.optArgument |TokenOptions.optRequired, -1);

	public ReadByChannelTest()
	{
		super();
	}
	
	@Override
	protected void addCustomArgs(CmdLineArgs cmdLineArgs)
	{
		super.addCustomArgs(cmdLineArgs);
		cmdLineArgs.addToken(chanArg);
	}


	@Override
	protected void runApp() throws Exception
	{
		Date d = getDay();
		XmitRecordDAI xmitRecordDAO = theDb.makeXmitRecordDao(31);
		int dayNum = XmitRecordDAO.msecToDay(d.getTime());
		System.out.println("dayNum=" + dayNum);
		System.out.println("channel=" + chanArg.getValue());
		ArrayList<DcpMsg> msgs = new ArrayList<DcpMsg>();
		xmitRecordDAO.readXmitsByChannel(msgs, dayNum, chanArg.getValue());
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
		ReadByChannelTest prog = new ReadByChannelTest();
		prog.execute(args);
	}

}
