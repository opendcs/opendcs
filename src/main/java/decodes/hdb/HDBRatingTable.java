/**
 * $Id$
 * 
 * Open Source Software
 * 
 * $Log$
 * Revision 1.3  2013/03/21 18:27:40  mmaloney
 * DbKey Implementation
 *
 */
package decodes.hdb;

import ilex.util.Logger;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.sql.Connection;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import decodes.sql.DbKey;
import decodes.tsdb.DbCompException;
import decodes.tsdb.RatingStatus;
import decodes.tsdb.TimeSeriesDb;
import decodes.util.DecodesSettings;
import decodes.db.Constants;

public class HDBRatingTable {
	private String ratingType = "Shift Adjusted Stage Flow"; //Default to expanded USGS type
	private DbKey ratingId=Constants.undefinedId;
	private DbKey sdi=Constants.undefinedId;
	private Date effStartDate;
	private Date effEndDate;
	private TimeSeriesDb tsdb;
    private Connection conn;
    private SimpleDateFormat rwdf;

	private boolean exceedLowerBound;
	private boolean exceedUpperBound;	
	
   public HDBRatingTable (TimeSeriesDb tsdb,String type, DbKey indep_sdi) 
   {
		ratingType=type;
		sdi=indep_sdi;
		exceedLowerBound = false;
		exceedUpperBound = false;
		this.tsdb=tsdb;
		conn=tsdb.getConnection();
		String tz = DecodesSettings.instance().sqlTimeZone;
		if (tz == null)
			tz = "MST"; // default to mountain time
		rwdf = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
        rwdf.setTimeZone(TimeZone.getTimeZone(tz));
	}

	public HDBRatingTable () 
	{
		String tz = DecodesSettings.instance().sqlTimeZone;
		if (tz == null)
			tz = "MST"; // default to mountain time
		rwdf = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
        rwdf.setTimeZone(TimeZone.getTimeZone(tz));
	}
	
	/** Given a valueDate, find the rating table 
	 * 
	 * @param valueDate
	 * @throws DbCompException
	 */
	public void findRating(Date valueDate)
	throws DbCompException
	{
		String q = "select ratings.find_site_rating('" + ratingType + 
		"'," + sdi + ", + to_date(?,'DD.MM.YYYY HH24:MI:SS')) from dual";
		CallableStatement cstmt =  null;

		try
		{
			cstmt = conn.prepareCall(q);
			String timestr = "";
			timestr = rwdf.format(valueDate.getTime());
			cstmt.setString(1, timestr);
			Boolean result = cstmt.execute();
			if (!result) {
				throw new DbCompException("query failed in findRating: " + q);
			}
			ResultSet rs = cstmt.getResultSet();
			if (rs.next())
			{
				ratingId = DbKey.createDbKey(rs, 1);
				if (ratingId.isNull()) 
					throw new SQLException("ratingId is null");
			}
		}
		catch(SQLException ex) 
		{
			String msg = 
				"Cannot find rating id for type " + ratingType + " sdi " +
				sdi +": "+ex;
			throw new DbCompException(msg);	
		}
		finally  //close callable statement always
		{
			try
			{
				if (cstmt != null) cstmt.close();
			}
			catch ( SQLException ex )
			{
				Logger.instance().warning(ex.toString());
			}
		}

		if (ratingId == Constants.undefinedId) {

			throw new DbCompException("Unable to find rating id for type: " +
					ratingType + " sdi: "+ sdi + " date: " + valueDate);					
		}
		else try 
		{
			q="select effective_start_date_time,effective_end_date_time from ref_site_rating " +
			"where rating_id = ?";
			cstmt = conn.prepareCall(q);
			cstmt.setInt(1, (int)ratingId.getValue());

			Boolean result = cstmt.execute();
			if (!result) {
				throw new DbCompException("query failed in findRating: " + q);
			}
			ResultSet rs = cstmt.getResultSet();
			if (rs.next())
			{
				effStartDate = rs.getDate(1); /* not an error to be null */
				effEndDate = rs.getDate(2); /* not an error to be null */
			}
		}
		catch(SQLException ex) 
		{
			String msg = 
				"Cannot find effective dates for rating id " + ratingId + ": " + ex;
			throw new DbCompException(msg);	
		}
		finally  //close callable statement always
		{
			try
			{
				if (cstmt != null) cstmt.close();
			}
			catch ( SQLException ex )
			{
				Logger.instance().warning(ex.toString());
			}
		}
	}

