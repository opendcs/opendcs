package org.opendcs.database.model;

import org.opendcs.database.api.OpenDcsDataException;

import decodes.sql.DbKey;
import opendcs.util.functional.ThrowingFunction;

public final class IdentityProviderMapping
{
    /**
     * Identity Provider associated to this user+subject.
     */
    public final IdentityProvider provider;
    /**
     * unique identifier used for this user in the given identity provider..
     */
    public final String subject;

    public IdentityProviderMapping(IdentityProvider provider, String subject)
    {
        this.provider = provider;
        this.subject = subject;
    }

    public static class Builder
    {
        private DbKey provider;
        private String subject;

        public IdentityProviderMapping build(ThrowingFunction<DbKey, IdentityProvider, OpenDcsDataException> providerGetter) throws OpenDcsDataException
        {
            return new IdentityProviderMapping(providerGetter.accept(provider), subject);
        }

        public Builder withProviderId(DbKey providerId)
        {
            this.provider = providerId;
            return this;
        }

        public Builder withSubject(String subject)
        {
            this.subject = subject;
            return this;
        }
    }
}