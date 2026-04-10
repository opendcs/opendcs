/*
* Where Applicable, Copyright 2025 OpenDCS Consortium and/or its contributors
*
* Licensed under the Apache License, Version 2.0 (the "License"); you may not
* use this file except in compliance with the License. You may obtain a copy
* of the License at
*
*   http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
* License for the specific language governing permissions and limitations
* under the License.
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
		// Initial 5 second pause
		try { sleep(5000L); } catch(InterruptedException ex) {}


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

					SwingUtilities.invokeLater(
						new Runnable()
						{
							public void run()
							{
								myFrame.updateStatus();
							}
						});
				}
			}
			try { sleep(1000L); } catch(InterruptedException ex) {}
		}
	}
}