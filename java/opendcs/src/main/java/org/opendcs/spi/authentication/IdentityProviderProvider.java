package org.opendcs.spi.authentication;

import java.time.ZonedDateTime;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.ServiceLoader;

import org.opendcs.database.model.IdentityProvider;

import decodes.sql.DbKey;

public interface IdentityProviderProvider
{
    String getName();
    IdentityProvider create(DbKey id, String name, ZonedDateTime updatedAt, Map<String, Object> config);

    static IdentityProviderProvider createFor(String type) throws UnsupportedProviderException
    {
        Objects.requireNonNull(type, "Provider type must be given.");
        ServiceLoader<IdentityProviderProvider> providers = ServiceLoader.load(IdentityProviderProvider.class);
        providers.reload();
        Iterator<IdentityProviderProvider> iter = providers.iterator();
        while (iter.hasNext())
        {
            IdentityProviderProvider provider = iter.next();
            if (provider.getName().equals(type))
            {
                return provider;
            }
        }

        throw new UnsupportedProviderException("Not Identity Provider named '" + type + "'");
    }
}
