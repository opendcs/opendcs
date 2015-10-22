package decodes.cwms.rating;

import ilex.util.Logger;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;

import decodes.cwms.BadRatingException;
import decodes.cwms.CwmsTimeSeriesDb;
import decodes.sql.DbKey;
import decodes.tsdb.DbIoException;
import decodes.tsdb.TsdbDao;

import hec.data.RatingException;
import hec.data.cwmsRating.AbstractRating;
import hec.data.cwmsRating.RatingSet;


public class CwmsRatingDao extends TsdbDao
{
	public static final String module = "CwmsRatingDao";
	public static final String cwms_v_rating_columns =
		"RATING_CODE, RATING_ID, EFFECTIVE_DATE, CREATE_DATE, ACTIVE_FLAG";
	
	private String officeId = null;
	
	class RatingWrapper
	{
		Date timeLoaded = null;
		Date lastTimeUsed = null;
		RatingSet ratingSet = null;
		RatingWrapper(Date timeLoaded, RatingSet ratingSet, Date lastTimeUsed)
		{
			this.timeLoaded = timeLoaded;
			this.ratingSet = ratingSet;
			this.lastTimeUsed = lastTimeUsed;
		}
	}
	static HashMap<String, RatingWrapper> ratingCache = new HashMap<String, RatingWrapper>();
	public static final int MAX_CACHED = 400;
	
	public CwmsRatingDao(CwmsTimeSeriesDb tsdb)
	{
		super(tsdb);
		officeId = tsdb.getDbOfficeId();
	}
	
	public void setOfficeId(String oid)
	{
		officeId = oid;
	}
	
	private CwmsRatingRef rs2rr(ResultSet rs)
		throws SQLException, BadRatingException
	{
		return new CwmsRatingRef(DbKey.createDbKey(rs, 1), officeId,
			rs.getString(2), tsdb.getFullDate(rs, 3),
			tsdb.getFullDate(rs, 4), tsdb.getBoolean(rs, 5));
	}
	
	/**
	 * List all the rating objects for this office ID. 
	 * Results are not sorted in any particular way.
	 * @param locationId location ID or null for no filter
	 * @return list of CwmsRatingRef objects
	 * @throws DbIoException if any error occurs in the query.
	 */
	List<CwmsRatingRef> listRatings(String locationId)
		throws DbIoException
	{
		String officeId = ((CwmsTimeSeriesDb)tsdb).getDbOfficeId();
		String q = "select distinct " + cwms_v_rating_columns
			+ " from CWMS_V_RATING"
			+ " where upper(OFFICE_ID) = " + tsdb.sqlString(officeId.toUpperCase());
		if (locationId != null)
			q = q + " and upper(LOCATION_ID) = " + tsdb.sqlString(locationId.toUpperCase());
		
		ArrayList<CwmsRatingRef> ret = new ArrayList<CwmsRatingRef>();

		ResultSet rs;
		rs = tsdb.doQuery(q);
		try
		{
			while(rs != null && rs.next())
			{
				try
				{
					ret.add(rs2rr(rs));
				}
				catch(BadRatingException ex)
				{
					tsdb.warning("Bad Rating: " + ex + " -- skipped.");
				}
			}
		}
		catch (SQLException ex)
		{
			String msg = "Error reading ratings: " + ex;
			System.err.println(msg);
			ex.printStackTrace(System.err);
			throw new DbIoException(msg);
		}
		
		return ret;
	}
	
