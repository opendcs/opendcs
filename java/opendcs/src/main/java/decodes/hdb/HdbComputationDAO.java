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
			String msg = "Error deleting computation with ID=" + id + ": " + ex;
			warning(msg);
			throw new DbIoException(msg);
		}
	}

}
