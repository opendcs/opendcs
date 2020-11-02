package decodes.snotel;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Properties;
import java.util.TimeZone;

import decodes.consumer.DataConsumer;
import decodes.consumer.DataConsumerException;
import decodes.consumer.OutputFormatter;
import decodes.consumer.OutputFormatterException;
import decodes.db.Constants;
import decodes.db.NetworkList;
import decodes.db.NetworkListEntry;
import decodes.db.PresentationGroup;
import decodes.decoder.DataOperations;
import decodes.decoder.DecodedMessage;
import decodes.decoder.EndOfDataException;
import decodes.decoder.FieldParseException;
import decodes.decoder.NumberParser;
import decodes.decoder.ScriptException;
import decodes.decoder.ScriptFormatException;
import decodes.util.DecodesSettings;
import decodes.util.PropertySpec;
import ilex.util.EnvExpander;
import ilex.var.NoConversionException;
import ilex.var.Variable;
import lrgs.common.DcpMsg;

public class SnotelOutputFormatter extends OutputFormatter
{
	private long lastLoadFileMsec = 0L;
	public static final String module = "SnotelOutputFormatter";
	private File specFile = null;
	private SnotelPlatformSpecList specList = new SnotelPlatformSpecList();
	private NetworkList netlist = new NetworkList();
	private TimeZone outTz = null;
	private int bufferTimeSec = 0;
	private DataConsumer theConsumer = null;
	private DecodedMessage lastMsg = null;

	public SnotelOutputFormatter()
	{
		ofPropSpecs = new PropertySpec[]
		{
			new PropertySpec("snotelSpecFile", PropertySpec.FILENAME, 
				"Path of file containing SNOTEL Platform Specifications."),
			new PropertySpec("bufferTimeSec", PropertySpec.INT,
				"Max # of seconds to wait before outputting data (used to reduce "
				+ "# of files for directory consumer.)")
		};

	}

	/**
	 * The formatter will do it's own decoding so it doesn't require a
	 * decoded message to be passed.
	 */
	@Override
	public boolean requiresDecodedMessage()
	{
		return false;
	}


	@Override
	protected void initFormatter(String type, TimeZone tz, 
		PresentationGroup presGrp, Properties rsProps)
		throws OutputFormatterException
	{
		if (tz == null)
		{
			logger.info("No timezone specified. Will use PST.");
			tz = TimeZone.getTimeZone("GMT-08:00");
		}
		this.outTz = tz;
		
		// Find the spec file.
		String s = rsProps.getProperty("snotelSpecFile");
		if (s == null)
		{
			s = DecodesSettings.instance().snotelSpecFile;
			if (s == null)
				throw new OutputFormatterException(module 
					+ " Missing required snotelSpecFile property.");
		}
		specFile = new File(EnvExpander.expand(s));
		if (!specFile.exists())
		{
			specFile = new File(EnvExpander.expand("$DCSTOOL_USERDIR"), s);
			if (!specFile.exists())
				throw new OutputFormatterException(module +
					" Cannot find spec file '" + s + "' -- set property to full path "
					+ "or place file in $DCSTOOL_USERDIR");
		}
		if (!specFile.canRead())
			throw new OutputFormatterException(module +
				" spec file '" + specFile.getPath() + "' exists but is not readable. "
				+ "Check Permissions on file.");
		try
		{
			// Create a dummy netlist in the routing spec to be loaded from spec file.
			if (rsThread != null)
			{
				netlist.name = rsThread.getRoutingSpec().getName() + "-netlist";
				netlist.transportMediumType = Constants.medium_Goes;
				rsThread.getRoutingSpec().addNetworkListName(netlist.name);
				rsThread.getRoutingSpec().networkLists.add(netlist);
			}			
			// Load the spec, will populate the network list.
			loadPlatformSpecs();
		}
		catch(IOException ex)
		{
			String msg = module + " loadPlatformSpecs failed: " + ex;
			logger.failure(msg);
			throw new OutputFormatterException(msg);
		}
		
		bufferTimeSec = 0;
		s = rsProps.getProperty("bufferTimeSec");
		if (s != null)
		{
			try { bufferTimeSec = Integer.parseInt(s.trim()); }
			catch(Exception ex)
			{
				logger.warning("Invalid bufferTimeSec property '" + s + "' ignored. Buffering disabled.");
				bufferTimeSec = 0;
			}
		}
		
	}

