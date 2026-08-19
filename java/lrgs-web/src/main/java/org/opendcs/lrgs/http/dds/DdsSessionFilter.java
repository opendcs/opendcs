package org.opendcs.lrgs.http.dds;

import java.io.IOException;

import org.opendcs.lrgs.dds.DdsSession;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.ext.Provider;
import lrgs.apistatus.AttachedProcess;
import lrgs.archive.XmlMsgArchive;
import lrgs.common.ArchiveUnavailableException;
import lrgs.common.DcpAddress;
import lrgs.common.SearchSyntaxException;
import lrgs.ddsserver.MessageArchiveRetriever;
import lrgs.lrgsmain.LrgsMain;

@Provider
@UseDdsSession
public class DdsSessionFilter implements ContainerRequestFilter
{
    @Context
    ServletContext servletContext;

    @Context
    private HttpServletRequest httpRequest;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException
    {
        
        var session = httpRequest.getSession(false);
        if (session == null)
        {
            session = httpRequest.getSession();
            // Shorter timeout than default. To converse resources drop sessions
            // quickly if not used. At least if not an authenticated user.
            var user = httpRequest.getUserPrincipal();
        
            if (user == null ||
                "guest".equalsIgnoreCase(user.getName()) ||
                "anonymous".equalsIgnoreCase((user.getName())))
            {
                session.setMaxInactiveInterval(300);
            }
        }

        var ddsSession = (DdsSession)session.getAttribute(UseDdsSession.KEY);
        if (ddsSession == null) // Session may have already been setup by going to a different endpoint.
        {
            var lrgs = (LrgsMain)servletContext.getAttribute("lrgs");
            if (lrgs != null)
            {
                try
                {
                    var mar = getMar(lrgs);
                    ddsSession = new DdsSession(mar, 15, lrgs.msgArchive);
                    session.setAttribute(UseDdsSession.KEY, ddsSession); // NOSONAR
                }
                catch (SearchSyntaxException | ArchiveUnavailableException | IOException ex)
                {
                    throw new IOException("Unable to initialize Message Archive Retrieval instance for this session.", ex);
                }
            }
        }
    }
    

    public MessageArchiveRetriever getMar(LrgsMain lrgs) throws SearchSyntaxException, ArchiveUnavailableException, IOException
    {
        MessageArchiveRetriever mar = null;
        if (lrgs != null)
        {
            org.opendcs.lrgs.dao.MsgArchive archive = lrgs.msgArchive;
            AttachedProcess ap = new AttachedProcess(1, "http", "http", "anonymous", 0, 0, 0, "running", (short)0);
            mar = new MessageArchiveRetriever((XmlMsgArchive)archive, ap);
            mar.setDcpNameMapper(DcpAddress::new);
            mar.setDcpMsgSource(mar);
            mar.init();
            return mar;
        }
        return null;
    }

}
