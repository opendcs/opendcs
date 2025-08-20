/**
 * $Id$
 * 
 * $Log$
 * Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
 * OPENDCS 6.0 Initial Checkin
 *
 * Revision 1.4  2013/03/21 18:27:40  mmaloney
 * DbKey Implementation
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

import java.util.Date;

import decodes.cwms.BadRatingException;
import decodes.db.Constants;
import decodes.sql.DbKey;

/**
 * Bean holding a reference to a rating in the CWMS database.
 * This is the information in the CWMS_V_RATING view.
 * @author mmaloney
 *
 */
public class CwmsRatingRef
{
	/** surrogate key */
	private DbKey ratingCode = Constants.undefinedId;
	
	/** office ID */
	private String officeId = null;
	
	/** CWMS Location ID = CWMS Site name Value */
	private String location = null;
	
	/** Independent 'Param' IDs */
	private String indep[] = new String[0];
	
	/** Dependent 'Param' ID */
	private String dep = null;
	
	/** Version of the Rating Template */
	private String tplVersion = null;
	
	/** Version of the Rating Specification */
	private String specVersion = null;
	
	/** Effective Date */
	private Date effectiveDate = null;
	
	/** Date created in the DB */
	private Date createDate = null;
	
	/** Active flag */
	private boolean active = false;

	/**
	 * Constructor called from CwmsRatingDao after reading CWMS_V_RATING
	 * @param ratingCode
	 * @param ratingId
	 * @param effectiveDate
	 * @param createDate
	 * @param active
	 */
	public CwmsRatingRef(DbKey ratingCode, String officeId, String ratingId, 
		Date effectiveDate,
		Date createDate, boolean active)
		throws BadRatingException
	{
		this.ratingCode = ratingCode;
		this.officeId = officeId;
		this.effectiveDate = effectiveDate;
		this.createDate = createDate;
		this.active = active;
		
		String parts[] = ratingId.split("\\.");
		if (parts.length != 4)
			throw new BadRatingException("Only " + parts.length
				+ " parts in id '" + ratingId + "'");
		
		this.location = parts[0];
		this.tplVersion = parts[2];
		this.specVersion = parts[3];
		
		String params[] = parts[1].split("[,;]");
		if (params.length < 2)
			throw new BadRatingException("Only " + params.length
				+ " params in id '" + ratingId + "'");
		indep = new String[params.length-1];
		for(int i=0; i<params.length - 1; i++)
			indep[i] = params[i];
		dep = params[params.length - 1];
	}

	public DbKey getRatingCode()
	{
		return ratingCode;
	}

	public void setRatingCode(DbKey ratingCode)
	{
		this.ratingCode = ratingCode;
	}

	public String getOfficeId()
	{
		return officeId;
	}

	public void setOfficeId(String officeId)
	{
		this.officeId = officeId;
	}

	public String getLocation()
	{
		return location;
	}

	public void setLocation(String location)
	{
		this.location = location;
	}

	public String[] getIndep()
	{
		return indep;
	}

	public void setIndep(String[] indep)
	{
		this.indep = indep;
	}

	public String getDep()
	{
		return dep;
	}

	public void setDep(String dep)
	{
		this.dep = dep;
	}

	public String getTplVersion()
	{
		return tplVersion;
	}

	public void setTplVersion(String tplVersion)
	{
		this.tplVersion = tplVersion;
	}

	public String getSpecVersion()
	{
		return specVersion;
	}

	public void setSpecVersion(String specVersion)
	{
		this.specVersion = specVersion;
	}

	public Date getEffectiveDate()
	{
		return effectiveDate;
	}

	public void setEffectiveDate(Date effectiveDate)
	{
		this.effectiveDate = effectiveDate;
	}

	public Date getCreateDate()
	{
		return createDate;
	}

	public void setCreateDate(Date createDate)
	{
		this.createDate = createDate;
	}

	public boolean isActive()
	{
		return active;
	}

	public void setActive(boolean active)
	{
		this.active = active;
	}

	public String toString()
	{
		StringBuilder ret = new StringBuilder(getRatingSpecId());
		ret.append(" effective=" + effectiveDate + " ");
		ret.append("create=" + createDate + " ");
		ret.append("active=" + active);
		return ret.toString();
	}
	
	public String getRatingSpecId()
	{
		StringBuilder ret = new StringBuilder(location + ".");
		for(int i = 0; i<indep.length; i++)
		{
			ret.append(indep[i]);
			if (i < indep.length-1)
				ret.append(",");
		}
		ret.append(";" + dep + "." + tplVersion + "." + specVersion);
		return ret.toString();
	}
}
