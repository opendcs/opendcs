/*
*  $Id$
*/
package decodes.dcpmon1;

import ilex.util.ArrayUtil;
import ilex.util.Logger;
import ilex.var.NoConversionException;
import ilex.var.Variable;

import java.io.OutputStream;
import java.util.Date;
import java.util.Iterator;
import java.util.Properties;

import lrgs.archive.MsgValidatee;
import lrgs.archive.MsgValidator;
import lrgs.archive.XmitWindow;
import lrgs.common.DcpAddress;
import lrgs.common.DcpMsg;
import lrgs.common.DcpMsgFlag;
import lrgs.lrgsmain.LrgsInputInterface;
import decodes.consumer.DataConsumer;
import decodes.consumer.DataConsumerException;
import decodes.datasource.GoesPMParser;
import decodes.datasource.RawMessage;
import decodes.db.Constants;
import decodes.db.DataType;
import decodes.decoder.DecodedMessage;
import decodes.decoder.Sensor;
import decodes.decoder.TimeSeries;
import decodes.util.ChannelMap;
import decodes.util.Pdt;
import decodes.util.PdtEntry;

/**
This class is part of the DCP Monitor application.
It puts data into the SQL Database.
It implements the decodes.consumer.DataConsumer interface, allowing
it to be the recipient of DecodedMessage objects in a routing spec.
*/
public class DcpMonitorConsumer 
	extends DataConsumer
	implements MsgValidatee
{
	/** time stamp of last RawMessage received */
	Date lastTimeStamp;

	DcpMonitorConfig cfg;
	public static final String module = "DcpMonitorConsumer";
	
	private MsgValidator msgValidator = null;
	
	private XmitRecord workingXr = null;

	private static final int defaultDcpMsgFlag = 
		DcpMsgFlag.MSG_PRESENT | DcpMsgFlag.MSG_NO_SEQNUM
		| DcpMsgFlag.CARRIER_TIME_EST;

	/** Constructor */
	public DcpMonitorConsumer()
	{
		super();
		lastTimeStamp = null;
		cfg = DcpMonitorConfig.instance();
	}

	/**
	  Does nothing.
	  @param consumerArg ignored
	  @param props ignored
	*/
	public void open(String consumerArg, Properties props)
		throws DataConsumerException
	{
		DcpMonitorConfig cfg = DcpMonitorConfig.instance();
		msgValidator = new MsgValidator(this, Pdt.instance(),
			ChannelMap.instance());
		msgValidator.setMaxCarrierMS(cfg.maxCarrierMS);
Logger.instance().info("MaxCarrierMS=" + cfg.maxCarrierMS);
		msgValidator.setMinSignalStrength(cfg.yellowSignalStrength);
		msgValidator.setMaxFreqOffset(cfg.yellowFreqOffset);
		msgValidator.setMinBattVolt(cfg.yellowBattery);
		msgValidator.setDoExtraChecks(true);
// MJM We don't want it to check for missing messages. We generate
// the 'M' codes as needed when formatting the web site. They aren't
// saved in the database.
//		msgValidator.startCheckMissingThread();
	}

	/**
	  Does nothing.
	*/
	public void close()
	{
		if (msgValidator != null)
			msgValidator.shutdown();
		msgValidator = null;
	}
	
	public void finalize()
	{
		close();
	}

	/**
	  Called when a new message is about to be output.
	  It extracts the performance measurement parameters and stores them in
	  a memory cache.
	  @param msg the DecodedMessage
	  @throws DataConsumerException if an error occurs.
	*/
	public void startMessage(DecodedMessage msg)
		throws DataConsumerException
	{
		RawMessage rawMsg = null;
		Date timeStamp = null;
		long tsms = 0L;
		int day = 0;
		String mediumId = "";
		Variable v = null;
		workingXr = null;
		DcpMsg dcpMsg = null;

		char failureCode = 'x';
		PlatformInfo platformInfo = null;
		String hdr = "";
		try
		{
			rawMsg = msg.getRawMessage();
			dcpMsg = rawMsg.getOrigDcpMsg();
			lastTimeStamp = rawMsg.getTimeStamp();

			timeStamp = rawMsg.getTimeStamp();
			if (timeStamp == null)
			{
				error(rawMsg, "No time stamp");
				return;
			}
			tsms = timeStamp.getTime();
			day = RecentDataStore.msecToDay(tsms);
			int sod = RecentDataStore.msecToSecondOfDay(tsms);
			mediumId = rawMsg.getMediumId().trim();
			if (mediumId.equalsIgnoreCase("BBBBBBBB")
			 || mediumId.equalsIgnoreCase("DADADADA")
			 || mediumId.equalsIgnoreCase("11111111")
			 || mediumId.equalsIgnoreCase("22222222")
			 || mediumId.equalsIgnoreCase("33333333"))
				return;
//Logger.instance().info("Received " + new String(rawMsg.getHeader()));
			DcpAddress dcpAddress = new DcpAddress(mediumId);

			failureCode = getVarCharValue(
				rawMsg.getPM(GoesPMParser.FAILURE_CODE), 'x');
			if (failureCode == 'x')
			{
				error(rawMsg, "No Failure Code");
				return;
			}
			if (failureCode != 'G' && failureCode != '?')
			{
				// Shouldn't happen, our searchcrit doesn't accept abnormal
				// response (a.k.a. DAPS status) messages.
				Logger.instance().warning(
					"Ignoring received DAPS Status Message '" + failureCode
					+ "'");
				return;
			}

			hdr = new String(rawMsg.getHeader());

			// Look for this Xmit record in the queue
			workingXr = XRWriteThread.instance().find(dcpAddress, tsms);
			if (workingXr == null)
				// Not in Queue: Look in the SQL DB
				workingXr = 
					DcpMonitor.instance().findDcpTranmission(dcpAddress, 
						timeStamp, day);
			if (workingXr == null)
				// Don't have it yet -- create new record.
				workingXr = new XmitRecord(dcpAddress, sod, day);
			workingXr.addCode(failureCode);
			workingXr.rmFailureCode('M'); // if it was missing, it is no longer.

			//Verify if we have to remove any old records from old days.
			//This should happen once a day only, when we are in a new day
			//The scrub method removes all records from the Database, 
			//if we say store 1 day - it will remove anything older than
			//1 day
			RecentDataStore.instance().scrub(day);
			
			boolean isMine = DcpGroupList.instance().isInGroup(dcpAddress);
			if (!isMine)
				workingXr.addFlags(XmitRecordFlags.NOT_MY_DCP);

			int chan = (int)getVarLongValue(
				rawMsg.getPM(GoesPMParser.CHANNEL), 0);
			if (!DcpMonitor.instance().isMyChannel(chan))
			{
				Logger.instance().warning("Received msg for DCP "
					+ mediumId + " on channel " + chan
					+ ". This is not one of my channels -- discarding!"
					+ " Hdr: [" + hdr + "]");
				return;
			}

			workingXr.setGoesChannel(chan);

			// Uplink Carrier 2-chars used to store DRGS source code.
			v = rawMsg.getPM(GoesPMParser.UPLINK_CARRIER);
			if (v != null)
				workingXr.setDrgsCode(v.getStringValue());

			// Get platform info from PDT or DECODES DB.
			platformInfo = 
				PlatformInfo.getPlatformInfo(rawMsg,dcpAddress,chan);

			// Add the message to the Xmit Record obj (raw message
			// is the entire msg including header)
			// check the length
			if (rawMsg.getData().length > 7500)// entire msg
			{ // The table field holds 10,000 characters, but we are
				// using Base64 - so the max we can have is 7500, Base64
				// will add a byte for every 3 bytes
				Logger.instance().failure(
				    "Received a DCP Msg longer than"
				        + " 7500 characters. Dcp Address: "
				        + dcpAddress.toString() + " Truncating the Message.");
				workingXr.setRawMsg(ArrayUtil.getField(rawMsg.getData(), 0, 7500));
			}
			else
			{
				workingXr.setRawMsg(rawMsg.getData());// save the entire message
			}

			int baud = dcpMsg.getBaud();
			if (baud == 0)
			{
				if (platformInfo != null)
					baud = platformInfo.baud;
				else // set baud to 300 (most common baud rate.)
					baud = 300;
			}
			workingXr.addFlags(
				  baud == 100 ? XmitRecordFlags.BAUD_100
			    : baud == 300 ? XmitRecordFlags.BAUD_300
			    : baud == 1200 ? XmitRecordFlags.BAUD_1200
			    : XmitRecordFlags.BAUD_UNKNOWN);

			if (platformInfo != null && !platformInfo.msgIsST)
				workingXr.addFlags(XmitRecordFlags.IS_RANDOM);

			int flags = (int)getVarLongValue(
				rawMsg.getPM(GoesPMParser.DCP_MSG_FLAGS), defaultDcpMsgFlag);

			// Set start time to carrier if we have it, else GOES time.
			Variable cstartv = rawMsg.getPM(GoesPMParser.CARRIER_START);
			long cstart = getVarLongValue(cstartv, tsms);
//try { Logger.instance().info("cstartv is " + (cstartv!=null?"NOT":"") 
//+ " null. v=" +cstartv.getDateValue() + ", tsms=" + tsms);
//Logger.instance().info("DCPMsg.carrierStart = " + dcpMsg.getCarrierStart()
//+ ", localRcv is " + dcpMsg.getLocalReceiveTime());
//}
//catch(Exception ex) {}
			long dt = tsms - cstart;
			if (dt < -3600000L || dt > 3600000L)
			{
				error(rawMsg, "Invalid carrier start (delta=" + dt
					+ "ms) - using msg time.");
				cstart = tsms;
			}
			workingXr.setCarrierStart(cstart);

			workingXr.setMsgLength((int)getVarLongValue(
				rawMsg.getPM(GoesPMParser.MESSAGE_LENGTH), 0));
			
			// Set stop time. Compute if it's not available from the source.
			long cstop = getVarLongValue(
				rawMsg.getPM(GoesPMParser.CARRIER_STOP), 0L);
			boolean haveStop = (cstop > cstart && cstop - cstart < 3600000L);

			if (haveStop)
			{
				workingXr.setCarrierEnd(cstop);
				// We have start and stop. If NOT estimated, then
				// set the XR flag indicating we have MSEC
				// resolution
				workingXr.setHasCarrierTimeMsec(
					(flags & DcpMsgFlag.CARRIER_TIME_EST) == 0);
			}
			else
			{
				// Compute duration for the bits.
				double dursec = workingXr.getMsgLength() * 8.0 / (double) baud;
				char preamble = XmitRecordFlags.getPreamble(workingXr);

				// Add in the overhead.
				if (baud == 100)
					dursec += (preamble == 'L' ? 7.760 : 1.44);
				else if (baud == 300)
					dursec += .693;
				else
					// 1200
					dursec += .298;

				// Compute end time
				long durmsec = (long) (dursec + .5) * 1000L;
				cstop = cstart + durmsec;
				workingXr.setCarrierEnd(cstop);
				workingXr.setHasCarrierTimeMsec(false);
			}

			// Get rest of header fields.
			workingXr.setSignalStrength((int)getVarLongValue(
				rawMsg.getPM(GoesPMParser.SIGNAL_STRENGTH), 0L));
			
			workingXr.setFreqOffset((int)getVarLongValue(
				rawMsg.getPM(GoesPMParser.FREQ_OFFSET), 0L));

			workingXr.setModIndex(getVarCharValue(
				rawMsg.getPM(GoesPMParser.MOD_INDEX), '?'));

			float bv = getBattVolt(msg);
			workingXr.setBattVolt(getBattVolt(msg));
			if (platformInfo.preamble != 'S')
				workingXr.addFlags(XmitRecordFlags.LONG_PREAMBLE);
			if (dcpMsg != null)
				dcpMsg.setBattVolt(bv);

			if (dcpMsg != null)
				msgValidator.validateMsg(dcpMsg, null, new Date());
			XmitWindow xmitWindow = msgValidator.getLastXmitWindow();
			
			if (xmitWindow != null)
			{
				workingXr.firstXmitSecOfDay = xmitWindow.firstXmitSecOfDay;
				workingXr.setWindowLength(xmitWindow.windowLengthSec);
				workingXr.setXmitInterval(xmitWindow.xmitInterval);
				workingXr.setWindowStartSec(xmitWindow.thisWindowStart);
			}

			// Finally, XR is either new or modified. Add it to the database's
			// write queue.
			XRWriteThread xwt = decodes.dcpmon1.XRWriteThread.instance();
			for(int nTries = 0; nTries < 10; nTries++)
			{
				Logger.instance().debug3("Enqueue: Enqueue XR started; nTries = " + nTries);
				if (xwt.enqueue(workingXr))
				{
					Logger.instance().debug3("Enqueue: Enqueue XR ended; nTries = " + nTries);
					return;
				}
				else
				{
					Logger.instance().warning(
						"Queue full, retrieval thread will pause.");
					try { Thread.sleep(2000L); }
					catch(InterruptedException ex) {}
				}
			}
			error(rawMsg, "Queue Full: Cannot enqueue to database!");
		}
		catch(NullPointerException ex)
		{
			System.err.println(module + 
				" NullPointerException in DcpMonitor Consumer: " + ex);
			ex.printStackTrace();
			return;
		}
		catch(BadDateException ex)
		{
			error(rawMsg, " Ignored message with invalid day number " 
				+ day + ": " + hdr);
			return;
		}
	}

	private void error(RawMessage rawmsg, String reason)
	{
		Logger.instance().warning("DCPMon Input problem: '" 
			+ (new String(rawmsg.getHeader())) + "': " + reason);
	}

	/**
	  Delegate to the current DirectoryConsumer to write the HTML file.
	  @param line ignored.
	*/
	public void println(String line)
	{
	}

	/**
	  Delegate to the current DirectoryConsumer to write the HTML file.
	*/
	public void endMessage()
	{
//		Logger.instance().debug3("endMessage done. (empty method)");
	}

	/**
	  Looks for, and returns latest value of, a battery voltage sensor.
	  @return battery voltage or 0.0 if there is none.
	*/
	public static float getBattVolt(DecodedMessage msg)
	{
		// Find sensor named 'batt' or with data type VB or equiv.
		TimeSeries batt_ts = null;
		Iterator<TimeSeries> it = msg.getAllTimeSeries();
		if (it == null)
			return (float)0.0;
		while(it.hasNext())
		{
			TimeSeries ts = it.next();
			Sensor sensor = ts.getSensor();

			if (sensor.getName().toLowerCase().startsWith("batt")
				 && ts.size() > 0)
			{
				batt_ts = ts;
				break;
			}
		}
		if (batt_ts == null)
		{
			// No match for sensor name starting with "batt".
			// Look for data type of SHEF VB
			DataType bv=
				DataType.getDataType(Constants.datatype_SHEF,"VB");
			for(it = msg.getAllTimeSeries(); it.hasNext(); )
			{
				TimeSeries ts = (TimeSeries)it.next();
				Sensor sensor = ts.getSensor();

				DataType sdt = sensor.getDataType();
				if (sdt == null)
					continue;

				if (bv.isEquivalent(sensor.getDataType()) && ts.size() > 0)
				{
					batt_ts = ts;
					break;
				}
			}
		}
		if (batt_ts != null)
		{
			// Take the most recent sample
			batt_ts.sort();
			try
			{
				double bv = 
					batt_ts.sampleAt(batt_ts.size()-1).getDoubleValue();
				return (float)bv;
			}
			catch(ilex.var.NoConversionException ex)
			{
			}
		}
		// Either couldn't find BV or it was in a bad format.
		return (float)0.0;
	}

	public OutputStream getOutputStream()
		throws DataConsumerException
	{
			return null;
	}

	/** From MsgValidatee interface */
	public void useValidationResults(char failureCode, String explanation,
			DcpMsg msg, LrgsInputInterface src, Date msgTimeStamp, 
			PdtEntry pdtEntry)
	{
		// Ignore failure codes that user doesn't want to see.
		if (cfg.omitFailureCodes.indexOf(failureCode) >= 0)
			return;

		// All codes but 'M' (missing) happen synchronously from the
		// 'validate' method called above. So we know we have the applicable
		// xmit record in 'workingXr'.
		if (failureCode != 'M')
		{
			workingXr.addCode(failureCode);
			return;
		}
		
		//MJM-MISSING New code - don't save 'M' records at all. 
		return;
		
//		// 'M' missing messages are called asynchronously from the validator
//		// at the end of the minute where the message was expected.
//		// It also means we are being called from a different thread.
//
//		// Search for a matching message, if one found, it's not missing.
//		Date xmitTime = msg.getXmitTime();
//		long xmitTimeMS = xmitTime.getTime();
//		XmitRecord mxr = DcpMonitor.instance().findDcpTranmission(
//			msg.getDcpAddress(), xmitTime, 
//			(int)(xmitTimeMS / (MsgValidator.SEC_PER_DAY*1000L))); 
//		if (mxr != null)
//			return; // It's not missing. I already have it.
//		
//		// Create a new XmitRecord and enque it.
//		int xmitTimeTT = (int)(xmitTimeMS / 1000L);
//		DcpAddress dcpAddress = msg.getDcpAddress();
//		mxr = new XmitRecord(dcpAddress, 
//			(xmitTimeTT % MsgValidator.SEC_PER_DAY),
//			(xmitTimeTT / MsgValidator.SEC_PER_DAY));
//
//		mxr.addCode('M');
//
//		boolean isMine = DcpGroupList.instance().isInGroup(dcpAddress);
//		if (!isMine)
//			mxr.addFlags(XmitRecordFlags.NOT_MY_DCP);
//
//		int chan = msg.getGoesChannel();
//		if (!DcpMonitor.instance().isMyChannel(chan))
//			return;
//		mxr.setGoesChannel(chan);
//
//		// Uplink Carrier 2-chars used to store DRGS source code.
//		mxr.setDrgsCode(msg.getDrgsCode());
//
//		mxr.setRawMsg(msg.getData());
//		mxr.setMsgLength(msg.getData().length);
//
//		int baud = msg.getBaud();
//		mxr.addFlags((baud == 100 ? XmitRecordFlags.BAUD_100
//		    : baud == 300 ? XmitRecordFlags.BAUD_300
//	        : baud == 1200 ? XmitRecordFlags.BAUD_1200
//            : XmitRecordFlags.BAUD_UNKNOWN));
//
//		// Set stop time. Compute if it's not available from the source.
//		mxr.setHasCarrierTimeMsec(false);
//
//		// Get rest of header fields.
//		mxr.setSignalStrength(msg.getSequenceNum());
//		mxr.setFreqOffset(msg.getFrequencyOffset());
//
//		mxr.setModIndex(msg.getModulationIndex());
//
//		mxr.firstXmitSecOfDay = pdtEntry.st_first_xmit_sod;
//		mxr.setWindowLength(pdtEntry.st_xmit_window);
//		mxr.setXmitInterval(pdtEntry.st_xmit_interval);
//
//		// Caller will set xmit time to end of window.
//		int wStart = xmitTimeTT % MsgValidator.SEC_PER_DAY;
//		if (wStart == 0) wStart = MsgValidator.SEC_PER_DAY;
//		wStart -= pdtEntry.st_xmit_window;
//		mxr.setWindowStartSec(wStart);
//
//		for(int nTries=0; nTries<10; nTries++)
//		{
//			if (decodes.dcpmon1.XRWriteThread.instance().enqueue(mxr))
//				return;
//			else
//			{
//				Logger.instance().warning(
//					"Queue full, check-missing thread will pause.");
//				try { Thread.sleep(2000L); }
//				catch(InterruptedException ex) {}
//			}
//		}
//		Logger.instance().warning(module + 
//			" Queue Full: Cannot enqueue 'M' to database!");
	}

	public static long getVarLongValue(Variable v, long dflt)
	{
		if (v == null)
			return dflt;
		try { return v.getLongValue(); }
        catch (NoConversionException ex) { return dflt; }
	}

	public static char getVarCharValue(Variable v, char dflt)
	{
		if (v == null)
			return dflt;
		try { return v.getCharValue(); }
        catch (NoConversionException ex) { return dflt; }
	}
	
//	private double getVarDblValue(Variable v, double dflt)
//	{
//		if (v == null)
//			return dflt;
//		try { return v.getDoubleValue(); }
//        catch (NoConversionException ex) { return dflt; }
//	}

}

