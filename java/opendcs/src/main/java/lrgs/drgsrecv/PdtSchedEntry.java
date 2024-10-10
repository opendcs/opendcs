/*
*  $Id$
*
*  $Log$
*  Revision 1.2  2008/09/08 19:14:03  mjmaloney
*  LRGS 7 dev
*
*  Revision 1.1  2008/04/04 18:21:13  cvs
*  Added legacy code to repository
*
*  Revision 1.1  2005/09/26 21:01:44  mmaloney
*  created.
*
*/
package lrgs.drgsrecv;

/**
This class contains a subset of PDT entries which are necessary to monitor
and validate the incoming stream of DCP messages.
@deprecated - use decodes.util.PdtEntry instead.
*/
@Deprecated
public class PdtSchedEntry
	implements Comparable
{
	/**
	 * DCP Address stored as a 32 bit integer.
	 * Must be cast & masked to an unsigned 32-bit quantity.
	 */
	private int dcpAddr;

	/**
	 * Channel for self-timed messages, -1 means this DCP doesn't support ST.
	 */
	private short stChan;

	/**
	 * Channel for random messages, -1 means this DCP doesn't support random.
	 */
	private short rdChan;

	/**
	 * Transmit window duration in seconds.
	 */
	private short xmitWindow;

	/**
	 * Transmit interval in seconds.
	 */
	private int xmitInterval;

	/**
	 * Second of day for first transmission.
	 */
	private int firstXmit;

	/**
	 * Constructs a new PdtSchedEntry.
	 * @param dcpAddr DCP Address
	 * @param stChan Channel for self-timed messages
	 * @param rdChan Channel for random messages
	 * @param xmitWindow Transmit window duration in seconds.
	 * @param xmitInterval Transmit interval in seconds.
	 * @param firstXmit Second of day for first transmission.
	 */
	public PdtSchedEntry(long dcpAddr, int stChan, int rdChan,
		int xmitWindow, int xmitInterval, int firstXmit)
	{
		this.dcpAddr = (int)(dcpAddr & 0xffffffffL);
		this.stChan = (short)stChan;
		this.rdChan = (short)rdChan;
		this.xmitWindow = (short)xmitWindow;
		this.xmitInterval = xmitInterval;
		this.firstXmit = firstXmit;
	}

	/** @return DCP Address */
	public long getDcpAddress()
	{
		return ((long)dcpAddr) & 0xffffffffL;
	}

	/**
	 * @return Channel for self-timed messages, -1 means none.
	 */
	public int getStChan() { return (int)stChan; }

	/**
	 * @return Channel for random messages, -1 means none.
	 */
	public int getRdChan() { return (int)rdChan; }

	/**
	 * @return Transmit window duration in seconds.
	 */
	public int getXmitWindow() { return xmitWindow; }

	/**
	 * @return Transmit interval in seconds.
	 */
	public int getXmitInterval() { return xmitInterval; }

	/**
	 * @return Second of day for first transmission.
	 */
	public int getFirstXmit() { return firstXmit; }

	public int compareTo(Object rhs)
	{
		PdtSchedEntry pse = (PdtSchedEntry)rhs;
		long r = getDcpAddress() - pse.getDcpAddress();
		if (r < 0)
			return -1;
		else if (r > 0)
			return 1;
		return 0;
	}
}
