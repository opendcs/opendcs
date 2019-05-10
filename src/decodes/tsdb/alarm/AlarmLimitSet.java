/**
 * $Id$
 * 
 * $Log$
 */
package decodes.tsdb.alarm;

import decodes.sql.DbKey;

/**
 * Bean class holding a single record in the ALARM_LIMIT_SET table.
 * @author mmaloney
 *
 */
public class AlarmLimitSet
{
	public static final double UNASSIGNED_LIMIT = Double.NEGATIVE_INFINITY;
	
	/** Primary key */
	private DbKey limitSetId = DbKey.NullKey;
	
	/** Link to parent AlarmScreening object */
	private DbKey screeningId = DbKey.NullKey;
	
	/** Optional seasonName */
	private String seasonName = null;
	
	
	// Absolute value limits
	private double rejectHigh = UNASSIGNED_LIMIT;
	private double criticalHigh = UNASSIGNED_LIMIT;
	private double warningHigh = UNASSIGNED_LIMIT;
	private double warningLow = UNASSIGNED_LIMIT;
	private double criticalLow = UNASSIGNED_LIMIT;
	private double rejectLow = UNASSIGNED_LIMIT;
	
	// Stuck sensor detection
	private String stuckDuration = null;
	private double stuckTolerance = 0.0;
	private double minToCheck = UNASSIGNED_LIMIT;
	private String maxGap = null;
	
	// Rate Of Change limits
	private String rocInterval = null;
	private double rejectRocHigh = UNASSIGNED_LIMIT;
	private double criticalRocHigh = UNASSIGNED_LIMIT;
	private double warningRocHigh = UNASSIGNED_LIMIT;
	private double warningRocLow = UNASSIGNED_LIMIT;
	private double criticalRocLow = UNASSIGNED_LIMIT;
	private double rejectRocLow = UNASSIGNED_LIMIT;
	
	// Missing value limits
	/** Period over which to check */
	private String missingPeriod = null;
	/** Should match interval of input param */
	private String missingInterval = null;
	/** More than this many missing values in the period is an alarm */
	private int maxMissingValues = 0;
	
	/** Will accompany alarm messages for this limit set */
	private String hintText = null;


	public AlarmLimitSet()
	{
	}


	public DbKey getLimitSetId()
	{
		return limitSetId;
	}


	public void setLimitSetId(DbKey limitSetId)
	{
		this.limitSetId = limitSetId;
	}


	public DbKey getScreeningId()
	{
		return screeningId;
	}


	public void setScreeningId(DbKey screeningId)
	{
		this.screeningId = screeningId;
	}


	public String getSeasonName()
	{
		return seasonName;
	}


	public void setSeasonName(String seasonName)
	{
		this.seasonName = seasonName;
	}


	public double getRejectHigh()
	{
		return rejectHigh;
	}


	public void setRejectHigh(double rejectHigh)
	{
		this.rejectHigh = rejectHigh;
	}


	public double getCriticalHigh()
	{
		return criticalHigh;
	}


	public void setCriticalHigh(double criticalHigh)
	{
		this.criticalHigh = criticalHigh;
	}


	public double getWarningHigh()
	{
		return warningHigh;
	}


	public void setWarningHigh(double warningHigh)
	{
		this.warningHigh = warningHigh;
	}


	public double getWarningLow()
	{
		return warningLow;
	}


	public void setWarningLow(double warningLow)
	{
		this.warningLow = warningLow;
	}


	public double getCriticalLow()
	{
		return criticalLow;
	}


	public void setCriticalLow(double criticalLow)
	{
		this.criticalLow = criticalLow;
	}


	public double getRejectLow()
	{
		return rejectLow;
	}


	public void setRejectLow(double rejectLow)
	{
		this.rejectLow = rejectLow;
	}


	public String getStuckDuration()
	{
		return stuckDuration;
	}


	public void setStuckDuration(String stuckDuration)
	{
		this.stuckDuration = stuckDuration;
	}


	public double getStuckTolerance()
	{
		return stuckTolerance;
	}


	public void setStuckTolerance(double stuckTolerance)
	{
		this.stuckTolerance = stuckTolerance;
	}


	public double getMinToCheck()
	{
		return minToCheck;
	}


	public void setMinToCheck(double minToCheck)
	{
		this.minToCheck = minToCheck;
	}


	public String getMaxGap()
	{
		return maxGap;
	}


	public void setMaxGap(String maxGap)
	{
		this.maxGap = maxGap;
	}


	public String getRocInterval()
	{
		return rocInterval;
	}


	public void setRocInterval(String rocInterval)
	{
		this.rocInterval = rocInterval;
	}


	public double getRejectRocHigh()
	{
		return rejectRocHigh;
	}


	public void setRejectRocHigh(double rejectRocHigh)
	{
		this.rejectRocHigh = rejectRocHigh;
	}


	public double getCriticalRocHigh()
	{
		return criticalRocHigh;
	}


	public void setCriticalRocHigh(double criticalRocHigh)
	{
		this.criticalRocHigh = criticalRocHigh;
	}


	public double getWarningRocHigh()
	{
		return warningRocHigh;
	}


	public void setWarningRocHigh(double warningRocHigh)
	{
		this.warningRocHigh = warningRocHigh;
	}


	public double getWarningRocLow()
	{
		return warningRocLow;
	}


	public void setWarningRocLow(double warningRocLow)
	{
		this.warningRocLow = warningRocLow;
	}


	public double getCriticalRocLow()
	{
		return criticalRocLow;
	}


	public void setCriticalRocLow(double criticalRocLow)
	{
		this.criticalRocLow = criticalRocLow;
	}


	public double getRejectRocLow()
	{
		return rejectRocLow;
	}


	public void setRejectRocLow(double rejectRocLow)
	{
		this.rejectRocLow = rejectRocLow;
	}


	public String getMissingPeriod()
	{
		return missingPeriod;
	}


	public void setMissingPeriod(String missingPeriod)
	{
		this.missingPeriod = missingPeriod;
	}


	public String getMissingInterval()
	{
		return missingInterval;
	}


	public void setMissingInterval(String missingInterval)
	{
		this.missingInterval = missingInterval;
	}


	public int getMaxMissingValues()
	{
		return maxMissingValues;
	}


	public void setMaxMissingValues(int maxMissingValues)
	{
		this.maxMissingValues = maxMissingValues;
	}


	public String getHintText()
	{
		return hintText;
	}


	public void setHintText(String hintText)
	{
		this.hintText = hintText;
	}

}
