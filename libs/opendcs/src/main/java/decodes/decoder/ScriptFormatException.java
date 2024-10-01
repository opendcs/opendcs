/*
*  $Id$
*
*  $State$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:21:02  cvs
*  Added legacy code to repository
*
*  Revision 1.2  2004/08/31 16:31:22  mjmaloney
*  javadoc
*
*  Revision 1.1  2001/05/05 23:53:51  mike
*  dev
*
*/
package decodes.decoder;

/**
This exception indicates a syntax error in a format statement.
*/
public class ScriptFormatException extends DecoderException
{
	private static final String NO_STATEMENT = "not_set";
	private static final String NO_OPERATION = "unknown";
	private final String statement;
	private final String operation;
	private final int idx;

	/**
	  Construct the exception.
	  @param msg the message
	*/
	public ScriptFormatException(String msg)
	{
		super(msg);
		statement = NO_STATEMENT;
		operation = NO_OPERATION;
		idx = -1;

	}

	/**
	 * Contains information about an error on a format statement.
	 *
	 * @param msg Friendly message
	 * @param fmtStatement actual format statement text
	 * @param operation last processed operator
	 * @param idx index in the format statement string of the error
	 */
	public ScriptFormatException(String msg, String fmtStatement, String operation, int idx)
	{
		super(msg);
		this.statement = fmtStatement;
		this.operation = operation;
		this.idx = idx;
	}

	public String getStatement()
	{
		return statement;
	}

	public String getOperation()
	{
		return operation;
	}

	public int getErrorIndex()
	{
		return idx;
	}

	@Override
	public String getMessage()
	{
		StringBuilder sb = new StringBuilder();
		sb.append(super.getMessage()).append("");
		if (!NO_STATEMENT.equals(getStatement()))
		{
			sb.append("In Statement {").append(getStatement()).append("} ");
			sb.append("Nearest offset at column").append(idx).append(".");
		}
		if (!NO_OPERATION.equals(getOperation()))
		{
			sb.append(" Last processed operation was {").append(getOperation()).append("}");
		}
		return sb.toString();
	}
}
