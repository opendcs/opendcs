package org.opendcs.database.impl.opendcs.dao.computation;

import java.util.List;
import java.util.Optional;

import org.opendcs.database.DatabaseKey;
import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.OpenDcsDataException;
import org.opendcs.database.dai.ComputationDao;
import org.openide.util.lookup.ServiceProvider;

import decodes.tsdb.DbComputation;

@ServiceProvider(service = ComputationDao.class)
public class ComputationDaoImpl implements ComputationDao
{

    @Override
    public Optional<DbComputation> getById(DataTransaction tx, DatabaseKey id) throws OpenDcsDataException
    {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getById'");
    }

    @Override
    public Optional<DbComputation> getByName(DataTransaction tx, String name) throws OpenDcsDataException
    {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getByName'");
    }

    @Override
    public DbComputation save(DataTransaction tx, DbComputation entry) throws OpenDcsDataException
    {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'save'");
    }

    @Override
    public void delete(DataTransaction tx, DatabaseKey id) throws OpenDcsDataException
    {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'delete'");
    }

    @Override
    public List<DbComputation> getAll(DataTransaction tx, int limit, int offset, boolean expand)
            throws OpenDcsDataException
    {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getAll'");
    }
    
}
