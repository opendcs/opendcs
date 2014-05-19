/**
 * $Id$
 * 
 * @author mjmaloney
 *
 * $Log$
 * Revision 1.2  2013/03/21 18:27:39  mmaloney
 * DbKey Implementation
 *
 * Revision 1.1  2008/09/29 00:22:08  mjmaloney
 * Network List Maintenance GUI Improvements
 *
 */
package decodes.db;

import java.util.Date;

import decodes.sql.DbKey;

/**
 * New lightweight descriptor for a network list. This holds the info
 * needed to populate a list of network lists, as in the database editor
 * or a selection pull-down.
 */
public class NetworkListSpec
{
	/** SQL Surrogate Key for this network list, or Constants.undefinedId */
	private DbKey id = Constants.undefinedId;
	
	/** Name of this network list */
	private String name = null;
	
	/** Transport medium type for entries in this list. */
	private String tmType = Constants.medium_Goes;

	/** Preferred site-name type for elements in this list */
	private String siteNameType = Constants.snt_NWSHB5;
	
	/** Last-modify time for this list */
	private Date lastModified = null;
	
	/** Number of entries in this list */
	private int numEntries = 0;
	

	/** Default constructor for empty list-spec */
	public  NetworkListSpec()
	{
	}
	
	/**
     * @param id SQL Surrogate Key, or Constants.undefinedId
     * @param name Name of this network list
     * @param tmType Transport medium type for entries in this list
     * @param siteNameType Preferred site-name type
     * @param lastModified Last-modify time for this list
     * @param numEntries Number of entries in this list
     */
    public NetworkListSpec(DbKey id, String name, String tmType,
        String siteNameType, Date lastModified, int numEntries)
    {
	    super();
	    this.id = id;
	    this.name = name;
	    this.tmType = tmType;
	    this.siteNameType = siteNameType;
	    this.lastModified = lastModified;
	    this.numEntries = numEntries;
    }

    /**
     * @return the id
     */
    public DbKey getId()
    {
    	return id;
    }

	/**
     * @param id the id to set
     */
    public void setId(DbKey id)
    {
    	this.id = id;
    }

	/**
     * @return the name
     */
    public String getName()
    {
    	return name;
    }

	/**
     * @param name the name to set
     */
    public void setName(String name)
    {
    	this.name = name;
    }

	/**
     * @return the tmType
     */
    public String getTmType()
    {
    	return tmType;
    }

	/**
     * @param tmType the tmType to set
     */
    public void setTmType(String tmType)
    {
    	this.tmType = tmType;
    }

	/**
     * @return the siteNameType
     */
    public String getSiteNameType()
    {
    	return siteNameType;
    }

	/**
     * @param siteNameType the siteNameType to set
     */
    public void setSiteNameType(String siteNameType)
    {
    	this.siteNameType = siteNameType;
    }

	/**
     * @return the lastModified
     */
    public Date getLastModified()
    {
    	return lastModified;
    }

	/**
     * @param lastModified the lastModified to set
     */
    public void setLastModified(Date lastModified)
    {
    	this.lastModified = lastModified;
    }

	/**
     * @return the numEntries
     */
    public int getNumEntries()
    {
    	return numEntries;
    }

	/**
     * @param numEntries the numEntries to set
     */
    public void setNumEntries(int numEntries)
    {
    	this.numEntries = numEntries;
    }

	
}
