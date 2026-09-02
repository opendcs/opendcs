package org.opendcs.lrgs.webhook.dadds;

import java.util.List;
import java.util.Map;

import org.opendcs.lrgs.http.LrgsHttpInterface;
import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import jakarta.servlet.ServletContext;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;

@Path("/webhook/dadds")
public class DaddsWebHookResource
{
    private static final Logger log = OpenDcsLoggerFactory.getLogger();

    @Context
    ServletContext servletContext;

    // Dadds WebHooks are always SNS messages
    @POST
    @Path("{hookId}")
    public Response handleHook(@PathParam("hookId") String hookId,
                               @HeaderParam("x-amz-sns-message-type") String msgType,
                               String message)
    {
        // validate hook id
        // validate subscription (if that's the message)        
        // validate signature
        // process message

        var hook = valiateHookId(hookId);
        
        if (hook != null)
        {
            log.trace("Received: {}", message);
            //LrgsMain lrgs = (LrgsMain)servletContext.getAttribute("lrgs");
            //lrgs.msgArchive.archiveMsg(null, hook);
            return Response.ok().build();
        }
        else
        {
            log.trace("Hook id not found in list.");
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        

        
    }

    private DaddsWebHookInput valiateHookId(String hookId)
    {
        var hooks = (Map<String,DaddsWebHookInput>)servletContext.getAttribute("hooks");
        return hooks.get(hookId);
    }
}
