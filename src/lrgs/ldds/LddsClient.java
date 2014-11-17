/*
*  $Id$
*/
package lrgs.ldds;

import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.util.Vector;
import java.util.zip.GZIPInputStream;

import ilex.util.*;
import ilex.net.BasicClient;
import lrgs.common.*;
import lrgs.apiadmin.AuthenticatorString;
import lrgs.db.Outage;

import decodes.db.Constants;
import decodes.db.Database;
import decodes.db.Platform;
import decodes.db.PlatformList;
import decodes.db.TransportMedium;

/**
  This class encapsulates the communication between a client process
  and the LRGS DCP Data Server (LDDS). Construct the LddsClient object
  with a host name and optionally a port number. The LddsClient object
  contains methods for all of messages & responses supported by the
  server.
*/
public class LddsClient extends BasicClient
{
	/** Input stream coming from the server */
	private LddsInputStream linput;

	/** The server's protocol version number */
	private int serverProtoVersion;
	private String serverProtoVersionStr;

	/** True if I should request messages in multi-mode. */
	private boolean multiMessageModeEnabled;

	/** True if I should request messages in extended-multi-mode. */
	private boolean extMessageModeEnabled;

	/** Buffer holding last multi-message response. */
	private DcpMsg multiMsg[];

	/** Index into multi-message response buffer */
	private int multiMsgIdx;

	/** The session key if an authenticated hello was done. */
	protected byte[] sessionKey = null;

	/** Optional module name to pre-pend to all log messages. */
	protected String module;

	ExtBlockXmlParser extBlockXmlParser;

	private SimpleDateFormat outageDateFormat;
	private SimpleDateFormat goesDateFormat;
	private OutageXmlParser outageXmlParser = null;
	public boolean implicitAllUsed = false;
	
	SearchCritLocalFilter searchCritLocalFilter = null;


	/**
	  Constructs client for LDDS at specified host. 
	  Use default port number.
	  The connection is not made until the 'connect()' method is called.
	  @param host the remote host
	*/
	public LddsClient(String host)
	{
		this(host, LddsParams.DefaultPort);
	}

	/**
	  Constructs client for LDDS at specified port on specified host.
	  The connection is not made until the 'connect()' method is called.
	  @param host the remote host
	  @param port the remote port
	*/
	public LddsClient(String host, int port)
	{
		super(host, port);

		goesDateFormat = new SimpleDateFormat("yyDDDHHmmss");
		TimeZone jtz = TimeZone.getTimeZone("UTC");
		goesDateFormat.setTimeZone(jtz);

		outageDateFormat = new SimpleDateFormat("yyyy/DDD-HH:mm:ss");
		outageDateFormat.setTimeZone(jtz);

		linput = null;
		serverProtoVersion = DdsVersion.version_unknown; // Not set until connected to a server.

		/// Client must explicitely enable multi-mode.
		multiMessageModeEnabled = false;
		extMessageModeEnabled = true;
		multiMsg = null;
		multiMsgIdx = 0;
		module = "";
		extBlockXmlParser = null;
	}

	/**
	  Multi-message mode allows the client to attempt to receive several
	  messages from the server in a single request. Approximately 80K worth
	  of message data can be retrieved in a single request.
	  Even if enabled, it will only be used if the server supports it.
	  @param tf true if you want to use multi-mode
	*/
	public void enableMultiMessageMode(boolean tf)
	{
		multiMessageModeEnabled = tf;
	}

	/**
	  Extended-message mode allows the client to attempt to receive several
	  messages from the server in a single request with extended status info
	  on each message.
	  Even if enabled, it will only be used if the server supports it.
	  @param tf true if you want to use multi-mode
	*/
	public void enableExtMessageMode(boolean tf)
	{
		extMessageModeEnabled = tf;
//Logger.instance().info("extMessageModeEnabled=" + extMessageModeEnabled);
	}

	/**
	  Connects to the specified host and port.
	  @throws IOException on socket error.
	  @throws UnknownHostException if can't resolve specified host name.
	*/
	public void connect() throws IOException, UnknownHostException
	{
		Logger.instance().debug2(module +
			"Connecting to '" + host + "', port " + port);
		super.connect();
		socket.setTcpNoDelay(true);
		socket.setSoTimeout(60000);
		linput = new LddsInputStream(input);
	}

