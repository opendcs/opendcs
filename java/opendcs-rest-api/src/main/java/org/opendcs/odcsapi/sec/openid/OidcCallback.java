package org.opendcs.odcsapi.sec.openid;

import static org.opendcs.odcsapi.util.ApiConstants.ODCS_API_GUEST;

import java.util.UUID;

import org.opendcs.authentication.OpenDcsAuthException;
import org.opendcs.authentication.identityprovider.impl.oidc.AuthCodeCredentials;
import org.opendcs.database.api.OpenDcsDataException;
import org.opendcs.database.dai.UserManagementDao;
import org.opendcs.odcsapi.res.OpenDcsResource;
import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import jakarta.annotation.security.RolesAllowed;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.Response;

@Path("/oidc-callback")
public final class OidcCallback extends OpenDcsResource
{
    private static final Logger log = OpenDcsLoggerFactory.getLogger();

    @Context
	HttpServletRequest request;

    

    @GET
    @RolesAllowed({ODCS_API_GUEST})
    public Response handle(@QueryParam("code") String code, @QueryParam("state") String state, @CookieParam("state") Cookie stateFromSession)
    {
        log.info("OIDC Login Attempt. State is '{}'", state);
        
        log.info("Session State {}", stateFromSession.getValue());

        if (!state.equals(stateFromSession.getValue()))
        {
            log.warn("Invalid login attempt.");
            return Response.status(Response.Status.FORBIDDEN).build();
        }

        var db = createDb();
        
        /***
         * 1. determine appropriate oidc provider
         * 2. call auth method
         */

        try (var tx = db.newTransaction())
        {
            var provider = db.getDao(UserManagementDao.class).orElseThrow().getIdentityProvider(tx, state.split("__")[0])
            if (provider.isEmpty()) 
            {
                return Response.notAcceptable(null).build();
            }
            provider.ifPresent(idp ->
            {
                try
                {
                    var user = idp.login(db, tx, new AuthCodeCredentials(code));
                }
                catch (OpenDcsAuthException ex)
                {
                    log.atError().setCause(ex).log("Bad login attempt");
                }
            }
            );
        }
        catch (OpenDcsDataException ex)
        {
            log.atError().setCause(ex).log("Error during login");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }


        request.changeSessionId();
        return Response.noContent().build();
    }
}