	/** Find if the valueDate is within the Effective Range of this ratingTable 
	 *  Intervals for dates here are as with the rest of the database, 
	 *  closed on the beginning, open on the end: [effStartDate, effEndDate)
	 * @param valueDate
	 * @return true if valueDate falls within our range
	 */
	private Boolean inEffRange(Date valueDate) {
		Logger.instance().debug3("Doing effRange: "+valueDate+ " effStart: " + effStartDate + " effEnd: " +effEndDate);
		
		if (valueDate !=null) {
			if (effStartDate !=null && valueDate.before(effStartDate)) {
				Logger.instance().debug3("Doing effRange start: "+valueDate.before(effStartDate));
				return false;
			}
			if (effEndDate !=null && 
				(valueDate.equals(effEndDate) || valueDate.after(effEndDate))) { 
				Logger.instance().debug3("Doing effRange end "+(valueDate.equals(effEndDate) || valueDate.after(effEndDate)));
				return false;
			}
		}
		return true;
	}

	
	/** Call the do_rating procedure in the database. The database knows which 
	 * algorithm to apply to arrive at a result, and returns the matching
	 * x and y values. The x value is really only needed for 
	 * the ACAPS algorithm, but may be useful for others.
	 */
	public RatingStatus doRating(double indepValue, Date valueDate) 
	throws DbCompException 
	{
		if (ratingId == Constants.undefinedId || !inEffRange(valueDate)) {
			findRating(valueDate);
		}

		RatingStatus rs = new RatingStatus(indepValue,0);

		String q = "{ call ratings.do_rating(?,?,to_date(?,'DD.MM.YYYY HH24:MI:SS'),?,?,?)}";
		CallableStatement cstmt =  null;

		String idString = " ratingId=" + ratingId
		+ ", indepValue=" + indepValue;
		Logger.instance().debug3("Performing rating: '" + idString + "'");

		try 
		{
			cstmt = conn.prepareCall(q);
		}
		catch(SQLException ex)
		{
			String msg = "Cannot prepare statement '" + q + "' " 
			+ " for " + idString + ": " + ex;
			Logger.instance().warning(msg);
			try { cstmt.close(); } catch(Exception ignore) {}
			throw new DbCompException(msg);
		}

		try
		{
			// Populate & execute the stored procedure call.
			cstmt.setLong(1, ratingId.getValue());
			cstmt.setDouble(2, indepValue);
			String timestr = rwdf.format(valueDate.getTime());
			cstmt.setString(3,timestr);
			cstmt.registerOutParameter(4, Types.NUMERIC); 
			cstmt.registerOutParameter(5, Types.NUMERIC);
			cstmt.registerOutParameter(6, Types.VARCHAR);
			cstmt.execute();
			rs.indep = cstmt.getDouble(4);
			rs.dep = cstmt.getDouble(5);
			rs.status = cstmt.getString(6);
		}
		catch(SQLException ex)
		{
			String msg = "SQL Error perfoming rating for rating " + 
			idString + " " + ex;
			Logger.instance().warning(msg);
			try { cstmt.close(); } catch(Exception ignore) {}
			throw new DbCompException(msg);
		}		
		finally  //close callable statement always
		{
			try
			{
				if (cstmt != null) cstmt.close();
			}
			catch ( SQLException ex )
			{
				Logger.instance().warning(ex.toString());
			}
		}

		if ("A".equals(rs.status) && !exceedUpperBound) {
			throw new DbCompException ("Upper Table Bound Exceeded for ratingId "+ ratingId +
					" indepValue "+indepValue);
		} 
		else if ("B".equals(rs.status) && !exceedLowerBound) {
			throw new DbCompException ("Lower Table Bound Exceeded for ratingId "+ ratingId +
					" indepValue "+indepValue);				
		}
		return rs;
	}

	/**
	 * @return the ratingType
	 */
	public String getRatingType() {
		return ratingType;
	}

	/**
	 * @param ratingType the ratingType to set
	 */
	public void setRatingType(String ratingType) {
		this.ratingType = ratingType;
	}

	/**
	 * @return the sdi
	 */
	public DbKey getSdi() {
		return sdi;
	}

	/**
	 * @param sdi the sdi to set
	 */
	public void setSdi(DbKey sdi) {
		this.sdi = sdi;
	}

	/**
	 * Resets the rating ID to undefined.
	 */
	public void resetRatingId() {
		ratingId = Constants.undefinedId;
	}

	/**
	 * @return the exceedLowerBound
	 */
	public boolean isExceedLowerBound() {
		return exceedLowerBound;
	}

	/**
	 * @param exceedLowerBound the exceedLowerBound to set
	 */
	public void setExceedLowerBound(boolean exceedLowerBound) {
		this.exceedLowerBound = exceedLowerBound;
	}

	/**
	 * @return the exceedUpperBound
	 */
	public boolean isExceedUpperBound() {
		return exceedUpperBound;
	}

	/**
	 * @param exceedUpperBound the exceedUpperBound to set
	 */
	public void setExceedUpperBound(boolean exceedUpperBound) {
		this.exceedUpperBound = exceedUpperBound;
	}

	/**
	 * @return the tsdb
	 */
	public TimeSeriesDb getTsdb() {
		return tsdb;
	}

	/**
	 * @param tsdb the tsdb to set
	 */
	public void setTsdb(TimeSeriesDb tsdb) {
		this.tsdb = tsdb;
	}

	/**
	 * @return the conn
	 */
	public Connection getConn() {
		return conn;
	}

	/**
	 * @param conn the conn to set
	 */
	public void setConn(Connection conn) {
		this.conn = conn;
	}
	
}
