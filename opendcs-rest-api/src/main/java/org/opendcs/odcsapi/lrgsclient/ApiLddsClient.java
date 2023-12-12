/*
 *  Copyright 2023 OpenDCS Consortium
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

/*
*  $Id: ApiLddsClient.java,v 1.3 2023/05/29 16:21:14 mmaloney Exp $
*/
package org.opendcs.odcsapi.lrgsclient;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.UnknownHostException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;

import org.opendcs.odcsapi.beans.ApiLrgsStatus;
import org.opendcs.odcsapi.beans.ApiNetList;
import org.opendcs.odcsapi.beans.ApiRawMessageBlock;
import org.opendcs.odcsapi.beans.ApiSearchCrit;
import org.opendcs.odcsapi.dao.ApiPlatformDAO;
import org.opendcs.odcsapi.errorhandling.WebAppException;
import org.opendcs.odcsapi.util.ApiBasicClient;
import org.opendcs.odcsapi.util.ApiByteUtil;
import org.opendcs.odcsapi.util.ApiConstants;
import org.opendcs.odcsapi.util.ApiTextUtil;

/**
  This class encapsulates the communication between a client process
  and the LRGS DCP Data Server (LDDS). Construct the LddsClient object
  with a host name and optionally a port number. The LddsClient object
  contains methods for all of messages and responses supported by the
  server.
*/
public class ApiLddsClient extends ApiBasicClient
{
	/** Input stream coming from the server */
	private LddsInputStream linput;

	/** The server's protocol version number */
	private int serverProtoVersion;
	private String serverProtoVersionStr;

	/** The session key if an authenticated hello was done. */
	protected byte[] sessionKey = null;

	/** Optional module name to pre-pend to all log messages. */
	protected String module;

	private SimpleDateFormat goesDateFormat;
	public boolean implicitAllUsed = false;
	
	// The user name used in the last call to hello or auth.
	private String userName = null;
	private boolean strongOnly = false;
	
	private long lastActivity = 0L;
	
	/**
	  Constructs client for LDDS at specified port on specified host.
	  The connection is not made until the 'connect()' method is called.
	  @param host the remote host
	  @param port the remote port
	*/
	public ApiLddsClient(String host, int port)
	{
		super(host, port);

		goesDateFormat = new SimpleDateFormat("yyDDDHHmmss");
		TimeZone jtz = TimeZone.getTimeZone("UTC");
		goesDateFormat.setTimeZone(jtz);

		linput = null;
		serverProtoVersion = DdsVersion.version_unknown; // Not set until connected to a server.
		module = "";
		lastActivity = System.currentTimeMillis();
	}

	/**
	  Connects to the specified host and port.
	  @throws IOException on socket error.
	  @throws UnknownHostException if can't resolve specified host name.
	*/
	public void connect() throws IOException, UnknownHostException
	{
		debug(module +
			"Connecting to '" + host + "', port " + port);
		super.connect();
		socket.setTcpNoDelay(true);
		socket.setSoTimeout(60000);
		linput = new LddsInputStream(input);
		lastActivity = System.currentTimeMillis();
	}

	/**
	  Disconnects from the server. 
	  Close input and output streams and release all socket resources.

	  This function should be called when any of the other methods throws
	  an exception. You can then call connect() again to reconnect to the
	  server.
	*/
	public void disconnect()
	{
		try 
		{
			if (linput != null)
			{
				debug(module + 
					"Disconnecting from '" + host + "', port " + port);
				linput.close();
			}
			else
				debug(module + 
					"Already disconnected from '" + host + "', port " + port);
			super.disconnect();
		}
		catch(Exception e) {}
		finally
		{
			linput = null;
		}
	}

	/**
	 * @return true if this is currently connected to a server.
	 */
	public boolean isConnected()
	{
		return linput != null;
	}

	/**
	 * @return true if successfully authenticated to the server.
	 */
	public boolean isAuthenticated()
	{
		return sessionKey != null;
	}

