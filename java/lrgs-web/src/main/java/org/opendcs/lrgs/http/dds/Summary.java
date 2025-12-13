package org.opendcs.lrgs.http.dds;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import decodes.db.NetworkList;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lrgs.apistatus.AttachedProcess;
import lrgs.archive.MsgArchive;
import lrgs.common.DcpAddress;
import lrgs.common.DcpNameMapper;
import lrgs.common.SearchCriteria;
import lrgs.ddsserver.MessageArchiveRetriever;
import lrgs.lrgsmain.LrgsMain;

@Path("dds")
public class Summary 
{
private static final Logger log = OpenDcsLoggerFactory.getLogger();

    @Context
    ServletContext servletContext;

    @GET
    @Path("/data/summary")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getNext(@Context HttpServletRequest request) throws Exception
    { 
        LrgsMain lrgs = (LrgsMain)servletContext.getAttribute("lrgs");
        NetworkList nl = new NetworkList();
        var mar = getMar(lrgs, nl);
        mar.forceAscending();
        //mar.

        return Response.ok().build();
    }

    private MessageArchiveRetriever getMar(LrgsMain lrgs, NetworkList networkList) throws Exception
    {
        MsgArchive archive = lrgs.msgArchive;
        AttachedProcess ap = new AttachedProcess(1, "test", "test", "tester", 0, 0, 0, "running", (short)0);
        var mar = new MessageArchiveRetriever(archive, ap);
        SearchCriteria sc = new SearchCriteria();
        sc.setAscendingTimeOnly(true);
        sc.setLrgsSince("now - 24 hours");
        sc.setLrgsUntil("now");
        networkList.iterator().forEachRemaining(e -> sc.addDcpName(e.transportId));
        mar.setDcpNameMapper(new DcpNameMapper()
        {
            @Override
            public DcpAddress dcpNameToAddress(String name)
            {
                return new DcpAddress(name);
            }
        });
        mar.setSearchCriteria(sc);
        return mar;
    }
}
