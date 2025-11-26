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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import decodes.sql.DbKey;

public class UserBuilder
{
    DbKey id = DbKey.NullKey;
    String email = null;
    ArrayList<IdentityProviderMapping> idpMap = new ArrayList<>();
    ArrayList<Role> roles = new ArrayList<>();
    ZonedDateTime createdAt = null;
    ZonedDateTime updatedAt = null;
    HashMap<String, Object> preferences = new HashMap<>();

    public User build()
    {
        return new User(this);
    }

    public UserBuilder withId(DbKey id)
    {
        this.id = id;
        return this;
    }

    public UserBuilder withEmail(String email)
    {
        this.email = email;
        return this;
    }

    public UserBuilder withPreferences(Map<String, Object> preferences)
    {
        this.preferences.putAll(preferences);
        return this;
    }

    public UserBuilder withPreference(String key, Object value)
    {
        this.preferences.put(key, value);
        return this;
    }

    public UserBuilder withCreatedAt(ZonedDateTime createdAt)
    {
        this.createdAt = createdAt;
        return this;
    }

    public UserBuilder withUpdatedAt(ZonedDateTime updatedAt)
    {
        this.updatedAt = updatedAt;
        return this;
    }

    public UserBuilder withRoles(List<Role> roles)
    {
        this.roles.addAll(roles);
        return this;
    }

    public UserBuilder withRole(Role role)
    {
        this.roles.add(role);
        return this;
    }

    public UserBuilder withIdentityMappings(List<IdentityProviderMapping> mappings)
    {
        this.idpMap.addAll(mappings);
        return this;
    }

    public UserBuilder withIdentityMapping(IdentityProviderMapping mapping)
    {
        this.idpMap.add(mapping);
        return this;
    }
}