	/**
	  Sends a Hello message with the specified user name. 
	  Then awaits the response from the server. If an exception is not thrown, 
	  then the login was successful.

	  @param name the user name to send to the server

	  @throws DdsServerError if the server rejected this request. In the case
	     of the hello message this means that the username is not known.
	  @throws DdsProtocolError if the server response could not be parsed
	     or it was of an unexpected type.
	  @throws IOException indicates that the socket is no longer usable.
	*/
	public void sendHello(String name)
		throws DdsServerError, DdsProtocolError, IOException
	{
		debug(module + 
			"DDS Connection (" + host + ":" + port + ") sendHello("
			+ name + ")");

		LddsMessage msg = new LddsMessage(LddsMessage.IdHello, 
			name + " " + DdsVersion.getVersion());
		sendData(msg.getBytes());

		// Read the response message - It should be same type ID.
		msg = linput.getMessage();
		if (msg.MsgId != LddsMessage.IdHello)
			throw new DdsProtocolError("Unexpected response '" + 
				msg.MsgId + "' - expected '" + LddsMessage.IdHello
				+ "' (hello)");

		String resp = ApiByteUtil.getCString(msg.MsgData, 0);
		debug("Hello response '" + resp + "'");

		// '?' means that server refused the login.
		if (resp.length() > 0 && resp.charAt(0) == '?')
			throw new DdsServerError(resp);
		else if (resp.length() >= 9 && resp.substring(0,9).equals("HELLO ???"))
			throw new DdsServerError(resp);

		// Try to parse the protocol version out of the response.
		serverProtoVersion = DdsVersion.version_1;   // Default assumption
		StringTokenizer st = new StringTokenizer(resp);
		if (st.countTokens() >= 2)
		{
			st.nextToken(); // skip name echo
			String s = st.nextToken();
			try { serverProtoVersion = Integer.parseInt(s); }
			catch(NumberFormatException ex)
			{
				warning(module +
					"Invalid protocol version '" + s 
					+ "' returned by server. Assuming protoVersion=3");
				serverProtoVersion = DdsVersion.version_1;
			}
		}

		debug(module + 
			"DDS Connection (" + host + ":" + port + ") Hello response '"
			+ resp+ "', protocolVersion=" + serverProtoVersion);
		userName = name;
		lastActivity = System.currentTimeMillis();
	}

	/**
	  Sends an Authenticated Hello message with the specified user name and
	  password. 
	  Then awaits the response from the server. If an exception is not thrown, 
	  then the login was successful.

	  @param username the user name to send to the server
	  @param password the password to send to the server

	  @throws DdsServerError if the server rejected this request. In the case
	     of the this message this means that the authentication failed.
	  @throws DdsProtocolError if the server response could not be parsed
	     or it was of an unexpected type.
	  @throws IOException indicates that the socket is no longer usable.
	*/
	public void sendAuthHello(String username, String password)
		throws DdsServerError, DdsProtocolError, IOException, WebAppException
	{
		debug(module + 
			"DDS Connection (" + host + ":" + port + ") sendAuthHello("
			+ username + ")");

		if (username == null || ApiTextUtil.isAllWhitespace(username))
			throw new DdsServerError(
				"Client-side reject: username cannot be blank", 0, 0);

		if (password == null || ApiTextUtil.isAllWhitespace(password))
			throw new DdsServerError(
				"Client-side reject: password cannot be blank", 0, 0);

		// Construct the password hash
		String digestAlgo = strongOnly ? AuthenticationUtils.ALGO_SHA256 : AuthenticationUtils.ALGO_SHA;
		doSendAuthHello(username, password, digestAlgo);
	}
	
