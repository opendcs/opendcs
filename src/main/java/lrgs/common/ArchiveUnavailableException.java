
package lrgs.common;

/**
Throws when attempt is made to access an archive that is not available.
*/
public class ArchiveUnavailableException extends ArchiveException
{
	/**
	 * Constructor.
	 * @param msg the error message
	 */
	public ArchiveUnavailableException(String msg, int errorCode)
	{
		super(msg, errorCode, true);
	}
}
