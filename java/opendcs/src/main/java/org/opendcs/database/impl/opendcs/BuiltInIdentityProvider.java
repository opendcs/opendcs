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
package org.opendcs.database.impl.opendcs;

import java.time.ZonedDateTime;
import java.util.Map;

import org.opendcs.database.model.IdentityProvider;
import org.opendcs.spi.authentication.IdentityProviderProvider;

import com.google.auto.service.AutoService;

import decodes.sql.DbKey;

/**
 * Identity Provider that marks an internal user.
 * Always active, at least used for initial setup/breakfix admin
 * account.
 */
public final class BuiltInIdentityProvider implements IdentityProvider
{
    public static final String TYPE = "BuiltIn";
    private final DbKey id;
    private final String name;
    private final ZonedDateTime updatedAt;
    private final Map<String, Object> config;

    public BuiltInIdentityProvider(DbKey id, String name, ZonedDateTime updateAt, Map<String, Object> configMap)
    {
        this.id = id;
        this.name = name;
        this.updatedAt = updateAt;
        this.config = configMap;
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

    @AutoService(IdentityProviderProvider.class)
    public static class BuiltInIdentityProviderProvider implements IdentityProviderProvider
    {

        @Override
        public IdentityProvider create(DbKey id, String name, ZonedDateTime updatedAt, Map<String, Object> config)
        {
            return new BuiltInIdentityProvider(id, name, updatedAt, config);
        }

        @Override
        public String getName()
        {
            return TYPE;
        }
    }
}
