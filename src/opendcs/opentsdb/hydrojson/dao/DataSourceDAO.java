package opendcs.opentsdb.hydrojson.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import decodes.sql.DbKey;
import decodes.tsdb.DbIoException;
import ilex.util.PropertiesUtil;
import opendcs.dao.DaoBase;
import opendcs.dao.DatabaseConnectionOwner;
import opendcs.opentsdb.hydrojson.ErrorCodes;
import opendcs.opentsdb.hydrojson.beans.DataSourceGroupMember;
import opendcs.opentsdb.hydrojson.beans.DataSourceRef;
import opendcs.opentsdb.hydrojson.beans.DecodesDataSource;
import opendcs.opentsdb.hydrojson.errorhandling.WebAppException;

public class DataSourceDAO extends DaoBase
{
	public static String module = "DataSourceDAO";

	public DataSourceDAO(DatabaseConnectionOwner tsdb)
	{
		super(tsdb, module);
	}

	public ArrayList<DataSourceRef> readDataSourceRefs()
		throws DbIoException
	{
		ArrayList<DataSourceRef> ret = new ArrayList<DataSourceRef>();
		
		String q = "select ds.ID, ds.NAME, ds.DATASOURCETYPE, ds.DATASOURCEARG"
			+ " from DATASOURCE ds";

		ResultSet rs = doQuery(q);
		try
		{
			while(rs.next())
			{
				DataSourceRef dsr = new DataSourceRef();
				dsr.setDataSourceId(rs.getLong(1));
				dsr.setName(rs.getString(2));
				dsr.setType(rs.getString(3));
				dsr.setArguments(rs.getString(4));
				ret.add(dsr);
			}
			
			q = " select RS.DATASOURCEID, count(RS.DATASOURCEID)"
				+ " from ROUTINGSPEC RS GROUP BY RS.DATASOURCEID";
			rs = doQuery(q);
			while(rs.next())
			{
				long id = rs.getLong(1);
				int count = rs.getInt(2);
				for(DataSourceRef dsr : ret)
					if (dsr.getDataSourceId() == id)
					{
						dsr.setUsedBy(count);
						break;
					}
			}
			
		}
		catch (SQLException ex)
		{
			throw new DbIoException(module + " error while reading DATASOURCE: " + ex);
		}
		
		return ret;
	}
	
	public DecodesDataSource readDataSource(long dataSourceId)
		throws DbIoException
	{
		String q = "select ds.ID, ds.NAME, ds.DATASOURCETYPE, ds.DATASOURCEARG"
				+ " from DATASOURCE ds"
				+ " where ds.ID = " + dataSourceId;

		ResultSet rs = doQuery(q);
		try
		{
			DecodesDataSource dds = new DecodesDataSource();
			if(rs.next())
			{
				dds.setDataSourceId(rs.getLong(1));
				dds.setName(rs.getString(2));
				dds.setType(rs.getString(3));
				String args = rs.getString(4);
				if (args != null)
					dds.setProps(PropertiesUtil.string2props(args));
			}
			else
				return null;
			
			q = "select count(*)"
				+ " from ROUTINGSPEC"
				+ " where DATASOURCEID = " + dataSourceId;
			rs = doQuery(q);
			if (rs.next())
				dds.setUsedBy(rs.getInt(1));
			
			q = " select gm.MEMBERID, ds.NAME"
					+ " from DATASOURCEGROUPMEMBER gm, DATASOURCE ds"
					+ " where gm.MEMBERID = ds.ID and gm.GROUPID = " + dataSourceId
					+ " order by gm.SEQUENCENUM";
			rs = doQuery(q);
			while(rs.next())
			{
				DataSourceGroupMember dsgm = new DataSourceGroupMember();
				dsgm.setDataSourceId(rs.getLong(1));
				dsgm.setDataSourceName(rs.getString(2));
				dds.getGroupMembers().add(dsgm);
			}
			return dds;
		}
		catch (SQLException ex)
		{
			throw new DbIoException(module + " error while reading DATASOURCE in query '"
				+ q + ": " + ex);
		}
	}

