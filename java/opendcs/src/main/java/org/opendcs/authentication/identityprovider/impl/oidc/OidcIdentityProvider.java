package org.opendcs.authentication.identityprovider.impl.oidc;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.opendcs.authentication.IdentityProviderCredentials;
import org.opendcs.authentication.OpenDcsAuthException;
import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.OpenDcsDataException;
import org.opendcs.database.api.OpenDcsDatabase;
import org.opendcs.database.dai.UserManagementDao;
import org.opendcs.database.dai.UsersDao;
import org.opendcs.database.model.IdentityProvider;
import org.opendcs.database.model.IdentityProviderMapping;
import org.opendcs.database.model.User;
import org.opendcs.database.model.UserBuilder;
import org.opendcs.spi.authentication.IdentityProviderProvider;
import org.opendcs.utils.WebUtility;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auto.service.AutoService;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.source.JWKSourceBuilder;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.jwt.proc.BadJWTException;

import decodes.sql.DbKey;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.security.SecurityScheme.Type;

public final class OidcIdentityProvider implements IdentityProvider
{
    public static final String TYPE = "OpenIdConnect";

    private static final ObjectMapper jsonMapper = new ObjectMapper();

    private final DbKey id;
    private final String name;
    private final ZonedDateTime updatedAt;
    private final Map<String, Object> config;

    private final OpenIdConfiguration oidcConfig;
    private final String clientSecret;
    private final String clientId;
    private final String redirectUri;
    private final Map<String, List<String>> queryParameters = new HashMap<>();


    public OidcIdentityProvider(DbKey id, String name, ZonedDateTime updateAt, Map<String, Object> configMap)
    {
        this.id = id;
        this.name = name;
        this.updatedAt = updateAt;
        this.config = configMap;
        this.clientSecret = (String)configMap.get("clientSecret");
        this.clientId = (String)configMap.get("clientId");
        this.redirectUri = (String)configMap.get("redirectUri");
        this.oidcConfig = new OpenIdConfiguration(URI.create((String)configMap.get("wellKnown")));

        final var queryParams = configMap.get("queryParameters");
        if (queryParams != null && queryParams instanceof Map<?, ?> parms)
        {
            parms.forEach((parameter, values) ->
            {
                if (values instanceof List<?> valuesList)
                {
                    if (valuesList.getFirst() instanceof String)
                    {
                        queryParameters.put((String)parameter, (List<String>)valuesList); // NOSONAR type is checked
                    }
                }
                else if (values instanceof String valueString)
                {
                    queryParameters.put((String)parameter, List.of(valueString));
                }
            });
        }
    }


    public OpenIdConfiguration getOidcConfiguration()
    {
        return oidcConfig;
    }

    @Override
    public DbKey getId()
    {
        return id;
    }

    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public String getType()
    {
        return TYPE;
    }

    @Override
    public ZonedDateTime getUpdatedAt()
    {
        return updatedAt;
    }

    @Override
    public Map<String, Object> configToMap()
    {
        return config;
    }

    @Override
    public Optional<User> login(OpenDcsDatabase db, DataTransaction tx, IdentityProviderCredentials credentials)
            throws OpenDcsAuthException
    {
        switch(credentials)
        {
            case AuthCodeCredentials acc: return loginAuthCode(db, tx, acc);
            case JwtCredentials jc: return loginJwt(db, tx, jc);
            default: return Optional.empty();
        }
    }


    private Optional<User> loginAuthCode(OpenDcsDatabase db, DataTransaction tx, AuthCodeCredentials credentials)
            throws OpenDcsAuthException
    {

        StringBuilder sb = new StringBuilder();
        sb.append("grant_type=authorization_code&")
          .append("client_id=").append(URLEncoder.encode(clientId, StandardCharsets.UTF_8)).append("&")
          .append("client_secret=").append(URLEncoder.encode(clientSecret, StandardCharsets.UTF_8)).append("&")
          .append("code=").append(URLEncoder.encode(credentials.code(), StandardCharsets.UTF_8)).append("&")
          .append("redirect_uri=").append(URLEncoder.encode(redirectUri, StandardCharsets.UTF_8))
        ;

        var request = HttpRequest.newBuilder(oidcConfig.getTokenUri())
                                .header("Content-Type", "application/x-www-form-urlencoded")
                                .POST(HttpRequest.BodyPublishers.ofString(sb.toString()))
                                .build();


        try (var client = HttpClient.newBuilder()
                                    .sslContext(WebUtility.sslContext(WebUtility.TRUST_EXISTING_CERTIFICATES))
                                    .build())
        {
            var response = client.send(request, BodyHandlers.ofString());
            if (response.statusCode() != 200)
            {
                throw new IOException(String.format("""
                    Request for client information was not expected. Status Code: %d, Reason: %s
                            """, response.statusCode(), response.body()));
            }
            var json = jsonMapper.readTree(response.body());
            if (json.has("id_token"))
            {
                var idToken = json.get("id_token").asText();
                var user = getUserFromToken(db, tx, idToken);

                if (user.isEmpty() && canRegister())
                {
                    user = Optional.of(register(db, tx, new JwtCredentials(idToken)));
                }
                return user;
            }
            else
            {
                throw new IOException("id_token was not provided by authentication provider.");
            }
        }
        catch (InterruptedException ex)
        {
            Thread.currentThread().interrupt();
            throw new OpenDcsAuthException("Unable to validate authentication data. Request could not complete.", ex);
        }
        catch (IOException ex)
        {
            throw new OpenDcsAuthException("Unable to validate authentication data.", ex);
        }
    }