	public void doSendAuthHello(String username, String password, String digestAlgo)
		throws DdsServerError, DdsProtocolError, IOException, WebAppException
	{
		byte[] pwDigest = AuthenticationUtils.makeShaPassword(username, password, AuthenticationUtils.ALGO_SHA);
debug(module + ".sendAuthHello pwdigest '" + ApiByteUtil.toHexString(pwDigest) + "'");

		Date now = new Date();
		int timet = (int)(now.getTime() / 1000);
		byte[] authenticator = 
			AuthenticationUtils.makeAuthenticator(timet, username, pwDigest, digestAlgo);
		String authStr = ApiByteUtil.toHexString(authenticator);
debug(module + ".sendAuthHello authenticator '" + authStr + "'");
		
		// 1 time session key substitutes authenticator for username
		sessionKey = AuthenticationUtils.makeAuthenticator(timet, 
				authStr, pwDigest, digestAlgo);
		
		String tstr = goesDateFormat.format(now);

		String body = username + " " + tstr + " " + authStr	+ " " + DdsVersion.getVersion();

		LddsMessage msg = new LddsMessage(LddsMessage.IdAuthHello, body);
		sendData(msg.getBytes());

		// Read the response message - It should be same type ID.
		msg = linput.getMessage();
		if (msg.MsgId != LddsMessage.IdAuthHello)
		{
			sessionKey = null;
			throw new DdsProtocolError("Unexpected response '" + 
				msg.MsgId + "' - expected '" + LddsMessage.IdAuthHello
				+ "' (auth-hello)");
		}

		String resp = ApiByteUtil.getCString(msg.MsgData, 0);
		debug("AuthHello response '" + resp + "'");

		// '?' means that server refused the login.
		if (resp.length() > 0 && resp.charAt(0) == '?')
		{
			sessionKey = null;
			DdsServerError DdsServerError = new DdsServerError(resp);
			if (DdsServerError.Derrno == LrgsErrorCode.DSTRONGREQUIRED
			 && digestAlgo.equals(AuthenticationUtils.ALGO_SHA))
			{
				// We tried with SHA, but server requires SHA-256. Try again.
				doSendAuthHello(username, password, AuthenticationUtils.ALGO_SHA256);
				return;
			}
			throw DdsServerError;
		}
		StringTokenizer st = new StringTokenizer(resp);
		if (st.countTokens() < 3)
		{
			sessionKey = null;
			throw new DdsProtocolError("Invalid response '" + resp 
				+ "' to AuthHello request");
		}
		st.nextToken(); // skip name echo
		st.nextToken(); // skip time echo
		serverProtoVersionStr = st.nextToken();
		
		try 
		{
			int i=0;
			while(i < serverProtoVersionStr.length()
				&& Character.isDigit(serverProtoVersionStr.charAt(i)))
				i++;
			serverProtoVersion = Integer.parseInt(
				serverProtoVersionStr.substring(0, i)); 
		}
		catch(NumberFormatException ex)
		{
			warning(module +
				"Invalid protocol version '" + serverProtoVersionStr 
				+ "' returned by server. Assuming protoVersion=3");
			serverProtoVersion = DdsVersion.version_3;
		}

		debug(module + 
			"DDS Connection (" + host + ":" + port + ") AuthHello response '"
			+ resp+ "', protocolVersion=" + serverProtoVersion);
		
		this.userName = username;
		lastActivity = System.currentTimeMillis();

	}

	/**
	  Sends a goodbye message to server. Then waits for response.

	  @throws IOException if the socket is no longer usable.
	  @throws DdsProtocolError if an unexpected response was received.
	     (i.e. the wrong message type).
	*/
	public void sendGoodbye()
		throws IOException, DdsProtocolError
	{
		debug(module + 
			"DDS Connection (" + host + ":" + port + ") sendGoodbye()");

		LddsMessage msg = new LddsMessage(LddsMessage.IdGoodbye, null);
		sendData(msg.getBytes());

		// Read the response message - It should be same type ID.
		msg = getAbortResponse();
		if (msg == null)
			throw new DdsProtocolError("No response received for GOODBYE");
		else if (msg.MsgId != LddsMessage.IdGoodbye)
			throw new DdsProtocolError(
				"Unexpected response '" + msg.MsgId + "'"
				+ " - Expected '" + LddsMessage.IdGoodbye + "' (Goodbye)");

		debug(module + 
			"DDS Connection (" + host + ":" + port + ") Goodbye response OK");
	}

	public void sendSearchCrit(ApiSearchCrit sc, ApiPlatformDAO platformDAO)
		throws IOException, DdsProtocolError, DdsServerError, SQLException
	{
		sendSearchCrit(SearchCritUtil.sc2String(sc,  platformDAO));
	}

	private void sendSearchCrit(String scdata)
		throws IOException, DdsProtocolError, DdsServerError
	{
		byte data[] = scdata.getBytes();
		
		// Construct an empty criteria message big enought for this file.
		LddsMessage msg = new LddsMessage(LddsMessage.IdCriteria, "");
		msg.MsgLength = data.length + 50;
		msg.MsgData = new byte[msg.MsgLength];

		// Create the 'header' portion containing the searchcrit filename.
		// (First 40 bytes is filename)
		String filename = "api";
		int i;
		for(i = 0; i<40 && i <filename.length(); i++)
			msg.MsgData[i] = (byte)filename.charAt(i);
		msg.MsgData[i] = (byte)0;

		// Copy the file data into the message & send it out.
		for(i=0; i<data.length; i++)
			msg.MsgData[i+50] = data[i];

		debug(module + 
			"DDS Connection (" + host + ":" + port + ") "
			+ "Sending criteria message (filesize = " +
				data.length + " bytes)");
		debug(module + ".sendSearchCrit: " + new String(msg.getBytes()));

		sendData(msg.getBytes());

		// Get response.
		msg = linput.getMessage();
		if (msg.MsgId != LddsMessage.IdCriteria)
			throw new DdsProtocolError("Unexpected responses '" + msg.MsgId + "'");
		else if (msg.MsgLength > 0 && (char)msg.MsgData[0] == '?')
		{
			String s = new String(msg.MsgData);
			throw new DdsServerError(s);
		}

		lastActivity = System.currentTimeMillis();
		debug(module + 
			"DDS Connection (" + host + ":" + port + ") "
			+ "Successfully sent searchcrit.");
	}
	 
