package lrgs.ldds;

import ilex.util.EnvExpander;
import ilex.util.Logger;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Random;

import org.passay.DictionarySubstringRule;
import org.passay.PasswordData;
import org.passay.RuleResult;
import org.passay.RuleResultDetail;
import org.passay.dictionary.ArrayWordList;
import org.passay.dictionary.WordListDictionary;
import org.passay.dictionary.WordLists;
import org.passay.dictionary.sort.ArraysSort;

import lrgs.db.LrgsDatabase;
import lrgs.db.LrgsDatabaseThread;

/**
 * This class implements the CITR-021 Password Management Requirements
 * for NOAA's LRGS.
 * <ul>
 *   <li>At least 12 non-blank characters</li>
 *   <li>At least 3 of 4 categories: LC, UC, Digits, Special</li>
 *   <li>No common words, acronyms, contractions and geographic locations</li>
 *   <li>Cannot be reused for last 2 years or last 8 passwords</li>
 *   <li>Notify user when passwords have been changed</li>
 *   <li>Notify user on 5 sequential failed attempt to login</li>
 * </ul>
 * @author mmaloney
 *
 */
public class NoaaPasswordChecker implements PasswordChecker
{
	private static final String specialChars = "~`@#$%^&*()-_=+\\\"|';:,<.>/?";
	private static final Random random = new Random(System.currentTimeMillis());
	private static DictionarySubstringRule dictRule = null;

	@Override
	public void checkPassword(String username, String newPassword, String newPwHash)
		throws BadPasswordException
	{
		// At least 12 non-blank chars
		int nnb = 0;
		for (int idx = 0; idx < newPassword.length(); idx++)
			if (!Character.isWhitespace(newPassword.charAt(idx)))
				nnb++;
		if (nnb < 12)
			throw new BadPasswordException("Password not long enough. Must have at least 12 non-blank chars.");
		
		// Check for 3 out of 4 character types:
		int ntypes = 0;
		for(int idx = 0; idx < newPassword.length(); idx++)
			if (Character.isLowerCase(newPassword.charAt(idx)))
			{
				ntypes++;
				break;
			}
		for(int idx = 0; idx < newPassword.length(); idx++)
			if (Character.isUpperCase(newPassword.charAt(idx)))
			{
				ntypes++;
				break;
			}
		for(int idx = 0; idx < newPassword.length(); idx++)
			if (Character.isDigit(newPassword.charAt(idx)))
			{
				ntypes++;
				break;
			}
		for(int idx = 0; idx < newPassword.length(); idx++)
			if (specialChars.indexOf(newPassword.charAt(idx)) >= 0)
			{
				ntypes++;
				break;
			}
		if (ntypes < 3)
			throw new BadPasswordException("Password must contain at least 3 categories of: "
				+ "lower, upper, digit, special characters.");
		
		if (newPassword.toLowerCase().contains(username.toLowerCase()))
			throw new BadPasswordException("Password cannot contain username.");

		checkDictionary(newPassword);
		
		// Check password history for reused passwords
		LrgsDatabaseThread ldt = LrgsDatabaseThread.instance();
		if (ldt != null)
		{
			LrgsDatabase lrgsDb = ldt.getLrgsDb();
			if (lrgsDb != null)
				lrgsDb.checkHistoricalPassword(username, newPwHash);
		}

	}

	@Override
	public synchronized String generateRandomPassword()
	{
		StringBuilder sb = new StringBuilder();
		
		for(int idx = 0; idx<4; idx++)
			sb.append((char)(((int)'a') + (int)(random.nextDouble()*26.0)));
		sb.append("-");
		for(int idx = 0; idx<4; idx++)
			sb.append((char)(((int)'A') + (int)(random.nextDouble()*26.0)));
		sb.append("-");
		for(int idx = 0; idx<2; idx++)
			sb.append((char)(((int)'0') + (int)(random.nextDouble()*10.0)));
		return sb.toString();
	}
	
	private synchronized void checkDictionary(String pw)
		throws BadPasswordException
	{
		if (dictRule == null)
		{
			FileReader fra[] = new FileReader[1];
			String wordListName = EnvExpander.expand("$DCSTOOL_HOME/noaa/noaapw.words");
			try
			{
				fra[0] = new FileReader(wordListName);
				boolean caseSensitive = false;
				ArrayWordList awl = WordLists.createFromReader(fra, caseSensitive, new ArraysSort());
				WordListDictionary dict = new WordListDictionary(awl);
				dictRule = new DictionarySubstringRule(dict);
				Logger.instance().info("Created word list with " + awl.size() + " words.");
			}
			catch (Exception ex)
			{
				Logger.instance().warning("Cannot find word list '"  + wordListName + "': " + ex);
				return;
			}
		}
		
		PasswordData pd = new PasswordData(pw);
		RuleResult rr = dictRule.validate(pd);
		Logger.instance().debug1("NoaaPasswordChecker.checkDictionary -- The password '" + pd.getPassword() + "' "
			+ (rr.isValid() ? "does NOT" : "DOES") + " contain dictionary words: " +
			rr.toString());
		if (!rr.isValid())
		{
			StringBuilder sb = new StringBuilder();
			for(RuleResultDetail rrd : rr.getDetails())
			{
				sb.append(rrd.getErrorCode() + " ");
				for(String k : rrd.getParameters().keySet())
					sb.append(" " + k + "=" + rrd.getParameters().get(k));
			}
			throw new BadPasswordException("Password fails dictionary test: " + sb.toString());
		}
	}

}