    private Optional<User> getUserFromToken(OpenDcsDatabase db, DataTransaction tx, String token) throws OpenDcsAuthException
    {
        try
        {
            var claims = verifyTokenAndGetClaims(oidcConfig, token);
            var subject = claims.getSubject();
            var umDao = db.getDao(UsersDao.class).orElseThrow();
            return umDao.getUsers(tx, -1, -1)
                        .stream()
                        .filter(u -> u.identityProviders.stream()
                                                          .filter(idpm -> idpm.provider.getId().equals(this.id)
                                                                         && idpm.subject.equals(subject)).count() == 1)
                        .findFirst();
        }
        catch (IOException | ParseException | BadJOSEException | JOSEException | OpenDcsDataException ex)
        {
            throw new OpenDcsAuthException("Unable to validate authentication data.", ex);
        }
    }

    private Optional<User> loginJwt(OpenDcsDatabase db, DataTransaction tx, JwtCredentials credentials)
            throws OpenDcsAuthException
    {
        return getUserFromToken(db, tx, credentials.accessToken());
    }

    private JWTClaimsSet verifyTokenAndGetClaims(OpenIdConfiguration config, String token) throws MalformedURLException, BadJOSEException, ParseException, JOSEException
    {
        var keySource = JWKSourceBuilder.create(config.getJwksUri().toURL()).retrying(true).build();
        return JwtVerifier.getInstance().getClaimsSet(keySource, token, oidcConfig.getIssuer(), this.clientId);
    }

    @Override
    public void updateUserCredentials(OpenDcsDatabase db, DataTransaction tx, User user,
            IdentityProviderCredentials credentials) throws OpenDcsAuthException
    {
        /* unable, and can update credentials will return false */
    }

    @Override
    public boolean canRegister()
    {
        return true;
    }

    @Override
    public User register(OpenDcsDatabase db, DataTransaction tx, IdentityProviderCredentials credentials) throws OpenDcsAuthException
    {
        try
        {
            final var umDao = db.getDao(UserManagementDao.class)
                                .orElseThrow(() -> new OpenDcsAuthException("Unable to register new user as a UserManagmentDao instance is not available."));
            final var claims = verifyTokenAndGetClaims(oidcConfig, ((JwtCredentials)credentials).accessToken());
            final var subject = claims.getSubject();
            final var email = claims.getStringClaim("email");
            final var preferredUserName = claims.getStringClaim("preferred_username");

            final var idpMapping = new IdentityProviderMapping(this, subject);
            final var user = new UserBuilder()
                                .withEmail(email)
                                .withIdentityMapping(idpMapping)
                                .withPreference("preferredUserName", preferredUserName)
                                .build();
            return umDao.addUser(tx, user);
        }
        catch (MalformedURLException | BadJOSEException | ParseException | JOSEException ex)
        {
            throw new OpenDcsAuthException("Unable to validate provided credentials", ex);
        }
        catch (OpenDcsDataException ex)
        {
            throw new OpenDcsAuthException("Unable to save new user.", ex);
        }
    }

    @Override
    public SecurityScheme getSecurityScheme()
    {
        var scheme =  new SecurityScheme().openIdConnectUrl(this.oidcConfig.wellKnownUri.toString());
        scheme.type(Type.OPENIDCONNECT).setDescription("OpenID Connect based Authorization");

        HashMap<String, Object> extension = new HashMap<>();
        extension.put("queryParameters", this.queryParameters);

        HashMap<String, Object> oidcData = new HashMap<>();
        oidcData.put("redirectUri", this.redirectUri);
        oidcData.put("clientId", clientId);
        boolean usePkce = (this.clientSecret == null || this.clientSecret.isBlank());
        oidcData.put("usePkce", usePkce);

        extension.put("oidcConfig", oidcData);
        scheme.addExtension("x-logincomponent-configuration", extension);
        return scheme;
    }

    /**
     * Determine if this particular instance can authenticate a given JWT.
     * Both the Aud/AZP need to have the clientID as well as the issuer matching
     * to distinguish between different clients from the same issuer.
     * @param jwt successfully parsed, but not validated, JWT with claims.
     * @return whether or not this provider is appropriate to the given JWT.
     * @throws OpenDcsAuthException
     */
    public boolean validFor(SignedJWT jwt) throws OpenDcsAuthException
    {
        boolean ret = true;
        var oidcConfig = this.getOidcConfiguration();

        try {
            var claims = jwt.getJWTClaimsSet();
            if (!oidcConfig.getIssuer().equals(claims.getIssuer()))
            {
                ret = false;
            }

            // we don't just assing here as we're only changing the value if one of the check
            // is false. We never want to set it back to true
            if (!JwtVerifier.clientAudienceMatches(claims, clientId))
            {
                ret = false;
            }
        }
        catch (ParseException ex)
        {
            // don't care, we can't process the claims.
            ret = false;
        }
        return ret;
    }

    @AutoService(IdentityProviderProvider.class)
    public static class OidcIdentityProviderProvider implements IdentityProviderProvider
    {
        @Override
        public IdentityProvider create(DbKey id, String name, ZonedDateTime updatedAt, Map<String, Object> config)
        {
            return new OidcIdentityProvider(id, name, updatedAt, config);
        }

        @Override
        public String getName()
        {
            return TYPE;
        }
    }
}
