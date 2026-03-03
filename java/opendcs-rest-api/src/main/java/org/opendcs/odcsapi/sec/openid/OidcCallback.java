package org.opendcs.odcsapi.sec.openid;

import static org.opendcs.odcsapi.util.ApiConstants.ODCS_API_GUEST;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import jakarta.annotation.security.RolesAllowed;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;

@Path("/oidc-callback")
public final class OidcCallback
{
    private static final Logger log = OpenDcsLoggerFactory.getLogger();

    @Context
	HttpServletRequest request;

    @GET
    @RolesAllowed({ODCS_API_GUEST})
    public Response handle(@QueryParam("code") String code, @QueryParam("state") String state)
    {

        log.info("OIDC Login Attempt.");
        request.changeSessionId();
        return Response.noContent().build();
    }
}
