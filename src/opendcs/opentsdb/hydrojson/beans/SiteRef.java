package opendcs.opentsdb.hydrojson.beans;

import java.util.HashMap;

import decodes.db.Site;
import decodes.db.SiteName;
import decodes.sql.DbKey;

public class SiteRef
{
	private long siteId = DbKey.NullKey.getValue();
	
	/** nametype - namevalue */
	private HashMap<String, String> sitenames = new HashMap<String, String>();
	
	private String description = null;
	
	public SiteRef()
	{	
	}
	
	public static SiteRef fromSite(Site site)
	{
		SiteRef ret = new SiteRef();
		ret.siteId = site.getId().getValue();
		for(SiteName sn : site.getNameArray())
			ret.sitenames.put(sn.getNameType(), sn.getNameValue());
		ret.description = site.getDescription();
		return ret;
	}

	public long getSiteId()
	{
		return siteId;
	}

	public void setSiteId(long siteId)
	{
		this.siteId = siteId;
	}

	public HashMap<String, String> getSitenames()
	{
		return sitenames;
	}

	public void setSitenames(HashMap<String, String> sitenames)
	{
		this.sitenames = sitenames;
	}

	public String getDescription()
	{
		return description;
	}

	public void setDescription(String description)
	{
		this.description = description;
	}
	
	

}
