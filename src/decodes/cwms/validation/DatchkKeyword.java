package decodes.cwms.validation;

public enum DatchkKeyword
{
	ALARM("AL"),
	ASSIGN("AS"),
	COMPUTE("COM"),
	CONTEXT("CON"),
	CRITERIA("CRITE"),
	CRITFILE("CRITF"),
	DATA("DA"),
	DEFINE("DE"),
	END("EN"),
	ESTIMATE("ES"),
	PRECISION("PR"),
	RANGE("RA"),
	TEST("TE"),
	TIME("TI");
	
	/** Minimum possible unique abbreviation */
	private String abbr;
	
	private DatchkKeyword(String abbr)
	{
		this.abbr = abbr;
	}

	/**
	 * Convert a token to a keyword enum value.
	 * @param token the token read from the datchk file
	 * @return the keyword enum or null if no match.
	 */
	public static DatchkKeyword token2keyword(String token)
	{
		token = token.toUpperCase();
		for(DatchkKeyword dk : values())
		{
			if (token.startsWith(dk.abbr))
			{
				String fullName = dk.name();
				for(int idx=dk.abbr.length(); 
				    idx < token.length() && idx < dk.abbr.length(); idx++)
					if (token.charAt(idx) != fullName.charAt(idx))
						return null;
				return dk;
			}
		}
		return null;
	}
	
	public static void main(String args[])
	{
		DatchkKeyword dk = token2keyword(args[0]);
		if (dk == null)
			System.out.println("No match");
		else
			System.out.println("keyword '" + dk.abbr + "' fullname='" + dk.name() + "'");
	}
}
