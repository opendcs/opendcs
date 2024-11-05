/**
 * $Id$
 * 
 * $Log$
 * Revision 1.1  2016/02/04 18:55:42  mmaloney
 * Added new method for changing passwords allows the server to check for length, complexity, history, etc. This enhancement was required by NOAA
 *
 * 
 */
package lrgs.ldds;


public interface PasswordChecker
{
	/**
	 * Check the password for length, complexity, history, etc.
	 * Silently return if password is ok. If not, throw BadPasswordException
	 * @param password the password
	 * @throws BadPasswordException containing message if password is not ok.
	 */
	public void checkPassword(String username, String newPassword, String newPwHash)
		throws BadPasswordException;
	
	public String generateRandomPassword();
}
