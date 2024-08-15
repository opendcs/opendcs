package org.opendcs.lrgs.http;

import java.io.IOException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.opendcs.lrgs.http.dto.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lrgs.apistatus.AttachedProcess;
import lrgs.archive.MsgArchive;
import lrgs.common.ArchiveUnavailableException;
import lrgs.common.DcpAddress;
import lrgs.common.DcpMsg;
import lrgs.common.DcpMsgIndex;
import lrgs.common.DcpNameMapper;
import lrgs.common.EndOfArchiveException;
import lrgs.common.SearchCriteria;
import lrgs.common.SearchTimeoutException;
import lrgs.common.UntilReachedException;
import lrgs.ddsserver.MessageArchiveRetriever;
import lrgs.lrgsmain.LrgsMain;

@Path("/dds")
public class DdsHttp
{
    private static final Logger log = LoggerFactory.getLogger(DdsHttp.class);

    @Context
    ServletContext servletContext;

    @GET
    @Path("/next")
    @Produces(MediaType.APPLICATION_JSON)
    public Response get(@Context HttpServletRequest request)
    {        
        LrgsMain lrgs = (LrgsMain)servletContext.getAttribute("lrgs");
        HttpSession session = request.getSession();
        MessageArchiveRetriever mar = null;
        try {
            mar = getMar(lrgs, session);
        } catch (Exception e) {
            log.error("can't get messages= archive retriever", e);
                return Response.status(HttpServletResponse.SC_SERVICE_UNAVAILABLE)
                            .entity("\"Failed to get messages\"")
                            .build();
        }
        if (mar != null)
        {
            final ArrayList<org.opendcs.lrgs.http.dto.DcpMsg> messages = new ArrayList<>();
            try {
                final DcpMsgIndex dmi = new DcpMsgIndex();
                
                int idx = mar.getNextPassingIndex(dmi, System.currentTimeMillis() + 500);
                while(idx != -1 && messages.size() < 1000)
                {
                    final DcpMsg msgOut = dmi.getDcpMsg();
                    if (msgOut != null)
                    {
                        
                        final String type = "" + lrgs.getLrgsInputById(msgOut.getDataSourceId()).getType();
                        
                        final org.opendcs.lrgs.http.dto.DcpMsg msg = 
                            new org.opendcs.lrgs.http.dto.DcpMsg(
                                new DataSource(msgOut.getSource(), type),
                                ZonedDateTime.ofInstant(msgOut.getLocalReceiveTime().toInstant(), ZoneId.of("UTC")),
                                Base64.getEncoder().encodeToString(msgOut.getData())
                                );
                        messages.add(msg);
                    }
                    idx = mar.getNextPassingIndex(dmi, System.currentTimeMillis() + 500);
                }
                return Response.ok().entity(messages).build();
            } catch (ArchiveUnavailableException e) {
                return Response.status(HttpServletResponse.SC_SERVICE_UNAVAILABLE)
                            .entity("\"Failed to get messages\"")
                            .build();
            }
            catch ( UntilReachedException | SearchTimeoutException| EndOfArchiveException ex)
            {
                if (messages.isEmpty())
                {
                    return Response.noContent().header("Retry-After", "10").build();
                }
                return Response.ok().entity(messages).build();
            }

            //return Response.ok("\"Active\"").build();
        }
        else
        {
            return Response.status(HttpServletResponse.SC_SERVICE_UNAVAILABLE)
                           .entity("\"Inactive\"")
                           .build();
        }
    }


    private MessageArchiveRetriever getMar(LrgsMain lrgs, HttpSession session) throws Exception
    {
        log.info(session.toString());
        MessageArchiveRetriever mar = (MessageArchiveRetriever)session.getAttribute("mar");
        if (mar != null)
        {
            return mar;
        }
        if (lrgs != null && lrgs.getDdsServer().statusProvider.getStatusSnapshot().isUsable)
        {
            MsgArchive archive = lrgs.msgArchive;
            AttachedProcess ap = new AttachedProcess(1, "test", "test", "tester", 0, 0, 0, "running", (short)0);
            mar = new MessageArchiveRetriever(archive, ap);
            SearchCriteria sc = new SearchCriteria();
            //sc.addDcpName("TEST");
            sc.setAscendingTimeOnly(true);
            sc.setLrgsSince("now - 1 hour");
            sc.setLrgsUntil("now");
            
            mar.setDcpNameMapper(new DcpNameMapper()
            {
                @Override
                public DcpAddress dcpNameToAddress(String name)
                {
                    return new DcpAddress(name);
                }
            });
            mar.setSearchCriteria(sc);
            session.setAttribute("mar", mar);
            return mar;
        }
        return null;
    }

    public static class Message
    {
        public List<String> messages;
    }
}
