
package lrgs.common;

/**
Thrown by getNext... methods when the until time has been reached.
*/
public class UntilReachedException extends ArchiveException
{
	/**
	 * Constructor.
	 */
	public UntilReachedException()
	{
		super("Until Reached", LrgsErrorCode.DUNTIL, false);
	}
}
