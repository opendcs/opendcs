package org.opendcs.database.dai;

import decodes.db.Site;
import decodes.db.SiteName;
import decodes.sql.DbKey;

import java.util.List;
import java.util.Optional;

import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.OpenDcsDao;
import org.opendcs.database.api.OpenDcsDataException;

public interface SiteDao extends OpenDcsDao
{
    Optional<Site> getById(DataTransaction tx, DbKey id) throws OpenDcsDataException;

    Optional<Site> getBySiteName(DataTransaction tx, SiteName siteName) throws OpenDcsDataException;

    Optional<Site> getByAnySiteName(DataTransaction tx, String siteName) throws OpenDcsDataException;

    Site save(DataTransaction tx, Site site) throws OpenDcsDataException;

    void delete(DataTransaction tx, DbKey id) throws OpenDcsDataException;

    List<Site> getAll(DataTransaction tx, int limit, int offset) throws OpenDcsDataException;
}
