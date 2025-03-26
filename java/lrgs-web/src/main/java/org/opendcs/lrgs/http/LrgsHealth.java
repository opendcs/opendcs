package org.opendcs.lrgs.http;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lrgs.lrgsmain.LrgsMain;

@Path("/health")
public class LrgsHealth
{
    private static final Logger log = LoggerFactory.getLogger(LrgsHealth.class);

    @Context
    ServletContext servletContext;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response get()
    {
        log.info("Sending Health status.");
        LrgsMain lrgs = (LrgsMain)servletContext.getAttribute("lrgs");
        if (lrgs != null && lrgs.getDdsServer().statusProvider.getStatusSnapshot().isUsable)
        {
            return Response.ok("\"Active\"").build();
        }
        else
        {
            return Response.status(HttpServletResponse.SC_SERVICE_UNAVAILABLE)
                           .entity("\"Inactive\"")
                           .build();
        }
    }
}
