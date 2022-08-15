package opendcs.opentsdb.hydrojson.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;

import decodes.sql.DbKey;
import decodes.tsdb.DbIoException;
import ilex.util.TextUtil;
import opendcs.dao.DaoBase;
import opendcs.dao.DatabaseConnectionOwner;
import opendcs.opentsdb.hydrojson.ErrorCodes;
import opendcs.opentsdb.hydrojson.beans.DecodesPresentationElement;
import opendcs.opentsdb.hydrojson.beans.DecodesPresentationGroup;
import opendcs.opentsdb.hydrojson.beans.PresentationRef;
import opendcs.opentsdb.hydrojson.errorhandling.WebAppException;

public class PresentationDAO
	extends DaoBase
{
	public static String module = "PresentationDAO";

	public PresentationDAO(DatabaseConnectionOwner tsdb)
	{
		super(tsdb, module);
	}

	public ArrayList<PresentationRef> getPresentationRefs()
		throws DbIoException
	{
		ArrayList<PresentationRef> ret = new ArrayList<PresentationRef>();
		
		String q = "select ID, NAME, INHERITSFROM, LASTMODIFYTIME, ISPRODUCTION"
				+ " from PRESENTATIONGROUP order by ID";
		ResultSet rs = doQuery(q);
		try
		{
			while (rs.next())
			{
				PresentationRef ref = new PresentationRef();
				ref.setGroupId(rs.getLong(1));
				ref.setName(rs.getString(2));
				long x = rs.getLong(3);
				if (!rs.wasNull())
					ref.setInheritsFromId(x);
				ref.setLastModified(db.getFullDate(rs, 4));
				ref.setProduction(TextUtil.str2boolean(rs.getString(5)));
				ret.add(ref);
			}
			
			for(PresentationRef ref : ret)
			{
System.out.println("check ref(" + ref.getGroupId() + ") '" + ref.getName() 
+ "' inheritsFrom=" + ref.getInheritsFromId());
				if (ref.getInheritsFromId() != null)
					for(PresentationRef r2 : ret)
					{
						if (ref.getInheritsFromId() == r2.getGroupId())
						{
							ref.setInheritsFrom(r2.getName());
							break;
						}
					}
			}
			return ret;
		}
		catch(SQLException ex)
		{
			throw new DbIoException(module + " error in query '" + q + "': " + ex);
		}
	}
	
	public DecodesPresentationGroup getPresentation(long id)
		throws DbIoException, WebAppException
	{
		String q = "select ID, NAME, INHERITSFROM, LASTMODIFYTIME, ISPRODUCTION"
				+ " from PRESENTATIONGROUP where ID = " + id;
		ResultSet rs = doQuery(q);
		try
		{
			if (!rs.next())
				throw new WebAppException(ErrorCodes.NO_SUCH_OBJECT, "No PresentationGroup with ID=" + id);
			
			DecodesPresentationGroup ret = new DecodesPresentationGroup();
			ret.setGroupId(rs.getLong(1));
			ret.setName(rs.getString(2));
			long x = rs.getLong(3);
			if (!rs.wasNull())
				ret.setInheritsFromId(x);
			ret.setLastModified(db.getFullDate(rs, 4));
			ret.setProduction(TextUtil.str2boolean(rs.getString(5)));

			if (ret.getInheritsFromId() != null)
			{
				q = "select NAME from PRESENTATIONGROUP where ID = " + ret.getInheritsFromId();
				rs = doQuery(q);
				if (rs.next())
					ret.setInheritsFrom(rs.getString(1));
			}
			
			q = "select dt.STANDARD, dt.CODE, dp.UNITABBR, dp.MAXDECIMALS, dp.MAX_VALUE, dp.MIN_VALUE"
				+ " from DATATYPE dt, DATAPRESENTATION dp"
				+ " where dp.GROUPID = " + ret.getGroupId()
				+ " and dp.DATATYPEID = dt.ID"
				+ " order by dt.STANDARD, dt.CODE";
			rs = doQuery(q);
			while(rs.next())
			{
				DecodesPresentationElement dpe = new DecodesPresentationElement();
				dpe.setDataTypeStd(rs.getString(1));
				dpe.setDataTypeCode(rs.getString(2));
				dpe.setUnits(rs.getString(3));
				dpe.setFractionalDigits(rs.getInt(4));
				Double d = rs.getDouble(5);
				if (!rs.wasNull())
					dpe.setMax(d);
				d = rs.getDouble(6);
				if (!rs.wasNull())
					dpe.setMin(d);
				ret.getElements().add(dpe);
			}
			return ret;

		}
		catch(SQLException ex)
		{
			throw new DbIoException(module + ".getPresentation(" + id 
				+ ") Exception in query '" + q + "': " + ex);
		}
	}

	public void writePresentation(DecodesPresentationGroup presGrp)
		throws DbIoException, WebAppException
	{
		// Check to make sure name is unique throw WebAppException if conflict
		// I.e. check for list with same name but different key.
		String q = "select ID from PRESENTATIONGROUP where lower(NAME) = " 
			+ sqlString(presGrp.getName().toLowerCase());
		if (presGrp.getGroupId() != null)
			q = q + " and ID != " + presGrp.getGroupId();
System.out.println("Check for dup name: " + q);
		ResultSet rs = doQuery(q);
		try
		{
			if (rs.next())
				throw new WebAppException(ErrorCodes.NOT_ALLOWED, 
					"Cannot write PresentationGroup with name '" + presGrp.getName() 
					+ "' because another group with id=" + rs.getLong(1) 
					+ " also has that name.");
			presGrp.setLastModified(new Date());
			if (presGrp.getGroupId() == null)
				insert(presGrp);
			else
				update(presGrp);
		}
		catch(SQLException ex)
		{
			throw new DbIoException(module + ".writePresentation error in query '" + q + "': " + ex);
		}
	}

	private void update(DecodesPresentationGroup presGrp)
		throws DbIoException
	{
		String q = "update PRESENTATIONGROUP set"
			+ " NAME = " + sqlString(presGrp.getName()) + ", "
			+ " INHERITSFROM = " + presGrp.getInheritsFromId() + ", "
			+ " LASTMODIFYTIME = " + db.sqlDate(presGrp.getLastModified()) + ", "
			+ " ISPRODUCTION = " + sqlBoolean(presGrp.isProduction())
			+ " where ID = " + presGrp.getGroupId();
		doModify(q);
		
		deleteSubordinates(presGrp.getGroupId());
		writeSubordinates(presGrp);
	}

	private void insert(DecodesPresentationGroup presGrp)
		throws DbIoException
	{
		presGrp.setGroupId(getKey("PRESENTATIONGROUP").getValue());

		String q = "insert into PRESENTATIONGROUP(ID, NAME, INHERITSFROM, LASTMODIFYTIME, ISPRODUCTION)"
			+ " values("
				+ presGrp.getGroupId() + ", "
				+ sqlString(presGrp.getName()) + ", "
				+ presGrp.getInheritsFromId() + ", "
				+ db.sqlDate(presGrp.getLastModified()) + ", "
				+ sqlBoolean(presGrp.isProduction())
			+ ")";
		doModify(q);
		
		deleteSubordinates(presGrp.getGroupId());
		writeSubordinates(presGrp);
	}

	private void writeSubordinates(DecodesPresentationGroup presGrp)
		throws DbIoException
	{
		try (DecodesDataTypeDAO dtDao = new DecodesDataTypeDAO(db))
		{
			dtDao.setManualConnection(getConnection());
			for(DecodesPresentationElement dpe : presGrp.getElements())
			{
				Long dtId = dtDao.lookup(dpe.getDataTypeStd(), dpe.getDataTypeCode());
				if (dtId == null)
					dtId = dtDao.create(dpe.getDataTypeStd(), dpe.getDataTypeCode());
				long id = this.getKey("DATAPRESENTATION").getValue();
				String q = "insert into DATAPRESENTATION(ID, GROUPID, DATATYPEID, UNITABBR, "
					+ "MAXDECIMALS, MAX_VALUE, MIN_VALUE) values ("
						+ id + ", "
						+ presGrp.getGroupId() + ", "
						+ dtId + ", "
						+ sqlString(dpe.getUnits()) + ", "
						+ dpe.getFractionalDigits() + ", "
						+ dpe.getMax() + ", "
						+ dpe.getMin()
					+ ")";
				doModify(q);
			}
		}
	}

	private void deleteSubordinates(long groupId)
		throws DbIoException
	{
		String q = "delete from DATAPRESENTATION where GROUPID = " + groupId;
		doModify(q);
	}
	
	public void deletePresentation(long groupId)
		throws DbIoException
	{
		deleteSubordinates(groupId);
		String q = "delete from PRESENTATIONGROUP where ID = " + groupId;
		doModify(q);
	}
	
	/**
	 * If the presentation group referened by groupId is used by one or more routing
	 * specs, return a list of routing spec IDs and names. If groupId is not used,
	 * return null.
	 * @param groupId
	 * @return
	 */
	public String routSpecsUsing(long groupId)
		throws DbIoException
	{
		String q = "select rs.ID, rs.NAME"
				+ " from ROUTINGSPEC rs, PRESENTATIONGROUP pg"
				+ " where lower(rs.PRESENTATIONGROUPNAME) = lower(pg.NAME)"
				+ " AND pg.ID = " + groupId;
		
		StringBuilder sb = new StringBuilder();
		ResultSet rs = doQuery(q);
		try
		{
			while(rs.next())
			{
				if (sb.length() > 0)
					sb.append(", ");
				sb.append(rs.getLong(1) + ":" + rs.getString(2));
			}
			return sb.length() == 0 ? null : sb.toString();
		}
		catch(SQLException ex)
		{
			throw new DbIoException(module + " SQL Error in query '" + q + "': " + ex);
		}
	}
}
