package org.opendcs.odcsapi.sec.openid;

import static org.opendcs.odcsapi.util.ApiConstants.ODCS_API_GUEST;

import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jwt.SignedJWT;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.StringToClassMapItem;
import io.swagger.v3.oas.annotations.headers.Header;
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
							responseCode = "302",
							description = "Successful authentication.",
                            headers = {
                                @Header(name = "Location", description = "Where the browser should code",
                                    examples = {
                                        @ExampleObject(value = "http://origin/platforms", description = "Default page for successful login"),
                                        @ExampleObject(value = "http://origin/sites", description = "Specific page if user had previosuly logged in."),
                                        @ExampleObject(value = "http://origin/login?errorMsg=<the error>", description = "Error during login."),
                                    }
                                )
                            }
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
					)
			}
	)
    public Response handle(@QueryParam("code") String code,
                           @QueryParam("state") String state,
                           @CookieParam("oidcInfo") Cookie oidcInfoCookie
                           )
    {
        var scheme = httpRequest.getScheme();
        var host = httpRequest.getServerName();
        var port = httpRequest.getServerPort();
        var origin = (port == 80 || port == 443) ? host : (host + ":" + port);
        final String defaultTarget = String.format("%s://%s/login?errorMsg=%%s", scheme,origin);
        // Location will either contain the redirect desired, or a redirect to the /login page with the error
        var location = URI.create(String.format(defaultTarget, URLEncoder.encode("Invalid login.", StandardCharsets.UTF_8)));
        var response = Response.status(Response.Status.FOUND);

        try
        {
            var oidcInfo = jsonMapper.readTree(URLDecoder.decode(oidcInfoCookie.getValue(), StandardCharsets.UTF_8));
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
                        location = URI.create(String.format(defaultTarget, URLEncoder.encode("Unable to handle request.", StandardCharsets.UTF_8)));
                    }
                    else
                    {
                        var userOpt = provider.get().login(db, tx, new AuthCodeCredentials(code));
                        if (userOpt.isPresent())
                        {
                            var user = userOpt.get();
                            response =  updateSessionWithUser(user, httpRequest);
                            location = URI.create(redirectAfterAuth);
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
            log.atDebug().setCause(ex).log("Authentication failed with invalid credentials.");
            location = URI.create(String.format(defaultTarget, URLEncoder.encode("Invalid credentials", StandardCharsets.UTF_8)));
        }
        catch (OpenDcsDataException ex)
        {
            log.atDebug().setCause(ex).log("Authentication failed as we were unable to perform required steps.");
            location = URI.create(String.format(defaultTarget, URLEncoder.encode("Unable to perform credential verification", StandardCharsets.UTF_8)));
        }
        catch (IOException ex)
        {
            log.atDebug().setCause(ex).log("Authentication failed as required information was available or incorrectly formatted.");
            location = URI.create(String.format(defaultTarget, URLEncoder.encode("Unable to process required information.", StandardCharsets.UTF_8)));   
        }
        
        return response.status(Response.Status.FOUND).location(location).build();
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
        if (parts != null && parts.length == 2)
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
