package org.opendcs.database.dai;

import java.util.List;
import java.util.Optional;

import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.OpenDcsDao;
import org.opendcs.database.api.OpenDcsDataException;

import decodes.db.EngineeringUnit;

public interface EngineeringUnitDao extends OpenDcsDao
{
	/**
     * Write/Update specific EngineeringUnit.
     * @param tx active transaction
     * @param EngineeringUnit EngineeringUnit to write
     * @throws OpenDcsDataException
     */
	EngineeringUnit save(DataTransaction tx, EngineeringUnit unit) throws OpenDcsDataException;
	
    /**
     * Remove a specific EngineeringUnit .
     * @param tx active transaction
     * @param unitAbberviation short name of the EngineeringUnit to delete.
     * @throws OpenDcsDataException
     */
    void delete(DataTransaction tx, String unitAbbreviation) throws OpenDcsDataException;

	/**
     * Given a EngineeringUnit name or abbreviation attempt to find a matching unit. {@see EngineeringUnit for more information}
     * @param tx active transaction
     * @param unit unit to search for either by name or abbreviation.
     * @return
     * @throws OpenDcsDataException
     */
	Optional<EngineeringUnit> getByName(DataTransaction tx, String unit) throws OpenDcsDataException;

	/**
     * Retreive all EngineeringUnits constrained to a limit and office if desired.
     * @param tx active transaction
     * @param limit -1 for all, otherwise maximum amount
     * @param offset -1 for no offset, otherwise a valid office from the start of data
     * @return
     * @throws OpenDcsDataException
     */
	List<EngineeringUnit> getEngineeringUnits(DataTransaction tx, int limit, int offset) throws OpenDcsDataException;

}
