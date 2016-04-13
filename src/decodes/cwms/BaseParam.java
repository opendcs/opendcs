package decodes.cwms;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import decodes.tsdb.DbIoException;

/**
 * Provides access to base parameters, parameters, and unit associations.
 * This class is created by and accessed from CwmsTimeSeriesDb
 */
public class BaseParam
{
	private HashMap<String, String> bparamUnitsMap = new HashMap<String, String>();
	private HashMap<String, String> bparamEnglishUnitsMap = new HashMap<String, String>();
	
	public BaseParam()
	{
	}
	
	public void print()
	{
		System.out.println("BaseParam - Storage Units - English Units");
		ArrayList<String> keyset = new ArrayList<String>();
		keyset.addAll(bparamUnitsMap.keySet());
		Collections.sort(keyset);
		for(Object key : keyset)
			System.out.println("\t" + key + " - " + bparamUnitsMap.get(key) + " - " + 
				bparamEnglishUnitsMap.get(key));
	}
	
	public void load(CwmsTimeSeriesDb db)
		throws DbIoException, SQLException
	{
		String q = "select distinct upper(a.base_parameter_id), b.unit_id "
			+ "from cwms_v_parameter a, cwms_v_storage_unit b "
			+ "where a.base_parameter_id = b.base_parameter_id "
			+ " and (db_office_code = " + db.getDbOfficeCode() + " or db_office_code=53)"
			+ " order by upper(a.base_parameter_id)";
		ResultSet rs = db.doQuery(q);
		
		while(rs.next())
		{
			String bparam = rs.getString(1);
			String units = rs.getString(2);
//Logger.instance().debug3("baseParam '" + bparam + "' units '" + units + "'");
			bparamUnitsMap.put(bparam, units);
		}
		
		q = "select parameter_id, unit_id from cwms_v_display_units "
			+ " where office_id = '" + db.getDbOfficeId() + "'"
			+ " and unit_system = 'EN'";
		rs = db.doQuery(q);
		
		while(rs.next())
		{
			String p = rs.getString(1);
			if (p.contains("-"))
				continue; // we only want base params
			String units = rs.getString(2);
//Logger.instance().debug3("baseParam '" + p + "' engUnits '" + units + "'");
			bparamEnglishUnitsMap.put(p.toUpperCase(), units);
		}
	}
	
	public String getStoreUnits4Param(String param)
	{
		int idx = param.indexOf('-');
		if (idx >= 0)
			param = param.substring(0, idx);
		return getStorUnits4BaseParam(param);
	}

	private String getStorUnits4BaseParam(String baseParam)
	{
		return bparamUnitsMap.get(baseParam.toUpperCase());
	}
	
	public String getEnglishUnits4Param(String param)
	{
		int idx = param.indexOf('-');
		if (idx >= 0)
			param = param.substring(0, idx);
		String ret = bparamEnglishUnitsMap.get(param.toUpperCase());
		if (ret != null)
			return ret;
		return getStorUnits4BaseParam(param);
	}
}
