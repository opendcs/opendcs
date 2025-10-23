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
package org.opendcs.database.model.mappers.user;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.jdbi.v3.core.statement.StatementContext;
import org.opendcs.database.model.IdentityProvider;
import org.opendcs.database.model.IdentityProviderMapping;
import org.opendcs.database.model.mappers.IdentityProviderMapper;
import org.opendcs.database.model.mappers.PrefixRowMapper;

public final class IdentityProviderMappingMapper extends PrefixRowMapper<IdentityProviderMapping>
{
    private final IdentityProviderMapper idpMapper;

    private IdentityProviderMappingMapper(String prefix)
    {
        super(prefix);
        idpMapper = IdentityProviderMapper.withPrefix(prefix);
    }

    @Override
    public IdentityProviderMapping map(ResultSet rs, StatementContext ctx) throws SQLException
    {

        IdentityProvider provider = idpMapper.map(rs, ctx);
        String subject = rs.getString(prefix+"subject");

        return new IdentityProviderMapping(provider, subject);
    }

    public static IdentityProviderMappingMapper withPrefix(String prefix)
    {
        return new IdentityProviderMappingMapper(prefix);
    }
}
