package decodes.hdb;

import decodes.sql.DbKey;

/**
 * Bean class for storing info about an HDB Data type
 * @author mmaloney
 *
 */
public class HdbDataType
{
	/** Key into HDB_DATATYPE table */
	private DbKey dataTypeId = DbKey.NullKey;
	
	private String name = null;
	
	private String unitsAbbr = null;

	public HdbDataType(DbKey dataTypeId, String name, String unitsAbbr)
	{
		super();
		this.dataTypeId = dataTypeId;
		this.name = name;
		this.unitsAbbr = unitsAbbr;
	}

	public DbKey getDataTypeId()
	{
		return dataTypeId;
	}

	public void setDataTypeId(DbKey dataTypeId)
	{
		this.dataTypeId = dataTypeId;
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public String getUnitsAbbr()
	{
		return unitsAbbr;
	}

	public void setUnitsAbbr(String unitsAbbr)
	{
		this.unitsAbbr = unitsAbbr;
	}

}
