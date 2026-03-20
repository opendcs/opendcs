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
import java.util.Map;
import java.util.Optional;

import org.opendcs.authentication.IdentityProviderCredentials;
import org.opendcs.authentication.OpenDcsAuthException;
import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.OpenDcsDataException;
import org.opendcs.database.api.OpenDcsDatabase;
import org.opendcs.database.dai.UserManagementDao;
import org.opendcs.database.model.IdentityProvider;
import org.opendcs.database.model.User;
import org.opendcs.spi.authentication.IdentityProviderProvider;
import org.opendcs.utils.WebUtility;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auto.service.AutoService;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.source.JWKSourceBuilder;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jwt.JWTClaimsSet;

import decodes.sql.DbKey;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.Scopes;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.security.SecurityScheme.In;
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


    public OidcIdentityProvider(DbKey id, String name, ZonedDateTime updateAt, Map<String, Object> configMap)
    {
        this.id = id;
        this.name = name;
        this.updatedAt = updateAt;
        this.config = configMap;
        this.clientSecret = (String)configMap.get("clientSecret");
        this.clientId = (String)configMap.get("clientId");
        this.redirectUri = (String)configMap.get("redirectUri");
        try
        {
            this.oidcConfig = new OpenIdConfiguration(URI.create((String)configMap.get("wellKnown")));
        }
        catch (IOException ex)
        {
            throw new OidcConfigurationException("Unable to process well known configuration for provider " + name, ex);
        }
        
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

        var request = HttpRequest.newBuilder(oidcConfig.tokenUri)
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
                return getUserFromToken(db, tx, idToken);
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
            var umDao = db.getDao(UserManagementDao.class).orElseThrow();
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
        var keySource = JWKSourceBuilder.create(config.jwksUri.toURL()).retrying(true).build();
        return JwtVerifier.getInstance().getClaimsSet(keySource, token, oidcConfig.issuer);
    }

    @Override
    public void updateUserCredentials(OpenDcsDatabase db, DataTransaction tx, User user,
            IdentityProviderCredentials credentials) throws OpenDcsAuthException
    {
        /* unable, and can update credentials will return false */
    }
    
    @Override
    public SecurityScheme getSecurityScheme()
    {
        var scheme =  new SecurityScheme().openIdConnectUrl(this.oidcConfig.wellKnownUri.toString());
        scheme.type(Type.OPENIDCONNECT).setDescription("OpenID Connect based Authorization");

        HashMap<String, Object> extension = new HashMap<>();

        HashMap<String, Object> oidcData = new HashMap<>();
        oidcData.put("redirectUri", this.redirectUri);
        oidcData.put("clientId", clientId);

        extension.put("oidcConfig", oidcData);
        scheme.addExtension("x-logincomponent-configuration", extension);
        return scheme;
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
