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

package org.opendcs.odcsapi.dao.cwms;

import java.util.List;

import org.jdbi.v3.core.Handle;
import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.OpenDcsDataException;
import org.opendcs.odcsapi.beans.ApiOrganization;
import org.opendcs.odcsapi.dao.DbException;
import org.opendcs.odcsapi.dao.OrganizationDao;

public final class CwmsOrganizationDao implements OrganizationDao
{

	@Override
	public List<ApiOrganization> retrieveOrganizationIds(DataTransaction tx, int limit, int offset) throws DbException
	{
		try
		{
			Handle handle = tx.connection(Handle.class).orElseThrow();
			String queryStr = "SELECT OFFICE_ID, LONG_NAME, REPORT_TO_OFFICE_ID FROM CWMS_V_OFFICE";
			if (limit > 0)
			{
				queryStr += " OFFSET :offset ROWS FETCH NEXT :limit ROWS ONLY";
			}
			try(var query = handle.createQuery(queryStr))
			{
				if (limit > 0)
				{
					query.bind("limit", limit);
					query.bind("offset", offset);
				}
				return query.map((rs, ctx) -> new ApiOrganization(rs.getString(1), rs.getString(2), rs.getString(3)))
					.list();
			}
		}
		catch(OpenDcsDataException ex)
		{
			throw new DbException("Unable to connect to the database.", ex);
		}
	}
}
