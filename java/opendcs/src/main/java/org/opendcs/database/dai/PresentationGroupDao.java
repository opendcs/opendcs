package org.opendcs.database.dai;

import java.util.List;
import java.util.Optional;

import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.OpenDcsDao;
import org.opendcs.database.api.OpenDcsDataException;

import decodes.db.PresentationGroup;
import decodes.sql.DbKey;

public interface PresentationGroupDao extends OpenDcsDao
{
    Optional<PresentationGroup> getById(DataTransaction tx, DbKey id) throws OpenDcsDataException;

    Optional<PresentationGroup> getByName(DataTransaction tx, String name) throws OpenDcsDataException;

    PresentationGroup save(DataTransaction tx, PresentationGroup group) throws OpenDcsDataException;

    void delete(DataTransaction tx, DbKey id) throws OpenDcsDataException;

    List<PresentationGroup> getAll(DataTransaction tx, int limit, int offset) throws OpenDcsDataException;
}