	private void loadPlatformSpecs()
		throws IOException
	{
		specList.loadFile(specFile, logger);
		lastLoadFileMsec = System.currentTimeMillis();
		Collection<SnotelPlatformSpec> specs = specList.getPlatformSpecs();
		logger.info(module + " after reading spec file '" + specFile.getPath()
			+ "' there are " + specs.size() + " Snotel Platform Specs");
		
		// build the network list and add it to routing spec
		netlist.clear();
		for(SnotelPlatformSpec spec : specs)
		{
			NetworkListEntry nle = new NetworkListEntry(netlist, 
				spec.getDcpAddress().toString());
			nle.setPlatformName(""+spec.getStationId());
			nle.setDescription(spec.getStationName());
			netlist.addEntry(nle);
		}
		
		// This tells routing spec thread to re-initialize the data source
		// with the new network list.
		if (rsThread != null)
			rsThread.forceReInit();
	}

	@Override
	public void shutdown()
	{
		if (bufferTimeSec <= 0)
			return;
		
		// If any data is accumulated in the buffer, flush it now.
		try
		{
			this.flushBuffer();
		}
		catch (DataConsumerException ex)
		{
			logger.warning("Error shutting down formatter: " + ex);
		}
	}

	@Override
	public void formatMessage(DecodedMessage msg, DataConsumer consumer)
			throws DataConsumerException, OutputFormatterException
	{
		theConsumer = consumer;
		lastMsg = msg;
		
		if (specFile.lastModified() > lastLoadFileMsec)
		{
			try { loadPlatformSpecs(); }
			catch(IOException ex)
			{
				String emsg = module + " Cannot load spec file: " + ex;
				logger.failure(emsg);
				throw new OutputFormatterException(emsg);
			}
		}
		
		DcpMsg dcpMsg = msg.getRawMessage().getOrigDcpMsg();
		if (dcpMsg == null)
			throw new OutputFormatterException(module + " requires raw DcpMsg object.");
		
		SnotelPlatformSpec spec = specList.getPlatformSpec(dcpMsg.getDcpAddress());
		if (spec == null)
			throw new OutputFormatterException(module + " no SnotelPlatformSpec for "
				+ " DCP Address: " + dcpMsg.getDcpAddress());
		
		
		DataOperations dops = new DataOperations(msg.getRawMessage());
		
		// Initialize calendar with the message's transmit time and routing spec's TZ.
		Date xmitTime = dcpMsg.getXmitTime();
		Calendar cal = Calendar.getInstance();
		cal.setTimeZone(outTz);
		cal.setTime(xmitTime);
		StringBuilder sb = new StringBuilder();
		SimpleDateFormat dateSdf = new SimpleDateFormat("MM/dd/yyyy");
		dateSdf.setTimeZone(outTz);
		SimpleDateFormat timeSdf = new SimpleDateFormat("HH:mm:ss");
		timeSdf.setTimeZone(outTz);
		SimpleDateFormat hhmmSdf = new SimpleDateFormat("HHmm");
		hhmmSdf.setTimeZone(outTz);
		
		boolean msgJan1 = cal.get(Calendar.DAY_OF_YEAR) == 1;
		NumberFormat numFmt = NumberFormat.getNumberInstance();
		numFmt.setGroupingUsed(false);
		numFmt.setMaximumFractionDigits(3);

		if (bufferTimeSec <= 0)
			theConsumer.startMessage(lastMsg);

		boolean isPB = spec.getDataFormat() == 'B' || spec.getDataFormat() == 'b';
		
		try
		{
			dops.forwardspace(); // Skip initial DADDS status char in msg.
			NumberParser numParser = new NumberParser();
			numParser.setDataType(isPB ? NumberParser.CAMPBELL_BINARY_FMT :
				NumberParser.ASCII_FMT);
			
			for(int hr = 0; hr < spec.getNumHours(); hr++)
			{
				sb.setLength(0);
				
				if (isPB)
				{
					// Pseudobinary includes sample time in first 2 PB fields.
					byte[] doyField = dops.getField(3, null);
					byte[] hhmmField = dops.getField(3, null);
					
					// Decode the time into a fresh calendar object
					try
					{
						int doy = numParser.parseIntValue(doyField);
						cal.set(Calendar.DAY_OF_YEAR, doy);
						int hhmm = numParser.parseIntValue(hhmmField);
						cal.set(Calendar.HOUR_OF_DAY, hhmm/100);
						cal.set(Calendar.MINUTE, hhmm % 100);
						logger.debug3(module + " parsed doy=" + doy + ", hhmm=" + hhmm
							+ ", resulting time=" + cal.getTime());
						
						// Handle case where msg received on Jan 1 and the 
						// day-of-year parsed is Dec 31. Subtract 1 from year.
						if (msgJan1
						 && cal.get(Calendar.MONTH) == Calendar.DECEMBER
						 && cal.get(Calendar.DAY_OF_MONTH) == 31)
							cal.add(Calendar.YEAR, -1);
					}
					catch(FieldParseException ex)
					{
						String emsg = module + " invalid time fields doy='" 
							+ new String(doyField) + "', hhmm='" + new String(hhmmField);
						logger.warning(emsg);
						throw new OutputFormatterException(emsg);
					}
				}
				else // ASCII does not include time. Truncate seconds and minutes in xmit time.
				{
					cal.set(Calendar.SECOND, 0);
					cal.set(Calendar.MINUTE, 0);
					dops.skipWhiteSpace(); // ASCII msg begins with CR LF.
				}
				
				sb.append(
					        dateSdf.format(dcpMsg.getXmitTime())
					+ "," + timeSdf.format(dcpMsg.getXmitTime())
					+ "," + spec.getStationId()
					+ "," + dateSdf.format(cal.getTime())
					+ "," + hhmmSdf.format(cal.getTime()));
		
				int chan = 0;
				for (chan = 0; chan < spec.getNumChannels(); chan++)
				{
					// Decode a data value.
					byte[] dataField = new byte[0];
					try
					{
						if (isPB)
							dataField = dops.getField(3, null);
						else
						{
							dataField = dops.getField(10, ",", false, false);
							dops.forwardspace(); // gobble the comma delimiter
						}
						Variable v = numParser.parseDataValue(dataField);
						
						sb.append("," + numFmt.format(v.getDoubleValue()));
					}
					catch (FieldParseException e)
					{
						logger.warning(module + " hour " + hr + " chan " + chan
							+ " bad data field '" + new String(dataField) + "' -- skipped.");
					}
					catch(EndOfDataException ex)
					{
						// ran out of message data. Break and print what we have.
						break;
					}
					catch (NoConversionException e)
					{
						logger.warning(module + " cannot represent value as a number: " + e
							+ " field='" + new String(dataField) + "' -- skipped.");
					}
				}
				String line = sb.toString();
logger.debug3(module + " msgTime=" + dcpMsg.getXmitTime() + ", tz=" 
+ outTz.getID() + ", #chans=" + chan + ", line='" + line + "'");

				if (bufferTimeSec <= 0)
					consumer.println(line);
				else
					addToBuffer(spec.getStationId(), line);
			}
		}
		catch(EndOfDataException ex)
		{
			logger.warning(module + " bad message hour - missing time fields: '" 
				+ new String(dcpMsg.getData()) + "' -- skipped.");
		}
		catch (ScriptFormatException e)
		{
			// Shouldn't happen. There is no script.
			e.printStackTrace();
		}
		
		if (bufferTimeSec <= 0)
			theConsumer.endMessage();
		else if (bufferingStarted > 0L
			&& (System.currentTimeMillis() - bufferingStarted)/1000L > bufferTimeSec)
			flushBuffer();
	}
	
