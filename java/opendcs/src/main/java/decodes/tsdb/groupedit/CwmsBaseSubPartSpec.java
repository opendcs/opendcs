package decodes.tsdb.groupedit;

import javax.swing.JLabel;

/**
 * Bean class used by the three cwms-specific component selectors for location,
 * param, and version.
 * @author mmaloney
 */
public class CwmsBaseSubPartSpec
{
	String base;
	String sub;
	int numTsids;
	String full;
	
	public CwmsBaseSubPartSpec(String base, String sub, int numTsids, String full)
	{
		super();
		this.base = base;
		this.sub = sub;
		this.numTsids = numTsids;
		this.full = full;
	}

	public String getBase()
	{
		return base;
	}

	public String getSub()
	{
		return sub;
	}

	public int getNumTsids()
	{
		return numTsids;
	}

	public void incNumTsids() { ++numTsids; }

	public String getFull()
	{
		return full;
	}
}
