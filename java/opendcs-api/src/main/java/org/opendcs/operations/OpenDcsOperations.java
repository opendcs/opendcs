package org.opendcs.operations;

/**
 * Marker interface for elements of OpenDCS operations
 * that may require implementation specific code.
 * Such as Transforming Time Series Identifiers for 
 * group computations.
 * 
 * Like a DAO Implementation methods <b>MAY</b> require a valid {@link DataTransaction}
 * to perform the operations. However, it is expected that results say in
 * memory and that these will not <b>save</b> data to the database.
 * 
 * Operations that are declare as "performs no database I/O" must <b>NOT</b>
 * provide a {@link DataTransaction} or any other method to perform database I/O
 */
public interface OpenDcsOperations
{
    /* Marker interface */
}
