/**
 * $Id$
 */
package lrgs.archive;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.LinkedList;
import java.util.Date;
import java.util.Iterator;
import java.util.TimeZone;

import decodes.util.ChannelMap;
import decodes.util.Pdt;
import decodes.util.PdtEntry;
import ilex.util.IDateFormat;
import ilex.util.Logger;
import lrgs.common.DcpAddress;
import lrgs.common.DcpMsgFlag;
import lrgs.common.DcpMsg;
import lrgs.drgsrecv.DrgsRecv;
import lrgs.lrgsmain.LrgsInputInterface;


/**
 * Validate Messages.
 * This class encapsulates message validation functions. It is used
 * by the DRGS interface and the DCP Monitor.
 */
public class MsgValidator
{
	private MsgValidatee caller;
	private Pdt pdt;
	private ChannelMap channelMap;
	
	public static final int SEC_PER_DAY = 86400;
	public static final double msecPerBit100 = 1000./100.;
	public static final double msecPerBit300 = 1000./300.;
	public static final double msecPerBit1200 = 1000./1200.;
	private SimpleDateFormat domsatDateFmt;
	private SimpleDateFormat errmsgDateFmt;
	private NumberFormat lenFormat;
	private CheckMissingThread checkMissingThread = null;
	private XmitWindow lastXmitWindow = null;
	
	private boolean _extraChecks = false;
	
	private long maxCarrierMS = 1500L;
	
	/** Anything < this will generate a warning */
	private int minSignalStrength = 35;
	
	/** Maximum allowable frequency offset (either + or -) in 50*Hz. */
	private int maxFreqOffset = 2;
	
	/** Minimum allowable battery voltage */
	private double minBattVolt = 11.0;
	
	/**
	An array of these objects is used to keep track of expected messages.
	*/
	class ExpectedMsg
	{
		/** Message expected by this (second-of-day) time */
		int endSod; 

		/** The PDT Schedule entry with dcp addr, channel, etc. */
		PdtEntry pdtEntry;
		
		ExpectedMsg(int endSod, PdtEntry pdtEntry)
		{
			this.endSod = endSod;
			this.pdtEntry = pdtEntry;
		}
	}

	/** An array of messages expected in the next minute. */
	private LinkedList<ExpectedMsg> expectedMsgList = new LinkedList<ExpectedMsg>();

	
	public MsgValidator(MsgValidatee caller, Pdt pdt, ChannelMap channelMap)
	{
		setCaller(caller);
		this.pdt = pdt;
		this.channelMap = channelMap;
		domsatDateFmt = new SimpleDateFormat("yyDDDHHmmss");
		domsatDateFmt.setTimeZone(TimeZone.getTimeZone("UTC"));
		errmsgDateFmt = new SimpleDateFormat("DDD/yyyy-HH:mm:ss");
		errmsgDateFmt.setTimeZone(TimeZone.getTimeZone("UTC"));
		lenFormat = NumberFormat.getIntegerInstance();
		lenFormat.setMinimumIntegerDigits(5);
		lenFormat.setGroupingUsed(false);
	}
	
	public synchronized void setCaller(MsgValidatee caller)
	{
		this.caller = caller;
	}
	
