package org.opendcs.database.dai;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.OpenDcsDao;
import org.opendcs.database.api.OpenDcsDataException;
import org.opendcs.database.api.OpenDcsDataRuntimeException;

import decodes.db.PlatformStatus;
import decodes.sql.DbKey;

public interface PlatformStatusDao extends OpenDcsDao
{
    /**
	 * Read the status record for a given platform
     * @param tx active transaction for this request
	 * @param platformId the platform ID
	 * @return the PlatformStatus record, or empty if none currently stored
	 * @throws OpenDcsDataException on any database error error
     * @throws OpenDcsDataRuntimeException on any runtime database error
	 */
	Optional<PlatformStatus> getByPlatformId(DataTransaction tx, DbKey platformId) throws OpenDcsDataException;

	/**
	 * Write a platform status record.
     * @param tx active transaction for this request
	 * @param platformStatus the record to write
	 * @throws OpenDcsDataException on any database error error
     * @throws OpenDcsDataRuntimeException on any runtime database error
	 */
	PlatformStatus updatePlatformStatus(DataTransaction tx, PlatformStatus platformStatus) throws OpenDcsDataException;
	
	/**
	 * Deletes the status record for this platform, if one exists.
	 * @param tx active transaction for this request
     * @param platformId the platform ID
	 * @throws OpenDcsDataException on any database error error
     * @throws OpenDcsDataRuntimeException on any runtime database error
	 */
	void deletePlatformStatus(DataTransaction tx, DbKey platformId) throws OpenDcsDataException;

    /**
	 * Used by GUIs and report generators to list all platform status records.
     * @param tx active transaction for this request
     * @param limit max number to return in this request
     * @param offset starting point in list to return for this request
	 * @return an array list with all current platform status records
	 * @throws OpenDcsDataException on any database error error
     * @throws OpenDcsDataRuntimeException on any runtime database error
	 */
	List<PlatformStatus> getAll(DataTransaction tx, int limit, int offset) throws OpenDcsDataException;

	/**
	 * Read the status records for a given netlist
	 * @param netlistId the netlist ID
     * @param limit max number to return in this request
     * @param offset starting point in list to return for this request
	 * @return the PlatformStatus records, or null if none currently stored
     * @throws OpenDcsDataException on any database error error
     * @throws OpenDcsDataRuntimeException on any runtime database error* @throws DbIoException on any I/O error
	 */
	List<PlatformStatus> getPlatformStatusForNetList(DataTransaction tx, DbKey netlistId, int limit, int offset) throws OpenDcsDataException;
	
}
