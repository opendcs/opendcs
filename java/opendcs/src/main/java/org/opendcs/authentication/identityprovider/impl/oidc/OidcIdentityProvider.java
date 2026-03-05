package org.opendcs.authentication.identityprovider.impl.oidc;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.time.ZonedDateTime;
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
import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auto.service.AutoService;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.JWKSourceBuilder;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;

import decodes.sql.DbKey;

public final class OidcIdentityProvider implements IdentityProvider
{
    private static final Logger log = OpenDcsLoggerFactory.getLogger();
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
        if(credentials instanceof AuthCodeCredentials acc)
        {
            return loginAuthCode(db, tx, acc);
        }
        else
        {
            return Optional.empty();
        }
    }


    private Optional<User> loginAuthCode(OpenDcsDatabase db, DataTransaction tx, AuthCodeCredentials credentials)
            throws OpenDcsAuthException
    {

        StringBuilder sb = new StringBuilder();
        sb.append("grant_type=authorization_code&")
          .append("client_id=").append(clientId).append("&")
          .append("client_secret=").append(URLEncoder.encode(clientSecret, StandardCharsets.UTF_8)).append("&")
          .append("code=").append(URLEncoder.encode(credentials.code(), StandardCharsets.UTF_8)).append("&")
          .append("redirect_uri=").append(URLEncoder.encode(redirectUri, StandardCharsets.UTF_8))
          ;

        var request = HttpRequest.newBuilder(oidcConfig.tokenUri)
                                .header("Content-Type", "application/x-www-form-urlencoded")
                                .POST(HttpRequest.BodyPublishers.ofString(sb.toString()))
                                .build();
        
        
        try (var client = HttpClient.newHttpClient()) // TODO: figure out the hole ssl context thing
        {
            var response = client.send(request, BodyHandlers.ofString());
            var json = jsonMapper.readTree(response.body());
            var idToken = json.get("id_token").asText();
            var jwtProcessor = new DefaultJWTProcessor<>();
            var keySource =JWKSourceBuilder.create(oidcConfig.jwksUri.toURL()).retrying(true).build();
            var selector = new JWSVerificationKeySelector<>(JWSAlgorithm.RS256, keySource);
            jwtProcessor.setJWSKeySelector(selector);
            var claims = jwtProcessor.process(idToken, null);
            var subject = claims.getSubject();
            log.info("User is {}", subject);

            var umDao = db.getDao(UserManagementDao.class).orElseThrow();
            return umDao.getUsers(tx, -1, -1).stream().filter(u -> u.identityProviders.stream().filter(idpm -> idpm.subject.equals(subject)).count() == 1).findFirst();
        }
        catch (IOException | InterruptedException | ParseException | BadJOSEException | JOSEException | OpenDcsDataException ex)
        {
            throw new OpenDcsAuthException("Unable to validate authentication data.", ex);
        }
    }

    @Override
    public void updateUserCredentials(OpenDcsDatabase db, DataTransaction tx, User user,
            IdentityProviderCredentials credentials) throws OpenDcsAuthException
    {
        /* unable, and can update credentials will return false */
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
