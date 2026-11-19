package org.opendcs.odcsapi.sec.user;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.opendcs.authentication.OpenDcsAuthException;
import org.opendcs.authentication.identityprovider.impl.builtin.BuiltInIdentityProvider;
import org.opendcs.authentication.identityprovider.impl.builtin.BuiltInProviderCredentials;
import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.OpenDcsDataException;
import org.opendcs.database.api.OpenDcsDatabase;
import org.opendcs.database.dai.IdentityProviderDao;
import org.opendcs.database.dai.UsersDao;
import org.opendcs.database.model.User;
import org.opendcs.odcsapi.beans.ApiPasswordChange;
import org.opendcs.odcsapi.beans.Status;
import org.opendcs.odcsapi.errorhandling.WebAppException;
import org.opendcs.odcsapi.res.OpenDcsResource;
import org.opendcs.odcsapi.sec.OpenDcsPrincipal;
import org.opendcs.odcsapi.util.ApiConstants;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import decodes.sql.DbKey;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.AdditionalPropertiesValue;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.security.RolesAllowed;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/user")
@Tag(name = "User Operations", description = "Endpoints used to manipulate user data.")
public final class UserResources extends OpenDcsResource
{
    private static final WebAppException UNABLE_TO_GET_IDP_DAO = new WebAppException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), "No IdentityProvider DAO available.");
    private static final WebAppException UNABLE_TO_GET_USERS_DAO = new WebAppException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), "No Users DAO available.");

    /**
     * Largest preferences document, serialized as JSON, that will be stored. The CWMS-Oracle
     * schema keeps preferences in a varchar(4000) column, so the same bound is applied everywhere.
     */
    static final int MAX_PREFERENCES_BYTES = 4000;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String PREFERENCES_EXAMPLE = """
            {"organization": "SPK", "colorMode": "dark"}""";

    @Context
	HttpServletRequest httpRequest;

    @POST
    @Path("updatePassword")
    @RolesAllowed({ApiConstants.ODCS_API_USER, ApiConstants.ODCS_API_REGISTERED})
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
            @ApiResponse(responseCode = "204", description = "Password was changed."),
            @ApiResponse(responseCode = "403", description = "Password was not changed.")
        }
    )
    public Response updatePassword(ApiPasswordChange passwordChange) throws WebAppException
    {
        //Security filters will ensure this method is only accessible via an authenticated client
        var sessionPrincipal = requirePrincipal(requireSession());

        final var db = createDb();
        final var idpDao = db.getDao(IdentityProviderDao.class).orElseThrow(() -> UNABLE_TO_GET_IDP_DAO);
        try (var tx = db.newTransaction())
        {
            applyPasswordChange(db, idpDao, tx, sessionPrincipal, passwordChange);
        }
        catch (OpenDcsDataException ex)
        {
            throw new WebAppException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), "Unable to process password change.", ex);
        }

        return Response.noContent().build();
    }

    @GET
    @Path("preferences")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({ApiConstants.ODCS_API_REGISTERED, ApiConstants.ODCS_API_USER, ApiConstants.ODCS_API_ADMIN})
    @Operation(
        summary = "Retrieve the current user's preferences",
        description = """
            Preferences are stored with the user's profile so they follow the user between browsers \
            and sessions. Values are generally simple strings, but may be any JSON value.""",
        tags = "User",
        responses = {
            @ApiResponse(responseCode = "200", description = "The stored preferences.",
                content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(type = "object", additionalProperties = AdditionalPropertiesValue.TRUE),
                    examples = @ExampleObject(value = PREFERENCES_EXAMPLE))),
            @ApiResponse(responseCode = "401", description = "No user session established."),
            @ApiResponse(responseCode = "404", description = "The session's user no longer exists.",
                content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = Status.class)))
        }
    )
    public Response getPreferences() throws WebAppException
    {
        var sessionPrincipal = requirePrincipal(requireSession());

        final var db = createDb();
        final var usersDao = db.getDao(UsersDao.class).orElseThrow(() -> UNABLE_TO_GET_USERS_DAO);
        try (var tx = db.newTransaction())
        {
            var user = findUser(usersDao, tx, sessionPrincipal.getUser().id);
            return Response.ok(user.preferences).build();
        }
        catch (OpenDcsDataException ex)
        {
            throw new WebAppException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), "Unable to retrieve preferences.", ex);
        }
    }

    @PATCH
    @Path("preferences")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({ApiConstants.ODCS_API_REGISTERED, ApiConstants.ODCS_API_USER, ApiConstants.ODCS_API_ADMIN})
    @Operation(
        summary = "Update the current user's preferences",
        description = """
            Merges the provided keys into the stored preferences. Keys absent from the request are left \
            unchanged and a key whose value is null is removed. The complete, updated preferences are returned. \
            The stored preferences, serialized as JSON, may not exceed 4000 bytes.""",
        tags = "User",
        requestBody = @RequestBody(
            description = "Preferences to add, replace, or (with a null value) remove.",
            required = true,
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(type = "object", additionalProperties = AdditionalPropertiesValue.TRUE),
                examples = @ExampleObject(value = PREFERENCES_EXAMPLE)
            )
        ),
        responses = {
            @ApiResponse(responseCode = "200", description = "The stored preferences after the update.",
                content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(type = "object", additionalProperties = AdditionalPropertiesValue.TRUE),
                    examples = @ExampleObject(value = PREFERENCES_EXAMPLE))),
            @ApiResponse(responseCode = "400", description = "No preferences were provided, or the result would be too large.",
                content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = Status.class))),
            @ApiResponse(responseCode = "401", description = "No user session established."),
            @ApiResponse(responseCode = "404", description = "The session's user no longer exists.",
                content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = Status.class)))
        }
    )
    public Response updatePreferences(Map<String, Object> changes) throws WebAppException
    {
        var session = requireSession();
        var sessionPrincipal = requirePrincipal(session);
        if (changes == null)
        {
            throw new WebAppException(Response.Status.BAD_REQUEST.getStatusCode(), "No preferences provided.");
        }

        final var db = createDb();
        final var usersDao = db.getDao(UsersDao.class).orElseThrow(() -> UNABLE_TO_GET_USERS_DAO);
        User updated;
        try (var tx = db.newTransaction())
        {
            updated = applyPreferenceChanges(usersDao, tx, sessionPrincipal.getUser().id, changes);
        }
        catch (OpenDcsDataException ex)
        {
            throw new WebAppException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), "Unable to save preferences.", ex);
        }

        // Keep the session's copy current so /check reflects the change without a new login.
        session.setAttribute(OpenDcsPrincipal.USER_PRINCIPAL_SESSION_ATTRIBUTE,
                             new OpenDcsPrincipal(updated, sessionPrincipal.getRoles()));
        return Response.ok(updated.preferences).build();
    }

    /**
     * Merges the changes into the user's stored preferences and saves them within the supplied
     * transaction. The transaction is rolled back and never committed if anything fails.
     */
    private User applyPreferenceChanges(UsersDao usersDao, DataTransaction tx, DbKey userId,
            Map<String, Object> changes) throws WebAppException
    {
        try
        {
            var user = findUser(usersDao, tx, userId);
            var merged = mergePreferences(user.preferences, changes);
            var updated = usersDao.updatePreferences(tx, userId, merged);
            tx.commit();
            return updated;
        }
        catch (OpenDcsDataException ex)
        {
            final var wex = new WebAppException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), "Unable to save preferences.", ex);
            rollbackQuietly(tx, wex);
            throw wex;
        }
    }

    /**
     * Apply a merge-patch style update to a set of preferences: keys in {@code changes} replace
     * the existing value, a null value removes the key, and every other key is kept.
     * @throws WebAppException with a 400 status if a key is blank or the result is too large to store.
     */
    static Map<String, Object> mergePreferences(Map<String, Object> current, Map<String, Object> changes)
            throws WebAppException
    {
        var merged = new HashMap<>(current);
        for (var change : changes.entrySet())
        {
            if (change.getKey() == null || change.getKey().isBlank())
            {
                throw new WebAppException(Response.Status.BAD_REQUEST.getStatusCode(), "Preference names may not be blank.");
            }
            if (change.getValue() == null)
            {
                merged.remove(change.getKey());
            }
            else
            {
                merged.put(change.getKey(), change.getValue());
            }
        }

        if (serializedSize(merged) > MAX_PREFERENCES_BYTES)
        {
            throw new WebAppException(Response.Status.BAD_REQUEST.getStatusCode(),
                    "Preferences may not exceed " + MAX_PREFERENCES_BYTES + " bytes when stored.");
        }
        return merged;
    }

    private static int serializedSize(Map<String, Object> preferences) throws WebAppException
    {
        try
        {
            return MAPPER.writeValueAsString(preferences).getBytes(StandardCharsets.UTF_8).length;
        }
        catch (JsonProcessingException ex)
        {
            throw new WebAppException(Response.Status.BAD_REQUEST.getStatusCode(), "Preferences could not be stored as JSON.", ex);
        }
    }

    private static User findUser(UsersDao usersDao, DataTransaction tx, DbKey userId)
            throws OpenDcsDataException, WebAppException
    {
        return usersDao.getUser(tx, userId)
                       .orElseThrow(() -> new WebAppException(Response.Status.NOT_FOUND.getStatusCode(), "User not found."));
    }

    private HttpSession requireSession() throws WebAppException
    {
        var session = httpRequest.getSession(false);
        if (session == null)
        {
            throw new WebAppException(Response.Status.FORBIDDEN.getStatusCode(), "User not logged in.");
        }
        return session;
    }

    private static OpenDcsPrincipal requirePrincipal(HttpSession session) throws WebAppException
    {
        var sessionPrincipal = (OpenDcsPrincipal)session.getAttribute(OpenDcsPrincipal.USER_PRINCIPAL_SESSION_ATTRIBUTE);
        if (sessionPrincipal == null)
        {
            throw new WebAppException(Response.Status.UNAUTHORIZED.getStatusCode(), "No user session established.");
        }
        return sessionPrincipal;
    }

    /**
     * Verifies the caller's current password and, if it matches, writes the new one within the
     * supplied transaction. The transaction is rolled back and never committed if anything fails.
     */
    private void applyPasswordChange(OpenDcsDatabase db, IdentityProviderDao idpDao, DataTransaction tx,
            OpenDcsPrincipal sessionPrincipal, ApiPasswordChange passwordChange) throws WebAppException
    {
        try
        {
            var idp = findCredentialUpdatingProvider(idpDao, tx, sessionPrincipal.getName());

            // check current password
            if (idp.login(db, tx, new BuiltInProviderCredentials(sessionPrincipal.getName(), passwordChange.currentPassword())).isPresent())
            {
                idp.updateUserCredentials(db, tx, sessionPrincipal.getUser(), new BuiltInProviderCredentials(sessionPrincipal.getName(), passwordChange.newPassword()));
                tx.commit();
            }
            else
            {
                throw new WebAppException(Response.Status.FORBIDDEN.getStatusCode(), "Unable to update password");
            }
        }
        catch (OpenDcsDataException | OpenDcsAuthException ex)
        {
            final var wex = new WebAppException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), "Unable to process password change.", ex);
            rollbackQuietly(tx, wex);
            throw wex;
        }
    }

    /**
     * @return the first provider registered for the subject that is able to update credentials.
     * @throws WebAppException with a 403 status if the subject has no such provider, which is the
     *         case for externally authenticated users whose passwords we do not own.
     */
    private BuiltInIdentityProvider findCredentialUpdatingProvider(IdentityProviderDao idpDao, DataTransaction tx,
            String subject) throws OpenDcsDataException, WebAppException
    {
        for (var provider : idpDao.getIdentityProvidersForSubject(tx, subject))
        {
            if (provider.canUpdateCredentials() && (provider instanceof BuiltInIdentityProvider bidp))
            {
                return bidp;
            }
        }
        throw new WebAppException(Response.Status.FORBIDDEN.getStatusCode(), "Unable to update password");
    }
}
