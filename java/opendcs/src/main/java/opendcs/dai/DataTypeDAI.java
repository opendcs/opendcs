/**
 * $Id$
 * 
 * $Log$
 */
package opendcs.dai;

import java.sql.SQLException;

import decodes.db.DataType;
import decodes.db.DataTypeSet;
import decodes.sql.DbKey;
import decodes.tsdb.DbIoException;
import decodes.tsdb.NoSuchObjectException;

/**
 * Data Access Interface for database-resident enumerations
 * @author mmaloney Mike Maloney
 */
public interface DataTypeDAI
	extends DaiBase
{
	/** Read a data type by the surrogate key */
	public DataType getDataType(DbKey id)
		throws DbIoException;

	/** Write a data type to the database */
	public void writeDataType(DataType dataType)
		throws DbIoException;
	
	/** Lookup best match for a data type code */
	public DataType lookupDataType(String dtcode)
		throws DbIoException, NoSuchObjectException;

	/** Fill the data type set with all known data types */
	public void readDataTypeSet(DataTypeSet dts)
		throws DbIoException;

	/** Fill the data type set with all known data types, filtering by data type standard */
	public void readDataTypeSet(DataTypeSet dts, String standard)
			throws DbIoException;
	
	/** Write the entire data type set including all equivalencies to database 
	 * @throws SQLException */
	public void writeDataTypeSet(DataTypeSet dts)
		throws DbIoException, SQLException;
	
	/** Silently close all resources */
	public void close();
}
