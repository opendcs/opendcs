package org.opendcs.odcsapi.sec.openid;

import static org.opendcs.odcsapi.util.ApiConstants.ODCS_API_GUEST;

import java.io.IOException;
import java.net.URI;
import java.text.ParseException;

import org.opendcs.authentication.OpenDcsAuthException;
import org.opendcs.authentication.identityprovider.impl.oidc.AuthCodeCredentials;
import org.opendcs.authentication.identityprovider.impl.oidc.JwtCredentials;
import org.opendcs.database.api.OpenDcsDataException;
import org.opendcs.database.dai.UserManagementDao;
import org.opendcs.database.model.User;
import org.opendcs.odcsapi.errorhandling.WebAppException;
import org.opendcs.odcsapi.res.OpenDcsResource;
import org.opendcs.odcsapi.util.ApiConstants;
import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;
import org.yaml.snakeyaml.util.UriEncoder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jwt.SignedJWT;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.StringToClassMapItem;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.security.RolesAllowed;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import static org.opendcs.odcsapi.sec.SessionResource.updateSessionWithUser;
import static org.opendcs.utils.json.Helpers.getTextField;

@Path("")
public final class OidcCallback extends OpenDcsResource
{
    private static final Logger log = OpenDcsLoggerFactory.getLogger();

    private static final ObjectMapper jsonMapper = new ObjectMapper();

    @Context
	HttpServletRequest httpRequest;

    

