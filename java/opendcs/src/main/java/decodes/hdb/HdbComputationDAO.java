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
package decodes.hdb;

import java.sql.ResultSet;
import java.sql.SQLException;

import decodes.sql.DbKey;
import decodes.tsdb.ConstraintException;
import decodes.tsdb.DbIoException;
import opendcs.dao.ComputationDAO;
import opendcs.dao.DatabaseConnectionOwner;

public class HdbComputationDAO extends ComputationDAO
{

	public HdbComputationDAO(DatabaseConnectionOwner tsdb)
	{
		super(tsdb);
	}

	@Override
	public void deleteComputation( DbKey id )
		throws DbIoException, ConstraintException
	{
		try
		{
			String q = "select count(*) from R_BASE"
                + " where COMPUTATION_ID = " + id;
			ResultSet rs = doQuery(q);
			if (rs.next())
			{
				int n = rs.getInt(1);
				if (n > 0)
					throw new ConstraintException(
						"Cannot delete computation with ID=" + id
						+ " because data in R_BASE relies on it.");
			}
			super.deleteComputation(id);
		}
		catch(SQLException ex)
		{
			String msg = "Error deleting computation with ID=" + id;
			throw new DbIoException(msg, ex);
		}
	}

}
