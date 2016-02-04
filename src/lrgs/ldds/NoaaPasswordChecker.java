package lrgs.ldds;

public class NoaaPasswordChecker implements PasswordChecker
{

	@Override
	public void checkPassword(String password)
		throws BadPasswordException
	{
		if (password.length() < 12)
			throw new BadPasswordException("Password not long enough. Must be minimum of 12 chars.");
		int ntypes = 0;
		for(int idx = 0; idx < password.length(); idx++)
			if (Character.isLowerCase(password.charAt(idx)))
			{
				ntypes++;
				break;
			}
		for(int idx = 0; idx < password.length(); idx++)
			if (Character.isUpperCase(password.charAt(idx)))
			{
				ntypes++;
				break;
			}
		for(int idx = 0; idx < password.length(); idx++)
			if (Character.isDigit(password.charAt(idx)))
			{
				ntypes++;
				break;
			}
		for(int idx = 0; idx < password.length(); idx++)
			if (" ~`@#$%^&*()-_=+\\\"|';:,<.>/?".indexOf(password.charAt(idx)) >= 0)
			{
				ntypes++;
				break;
			}
		if (ntypes < 3)
			throw new BadPasswordException("Password must contain at least 3 categories of: "
				+ "lower, upper, digit, special characters.");

		//TODO check for dictionary words
		
		//TODO check password history for reused passwords

	}

}
