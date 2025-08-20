/*
* $Id$
*/
package lrgs.rtstat;

import javax.swing.SwingUtilities;

/**
This class is paired with an RtSummaryStatFrame. 
It runs in the background, using
the frames 'client' connection to poll for status and events.
*/
public class RtSummaryStatThread extends Thread
{
	private RtSummaryStatFrame myFrame;
	private int scanPeriod;
	boolean updateNow;

	/**
	 * Constructor.
	 * @param frame the controlling frame.
	 * @param scanPeriod the scan period in seconds
	 */
	public RtSummaryStatThread(RtSummaryStatFrame frame, int scanPeriod)
	{
		myFrame = frame;
		this.scanPeriod = scanPeriod;
		updateNow = false;
	}

	/**
	 * Thread run method.
	 */
	public void run()
	{
System.out.println("thread starting, pause for 5 seconds.");
		// Initial 5 second pause
		try { sleep(5000L); } catch(InterruptedException ex) {}
System.out.println("thread update loop starting.");

		// Don't need to worry about shutdown, parent will call System.exit()
		long lastUpdate = 0L;
		while(true)
		{
			long now = System.currentTimeMillis();
			if (updateNow || now - lastUpdate >= scanPeriod * 1000L)
			{
				updateNow = false;
				if (!myFrame.isPaused)
				{
					lastUpdate = now;
System.out.println("scheduling update in GUI thread.");
					SwingUtilities.invokeLater(
						new Runnable()
						{
							public void run()
							{
System.out.println("GUI thread doing update.");
								myFrame.updateStatus();
							}
						});
				}
			}
			try { sleep(1000L); } catch(InterruptedException ex) {}
		}
	}
}
