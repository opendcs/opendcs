/*
*  $Id$
*/
package lrgs.dqm;

import java.util.LinkedList;
import java.text.NumberFormat;

import ilex.util.Logger;
import ilex.util.TextUtil;
import lrgs.apistatus.DownLink;
import lrgs.lrgsmain.JavaLrgsStatusProvider;
import lrgs.lrgsmain.LrgsInputInterface;

/**
This class sends serial messages to the legacy DAPS system over the COM1
serial port. It is enabled by setting the DapsDqm configuration setting to 
true.

<p> Two types of messages can be sent:

<p>"S sssss eeeee ddddd \n", where sssss is start message sequence number,
eeeee is gap-end sequence number (i.e. the last dropped sequence), and
ddddd is the duration of the gap in seconds.

<p>P lllll       ddddd \n", where lllll is the last sequence number seen,
and ddddd is the duration of the current outage (00000 if we are not in an
outage).

<p>The sssss, eeeee, and lllll values are all zero-filled five-digit decimals.
The ddddd values in both messages are 5-digit, right justified, space filled.
Each message ends with space and line feed.
*/
public class DapsDqmInterface
	extends Thread
	implements DqmInterface
{
	/** Queue of text messages to send to DAPS. */
	private LinkedList<String> msgQueue;

	/** Used to format the 5-digit numbers. */
	private NumberFormat numberFormat;

	/** Shutdown flag */
	private boolean shutdownFlag;

	private JavaLrgsStatusProvider statusProvider;

	private DqmSerialInterface serialIf;

	/**
	 * Construct the DapsDqmInterface.
	 */
	public DapsDqmInterface(JavaLrgsStatusProvider statusProvider)
	{
		Logger.instance().info("Constructing DAPS DQM Interface.");
		msgQueue = new LinkedList<String>();
		numberFormat = NumberFormat.getIntegerInstance();
		numberFormat.setGroupingUsed(false);
		numberFormat.setMinimumIntegerDigits(5);
		shutdownFlag = false;
		this.statusProvider = statusProvider;
		serialIf = null;
	}

	/**
	 * Enqueues a dropout message to send to DAPS.
	 * @param gapStart the start sequence number of the dropout.
	 * @param numDropped the number dropped.
	 * @param elapsedSec the elapsed number of seconds in the gap.
	 */
	public synchronized void domsatDropped(
		int gapStart, int numDropped, int elapsedSec)
	{
		if (shutdownFlag == true)
			return;

		// "S sssss eeeee ddddd \n"
		String msg = "S " + numberFormat.format(gapStart)
			+ " " + numberFormat.format((gapStart + (numDropped-1)) % 65536)
			+ " " + TextUtil.setLengthRightJustify("" + elapsedSec, 5)
			+ "\r\n";
		Logger.instance().debug1("Enqueueing DQM msg '" + msg + "'");
		msgQueue.addLast(msg);
	}

	/**
	 * Tells the DQM interface to stop.
	 */
	public void shutdown()
	{
		shutdownFlag = true; 
	}

	/**
	 * The thread main method. 
	 * Once per minute, send status message with last sequence number to DAPS.
	 * Wakeup every second and check for dropout reports to send to DAPS.
	 */
	public void run()
	{
		Logger.instance().debug1("DAPS DQM Interface starting up.");
		int lastMin = (int)(System.currentTimeMillis() / 60000L);
		statusProvider.setDqmInterface(this);
		while(!shutdownFlag)
		{
			long now = System.currentTimeMillis();
			String msg;
			while((msg = dequeue()) != null)
			{
Logger.instance().info("DQM Sending '" + msg + "'");
				sendMsg(msg);
			}

			now = System.currentTimeMillis();
			int thisMin = (int)(System.currentTimeMillis() / 60000L);
			if (thisMin != lastMin)
			{
				DownLink dl = statusProvider.findDownLink(
					LrgsInputInterface.DL_DOMSAT);

				// Compute 'duration of outage' which is 0 if we're not in one.
				int dur = 0;
				if (dl.statusCode != LrgsInputInterface.DL_ACTIVE 
				 && dl.statusCode != LrgsInputInterface.DL_INIT)
				{
					int last = dl.lastMsgRecvTime;
					if (last == 0)
						last = (int)(statusProvider.lrgsStartupTime.getTime() 
							 / 1000L);
					dur = (int)(now/1000L) - last;
				}

				// Periodic Msg Format: "P lllll       ddddd\r\n"
				msg = "P " 
					+ numberFormat.format(dl.lastSeqNum)
					+ "       "
					+ TextUtil.setLengthRightJustify("" + dur, 5)
					+ "\r\n";

Logger.instance().info("DQM Sending '" + msg + "'");
				sendMsg(msg);

				lastMin = thisMin;
			}

			try{ sleep(1000L); } catch(InterruptedException ex) {}
		}

		Logger.instance().info("DAPS DQM Interface Shutting Down.");
		msgQueue.clear();
		if (serialIf != null)
		{
			try
			{
				serialIf.close();
				serialIf = null;
			}
			catch(Exception ex) {}
		}
	}

	private synchronized String dequeue()
	{
		return msgQueue.poll();
	}

	private void sendMsg(String msg)
	{
		try
		{
			if (serialIf == null)
			{
				Logger.instance().info("DQM opening serial port...");
				serialIf = new DqmSerialInterface();
				serialIf.open();
			}
			Logger.instance().debug1("Sending DQM msg '" + msg + "'");
			serialIf.write(msg);
		}
		catch(DqmSerialException ex)
		{
			Logger.instance().warning("Cannot open serial port: "
				+ ex + " -- DQM output skipped.");
			serialIf = null;
		}
	}
}
