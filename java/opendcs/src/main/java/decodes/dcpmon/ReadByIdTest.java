package decodes.dcpmon;

import java.util.ArrayList;
import java.util.Date;

import lrgs.common.DcpMsg;
import opendcs.dai.XmitRecordDAI;
import opendcs.dao.XmitRecordDAO;
import decodes.util.CmdLineArgs;
import ilex.cmdline.StringToken;
import ilex.cmdline.TokenOptions;

/**
 * Read transmissions by date and medium ID (dcp address).
 * Usage: decj decodes.tsdb.ReadByIdTest [-y YYYYMMDD] mediumId
 */
public class ReadByIdTest 
	extends ReadAllXmitsTest
{
	protected StringToken idArg = new StringToken("", "mediumId", "",
		TokenOptions.optArgument |TokenOptions.optRequired, "");

	public ReadByIdTest()
	{
		super();
	}
	
	@Override
	protected void addCustomArgs(CmdLineArgs cmdLineArgs)
	{
		super.addCustomArgs(cmdLineArgs);
		cmdLineArgs.addToken(idArg);
	}


	@Override
	protected void runApp() throws Exception
	{
		Date d = getDay();
		XmitRecordDAI xmitRecordDAO = theDb.makeXmitRecordDao(31);
		int dayNum = XmitRecordDAO.msecToDay(d.getTime());
		System.out.println("dayNum=" + dayNum);
		ArrayList<DcpMsg> msgs = new ArrayList<DcpMsg>();
		xmitRecordDAO.readXmitsByMediumId(msgs, dayNum, XmitMediumType.GOES, idArg.getValue());
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
		ReadByIdTest prog = new ReadByIdTest();
		prog.execute(args);
	}

}
