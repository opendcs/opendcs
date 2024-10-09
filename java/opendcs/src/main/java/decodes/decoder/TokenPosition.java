package decodes.decoder;

/**
 * Encapsulates the position of a token within a string.
 * @author mmaloney
 *
 */
public class TokenPosition
{
	private int start=0, end=0;
	
	public TokenPosition() {}
	
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

	public void setStart(int start)
	{
		this.start = start;
	}

	public void setEnd(int end)
	{
		this.end = end;
	}
}