	/**
	 * Validate a message, issuing callbacks for any problems found.
	 * @param msg The message to validate
	 * @param origAddr From a DRGS this is original address, null for other apps.
	 * @param localRecvTime message local receive-time
	 */
	public synchronized void validateMsg(DcpMsg msg, 
		LrgsInputInterface src, Date localRecvTime)
	{
		if (!pdt.isLoaded())
			return;
		
		lastXmitWindow = null;
		DcpAddress dcpAddress = msg.getDcpAddress();
		PdtEntry pdtEntry = pdt.find(dcpAddress);
		if (pdtEntry == null)
		{
			caller.useValidationResults('I', 
				"Invalid DCP Address " + dcpAddress.toString(), 
				msg, src, localRecvTime, null);
			return;
		}
			
		int chan = msg.getGoesChannel();
		int expChan = channelMap.isRandom(chan) ? 
			pdtEntry.rd_channel : pdtEntry.st_channel;
		if (chan != expChan)
		{
			caller.useValidationResults('W',
				"Wrong channel -- expected " + expChan, msg, src, 
				localRecvTime, pdtEntry);
		}

		boolean isRandom = channelMap.isRandom(chan);
		
		// Check this entry off of the expected list, if it's there.
		if (!isRandom)
			for(Iterator<ExpectedMsg> expit = expectedMsgList.iterator();
				expit.hasNext(); )
			{
				ExpectedMsg expected = expit.next();
				if (expected.pdtEntry.dcpAddress.equals(dcpAddress))
				{
					expit.remove();
					break;
				}
			}

		if ((msg.flagbits & DcpMsgFlag.ADDR_CORRECTED) != 0 
		 && msg.getOrigAddress() != null)
			caller.useValidationResults('A', 
				"Address corrected, original=" + msg.getOrigAddress(), 
				msg, src, localRecvTime, pdtEntry);

		int xi = pdtEntry.st_xmit_interval;
		if (!isRandom && xi != 0)
		{
			// Compute expected start & end time for this message.
			Date xmitTime = msg.getXmitTime();
			Date cstart = msg.getCarrierStart();
			if (cstart != null)
				xmitTime = cstart;
			long xmitmsec = xmitTime.getTime();
			long xmit_timet = xmitmsec/1000L;
			long base_timet = (xmit_timet/SEC_PER_DAY - 1) * SEC_PER_DAY;
			long windows_since_base =
				((xmit_timet - base_timet - pdtEntry.st_first_xmit_sod) + xi/2)
				/ xi;
			long expected_start_tt = base_timet + pdtEntry.st_first_xmit_sod
				+ windows_since_base * xi;
			long expected_end_tt = expected_start_tt + pdtEntry.st_xmit_window;
			lastXmitWindow = new XmitWindow(pdtEntry.st_first_xmit_sod,
				pdtEntry.st_xmit_window, pdtEntry.st_xmit_interval, 
				(int)(expected_start_tt % SEC_PER_DAY));

			// Determine msg end time to nearest msec.
			Date cstop = msg.getCarrierStop();
			long end_msec;
			if (cstop != null)
				end_msec = cstop.getTime();
			else
			{
				int baud = msg.getBaud();
				double bitlen = msg.getData().length * 8;
				long durmsec = (long)(bitlen * 
					(baud == 100 ? msecPerBit100 :
					 baud == 1200 ? msecPerBit1200: msecPerBit300));
				end_msec = xmitmsec + durmsec;
			}
			
			if (xmitmsec < expected_start_tt*1000L) // Early
			{
				if (end_msec < expected_start_tt*1000L) // U=way early
				{
					String errmsg = 
						"Very Early: nearest window for ("
						+ (cstart==null?"DAPS":"CARRIER") + ") " 
						+ errmsgDateFmt.format(xmitTime)
						+ " is " 
						+ IDateFormat.printSecondOfDay(expected_start_tt, true)
						+ "-" 
						+ IDateFormat.printSecondOfDay(expected_end_tt, true);
					caller.useValidationResults('U', errmsg,
						msg, src, localRecvTime, pdtEntry);
				}
				else
				{
					caller.useValidationResults('T',
						"Early: Expected window is " 
						+ IDateFormat.printSecondOfDay(expected_start_tt, true) 
						+ "-" 
						+ IDateFormat.printSecondOfDay(expected_end_tt,	true),
						msg, src, localRecvTime, pdtEntry);
				}
			}
			else if (xmitmsec <= expected_end_tt*1000L)
			{
				if (end_msec > expected_end_tt*1000L) // Ended late
				{
					caller.useValidationResults('T',
						"Late: Expected window is " 
						+ IDateFormat.printSecondOfDay(expected_start_tt, true) 
						+ "-" 
						+ IDateFormat.printSecondOfDay(expected_end_tt, true),
						msg, src, localRecvTime, pdtEntry);
				}
			}
			else // start was after expected end.
			{
				String errmsg = 
					"Very Late: nearest window for ("
					+ (cstart==null?"DAPS":"CARRIER") + ") "
					+ errmsgDateFmt.format(xmitTime)
					+ " is " 
					+ IDateFormat.printSecondOfDay(expected_start_tt, true)
					+ "-" 
					+ IDateFormat.printSecondOfDay(expected_end_tt, true);
				caller.useValidationResults('U', errmsg,
					msg, src, localRecvTime, pdtEntry);
			}
	 	}
		if (!_extraChecks)
			return;
		
		Date xmitTime = msg.getXmitTime();
		Date cstart = msg.getCarrierStart();
		if (xmitTime != null && cstart != null)
		{
			long carrierMsec = xmitTime.getTime() - cstart.getTime();
			if (carrierMsec > maxCarrierMS)
				caller.useValidationResults('C', 
					"Excessive carrier: " + carrierMsec + " ms."
					+ ", xmitTime=" + xmitTime + ", carrierStart=" + cstart,
					msg, src, localRecvTime, pdtEntry);
		}
		
		int ss = msg.getSignalStrength();
		if (ss < minSignalStrength)
			caller.useValidationResults('S', 
				"Low signal strength: " + ss + " dB.",
				msg, src, localRecvTime, pdtEntry);
	
		int fo = msg.getFrequencyOffset();
		if (fo > maxFreqOffset || fo < -maxFreqOffset)
			caller.useValidationResults('F', 
				"Excessive Frequency Offset: " + fo + " ("
				+ (fo*50) + " Hz.)",
				msg, src, localRecvTime, pdtEntry);
		
		char mi = msg.getModulationIndex();
		if (mi != 'N')
			caller.useValidationResults('X', 
				"Bad modulation index code: " + mi,
				msg, src, localRecvTime, pdtEntry);
		
		double bv = msg.getBattVolt();
		if (bv < -.1 || (bv > .1 && bv < minBattVolt))
			caller.useValidationResults('V', 
				"Low battery voltage: " + bv,
				msg, src, localRecvTime, pdtEntry);
	}
	
	

