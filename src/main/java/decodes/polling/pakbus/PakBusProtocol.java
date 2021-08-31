package decodes.polling.pakbus;

import ilex.util.EnvExpander;
import ilex.util.FileUtil;
import ilex.util.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

import com.campbellsci.pakbus.ColumnDef;
import com.campbellsci.pakbus.DataCollectClient;
import com.campbellsci.pakbus.DataCollectMode;
import com.campbellsci.pakbus.DataCollectModeDateToNewest;
import com.campbellsci.pakbus.DataCollectTran;
import com.campbellsci.pakbus.Datalogger;
import com.campbellsci.pakbus.GetTableDefsClient;
import com.campbellsci.pakbus.GetTableDefsTran;
import com.campbellsci.pakbus.LoggerDate;
import com.campbellsci.pakbus.Network;
import com.campbellsci.pakbus.Packet;
import com.campbellsci.pakbus.Record;
import com.campbellsci.pakbus.TransactionBase;
import com.campbellsci.pakbus.ValueBase;

import lrgs.common.DcpAddress;
import lrgs.common.DcpMsg;
import lrgs.common.DcpMsgFlag;
import decodes.db.TransportMedium;
import decodes.polling.IOPort;
import decodes.polling.LoggerProtocol;
import decodes.polling.LoginException;
import decodes.polling.PollingDataSource;
import decodes.polling.ProtocolException;
import decodes.util.DecodesSettings;
import decodes.tsdb.IntervalCodes;
import decodes.tsdb.IntervalIncrement;

/**
 * Implements the Campbell PakBus protocol for talking to remote logger.
 */
