package org.opendcs.database.dai;

import java.util.List;
import java.util.Optional;

import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.OpenDcsDao;
import org.opendcs.database.api.OpenDcsDataException;

import decodes.db.EngineeringUnit;
import decodes.db.UnitConverterDb;
import decodes.sql.DbKey;

public interface UnitConversionDao extends OpenDcsDao
{
    /**
     * Retrieve data type by key
     * @param tx active transaction
     * @param id known UnitConverterDb key
     * @return the UnitConverterDb instance, if found, otherwise empty.
     * @throws OpenDcsDataException
     */
	Optional<UnitConverterDb> getById(DataTransaction tx, DbKey id) throws OpenDcsDataException;

	/**
     * Write/Update specific data type.
     * @param tx active transaction
     * @param UnitConverterDb UnitConverterDb to write
     * @throws OpenDcsDataException
     */
	UnitConverterDb save(DataTransaction tx, UnitConverterDb unitConverter) throws OpenDcsDataException;
	
    /**
     * Remove a specific data type.
     * @param tx active transaction
     * @param id id of the UnitConverterDb to delete.
     * @throws OpenDcsDataException
     */
    void delete(DataTransaction tx, DbKey id) throws OpenDcsDataException;

	/**
     * Given a UnitConverterDb Code attempt to find a matching data type. {@see UnitConverterDb for more information}
     * @param tx active transaction
     * @param UnitConverterDbCode data type code to search for.
     * @return
     * @throws OpenDcsDataException
     */
	Optional<UnitConverterDb> lookup(DataTransaction tx, String fromAbbr, String toAbbr) throws OpenDcsDataException;

    /**
     * Given a UnitConverterDb Code attempt to find a matching data type. {@see UnitConverterDb for more information}
     * @param tx active transaction
     * @param UnitConverterDbCode data type code to search for.
     * @return
     * @throws OpenDcsDataException
     */
	Optional<UnitConverterDb> lookup(DataTransaction tx, EngineeringUnit from, EngineeringUnit to) throws OpenDcsDataException;

	/**
     * Retreive all UnitConverterDbs constrained to a limit and office if desired.
     * @param tx active transaction
     * @param limit -1 for all, otherwise maximum amount
     * @param offset -1 for no offset, otherwise a valid office from the start of data
     * @return
     * @throws OpenDcsDataException
     */
	List<UnitConverterDb> getUnitConverterDbs(DataTransaction tx, int limit, int offset) throws OpenDcsDataException;
}
