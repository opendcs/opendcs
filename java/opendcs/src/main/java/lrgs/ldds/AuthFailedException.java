package lrgs.ldds;

import lrgs.common.LrgsErrorCode;

/**
Thrown when authorization fails.
*/
public class AuthFailedException extends LddsRequestException
{
	/**
	 * Constructor.
	 * @param msg the message
	 */
	public AuthFailedException(String msg)
	{
		super(msg, LrgsErrorCode.DDDSAUTHFAILED, true);
	}
}
