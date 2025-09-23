/*
* Where Applicable, Copyright 2025 OpenDCS Consortium and/or its contributors
*
* Licensed under the Apache License, Version 2.0 (the "License"); you may not
* use this file except in compliance with the License. You may obtain a copy
* of the License at
*
*   http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
* License for the specific language governing permissions and limitations
* under the License.
*/
package decodes.tsdb.alarm;


import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import decodes.db.Constants;
import decodes.db.Database;
import decodes.db.DbEnum;
import decodes.db.EnumValue;
import decodes.decoder.FieldParseException;
import decodes.decoder.Season;
import decodes.sql.DbKey;
import ilex.util.TextUtil;

/**
 * Bean class holding a single record in the ALARM_LIMIT_SET table.
 * @author mmaloney
 *
 */
public class AlarmLimitSet
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
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


	/** set in prepareForExec() */
	private transient Season season = null;
	private transient boolean prepared = false;


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

	public void prepareForExec()
	{
		if (seasonName != null && seasonName.trim().length() > 0
		 && !seasonName.contains("default"))
		{
			DbEnum seasonEnum = Database.getDb().enumList.getEnum(Constants.enum_Season);
			if (seasonEnum != null)
			{
				EnumValue ev = seasonEnum.findEnumValue(seasonName);
				if (ev == null)
				{
					log.warn("AlarmLimitSet with id={} season '{}' is not defined. Run rledit and add it.",
							 getLimitSetId(), this.getSeasonName());
				}
				else
				{
					try
					{
						season = new Season(ev);
					}
					catch (FieldParseException ex)
					{
						log.atWarn()
						   .setCause(ex)
						   .log("AlarmLimitSet with id={} season '{}' is not valid. Run rledit and fix it.",
						   		getLimitSetId(), this.getSeasonName());
						season = null;
					}
				}
			}
			else
			{
				log.warn("AlarmLimitSet with id={} season '{}' cannot be resolved " +
						 "-- there is no Seasons enumeration in this database.",
						 getLimitSetId(), seasonName);
			}
		}
		// Else either no season name or it's set to the "(default)" season (i.e. all year).
		prepared = true;
	}


	public Season getSeason()
	{
		return season;
	}


	public void setSeason(Season season)
	{
		this.season = season;
		seasonName = season == null ? null : season.getAbbr();
	}

	/**
	 * Copy the values in the passed limit set to THIS limit set.
	 * @param als
	 */
	public void copyFrom(AlarmLimitSet als)
	{
		this.limitSetId = als.limitSetId;
		this.seasonName = als.seasonName;
		this.rejectHigh = als.rejectHigh;
		this.criticalHigh = als.criticalHigh;
		this.warningHigh = als.warningHigh;
		this.warningLow = als.warningLow;
		this.criticalLow = als.criticalLow;
		this.rejectLow = als.rejectLow;

		// Stuck sensor detection
		this.stuckDuration = als.stuckDuration;
		this.stuckTolerance = als.stuckTolerance;
		this.minToCheck = als.minToCheck;
		this.maxGap = als.maxGap;

		// Rate Of Change limits
		this.rocInterval = als.rocInterval;
		this.rejectRocHigh = als.rejectRocHigh;
		this.criticalRocHigh = als.criticalRocHigh;
		this.warningRocHigh = als.warningRocHigh;
		this.warningRocLow = als.warningRocLow;
		this.criticalRocLow = als.criticalRocLow;
		this.rejectRocLow = als.rejectRocLow;

		this.missingPeriod = als.missingPeriod;
		this.missingInterval = als.missingInterval;
		this.maxMissingValues = als.maxMissingValues;
		this.hintText = als.hintText;

		this.season = als.season;

	}

	@Override
	public boolean equals(Object rhs)
	{
		if (this == rhs)
			return true;
		if (!(rhs instanceof AlarmLimitSet))
			return false;
		AlarmLimitSet ls2 = (AlarmLimitSet)rhs;

		// ignore limitSetId

		if (!TextUtil.strEqual(seasonName, ls2.seasonName))
			return false;

		if (this.rejectHigh != ls2.rejectHigh
		 || this.criticalHigh != ls2.criticalHigh
		 || this.warningHigh != ls2.warningHigh
		 || this.warningLow != ls2.warningLow
		 || this.criticalLow != ls2.criticalLow
		 || this.rejectLow != ls2.rejectLow)
			return false;

		// Stuck sensor detection
		if (!TextUtil.strEqual(this.stuckDuration, ls2.stuckDuration))
			return false;

		if (this.stuckTolerance != ls2.stuckTolerance
		 || this.minToCheck != ls2.minToCheck
		 || !TextUtil.strEqual(this.maxGap, ls2.maxGap))
			return false;

		// Rate Of Change limits
		if (!TextUtil.strEqual(this.rocInterval,ls2.rocInterval))
			return false;

		if (this.rejectRocHigh != ls2.rejectRocHigh)
			return false;

		if (this.criticalRocHigh != ls2.criticalRocHigh)
			return false;

		if (this.warningRocHigh != ls2.warningRocHigh)
			return false;

		if (this.warningRocLow != ls2.warningRocLow)
			return false;

		if (this.criticalRocLow != ls2.criticalRocLow)
			return false;

		if (this.rejectRocLow != ls2.rejectRocLow)
			return false;

		if (!TextUtil.strEqual(missingPeriod, ls2.missingPeriod)
		 || !TextUtil.strEqual(missingInterval, ls2.missingInterval)
		 || this.maxMissingValues != ls2.maxMissingValues)
			return false;

		if (!TextUtil.strEqual(hintText, ls2.hintText))
			return false;

		return true;
	}


	public boolean isPrepared()
	{
		return prepared;
	}


	public boolean hasRocLimits()
	{
		if (rocInterval == null || rocInterval.trim().length() == 0)
			return false;

		if (rejectRocHigh == UNASSIGNED_LIMIT
		 && criticalRocHigh == UNASSIGNED_LIMIT
		 && warningRocHigh == UNASSIGNED_LIMIT
		 && warningRocLow == UNASSIGNED_LIMIT
		 && criticalRocLow == UNASSIGNED_LIMIT
		 && rejectRocLow == UNASSIGNED_LIMIT)
			return false;

		return true;
	}

}
