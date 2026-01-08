/*
 *  Copyright 2025 OpenDCS Consortium and its Contributors
 *
 *  Licensed under the Apache License, Version 2.0 (the "License")
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.opendcs.odcsapi.sec.basicauth;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Set;
import javax.sql.DataSource;

import decodes.util.DecodesSettings;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.StringToClassMapItem;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.security.RolesAllowed;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.OpenDcsDataException;
import org.opendcs.database.api.OpenDcsDatabase;
import org.opendcs.database.dai.UserManagementDao;
import org.opendcs.database.model.User;
import org.opendcs.odcsapi.dao.ApiAuthorizationDAI;
import org.opendcs.odcsapi.errorhandling.WebAppException;
import org.opendcs.odcsapi.res.OpenDcsResource;
import org.opendcs.odcsapi.sec.OpenDcsApiRoles;
import org.opendcs.odcsapi.sec.OpenDcsPrincipal;
import org.opendcs.odcsapi.sec.cwms.CwmsAuthorizationDAO;
import org.opendcs.odcsapi.util.ApiConstants;
import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import static org.opendcs.odcsapi.res.DataSourceContextCreator.DATA_SOURCE_ATTRIBUTE_KEY;

@Path("/")
@Tag(name = "REST - Authentication and Authorization", description = "Endpoints for authentication and authorization.")
public final class BasicAuthResource extends OpenDcsResource
{

	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	private static final String MODULE = "BasicAuthResource";

	@Context
	private HttpServletRequest httpServletRequest;
	@Context
	private HttpHeaders httpHeaders;

	@POST
	@Path("credentials")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({ApiConstants.ODCS_API_GUEST})
	@Operation(
			summary = "The ‘credentials’ POST method is used to obtain a new token",
			description = """
					The user name and password provided must be a valid login for the underlying database.
					Also, that user must be assigned either of the roles OTSDB_ADMIN or OTSDB_MGR.
					---
					Starting in **API Version 0.0.3**, authentication credentials (username and password) \
					may be passed as shown above in the POST body.
					They may also be passed in a GET call to the 'credentials' method, \
					(e.g. '*http://localhost:8080/odcsapi/credentials*') containing an HTTP Authentication Basic \
					header in the form 'username:password'.
					
					The returned data to the GET call will be empty.""",
			requestBody = @RequestBody(
					description = "Login Credentials",
					required = true,
					content = @Content(
							mediaType = MediaType.APPLICATION_JSON,
							schema = @Schema(implementation = Credentials.class)
					)
			),
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
											"\"message\": \"Neither username nor password may be null.\"}"))
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
	public Response postCredentials(Credentials credentials) throws WebAppException
	{
		//If credentials are null, Authorization header will be checked.
		if(credentials != null)
		{
			verifyCredentials(credentials);
		}

		String authorizationHeader = httpHeaders.getHeaderString(HttpHeaders.AUTHORIZATION);
		credentials = getCredentials(credentials, authorizationHeader);
		validateDbCredentials(credentials);
		Set<OpenDcsApiRoles> roles = getUserRoles(credentials.getUsername(), organizationId);
		OpenDcsPrincipal principal = new OpenDcsPrincipal(credentials.getUsername(), roles);
		HttpSession oldSession = httpServletRequest.getSession(false);
		if(oldSession != null)
		{
			oldSession.invalidate();
		}
		HttpSession session = httpServletRequest.getSession(true);
		
		session.setAttribute(OpenDcsPrincipal.USER_PRINCIPAL_SESSION_ATTRIBUTE, principal);
		HashMap<String,String> ret = new HashMap<>();
		ret.put("email", credentials.getUsername());
		return Response.ok()
				.entity(ret)
				.build();
	}

	private static void verifyCredentials(Credentials credentials)
	{
		String u = credentials.getUsername();
		String p = credentials.getPassword();
		if(u == null || u.trim().isEmpty() || p == null || p.trim().isEmpty())
		{
			throw new BadRequestException("Neither username nor password may be null.");
		}
		for(int i = 0; i < u.length(); i++)
		{
			char c = u.charAt(i);
			if(!Character.isLetterOrDigit(c) && c != '_' && c != '.')
			{
				throw new BadRequestException("Username may only contain alphanumeric, underscore, or period.");
			}
		}
		for(int i = 0; i < p.length(); i++)
		{
			char c = p.charAt(i);
			if(Character.isWhitespace(c) || c == '\'')
			{
				throw new BadRequestException("Password may not contain whitespace or quote.");
			}
		}
	}

	private static Credentials getCredentials(Credentials postBody, String authorizationHeader)
	{
		if(postBody != null)
		{
			return postBody;
		}
		if(authorizationHeader == null || authorizationHeader.isEmpty())
		{
			throw newAuthException();
		}
		return parseAuthorizationHeader(authorizationHeader);
	}

	private static BadRequestException newAuthException()
	{
		return new BadRequestException("Credentials not provided.");
	}

	private static Credentials parseAuthorizationHeader(String authString)
	{
		String[] authHeaders = authString.split(",");
		for(String header : authHeaders)
		{
			String trimmedHeader = header.trim();
			log.debug(MODULE + ".makeToken authHdr = {}", trimmedHeader);
			if(trimmedHeader.startsWith("Basic"))
			{
				return extractCredentials(trimmedHeader.substring(6).trim());
			}
		}
		throw newAuthException();
	}

	private static Credentials extractCredentials(String base64Credentials)
	{
		String decodedCredentials = new String(Base64.getDecoder().decode(base64Credentials.getBytes()));
		String[] parts = decodedCredentials.split(":", 2);

		if(parts.length < 2 || parts[0] == null || parts[1] == null
				|| parts[0].isEmpty() || parts[1].isEmpty())
		{
			throw newAuthException();
		}

		Credentials credentials = new Credentials();
		credentials.setUsername(parts[0]);
		credentials.setPassword(parts[1]);

		log.info(MODULE + ".checkToken found tokstr in header.");
		return credentials;
	}

	private void validateDbCredentials(Credentials creds) throws WebAppException
	{
		var db = this.createDb();
		// Use username and password to attempt to connect to the database

		try (var tx = db.newTransaction())
		{
			var userMgmt = db.getDao(UserManagementDao.class).orElseThrow()
			var user userMgmt.getUser(tx, )
			/*
			 Intentional unused connection. Makes a new db connection using passed credentials
			 This validates the username & password and will throw SQLException if user/pw is not valid.
			*/
			DataSource dataSource = (DataSource) context.getAttribute(DATA_SOURCE_ATTRIBUTE_KEY);
			//noinspection EmptyTryBlock
			try (Connection ignored = dataSource.getConnection(creds.getUsername(), creds.getPassword()))
			{// NOSONAR

			}
		}
		catch(OpenDcsDataException | SQLException ex)
		{
			throw new WebAppException(Response.Status.FORBIDDEN.getStatusCode(), "Unable to authorize user.", ex);
		}
	}

	private Set<OpenDcsApiRoles> getUserRoles(String username, String organizationId)
	{
		OpenDcsDatabase db = createDb();
		ApiAuthorizationDAI dao = getAuthDao(db);
		try(DataTransaction tx = db.newTransaction())
		{
			return dao.getRoles(tx, username, organizationId);
		}
		catch(Exception ex)
		{
			throw new IllegalStateException("Unable to query the database for user authorization", ex);
		}
	}

	private ApiAuthorizationDAI getAuthDao(OpenDcsDatabase db)
	{
		String databaseType = db.getSettings(DecodesSettings.class).orElseThrow().editDatabaseType;
		// Username+Password login only supported by OpenTSDB
		if("opentsdb".equalsIgnoreCase(databaseType))
		{
			return new OpenTsdbAuthorizationDAO();
		}
		else if("cwms".equalsIgnoreCase(databaseType))
		{
			return new CwmsAuthorizationDAO();
		}
		throw new UnsupportedOperationException("Endpoint is unsupported by the OpenDCS REST API.");
	}
}
