package opendcs.opentsdb.hydrojson.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import decodes.tsdb.DbIoException;
import opendcs.dao.DaoBase;
import opendcs.dao.DatabaseConnectionOwner;
import opendcs.opentsdb.hydrojson.beans.DecodesDataType;
import opendcs.opentsdb.hydrojson.beans.DecodesUnit;

public class UnitDAO
	extends DaoBase
{
	public static String module = "UnitDAO";

	public UnitDAO(DatabaseConnectionOwner tsdb)
	{
		super(tsdb, module);
	}


	public ArrayList<DecodesUnit> getUnitList() 
		throws DbIoException
	{
		ArrayList<DecodesUnit> ret = new ArrayList<DecodesUnit>();
		
		String q = "select UNITABBR, NAME, FAMILY, MEASURES"
			+ " from ENGINEERINGUNIT order by unitabbr";
		
		ResultSet rs = doQuery(q);
		try
		{
			while (rs.next())
			{
				DecodesUnit du = new DecodesUnit();
				du.setAbbr(rs.getString(1));
				du.setName(rs.getString(2));
				du.setFamily(rs.getString(3));
				du.setMeasures(rs.getString(4));
				ret.add(du);
			}
			return ret;
		}
		catch(SQLException ex)
		{
			throw new DbIoException(module + ".getUnitList() error in query '" + q + "': " + ex);
		}
	}


	public ArrayList<DecodesDataType> getDataTypeList(String std)
		throws DbIoException
	{
		ArrayList<DecodesDataType> ret = new ArrayList<DecodesDataType>();
		
		String q = "select ID, STANDARD, CODE, DISPLAY_NAME from DATATYPE";
		if (std != null)
			q = q + " where lower(STANDARD) = " + sqlString(std.toLowerCase());
		q = q + " order by STANDARD, CODE";
		ResultSet rs = doQuery(q);
		try
		{
			while (rs.next())
			{
				DecodesDataType dt = new DecodesDataType();
				dt.setId(rs.getLong(1));
				dt.setStandard(rs.getString(2));
				dt.setCode(rs.getString(3));
				dt.setDisplayName(rs.getString(4));
				
				ret.add(dt);
			}
			return ret;
		}
		catch(SQLException ex)
		{
			throw new DbIoException(module + ".getDataTypeList() error in query '" + q + "': " + ex);
		}
	}

}
