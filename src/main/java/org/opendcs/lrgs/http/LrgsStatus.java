package org.opendcs.lrgs.http;

import java.io.ByteArrayOutputStream;

import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import ilex.xml.XmlOutputStream;
import lrgs.lrgsmain.LrgsMain;
import lrgs.lrgsmon.DetailReportGenerator;
import lrgs.statusxml.LrgsStatusSnapshotExt;

@Path("/status")
public class LrgsStatus
{

    @Context
    ServletContext servletContext;

    
    @GET
    @Produces(MediaType.TEXT_HTML)
    public Response getStatus()
    {        
        LrgsMain lrgs = (LrgsMain)servletContext.getAttribute("lrgs");
        LrgsStatusSnapshotExt status = lrgs.getStatusProvider().getStatusSnapshot();
        DetailReportGenerator gen = lrgs.getReportGenerator();
        try(ByteArrayOutputStream bos = new ByteArrayOutputStream();)
        {
            XmlOutputStream xos = new XmlOutputStream(bos, "html");
            gen.writeReport(xos, status.hostname, status, 10);
            return Response.ok(bos.toByteArray()).build();
        }
        catch (Exception ex)
        {
            return Response.serverError()
                           .entity("\"Cannot generate report.\"")
                           .type(MediaType.APPLICATION_JSON)
                           .build();
        }            
    }
}
