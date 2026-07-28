package org.opendcs.database.impl.opendcs.dao;

import java.util.List;
import java.util.Optional;

import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.OpenDcsDataException;
import org.opendcs.database.dai.RoutingSpecDao;

import decodes.db.RoutingSpec;
import decodes.sql.DbKey;

public class RoutingSpecDaoImpl implements RoutingSpecDao
{

    @Override
    public Optional<RoutingSpec> getById(DataTransaction tx, DbKey id) throws OpenDcsDataException
     {
        return Optional.empty();
    }

    @Override
    public Optional<RoutingSpec> getByName(DataTransaction arg0, String arg1) throws OpenDcsDataException
    {
        return Optional.empty();
    }

    @Override
    public RoutingSpec save(DataTransaction arg0, RoutingSpec arg1) throws OpenDcsDataException 
    {
        return null;
    }
    
    @Override
    public void delete(DataTransaction tx, DbKey id) throws OpenDcsDataException
    {
        /* empty */
    }

    @Override
    public List<RoutingSpec> getAll(DataTransaction tx, int limit, int offset, boolean includeAll, String forSchedule)
            throws OpenDcsDataException 
    {
        return List.of();
    }

}