	/**
	 * Add to the expected array all messages expected by the minute that
	 * ends with the specified time.
	 * @param sod second-of-day of the minute-end 
	 */
	public synchronized void findExpectedBy(int sod)
	{
		if (sod == 0)
			sod = (3600*24); 	//  for midnight use 86400.

		synchronized(pdt)
		{
			for(PdtEntry pe : pdt.getEntries())
			{
				int xi = pe.st_xmit_interval;
				int stChan = pe.st_channel;

				if (stChan <= 0 || xi <= 0)
					continue;
				if (pe.active_flag == 'N')
					continue;

				int offset = pe.st_xmit_window + pe.st_first_xmit_sod;
				int mnum = (sod - offset) / xi;
				int expectBy = offset + mnum * xi;
				if (expectBy > sod-60 && expectBy <= sod)
					expectedMsgList.add(new ExpectedMsg(expectBy, pe));
			}
		}
	}

	/**
	 * Generate MISSING status messages for anything in the expected-array
	 * that was expected by the specified second-of-day.
	 * Finally the expected array is cleared.
	 * @param sod second of day (midnight = 86400)
	 * @param daynum since the epoch.
	 */
	public synchronized void genMissingFor(int sod, int daynum)
	{
		if (!pdt.isLoaded())
			return;
		
		for(Iterator<ExpectedMsg> expit = expectedMsgList.iterator();
			expit.hasNext(); )
		{
			ExpectedMsg expected = expit.next();
			Date msgTime = new Date(
				(daynum * SEC_PER_DAY + expected.endSod)*1000L);
			if (expected.endSod < sod)
			{
				String body = "Missing message from "
					+ expected.pdtEntry.dcpAddress.toString()
					+ " expected by "
					+ IDateFormat.printSecondOfDay(expected.endSod, true);
				String msgData = expected.pdtEntry.dcpAddress.toString() 
					+ formatDomsatDate(msgTime)
					+ "M00+0NN"
					+ fmtChan(expected.pdtEntry.st_channel)
					+ "--"
					+ formatLength(body.length())
					+ body;
				byte[] md = msgData.getBytes();
				DcpMsg msg = new DcpMsg(md, md.length);
				msg.setBaud(expected.pdtEntry.baud);
				
				caller.useValidationResults('M', body, msg, 
					(LrgsInputInterface)null, msgTime, expected.pdtEntry);
				expit.remove();
			}
		}
	}

