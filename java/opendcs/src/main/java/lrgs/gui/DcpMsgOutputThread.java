/*
*  $Id$
*/
package lrgs.gui;

import java.io.OutputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.FileInputStream;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.TimeZone;
import java.io.StringWriter;
import java.io.PrintWriter;

import ilex.util.AsciiUtil;
import ilex.util.TextUtil;
import ilex.util.Logger;
import ilex.gui.GuiApp;

import lrgs.common.*;
import lrgs.ldds.*;

import decodes.consumer.OutputFormatter;
import decodes.db.Database;

/**
This works with DcpMsgOutputMonitor. This Thread will do the actual message
retrieval and then call the monitor to do something with the message and
to pace the output.

@see DcpMsgOutputMonitor
*/
class DcpMsgOutputThread extends Thread
{
	DcpMsgOutputMonitor parent;
	LddsClient client;
	OutputStream outs;
	int timeout;
	boolean keepGoing;
	String prefix, suffix;
	String beforeData, afterData;
	boolean doDecode;
	DecodesInterface decodesIf;
	OutputFormatter formatter = null;
	boolean decodesInitialized;
	boolean showRaw;
	boolean printDecodingErrorMessages = true;
	String outFormatName;
	boolean showMetaData = false;
	
	/**
	Constructor typically called from the parent.
	  @param parent the object that will monitor this thread's progress
	  @param client the LddsClient interface to get messages from
	  @param outs the OutputStream to write message data to.
	  @param timeout passed along to LddsClient
	  @param prefix String to display before every message
	  @param prefix String to display after every message
	*/
	public DcpMsgOutputThread(DcpMsgOutputMonitor parent, LddsClient client,
		OutputStream outs, int timeout, String prefix, String suffix)
	{
		this.parent = parent;
		this.client = client;
		this.outs = outs;
		this.timeout = timeout;
		this.prefix = (prefix != null && prefix.length() > 0) ? prefix : null;
		this.suffix = (suffix != null && suffix.length() > 0) ? suffix : null;
		this.beforeData = "";
		this.afterData = "";
		this.doDecode = false;
		this.showRaw = true;
		this.decodesInitialized = false;
		this.decodesIf = null;
		this.printDecodingErrorMessages = true;
	}

	/*  Disable decoding error messages -- used when data are sent
			to a file for uploading  - messages would interferr with ingest 
			programs.
	*/
	public void disableDecodingErrorMessages() {
		this.printDecodingErrorMessages = false;
	}
	/**
	  Thread run method gets messages from the LrgsClient interface, writes
	  them to the provided stream, and interfaces with the monitor to pace
	  the output.
	*/
	public void run()
	{
		long lastMsgReceived = System.currentTimeMillis();
		keepGoing = true;
	  mainLoop:
		while(keepGoing)
		{
			while(keepGoing && parent.dcpMsgOutputIsPaused())
			{
				try { sleep(100L); }
				catch(InterruptedException ie)
				{
					if (keepGoing == false)
						continue mainLoop;
				}
			}

			DcpMsg msg = null;
			String errmsg = null;
			boolean done = false;
			boolean timedOut = false;
			try
			{
				msg = client.getDcpMsg(timeout);
				if (msg != null)
				{
					lastMsgReceived = System.currentTimeMillis();

					if (showRaw || msg.isDapsStatusMsg())
					{
						if (prefix != null)
							outs.write(AsciiUtil.ascii2bin(prefix));
						if (showMetaData)
						{
							SimpleDateFormat sdf = new SimpleDateFormat(
								"yyyy-MMM-dd HH:mm:ss.SSS");
							sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
							StringBuilder md = new StringBuilder();
							DcpAddress addr = msg.getDcpAddress();
							if (addr != null)
								md.append("ID: " + addr + "\r\n");
							Date d = msg.getCarrierStart();
							if (d != null)
								md.append("Carrier Start: " + sdf.format(d) + "\r\n");
							d = msg.getCarrierStop();
							if (d != null)
								md.append("Carrier Stop: " + sdf.format(d) + "\r\n");
							d = msg.getLocalReceiveTime();
							if (d != null)
								md.append("Local Receive Time: " + sdf.format(d) + "\r\n");
							long x = msg.getMtmsm();
							if (x != 0)
								md.append("MTMSM: " + x + "\r\n");
							x = msg.getCdrReference();
							if (x != 0)
								md.append("Iridium CDR Reference: " + x + "\r\n");
							outs.write(md.toString().getBytes());
						}
						outs.write(msg.getData());
						if (suffix != null)
							outs.write(AsciiUtil.ascii2bin(suffix));
					}
					if (doDecode)
					{
						if (beforeData != null)
							outs.write(AsciiUtil.ascii2bin(beforeData));
						tryDecode(msg);
						if (afterData != null)
							outs.write(AsciiUtil.ascii2bin(afterData));
					}
					outs.flush();
				}
			}
			catch(InterruptedIOException ie)
			{
				parent.dcpMsgOutputError("Request Aborted");
			}
			catch(IOException ioe)
			{
				if (msg == null)
					parent.dcpMsgOutputError(
						"IO Error trying to get message from server: " + ioe);
				else
					parent.dcpMsgOutputError(
						"IO Error trying to save message to file: " + ioe);
				break;
			}
			catch(ProtocolError pe)
			{
				parent.dcpMsgOutputError(pe.toString());
				break;
			}
			catch(ServerError se)
			{
				if (se.Derrno == LrgsErrorCode.DUNTIL)
				{
					done = true;
					parent.dcpMsgOutputDone();
				}
				else if (se.Derrno == LrgsErrorCode.DMSGTIMEOUT)
				{
					long now = System.currentTimeMillis();
					long idleSec = (now - lastMsgReceived) / 1000L;
					if (idleSec > timeout)
					{
						parent.dcpMsgOutputError("No Msg in " + idleSec
							+ " seconds: " + se.toString());
						timedOut = true;
						break;
					}
					else // pause & stay in loop until true timeout occurs.
					{
						try { Thread.sleep(1000L); }
						catch(InterruptedException ie) {}
					}
				}
				else
				{
					parent.dcpMsgOutputError(se.toString());
					break;
				}
			}
			if (msg == null)
			{
				if (!done && timedOut )
					parent.dcpMsgTimeout();
			}
			else
				parent.dcpMsgOutputStatus(msg);
		}
	}