	private LddsMessage getAbortResponse()
		throws IOException, DdsProtocolError
	{
		// Wait up to 10 seconds for the response to the abort.
		// If none received, we'll just abort anyway.
		socket.setSoTimeout(10000);

		return linput.getMessage();
	}

	/**
	  sends a network list object to the server.

	  @param netlist the NetworkList object to send

	  @throws IOException if network socket error.
	  @throws DdsProtocolError if an unexpected response was received.
	     (i.e. the wrong message type).
	  @throws DdsServerError if the server responded to the request with an
	     error message. This most likely means that that the named
	     file could not be created on the server.
	*/
	public void sendNetList(ApiNetList netlist)
		throws IOException, DdsProtocolError, DdsServerError
	{
		debug(module + 
			"DDS Connection (" + host + ":" + port + ") "
			+ "sendNetList from NetworkList object in memory");

		// Construct an empty criteria message big enought for this file.
		LddsMessage msg = new LddsMessage(LddsMessage.IdPutNetlist, "");
		String data = SearchCritUtil.nl2String(netlist);
		byte databytes[] = data.getBytes();
		msg.MsgLength = databytes.length + 64;
		msg.MsgData = new byte[msg.MsgLength];


		// Create the 'header' portion, which contains 64 char filename
		String name = netlist.getName();
		if (name == null)
			name = "list-in-memory";

		byte namebytes[] = name.getBytes();
		int i;
		for(i = 0; i<63 && i < namebytes.length; i++)
			msg.MsgData[i] = namebytes[i];
		msg.MsgData[i] = (byte)0;

		// Copy the file data into the message & send it out.
		for(i=0; i<databytes.length; i++)
			msg.MsgData[i+64] = databytes[i];

		debug(module + 
			"DDS Connection (" + host + ":" + port + ") "
			+ "Sending netlist message (total length=" + msg.MsgLength + ")");
		sendData(msg.getBytes());

		// Get response.
		msg = linput.getMessage();
		if (msg.MsgId != LddsMessage.IdPutNetlist)
			throw new DdsProtocolError("Unexpected responses '" + 
				msg.MsgId + "'");
		else if ((char)msg.MsgData[0] == '?')
			throw new DdsServerError(new String(msg.MsgData));
		debug(module + 
			"DDS Connection (" + host + ":" + port + ") netlist response OK");
		lastActivity = System.currentTimeMillis();
	}

	/**
	  Retrieves the status from the server.
	  @return byte array containing an XML block representing the server's
	  current status.
	*/
	public byte[] getStatusBytes()
		throws IOException, DdsProtocolError, DdsServerError 
	{
		debug(module + 
			"DDS Connection (" + host + ":" + port + ") getStatus");

		// Send request
		LddsMessage msg = new LddsMessage(LddsMessage.IdStatus, "?");
		sendData(msg.getBytes());

		// Read the response message - It should be same type ID.
		msg = linput.getMessage();
		if (msg.MsgId != LddsMessage.IdStatus)
			throw new DdsProtocolError(
				"Unexpected response to getStatus '" + msg.MsgId + "'");

		// Validate response
		if ((char)msg.MsgData[0] == '?')
			throw new DdsServerError(new String(msg.MsgData));

		// Some servers throw a null byte at the end. If so, convert to LF.
		if (msg.MsgData[msg.MsgData.length-1] == 0)
			msg.MsgData[msg.MsgData.length-1] = (byte)'\n';

		return msg.MsgData;
	}
	
