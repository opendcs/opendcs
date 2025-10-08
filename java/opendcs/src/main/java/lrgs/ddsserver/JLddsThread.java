/*
* Where Applicable, Copyright 2025 OpenDCS Consortium and/or its contributors
*
* Licensed under the Apache License, Version 2.0 (the "License"); you may not
* use this file except in compliance with the License. You may obtain a copy
* of the License at
*
*   http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
* License for the specific language governing permissions and limitations
* under the License.
*/
package lrgs.ddsserver;

import java.io.IOException;
import java.net.Socket;
import java.util.Date;
import java.util.LinkedList;

import javax.net.ssl.SSLSocketFactory;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import ilex.net.*;
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
    private static final Logger log = OpenDcsLoggerFactory.getLogger();
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
      @param id unique integer ID for this client.
      @param msgArchive the archive from which to serve data.
      @param globalMapper
      @param ap
      @throws IOException
    */
    public JLddsThread(BasicServer parent, Socket socket, int id, SSLSocketFactory socketFactory,
        MsgArchive msgArchive, DcpNameMapper globalMapper, AttachedProcess ap)
        throws IOException
    {
        super(parent, socket, id, socketFactory);
        this.msgArchive = msgArchive;
        this.globalMapper = globalMapper;
        this.attachedProcess = ap;
        attachedProcess.pid = id;
        attachedProcess.type = "DDS-CLI";
        attachedProcess.user = "(unknown)";
        attachedProcess.status = "user?";
        this.setName("DDS-Client "+socket.getInetAddress().toString()+":"+socket.getPort());
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
        if (user == null)
        {
            log.error("JLddsThread: makeDcpMsgSrc, user is null!");
        }
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
            {
                peerLastActivity = new Date(0L);
            }
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
            log.warn("More than 50 connections with same user/host = {}/{} -- Hanging up oldest.",
                     myusername, myhostname);
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

    public void disconnect()
    {
        attachedProcess.pid = -1;
        super.disconnect();
    }

    public void setHostName(String hostname)
    {
        super.setHostName(hostname);
        if (attachedProcess != null)
        {
            attachedProcess.setName(hostname);
        }
    }
}
