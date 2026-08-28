package org.opendcs.database.impl.opendcs.dao;

import java.util.List;
import java.util.Optional;

import org.jdbi.v3.stringtemplate4.StringTemplateSqlLocator;
import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.OpenDcsDataException;
import org.opendcs.database.dai.PlatformStatusDao;
import org.openide.util.lookup.ServiceProvider;
import org.stringtemplate.v4.STGroup;

import decodes.db.PlatformStatus;
import decodes.sql.DbKey;

@ServiceProvider(service = PlatformStatusDao.class)
public class PlatformStatusDaoImpl implements PlatformStatusDao
{
    private final STGroup queries;

    public PlatformStatusDaoImpl()
    {
        queries = StringTemplateSqlLocator.findStringTemplateGroup(PlatformStatusDaoImpl.class);
    }

    @Override
    public Optional<PlatformStatus> getByPlatformId(DataTransaction tx, DbKey platformId) throws OpenDcsDataException
    {
        return Optional.empty();
    }

    @Override
    public PlatformStatus updatePlatformStatus(DataTransaction tx, PlatformStatus platformStatus) throws OpenDcsDataException
    {
        return null;
    }

    @Override
    public void deletePlatformStatus(DataTransaction tx, DbKey platformId) throws OpenDcsDataException
    {
        
    }

    @Override
    public List<PlatformStatus> getAll(DataTransaction tx, int limit, int offset) throws OpenDcsDataException
    {
        return List.of();
    }

    @Override
    public List<PlatformStatus> getPlatformStatusForNetList(DataTransaction tx, DbKey netlistId, int limit, int offset)
            throws OpenDcsDataException 
    {
        return List.of();
    }
}
