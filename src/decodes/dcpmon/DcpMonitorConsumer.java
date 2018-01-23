/*
*  $Id$
*/
package decodes.dcpmon;

import ilex.util.Logger;
import ilex.var.IFlags;
import ilex.var.NoConversionException;
import ilex.var.TimedVariable;
import ilex.var.Variable;

import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Properties;
import java.util.TimeZone;

import lrgs.archive.MsgValidatee;
import lrgs.archive.MsgValidator;
import lrgs.common.DcpAddress;
import lrgs.common.DcpMsg;
import lrgs.lrgsmain.LrgsInputInterface;
import decodes.consumer.DataConsumer;
import decodes.consumer.DataConsumerException;
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
This class is used by a routing spec to ingest transmission records
into the database for the web DCP Monitor application.
*/
public class DcpMonitorConsumer 
	extends DataConsumer
	implements MsgValidatee
{
	/** time stamp of last RawMessage received */
	Date lastTimeStamp;

	DcpMonitorConfig cfg;
	public static final String module = "DcpMonitorConsumer";
	private SimpleDateFormat debugSdf = new SimpleDateFormat("yyyy/MM/dd-HH:mm:ss z");
	enum Action { Ignore, Save, ReplaceExisting, ModifyExisting };
	
	private MsgValidator msgValidator = null;
	
	/** Constructor */
	public DcpMonitorConsumer()
	{
		super();
		lastTimeStamp = null;
		cfg = DcpMonitorConfig.instance();
		debugSdf.setTimeZone(TimeZone.getTimeZone("UTC"));
	}

	/**
	  Does nothing.
	  @param consumerArg ignored
	  @param props ignored
	*/
	public void open(String consumerArg, Properties props)
		throws DataConsumerException
	{
		Logger.instance().debug1("DcpMonitorConsumer.open()");
		DcpMonitorConfig cfg = DcpMonitorConfig.instance();
		msgValidator = new MsgValidator(this, Pdt.instance(),
			ChannelMap.instance());
		msgValidator.setMaxCarrierMS(cfg.maxCarrierMS);
		msgValidator.setMinSignalStrength(cfg.yellowSignalStrength);
		msgValidator.setMaxFreqOffset(cfg.yellowFreqOffset);
		msgValidator.setMinBattVolt(cfg.yellowBattery);
		msgValidator.setDoExtraChecks(true);
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
		Date xmitTime = null;
		DcpMsg dcpMsg = null;

		char failureCode = 'x';
		try
		{
			rawMsg = msg.getRawMessage();
			dcpMsg = rawMsg.getOrigDcpMsg();
			if (dcpMsg == null)
			{
				error(rawMsg, 
					"rawMsg without DcpMsg ignored. " + "Medium ID=" + rawMsg.getMediumId());
				return;
			}
			Logger.instance().debug3("DcpMonitorConsumer.startMessage() hdr=" + dcpMsg.getHeader()
				+ ", flags=0x" + Integer.toHexString(dcpMsg.getFlagbits()));
		
			
			lastTimeStamp = xmitTime = dcpMsg.getXmitTime();
			if (xmitTime == null)
			{
				error(rawMsg, "DcpMonitorConsumer: rawMsg with no time stamp ignored. " +
					"Medium ID=" + rawMsg.getMediumId());
				return;
			}
			
			DcpAddress dcpAddress = dcpMsg.getDcpAddress();
			failureCode = dcpMsg.getFailureCode();
			XRWriteThread xrWriteThread = DcpMonitor.instance().getXrWriteThread();
			DcpMsg existingMsg = xrWriteThread.find(dcpMsg, xmitTime);
			
			Action action = Action.Ignore;
			switch(failureCode)
			{
			case (char)0: // shouldn't happen. It's a bug if it does.
				error(rawMsg, "DcpMonitorConsumer: rawMsg with no Failure Code ignored. " +
					"Medium ID=" + rawMsg.getMediumId());
				action = Action.Ignore;
				break;
			case 'M':
				// Ignore MISSING notifications.
				logAction(action = Action.Ignore, dcpAddress, xmitTime, "failureCode='M'.");
				break;
			case 'G':
				if (existingMsg != null)
				{
					if (existingMsg.getFailureCode() == '?')
						logAction(action = Action.ReplaceExisting, 
							dcpAddress, xmitTime, "new message has failure code 'G'"
							+ " and existing one is '?'.");
					else // failure code should be 'G'
						logAction(action = Action.Ignore, 
							dcpAddress, xmitTime, "new message has failure code 'G'"
							+ " but an existing 'G' is already stored.");
				}
				else
					logAction(action = Action.Save, 
						dcpAddress, xmitTime, "this msg failureCode='G'. No existing msg stored.");
				break;
			case '?':
				if (existingMsg != null)
				{
					logAction(action = Action.Ignore, dcpAddress, xmitTime, 
						"new message has failure code '?'"
						+ " but an existing msg is already stored with failure code "
						+ failureCode);
					action = Action.Ignore;
				}
				else
					logAction(action = Action.Save, 
						dcpAddress, xmitTime, "this msg failureCode='?'. No existing msg stored.");
				break;
			default: // Must by some type of status code
				if (existingMsg == null)
				{
					logAction(action = Action.Ignore, dcpAddress, xmitTime, 
						" received status code '" + failureCode
						+ "' but no existing message is stored.");
				}
				else
				{
					existingMsg.addXmitFailureCode(failureCode);
					logAction(action = Action.ModifyExisting, dcpAddress, xmitTime, 
						" added status code '" + failureCode
						+ "'.");
				}
			}
			
			if (action == Action.Ignore)
				return;
			else if (action == Action.ModifyExisting)
			{
				if (dcpMsg.getRecordId().isNull())
				{
					// This would mean that existing message is still in the
					// queue waiting to be written, so we don't have to do anything.
				}
				else // it was already written to the DB. Just re-enqueue it.
					xrWriteThread.enqueue(existingMsg);
				return;
			}
			
			// Get platform info from PDT or DECODES DB.
			dcpMsg.setBattVolt(getBattVolt(msg));

			// Validator will set XmitWindow if it can.
			if (dcpMsg != null)
				msgValidator.validateMsg(dcpMsg, null, new Date());
			
			if (action == Action.ReplaceExisting)
			{
				if (xrWriteThread.replace(existingMsg, dcpMsg))
					return;
				// Already in DB, have to replace it there. Set record ID to
				// existing message's ID so that DAO will do an update.
				dcpMsg.setRecordId(existingMsg.getRecordId());
			}
			// else action == Save
			
			// If queue full (probably due to backlog during startup), keep trying
			// for up to 2 minutes.
			long endTry = System.currentTimeMillis() + 120000L;
			while(System.currentTimeMillis() < endTry)
			{
				if (xrWriteThread.enqueue(dcpMsg))
					return;
				Logger.instance().warning(
					"Queue full, retrieval thread will pause until "
					+ debugSdf.format(new Date(endTry)));
				try { Thread.sleep(2000L); }
				catch(InterruptedException ex) {}
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
	}

	private void error(RawMessage rawmsg, String reason)
	{
		Logger.instance().warning("DCPMon Input problem: '" 
			+ (new String(rawmsg.getHeader())) + "': " + reason);
	}
	
	private void logAction(Action action, DcpAddress dcpAddress, Date xmitTime, String reason)
	{
		if (Logger.instance().getMinLogPriority() == Logger.E_DEBUG3)
			Logger.instance().debug3(module + " " + action + " message for "
				+ dcpAddress + " at time " + debugSdf.format(xmitTime)
				+ " because " + reason);
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
			// Take the most recent sample that's not flagged as missing and is in proper range.
			batt_ts.sort();
			for(int idx = batt_ts.size()-1; idx >= 0; idx--)
			{
				try
				{
					TimedVariable tv = batt_ts.sampleAt(idx);
					if ((tv.getFlags() & (IFlags.IS_ERROR|IFlags.IS_MISSING)) != 0)
						continue;
					double bv = tv.getDoubleValue();
					if (bv > 0.0 && bv < 20.)
						return (float)bv;
				}
				catch(ilex.var.NoConversionException ex) {}
			}
		}
		// Either couldn't find BV or it was in a bad format.
		return (float)0.0;
	}

	@Override
	public OutputStream getOutputStream()
		throws DataConsumerException
	{
			return null;
	}

	@Override
	public void useValidationResults(char failureCode, String explanation,
			DcpMsg msg, LrgsInputInterface src, Date msgTimeStamp, 
			PdtEntry pdtEntry)
	{
		// The only validation code we use is 'V'. All others are supplied
		// by the LRGS and fielded above as DAPS status messages.
		if (failureCode == 'V')
			msg.addXmitFailureCode(failureCode);
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
}

