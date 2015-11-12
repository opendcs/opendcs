/**
 * $Id$
 * 
 * Copyright 2015 U.S. Army Corps of Engineers, Hydrologic Engineering Center.
 * 
 * $Log$
 */
package decodes.cwms.validation;

/**
 * Mapps a DSS path to a CWMS path. Also specifies the engineering units used
 * in the DATCHK files to specify units.
 * @author mmaloney
 *
 */
public class PathMapping
{
	/** The DSS Path */
	private String dssPath = null;
	
	/** The CWMS Path */
	private String cwmsPath = null;
	
	/** The Engineering Units used in the DATCHK files */
	private String dssUnitsAbbr = null;
	
	/**
	 * @param dssPath
	 * @param cwmsPath
	 * @param dssUnitsAbbr
	 */
	public PathMapping(String dssPath, String cwmsPath,
		String dssUnitsAbbr)
	{
		super();
		this.dssPath = dssPath;
		this.cwmsPath = cwmsPath;
		this.dssUnitsAbbr = dssUnitsAbbr;
	}

	public String getDssPath()
	{
		return dssPath;
	}

	public String getCwmsPath()
	{
		return cwmsPath;
	}

	public String getDssUnitsAbbr()
	{
		return dssUnitsAbbr;
	}


}
