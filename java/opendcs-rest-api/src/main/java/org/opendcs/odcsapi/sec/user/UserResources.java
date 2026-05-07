package org.opendcs.odcsapi.sec.user;

import org.opendcs.authentication.OpenDcsAuthException;
import org.opendcs.authentication.identityprovider.impl.builtin.BuiltInIdentityProvider;
import org.opendcs.authentication.identityprovider.impl.builtin.BuiltInProviderCredentials;
import org.opendcs.database.api.OpenDcsDataException;
import org.opendcs.database.dai.UserManagementDao;
import org.opendcs.odcsapi.beans.ApiPasswordChange;
import org.opendcs.odcsapi.errorhandling.WebAppException;
import org.opendcs.odcsapi.res.OpenDcsResource;
import org.opendcs.odcsapi.sec.OpenDcsPrincipal;
import org.opendcs.odcsapi.util.ApiConstants;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.security.RolesAllowed;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/user")
@Tag(name = "User Operations", description = "Endpoints used to manipulate user data.")
public final class UserResources extends OpenDcsResource
{
    private static final WebAppException UNABLE_TO_GET_UM_DAO = new WebAppException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), "No User Management DAO available.");

    @Context
	HttpServletRequest request;

    @POST
    @Path("updatePassword")
    @RolesAllowed({ApiConstants.ODCS_API_USER})
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(
        description = "Update user password",
        tags = "User",
        requestBody = @RequestBody(
            description = "existing password for validation and new password for change",
					required = true,
					content = @Content(
							mediaType = MediaType.APPLICATION_JSON,
							schema = @Schema(implementation = ApiPasswordChange.class)
					)
        ),
        responses = {
            @ApiResponse(responseCode = "200", description = "Password was changed."),
            @ApiResponse(responseCode = "403", description = "Password was not changed.")
        }
    )
    public Response updatePassword(ApiPasswordChange passwordChange) throws WebAppException
    {
        //Security filters will ensure this method is only accessible via an authenticated client
		var session = request.getSession(false);

        if (session == null)
        {
            throw new WebAppException(Response.Status.FORBIDDEN.getStatusCode(), "User not logged in.");
        }
		var sessionPrincipal = (OpenDcsPrincipal)session.getAttribute(OpenDcsPrincipal.USER_PRINCIPAL_SESSION_ATTRIBUTE);

        final var db = createDb();
        final var umDao = db.getDao(UserManagementDao.class).orElseThrow(() -> UNABLE_TO_GET_UM_DAO);
        try (var tx = db.newTransaction())
        {
            var providers = umDao.getIdentityProvidersForSubject(tx, sessionPrincipal.getName());
            BuiltInIdentityProvider idp = null;
            for (var provider: providers)
            {
                if (provider.canUpdateCredentials() && (provider instanceof BuiltInIdentityProvider bidp))
                {
                    idp = bidp;
                    break;
                }
            }
            if (idp == null)
            {
                throw new WebAppException(Response.Status.FORBIDDEN.getStatusCode(), "Unable to update password");
            }
            
            // check current password
            if (idp.login(db, tx, new BuiltInProviderCredentials(sessionPrincipal.getName(), passwordChange.currentPassword())).isPresent())
            {
                idp.updateUserCredentials(db, tx, sessionPrincipal.getUser(), new BuiltInProviderCredentials(sessionPrincipal.getName(), passwordChange.newPassword()));
            }
            else
            {
                throw new WebAppException(Response.Status.FORBIDDEN.getStatusCode(), "Unable to update password");
            }

        }
        catch (OpenDcsDataException | OpenDcsAuthException ex)
        {
            throw new WebAppException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), "Unable to process password change.", ex);
        }


        return Response.noContent().build();
    }
}
