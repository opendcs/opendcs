package org.opendcs.lrgs.webhook.dadds;

import org.opendcs.lrgs.http.LrgsHttpInterface;

import jakarta.servlet.ServletContext;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import lrgs.lrgsmain.LrgsMain;

@Path("/webhook/dadds")
public class DaddsWebHook
{

    @Context
    ServletContext servletContext;

    // Dadds WebHooks are always SNS messages
    @POST
    @Path("{hookId}")
    public Response handleHook(@HeaderParam("x-amz-sns-message-type") String msgType)
    {
        // validate hook id
        // validate signature
        // process message

        LrgsMain lrgs = (LrgsMain)servletContext.getAttribute("lrgs");
        lrgs.msgArchive.archiveMsg(null, (LrgsHttpInterface)servletContext.getAttribute("input"));
        return Response.ok().build();
    }
}
