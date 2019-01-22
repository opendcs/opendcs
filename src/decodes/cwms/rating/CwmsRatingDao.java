/*
 * $Id$
 * 
 * $Log$
 * Revision 1.10  2018/09/11 21:33:45  mmaloney
 * CWMS-13483 Catch RatingException in GUI when ID doesn't exist.
 *
 * Revision 1.9  2017/11/02 20:44:24  mmaloney
 * Cwms Rating perform multiple points at the same time.
 *
 * Revision 1.8  2017/03/09 21:38:23  mmaloney
 * Don't attempt to detect change in a rating set in the database.
 * Set the databaseLookupMode to reference.
 *
 * Revision 1.7  2017/02/15 15:09:48  mmaloney
 * Fixed rating check as per Mike Perryman's email 2/14/17
 *
 * Revision 1.6  2016/09/29 18:54:37  mmaloney
 * CWMS-8979 Allow Database Process Record to override decodes.properties and
 * user.properties setting. Command line arg -Dsettings=appName, where appName is the
 * name of a process record. Properties assigned to the app will override the file(s).
 *
 * 
 * This software was written by Cove Software, LLC ("COVE") under contract 
 * to the United States Government. 
 * 
 * No warranty is provided or implied other than specific contractual terms
 * between COVE and the U.S. Government
 * 
 * Copyright 2016 U.S. Army Corps of Engineers, Hydrologic Engineering Center.
 * All rights reserved.
 */
package decodes.cwms.rating;

import ilex.util.Logger;
import ilex.util.TextUtil;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;

import opendcs.dao.DaoBase;
import decodes.cwms.BadRatingException;
import decodes.cwms.CwmsTimeSeriesDb;
import decodes.sql.DbKey;
import decodes.tsdb.DbIoException;
import hec.data.RatingException;
import hec.data.cwmsRating.AbstractRating;
import hec.data.cwmsRating.RatingSet;


public class CwmsRatingDao extends DaoBase
{
	public static final String module = "CwmsRatingDao";
	public static final String cwms_v_rating_columns =
		"RATING_CODE, RATING_ID, EFFECTIVE_DATE, CREATE_DATE, ACTIVE_FLAG";
	
//	// Suggested by Mike Perryman as a quick way to detect change:
//	public static final String ratCheckQ = 
//		"select greatest(max(effective_date), max(create_date)) "
//		+ "from cwms_v_rating where upper(rating_id) = ";
	
	private String officeId = null;
	
	class RatingWrapper
	{
		Date timeLoaded = null;
//		String check = null;
		Date lastTimeUsed = null;
		RatingSet ratingSet = null;
		RatingWrapper(Date timeLoaded, RatingSet ratingSet, Date lastTimeUsed)
//			String check)
		{
			this.timeLoaded = timeLoaded;
			this.ratingSet = ratingSet;
			this.lastTimeUsed = lastTimeUsed;
//			this.check = check;
		}
	}
	static HashMap<String, RatingWrapper> ratingCache = new HashMap<String, RatingWrapper>();
	public static final int MAX_CACHED = 400;
	// Ratings older than this in the cache are discarded.
	private long MAX_AGE_MSEC = 9 * 3600000L;
	
	
//	static
//	{
//		// Mike Perryman's email on March 1, 2017 - Rating API now has a reference mode
//		// whereby the rating calculations are done on the database side and no (huge)
//		// rating table have to be actually loaded. Thus use the reference mode and also
//		// remove the 'check for update' operation.
//		System.setProperty("hec.data.cwmsRating.RatingSet.databaseLoadMethod", "reference");
//	}
//	
	public CwmsRatingDao(CwmsTimeSeriesDb tsdb)
	{
		super(tsdb, "CwmsRatingDao");
		officeId = tsdb.getDbOfficeId();
		setUseReference(true);
	}

	public void setUseReference(boolean useReference)
	{
		System.setProperty("hec.data.cwmsRating.RatingSet.databaseLoadMethod", 
			useReference ? "reference" : "eager");
	}
	