	/**
	  Disconnects from the server. 
	  Close input & output streams and release all socket resources.

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
				Logger.instance().debug2(module + 
					"Disconnecting from '" + host + "', port " + port);
				linput.close();
			}
			else
				Logger.instance().debug2(module + 
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

	  @throws ServerError if the server rejected this request. In the case
	     of the hello message this means that the username is not known.
	  @throws ProtocolError if the server response could not be parsed
	     or it was of an unexpected type.
	  @throws IOException indicates that the socket is no longer usable.
	*/
	public void sendHello(String name)
		throws ServerError, ProtocolError, IOException
	{
		// Send hello
		if (debug != null)
			debug.println("sendHello("+name+")");

		Logger.instance().debug2(module + 
			"DDS Connection (" + host + ":" + port + ") sendHello("
			+ name + ")");

		LddsMessage msg = new LddsMessage(LddsMessage.IdHello, 
			name + " " + DdsVersion.getVersion());
		sendData(msg.getBytes());

		// Read the response message - It should be same type ID.
		msg = linput.getMessage();
		if (msg.MsgId != LddsMessage.IdHello)
			throw new ProtocolError("Unexpected response '" + 
				msg.MsgId + "' - expected '" + LddsMessage.IdHello
				+ "' (hello)");

		String resp = ByteUtil.getCString(msg.MsgData, 0);
		if (debug != null)
			debug.println("Hello response '" + resp + "'");

		// '?' means that server refused the login.
		if (resp.length() > 0 && resp.charAt(0) == '?')
			throw new ServerError(resp);
		else if (resp.length() >= 9 && resp.substring(0,9).equals("HELLO ???"))
			throw new ServerError(resp);

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
				Logger.instance().warning(module +
					"Invalid protocol version '" + s 
					+ "' returned by server. Assuming protoVersion=3");
				serverProtoVersion = DdsVersion.version_1;
			}
		}

		Logger.instance().debug2(module + 
			"DDS Connection (" + host + ":" + port + ") Hello response '"
			+ resp+ "', protocolVersion=" + serverProtoVersion);
	}

	/**
	  Sends an Authenticated Hello message with the specified user name and
	  password. 
	  Then awaits the response from the server. If an exception is not thrown, 
	  then the login was successful.

	  @param name the user name to send to the server
	  @param passwd the password to send to the server

	  @throws ServerError if the server rejected this request. In the case
	     of the this message this means that the authentication failed.
	  @throws ProtocolError if the server response could not be parsed
	     or it was of an unexpected type.
	  @throws IOException indicates that the socket is no longer usable.
	*/
	public void sendAuthHello(String name, String passwd)
		throws ServerError, ProtocolError, IOException
	{
		// Send hello
		if (debug != null)
			debug.println("sendAuthHello("+name+")");

		Logger.instance().debug2(module + 
			"DDS Connection (" + host + ":" + port + ") sendAuthHello("
			+ name + ")");

		if (name == null || TextUtil.isAllWhitespace(name))
			throw new ServerError(
				"Client-side reject: username cannot be blank", 0, 0);

		if (passwd == null || TextUtil.isAllWhitespace(passwd))
			throw new ServerError(
				"Client-side reject: password cannot be blank", 0, 0);

		try
		{
			PasswordFileEntry pfe = new PasswordFileEntry(name, passwd);
			sendAuthHello(pfe);
		}
		catch(AuthException ex)
		{
			throw new ProtocolError("Invalid username or password: " + ex);
		}
	}

	/**
	 * Sends an authenticated hello, using a prepared password file entry.
	 * @param pfe the password file entry.
	 */
	public void sendAuthHello(PasswordFileEntry pfe)
		throws ServerError, ProtocolError, IOException
	{
		Date now = new Date();
		int timet = (int)(now.getTime() / 1000);
		AuthenticatorString auth;
		try 
		{
			auth = new AuthenticatorString(timet, pfe);

			// Construct a 1-time session key for admin functions.
			sessionKey = AuthenticatorString.makeAuthenticator(
				auth.getString().getBytes(), pfe.getShaPassword(), timet);
		}
		catch(Exception ex)
		{
			sessionKey = null;
			throw new ServerError(
				"Client-side reject: cannot build authenticator: " + ex);
		}
		String tstr = goesDateFormat.format(now);

		String body = pfe.getUsername() + " " + tstr + " " + auth.getString()
			+ " " + DdsVersion.getVersion();

		LddsMessage msg = new LddsMessage(LddsMessage.IdAuthHello, body);
		sendData(msg.getBytes());

		// Read the response message - It should be same type ID.
		msg = linput.getMessage();
		if (msg.MsgId != LddsMessage.IdAuthHello)
		{
			sessionKey = null;
			throw new ProtocolError("Unexpected response '" + 
				msg.MsgId + "' - expected '" + LddsMessage.IdAuthHello
				+ "' (auth-hello)");
		}

		String resp = ByteUtil.getCString(msg.MsgData, 0);
		if (debug != null)
			debug.println("AuthHello response '" + resp + "'");

		// '?' means that server refused the login.
		if (resp.length() > 0 && resp.charAt(0) == '?')
		{
			sessionKey = null;
			throw new ServerError(resp);
		}
		StringTokenizer st = new StringTokenizer(resp);
		if (st.countTokens() < 3)
		{
			sessionKey = null;
			throw new ProtocolError("Invalid response '" + resp 
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
			Logger.instance().warning(module +
				"Invalid protocol version '" + serverProtoVersionStr 
				+ "' returned by server. Assuming protoVersion=3");
			serverProtoVersion = DdsVersion.version_3;
		}

		Logger.instance().debug2(module + 
			"DDS Connection (" + host + ":" + port + ") AuthHello response '"
			+ resp+ "', protocolVersion=" + serverProtoVersion);
	}

	/**
	  Sends a goodbye message to server. Then waits for response.

	  @throws IOException if the socket is no longer usable.
	  @throws ProtocolError if an unexpected response was received.
	     (i.e. the wrong message type).
	*/
	public void sendGoodbye()
		throws IOException, ProtocolError
	{
		if (debug != null)
			debug.println("sendGoodbye()");

		Logger.instance().debug2(module + 
			"DDS Connection (" + host + ":" + port + ") sendGoodbye()");

		LddsMessage msg = new LddsMessage(LddsMessage.IdGoodbye, null);
		sendData(msg.getBytes());

		// Read the response message - It should be same type ID.
		msg = getAbortResponse();
		if (msg == null)
			throw new ProtocolError("No response received for GOODBYE");
		else if (msg.MsgId != LddsMessage.IdGoodbye)
			throw new ProtocolError(
				"Unexpected response '" + msg.MsgId + "'"
				+ " - Expected '" + LddsMessage.IdGoodbye + "' (Goodbye)");

		if (debug != null)
			debug.println("Goodbye action complete");
		Logger.instance().debug2(module + 
			"DDS Connection (" + host + ":" + port + ") Goodbye response OK");
	}


	/**
	  Retrieves a search criteria file from the server. The file is always
	  called 'searchcrit' on the server and is always stored in the user's
	  home directory. The filename passed to this function is the local
	  file that will be created to hold the retrieved searchcrit.
	  <p>
	  If the 'localfile' argument is left blank, a file called 'searchcrit'
	  will be created in the current directory.

	  @param localfile the name of the local file to write SC data to.

	  @throws IOException if the socket is no longer usable.
	  @throws ProtocolError if an unexpected response was received.
	     (i.e. the wrong message type).
	  @throws ServerError if the server returned an error response for
	     the request.
	*/
	public void getSearchCrit(String localfile)
		throws IOException, ProtocolError, ServerError 
	{
		if (localfile == null)
			localfile = "searchcrit";

		if (debug != null)
			debug.println("getSearchCrit("+localfile+")");
		Logger.instance().debug2(module + 
			"DDS Connection (" + host + ":" + port + ") getSearchCrit("
			+ localfile + ")");

		// Send request
		LddsMessage msg = new LddsMessage(LddsMessage.IdCriteria, "?");
		sendData(msg.getBytes());

		// Read the response message - It should be same type ID.
		msg = linput.getMessage();
		if (msg.MsgId != LddsMessage.IdCriteria)
			throw new ProtocolError(
				"Unexpected response to getSearchCrit '" + msg.MsgId + "'");

		// Validate response
		if ((char)msg.MsgData[0] == '?')
			throw new ServerError(new String(msg.MsgData));

		if (msg.MsgData.length < 50)
				throw new ProtocolError("No searchcrit data returned");

		// Save data.
		if (debug != null)
			debug.println("Saving " + (msg.MsgData.length - 50) 
				+ " bytes to " + localfile);

		Logger.instance().debug2(module + 
			"DDS Connection (" + host + ":" + port + ") Saving "
			+ (msg.MsgData.length - 50) + " bytes to " + localfile);

		File file = new File(localfile);
		FileOutputStream fos = new FileOutputStream(file);
		fos.write(ArrayUtil.getField(msg.MsgData, 50, msg.MsgData.length-50));
	}

	/**
	  Sends a search criteria file to the server. The file is always
	  called 'searchcrit' on the server and is always stored in the user's
	  sandbox directory. The filename passed to this function is the local
	  file that will be read and transferred to the server.
	  <p>
	  If the 'localfile' argument is left blank, a file called 'searchcrit'
	  will be read from the current directory.

	  @param localfile the name of the local sc file to send.

	  @throws IOException if either the socket is no longer usable, or
		that the specified localfile could not be read.
	  @throws ProtocolError if an unexpected response was received.
	     (i.e. the wrong message type).
	  @throws ServerError if the server returned an error response for
	     the request.
	*/
	public void sendSearchCrit(String localfile)
		throws IOException, ProtocolError, ServerError
	{
		if (localfile == null)
			localfile = "searchcrit";

		if (debug != null)
			debug.println("sendSearchCrit("+localfile+")");

		Logger.instance().debug2(module + 
			"DDS Connection (" + host + ":" + port + ") sendSearchCrit("
			+ localfile + ")");

		// Read contents of specified searchcrit file into memory.
		File f = new File(localfile);
		try 
		{
			SearchCriteria sc = new SearchCriteria(f); 
			sendSearchCrit(sc);
		}
		catch(SearchSyntaxException ex)
		{
			throw new IOException("Bad SearchCriteria format: " + ex);
		}
	}
	
	/**
	  Sends search criteria from an existing SearchCriteria object rather
	  than a file.

	  <p>
	  New feature in LRGS 6: if the LRGS_SINCE field contains a string of
	  the form filetime(filename), then this method attempts to find the
	  local file, get its last-modify-time, and send that to the server.
	  If the file does not exist locally, it is equivalent to no SINCE
	  time being sent.

	  @param searchcrit the SearchCriteria object to send to the server
	  @throws IOException if either the socket is no longer usable.
	  @throws ProtocolError if an unexpected response was received.
	     (i.e. the wrong message type).
	  @throws ServerError if the server returned an error response for
	     the request.
	 */
	public void sendSearchCrit(SearchCriteria searchcrit)
		throws IOException, ProtocolError, ServerError
	{
		if (debug != null)
			debug.println("sendSearchCrit(OBJECT)");

		// Make a copy, preprocess, and send the copy.
		SearchCriteria toSend = new SearchCriteria(searchcrit);
		
		// If single-mode then set in search-crit.
		// Thus for a V11 server, we will still get expanded-mode data.
		if (!multiMessageModeEnabled)
			toSend.single = true;
		
		// Expand filetime for since, if used.
		String st = toSend.getLrgsSince();
		if (st != null)
		{
			st = st.trim();
			if (TextUtil.startsWithIgnoreCase(st,"filetime("))
			{
				st = st.substring(9);
				int idx = st.indexOf(")");
				if (idx != -1)
					st = st.substring(0, idx);
				String fn = EnvExpander.expand(st);
				File f = new File(fn);
				long t;
				if (!f.exists()
				 || (t = f.lastModified()) <= 0)
					toSend.setLrgsSince(null);
				else
				{
					toSend.setLrgsSince(
						IDateFormat.toString(new Date(t-60000L), false));
				}
			}
		}

		// MJM - The following added in Nov, 2007. Try to convert the
		// DCP name to an address before sending to the server. This goes
		// with the strategy of trying to void persistent client context
		// on the server.
		// Try to find a matching site name in the DECODES database.
		if (toSend.DcpNames != null && toSend.DcpNames.size() > 0
		 && Database.getDb() != null
		 && Database.getDb().platformList != null)
		{
			PlatformList platList = Database.getDb().platformList;
			for (Iterator<String> nmIt = toSend.DcpNames.iterator(); nmIt.hasNext(); )
			{
				String nm = nmIt.next();
				Platform plat = platList.getByFileName(nm);
				if (plat == null)
					plat = platList.getBySiteNameValue(nm);
				if (plat != null)
				{
					String dcpaddr = plat.getDcpAddress();
					if (dcpaddr != null)
					{
						toSend.addDcpAddress(new DcpAddress(dcpaddr));
						nmIt.remove();
					}
				}
			}
		}
		
		
		implicitAllUsed = false;
		// MJM 2008 09/25 Implement the implicit <all> and <production> network
		// lists. If these are used, build a dynamic list from the DECODES
		// platform list containing all non-expired GOES IDs.
		for(int i = 0; i<toSend.NetlistFiles.size(); i++)
		{
			String nlname = toSend.NetlistFiles.get(i);
			if (nlname.equalsIgnoreCase("<all>")
			 || nlname.equalsIgnoreCase("<production>"))
			{
				implicitAllUsed = true;
				boolean productionOnly = nlname.equalsIgnoreCase("<production>");
				decodes.db.Database db = decodes.db.Database.getDb();
				if (db == null)
				{
					toSend.NetlistFiles.remove(i--);
					continue;
				}
				NetworkList nl = new NetworkList();
				for(Platform plat : db.platformList.getPlatformVector())
				{
					if (plat.expiration != null)
						continue;
					if (productionOnly && !plat.isProduction)
						continue;
					for(TransportMedium tm : plat.transportMedia)
						if (tm.isGoes() 
						 || tm.getMediumType().equalsIgnoreCase(Constants.medium_IRIDIUM))
						{
							DcpAddress addr = new DcpAddress(tm.getMediumId());
							if (!nl.containsDcpAddr(addr))
								nl.add(new NetworkListItem(addr, 
									plat.getDisplayName(), 
									plat.getDescription()));
						}
				}
				String listname = productionOnly ? 
					"decodes_production.nl" : "decodes_all.nl";
				sendNetList(nl, listname);
				toSend.NetlistFiles.set(i, listname);
			}
		}
		Logger.instance().debug2(module + 
			"DDS Connection (" + host + ":" + port + ") sendSearchCrit(OBJECT)");
		
		// If there are too many explicit DCP addresses, convert them to
		// temporary network lists and send them first.
		int listnum = 1;
		while(toSend.ExplicitDcpAddrs.size() > 500)
		{
			NetworkList tmpNl = new NetworkList();
			for(int idx = 0; idx < 500; idx++)
				tmpNl.add(new NetworkListItem(toSend.ExplicitDcpAddrs.get(idx),"",""));
			String nlName = "searchcrit_" + (listnum++) + ".nl";
			sendNetList(tmpNl, nlName);
			toSend.addNetworkList(nlName);
			ArrayList<DcpAddress> tl = new ArrayList<DcpAddress>(toSend.ExplicitDcpAddrs.size()-500);
			for(int idx = 500; idx < toSend.ExplicitDcpAddrs.size(); idx++)
				tl.add(toSend.ExplicitDcpAddrs.get(idx));
			toSend.ExplicitDcpAddrs = tl;
		}

		// Put searchcrit into a byte array.
		byte data[] = toSend.toString(serverProtoVersion).getBytes();
		searchCritLocalFilter = toSend.getSearchCritLocalFilter();
		sendSearchCrit("OBJECT", data);
	}

	/**
	  Sends search criteria from an in-memory byte array.
	  @param filename the name of the searchcrit to pass to the server
	  @param data the search criteria data
	  @throws IOException if either the socket is no longer usable.
	  @throws ProtocolError if an unexpected response was received.
	     (i.e. the wrong message type).
	  @throws ServerError if the server returned an error response for
	     the request.
	 */
	private void sendSearchCrit(String filename, byte data[])
		throws IOException, ProtocolError, ServerError
	{
		// Construct an empty criteria message big enought for this file.
		LddsMessage msg = new LddsMessage(LddsMessage.IdCriteria, "");
		msg.MsgLength = data.length + 50;
		msg.MsgData = new byte[msg.MsgLength];

		// Create the 'header' portion containing the searchcrit filename.
		// (First 40 bytes is filename)
		int i;
		for(i = 0; i<40 && i <filename.length(); i++)
			msg.MsgData[i] = (byte)filename.charAt(i);
		msg.MsgData[i] = (byte)0;

		// Copy the file data into the message & send it out.
		for(i=0; i<data.length; i++)
			msg.MsgData[i+50] = data[i];

		if (debug != null)
			debug.println("Sending criteria message (filesize = " +
				data.length + " bytes)");

		Logger.instance().debug2(module + 
			"DDS Connection (" + host + ":" + port + ") "
			+ "Sending criteria message (filesize = " +
				data.length + " bytes)");

		sendData(msg.getBytes());

		// Get response.
		msg = linput.getMessage();
		if (msg.MsgId != LddsMessage.IdCriteria)
			throw new ProtocolError("Unexpected responses '" + msg.MsgId + "'");
		else if (msg.MsgLength > 0 && (char)msg.MsgData[0] == '?')
		{
			String s = new String(msg.MsgData);
			throw new ServerError(s);
		}

		if (debug != null)
			debug.println("Successfully sent searchcrit.");
		Logger.instance().debug2(module + 
			"DDS Connection (" + host + ":" + port + ") "
			+ "Successfully sent searchcrit.");

		// MJM 20080619 - Just sent new crit, clear any buffered messages.
		flushBufferedMessages();
	}
	 

	/**
	 * Requests the next DCP message from the server. 
	 * If a search criteria file has previously been transfered
	 * (either to or from the server), then the server will only pass messages
	 * that meet the specified criteria.
	 * <p>
	 * The timeout argument specifies the number of seconds to wait for a
	 * response from the server.
	 * <p>
	 * Returns a DcpMsg or null if no response was received from the server
	 * within the specified # of seconds.
	 * <p>
	 * @param timeout number of seconds to wait
	 * @throws IOException if socket is no longer usable.
	 * @throws ProtocolError if an unexpected response was received.
	 * (i.e. the wrong message type).
	 * @throws ServerError with Derrno equal to 11 (DMSGTIMEOUT) if
	 * no message arrived at the server within the specified timeout 
	 * period. This typically indicates that you are already caught-up
	 * to the present time.
	 * @throws ServerError with other Derrno codes to indicate other errors 
	 * on the server which are not recoverable. When this happens, call
	 * disconnect() and then start the session over.
	*/
	public DcpMsg getDcpMsg(int timeout)
		throws IOException, ProtocolError, ServerError
	{
		if (!this.multiMessageModeEnabled)		//Single mode
		{
			return getDcpMsgSingle(timeout);
		}
		else if (this.multiMessageModeEnabled && serverProtoVersion >= DdsVersion.version_11)		
		{
			//Proto V11, single mode is sent in searchcrit.
			return getDcpMsgExt(timeout);
		}
		else if (this.extMessageModeEnabled && serverProtoVersion >= DdsVersion.version_8)
		{
			return getDcpMsgExt(timeout);
		}
		else if (this.multiMessageModeEnabled && serverProtoVersion >= DdsVersion.version_5)
		{
			return getDcpMsgMulti(timeout);
		}
		else
		{
			return getDcpMsgSingle(timeout);
		}
	}

	/**
	  Gets the next message from the server (single-mode).
	  Requests the next DCP message from the server. 
	  If a search criteria file has previously been transfered
	  (either to or from the server), then the server will only pass messages
	  that meet the specified criteria.
	  <p>
	  The timeout argument specifies the number of seconds to wait for a
	  response from the server.
	  <p>
	  @param timeout number of seconds to wait

	  @return a DcpMsg or null if no response was received from the server
	  within the specified # of seconds.

	  @throws IOException if socket is no longer usable.
	  @throws ProtocolError if an unexpected response was received.
	  (i.e. the wrong message type).
	  @throws ServerError with Derrno equal to 11 (DMSGTIMEOUT) if
	  no message arrived at the server within the specified timeout 
	  period. This typically indicates that you are already caught-up
	  to the present time.
	  @throws ServerError with other Derrno codes to indicate other errors 
	  on the server which are not recoverable. When this happens, call
	  disconnect() and then start the session over.
	*/
	public DcpMsg getDcpMsgSingle(int timeout)
		throws IOException, ProtocolError, ServerError
	{
		boolean requestAgain = true;
		DcpMsg ret = null;
		while(requestAgain)
		{
			requestDcpMsg();
			ret = receiveDcpMsg(timeout);
			if (ret == null)
				ret = abortDcpMsgRequest();

			// If we have a local filter (i.e. we are talking to a legacy
			// server and must do some of the filtering locally)...
			requestAgain = ret != null
				&& searchCritLocalFilter != null
				&& !searchCritLocalFilter.passesCrit(ret);
		}
				
		return ret;
	}

	/**
	  Most clients can call the synchronous getDcpMsg() function. If you
	  need the ability to abort a request, call the requestDcpMsg method
	  followed by the receiveDcpMsg method.

	  Request the next DCP message from the server. If your client process
	  needs the ability to abort a request in progress, you will have to
	  call requestDcpMsg followed by repeated calls to receiveDcpMsg.

	  @throws IOException if the socket is no longer usable.
	*/
	public void requestDcpMsg() throws IOException
	{
		if (debug != null)
			debug.println("Requesting DCP Message...");

		Logger.instance().debug2(module + 
			"DDS Connection (" + host + ":" + port + ") "
			+ "Requesting DCP Message...");

		// Request Dcp Message
		LddsMessage msg = new LddsMessage(LddsMessage.IdDcp, "");
		sendData(msg.getBytes());
	}

	/**
	 * Receives a DCP Message after calling requestDcpMsg.
	 * If a message is received within timeout seconds, it will be returned. 
	 * Otherwise null will be returned.
	 * <p>
	 * Note: One message is returned by the server for each request. Once
	 * a message has been received you need to call requestDcpMsg again
	 * before calling this method.
	 * <p>
	 * @throws IOException if the socket is no longer usable.
	 * @throws ProtocolError if an unexpected response was received.
	 * (i.e. the wrong message type).
	 * @throws ServerError with a Derrno equal to (DUNTIL) if the specified
	 * until time was reached.
	 * @throws ServerError with other codes  for other errors on the server
	 * (probably not recoverable).
	*/
	public DcpMsg receiveDcpMsg(int timeout)
		throws IOException, ProtocolError, ServerError
	{
		// Check for asynchronous disconnection.
		if (linput == null)
			return null;

		socket.setSoTimeout((timeout+1)*1000);

		// Get response.
		LddsMessage msg = linput.getMessage();
		if (msg.MsgId != LddsMessage.IdDcp)
			throw new 
				ProtocolError("Unexpected responses '" + msg.MsgId + "'");
		if (msg.MsgData[0] == (byte)'?')
			throw new ServerError(new String(msg.MsgData));

		return lddsMsg2DcpMsg(msg);
	}

	private DcpMsg lddsMsg2DcpMsg(LddsMessage msg)
		throws ProtocolError
	{
		if (msg.MsgLength < 40 + DcpMsg.DCP_MSG_MIN_LENGTH)
		{
			if (debug != null)
			{
				debug.println("Too-Short DCP message response: id='" 
					+ msg.MsgId + "' length=" + msg.MsgLength);
				if (msg.MsgLength > 40)
				{
					String d = new String(ArrayUtil.getField(msg.MsgData,
						40, msg.MsgLength - 40));
					debug.println("data: " + d);
				}
			}

			Logger.instance().warning(module +
				"DDS Connection (" + host + ":" + port + ") "
				+ "Too-Short DCP message response: id='" 
				+ msg.MsgId + "' length=" + msg.MsgLength + " -- skipped.");

			if (msg.MsgLength > 40)
			{
				String d = new String(
					ArrayUtil.getField(msg.MsgData, 40, msg.MsgLength - 40));
				Logger.instance().warning(module +
					"Complete response '" + d + "'");
			}

			throw new ProtocolError("Too-Short DCP message response received");
		}
//		DcpMsg ret = 
//			new DcpMsg(ArrayUtil.getField(msg.MsgData, 40, msg.MsgLength-40),
//				msg.MsgLength - 40);
		DcpMsg ret = new DcpMsg(msg.MsgData, msg.MsgLength-40, 40);
		ret.flagbits = DcpMsgFlag.MSG_PRESENT |  DcpMsgFlag.SRC_DDS
			| DcpMsgFlag.MSG_NO_SEQNUM;
		// Turn off FORCE save. Messages received from DDS are never FORCE_SAVE.
		ret.setFlagbits(ret.getFlagbits() & (~DcpMsgFlag.FORCE_SAVE));

		// Get sequence filename from first 40 bytes of message.
		ret.setSeqFileName(ByteUtil.getCString(msg.MsgData, 0));
		if (debug != null)
		{
			debug.println("Response received: fn = " + 
				ret.getSeqFileName() + ", length = " + (msg.MsgLength - 40));
		}
		Logger.instance().debug2(module + 
			"DDS Connection (" + host + ":" + port + ") "
			+ "Response received: fn = " + 
				ret.getSeqFileName() + ", length = " + (msg.MsgLength - 40));

		return ret;
	}

	/**
	  In order to be able to abort DCP message requests. You will need to
	  first call requestDcpMsg(). Then in a loop, repeatedly call 
	  receiveDcpMsg with a brief timeout. When you want to stop, call
	  abortDcpMsgRequest(). This tells the server to abort the request.

	  A race condition exists. The server might be just sending the
	  Dcp message response when I send the abort. If this happens, this
	  method will return the DCP message. Otherwise, null is returned.

	  Failure to abort an outstanding DCP Message request will render the
	  socket unusable for other operations.

	  @throws IOException if the socket is no longer usable.
	  @throws ProtocolError if an unexpected response was received.
		(i.e. the wrong message type).
	*/
	public DcpMsg abortDcpMsgRequest()
		throws IOException, ProtocolError
	{
		if (debug != null)
			debug.println("abortDcpMsgRequest()");
		Logger.instance().debug2(module + 
			"DDS Connection (" + host + ":" + port + ") "
			+ "abortDcpMsgRequest()");

		if (linput == null)
			return null;

		LddsMessage msg = new LddsMessage(LddsMessage.IdStop, null);
		sendData(msg.getBytes());

		msg = getAbortResponse();

		// There is a race condition here. The server may have just
		// sent the message out when I sent it an abort. If the message I
		// just read is a DCP message return it to the client.
		DcpMsg ret = null;
		if (msg != null && msg.MsgId == LddsMessage.IdDcp)
		{
			ret = lddsMsg2DcpMsg(msg);

			// Now read the response to the Abort request.
			msg = getAbortResponse();
		}

		if (msg == null)
			throw new ProtocolError(
				"No response to abort request received");
		else if (msg.MsgId != LddsMessage.IdStop)
			throw new ProtocolError(
				"Unexpected response '" + msg.MsgId + "'"
				+ " - Expected '" + LddsMessage.IdStop + "' (Stop)");

		return ret;
	}

	private LddsMessage getAbortResponse()
		throws IOException, ProtocolError
	{
		// Wait up to 10 seconds for the response to the abort.
		// If none received, we'll just abort anyway.
		socket.setSoTimeout(10000);

		return linput.getMessage();
	}

	/**
	  The getNetList method retrieves a network list file from the server.
	  The first argument (serverfile) specifies the name of the network 
	  list file on the server. The second argument (localfile) specifies
	  the name of the file on the local system that will be created.

	  @param serverfile name of network list on the server
	  @param localfile name of network list on the local computer

	  @throws IOException if a network socket error or local file error.
	  @throws ProtocolError if an unexpected response was received.
	     (i.e. the wrong message type).
	  @throws ServerError if the server responded to the request with an
	     error message. This most likely means that that the requested 
	     network list file does not exist on the server.

	*/
	public void getNetList(String serverfile, File localfile)
		throws IOException, ProtocolError, ServerError
	{
		if (debug != null)
			debug.println("getNetList("+serverfile+ ", " 
				+ localfile.getName() + ")");
		Logger.instance().debug2(module + 
			"DDS Connection (" + host + ":" + port + ") "
			+ "getNetList("+serverfile+ ", " + localfile.getName() + ")");

		// Send request
		LddsMessage msg = new LddsMessage(LddsMessage.IdGetNetlist, 
			serverfile);
		sendData(msg.getBytes());

		// Read the response message - It should be same type ID.
		msg = linput.getMessage();
		if (msg.MsgId != LddsMessage.IdGetNetlist)
			throw new ProtocolError("Unexpected responses '" + msg.MsgId + "'");

		// Validate response
		if ((char)msg.MsgData[0] == '?')
			throw new ServerError(new String(msg.MsgData));
		if (msg.MsgLength < 64)
			throw new ServerError("?0,0,No file data returned");

		// Save data.
		if (debug != null)
			debug.println("Saving " + (msg.MsgLength - 64) 
				+ " bytes to " + localfile.getName());
		Logger.instance().debug2(module + 
			"DDS Connection (" + host + ":" + port + ") "
			+ "Saving " + (msg.MsgLength - 64) 
				+ " bytes to " + localfile.getName());

		FileOutputStream fos = new FileOutputStream(localfile);
		fos.write( ArrayUtil.getField(msg.MsgData, 64, msg.MsgData.length-64));
		fos.close();
	}

	/**
	  sends a network list file to the server.

	  @param serverfile name of network list on the server
	  @param localfile name of network list on the local computer

	  @throws IOException on network socket error or local file error.
	  @throws ProtocolError if an unexpected response was received.
	     (i.e. the wrong message type).
	  @throws ServerError if the server responded to the request with an
	     error message. This most likely means that that the named
	     file could not be created on the server.
	*/
	public void sendNetList(File localfile, String serverfile)
		throws IOException, ProtocolError, ServerError
	{
		if (debug != null)
			debug.println("sendNetList("+localfile.getName()
				+", " + serverfile + ")");
		Logger.instance().debug2(module + 
			"DDS Connection (" + host + ":" + port + ") "
			+ "sendNetList("+localfile.getName() +", " + serverfile + ")");

		// Read contents of specified searchcrit file into memory.
		FileInputStream fis = new FileInputStream(localfile);
		byte data[] = new byte[(int)localfile.length()];
		fis.read(data);
		fis.close();

		// Construct an empty criteria message big enought for this file.
		LddsMessage msg = new LddsMessage(LddsMessage.IdPutNetlist, "");
		msg.MsgLength = data.length + 64;
		msg.MsgData = new byte[msg.MsgLength];

		// Create the 'header' portion, which contains 64 char filename
		int i;
		for(i = 0; i<64 && i <serverfile.length(); i++)
			msg.MsgData[i] = (byte)serverfile.charAt(i);
		msg.MsgData[i] = (byte)0;

		// Copy the file data into the message & send it out.
		for(i=0; i<data.length; i++)
			msg.MsgData[i+64] = data[i];
		if (debug != null)
			debug.println("Sending netlist message (total length=" +
				msg.MsgLength + ")");
		Logger.instance().debug2(module + 
			"DDS Connection (" + host + ":" + port + ") "
			+ "Sending netlist message (total length=" +
				msg.MsgLength + ")");

		sendData(msg.getBytes());

		// Get response.
		msg = linput.getMessage();
		if (msg.MsgId != LddsMessage.IdPutNetlist)
			throw new ProtocolException("Unexpected responses '" + 
				msg.MsgId + "'");
		else if ((char)msg.MsgData[0] == '?')
			throw new ServerError(new String(msg.MsgData));
		Logger.instance().debug2(module + 
			"DDS Connection (" + host + ":" + port + ") Netlist response OK");
	}

	/**
	  sends a network list object to the server.

	  @param netlist the NetworkList object to send

	  @throws IOException if network socket error.
	  @throws ProtocolError if an unexpected response was received.
	     (i.e. the wrong message type).
	  @throws ServerError if the server responded to the request with an
	     error message. This most likely means that that the named
	     file could not be created on the server.
	*/
	public void sendNetList(NetworkList netlist, String svrListName)
		throws IOException, ProtocolError, ServerError
	{
		if (debug != null)
			debug.println("sendNetList from NetworkList object in memory");
		Logger.instance().debug2(module + 
			"DDS Connection (" + host + ":" + port + ") "
			+ "sendNetList from NetworkList object in memory");

		// Construct an empty criteria message big enought for this file.
		LddsMessage msg = new LddsMessage(LddsMessage.IdPutNetlist, "");
		String data = netlist.toFileString();
		byte databytes[] = data.getBytes();
		msg.MsgLength = databytes.length + 64;
		msg.MsgData = new byte[msg.MsgLength];


		// Create the 'header' portion, which contains 64 char filename
		String name = svrListName;
		if (name == null)
			name = "list-in-memory";
		if (netlist.file != null)
			name = netlist.file.getName();

		byte namebytes[] = name.getBytes();
		int i;
		for(i = 0; i<63 && i < namebytes.length; i++)
			msg.MsgData[i] = namebytes[i];
		msg.MsgData[i] = (byte)0;

		// Copy the file data into the message & send it out.
		for(i=0; i<databytes.length; i++)
			msg.MsgData[i+64] = databytes[i];

		if (debug != null)
			debug.println("Sending netlist message (total length=" +
				msg.MsgLength + ")");
		Logger.instance().debug2(module + 
			"DDS Connection (" + host + ":" + port + ") "
			+ "Sending netlist message (total length=" + msg.MsgLength + ")");
		sendData(msg.getBytes());

		// Get response.
		msg = linput.getMessage();
		if (msg.MsgId != LddsMessage.IdPutNetlist)
			throw new ProtocolException("Unexpected responses '" + 
				msg.MsgId + "'");
		else if ((char)msg.MsgData[0] == '?')
			throw new ServerError(new String(msg.MsgData));
		Logger.instance().debug2(module + 
			"DDS Connection (" + host + ":" + port + ") netlist response OK");
	}


	/**
	 * Requests the next block of DCP messages from the server. 
	 * If a search criteria file has previously been transfered
	 * (either to or from the server), then the server will only pass messages
	 * that meet the specified criteria.
	 * <p>
	 * The timeout argument specifies the number of seconds to wait for a
	 * response from the server.
	 * <p>
	 * Returns an array DcpMsg objects, or null if no response was received 
	 * from the server within the specified # of seconds.
	 * <p>
	 * @param timeout number of seconds to wait

	 * @throws IOException if socket is no longer usable.
	 * @throws ProtocolError if an unexpected response was received.
	 * (i.e. the wrong message type).
	 * @throws ServerError with Derrno equal to 11 (DMSGTIMEOUT) if
	 * the server indicates that you are already caught-up to the present
	 * time. When this happens, pause and try again later.
	 * @throws ServerError with other Derrno codes to indicate other errors 
	 * on the server which are not recoverable. When this happens, call
	 * disconnect() and then start the session over.
	*/
	public DcpMsg[] getDcpMsgBlock(int timeout)
		throws IOException, ProtocolError, ServerError
	{
		requestDcpMsgBlock();
		DcpMsg ret[] = receiveDcpMsgBlock(timeout);
		if (ret == null)
			ret = abortDcpMsgBlockRequest();
		return ret;
	}

	public void requestDcpMsgBlock() throws IOException
	{
		if (debug != null)
			debug.println("Requesting DCP Message Block...");

		Logger.instance().debug2(module + 
			"DDS Connection (" + host + ":" + port + ") "
			+ "Requesting DCP Message Block...");

		// Request Dcp Message
		LddsMessage msg = new LddsMessage(LddsMessage.IdDcpBlock, "");
		sendData(msg.getBytes());
	}

	/**
	 * Receives a DCP Message Block after calling requestDcpMsg.
	 * The block is parsed into an array of DcpMsg objects and returned.
	 * Returns null if no response is received from server in timeout seconds.
	 * <p>
	 * @throws IOException if the socket is no longer usable.
	 * @throws ProtocolError if an unexpected response was received.
	 * (i.e. the wrong message type).
	 * @throws ServerError with a Derrno equal to (DUNTIL) if the specified
	 * until time was reached.
	 * @throws ServerError with other codes  for other errors on the server
	 * (probably not recoverable).
	*/
	public DcpMsg[] receiveDcpMsgBlock(int timeout)
		throws IOException, ProtocolError, ServerError
	{
		// Check for asynchronous disconnection.
		if (linput == null)
		{
			Logger.instance().debug2(module + 
				"DDS Connection (" + host + ":" + port + ") "
				+ "receiveDcpMsgBlock asynchronous disconect, aborting.");
			return null;
		}

		socket.setSoTimeout((timeout+1) * 1000);

		// Get response.
		LddsMessage msg = linput.getMessage();
		if (msg.MsgId != LddsMessage.IdDcpBlock)
			throw new 
				ProtocolError("Unexpected response '" + msg.MsgId 
					+ "', expected type '" + LddsMessage.IdDcpBlock + "'");

		if (msg.MsgData[0] == (byte)'?')
		{
			String err = new String(msg.MsgData);
			Logger.instance().debug2(module + 
				"DDS Connection (" + host + ":" + port + ") ServerError: "
				+ err);
			throw new ServerError(err);
		}

		Logger.instance().debug2(module + 
			"DDS Connection (" + host + ":" + port + ") "
			+ "received block response, length=" + msg.MsgLength);

		return lddsMsg2DcpMsgBlock(msg);
	}

	private DcpMsg[] lddsMsg2DcpMsgBlock(LddsMessage msg)
		throws ProtocolError
	{
		Logger.instance().debug3(module + 
			"Parsing block response. Total length = " + msg.MsgLength);

		// Got response, parse it into an array of DCP Messages.
		ArrayList<DcpMsg> v = new ArrayList<DcpMsg>();
		boolean garbled = false;
		int msgnum = 0;
		for(int msgStart=0; msgStart<msg.MsgLength && !garbled; msgnum++)
		{
			if (msg.MsgLength - msgStart < DcpMsg.DCP_MSG_MIN_LENGTH)
			{
				Logger.instance().failure(
					"DDS Connection (" + host + ":" + port
					+ ") Response to IdDcpBlock incomplete. "
					+ "Need at least 37 bytes. Only have "
					+ (msg.MsgLength - msgStart) + " at location "
					+ msgStart);
				Logger.instance().failure("Response='" 
					+ new String(msg.MsgData, msgStart,msg.MsgLength-msgStart)
					+ "'");
				garbled = true;
			}
			int msglen = 0;
			try 
			{
				msglen = ByteUtil.parseInt(msg.MsgData,
					msgStart + DcpMsg.IDX_DATALENGTH, 5);
			}
			catch(NumberFormatException nfe)
			{
				String lenfield = new String(ArrayUtil.getField(msg.MsgData, 
					msgStart + DcpMsg.IDX_DATALENGTH, 5));
				Logger.instance().failure(module + 
					"DDS Connection (" + host + ":" + port
					+ ") Response to IdDcpBlock contains bad length field '"
					+ lenfield 
					+ "' requires a 5-digit 0-filled integer, msgnum="
					+ msgnum + ", msgStart=" + msgStart);
				garbled = true;
			}

			int numbytes = DcpMsg.DCP_MSG_MIN_LENGTH + msglen;
//			if (numbytes > msg.MsgData.length - msgStart)
//				numbytes = msg.MsgData.length - msgStart;

			DcpMsg dcpMsg = new DcpMsg(msg.MsgData, numbytes, msgStart);
			dcpMsg.flagbits = DcpMsgFlag.MSG_PRESENT |  DcpMsgFlag.SRC_DDS
				| DcpMsgFlag.MSG_NO_SEQNUM;
			
			// Turn off FORCE save. Messages received from DDS are never FORCE_SAVE.
			dcpMsg.setFlagbits(dcpMsg.getFlagbits() & (~DcpMsgFlag.FORCE_SAVE));

			v.add(dcpMsg);

			//Logger.instance().debug3(module + 
			//	"Extracted DCP Msg '" + dcpMsg.getHeader() + "' from block.");
			msgStart += (DcpMsg.DCP_MSG_MIN_LENGTH + msglen);
		}

		Logger.instance().debug2(module + 
			"Message Block Response contained " + v.size() + " dcp msgs.");

		if (v.size() == 0)
			return null;
		else
		{
			DcpMsg ret[] = new DcpMsg[v.size()];
			return v.toArray(ret);
		}
	}
	

	private DcpMsg[] abortDcpMsgBlockRequest()
		throws IOException, ProtocolError
	{
		if (debug != null)
			debug.println("abortDcpMsgBlockRequest()");
		Logger.instance().debug2(module + 
			"DDS Connection (" + host + ":" + port + ") "
			+ "abortDcpMsgBlockRequest()");

		if (linput == null)
			return null;

		LddsMessage msg = new LddsMessage(LddsMessage.IdStop, null);
		sendData(msg.getBytes());

		msg = getAbortResponse();

		// There is a race condition here. The server may have just
		// sent the message out when I sent it an abort. If the message I
		// just read is a DCP message return it to the client.
		DcpMsg ret[] = null;
		if (msg != null && msg.MsgId == LddsMessage.IdDcpBlock)
		{
			ret = lddsMsg2DcpMsgBlock(msg);

			// Now read the response to the Abort request.
			msg = getAbortResponse();
		}

		if (msg == null)
			throw new ProtocolError(
				"No response to abort request received");
		else if (msg.MsgId != LddsMessage.IdStop)
			throw new ProtocolError(
				"Unexpected response '" + msg.MsgId + "'"
				+ " - Expected '" + LddsMessage.IdStop + "' (Stop)");

		return ret;
	}

	/**
	  Get the next DCP message using multi-mode.
	
	  @param timeout number of seconds to wait
	  @return DcpMsg object 
	*/
	private DcpMsg getDcpMsgMulti(int timeout)
		throws IOException, ProtocolError, ServerError
	{
		if (multiMsg == null || multiMsgIdx >= multiMsg.length)
		{
			// get next block of data
			int numFiltered = 0;
			do
			{
				multiMsgIdx = 0;
				multiMsg = getDcpMsgBlock(timeout);
				if (multiMsg == null)
					throw new ProtocolError(
						"Timeout waiting for block response from server.");
				// If we have a local filter (i.e. we are talking to a legacy
				// server and must do some of the filtering locally)...
				if (searchCritLocalFilter != null)
				{
					numFiltered = 0;
					for(int idx = 0; idx < multiMsg.length; idx++)
						if (!searchCritLocalFilter.passesCrit(multiMsg[idx]))
						{
							multiMsg[idx] = null;
							numFiltered++;
						}
					if (numFiltered > 0)
					{
						DcpMsg fret[] = new DcpMsg[multiMsg.length - numFiltered];
						int fidx = 0;
						for(int idx = 0; idx < multiMsg.length; idx++)
							if (multiMsg[idx] != null)
								fret[fidx++] = multiMsg[idx];
						multiMsg = fret;
					}
				}
			} while(multiMsg != null && multiMsg.length == 0 && 
				searchCritLocalFilter != null && numFiltered > 0);
		}
		if (multiMsg != null && multiMsgIdx < multiMsg.length)
			return multiMsg[multiMsgIdx++];
		else
			return null;
	}

	/**
	 * @return the number of messages already received and buffered.
	 */
	public int getNumberBuffered()
	{
		if (multiMsg != null)
			return multiMsg.length - multiMsgIdx;
		else
			return 0;
	}

	/**
	 * Deletes any buffered messages, forcing next retrieve to send request 
	 * to server.
	 */
	public void flushBufferedMessages()
	{
		multiMsgIdx = 0;
		multiMsg = null;
	}
	
	/**
	  Retrieves the status from the server.
	  @return byte array containing an XML block representing the server's
	  current status.
	*/
	public byte[] getStatus()
		throws IOException, ProtocolError, ServerError 
	{
		if (debug != null)
			debug.println("getStatus");
		Logger.instance().debug2(module + 
			"DDS Connection (" + host + ":" + port + ") getStatus");

		// Send request
		LddsMessage msg = new LddsMessage(LddsMessage.IdStatus, "?");
		sendData(msg.getBytes());

		// Read the response message - It should be same type ID.
		msg = linput.getMessage();
		if (msg.MsgId != LddsMessage.IdStatus)
			throw new ProtocolError(
				"Unexpected response to getStatus '" + msg.MsgId + "'");

		// Validate response
		if ((char)msg.MsgData[0] == '?')
			throw new ServerError(new String(msg.MsgData));

		// Some servers throw a null byte at the end. If so, convert to LF.
		if (msg.MsgData[msg.MsgData.length-1] == 0)
			msg.MsgData[msg.MsgData.length-1] = (byte)'\n';

		return msg.MsgData;
	}

	/**
	  Retrieve the next block of events from the server.
	  return an array of Strings, each represeting a single formated text
	  event.
	  @return array of event strings 
	*/
	public String[] getEvents()
		throws IOException, ProtocolError, ServerError 
	{
		if (serverProtoVersion < DdsVersion.version_6)
			return new String[0];

		if (debug != null)
			debug.println("getEvent");
		Logger.instance().debug2(module + 
			"DDS Connection (" + host + ":" + port + ") getEvents");

		// Send request
		LddsMessage msg = new LddsMessage(LddsMessage.IdEvents, "?");
		sendData(msg.getBytes());

		// Read the response message - It should be same type ID.
		msg = linput.getMessage();
		if (msg.MsgId != LddsMessage.IdEvents)
			throw new ProtocolError(
				"Unexpected response to getEvents '" + msg.MsgId + "'");

		// Validate response
		if (msg.MsgData.length == 0)
			return new String[0];

		if ((char)msg.MsgData[0] == '?')
			throw new ServerError(new String(msg.MsgData));

		// Some servers throw a null byte at the end. If so, convert to space.
		if (msg.MsgData[msg.MsgData.length-1] == 0)
			msg.MsgData[msg.MsgData.length-1] = (byte)' ';

		int numEvents = 0;
		for(int i=0; i < msg.MsgData.length; i++)
			if (msg.MsgData[i] == (byte)'\n')
				numEvents++;
		if (numEvents == 0)
			return null;

		String ret[] = new String[numEvents];
		int num = 0;
		int sidx = 0;
		for(int i=0; i<msg.MsgData.length; i++)
		{
			if (msg.MsgData[i] == (byte)'\n')
			{
				ret[num++] = new String(msg.MsgData, sidx, i-sidx);
				sidx = i+1;
			}
		}

		return ret;
	}

	/**
	  Retrieves a configuration file from the server.

	  @param cfgType one of "lrgs", "ddsrecv", or "drgs"

	  @throws IOException if a network socket error or local file error.
	  @throws ProtocolError if an unexpected response was received.
	     (i.e. the wrong message type).
	  @throws ServerError if the server responded to the request with an
	     error message. This most likely means that that the requested 
	     network list file does not exist on the server.

	*/
	public byte[] getConfig(String cfgType)
		throws IOException, ProtocolError, ServerError
	{
		if (debug != null)
			debug.println("getConfig(" + cfgType + ")");

		Logger.instance().debug2(module + 
			"DDS Connection (" + host + ":" + port + ") "
			+ "getConfig(" + cfgType + ")");

		// Send request
		LddsMessage msg = new LddsMessage(LddsMessage.IdRetConfig, cfgType);
		sendData(msg.getBytes());

		// Read the response message - It should be same type ID.
		msg = linput.getMessage();
		if (msg.MsgId != LddsMessage.IdRetConfig)
			throw new ProtocolError("Unexpected responses '" + msg.MsgId + "'");

		// Validate response
		if (msg.MsgLength > 0 && (char)msg.MsgData[0] == '?')
			throw new ServerError(new String(msg.MsgData));
		if (msg.MsgLength < 64)
			throw new ServerError("?0,0,No file data returned");

		return ArrayUtil.getField(msg.MsgData, 64, msg.MsgLength - 64);
	}

	/**
	 * Installs a configuration on the server.
	 * @throws AuthException if you are not authenticated, or on any 
	 *  server-side error.
	 */
	public void installConfig(String cfgType, byte[] data)
		throws AuthException
	{
		if (debug != null)
			debug.println("installConfig(" + cfgType + ")");
		Logger.instance().debug2(module + 
			"DDS Connection (" + host + ":" + port + ") "
			+ "installConfig(" + cfgType + ")");

		// Construct an empty criteria message big enought for this file.
		LddsMessage msg = new LddsMessage(LddsMessage.IdInstConfig, "");
		msg.MsgLength = 64 + (data != null ? data.length : 0);
		msg.MsgData = new byte[msg.MsgLength];

		// Create the 'header' portion, which contains 64 char cfg type
		int i;
		for(i = 0; i<64 && i <cfgType.length(); i++)
			msg.MsgData[i] = (byte)cfgType.charAt(i);
		msg.MsgData[i] = (byte)0;

		// Copy the file data into the message & send it out.
		if (data != null)
		{
			for(i=0; i<data.length; i++)
				msg.MsgData[i+64] = data[i];
		}
		if (debug != null)
			debug.println("Sending config " + cfgType + " (total length=" +
				msg.MsgLength + ")");
		Logger.instance().debug2(module + 
			"DDS Connection (" + host + ":" + port + ") "
				+ "Sending config " + cfgType + " (total length=" +
				msg.MsgLength + ")");

		try 
		{
			sendData(msg.getBytes()); 
			msg = linput.getMessage();
		}
		catch(Exception ex)
		{
			throw new AuthException("Config Send Failed: " + ex);
		}

		if (msg.MsgId != LddsMessage.IdInstConfig)
			throw new AuthException("Unexpected response '" + 
				msg.MsgId + "'");
		else if ((char)msg.MsgData[0] == '?')
			throw new AuthException(new String(msg.MsgData));
		Logger.instance().debug2(module + 
			"DDS Connection (" + host + ":" + port + ") Inst Cfg response OK");
	}



	/**
	 * Low-level method to retrieve the next block of data containing
	 * extended DCP messages in an XML format.
	 * If a search criteria file has previously been transfered
	 * (either to or from the server), then the server will only pass messages
	 * that meet the specified criteria.
	 * <p>
	 * The timeout argument specifies the number of seconds to wait for a
	 * response from the server.
	 *
	 * @return uncompressed byte-array response to the Extended Block Request.
	 * from the server within the specified # of seconds.
	 * @param timeout number of seconds to wait

	 * @throws IOException if socket is no longer usable.
	 * @throws ProtocolError if an unexpected response was received.
	 * (i.e. the wrong message type).
	 * @throws ServerError with Derrno equal to 11 (DMSGTIMEOUT) if
	 * the server indicates that you are already caught-up to the present
	 * time. When this happens, pause and try again later.
	 * @throws ServerError with other Derrno codes to indicate other errors 
	 * on the server which are not recoverable. When this happens, call
	 * disconnect() and then start the session over.
	*/
	public byte[] getMsgBlockExtXml(int timeout)
		throws IOException, ProtocolError, ServerError
	{
		Logger.instance().debug2(module + 
			"DDS Connection (" + host + ":" + port + ") "
			+ "Requesting DCP Message Block Ext (timeout=" 
			+ timeout + ")...");

		LddsMessage req = new LddsMessage(LddsMessage.IdDcpBlockExt, "");

		LddsMessage resp = serverExec(req);
		if (resp.MsgId != LddsMessage.IdDcpBlockExt)
			throw new ProtocolError("Unexpected response '" + resp.MsgId 
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
			return ret;
		}
		catch(IOException ex)
		{
			throw new ProtocolError("Error unpacking compressed block: " + ex);
		}
	}

	/**
	 * Gets the next extended mode block and converts the XML into an array
	 * of DcpMsg objects.
	 */
	public DcpMsg[] getMsgBlockExt(int timeout)
		throws IOException, ProtocolError, ServerError
	{
		DcpMsg ret[] = null;
		int numFiltered = 0;
		do
		{
			byte[] xmldata = getMsgBlockExtXml(timeout);
			if (extBlockXmlParser == null)
				extBlockXmlParser = new ExtBlockXmlParser(DcpMsgFlag.SRC_DDS);
			ret = extBlockXmlParser.parseMsgBlock(xmldata);
			// If we have a local filter (i.e. we are talking to a legacy
			// server and must do some of the filtering locally)...
			if (searchCritLocalFilter != null && ret != null)
			{
				numFiltered = 0;
				for(int idx = 0; idx < ret.length; idx++)
					if (!searchCritLocalFilter.passesCrit(ret[idx]))
					{
						ret[idx] = null;
						numFiltered++;
					}
				if (numFiltered > 0)
				{
					DcpMsg fret[] = new DcpMsg[ret.length - numFiltered];
					int fidx = 0;
					for(int idx = 0; idx < ret.length; idx++)
						if (ret[idx] != null)
							fret[fidx++] = ret[idx];
					ret = fret;
				}
			}
		} while(ret != null && ret.length == 0 && 
			searchCritLocalFilter != null && numFiltered > 0);
		return ret;
	}

	/**
	  Get the next DCP message using extended-mode.
	
	  @param timeout number of seconds to wait
	  @return DcpMsg object 
	*/
	private DcpMsg getDcpMsgExt(int timeout)
		throws IOException, ProtocolError, ServerError
	{
		if (multiMsg == null || multiMsgIdx >= multiMsg.length)
		{
			// get next block of data
			multiMsgIdx = 0;
			multiMsg = null;
			multiMsg = getMsgBlockExt(timeout);
			if (multiMsg == null)
				throw new ProtocolError(
					"Timeout waiting for ext block response from server.");
		}
		if (multiMsg != null && multiMsgIdx < multiMsg.length)
			return multiMsg[multiMsgIdx++];
		else
			return null;
	}
	
	/**
	 * Returns the array of outages, optionally within a time range.
	 * If both start and end are null, all outages are returned.
	 * Else if only end is null, all outages since the start time are returned.
	 */
	public ArrayList<Outage> getOutages(Date start, Date end)
		throws IOException, ProtocolError, ServerError
	{
		String txt = "";
		if (start != null)
		{
			txt = outageDateFormat.format(start);
			if (end != null)
				txt = txt + " " + outageDateFormat.format(end);
		}
		LddsMessage msg = new LddsMessage(LddsMessage.IdGetOutages, txt);
		LddsMessage resp = serverExec(msg);
		// Response contains GZipped array of XML Outages. First unzip it.

		byte respData[] = null;
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
			respData = baos.toByteArray();
//Logger.instance().info("orig response len=" + resp.MsgData.length 
//+ " after unzip=" + respData.length);
		}
		catch(IOException ex)
		{
			throw new ProtocolError("Error unpacking compressed outages: "+ex);
		}

		try { return getOutageXmlParser().parse(respData); }
		catch(IOException ex)
		{
			Logger.instance().warning("Unparsable outages: " + ex);
			return new ArrayList<Outage>();
		}
	}

