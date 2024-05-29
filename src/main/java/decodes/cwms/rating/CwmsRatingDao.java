/*
 * $Id$
 *
 * $Log$
 * Revision 1.16  2019/02/04 21:19:17  mmaloney
 * Don't use reference ratings for import/export and edit functions.
 *
 * Revision 1.15  2019/02/04 20:46:31  mmaloney
 * Switch BACK to the fromDatabase method for reading RatingSet objects (!).
 *
 * Revision 1.14  2019/01/22 21:29:43  mmaloney
 * dev
 *
 * Revision 1.13  2019/01/22 15:46:32  mmaloney
 * Set the RatingSet db connection. New requirement for 3.2
 *
 * Revision 1.12  2019/01/22 14:34:20  mmaloney
 * dev
 *
 * Revision 1.11  2019/01/22 13:03:29  mmaloney
 * dev
 *
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

import ilex.util.TextUtil;

import java.io.PrintStream;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
	private static final Logger log = LoggerFactory.getLogger(CwmsRatingDao.class);
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
	// Ratings older than this in the cache are discarded.
	private long MAX_AGE_MSEC = 9 * 3600000L;

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
			+ " where upper(OFFICE_ID) = upper(?)";
		ArrayList<Object> parameters = new ArrayList<>();
		parameters.add(officeId);

		if (locationId != null)
		{
			q = q + " and upper(LOCATION_ID) = upper(?)";
			parameters.add(locationId);
		}

		try
		{
			return getResultsIgnoringNull(
						q,
						rs -> 
						{
							try
							{
								return rs2rr(rs);
							}
							catch(BadRatingException ex)
							{
								log.atWarn()
								   .setCause(ex)
								   .log("Bad Rating was skipped.");
							}
							return null;
						},
						parameters.toArray(new Object[0])
					);
		}
		catch (SQLException ex)
		{
			String msg = "Error reading ratings";
			throw new DbIoException(msg, ex);
		}
	}

	public void deleteRating(CwmsRatingRef crr)
		throws RatingException
	{
		try
		{
			this.inTransaction(txDao ->
			{
				try (CwmsRatingDao dao = new CwmsRatingDao((CwmsTimeSeriesDb)this.db))
				{
					dao.inTransactionOf(txDao);
					String q =
							"{ call cwms_rating.delete_ratings(?," +
							"to_date(?,'DD.MM.YYYY HH24:MI:SS')," +
							"to_date(?,'DD.MM.YYYY HH24:MI:SS'),null, ?) }";
					try (Connection c = dao.getConnection();
						 CallableStatement cstmt = c.prepareCall(q);)
					{
						RatingSet ratingSet = RatingSet.fromDatabase(c,
						((CwmsTimeSeriesDb)db).getDbOfficeId(),
						crr.getRatingSpecId());

						if(ratingSet.getRatingCount() <= 1)
						{
							dao.deleteRatingSpec(crr);
							return;
						}

						SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
						sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

						cstmt.setString(1, crr.getRatingSpecId());
						cstmt.setString(2, sdf.format(crr.getEffectiveDate()));
						cstmt.setString(3, sdf.format(crr.getEffectiveDate()));
						cstmt.setString(4, crr.getOfficeId());
						cstmt.execute();
					}
				}
			});
		}
		catch (Exception ex)
		{
			throw new RatingException("Unable to delete rating.", ex);
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

		String doing = "prepare call";
		try (Connection c = getConnection();
			 CallableStatement cstmt = c.prepareCall(q);)
		{
			doing = "set params for";
			cstmt.setString(1, crr.getRatingSpecId());
			cstmt.setString(2, crr.getOfficeId());
			doing = "execute";
			cstmt.execute();
		}
		catch (SQLException ex)
		{
			String msg = "Cannot {} '{}' for specId '{}' and office '{}'";

			log.atWarn()
			   .setCause(ex)
			   .log(msg, doing, q, crr.getRatingSpecId(), crr.getOfficeId());
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
				{
					ratingSet.removeRating(rating.getEffectiveDate());
				}
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
		log.trace("importXmlToDatabase: {}");
		RatingSet newSet = RatingSet.fromXml(xml);
		String specId = newSet.getRatingSpec().getRatingSpecId();
		log.trace("importXmlToDatabase fromXml success, specId='{}'", specId + "'");
		try
		{
			log.trace("importXmlToDatabase trying to read existing specId='{}'", specId);
			RatingSet dbSet = getRatingSet(specId);
			if (dbSet != null)
			{
				log.trace("importXmlToDatabase spec exists");
				for(AbstractRating newAr : newSet.getRatings())
				{
					boolean replaced = false;
					for(AbstractRating oldAr : dbSet.getRatings())
					{
						if (newAr.getEffectiveDate() == oldAr.getEffectiveDate())
						{
							if (log.isTraceEnabled())
							{
								log.trace("importXmlToDatabase replacing rating with same spec and effectiveDate={}", new Date(newAr.getEffectiveDate()));
							}
							dbSet.replaceRating(newAr);
							replaced = true;
							break;
						}
					}
					if (!replaced)
					{
						if (log.isTraceEnabled())
						{
							log.trace("importXmlToDatabase adding rating with effectiveDate={}", new Date(newAr.getEffectiveDate()));
						}
						dbSet.addRating(newAr);
					}
				}
				newSet = dbSet;
			}
			else
			{
				log.trace("importXmlToDatabase spec does not exist.");
			}
		}
		catch(RatingException ex)
		{
			log.atWarn()
			   .setCause(ex)
			   .log("Cannot read rating for spec ID '{}'", specId);
		}

		log.trace("Calling storeToDatabase");
		try (Connection c = getConnection())
		{
			newSet.storeToDatabase(c, true);
		}
		catch (SQLException ex)
		{
			log.atError()
			   .setCause(ex)
			   .log("Unable to store rating set to database.");
		}
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
			if (System.currentTimeMillis() - rw.timeLoaded.getTime() < MAX_AGE_MSEC)
			{
				rw.lastTimeUsed = new Date();
				log.trace(
					"Retrieving rating spec from cache with officeId={} and spec '{}' -- was loaded into cache at {}",
					officeId, specId, rw.timeLoaded);
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
				log.trace("Removing '{}' from cache because cache is full.", oldestSpec);
				ratingCache.remove(oldestSpec);
			}
		}

		log.trace("Constructing RatingSet with officeId={} and spec '{}'", officeId, specId);
		Date timeLoaded = new Date();
		try (Connection c = getConnection())
		{
			RatingSet ratingSet = RatingSet.fromDatabase(c, officeId, specId);
			ratingCache.put(ucSpecId, new RatingWrapper(timeLoaded, ratingSet, timeLoaded));
			if (log.isTraceEnabled())
			{
				log.trace("Reading rating from database took "
					+ (System.currentTimeMillis()/1000L - timeLoaded.getTime()/1000L) + " seconds.");
			}
			return ratingSet;
		}
		catch (SQLException ex)
		{
			throw new RatingException("Unable to load rating " + specId, ex);
		}
	}
}
