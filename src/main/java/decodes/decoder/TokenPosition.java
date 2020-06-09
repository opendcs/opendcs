package decodes.decoder;

/**
 * Encapsulates the position of a token within a string.
 * @author mmaloney
 *
 */
public class TokenPosition
{
	private int start, end;
	
	public TokenPosition(int start, int end)
	{
		this.start = start;
		this.end = end;
	}

	/**
	 * @return start position of the token
	 */
	public int getStart()
	{
		return start;
	}

	/**
	 * @return character position after the end of the token
	 */
	public int getEnd()
	{
		return end;
	}
	
	public String toString()
	{
		return "(" + start + ", " + end + ")";
	}
}
