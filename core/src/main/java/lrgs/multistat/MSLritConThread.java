//package lrgs.multistat;
//
//import ilex.net.BasicClient;
//import ilex.util.Logger;
//import ilex.util.PropertiesUtil;
//import ilex.xml.XmlOutputStream;
//
//import java.io.BufferedReader;
//import java.io.ByteArrayInputStream;
//import java.io.ByteArrayOutputStream;
//import java.io.IOException;
//import java.io.InputStreamReader;
//import java.io.PrintWriter;
//import java.util.Calendar;
//import java.util.Date;
//import java.util.Properties;
//import java.util.TimeZone;
//import java.util.Vector;
//
//import javax.swing.SwingUtilities;
//
//import lrgs.common.SearchCriteria;
//import lrgs.common.SearchSyntaxException;
//import lrgs.rtstat.RtStatPanel;
//import lritdcs.LritDcsConfig;
//import lritdcs.LritDcsStatus;
//import lritdcs.LritReportGenerator;
//import lritdcs.StatusInvalidException;
//
//public class MSLritConThread extends Thread
//{
//	boolean shutdown;
//	BasicClient lritcon;
//	static final String eventsPoll = "EVENTS";
//	static final String statusPoll = "STATUS";
//	static final String putConfigReq = "PUTCONFIG";
//	static final String getConfigReq = "GETCONFIG";
//	static final String getCritReq = "GETCRIT";
//	static final String putCritReq = "PUTCRIT";
//	static final String lastRetrieval = "LASTRETRIEVAL";
//	private BufferedReader reader;
//	private PrintWriter writer;
//	private String cmd = null;
//	private RtStatPanel rtStatPanel;
//	private LritSummaryStatPanel summaryStatPanel;
//	private MultiStatFrame parent;
//	private LritReportGenerator repgen;
//	private XmlOutputStream xos;
//	private ByteArrayOutputStream htmlOS;
//	public LritDcsStatus currentStatus;
//	public String lritHostName;
//	public boolean configChanged = false;
//
//	//SearchCriteria scHigh;
//	//SearchCriteria scMedium;
//	//SearchCriteria scLow;
//	//SearchCriteria scManual;
//	boolean sendHigh = false;
//	boolean sendMedium = false;
//	boolean sendLow = false;
//	boolean sendManual = false;
//
//	SearchCriteria scHigh2Send = null;
//	SearchCriteria scMedium2Send = null;
//	SearchCriteria scLow2Send = null;
//	SearchCriteria scManual2Send = null;
//
//	public String lritHost;
//	public int lritPort;
//	public String lritDisplayName;
//
//	public boolean backUp;
//	public Properties props = new Properties();
//
//
//	public MSLritConThread(MultiStatFrame parent, RtStatPanel rtStatPanel, 
//			LritSummaryStatPanel summaryStatPanel)
//	{
//		super("MSLritConThread");
//		this.parent = parent;
//		this.rtStatPanel = rtStatPanel;
//		this.summaryStatPanel = summaryStatPanel;
//		currentStatus = new LritDcsStatus("currentLritStatus");
//		lritHostName = "";
//		htmlOS = new ByteArrayOutputStream(32000);
//		xos = new XmlOutputStream(htmlOS, "html");
//		repgen = new LritReportGenerator();
//		configChanged = false;
//
//
//	}
//
//	public void setLritInfo(String strLritHost, int strLritPort, String strLritDisplayname, boolean blnbackUp)
//	{
//
//
//		this.lritHost = strLritHost;
//		this.lritPort  = strLritPort;
//		this.lritDisplayName = strLritDisplayname;
//		this.backUp = blnbackUp;
//	}
//
//	public void run()
//	{		
//		MultiStatConfig cfg = MultiStatConfig.instance();
//		shutdown = false;		
//		//System.out.println(this.lritHost +":"+ this.lritPort);
//		lritcon = new BasicClient(this.lritHost, this.lritPort);
//
//		long lastConfigPoll = 0L;
//
//		//long now = System.currentTimeMillis();
//		while(!shutdown)
//		{				
//			//System.out.println("is lrit connected thread !!!"+  this.lritHost+":"+lritcon.isConnected());
//			if (!lritcon.isConnected() && !tryConnect())
//			{		
//
//
//				emptyStatus();
//				try { sleep(5000L); }
//				catch(Exception ex2) {
//
//					ex2.printStackTrace();
//				}
//				continue;
//			} 
//
//			try
//			{
//
//				long now = System.currentTimeMillis();
//				//if (now - lastConfigPoll > 60000L)
//				if (now - lastConfigPoll > 5000L)
//				{
//
//					lastConfigPoll = now;
//					
//					if(backUp)
//					{
//						if(!parent.dlgbackup.isShowing())
//						parent.dlgbackup.fillConfigValues(getConfig());
//						else
//						getConfig();
//					}
//					else
//					{
//						if(!parent.dlg.isShowing())
//						parent.dlg.fillConfigValues(getConfig());
//						else
//						getConfig();
//					}
//
//					if (parent.scDlg == null)
//					{
//						getCrit("high", parent.scHigh);
//						getCrit("medium", parent.scMedium);
//						getCrit("low", parent.scLow);
//						getCrit("manual", parent.scManual);
//					}
//				}
//
//				if (configChanged)
//				{					
//					sendConfig();
//					configChanged = false;
//				}
//
//
//				if (scHigh2Send != null)
//				{
//					putCrit("high", scHigh2Send);
//					scHigh2Send = null;
//				}
//				if (scMedium2Send != null)
//				{
//					putCrit("medium", scMedium2Send);
//					scMedium2Send = null;
//				}
//				if (scLow2Send != null)
//				{
//					putCrit("low", scLow2Send);
//					scLow2Send = null;
//				}
//				if (scManual2Send != null)
//				{
//					putCrit("manual", scManual2Send);
//					scManual2Send = null;
//				}
//
//				if (cmd != null)
//					sendCommand();
//
//				pollForStatus();
//				pollForEvents();
//
//				try { sleep(1000L); }
//				catch(InterruptedException ex2) {
//					ex2.printStackTrace();
//				}
//			}
//			catch(Exception ex)
//			{
//				ex.printStackTrace();
//				String msg = "Error on UI LRIT connection to '"
//					+ lritcon.getName() + "': " + ex;
//				Logger.instance().failure(
//						MultiStat.module + ":" 
//						+ MultiStat.EVT_CANT_CONNECT_LRIT + " " + msg);
//				try
//				{
//					reader.close();
//					writer.close();
//				}
//				catch(Exception ex3) {					
//				}
//				lritcon.disconnect();
//				setStatus("Connect Error", true, false);
//				try { sleep(10000L); }
//				catch(InterruptedException ex2) {
//					ex2.printStackTrace();
//				}
//			}
//		}
//	}
//
//	private boolean tryConnect()
//	{
//
//		//System.out.println("connecting ...."+lritDisplayName);
//		//String dn = MultiStatConfig.instance().LritDisplayName;
//		String dn = this.lritDisplayName;
//		if (dn == null || dn.length() == 0)
//			return false;
//
//		setStatus("Connecting", false, true);
//		try
//		{  
//			lritcon.connect();
//			reader = new BufferedReader(new InputStreamReader(
//					lritcon.getInputStream()));
//			writer = new PrintWriter(lritcon.getOutputStream(), true);
//			// System.out.println("Connected!" + dn);
//			setStatus("Connected", false, false);
//			return true;
//		}
//		catch(Exception ex)
//		{
//			setStatus("Connect Error", true, false);
//			String msg = "Can't connect to LRIT at "
//				+ lritcon.getName() + ": " + ex;
//			Logger.instance().failure(
//					MultiStat.module + ":" 
//					+ MultiStat.EVT_CANT_CONNECT_LRIT + " " + msg);
//
//			return false;
//		}
//	}
//
//	private void emptyStatus()
//
//	{
//
//		currentStatus.clear();		
//		SwingUtilities.invokeLater(
//				new Runnable()
//				{
//					public void run()
//					{
//						int n = parent.alarmList.countAlarmsForSource("LRIT");
//						summaryStatPanel.update(currentStatus, n);
//						//lritHostName = MultiStatConfig.instance().LritHostName;
//
//
//						lritHostName = lritHost;						
//						try
//						{
//							htmlOS.reset();
//							repgen.writeReport(xos, lritHostName,
//									currentStatus, 0);
//							String report = htmlOS.toString();
//							rtStatPanel.updateStatus(report);
//						}
//						catch(IOException ex)
//						{
//							Logger.instance().warning(
//									"Unexpected IO exception writing LRIT report: "
//									+ ex);
//						}
//					}
//				});		
//
//	}
//
//
//
//	private void pollForStatus()
//	throws IOException
//	{
//		writer.println(statusPoll);
//		ByteArrayOutputStream statusBufOS = new ByteArrayOutputStream();
//		byte nl[] = new byte[] { (byte)'\n' };
//		while(true)
//		{
//			String line = reader.readLine();
//			if (line == null)
//			{
//				return;
//			}
//			if (line.trim().length() == 0)
//				break;
//
//			statusBufOS.write(line.getBytes());
//			statusBufOS.write(nl);
//		}
//		statusBufOS.close();
//
//		Properties props = new Properties();
//		ByteArrayInputStream bais = new ByteArrayInputStream(
//				statusBufOS.toByteArray());
//		props.load(bais);
//		bais.close();
//
//		try 
//		{
//			currentStatus.loadFromProps(props); 
//			
//			if(currentStatus.status.equalsIgnoreCase("Running"))
//			
//			{	
//				this.parent.lastRetrieval = currentStatus.lastRetrieval;
//			}
//
//			SwingUtilities.invokeLater(
//					new Runnable()
//					{
//						public void run()
//						{
//
//							int n = parent.alarmList.countAlarmsForSource("LRIT");
//							summaryStatPanel.update(currentStatus, n);
//							//lritHostName = MultiStatConfig.instance().LritHostName;
//							lritHostName = lritHost;
//
//							try
//							{
//								htmlOS.reset();
//								repgen.writeReport(xos, lritHostName,
//										currentStatus, 0);
//								String report = htmlOS.toString();
//								rtStatPanel.updateStatus(report);
//							}
//							catch(IOException ex)
//							{
//								Logger.instance().warning(
//										"Unexpected IO exception writing LRIT report: "
//										+ ex);
//							}
//						}
//					});
//		}
//		catch(StatusInvalidException ex)
//		{
//			System.err.println("Invalid LRIT status received & ignored.");
//			setStatus("Invalid Status", false, true);
//		}
//	}
//
//
//
//
//	private void pollForEvents()
//	throws IOException
//	{
//		writer.println(eventsPoll);
//		final Vector events = new Vector();
//		while(true)
//		{
//			String line = reader.readLine();
//			if (line == null)
//				break;
//			line = line.trim();
//			if (line.length() == 0)
//				break;
//			events.add(line);
//		}
//		SwingUtilities.invokeLater(
//				new Runnable()
//				{
//					public void run()
//					{
//						for(int i=0; i<events.size(); i++)
//							parent.addEvent((String)events.get(i), lritHostName);
//					}
//				});
//	}
//
//	private void sendConfig()
//	throws IOException
//	{
//		sendLastRetreivalTime();
//		
//		//System.out.println("Sending LRIT Config");
//		configChanged = false;
//		writer.println(putConfigReq);
//		//LritDcsConfig.instance().save(writer);
//
//		if(backUp)
//			LritDcsConfig.instance().saveConfig(writer, parent.dlgbackup.getConfigProps());
//		else
//			LritDcsConfig.instance().saveConfig(writer, parent.dlg.getConfigProps());
//
//		writer.println();
//	}
//
//	/**
//	 * 
//	 * Sends the last retreival time to the LRIT application
//	 * @throws IOException
//	 */
//	private void sendLastRetreivalTime()
//	throws IOException
//	{
//		
//		writer.println(lastRetrieval);		
//		writer.println(String.valueOf(this.parent.lastRetrieval));	
//	}
//	/*private void getConfig()
//		throws IOException
//	{
//System.out.println("Requesting LRIT Config");
//
//
//		writer.println(getConfigReq);
//
//		byte cfgbuf[] = captureToBlankLine();
//		Properties props = new Properties();
//		ByteArrayInputStream bais = new ByteArrayInputStream(cfgbuf);
//		props.load(bais);
//
//		PropertiesUtil.loadFromProps(LritDcsConfig.instance(), props);
//	}*/
//
//
//	public Properties getConfig()
//	throws IOException
//	{
//	//	 props = new Properties();
//		if(writer!=null)
//		{
//			writer.println(getConfigReq);
//
//			byte cfgbuf[] = captureToBlankLine();
//
//			ByteArrayInputStream bais = new ByteArrayInputStream(cfgbuf);
//		
//			//System.out.println("get config");
//			this.props.load(bais);
//			//System.out.println("values in thread##### are ::::" +props.toString());
//			PropertiesUtil.loadFromProps(LritDcsConfig.instance(), this.props);
//		}
//		else		
//		{
//			//System.out.println("no load");
//			LritDcsConfig.makeInstanceNoLoad();
//		}
//		
//		//System.out.println("values in thread are ::::" +props.getProperty("dom2AUser"));
//		return props;
//
//	}
//
//
//	private void getCrit(String what, SearchCriteria sc)
//	throws IOException
//	{
//		//System.out.println("Getting LRIT SearchCrit " + what);
//		writer.println(getCritReq + " " + what);
//		byte buf[] = captureTo("ENDSC");
//		ByteArrayInputStream bais = new ByteArrayInputStream(buf);
//		sc.clear();
//		try { sc.parseFile(new InputStreamReader(bais)); }
//		catch(SearchSyntaxException ssex)
//		{
//			Logger.instance().warning("Server returned invalid '" + what 
//					+ "' search criteria: " + ssex);
//		}
//		bais.close();
//	}
//
//	private void putCrit(String what, SearchCriteria sc)
//	throws IOException
//	{
//		//System.out.println("Putting LRIT SearchCrit " + what);
//		//System.out.println("putting ......"+sc.toString());
//		writer.println(putCritReq + " " + what);
//		writer.println(sc.toString());
//	}
//
//	/// Sends a simple command and expects no response.
//	private void sendCommand()
//	throws IOException
//	{		
//		if (cmd != null)
//			writer.println(cmd);
//		cmd = null;
//	}
//
//	private byte[] captureToBlankLine()
//	throws IOException
//	{
//		//System.out.println("Capturing to blank line.");
//		ByteArrayOutputStream bufOS = new ByteArrayOutputStream();
//		byte nl[] = new byte[] { (byte)'\n' };
//		while(true)
//		{
//			String line = reader.readLine();
//			if (line == null)
//				throw new IOException("Socket closed by LRIT Sender");
//			line = line.trim();
//			if (line.length() == 0)
//				break;
//
//			bufOS.write(line.getBytes());
//			bufOS.write(nl);
//		}
//		bufOS.close();
//		//System.out.println("Capture to blank line done.");
//		return bufOS.toByteArray();
//	}
//
//	private byte[] captureTo(String endTag)
//	throws IOException
//	{
//		ByteArrayOutputStream configBufOS = new ByteArrayOutputStream();
//		byte nl[] = new byte[] { (byte)'\n' };
//		while(true)
//		{
//			String line = reader.readLine();
//			if (line == null)
//				throw new IOException("Socket closed by LRIT Sender");
//			line = line.trim();
//			if (line.equals(endTag))
//				break;
//
//			configBufOS.write(line.getBytes());
//			configBufOS.write(nl);
//		}
//		configBufOS.close();
//		return configBufOS.toByteArray();
//	}
//
//	private void setStatus(final String st, 
//			final boolean isError, final boolean isWarning)
//	{
//		SwingUtilities.invokeLater(
//				new Runnable()
//				{
//					public void run()
//					{
//						summaryStatPanel.systemStatusField.setText(st);
//						if (isWarning)
//							summaryStatPanel.systemStatusField.setWarning();
//						else if (isError)
//							summaryStatPanel.systemStatusField.setError();
//						else
//							summaryStatPanel.systemStatusField.setOk();
//					}
//				});
//	}
//
//	/**
//	 * Put cmd in queue to be sent.
//	 * @return true if queued, false if busy.
//	 */
//	public boolean setCmd(String cmd)
//	{
//		if (this.cmd != null)
//			return false;
//		this.cmd = cmd;
//		return true;
//	}
//}
