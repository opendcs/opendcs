/**
 * 
 */
package lrgs.archive;

/**
 * Data structure that encapsulates information about a transmit window
 * for a self-timed GOES message.
 * @author mjmaloney
 */
public class XmitWindow
{
	public int firstXmitSecOfDay;  // Second-of-day for 1st xmit
	public int windowLengthSec;    // Window length in seconds
	public int xmitInterval;       // Transmit interval in seconds
	public int thisWindowStart;    // Start sec-of-day for this specific window

	public XmitWindow(int firstXmitSecOfDay, int windowLengthSec,
		int xmitInterval, int thisWindowStart)
	{
		this.firstXmitSecOfDay = firstXmitSecOfDay;
		this.windowLengthSec = windowLengthSec;
		this.xmitInterval = xmitInterval;
		this.thisWindowStart = thisWindowStart;
	}

}
