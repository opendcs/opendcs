/*
*  $Id$
*
*  This is open-source software written by ILEX Engineering, Inc., under
*  contract to the federal government. You are free to copy and use this
*  source code for your own purposes, except that no part of this source
*  code may be claimed to be proprietary.
*
*  Except for specific contractual terms between ILEX and the federal 
*  government, this source code is provided completely without warranty.
*  For more information contact: info@ilexeng.com
*
*  $Log$
*  Revision 1.2  2016/02/29 22:22:02  mmaloney
*  Encapsulate 'name'.
*
*  Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
*  OPENDCS 6.0 Initial Checkin
*
*  Revision 1.6  2009/12/09 19:04:32  mjmaloney
*  GetHostnameThread addition
*
*  Revision 1.5  2009/01/26 01:08:19  mjmaloney
*  Implement Local User Accounts in LRGS
*
*  Revision 1.4  2008/06/10 21:39:52  cvs
*  dev
*
*  Revision 1.3  2008/06/03 15:21:19  cvs
*  dev
*
*  Revision 1.2  2008/05/05 15:03:08  cvs
*  Algorithm Editor Updates
*
*  Revision 1.1  2008/04/04 18:21:12  cvs
*  Added legacy code to repository
*
*  Revision 1.4  2007/12/05 15:46:34  mmaloney
*  dev
*
*  Revision 1.3  2005/07/21 15:17:44  mjmaloney
*  LRGS-5.0
*
*  Revision 1.2  2005/07/20 20:18:56  mjmaloney
*  LRGS 5.0 Release preparation
*
*  Revision 1.1  2005/06/30 15:15:28  mjmaloney
*  Java Archive Development.
*
*/
package lrgs.ddsserver;

import java.io.IOException;
import java.io.File;
import java.io.OutputStream;
import java.net.Socket;
import java.net.InetAddress;
import java.util.Date;
import java.util.LinkedList;

import ilex.net.*;
import ilex.util.Logger;
import ilex.util.QueueLogger;

import lrgs.apistatus.AttachedProcess;
import lrgs.common.*;
import lrgs.ldds.LddsThread;
import lrgs.archive.MsgArchive;
import lrgs.lrgsmain.LrgsConfig;


/**
This subclass of LddsThread uses the Java-Only Archive.
*/
public class JLddsThread extends LddsThread
{
	/** The one and only archive object. */
	private MsgArchive msgArchive;

	/** The global name mapper. */
	private DcpNameMapper globalMapper;

	/** The AttachedProcess structure for tracking status info. */
	private AttachedProcess attachedProcess;

	/**
	  Constructor.
	  @param parent the server object
	  @param socket the socket to the client
	  @param ID unique integer ID for this client.
	  @param msgArchive the archive from which to serve data.
	*/
	public JLddsThread(BasicServer parent, Socket socket, int id, 
		MsgArchive msgArchive, DcpNameMapper globalMapper, AttachedProcess ap)
		throws IOException
	{
		super(parent, socket, id);
		this.msgArchive = msgArchive;
		this.globalMapper = globalMapper;
		this.attachedProcess = ap;
		attachedProcess.pid = id;
		attachedProcess.type = "DDS-CLI";
		attachedProcess.user = "(unknown)";
		attachedProcess.status = "user?";
	}

	/**
	  Template method to create the name mapper.
	  This version is for the Java-Only DDS server. It returns a name
	  mapper that first looks for a mapping in network lists in the user's
	  sandbox directory, and then in the global directory.
	*/
	protected DcpNameMapper makeDcpNameMapper()
	{
		// This can happen if user disconnects in the middle of Hello sequence.
		if (user == null)
			return null;

		return new NetlistDcpNameMapper(user.getDirectory(), globalMapper);
	}

	/**
	  Template method to create the DcpMsgSource object.
	  This version creates a MessageArchiveRetriever, which acts as both
	  the source and the retriever.
	*/
	protected DcpMsgSource makeDcpMsgSource()
		throws ArchiveUnavailableException
	{
		LinkedList clients = parent.getAllSvrThreads();
		String myhostname = getHostName();
if (user == null) System.err.println("JLddsThread: makeDcpMsgSrc, user is null!");
		String myusername = user.getName();
		int nsame = 0;
		Date oldestDate = null;
		JLddsThread oldestPeer = null;
		for(Object ob : clients)
		{
			JLddsThread peer = (JLddsThread)ob;
			if (peer == this)
				continue;
			String peerhostname = peer.getHostName();
			String peerusername = peer.user != null ? peer.user.getName():null;
			Date peerLastActivity = peer.getLastActivity();
			if (peerLastActivity == null)
				peerLastActivity = new Date(0L);
			if (myhostname != null
			 && peerhostname != null
			 && myhostname.equalsIgnoreCase(peerhostname)
			 && myusername != null
			 && peerusername != null
			 && myusername.equals(peerusername))
			{
				nsame++;
				if (oldestDate == null
				 || peerLastActivity.compareTo(oldestDate) < 0)
				{
					oldestDate = peerLastActivity;
					oldestPeer = peer;
				}
			}
		}
		if (nsame > 50)
		{
			Logger.instance().warning("More than 50 connections with same "
				+ "user/host = " + myusername + "/" + myhostname
				+ " -- Hanging up oldest.");
			oldestPeer.disconnect();
		}
		MessageArchiveRetriever ret = 
			new MessageArchiveRetriever(msgArchive, attachedProcess);
		ret.setForceAscending(user.getDisableBackLinkSearch());
		ret.setGoodOnly(user.isGoodOnly());
		return ret;
	}

	/**
	  Template method to create the DcpMsgRetriever object.
	  The source is created first. This method just returns the source
	  object, which also acts as the retriever.
	*/
	protected DcpMsgRetriever makeDcpMsgRetriever()
	{
		return (DcpMsgRetriever)msgacc;
	}

	/**
	 * @return true if authentication is required by this server.
	 */
	public boolean isAuthRequired()
	{
		return LrgsConfig.instance().ddsRequireAuth;
	}

	/**
	 * @return true if same user is allowed multiple connections.
	 */
	public boolean isSameUserMultAttachOK()
	{
		return true;
	}

	/**
	 * @return the root directory for user sandbox directories.
	 */
//	public String getDdsUserRootDir()
//	{
//		return LrgsConfig.instance().ddsUserRootDir;
//	}

	public void disconnect()
	{
		attachedProcess.pid = -1;
		super.disconnect();
	}
	
	public void setHostName(String hostname)
	{
		super.setHostName(hostname);
		if (attachedProcess != null)
			attachedProcess.setName(hostname);
	}
}
