package org.opendcs.database.dai;

import java.util.List;
import java.util.Optional;

import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.OpenDcsDao;
import org.opendcs.database.api.OpenDcsDataException;

import decodes.db.DataSource;
import decodes.sql.DbKey;

public interface DataSourceDao extends OpenDcsDao
{
    Optional<DataSource> getDataSource(DataTransaction tx, DbKey id) throws OpenDcsDataException;

    Optional<DataSource> getDataSource(DataTransaction tx, String name) throws OpenDcsDataException; 
    

    DataSource save(DataTransaction tx, DataSource dataSource) throws OpenDcsDataException;

    void delete(DataTransaction tx, DbKey id) throws OpenDcsDataException;

    List<DataSource> getDataSources(DataTransaction tx, int limit, int offset) throws OpenDcsDataException;
    
}
