package decodes.dcpmon;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import lrgs.archive.XmitWindow;
import lrgs.common.DcpMsg;

import opendcs.dai.XmitRecordDAI;
import opendcs.dao.XmitRecordDAO;

import ilex.cmdline.StringToken;
import ilex.cmdline.TokenOptions;
import decodes.tsdb.TsdbAppTemplate;
import decodes.util.CmdLineArgs;

/**
 * Read all transmissions for a given day.
 * Usage: decj decodes.dcpmon.ReadAllXmitsTest [-y YYYYMMDD]
 * @author mmaloney
 */
public class ReadAllXmitsTest extends TsdbAppTemplate
{
	protected StringToken dateArg = new StringToken("y", "YYYYMMDD default=today", "",
		TokenOptions.optSwitch, "");
	protected SimpleDateFormat dateSdf = new SimpleDateFormat("yyyyMMdd");
	protected SimpleDateFormat msecSdf = new SimpleDateFormat("yyyy/MM/dd-HH:mm:ss.SSS");

	public ReadAllXmitsTest()
	{
		super("test.log");
		dateSdf.setTimeZone(TimeZone.getTimeZone("UTC"));
		msecSdf.setTimeZone(TimeZone.getTimeZone("UTC"));
	}
	
	@Override
	protected void addCustomArgs(CmdLineArgs cmdLineArgs)
	{
		cmdLineArgs.addToken(dateArg);
	}

	protected Date getDay() throws ParseException
	{
		return dateArg.getValue().length() > 0 ? dateSdf.parse(dateArg.getValue()) : new Date();
	}

	@Override
	protected void runApp() throws Exception
	{
		Date d = getDay();
		XmitRecordDAI xmitRecordDAO = theDb.makeXmitRecordDao(31);
		int dayNum = XmitRecordDAO.msecToDay(d.getTime());
		System.out.println("dayNum=" + dayNum);
		DcpMsg msg = null;
		long recId;
		for(recId = 1L; (msg = xmitRecordDAO.readDcpMsg(dayNum, recId)) != null; recId++)
			printMsg(msg);
		System.out.println(recId + " messages processed.");
		System.exit(0);
	}

	protected void printMsg(DcpMsg msg)
	{
		System.out.println();
		System.out.println("============ recordId = " + msg.getRecordId() + " ============");
		System.out.println("Medium Type=" + XmitMediumType.flags2type(msg.getFlagbits()).code
			+ " Id=" + msg.getDcpAddress() 
			+ ", flags=0x" + Integer.toHexString(msg.getFlagbits())
			+ ", failureCodes=" + msg.getXmitFailureCodes()
			+ ", recvTime=" + msecSdf.format(msg.getLocalReceiveTime()));
		XmitWindow xmitWindow = msg.getXmitTimeWindow();
		if (xmitWindow != null)
			System.out.println("Window Start=" + xmitWindow.thisWindowStart
				+ " Len=" + xmitWindow.windowLengthSec
				+ " Interval=" + xmitWindow.xmitInterval
				+ " First=" + xmitWindow.firstXmitSecOfDay);
		System.out.print("xmitTime=" + msecSdf.format(msg.getXmitTime()));
		if (msg.getCarrierStart() != null)
			System.out.print(", carrier start=" + msecSdf.format(msg.getCarrierStart())
				+ ", stop=" + msecSdf.format(msg.getCarrierStop()));
		System.out.println();
		System.out.println("Battery=" + msg.getBattVolt() + ", msgLength=" + msg.getMessageLength());
		System.out.println("Data:");
		System.out.println(new String(msg.getData()));
	}

	/**
	 * @param args
	 */
	public static void main(String[] args)
		throws Exception
	{
		ReadAllXmitsTest prog = new ReadAllXmitsTest();
		prog.execute(args);
	}

}
