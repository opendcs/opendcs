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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.EnumSet;
import java.util.Set;

import opendcs.dao.DaoBase;
import opendcs.dao.DatabaseConnectionOwner;
import org.opendcs.odcsapi.dao.ApiAuthorizationDAI;
import org.opendcs.odcsapi.dao.DbException;
import org.opendcs.odcsapi.hydrojson.DbInterface;
import org.opendcs.odcsapi.sec.OpenDcsApiRoles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CwmsAuthorizationDAO extends DaoBase implements ApiAuthorizationDAI
{
	private static final Logger LOGGER = LoggerFactory.getLogger(CwmsAuthorizationDAO.class);

	public CwmsAuthorizationDAO(DatabaseConnectionOwner tsdb)
	{
		super(tsdb, "AuthorizationDAO");
	}

	@Override
	public Set<OpenDcsApiRoles> getRoles(String username) throws DbException
	{
		Set<OpenDcsApiRoles> roles = EnumSet.noneOf(OpenDcsApiRoles.class);
		roles.add(OpenDcsApiRoles.ODCS_API_GUEST);
		String q = "select user_group_id " +
				"from cwms_20.av_sec_users " +
				"where db_office_code = cwms_20.cwms_util.get_db_office_code(?) " +
				"  and upper(username) = case " +
				"     when instr(?, '.', -1) > 0 then " +
				"        (select userid " +
				"           from cwms_20.at_sec_cwms_users " +
				"           where edipi = substr(?, instr(?, '.', -1) + 1)) " +
				"         else " +
				"            upper(?) " +
				"      end " +
				"  and is_member = 'T'";
		String cwmsOfficeId = DbInterface.decodesProperties.getProperty("CwmsOfficeId");
		try
		{
			withConnection(c ->
			{
				try(PreparedStatement statement = c.prepareStatement(q))
				{
					statement.setString(1, cwmsOfficeId);
					statement.setString(2, username);
					statement.setString(3, username);
					statement.setString(4, username);
					statement.setString(5, username);
					try(ResultSet rs = statement.executeQuery())
					{
						while(rs.next())
						{
							String role = rs.getString(1);
							LOGGER.info("User '{}' has role {}", username, role);
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
				}
			});
			return roles;
		}
		catch(SQLException ex)
		{
			throw new DbException("Unable to determine user roles for user: " + username
					+ " and office: " + cwmsOfficeId, ex);
		}
	}
}