	public void deleteRating(CwmsRatingRef crr)
		throws RatingException
	{
		RatingSet ratingSet = RatingSet.fromDatabase(tsdb.getConnection(), 
			((CwmsTimeSeriesDb)tsdb).getDbOfficeId(),
			crr.getRatingSpecId());
		if(ratingSet.getRatingCount() <= 1)
		{
			deleteRatingSpec(crr);
			return;
		}
		
		SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

		String q =
			"{ call cwms_rating.delete_ratings(?," +
			"to_date(?,'DD.MM.YYYY HH24:MI:SS')," +
			"to_date(?,'DD.MM.YYYY HH24:MI:SS'),null, ?) }";
		CallableStatement cstmt =  null;
		String doing = "prepare call";
		try
		{
			cstmt = tsdb.getConnection().prepareCall(q);
			doing = "set params for";
			cstmt.setString(1, crr.getRatingSpecId());
			cstmt.setString(2, sdf.format(crr.getEffectiveDate()));
			cstmt.setString(3, sdf.format(crr.getEffectiveDate()));
			cstmt.setString(4, crr.getOfficeId());
			doing = "execute";
			cstmt.execute();
		}
		catch (SQLException ex)
		{
			String msg = "Cannot " + doing + " '" + q + "' " 
				+ " for specId '" + crr.getRatingSpecId()
				+ "' and office '" + crr.getOfficeId() + "': " + ex;
				
			Logger.instance().warning(msg);
			try { cstmt.close(); } catch(Exception ignore) {}
		}
	}
	
	/**
	 * Deletes the RatingSpec associated with crr, including all ratings.
	 * @param crr The reference to the rating
	 */
	public void deleteRatingSpec(CwmsRatingRef crr)
	{
		String q =
			"{ call cwms_rating.delete_specs(?,'DELETE ALL', ?) }";
		CallableStatement cstmt =  null;
		String doing = "prepare call";
		try
		{
			cstmt = tsdb.getConnection().prepareCall(q);
			doing = "set params for";
			cstmt.setString(1, crr.getRatingSpecId()); 
			cstmt.setString(2, crr.getOfficeId());
			doing = "execute";
			cstmt.execute();
		}
		catch (SQLException ex)
		{
			String msg = "Cannot " + doing + " '" + q + "' " 
				+ " for specId '" + crr.getRatingSpecId()
				+ "' and office '" + crr.getOfficeId() + "': " + ex;
				
			Logger.instance().warning(msg);
			try { cstmt.close(); } catch(Exception ignore) {}
		}
	}
	
	/**
	 * Generate an XML string for the referenced rating
	 * @param crr the referenced rating
	 * @param allInSpec true for XML to include all ratings with the
	 *        matching spec. False for just the specified rating
	 * @return XML String
	 */
	public String toXmlString(CwmsRatingRef crr, boolean allInSpec)
		throws RatingException
	{
		String officeId = ((CwmsTimeSeriesDb)tsdb).getDbOfficeId();
		String specId = crr.getRatingSpecId();
		RatingSet ratingSet = getRatingSet(specId);
		if (!allInSpec)
		{
			Date selectedDate = crr.getEffectiveDate();
			long selectedTime = selectedDate == null ? 0L : selectedDate.getTime();
			AbstractRating[] ratings = ratingSet.getRatings();
			for(AbstractRating rating : ratings)
			{
				if (rating.getEffectiveDate() != selectedTime)
					ratingSet.removeRating(rating.getEffectiveDate());
			}
		}
		return ratingSet.toXmlString("");
	}

	/**
	 * Import the passed XML into CWMS, merging with any existing ratings.
	 * @param xml
	 * @throws RatingException
	 */
	public void importXmlToDatabase(String xml)
		throws RatingException
	{
		Logger.instance().debug3("importXmlToDatabase: " + xml);
		RatingSet newSet = RatingSet.fromXml(xml);
		String specId = newSet.getRatingSpec().getRatingSpecId();
		Logger.instance().debug3("importXmlToDatabase fromXml success, specId='" + specId + "'");
		try
		{
			Logger.instance().debug3("importXmlToDatabase trying to read existing specId='" + specId + "'");
			RatingSet dbSet = getRatingSet(specId);
			if (dbSet != null)
			{
				Logger.instance().debug3("importXmlToDatabase spec exists");
				for(AbstractRating newAr : newSet.getRatings())
				{
					boolean replaced = false;
					for(AbstractRating oldAr : dbSet.getRatings())
					{
						if (newAr.getEffectiveDate() == oldAr.getEffectiveDate())
						{
							Logger.instance().debug3("importXmlToDatabase replacing rating with same "
								+ "spec and effectiveDate=" + new Date(newAr.getEffectiveDate()));
							dbSet.replaceRating(newAr);
							replaced = true;
							break;
						}
					}
					if (!replaced)
					{
						Logger.instance().debug3("importXmlToDatabase adding rating with "
							+ "effectiveDate=" + new Date(newAr.getEffectiveDate()));
						dbSet.addRating(newAr);
					}
				}
				newSet = dbSet;
			}
			else
				Logger.instance().debug3("importXmlToDatabase spec does not exist.");
		}
		catch(RatingException ex)
		{
			Logger.instance().warning(module + " Cannot read rating for spec ID '"
				+ specId + "': " + ex);
			Logger.instance().info(module + " New rating " + specId);
		}

		Logger.instance().debug3(module + " calling storeToDatabase");
		newSet.storeToDatabase(tsdb.getConnection(), true);
	}
	
