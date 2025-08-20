/*
* $Id$
*/
package lrgs.rtstat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.swing.SwingUtilities;

import ilex.xml.XmlOutputStream;
import lrgs.lrgsmon.DetailReportGenerator;
import lrgs.lrgsmon.NetworkDcpReportGenerator;
import lrgs.statusxml.LrgsStatusSnapshotExt;
import lrgs.statusxml.TopLevelXio;

/**
This class is paired with an RtStatFrame. It runs in the background, using
the frames 'client' connection to poll for status and events.
*/
public class RtStatFrameThread extends Thread
{
	private RtStatFrame myFrame;
	private boolean isShutdown;
	private DetailReportGenerator repgen;
	private NetworkDcpReportGenerator netDcpRepGen = null;
	private TopLevelXio statusParser;
	private ByteArrayOutputStream baos;
	private XmlOutputStream xos;
	private int scanPeriod;

	/**
	 * Constructor.
	 * @param frame the controlling frame.
	 */
	public RtStatFrameThread(RtStatFrame frame, int scanPeriod,
		String iconFile, String headerFile)
	{
		myFrame = frame;
		this.scanPeriod = scanPeriod;
		repgen = new DetailReportGenerator(iconFile);
		repgen.showNetworkDcpDetail = true;
		netDcpRepGen = new NetworkDcpReportGenerator(iconFile);
		if (headerFile != null)
		{
			try { repgen.setHeader(headerFile); }
			catch(IOException ex)
			{
				System.err.println("Cannot open header file '" + headerFile
					+ "': " + ex);
				System.exit(1);
			}
		}
		try { statusParser = new TopLevelXio(); }
		catch(Exception ex)
		{
			System.err.println("Cannot construct XML parser: " + ex);
		}
		baos = new ByteArrayOutputStream(16000);
		xos = new XmlOutputStream(baos, "html");
	}

	/**
	 * Thread run method.
	 */
	public void run()
	{
		isShutdown = false;
		while(!isShutdown)
		{
			try { sleep(scanPeriod * 1000L); } catch(InterruptedException ex) {}
			byte[] status = myFrame.getStatus();
			if (status != null)
			{
				try
				{
					LrgsStatusSnapshotExt lsse = statusParser.parse(status,
						0, status.length, "LRGS-Status");
					baos.reset();
					repgen.writeReport(xos, lsse.hostname, lsse, 0);
					String detailReport = baos.toString();
					baos.reset();
					String networkDcpStatus = null;
					if (lsse.networkDcpStatusList != null
					 && lsse.networkDcpStatusList.getStatusList().size() > 0)
					{
						netDcpRepGen.writeReport(xos, lsse.hostname, lsse);
						networkDcpStatus = baos.toString();
					}
					updateStatus(detailReport, networkDcpStatus);
				}
				catch(Exception ex)
				{
					System.err.println("Error parsing status: " + ex);
				}
			}
			String events[] = myFrame.getEvents();
			if (events != null && events.length > 0)
				updateEvents(events);
		}
	}

	public void shutdown()
	{
		isShutdown = true;
	}

	/**
	 * Update the events panel in the GUI thread.
	 */
	public void updateEvents(final String events[])
	{
		SwingUtilities.invokeLater(
			new Runnable()
			{
				public void run()
				{
					for(int i=0; i<events.length; i++)
						myFrame.displayEvent(events[i]);
				}
			});
	}

	/**
	 * Update the frame in the GUI thread.
	 */
	public void updateStatus(final String status, 
		final String networkDcpStatus)
	{
		SwingUtilities.invokeLater(
			new Runnable()
			{
				public void run()
				{
					myFrame.updateStatus(status, networkDcpStatus);
					if (networkDcpStatus != null)
						myFrame.updateNetworkDcpStatus(networkDcpStatus);
				}
			});
	}
}
