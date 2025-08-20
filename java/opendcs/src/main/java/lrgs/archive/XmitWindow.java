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
	/** Second-of-day for 1st xmit */
	public int firstXmitSecOfDay;
	/** Window length in seconds */
	public int windowLengthSec;
	/** Transmit interval in seconds */
	public int xmitInterval;
	/** Start sec-of-day for this specific window */
	public int thisWindowStart;

	public XmitWindow(int firstXmitSecOfDay, int windowLengthSec,
		int xmitInterval, int thisWindowStart)
	{
		this.firstXmitSecOfDay = firstXmitSecOfDay;
		this.windowLengthSec = windowLengthSec;
		this.xmitInterval = xmitInterval;
		this.thisWindowStart = thisWindowStart;
	}

}
