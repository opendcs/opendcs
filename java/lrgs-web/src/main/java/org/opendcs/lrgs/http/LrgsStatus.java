package org.opendcs.lrgs.http;

import java.io.ByteArrayOutputStream;

import jakarta.servlet.ServletContext;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

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
