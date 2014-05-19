/*
*  $Id$
*
*  This is open-source software written by ILEX Engineering, Inc., under
*  contract to the federal government. You are free to copy and use this
*  source code for your own purposes, except that no part of the information
*  contained in this file may be claimed to be proprietary.
*
*  Except for specific contractual terms between ILEX and the federal 
*  government, this source code is provided completely without warranty.
*  For more information contact: info@ilexeng.com
*/
package decodes.tsdb;

/**
This is a utility class used by algorithms. The DbAlgorithmExec.apply()
method constructs a hashmap of these objects by role name. This provides
an easy way for algorithm subclasses to retrieve the computation parameter
meta-data and the time-series data.
*/
public class ParmRef
{
	/** The role name */
	public String role;

	/** The Computation Parameter object */
	public DbCompParm compParm;

	/** The time series */
	public CTimeSeries timeSeries;

	public MissingAction missingAction;

	public ParmRef(String role, DbCompParm compParm, CTimeSeries timeSeries)
	{
		this.role = role;
		this.compParm = compParm;
		this.timeSeries = timeSeries;
		this.missingAction = MissingAction.FAIL;
	}

	public void setMissingAction(String ma)
	{
		if (ma == null)
			missingAction = MissingAction.FAIL;
		else for(MissingAction action : MissingAction.values())
			if (ma.equalsIgnoreCase(action.toString()))
				missingAction = action;
	}

	public String getDescription()
	{
		StringBuilder sb = new StringBuilder();
		sb.append("role=" + role);
		sb.append(", sdi=" + compParm.getSiteDataTypeId());
		sb.append(", intv=" + compParm.getInterval());
		sb.append(", tabsel=" + compParm.getTableSelector());
		if (compParm.getTableSelector().equals("M_"))
		{
			sb.append(", modelId=" + compParm.getModelId());
			sb.append(", modelRunId=" + timeSeries.getModelRunId());
		}
		return sb.toString();
	}
}
