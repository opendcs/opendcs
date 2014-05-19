/*
*  $Id$
*/
package lrgs.ldds;

/**
  Encapsulate various constants used both on the client and server side.
*/
public class LddsParams
{
	/** Default port that the server will listen on: */
	public static final int DefaultPort = 16003;

	/** The server will hangup after this many seconds of inactivity. */
	public static final int ServerHangupSeconds = 600;  // 10 min.

	/** The highest version of DDS protocol supported by this server. */
	// This was moved to DdsVersion.java
//	public static final int protocolVersion = 8;

	/** Module name for log messages. */
	public static final String module = "DdsSvr";
}