public class PakBusProtocol
	extends LoggerProtocol
	implements DataCollectClient, GetTableDefsClient
{
	public static String module = "PakBusProtocol";
	
	/** Use this ID to identify this server when connecting to remote logger. */
	public static final short svrPakBusId = (short)4079;
	
	/** The PakBus Network object */
	private Network pbNetwork = null;

	/** The PakBus Datalogger object */
	private Datalogger pbLogger = null;

	/** Used for timing the execution of transactions */
	private long transactionDurMs;
	
	/** Used to construct the message */
	private StringBuilder msgBuf = new StringBuilder();

	/** Set to true when the get-data transaction has completed */
	private boolean transComplete;
	
	/** Set by the callback with the outcome of the transaction. */
	private int transOutcome = -1;
	
	private String stationName = "unknown";
	
	private Exception abnormalShutdown = null;
	
	private Date sessionStart = null;

	public PakBusProtocol()
	{
		super();
	}
	
	@Override
	public void login(IOPort port, TransportMedium tm)
		throws LoginException
	{
		sessionStart = new Date();

		// The IOPort is already connected. Use it to create PakBus Network.
		pbNetwork = new Network(svrPakBusId, port.getIn(), port.getOut());

		// create the station
		int pakBusId = 1;
		String pbid = tm.platform.getProperty("pakBusId");
		if (pbid != null)
		{
			try { pakBusId = Integer.parseInt(pbid.trim()); }
			catch(Exception ex)
			{
				warning("Invalid pakBusId setting -- shoule be integer. Using default of 1.");
				pakBusId = 1;
			}
		}
		pbLogger = new Datalogger((short)pakBusId);
		int secCode = DecodesSettings.instance().pakBusSecurityCode;
		if (tm.platform != null)
		{
			stationName = tm.platform.getSiteName(false);
			String pv = tm.platform.getProperty("pakBusSecurityCode");
			if (pv != null)
			{
				try { secCode = Integer.parseInt(pv); }
				catch(Exception ex)
				{
					warning("Invalid pakBusSecurityCode property in "
						+ "platform. Must be integer. Ignored.");
				}
			}
		}
		pbLogger.set_security_code(secCode);
		pbNetwork.add_station(pbLogger);
		annotate("Session start for station " + stationName + " at " + sessionStart);
	}

	@Override
	public DcpMsg getData(IOPort port, TransportMedium tm, Date since)
		throws ProtocolException
	{
		if (!loadCachedTableDef(tm))
		{
			annotate("Requesting table defs from station " + stationName);
			runTransaction(new GetTableDefsTran(this));
			if (transOutcome != GetTableDefsTran.outcome_success)
			{
				String desc = transOutcome == -1 ? "Interrupted Transaction"
					: GetTableDefsTran.describe_outcome(transOutcome);
				String msg = "Cannot get table defs from station " + stationName + ": " + desc;
				warning(msg);
				throw new ProtocolException(msg);
			}
			saveTableDef();
		}

		msgBuf.setLength(0);
		
		// Determine what table name(s) to retrieve.
		String tableName = DecodesSettings.instance().pakBusTableName;
		String p = tm.platform == null ? null : tm.platform.getProperty("pakBusTableName");
		if (p != null)
			tableName = p;
		
		// Construct a LoggerDate represeting the since time using
		// the time zone specified in the transport medium.
		if (since == null)
			since = new Date(System.currentTimeMillis() - 3600000L * 4);
		Calendar cal = new GregorianCalendar();
		String tzid = tm.getTimeZone();
		if (tzid != null && tzid.trim().length() > 0)
			cal.setTimeZone(TimeZone.getTimeZone(tzid));
		// Otherwise cal will default to the local system time zone.
		cal.setTime(since);
		DataCollectMode collectMode = new DataCollectModeDateToNewest(new LoggerDate(cal));
		
		String msg = "Polling station " + stationName + " table " + tableName
			+ " for all data since " + since;
		debug1(msg);
		annotate(msg);
		
		Date dataPollTime = new Date();
		runTransaction(new DataCollectTran(tableName, this, collectMode));

		if (transOutcome != DataCollectTran.outcome_success)
		{
			String desc = transOutcome == -1 ? "Interrupted Transaction"
				: DataCollectTran.describe_outcome(transOutcome);

			msg = "Error polling station " + stationName + " for data: " + desc;
			warning(msg);
			throw new ProtocolException(msg);
		}
		
		// else outcome==success, build message and return.
		
		// Header times are always UTC.
		Date recvTime = new Date();
		SimpleDateFormat headerSdf = new SimpleDateFormat("yyMMdd HHmmss Z");
		headerSdf.setTimeZone(TimeZone.getTimeZone("UTC"));

		String header = 
			  "//STATION " + stationName + "\n"
			+ "//SOURCE " + tm.getLoggerType() + "\n"
			+ "//DEVICE END TIME " + headerSdf.format(dataPollTime) + "\n"
			+ "//POLL START " + headerSdf.format(sessionStart) + "\n"
			+ "//POLL STOP " + headerSdf.format(recvTime) + "\n";
		msgBuf.insert(0, header);

		byte[] msgdata = msgBuf.toString().getBytes();
		DcpMsg ret = new DcpMsg(msgdata, msgdata.length, 0);
		ret.setLocalReceiveTime(recvTime);
		ret.setXmitTime(recvTime);
		ret.setCarrierStart(sessionStart);
		ret.setCarrierStop(recvTime);
		ret.setDcpAddress(new DcpAddress(tm.getMediumId()));
		ret.setFailureCode('G');
		
		ret.setFlagbits(
			DcpMsgFlag.MSG_PRESENT
			| DcpMsgFlag.SRC_NETDCP
            | DcpMsgFlag.HAS_CARRIER_TIMES
			| DcpMsgFlag.MSG_TYPE_NETDCP
            | DcpMsgFlag.MSG_NO_SEQNUM);
		ret.setHeaderLength(msgdata.length - header.getBytes().length);
		
		annotate("DCP Message received: " + ret.getDataStr());
		debug1("getData() returning message.");

		return ret;
	}


	@Override
	public void goodbye(IOPort port, TransportMedium tm)
	{
		// No goodbye handshake needed in PakBus protocol
	}

	@Override
	public void setDataSource(PollingDataSource dataSource)
	{
		// No need to save reference to data source
	}

	@Override
	public void setAbnormalShutdown(Exception abnormalShutdown)
	{
		this.abnormalShutdown = abnormalShutdown;
	}
	
	/**
	 * Determine the pakbus ID to use to communicate with the remote logger.
	 * 
	 * @param tm
	 * @return
	 */
//	private int getPakBusID(TransportMedium tm)
//	{
//		int colon = tm.getMediumId().lastIndexOf(':');
//		if (colon == -1)
//			return 1;
//
//		if (tm.getMediumType().equalsIgnoreCase(decodes.db.Constants.medium_PolledTcp))
//		{
//			if (colon == tm.getMediumId().indexOf(':'))
//				// 1st colon is same as last colon means there is only 1. No ID specified.
//				return 1;
//		}
//
//		// An integer after the last colon contains the pakbus ID to use.
//		try { return Integer.parseInt(tm.getMediumId().substring(colon+1)); }
//		catch(Exception ex)
//		{
//			warning("Non-numeric pakbus ID after colon in '"
//				+ tm.getMediumId() + "' -- ignored.");
//			return 1;
//		}
//	}
	
	/**
	 * Load the stored table-definitions into the logger for this transport medium.
	 * @param tm the transport medium
	 * @return true if success, false if no cache file found.
	 */
	private boolean loadCachedTableDef(TransportMedium tm)
	{
		if (tm.platform == null)
		{
			debug1("Cannot retrieve cached table def because no Platform defined.");
			return false;
		}

		File tableDefDir = new File(EnvExpander.expand(DecodesSettings.instance().pakBusTableDefDir));
		File cf = new File(tableDefDir, stationName);
		if (!cf.canRead())
		{
			String msg = "Cached table def '" + cf.getPath() + "' doesn't exist or is not readable.";
			debug1(msg);
			annotate(msg);
			return false;
		}
		
		String maxAge = DecodesSettings.instance().pakBusMaxTableDefAge;
		if (maxAge != null && maxAge.trim().length() > 0)
		{
			IntervalIncrement ii = IntervalCodes.getIntervalCalIncr(maxAge);
//info("maxAge='" + maxAge + "' ii=" + ii);
			if (ii == null)
			{
				String msg = "Invalid pakBusMaxTableDefAge setting '" + maxAge + "' -- default to 48 hours";
				warning(msg);
				ii = new IntervalIncrement(Calendar.HOUR, 48);
			}
			long now = System.currentTimeMillis();
			if (now - cf.lastModified() > ii.toMsec())
			{
				String msg = "Cached table def '" + cf.getPath() + "' exists but is older than "
					+ maxAge;
				debug1(msg);
//debug1("\tlmt=" + cf.lastModified() + " - " + new Date(cf.lastModified()));
//debug1("\tii.toMsec=" + ii.toMsec());
//debug1("\tcurrent - lmt = " + (now - cf.lastModified()));
				annotate(msg);

				return false;
			}
		}
		
		// Else file exists and is recent enough to use.
		try
		{
			byte[] contents = FileUtil.getfileBytes(cf);
			Packet packet = new Packet();
			packet.add_bytes(contents, contents.length);
			pbLogger.set_raw_table_defs(packet);
			return true;
		}
		catch (IOException ex)
		{
			warning("IO Error reading cached table def file '" + cf.getPath() + "': " + ex);
			return false;
		}
	}

	/**
	 * Called after a successful GetTableDef transaction. Save the table defs in
	 * the cache file.
	 */
	private void saveTableDef()
	{
		File tableDefDir = new File(EnvExpander.expand(DecodesSettings.instance().pakBusTableDefDir));
		if (!tableDefDir.isDirectory())
		{
			debug1("Making table def dir '" + tableDefDir.getPath() + "'");
			tableDefDir.mkdirs();
		}
		File cf = new File(tableDefDir, stationName);
		FileOutputStream os = null;
		try
		{
			os = new FileOutputStream(cf);
			Packet packet = pbLogger.get_raw_table_defs();
			os.write(packet.get_storage(), 0, packet.get_storage_len());
			annotate("Saved table defs to " + cf.getPath());
		}
		catch (IOException ex)
		{
			warning("Cannot write to file '" + cf.getPath() + "' to cache table defs: " + ex);
		}
		finally
		{
			if (os != null)
				try { os.close(); } catch(Exception ex) {}
		}
	}



	/**
	 * PakBus callback when table defs transaction has completed.
	 * @param tran the transaction object
	 * @param outcome one of the symbolic constants defined in GetTableDefsTran 
	 * @throws Exception never
	 */
	@Override
	public void on_complete(GetTableDefsTran tran, int outcome)
		throws Exception
	{
		transComplete = true;
		transOutcome = outcome;
	}

	/**
	 * PakBus callback on completion of a DataCollectTran.
	 * @param tran the transaction
	 * @param outcome one of the symbolic constants defined in DataCollectTran
	 */
	@Override
	public void on_complete(DataCollectTran tran, int outcome) throws Exception
	{
		transComplete = true;
		transOutcome = outcome;
	}

	/**
	 * PakBus callback for each set of records received. May be called multiple
	 * times in a get-data transaction.
	 * Each call will format and store the received data into the msgBuf.
	 */
	@Override
	public boolean on_records(DataCollectTran tran, List<Record> records)
	{
		for (Record record : records)
		{
			msgBuf.append("TIMESTAMP: " + record.get_time_stamp().format("%y/%m/%d %H:%M:%S") + "\n");
			msgBuf.append("RECORD: "  + record.get_record_no() + "\n");
			for (ValueBase value : record.get_values())
			{
				ColumnDef columnDef = value.get_column_def();
				msgBuf.append(columnDef.name + ": " + value.format() + "\n");
			}
			msgBuf.append("\n"); // Blank line after each record.
		}

		return true;
	}
	
	private void runTransaction(TransactionBase trans)
	{
		long transactionStart = System.currentTimeMillis();
		transOutcome = -1;
		transComplete = false;
		int active_links = 0;
		String action = "add_transaction";
		try
		{
			pbLogger.add_transaction(trans);
			
			action = "check_state";
			while ((!transComplete || active_links > 0)
				&& abnormalShutdown == null)
			{
				active_links = pbNetwork.check_state();
				
				// Campbell recommends a 100ms pause in the loop.
				try { Thread.sleep(100); } catch (InterruptedException ex) {}
			}
		}
		catch (Exception ex)
		{
			warning("Exception running transaction " + trans.getClass().getName()
				+ " during " + action + ": " + ex);
			transOutcome = -1;
			
//			ex.printStackTrace();
		}
		if (abnormalShutdown != null)
		{
			String msg = "Transaction interrupted by abnormal shutdown: " + abnormalShutdown;
			warning(msg);
			transOutcome = -1;
		}
		transactionDurMs = System.currentTimeMillis() - transactionStart;
		transComplete = true;
	}
	
	private void warning(String msg)
	{
		if (pollingThread != null)
			pollingThread.warning(msg);
		else
			Logger.instance().warning(module + " " + msg);
		annotate("WARNING: " + msg);
	}
	
	private void debug1(String msg)
	{
		if (pollingThread != null)
			pollingThread.debug1(msg);
		else
			Logger.instance().debug1(module + " " + msg);
	}
	
	private void info(String msg)
	{
		if (pollingThread != null)
			pollingThread.info(msg);
		else
			Logger.instance().info(module + " " + msg);
	}

}