	public void setOfficeId(String oid)
	{
		officeId = oid;
	}
	
	private CwmsRatingRef rs2rr(ResultSet rs)
		throws SQLException, BadRatingException
	{
		return new CwmsRatingRef(DbKey.createDbKey(rs, 1), officeId,
			rs.getString(2), db.getFullDate(rs, 3),
			db.getFullDate(rs, 4), TextUtil.str2boolean(rs.getString(5)));
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
		String officeId = ((CwmsTimeSeriesDb)db).getDbOfficeId();
		String q = "select distinct " + cwms_v_rating_columns
			+ " from CWMS_V_RATING"
			+ " where upper(OFFICE_ID) = " + sqlString(officeId.toUpperCase());
		if (locationId != null)
			q = q + " and upper(LOCATION_ID) = " + sqlString(locationId.toUpperCase());
		
		ArrayList<CwmsRatingRef> ret = new ArrayList<CwmsRatingRef>();

		ResultSet rs;
		rs = doQuery(q);
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
					warning("Bad Rating: " + ex + " -- skipped.");
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
//		RatingSet ratingSet = RatingSet.fromDatabase(db.getConnection(), 
//			((CwmsTimeSeriesDb)db).getDbOfficeId(),
//			crr.getRatingSpecId());

		RatingSet ratingSet = new RatingSet(db.getConnection(), 
			((CwmsTimeSeriesDb)db).getDbOfficeId(),
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
			cstmt = db.getConnection().prepareCall(q);
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
			cstmt = db.getConnection().prepareCall(q);
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
//		String officeId = ((CwmsTimeSeriesDb)db).getDbOfficeId();
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
			throw ex;
		}

		Logger.instance().debug3(module + " calling storeToDatabase");
		newSet.storeToDatabase(db.getConnection(), true);
	}
	
	public RatingSet getRatingSet(String specId)
		throws RatingException
	{
		// Internally use specId all in upper case.
		String ucSpecId = specId.toUpperCase();
		String officeId = ((CwmsTimeSeriesDb)db).getDbOfficeId();
		
		RatingWrapper rw = ratingCache.get(ucSpecId);
		if (rw != null)
		{
			// If # points has not changed and not too old in the cache, use it.
//			String rcheck = getRatingCheck(ucSpecId);
//			if (TextUtil.strEqual(rcheck, rw.check)

			if (System.currentTimeMillis() - rw.timeLoaded.getTime() < MAX_AGE_MSEC)
			{
				rw.lastTimeUsed = new Date();
				Logger.instance().debug3(module 
					+ " retrieving rating spec from cache with officeId="
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

//		String rcheck = getRatingCheck(ucSpecId);

		Logger.instance().debug3(module + " calling RatingSet.fromDatabase with officeId=" 
			+ officeId + " and spec '" + specId + "'");
		Date timeLoaded = new Date();
//RatingSet.setAlwaysAllowUnsafe(false);
//		RatingSet ratingSet = RatingSet.fromDatabase(db.getConnection(), officeId, specId);
		RatingSet ratingSet = new RatingSet(db.getConnection(), officeId, specId);

		ratingCache.put(ucSpecId, new RatingWrapper(timeLoaded, ratingSet, timeLoaded));
		//, rcheck));
		
		Logger.instance().debug3(module + " reading rating from database took "
			+ (System.currentTimeMillis()/1000L - timeLoaded.getTime()/1000L) + " seconds.");
		
		return ratingSet;
	}
	
	/**
	 * Used as a check to detect rating changes.
	 * @param ratingId
	 * @return
	 */
// MJM - this is not used since 3/1/17 because we are no using the 'reference'
// mode for accessing rating tables.
//	private String getRatingCheck(String ratingId)
//	{
//		String q = ratCheckQ + sqlString(ratingId);
//		try
//		{
//			ResultSet rs = doQuery(q);
//			if (!rs.next())
//				return null;
//			return rs.getString(1);
//		}
//		catch (Exception ex)
//		{
//			warning("Cannot get rating check, q=" + q);
//			return null;
//		}
//	}
}