	public RatingSet getRatingSet(String specId)
		throws RatingException
	{
		// Internally use specId all in upper case.
		String ucSpecId = specId.toUpperCase();
		String officeId = ((CwmsTimeSeriesDb)tsdb).getDbOfficeId();
		
		RatingWrapper rw = ratingCache.get(ucSpecId);
		if (rw != null)
		{
			// If it's in cache and hasn't been modified in DB, just return it.
			Date lastUpdate = getRatingLastUpdateTime(specId);
			if (lastUpdate != null && rw.timeLoaded.after(lastUpdate))
			{
				rw.lastTimeUsed = new Date();
				Logger.instance().debug3(module + " retrieving rating spec from cache with officeId="
					+ officeId + " and spec '" + specId + "' -- was loaded into cache at "
					+ rw.timeLoaded);
				return rw.ratingSet;
			}
		}
		
		// If cache is full, have to delete one.
		if (ratingCache.size() >= MAX_CACHED)
		{
			Date oldestUsed = new Date();
			String oldestSpec = null;
			for(String tspecId : ratingCache.keySet())
			{
				RatingWrapper trw = ratingCache.get(tspecId);
				if (trw.lastTimeUsed.before(oldestUsed))
				{
					oldestSpec = tspecId;
					oldestUsed = trw.lastTimeUsed;
				}
			}
			if (oldestSpec != null)
			{
				Logger.instance().debug3(module + " Removing '" + oldestSpec
					+ "' from cache because cache is full.");
				ratingCache.remove(oldestSpec);
			}
		}
		
		Logger.instance().debug3(module + " calling RatingSet.fromDatabase with officeId=" 
			+ officeId + " and spec '" + specId + "'");
		Date timeLoaded = new Date();
//RatingSet.setAlwaysAllowUnsafe(false);
		RatingSet ratingSet = RatingSet.fromDatabase(tsdb.getConnection(),
			officeId, specId);
		ratingCache.put(ucSpecId, new RatingWrapper(timeLoaded, ratingSet, timeLoaded));
		Logger.instance().debug3(module + " reading rating from database took "
			+ (System.currentTimeMillis()/1000L - timeLoaded.getTime()/1000L) + " seconds.");
		
		return ratingSet;
	}
	
	/**
	 * Query the database to determine the last time a rating was created
	 * for this spec.
	 * @param specId the spec ID
	 * @return the date/time of the last rating object for this spec.
	 */
	public Date getRatingLastUpdateTime(String specId)
	{
		String q = "select max(create_date) from cwms_v_rating where rating_id = "
			+ tsdb.sqlString(specId);
		
		try
		{
			ResultSet rs = tsdb.doQuery(q);
			if (rs != null && rs.next())
				return tsdb.getFullDate(rs, 1);
			Logger.instance().warning(module + " No results in getRatingLastUpdateTime for '"
				+ specId + "'");
		}
		catch (Exception ex)
		{
			String msg = module + " Error in getRatingLastUpdateTime for '"
				+ specId + "': " + ex;
			Logger.instance().warning(msg);
			System.err.println(msg);
			ex.printStackTrace(System.err);
		}
		return null;
	}
}
