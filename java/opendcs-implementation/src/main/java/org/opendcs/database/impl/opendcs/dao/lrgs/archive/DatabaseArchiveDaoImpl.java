package org.opendcs.database.impl.opendcs.dao.lrgs.archive;

import java.util.List;

import org.opendcs.data.lrgs.archive.ArchiveEntry;
import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.OpenDcsDataException;
import org.opendcs.database.dao.lrgs.archive.LrgsArchiveDao;

import mil.army.usace.hec.services.annotations.ServiceProvider;

@ServiceProvider(service = LrgsArchiveDao.class)
public class DatabaseArchiveDaoImpl implements LrgsArchiveDao
{
    @Override
    public ArchiveEntry archive(DataTransaction tx, ArchiveEntry entry) throws OpenDcsDataException
    {
        return null;
    }

    @Override
    public List<ArchiveEntry> getAllBy(DataTransaction tx, int limit, int offset, LrgsSearchCriteria criteria,
            OrderBy order) throws OpenDcsDataException
    {
        return List.of();
    }
}