	public ApiLrgsStatus getLrgsStatus()
		throws IOException, DdsProtocolError, DdsServerError 
	{
		byte[] statusBytes = getStatusBytes();
		try
		{
			LrgsStatusXio statParser = new LrgsStatusXio();
			lastActivity = System.currentTimeMillis();
			return statParser.parse(statusBytes, 0, statusBytes.length, "dds");
		}
		catch(Exception ex)
		{
			String em = "Error in LrgsStatusXio: " + ex;
			warning(em);
			System.err.println(em);
			ex.printStackTrace();
			throw new DdsProtocolError(em);
		}
	}

	public byte[] getMsgBlockExtXml(int timeout)
		throws IOException, DdsProtocolError, DdsServerError
	{
		debug(module + 
			"DDS Connection (" + host + ":" + port + ") "
			+ "Requesting DCP Message Block Ext (timeout=" 
			+ timeout + ")...");

		LddsMessage req = new LddsMessage(LddsMessage.IdDcpBlockExt, "");

		LddsMessage resp = serverExec(req);
		if (resp.MsgId != LddsMessage.IdDcpBlockExt)
			throw new DdsProtocolError("Unexpected response '" + resp.MsgId 
					+ "', expected type '" + LddsMessage.IdDcpBlockExt + "'");

		// Message contains compressed byte-array of XML. Uncompress it.
		try
		{
			ByteArrayInputStream bais = new ByteArrayInputStream(resp.MsgData);
			GZIPInputStream gzis = new GZIPInputStream(bais);
			ByteArrayOutputStream baos = 
				new ByteArrayOutputStream(resp.MsgData.length);
			byte buf[] = new byte[4096];
			int len;
			while ((len = gzis.read(buf)) > 0)
				baos.write(buf, 0, len);
			gzis.close();
			bais.close();
			baos.close();
			byte ret[] = baos.toByteArray();
//Logger.instance().info("orig response len=" + resp.MsgData.length 
//+ " after unzip=" + ret.length);
			lastActivity = System.currentTimeMillis();
			return ret;
		}
		catch(IOException ex)
		{
			throw new DdsProtocolError("Error unpacking compressed block: " + ex);
		}
	}

	/**
	 * Gets the next extended mode block and converts the XML into an array
	 * of DcpMsg objects.
	 */
	public ApiRawMessageBlock getMsgBlockExt(int timeout)
		throws IOException, DdsProtocolError, DdsServerError
	{
		byte[] xmldata = getMsgBlockExtXml(timeout);
		try
		{
			RawMessageBlockParser parser = new RawMessageBlockParser();
			lastActivity = System.currentTimeMillis();
			return parser.parse(xmldata, 0, xmldata.length, "dds");
		}
		catch(Exception ex)
		{
			String em = "Error in RawMessageBlockParser: " + ex;
			warning(em);
			System.err.println(em);
			ex.printStackTrace();
			throw new DdsProtocolError(em);
		}
	}

	/**
	  @return the protocol version supported by the server to which you
	  are connected.
	*/
	public int getServerProtoVersion() { return serverProtoVersion; }

	/**
	 * Sends a raw LddsMessage and gets the response returned by server.
	 * It's up to the caller to validate the contents of the response.
	 * @param msg the message to send
	 * @return the message response from the server.
	 */
	public LddsMessage serverExec(LddsMessage msg)
		throws IOException, DdsProtocolError, DdsServerError 
	{
		debug("serverExec: sending LddsMessage type=" 
			+ msg.MsgId + ", length=" + msg.MsgLength);
		sendData(msg.getBytes());
		LddsMessage ret = linput.getMessage();
		debug("serverExec: response LddsMessage type=" 
			+ ret.MsgId + ", length=" + ret.MsgLength);

		if ((char)ret.MsgData[0] == '?')
			throw new DdsServerError(new String(ret.MsgData));
		return ret;
	}

	/**
	 * @return the session key, or null if this is an unauthenticated 
	 * connection.
	 */
	public byte[] getSessionKey()
	{
		return sessionKey;
	}

	public String getUserName()
	{
		return userName;
	}

	public void setStrongOnly(boolean strongOnly)
	{
		this.strongOnly = strongOnly;
	}

	public void info(String str)
	{
		Logger.getLogger(ApiConstants.loggerName).info("LddsClient INFO: " + str);
	}
	
	private void debug(String str)
	{
		Logger.getLogger(ApiConstants.loggerName).fine("LddsClient DEBUG: " + str);
	}
	
	private void warning(String str)
	{
		Logger.getLogger(ApiConstants.loggerName).warning("LddsClient WARNING: " + str);
	}

	public long getLastActivity()
	{
		return lastActivity;
	}
}
