package org.opendcs.operations;

import org.opendcs.database.api.OpenDcsDatabase;

/**
 * Marker interface for elements of OpenDCS operations
 * that may require implementation specific code.
 * Such as Transforming Time Series Identifiers for
 * group computations.
 *
 * Like a DAO Implementation methods <b>MAY</b> require a valid {@link org.opendcs.database.api.DataTransaction}
 * to perform the operations. However, it is expected that results say in
 * memory and that these will not <b>save</b> data to the database.
 *
 * Operations that are declared as "performs no database I/O" must <b>NOT</b>
 * provide a {@link org.opendcs.database.api.DataTransaction} or any other method to perform database I/O
 *
 * Operations that may perform database operations outside the the current transaction, for example looking up enums
 * or a cached instance of something, may store the instance of the {@link org.opendcs.database.api.OpenDcsDatabase}
 * provided by {@link #injectDatabase}. Suggestion on how to make that method unnecessary are welcome.
 * We may alter this to include a "supplier/create" interface in the future so that it can be provided in the
 * constructor.
 */
public interface OpenDcsOperations
{

    /**
     * {@see OpenDcsOperations} that require or expect database interaction should store
     * the provide OpenDcsDatabase instance for use.
     *
     * NOTE: while creating Operations that interact with the database consider the the performance implications as
     * well as whether or not it makes sense to accept a DataTransaction on which to perform said operation.
     * @param db
     */
    default void injectDatabase(OpenDcsDatabase db)
    {
        /* by default do nothing, *MOST* operations should not take a database */
    }
}
