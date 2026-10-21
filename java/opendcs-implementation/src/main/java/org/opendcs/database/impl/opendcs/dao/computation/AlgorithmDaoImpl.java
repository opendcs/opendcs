package org.opendcs.database.impl.opendcs.dao.computation;

import java.util.List;
import java.util.Optional;

import org.opendcs.database.DatabaseKey;
import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.OpenDcsDataException;
import org.opendcs.database.dai.AlgorithmDao;
import org.openide.util.lookup.ServiceProvider;

import decodes.tsdb.DbCompAlgorithm;

@ServiceProvider (service = AlgorithmDao.class)
public class AlgorithmDaoImpl implements AlgorithmDao
{

    @Override
    public Optional<DbCompAlgorithm> getById(DataTransaction tx, DatabaseKey id) throws OpenDcsDataException
    {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getById'");
    }

    @Override
    public Optional<DbCompAlgorithm> getByName(DataTransaction tx, String name) throws OpenDcsDataException
    {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getByName'");
    }

    @Override
    public DbCompAlgorithm save(DataTransaction tx, DbCompAlgorithm entry) throws OpenDcsDataException
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
    public List<DbCompAlgorithm> getAll(DataTransaction tx, int limit, int offset, boolean expand)
            throws OpenDcsDataException
    {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getAll'");
    }
    
}
