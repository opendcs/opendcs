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

package org.opendcs.odcsapi.sec.cwms;

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

public final class CwmsAuthorizationDAO extends ApiDaoBase implements ApiAuthorizationDAI
{
	private static final Logger LOGGER = LoggerFactory.getLogger(CwmsAuthorizationDAO.class);

	private CwmsAuthorizationDAO(DbInterface dbi)
	{
		super(dbi, "CwmsAuthorizationDAO");
	}

	@Override
	public Set<OpenDcsApiRoles> getRoles(String username) throws DbException
	{
		Set<OpenDcsApiRoles> roles = EnumSet.noneOf(OpenDcsApiRoles.class);
		roles.add(OpenDcsApiRoles.ODCS_API_GUEST);
		String q = "select user_group_id " +
				"from cwms_20.av_sec_users " +
				"where db_office_code = cwms_20.cwms_util.get_db_office_code(:input_db_office_code) " +
				"  and upper(username) = case " +
				"     when instr(:username_str, '.', -1) > 0 then " +
				"        (select userid " +
				"           from cwms_20.at_sec_cwms_users " +
				"           where edipi = substr(:username_str, instr(:username_str, '.', -1) + 1)) " +
				"         else " +
				"            upper(:username_str) " +
				"      end " +
				"  and is_member = 'T';";
		String cwmsOfficeId = DbInterface.decodesProperties.getProperty("CwmsOfficeId");
		try(ResultSet rs = doQueryPs(null, q, cwmsOfficeId, username))
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
			return roles;
		}
		catch(SQLException ex)
		{
			throw new DbException(module, ex, "Unable to determine user roles for user: " + username
					+ " and office: " + cwmsOfficeId);
		}
	}

	@AutoService(value = DAOProvider.class)
	public static final class AuthorizationDAOProvider implements DAOProvider
	{

		@Override
		public boolean provides(Class<?> type, String dbType)
		{
			return ApiAuthorizationDAI.class.equals(type) && DbInterface.CWMS.equals(dbType);
		}

		@Override
		public CwmsAuthorizationDAO createDAO(DbInterface dbInterface)
		{
			return new CwmsAuthorizationDAO(dbInterface);
		}
	}
}