	public DecodesDataSource writedDataSource(DecodesDataSource ds)
		throws DbIoException, WebAppException
	{
		// Check to make sure name is unique throw WebAppException if conflict
		// I.e. check for list with same name but different key.
		String q = "select ID from DATASOURCE where lower(NAME) = " 
			+ sqlString(ds.getName().toLowerCase());
		if (ds.getDataSourceId() != DbKey.NullKey.getValue())
			q = q + " and ID != " + ds.getDataSourceId();
System.out.println(module + " Check for dup name: " + q);
		ResultSet rs = doQuery(q);
		try
		{
			if (rs.next())
				throw new WebAppException(ErrorCodes.NOT_ALLOWED, 
					"Cannot write DataSource with name '" + ds.getName() 
					+ "' because another DataSource with id=" + rs.getLong(1) 
					+ " also has that name.");

			if (ds.getDataSourceId() == DbKey.NullKey.getValue())
			{
				ds.setDataSourceId(getKey("DATASOURCE").getValue());
				insert(ds);
			}
			else
				update(ds);
			return ds;
		}
		catch (SQLException e)
		{
			throw new DbIoException("writeDataSource Error in query '" + q + "'");
		}
	}

	/**
	* Update a pre-existing DataSource into the database.
	* @param ds the DataSource
	*/
	private void update(DecodesDataSource ds)
		throws DbIoException
	{
		String q = "update DATASOURCE set " +
			"NAME = " + sqlString(ds.getName()) + ", " +
			"DATASOURCETYPE = " + sqlString(ds.getType()) + ", " +
			"DATASOURCEARG = " + sqlString(PropertiesUtil.props2string(ds.getProps())) + " " +
			"where ID = " + ds.getDataSourceId();
		doModify(q);

		writeGroupMembers(ds);
	}

	/**
	* Insert a new DataSource into the database.
	* @param ds the DataSource
	*/
	private void insert(DecodesDataSource ds)
		throws DbIoException
	{
		DbKey id = getKey("DataSource");
		ds.setDataSourceId(id.getValue());
		String args = PropertiesUtil.props2string(ds.getProps());
		String q = "INSERT INTO DataSource(id, name, datasourcetype, datasourcearg) "
			+ "VALUES (" +
			id + ", " +
			sqlString(ds.getName()) + ", " +
			sqlString(ds.getType()) + ", " +
			sqlString(args) +
			 ")";
		doModify(q);

		writeGroupMembers(ds);
	}

	/**
	* This inserts records into the DataSourceGroupMember table, one for
	* each member of a group-type DataSource.
	* @param ds the DataSource
	*/
	private void writeGroupMembers(DecodesDataSource ds)
		throws DbIoException
	{
		String q = "delete from DATASOURCEGROUPMEMBER where GROUPID = " + ds.getDataSourceId();
		doModify(q);
		
		for(int seq=0; seq < ds.getGroupMembers().size(); seq++)
		{
			q = "INSERT INTO DATASOURCEGROUPMEMBER(GROUPID, SEQUENCENUM, MEMBERID) VALUES (" +
					ds.getDataSourceId() + ", " +
					seq + ", " + 
					ds.getGroupMembers().get(seq).getDataSourceId() + 
					")";
			doModify(q);
		}
	}
	
	public String datasourceUsedByRs(long datasourceId)
			throws DbIoException
	{
		String q = "select rs.ID, rs.NAME"
			+ " from ROUTINGSPEC rs"
			+ " where rs.DATASOURCEID = " + datasourceId;
		
		try
		{
			StringBuilder sb = new StringBuilder();
			int n = 0;
			for(ResultSet rs = doQuery(q); rs.next(); n++)
			{
				if (n > 0)
					sb.append(", ");
				sb.append("" + rs.getLong(1) + ":" + rs.getString(2));
			}
			if (n > 0)
				return sb.toString();
			else
				return null;
				
		}
		catch (SQLException ex)
		{
			ex.printStackTrace();
			throw new DbIoException(module + " Error in query '" + q + "': " + ex);
		}
	}

	public void deleteDatasource(long id)
		throws DbIoException
	{
		// If this DS is a member of a group, or if it IS a group, delete the associations:
		String q = "delete from DATASOURCEGROUPMEMBER " +
				   "where GROUPID = " + id + " or MEMBERID = " + id;
		doModify(q);
		
		q = "DELETE FROM DataSource WHERE ID = " + id;
		doModify(q);
	}
}
