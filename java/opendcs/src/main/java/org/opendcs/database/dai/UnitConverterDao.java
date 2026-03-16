package org.opendcs.database.dai;

import java.util.List;
import java.util.Optional;

import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.OpenDcsDao;
import org.opendcs.database.api.OpenDcsDataException;

import decodes.db.EngineeringUnit;
import decodes.db.UnitConverterDb;
import decodes.sql.DbKey;

public interface UnitConverterDao extends OpenDcsDao
{
    /**
     * Retrieve Unit converter by key
     * @param tx active transaction
     * @param id known UnitConverterDb key
     * @return the UnitConverterDb instance, if found, otherwise empty.
     * @throws OpenDcsDataException
     */
	Optional<UnitConverterDb> getById(DataTransaction tx, DbKey id) throws OpenDcsDataException;

	/**
     * Write/Update specific Unit converter.
     * @param tx active transaction
     * @param UnitConverterDb UnitConverterDb to write
     * @throws OpenDcsDataException
     */
	UnitConverterDb save(DataTransaction tx, UnitConverterDb unitConverter) throws OpenDcsDataException;
	
    /**
     * Remove a specific Unit converter.
     * @param tx active transaction
     * @param id id of the UnitConverterDb to delete.
     * @throws OpenDcsDataException
     */
    void delete(DataTransaction tx, DbKey id) throws OpenDcsDataException;

	/**
     * Given a UnitConverterDb Code attempt to find a matching Unit converter. {@see UnitConverterDb for more information}
     * @param tx active transaction
     * @param UnitConverterDbCode Unit converter code to search for.
     * @return
     * @throws OpenDcsDataException
     */
	Optional<UnitConverterDb> findUnitConverterFor(DataTransaction tx, String fromAbbr, String toAbbr) throws OpenDcsDataException;

    /**
     * Given a UnitConverterDb Code attempt to find a matching Unit converter. {@see UnitConverterDb for more information}
     * @param tx active transaction
     * @param UnitConverterDbCode Unit converter code to search for.
     * @return
     * @throws OpenDcsDataException
     */
	Optional<UnitConverterDb> findUnitConverterFor(DataTransaction tx, EngineeringUnit from, EngineeringUnit to) throws OpenDcsDataException;

	/**
     * Retreive all UnitConverterDbs constrained to a limit and offset if desired.
     *
     * From raw units are excluded. Those unit converters are used by the DecodesScripts and are not 
     * useful for general unit conversion. From raw unit converters are accessible through the getById method.
     *
     * @param tx active transaction
     * @param limit -1 for all, otherwise maximum amount
     * @param offset -1 for no offset, otherwise a valid office from the start of data
     * @return
     * @throws OpenDcsDataException
     */
	List<UnitConverterDb> getUnitConverterDbs(DataTransaction tx, int limit, int offset) throws OpenDcsDataException;

     /**
     * Retreive all UnitConverterDbs constrained to a limit and offset if desired. Limiting to a
     * specific family of measurements, such as length.
     * 
     * From raw units are excluded. Those unit converters are used by the DecodesScripts and are not
     * useful for general unit conversion. From raw unit converters are accessible through the getById method.
     * 
     * @param tx active transaction
     * @param family which measurement to get conversions for. Null means all measures.
     * @param limit -1 for all, otherwise maximum amount
     * @param offset -1 for no offset, otherwise a valid office from the start of data
     * @return
     * @throws OpenDcsDataException
     */
	List<UnitConverterDb> getUnitConverterDbs(DataTransaction tx, String family, int limit, int offset) throws OpenDcsDataException;
}
