package lrgs.ldds;

import lrgs.common.ArchiveException;
import lrgs.common.LrgsErrorCode;

/**
 * Throws by the PasswordChecker if the password does not meet local
 * requirements for length, complexity, history, etc.
 * @author mmaloney
 *
 */
public class BadPasswordException
	extends ArchiveException
{
	public BadPasswordException(String reason)
	{
		super(reason, LrgsErrorCode.DBADPASSWORD, false);
	}
}
