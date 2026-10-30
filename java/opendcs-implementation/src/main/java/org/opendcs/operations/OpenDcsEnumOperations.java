package org.opendcs.operations;

import java.util.Optional;

import org.opendcs.database.api.OpenDcsDataException;
import org.opendcs.database.api.OpenDcsDataRuntimeException;
import org.opendcs.database.api.OpenDcsDatabase;
import org.opendcs.database.dai.EnumDao;
import org.opendcs.operations.enums.EnumOperations;
import org.openide.util.lookup.ServiceProvider;

import decodes.db.DbEnum;

/**
 * Handle retrieving OpenDCS DbEnums from the database. In the future, with caching.
 * OpenDcsEnumOperations
 */
@ServiceProvider(service = EnumOperations.class)
public class OpenDcsEnumOperations implements EnumOperations
{
    private OpenDcsDatabase db;
    private EnumDao enumDao;

    /**    (non-Javadoc)
     * Retrieve an Enum from the database. Current implementation always
     * opens a new transaction. The DataSource used by the Database should allow more than once
     * connection open in case this is called in a transaction.
     * @see org.opendcs.operations.enums.EnumOperations#getEnum(java.lang.String)
     */
    @Override
    public Optional<DbEnum> getEnum(String enumName)
    {
        try (var tx = db.newTransaction())
        {
            return enumDao.getEnum(tx, enumName);
        }
        catch (OpenDcsDataException ex)
        {
            throw new OpenDcsDataRuntimeException(ex);
        }
    }

    @Override
    public void injectDatabase(OpenDcsDatabase db)
    {
        this.db = db;
        this.enumDao = db.getDao(EnumDao.class)
                         .orElseThrow(() -> new OpenDcsDataRuntimeException("No Enum DAO available."));
    }
}
