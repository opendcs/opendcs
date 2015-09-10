package decodes.cwms;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

import decodes.tsdb.DbIoException;

/**
 * Provides access to base parameters, parameters, and unit associations.
 * This class is created by and accessed from CwmsTimeSeriesDb
 */
public class BaseParam
{
	private HashMap<String, String> bparamUnitsMap = new HashMap<String, String>();
	
	public BaseParam()
	{
	}
	
	public void load(CwmsTimeSeriesDb db)
		throws DbIoException, SQLException
	{
		String q = "select distinct upper(a.base_parameter_id), b.unit_id "
			+ "from cwms_v_parameter a, cwms_v_storage_unit b "
			+ "where a.base_parameter_id = b.base_parameter_id "
			+ "order by upper(a.base_parameter_id)";
		ResultSet rs = db.doQuery(q);
		
		while(rs.next())
		{
			String bparam = rs.getString(1);
			String units = rs.getString(2);
			bparamUnitsMap.put(bparam, units);
		}
	}
	
	public String getUnitsAbbr4Param(String param)
	{
		int idx = param.indexOf('-');
		if (idx >= 0)
			param = param.substring(0, idx);
		return getUntsAbbr4BaseParam(param);
	}

	private String getUntsAbbr4BaseParam(String baseParam)
	{
		return bparamUnitsMap.get(baseParam.toUpperCase());
	}
}
