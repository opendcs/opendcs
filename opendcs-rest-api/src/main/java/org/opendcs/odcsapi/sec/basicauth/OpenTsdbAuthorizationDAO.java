/*
 *  Copyright 2024 OpenDCS Consortium and its Contributors
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

package org.opendcs.odcsapi.sec.basicauth;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.EnumSet;
import java.util.Set;

import com.google.auto.service.AutoService;
import org.opendcs.odcsapi.dao.ApiAuthorizationDAI;
import org.opendcs.odcsapi.dao.ApiDaoBase;
import org.opendcs.odcsapi.dao.DAOProvider;
import org.opendcs.odcsapi.dao.DbException;
import org.opendcs.odcsapi.hydrojson.DbInterface;
import org.opendcs.odcsapi.sec.OpenDcsApiRoles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class OpenTsdbAuthorizationDAO extends ApiDaoBase implements ApiAuthorizationDAI
{
	private static final Logger LOGGER = LoggerFactory.getLogger(OpenTsdbAuthorizationDAO.class);

	private OpenTsdbAuthorizationDAO(DbInterface dbi)
	{
		super(dbi, "OpenTsdbAuthorizationDAO");
	}

	@Override
	public Set<OpenDcsApiRoles> getRoles(String username) throws DbException
	{
		Set<OpenDcsApiRoles> roles = EnumSet.noneOf(OpenDcsApiRoles.class);
		roles.add(OpenDcsApiRoles.ODCS_API_GUEST);
		// Now verify that user has appropriate privilege. This only works on Postgress currently:
		String q = "select pm.roleid, pr.rolname from pg_auth_members pm, pg_roles pr"
				+ " where pm.member = (select oid from pg_roles where upper(rolname) = upper(?))"
				+ " and pm.roleid = pr.oid";
		try(ResultSet rs = doQueryPs(null, q, username))
		{
			while(rs.next())
			{
				int roleid = rs.getInt(1);
				String role = rs.getString(2);
				LOGGER.info("User '{}' has role {}={}", username, roleid, role);
				if("OTSDB_ADMIN".equalsIgnoreCase(role))
				{
					roles.add(OpenDcsApiRoles.ODCS_API_ADMIN);
				}
				if("OTSDB_MGR".equalsIgnoreCase(role))
				{
					roles.add(OpenDcsApiRoles.ODCS_API_USER);
				}
			}
			return roles;
		}
		catch(SQLException ex)
		{
			throw new DbException(module, ex, "Unable to determine user roles in the database.");
		}
	}

	@AutoService(value = DAOProvider.class)
	public static final class AuthorizationDAOProvider implements DAOProvider
	{
		@Override
		public boolean provides(Class<?> type, String dbType)
		{
			return ApiAuthorizationDAI.class.equals(type) && DbInterface.OPENTSDB.equals(dbType);
		}

		@Override
		public OpenTsdbAuthorizationDAO createDAO(DbInterface dbInterface)
		{
			return new OpenTsdbAuthorizationDAO(dbInterface);
		}
	}
}
