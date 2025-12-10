/*
 *  Copyright 2025 OpenDCS Consortium and its Contributors
 *
 *  Licensed under the Apache License, Version 2.0 (the "License")
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.opendcs.odcsapi.sec.cwms;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.EnumSet;
import java.util.Set;

import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.OpenDcsDataException;
import org.opendcs.odcsapi.dao.ApiAuthorizationDAI;
import org.opendcs.odcsapi.dao.DbException;
import org.opendcs.odcsapi.sec.OpenDcsApiRoles;
import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

public final class CwmsAuthorizationDAO implements ApiAuthorizationDAI
{
    private static final Logger log = OpenDcsLoggerFactory.getLogger();

	@Override
	public Set<OpenDcsApiRoles> getRoles(DataTransaction tx, String username, String organizationId) throws DbException
	{
        Set<OpenDcsApiRoles> roles = EnumSet.noneOf(OpenDcsApiRoles.class);
        roles.add(OpenDcsApiRoles.ODCS_API_GUEST);
        String q = """
            with inputs as (select ? as username, ? as office_id from dual)
            select avsu.user_group_id
            from cwms_20.av_sec_users avsu, inputs
            where avsu.db_office_code = cwms_20.cwms_util.get_db_office_code(inputs.office_id)
            and upper(avsu.username) = case
                when instr(inputs.username, '.', -1) > 0 then
                    (select userid
                    from cwms_20.at_sec_cwms_users
                    where edipi = substr(inputs.username, instr(inputs.username, '.', -1) + 1))
                    else
                        upper(inputs.username)
                end
            and avsu.is_member = 'T'
        """;
		try(Connection c = tx.connection(Connection.class).orElseThrow();
			PreparedStatement statement = c.prepareStatement(q))
        {
			statement.setString(1, username);
			statement.setString(2, organizationId);

			try(ResultSet rs = statement.executeQuery())
			{
				while(rs.next())
				{
					String role = rs.getString(1);
					log.info("User '{}' has role {}", username, role);
					if("CCP Mgr".equalsIgnoreCase(role))
					{
						roles.add(OpenDcsApiRoles.ODCS_API_ADMIN);
					}
					if("CCP Proc".equalsIgnoreCase(role))
					{
						roles.add(OpenDcsApiRoles.ODCS_API_USER);
					}
				}
			}
            return roles;
        }
		catch(SQLException | OpenDcsDataException ex)
        {
            throw new DbException("Unable to determine user roles for user: " + username
                    + " and office: " + organizationId, ex);
        }
    }
}
