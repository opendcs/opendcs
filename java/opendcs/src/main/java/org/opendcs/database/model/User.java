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

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import decodes.sql.DbKey;

/**
 * Holds information about a particular user
 */
public class User
{
    /**
     * Unique identifier of user within OpenDCS itself
     */
    public final DbKey id;
    /**
     * User preferences, generally key -> value,
     * but can be nested objects or arrays depending on needs.
     */
    public final Map<String, Object> preferences;

    public final String email;
    public final ZonedDateTime createdAt;
    public final ZonedDateTime updatedAt;
    public final List<Role> roles;
    public final String password;
    /**
     * List of external identity providers that are tied to this user account.
     */
    public final List<IdentityProviderMapping> identityProviders;


    private User(Builder builder)
    {
        this.id = builder.id;
        this.preferences = builder.preferences;
        this.email = builder.email;
        this.createdAt = builder.createdAt;
        this.updatedAt = builder.updatedAt;
        this.roles = Collections.unmodifiableList(builder.roles);
        this.password = builder.password;
        this.identityProviders = Collections.unmodifiableList(builder.idpMap);
    }

    public static class Builder
    {
        DbKey id = DbKey.NullKey;
        String email = null;
        ArrayList<IdentityProviderMapping> idpMap = new ArrayList<>();
        ArrayList<Role> roles = new ArrayList<>();
        String password = null;
        ZonedDateTime createdAt = null;
        ZonedDateTime updatedAt = null;
        HashMap<String, Object> preferences = new HashMap<>();

        public User build()
        {
            return new User(this);
        }

        public Builder withId(DbKey id)
        {
            this.id = id;
            return this;
        }

        public Builder withEmail(String email)
        {
            this.email = email;
            return this;
        }

        public Builder withPreferences(Map<String, Object> preferences)
        {
            this.preferences.putAll(preferences);
            return this;
        }

        public Builder withPreference(String key, Object value)
        {
            this.preferences.put(key, value);
            return this;
        }

        public Builder withCreatedAt(ZonedDateTime createdAt)
        {
            this.createdAt = createdAt;
            return this;
        }

        public Builder withUpdatedAt(ZonedDateTime updatedAt)
        {
            this.updatedAt = updatedAt;
            return this;
        }

        public Builder withRoles(List<Role> roles)
        {
            this.roles.addAll(roles);
            return this;
        }

        public Builder withRole(Role role)
        {
            this.roles.add(role);
            return this;
        }

        public Builder withIdentityMappings(List<IdentityProviderMapping> mappings)
        {
            this.idpMap.addAll(mappings);
            return this;
        }

        public Builder withIdentityMapping(IdentityProviderMapping mapping)
        {
            this.idpMap.add(mapping);
            return this;
        }

        public Builder withPassword(String password)
        {
            this.password = password;
            return this;
        }
    }
}
