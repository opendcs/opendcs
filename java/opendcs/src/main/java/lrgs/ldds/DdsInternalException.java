
package lrgs.ldds;
import lrgs.common.LrgsErrorCode;

/**
Thrown when a search timed-out, meaning the designated wait time has
elapsed and no messages were retrieved.
*/
public class DdsInternalException extends LddsRequestException
{
	/**
	 * Constructor.
	 * @param msg the message
	 */
	public DdsInternalException(String msg)
	{
		super(msg, LrgsErrorCode.DDDSINTERNAL, true);
	}
}
