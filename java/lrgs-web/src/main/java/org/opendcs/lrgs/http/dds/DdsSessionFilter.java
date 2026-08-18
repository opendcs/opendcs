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
import lrgs.common.DcpAddress;
import lrgs.ddsserver.MessageArchiveRetriever;
import lrgs.lrgsmain.LrgsMain;

@Provider
@UseDdsSession
public class DdsSessionFilter implements ContainerRequestFilter
{
    @Context
    ServletContext servletContext;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException
    {
        var request = (HttpServletRequest)requestContext.getRequest();
        var session = request.getSession(false);
        if (session == null)
        {
            session = request.getSession();
            // Shorter timeout than default. To converse resources drop sessions
            // quickly if not used. At least if not an authenticated user.
            var user = request.getUserPrincipal();
        
            if (user == null || "guest".equalsIgnoreCase(user.getName()) || "anonymous".equalsIgnoreCase((user.getName())))
            {
                session.setMaxInactiveInterval(300);
            }
        }

        var mar = (MessageArchiveRetriever)session.getAttribute(UseDdsSession.KEY);
        if (mar == null) // Session may have already been setup by going to a different endpoint.
        {
            var lrgs = (LrgsMain)servletContext.getAttribute("lrgs");
            if (lrgs != null)
            {
                mar = getMar(lrgs);
                var ddsSession = new DdsSession(mar, 15, lrgs.msgArchive);
                session.setAttribute(UseDdsSession.KEY, ddsSession); // NOSONAR
            }
        }
    }
    

    public MessageArchiveRetriever getMar(LrgsMain lrgs)
    {
        MessageArchiveRetriever mar = null;
        if (lrgs != null)
        {
            org.opendcs.lrgs.dao.MsgArchive archive = lrgs.msgArchive;
            AttachedProcess ap = new AttachedProcess(1, "http", "http", "anonymous", 0, 0, 0, "running", (short)0);
            mar = new MessageArchiveRetriever((XmlMsgArchive)archive, ap);
            mar.setDcpNameMapper(DcpAddress::new);
            return mar;
        }
        return null;
    }

}
