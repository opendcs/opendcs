/*
* Where Applicable, Copyright 2025 OpenDCS Consortium and/or its contributors
*
* Licensed under the Apache License, Version 2.0 (the "License"); you may not
* use this file except in compliance with the License. You may obtain a copy
* of the License at
*
*   http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
* License for the specific language governing permissions and limitations
* under the License.
*/
package org.opendcs.database.model;

import org.opendcs.database.api.OpenDcsDataException;

import decodes.sql.DbKey;
import opendcs.util.functional.ThrowingFunction;

/**
 * association of this user to an identity provider
 */
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