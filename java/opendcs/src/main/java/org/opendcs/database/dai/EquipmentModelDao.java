package org.opendcs.database.dai;

import java.util.List;
import java.util.Optional;

import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.OpenDcsDao;
import org.opendcs.database.api.OpenDcsDataException;

import decodes.db.EquipmentModel;
import decodes.sql.DbKey;

public interface EquipmentModelDao extends OpenDcsDao
{
    /**
     * Retrieve list of equipment models configured in this database.
     * @param tx Transaction object for the request.
     * @param limit max number of rows. (-1 for all).
     * @param offset start row. (-1 for no offset).
     * @return
     * @throws OpenDcsDataException
     */
    List<EquipmentModel> getEquipmentModels(DataTransaction tx, int limit, int offset) throws OpenDcsDataException;

    /**
     * Retrieve Equipment Model by ID
     * @param tx Transaction object for the request
     * @param id Equipment Model Surrogate key
     * @return Equipment Model if found, otherwise empty.
     * @throws OpenDcsDataException
     */
    Optional<EquipmentModel> getEquipmentModel(DataTransaction tx, DbKey id) throws OpenDcsDataException;

    /**
     * Retrieve EquipmentModel by name.
     * @param tx Transaction object for the request.
     * @param name equipment model name name to look for. Comparison will be upper(name) = upper(name) in database.
     * @return Equipment Model if found, otherwise empty.
     */
    Optional<EquipmentModel> getEquipmentModel(DataTransaction tx, String name) throws OpenDcsDataException;

    /**
     * Remove a given Equipment model from the database.
     * @param tx Transaction object for the request
     * @param id
     * @throws OpenDcsDataException Error deleting data. Most likely due to references to the
     * given model
     */
    void deleteEquipmentModel(DataTransaction tx, DbKey id) throws OpenDcsDataException;

    /**
     * Save (write new or update) a given equipment model. Saved object is returned populated with any
     * generated fields. If saving new equipment model the DAO will generate or acquire the ID.
     * Usages *SHOULD* abandon their passed in instance in favor of the returned value.
     *
     * @param tx Transaction object for the request
     * @param em Equipment model to use
     * @return
     * @throws OpenDcsDataException
     */
    EquipmentModel saveEquipmentModel(DataTransaction tx, EquipmentModel em) throws OpenDcsDataException;
}
