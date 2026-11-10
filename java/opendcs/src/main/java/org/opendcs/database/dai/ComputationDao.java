package org.opendcs.database.dai;

import org.opendcs.database.dao.BasicDao;

import decodes.tsdb.DbComputation;

/**
 * Definition of Computation DAO
 * 
 * Will likely bring the listComps(Predicate<...) back from the original design,
 * just with something more generic so it can be applied directly to the database query.
 * 
 * ComputationDao
 */
public interface ComputationDao extends BasicDao<DbComputation>
{
}
