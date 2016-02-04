/**
 * $Id$
 * 
 * $Log$
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
	public void checkPassword(String password)
		throws BadPasswordException;
}
