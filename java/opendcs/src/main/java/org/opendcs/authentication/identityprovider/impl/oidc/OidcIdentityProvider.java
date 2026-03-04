package org.opendcs.authentication.identityprovider.impl.oidc;

import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Optional;

import org.opendcs.authentication.IdentityProviderCredentials;
import org.opendcs.authentication.OpenDcsAuthException;
import org.opendcs.authentication.identityprovider.impl.builtin.BuiltInIdentityProvider;
import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.OpenDcsDatabase;
import org.opendcs.database.model.IdentityProvider;
import org.opendcs.database.model.User;
import org.opendcs.spi.authentication.IdentityProviderProvider;

import com.google.auto.service.AutoService;

import decodes.sql.DbKey;

public final class OidcIdentityProvider implements IdentityProvider
{
    public static final String TYPE = "OpenIdConnect";
    private final DbKey id;
    private final String name;
    private final ZonedDateTime updatedAt;
    private final Map<String, Object> config;


    private final String issuer;
    private final String clientSecret;


    public OidcIdentityProvider(DbKey id, String name, ZonedDateTime updateAt, Map<String, Object> configMap)
    {
        this.id = id;
        this.name = name;
        this.updatedAt = updateAt;
        this.config = configMap;
        this.issuer = (String)configMap.get("issuer");
        this.clientSecret = (String)configMap.get("clientSecret"); 
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
        return Optional.empty();
    }

    @Override
    public void updateUserCredentials(OpenDcsDatabase db, DataTransaction tx, User user,
            IdentityProviderCredentials credentials) throws OpenDcsAuthException {
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
