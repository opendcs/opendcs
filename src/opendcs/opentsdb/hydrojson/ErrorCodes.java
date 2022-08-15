package opendcs.opentsdb.hydrojson;

import javax.servlet.http.HttpServletResponse;

public class ErrorCodes
{
	/** Missing required token argument or invalid token provided */
	public static final int TOKEN_REQUIRED = HttpServletResponse.SC_UNAUTHORIZED;
	
	/** username/password authentication failed */
	public static final int AUTH_FAILED = HttpServletResponse.SC_FORBIDDEN;
	
	/** Client requests object with ID that is not in the database */
	public static final int NO_SUCH_OBJECT = HttpServletResponse.SC_GONE;
	
	/** Missing required ID arg */
	public static final int MISSING_ID = HttpServletResponse.SC_NOT_ACCEPTABLE;
	
	/** Unexplained exception during database I/O */
	public static final int DATABASE_ERROR = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
	
	public static final int NOT_ALLOWED = HttpServletResponse.SC_METHOD_NOT_ALLOWED;
	
	public static final int BAD_CONFIG = HttpServletResponse.SC_PRECONDITION_FAILED;
	
	public static final int IO_ERROR = HttpServletResponse.SC_CONFLICT;
}