	class SnotelPlatformData
	{
		int platformId = 0;
		ArrayList<String> csvData = new ArrayList<String>();

		public SnotelPlatformData(int platformId)
		{
			super();
			this.platformId = platformId;
		}
		
		public void add(String line)
		{
			csvData.add(line);
		}
	}
	
	ArrayList<SnotelPlatformData> buffer = new ArrayList<SnotelPlatformData>();
	long bufferingStarted = -1L;
	
	private SnotelPlatformData getBufferFor(int platformId)
	{
		for(SnotelPlatformData ret : buffer)
			if (platformId == ret.platformId)
				return ret;
		SnotelPlatformData ret = new SnotelPlatformData(platformId);
		buffer.add(ret);
		return ret;
	}
	
	private void addToBuffer(int platformId, String csvLine)
	{
logger.debug1(module + " addToBuffer(" + platformId + ", " + csvLine + ") buffer.size=" + buffer.size());
		if (bufferingStarted == -1L)
			bufferingStarted = System.currentTimeMillis();
		getBufferFor(platformId).add(csvLine);
	}
	
	private void flushBuffer()
		throws DataConsumerException
	{
logger.debug1(module + " flushBuffer() buffer size = " + buffer.size());
		if (buffer.size() == 0)
			return;
		theConsumer.startMessage(lastMsg);

		for(SnotelPlatformData spd : buffer)
		{
			for(String line : spd.csvData)
				theConsumer.println(line);
		}
		
		theConsumer.endMessage();
		buffer.clear();
		bufferingStarted = -1L;
	}
	
	@Override
	public void dataSourceCaughtUp(boolean dataSourceEnd)
	{
		logger.debug1(module + " dataSourceCaughtUp(" + dataSourceEnd + ") buffer size=" + buffer.size());
		try
		{
			flushBuffer();
		}
		catch (DataConsumerException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	
}