//============================
//	/**
//	 * Receives a DCP Message Block after calling requestDcpMsg.
//	 * The block is parsed into an array of DcpMsg objects and returned.
//	 * Returns null if no response is received from server in timeout seconds.
//	 * <p>
//	 * @throws IOException if the socket is no longer usable.
//	 * @ProtocolError if an unexpected response was received.
//	 * (i.e. the wrong message type).
//	 * @throws ServerError with a Derrno equal to (DUNTIL) if the specified
//	 * until time was reached.
//	 * @throws ServerError with other codes  for other errors on the server
//	 * (probably not recoverable).
//	*/
//	public DcpMsg[] receiveDcpMsgBlock(int timeout)
//		throws IOException, ProtocolError, ServerError
//	{
//		// Check for asynchronous disconnection.
//		if (linput == null)
//		{
//			Logger.instance().debug2(module + 
//				"DDS Connection (" + host + ":" + port + ") "
//				+ "receiveDcpMsgBlock asynchronous disconect, aborting.");
//			return null;
//		}
//
//		socket.setSoTimeout((timeout+1) * 1000);
//
//		// Get response.
//		LddsMessage msg = linput.getMessage();
//		if (msg.MsgId != LddsMessage.IdDcpBlock)
//			throw new 
//				ProtocolError("Unexpected response '" + msg.MsgId 
//					+ "', expected type '" + LddsMessage.IdDcpBlock + "'");
//
//		if (msg.MsgData[0] == (byte)'?')
//		{
//			String err = new String(msg.MsgData);
//			Logger.instance().debug2(module + 
//				"DDS Connection (" + host + ":" + port + ") ServerError: "
//				+ err);
//			throw new ServerError(err);
//		}
//
//		Logger.instance().debug2(module + 
//			"DDS Connection (" + host + ":" + port + ") "
//			+ "received block response, length=" + msg.MsgLength);
//
//		return lddsMsg2DcpMsgBlock(msg);
//	}
//
//	private DcpMsg[] lddsMsg2DcpMsgBlock(LddsMessage msg)
//		throws ProtocolError
//	{
//		Logger.instance().debug3(module + 
//			"Parsing block response. Total length = " + msg.MsgLength);
//
//		// Got response, parse it into an array of DCP Messages.
//		Vector v = new Vector();
//		boolean garbled = false;
//		int msgnum = 0;
//		for(int msgStart=0; msgStart<msg.MsgLength && !garbled; msgnum++)
//		{
//			if (msg.MsgLength - msgStart < DcpMsg.DCP_MSG_MIN_LENGTH)
//			{
//				Logger.instance().failure(
//					"DDS Connection (" + host + ":" + port
//					+ ") Response to IdDcpBlock incomplete. "
//					+ "Need at least 37 bytes. Only have "
//					+ (msg.MsgLength - msgStart) + " at location "
//					+ msgStart);
//				Logger.instance().failure("Response='" 
//					+ new String(msg.MsgData, msgStart,msg.MsgLength-msgStart)
//					+ "'");
//				garbled = true;
//			}
//			int msglen = 0;
//			try 
//			{
//				msglen = ByteUtil.parseInt(msg.MsgData,
//					msgStart + DcpMsg.IDX_DATALENGTH, 5);
//			}
//			catch(NumberFormatException nfe)
//			{
//				String lenfield = new String(ArrayUtil.getField(msg.MsgData, 
//					msgStart + DcpMsg.IDX_DATALENGTH, 5));
//				Logger.instance().failure(module + 
//					"DDS Connection (" + host + ":" + port
//					+ ") Response to IdDcpBlock contains bad length field '"
//					+ lenfield 
//					+ "' requires a 5-digit 0-filled integer, msgnum="
//					+ msgnum + ", msgStart=" + msgStart);
//				garbled = true;
//			}
//
//			int numbytes = DcpMsg.DCP_MSG_MIN_LENGTH + msglen;
////			if (numbytes > msg.MsgData.length - msgStart)
////				numbytes = msg.MsgData.length - msgStart;
//
//			DcpMsg dcpMsg = new DcpMsg(msg.MsgData, numbytes, msgStart);
//			dcpMsg.flagbits = DcpMsgFlag.MSG_PRESENT |  DcpMsgFlag.SRC_DDS
//				| DcpMsgFlag.MSG_NO_SEQNUM;
//
//			v.add(dcpMsg);
//
//			//Logger.instance().debug3(module + 
//			//	"Extracted DCP Msg '" + dcpMsg.getHeader() + "' from block.");
//			msgStart += (DcpMsg.DCP_MSG_MIN_LENGTH + msglen);
//		}
//
//		Logger.instance().debug2(module + 
//			"Message Block Response contained " + v.size() + " dcp msgs.");
//
//		if (v.size() == 0)
//			return null;
//		else
//		{
//			DcpMsg ret[] = new DcpMsg[v.size()];
//			return (DcpMsg[])v.toArray(ret);
//		}
//	}
//	
//
//	private DcpMsg[] abortDcpMsgBlockRequest()
//		throws IOException, ProtocolError
//	{
//		if (debug != null)
//			debug.println("abortDcpMsgBlockRequest()");
//		Logger.instance().debug2(module + 
//			"DDS Connection (" + host + ":" + port + ") "
//			+ "abortDcpMsgBlockRequest()");
//
//		if (linput == null)
//			return null;
//
//		LddsMessage msg = new LddsMessage(LddsMessage.IdStop, null);
//		sendData(msg.getBytes());
//
//		msg = getAbortResponse();
//
//		// There is a race condition here. The server may have just
//		// sent the message out when I sent it an abort. If the message I
//		// just read is a DCP message return it to the client.
//		DcpMsg ret[] = null;
//		if (msg != null && msg.MsgId == LddsMessage.IdDcpBlock)
//		{
//			ret = lddsMsg2DcpMsgBlock(msg);
//
//			// Now read the response to the Abort request.
//			msg = getAbortResponse();
//		}
//
//		if (msg == null)
//			throw new ProtocolError(
//				"No response to abort request received");
//		else if (msg.MsgId != LddsMessage.IdStop)
//			throw new ProtocolError(
//				"Unexpected response '" + msg.MsgId + "'"
//				+ " - Expected '" + LddsMessage.IdStop + "' (Stop)");
//
//		return ret;
//	}
//
//
//============================

	/**
	  @return the protocol version supported by the server to which you
	  are connected.
	*/
	public int getServerProtoVersion() { return serverProtoVersion; }

	/**
	 * Sends a no-op command, which can be useful for keeping a connection
	 * alive in the absense of other requests.
	 */
	public void sendNoop()
		throws IOException, ProtocolError, ServerError 
	{
		LddsMessage msg = new LddsMessage(LddsMessage.IdStop, getName());
		sendData(msg.getBytes());
		msg = getAbortResponse();
		if (msg.MsgId != LddsMessage.IdStop)
			throw new ProtocolError("Unexpected response to noop.");
	}
	

	/**
	 * Sets the module name, which will be prepended on log messages.
	 * @param mod the module name
	 */
	public void setModule(String mod)
	{
		module = mod;
		if (!module.endsWith(" "))
			module = module + " ";
	}

	/**
	 * Sends a raw LddsMessage and gets the response returned by server.
	 * It's up to the caller to validate the contents of the response.
	 * @param msg the message to send
	 * @return the message response from the server.
	 */
	public LddsMessage serverExec(LddsMessage msg)
		throws IOException, ProtocolError, ServerError 
	{
		Logger.instance().debug3("serverExec: sending LddsMessage type=" 
			+ msg.MsgId + ", length=" + msg.MsgLength);
		sendData(msg.getBytes());
		LddsMessage ret = linput.getMessage();
		Logger.instance().debug3("serverExec: response LddsMessage type=" 
			+ ret.MsgId + ", length=" + ret.MsgLength);

		if ((char)ret.MsgData[0] == '?')
			throw new ServerError(new String(ret.MsgData));
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

	/**
	 * Gets a list of valid DDS users from the server.
	 * @throws AuthException if server rejects the request or on any other 
	 *  error.
	 * @return a list of valid DDS users from the server.
	 */
	public ArrayList<DdsUser> getUsers()
		throws AuthException
	{
		try 
		{
			LddsMessage req = new LddsMessage(LddsMessage.IdUser,"list");
			LddsMessage resp = serverExec(req);
			String listResp = new String(resp.MsgData);
			StringTokenizer st = new StringTokenizer(listResp, "\r\n");
			ArrayList<DdsUser> userList = new ArrayList<DdsUser>();
			while(st.hasMoreTokens())
			{
				String line = st.nextToken();
				try 
				{
					userList.add(new DdsUser(line));
				}
				catch(BadConfigException ex)
				{
					Logger.instance().warning(module + " Ignoring user spec '" 
						+ line + "': " + ex);
				}
			}
			return userList;
		}
		catch(Exception ex)
		{
			throw new AuthException("Cannot list users: " + ex);
		}
	}

	/**
	 * Sends a request to add or edit a user on the DDS server.
	 * @param ddsUser the user data
	 * @param pw the password, or null to leave password unchanged on server.
	 * @throws AuthException if you are not authenticated, or on any 
	 *  server-side error.
	 */
	public void modUser(DdsUser ddsUser, String pw)
		throws AuthException
	{
		String cmd = "set " + ddsUser.userName + " ";
		if (pw == null || pw.length() == 0)
			cmd += "-";
		else
		{
			byte[] sk = getSessionKey();
			if (sk == null)
				throw new AuthException(
					"Modify user requires authenticated connection.");
			PasswordFileEntry pfe = new PasswordFileEntry(ddsUser.userName);
			pfe.setPassword(pw);
			String sks = ByteUtil.toHexString(sk);
			pw = ByteUtil.toHexString(pfe.getShaPassword());
			DesEncrypter de = new DesEncrypter(sks);
			pw = de.encrypt(pw);
			cmd += pw;
		}

		// Syntax: set username DES(pw) perms props
		// No field may have embedded blanks.
		cmd = cmd + " " + ddsUser.permsString() + " " + ddsUser.propsString();

		try
		{
			LddsMessage msg = new LddsMessage(LddsMessage.IdUser, cmd);
			LddsMessage resp = serverExec(msg);
		}
		catch(Exception ex)
		{
			throw new AuthException("Cannot set user info: " + ex);
		}
	}

	/**
	 * Removes a user on the server.
	 * @param userName the user name to remove.
	 * @throws AuthException on any server-side error.
	 */
	public void rmUser(String userName)
		throws AuthException
	{
		String cmd = "rm " + userName;
		try
		{
			LddsMessage msg = new LddsMessage(LddsMessage.IdUser, cmd);
			LddsMessage resp = serverExec(msg);
		}
		catch(Exception ex)
		{
			throw new AuthException("Cannot remove DDS user: " + ex);
		}
	}

	public OutageXmlParser getOutageXmlParser()
	{
		if (outageXmlParser == null)
			outageXmlParser = new OutageXmlParser();
		return outageXmlParser;
	}
	

	/** find the LddsConnections file and return path */
	public static String getLddsConnectionsFile()
	{
		String dirs[] = { "DCSTOOL_USERDIR", "DCSTOOL_HOME", "user.home" };
		for(String p : dirs)
		{
			String dir = System.getProperty(p);
			if (dir != null && dir.length() > 0)
				return dir + "/LddsConnections";
		}
		return "LddsConnections";
	}

}