	/**
	  Called when application is exiting.
	*/
	public void cleanupAndDie()
	{
		keepGoing = false;
		this.interrupt();
		// Pause the calling thread to give running thread time
		// to cleanup.
		try { sleep(500L); }
		catch(InterruptedException ie) {}
	}


	/**
	  Called after each message is written to the output.
	  If decodes is initialized AND I can successfully decode this message,
	  print its decoded output to the output stream.
	  @param msg the raw DCP message
	*/
	private void tryDecode(DcpMsg msg)
	{
		String newFormatName;

		if (!decodesInitialized)
		{
			try 
			{
				initDecodes();
				String name = 
					GuiApp.getProperty("MessageBrowser.PresentationGroup",
					"empty-presentation");
				decodesIf.setPresentation(name);
		
				name = GuiApp.getProperty("MessageBrowser.TimeZone");
				decodesIf.setTimeZone(name);

				outFormatName = GuiApp.getProperty("MessageBrowser.OutputFormat");
				decodesIf.setFormatter(outFormatName, GuiApp.getProperties());
				formatter = decodesIf.getFormatter();
			}
			catch(Exception e)
			{
				String es = "Error initializing DECODES: " + e.toString();
				System.err.println(es);
				e.printStackTrace(System.err);
				try { outs.write(es.getBytes()); }
				catch(IOException ex) {}
				return;
			}
		} else {
			newFormatName = GuiApp.getProperty("MessageBrowser.OutputFormat");
			if (outFormatName != newFormatName)
			{
				try
				{
					decodesIf.setFormatter(newFormatName, GuiApp.getProperties());
					formatter = decodesIf.getFormatter();
					outFormatName = newFormatName;
				}
				catch(Exception e)
				{
					String es = "Error changing OutputFormat: " + e.toString();
					try { outs.write(es.getBytes()); }
					catch(IOException ex) {}
					return;
				}
			}
		}
		try
		{
			if (formatter != null
			 && (!msg.isDapsStatusMsg()
				|| !formatter.acceptRealDcpMessagesOnly()))
			{
				String data = decodesIf.decodeMessage(msg);
				outs.write(data.getBytes());
			}
		}
		catch(Exception e)
		{
			if ( printDecodingErrorMessages ) {
				String es = "Decoding error: " + e.toString() + "\n";
				try 
				{
					outs.write(es.getBytes()); 
					if (Logger.instance().getMinLogPriority() <= Logger.E_DEBUG1)
					{
						StringWriter sw = new StringWriter();
						PrintWriter pw = new PrintWriter(sw);
						e.printStackTrace(pw);
						Logger.instance().log(Logger.E_DEBUG1, sw.toString());
					}
				}
				catch(IOException ex) {}
			}
		}
	}

	/** Initializes DECODES */
	private void initDecodes()
	{
		Logger.instance().log(Logger.E_DEBUG1, 
			"Initializing msgaccess DECODES refs");
		try
		{
			/* by the time this is called the Database instance is valid or will be appropriately created. */
			decodesIf = new DecodesInterface(Database.getDb());
		}
		catch(NoClassDefFoundError ex)
		{
			PrintStream ps = new PrintStream(outs);
			ps.println(
				"Cannot decode data, cannot initialize DECODES: " + ex);
			ex.printStackTrace(ps);
			return;
		}
		decodesInitialized = true;
	}
}