	public void startCheckMissingThread()
	{
		checkMissingThread = new CheckMissingThread(this);
		checkMissingThread.start();
	}
	
	/**
	 * @return String in the format cccS, where ccc is 3-digit channel number
	 * and S is the spacecraft designator.
	 */
	public String fmtChan(int chan)
	{
		byte b[] = new byte[4];
		b[0] = (byte)((int)'0' + chan / 100);
		chan %= 100;
		b[1] = (byte)((int)'0' + chan / 10);
		b[2] = (byte)((int)'0' + chan % 10);
		b[3] = (chan%2 == 1 ? (byte)'E' : (byte)'W');
		return new String(b);
	}
	
	public String formatLength(int len)
	{
		synchronized(lenFormat)
		{
			return lenFormat.format(len);
		}
	}

	public String formatDomsatDate(Date d)
	{
		synchronized(domsatDateFmt)
		{
			return domsatDateFmt.format(d);
		}
	}

	/**
	 * Call before application exit to release resources & stop the missing-
	 * message check thread.
	 */
	public synchronized void shutdown()
	{
		if (checkMissingThread != null)
			checkMissingThread.shutdown = true;
		expectedMsgList.clear();
	}

	/**
     * @return the lastXmitWindow
     */
    public XmitWindow getLastXmitWindow()
    {
	    return lastXmitWindow;
    }

    /**
     * Call with true to have validator perform the additional checks for
     * C=excessive carrier, V=low battery, S=low signal strength,
     * F=excessive Frequency Offset, X=bad modulation index.
     * @param tf
     */
    public void setDoExtraChecks(boolean tf) { _extraChecks = tf; }

	/**
     * @param maxCarrierMS Maximum allowable carrier in msec
     */
    public void setMaxCarrierMS(long maxCarrierMS)
    {
	    this.maxCarrierMS = maxCarrierMS;
    }

	/**
     * @return Maximum allowable carrier in msec
     */
    public long getMaxCarrierMS()
    {
	    return maxCarrierMS;
    }

	/**
     * @param minSignalStrength Minimum allowable signal strength in dB
     */
    public void setMinSignalStrength(int minSignalStrength)
    {
	    this.minSignalStrength = minSignalStrength;
    }

	/**
     * @return Minimum allowable signal strength in dB
     */
    public int getMinSignalStrength()
    {
	    return minSignalStrength;
    }

	/**
     * @param maxFreqOffset maximum allowable freq offset in units of 50Hz
     */
    public void setMaxFreqOffset(int maxFreqOffset)
    {
	    this.maxFreqOffset = maxFreqOffset;
    }

	/**
     * @return maximum allowable freq offset in units of 50Hz
     */
    public int getMaxFreqOffset()
    {
	    return maxFreqOffset;
    }

	/**
     * @param minBattVolt the minBattVolt to set
     */
    public void setMinBattVolt(double minBattVolt)
    {
	    this.minBattVolt = minBattVolt;
    }

	/**
     * @return the minBattVolt
     */
    public double getMinBattVolt()
    {
	    return minBattVolt;
    }
}


class CheckMissingThread
	extends Thread
{
	MsgValidator validator;
	boolean shutdown = false;
	public CheckMissingThread(MsgValidator validator)
	{
		this.validator = validator;
	}
	public void run()
	{
		int lastMin = -1;
		long lastPoll = System.currentTimeMillis();
		while(!shutdown)
		{
			long now = System.currentTimeMillis();

			/*
			  At each new minute, generate MISSING stat messages for
			  anything expected by one minute ago. Then add expectations
			  for one minute from now.
			*/
			int min = (int)((now/60000L) % (60*24));
			if (min != lastMin)
			{
				lastMin = min;

				int daynum = (int)(now / DrgsRecv.MS_PER_DAY);
				int prevMin = min - 1;
				if (prevMin <= 0) 
				{
					prevMin += (60*24);
					daynum--;
				}
				validator.genMissingFor(prevMin*60, daynum);

				int nextMin = (min+1) % (60*24);
				validator.findExpectedBy(nextMin*60);
			}
			try { sleep(1000L); }
			catch(InterruptedException ex) {}
		}
	}


}