    @GET()
    @Path("oidc-callback")
    @Tag(name = "REST - Authentication and Authorization", description = "Endpoints for authentication and authorization.")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({ODCS_API_GUEST})
    @Operation(
			summary = "The ‘oidc-callback’ GET method is the target for AuthCode with client secret redirects",
			description = "Perform Login using provided code to get an id token from the OpenId Connect provider.",
			responses = {
					@ApiResponse(
							responseCode = "200",
							description = "Successful authentication.",
							content = @Content(mediaType = MediaType.APPLICATION_JSON,
								schema = @Schema(implementation = User.class)
							)
					),
					@ApiResponse(
							responseCode = "400",
							description = "Bad request - null or otherwise invalid credentials.",
							content = @Content(mediaType = MediaType.APPLICATION_JSON,
									schema = @Schema(type = "object", implementation = StringToClassMapItem.class),
									examples = @ExampleObject(value = "{\"status\":400," +
											"\"message\": \"Provided credentials are invalid.\"}"))
					),
					@ApiResponse(
							responseCode = "403",
							description = "Invalid credentials or insufficient role.",
							content = @Content(mediaType = MediaType.APPLICATION_JSON,
									schema = @Schema(type = "object", implementation = StringToClassMapItem.class),
									examples = @ExampleObject(value = "{\"status\":403," +
											"\"message\":\"Failed to authorize user.\"}"))
					),
					@ApiResponse(
							responseCode = "500",
							description = "Internal Server Error"
					),
					@ApiResponse(
							responseCode = "501",
							description = "This authentication method is only supported by the OpenTSDB database.",
							content = @Content(mediaType = MediaType.APPLICATION_JSON,
									schema = @Schema(type = "object", implementation = StringToClassMapItem.class),
									examples = @ExampleObject(value = "{\"status\":501," +
											"\"message\":\"Basic Auth is not supported.\"}"))
					)
			}
	)
    public Response handle(@QueryParam("code") String code,
                           @QueryParam("state") String state,
                           @CookieParam("oidcInfo") Cookie oidcInfoCookie
                           ) throws WebAppException
    {
         var response = Response.status(Response.Status.UNAUTHORIZED)
					           .entity("""
                            {"message": "Invalid Credentials."}
                        """);
        try
        {
            var oidcInfo = jsonMapper.readTree(UriEncoder.decode(oidcInfoCookie.getValue()));
            var stateFromSession = getTextField(oidcInfo, "state");
            var oidcProvider = getTextField(oidcInfo, "provider");
            var redirectAfterAuth = getTextField(oidcInfo, "redirectAfterAuth");

            if (state != null && stateFromSession != null &&  state.equals(stateFromSession))
            {
                log.info("Starting login attempt.");
                var db = createDb();

                try (var tx = db.newTransaction())
                {
                    var provider = db.getDao(UserManagementDao.class).orElseThrow()
                                    .getIdentityProvider(tx, oidcProvider);
                    if (provider.isEmpty()) 
                    {
                        return Response.notAcceptable(null).build();
                    }
                    else
                    {
                        var userOpt = provider.get().login(db, tx, new AuthCodeCredentials(code));
                        if (userOpt.isPresent())
                        {
                            var user = userOpt.get();
                            response =  updateSessionWithUser(user, httpRequest);
                            response.location(URI.create(redirectAfterAuth));
                            response.status(Response.Status.FOUND);
                        }
                    }
                }
            }
            else
            {
                log.warn("Invalid login attempt.");
            }
        }
        catch (OpenDcsAuthException ex)
        {
            throw new WebAppException(Response.Status.UNAUTHORIZED.getStatusCode(), "Invalid credentials", ex);
        }
        catch (OpenDcsDataException ex)
        {
            throw new WebAppException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), "Unable to perform credential verification", ex);
        }
        catch (IOException ex)
        {
            throw new WebAppException(Response.Status.BAD_REQUEST.getStatusCode(), "Unable to process required information.", ex);   
        }
        return response.build();
    }


    @POST
	@Path("login_jwt")
    @Tag(name = "REST - Authentication and Authorization", description = "Endpoints for authentication and authorization.")
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({ApiConstants.ODCS_API_GUEST})
	@Operation(
			summary = "The ‘login_jwt’ POST method is used to start a new session using a JWT as an authenticator",
			description = "Perform Login using retrieved JWT, such as through use of the AuthCode + PKCE flow.",
			responses = {
					@ApiResponse(
							responseCode = "200",
							description = "Successful authentication.",
							content = @Content(mediaType = MediaType.APPLICATION_JSON,
								schema = @Schema(implementation = User.class)
							)
					),
					@ApiResponse(
							responseCode = "400",
							description = "Bad request - null or otherwise invalid credentials.",
							content = @Content(mediaType = MediaType.APPLICATION_JSON,
									schema = @Schema(type = "object", implementation = StringToClassMapItem.class),
									examples = @ExampleObject(value = "{\"status\":400," +
											"\"message\": \"Provided credentials are invalid.\"}"))
					),
					@ApiResponse(
							responseCode = "403",
							description = "Invalid credentials or insufficient role.",
							content = @Content(mediaType = MediaType.APPLICATION_JSON,
									schema = @Schema(type = "object", implementation = StringToClassMapItem.class),
									examples = @ExampleObject(value = "{\"status\":403," +
											"\"message\":\"Failed to authorize user.\"}"))
					),
					@ApiResponse(
							responseCode = "500",
							description = "Internal Server Error"
					),
					@ApiResponse(
							responseCode = "501",
							description = "This authentication method is only supported by the OpenTSDB database.",
							content = @Content(mediaType = MediaType.APPLICATION_JSON,
									schema = @Schema(type = "object", implementation = StringToClassMapItem.class),
									examples = @ExampleObject(value = "{\"status\":501," +
											"\"message\":\"Basic Auth is not supported.\"}"))
					)
			}
	)
	public Response postJwt(@HeaderParam("Authorization") String authHeader) throws WebAppException
	{
        var response = Response.status(Response.Status.UNAUTHORIZED)
					           .entity("""
                            {"message": "Invalid Credentials."}
                        """);
        final String[] parts = authHeader.split(" ");
        if (parts.length == 2)
        {
            log.info("Starting login attempt.");
            final String accessToken = parts[1];
            SignedJWT jwt;
            try
            {
                jwt = SignedJWT.parse(accessToken);
                var subject = jwt.getJWTClaimsSet().getSubject();
                final var db = this.createDb();
                try (var tx = db.newTransaction())
                {
                    var umDao = db.getDao(UserManagementDao.class).orElseThrow(() -> new OpenDcsDataException("UserManagement not currently supported."));
                    var idps = umDao.getIdentityProvidersForSubject(tx, subject);
                    if (!idps.isEmpty())
                    {   
                        for(var idp: idps)
                        {
                            var userOpt = idp.login(db, tx, new JwtCredentials(accessToken));
                            if (userOpt.isPresent())
                            {
                                var user = userOpt.get();
                                response = updateSessionWithUser(user, httpRequest);
                                break;
                            }
                        }   
                    }
                }
            }
            catch (OpenDcsAuthException ex)
            {
                throw new WebAppException(Response.Status.UNAUTHORIZED.getStatusCode(), "Invalid credentials", ex);
            }
            catch (OpenDcsDataException ex)
            {
                throw new WebAppException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), "Unable to perform credential verification", ex);
            }
            catch (ParseException ex)
            {
                throw new WebAppException(Response.Status.BAD_REQUEST.getStatusCode(), "Unable to process provided credentials", ex);
            }
        }
        else
        {
            log.warn("Invalid login attempt.");
        }
		return response.build();
	}



}
