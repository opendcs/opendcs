package lrgs.db;

import java.util.Date;
import java.util.TimeZone;
import java.text.SimpleDateFormat;

import lrgs.common.DcpAddress;
import ilex.util.Logger;

/**
 * 
 * This encapsulates information about LRGS domsat_gap, system_outage and
 * damsnt_outage. The outage type will indicate where the data will be stored
 * or read from.
 * <p>
 * Outage implements the Comparable interface, allowing LRGS to store a
 * priority queue of Outage objects. 
 */
public class Outage
	implements Comparable<Outage>, LrgsDatabaseObject
{
	private int outageId;
	private Date beginTime;
	private Date endTime;
	private char outageType;
	private char statusCode;
	private int sourceId;
	private int dcpAddress;
	private int beginSeq;
	private int endSeq;

	/** True after this outage has been saved to the database. */
	private boolean _inDb;

	/** Transient data source name (for dams-nt outages) not stored in DB */
	private String dataSourceName;

	private static SimpleDateFormat sdf = 
		new SimpleDateFormat("MMM dd, yyyy ('day' DDD) HH:mm z");
	static
	{
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
	}

	/**
	 * Constructor.
	 * Initialize all private variables.
	 */
	public Outage()
	{
		outageId = 0;
		beginTime = null;
		endTime = null;
		outageType = '\0';
		statusCode = LrgsConstants.outageStatusActive;
		sourceId = 0;
		dcpAddress = 0;
		beginSeq = 0;
		endSeq = 0;
		_inDb = false;
	}

	/**
	 * Construct Outage Object from a outageId, beginTime, endTime, outageType,
	 * statusCode, sourceId, dcpAddress, beginSeq and endSeq.
	 * 
	 * @param outageId the unique id for the outage
	 * @param beginTime begin time of the outage
	 * @param endTime end time of the outage
	 * @param outageType indicates the type of outage (S - system, G - domsat C - dams-nt)
	 * @param statusCode the status code of the outage
	 * @param sourceId the source id of the outage
	 * @param dcpAddress the dcp address number of the outage
	 * @param beginSeq indicates the outage begin sequence number
	 * @param endSeq indicates the outage end sequence number
	 */
	public Outage(int outageId, Date beginTime, Date endTime, char outageType,
			char statusCode, int sourceId, int dcpAddress, int beginSeq, int endSeq)
	{
		this();
		this.outageId = outageId;
		this.beginTime = beginTime;
		this.endTime = endTime;
		this.outageType = outageType;
		this.statusCode = statusCode;
		this.sourceId = sourceId;
		this.dcpAddress = dcpAddress;
		this.beginSeq = beginSeq;
		this.endSeq = endSeq;
	}

	/** Copy Constructor */
	public Outage(Outage rhs)
	{
		this();
		this.outageId = rhs.outageId;
		this.beginTime = rhs.beginTime;
		this.endTime = rhs.endTime;
		this.outageType = rhs.outageType;
		this.statusCode = rhs.statusCode;
		this.sourceId = rhs.sourceId;
		this.dcpAddress = rhs.dcpAddress;
		this.beginSeq = rhs.beginSeq;
		this.endSeq = rhs.endSeq;
	}

	/**
	 * This method returns the begin time of the outage.
	 *
	 * @return beginTime begin time of the outage
	 */
	public Date getBeginTime()
	{
		return beginTime;
	}

	/**
	 * This method sets the begin time of the outage.
	 * 
	 * @param beginTime begin time of the outage
	 */
	public void setBeginTime(Date beginTime)
	{
		this.beginTime = beginTime;
	}

	/**
	 * This method returns the dcp address number of the outage.
	 *
	 * @return dcpAddress the dcp address number of the outage
	 */
	public int getDcpAddress()
	{
		return dcpAddress;
	}

	/**
	 * This method sets the dcp address number of the outage.
	 * 
	 * @param dcpAddress the dcp address number of the outage
	 */
	public void setDcpAddress(int dcpAddress)
	{
		this.dcpAddress = dcpAddress;
	}

	/**
	 * This method returns the end time of the outage.
	 *
	 * @return endTime end time of the outage
	 */
	public Date getEndTime()
	{
		return endTime;
	}

	/**
	 * This method sets the end time of the outage.
	 * 
	 * @param endTime end time of the outage
	 */
	public void setEndTime(Date endTime)
	{
		this.endTime = endTime;
	}

	/**
	 * This method returns the unique id for the outage.
	 *
	 * @return outageId the unique id for the outage
	 */
	public int getOutageId()
	{
		return outageId;
	}

	/**
	 * This method sets the unique id for the outage.
	 * 
	 * @param outageId the unique id for the outage
	 */
	public void setOutageId(int outageId)
	{
		this.outageId = outageId;
	}

	/**
	 * This method returns the outage type.
	 *
	 * @return outageType indicates the type of outage (S - system, G - domsat C - dams-nt)
	 */
	public char getOutageType()
	{
		return outageType;
	}

	/**
	 * This method sets the outage type.
	 * 
	 * @param outageType indicates the type of outage (S - system, G - domsat C - dams-nt)
	 */
	public void setOutageType(char outageType)
	{
		this.outageType = outageType;
	}

	/**
	 * This method returns the outage begin sequence number.
	 *
	 * @return beginSeq indicates the outage begin sequence number
	 */
	public int getBeginSeq()
	{
		return beginSeq;
	}

	/**
	 * This method sets the outage begin sequence number.
	 * 
	 * @param beginSeq indicates the outage begin sequence number
	 */
	public void setBeginSeq(int beginSeq)
	{
		this.beginSeq = beginSeq;
	}

	/**
	 * This method returns the outage end sequence number.
	 *
	 * @return endSeq indicates the outage end sequence number
	 */
	public int getEndSeq()
	{
		return endSeq;
	}

	/**
	 * This method sets the outage end sequence number.
	 * 
	 * @param endSeq indicates the outage end sequence number
	 */
	public void setEndSeq(int endSeq)
	{
		this.endSeq = endSeq;
	}
	
	/**
	 * This method returns the source id of the outage.
	 *
	 * @return sourceId the source id of the outage
	 */
	public int getSourceId()
	{
		return sourceId;
	}

	/**
	 * This method sets the source id of the outage.
	 * 
	 * @param sourceId the source id of the outage
	 */
	public void setSourceId(int sourceId)
	{
		this.sourceId = sourceId;
	}

	/**
	 * This method returns the status code of the outage.
	 *
	 * @return statusCode the status code of the outage
	 */
	public char getStatusCode()
	{
		return statusCode;
	}

	/**
	 * This method sets the status code of the outage.
	 * 
	 * @param statusCode the status code of the outage
	 */
	public void setStatusCode(char statusCode)
	{
		this.statusCode = statusCode;
	}

	/**
	 * Comparable method causing Outages to be sorted by priority.
	 */
	public int compareTo(Outage rhs)
	{
		if (this == rhs)
			return 0;

		// Active outages are always less than inactive ones.
		if (this.statusCode == LrgsConstants.outageStatusActive)
		{
			if (rhs.statusCode != LrgsConstants.outageStatusActive)
				return -1;
		}
		else if (rhs.statusCode == LrgsConstants.outageStatusActive)
			return 1;

		// Else, they're either both active or both inactive.
		// Sort by outage type, and then begin time.

		if (this.outageType == LrgsConstants.systemOutageType)
		{
			if (rhs.outageType == LrgsConstants.systemOutageType)
				// Most recent system outage has highest priority.
				return -(this.beginTime.compareTo(rhs.beginTime));
			else
				return -1; // System outage is always < other types.
		}
		else if (this.outageType == LrgsConstants.realTimeOutageType)
		{
			if (rhs.outageType == LrgsConstants.systemOutageType)
				return 1;
			else if (rhs.outageType == LrgsConstants.realTimeOutageType)
				 return -(this.beginTime.compareTo(rhs.beginTime));
			else
				return -1;
		}
		else if (this.outageType == LrgsConstants.domsatGapOutageType)
		{
			if (rhs.outageType == LrgsConstants.systemOutageType
			 || rhs.outageType == LrgsConstants.realTimeOutageType)
				return 1;
			else if (rhs.outageType == LrgsConstants.domsatGapOutageType)
				 return -(this.beginTime.compareTo(rhs.beginTime));
			else
				return -1;
		}
		else if (this.outageType == LrgsConstants.damsntOutageType)
		{
			if (rhs.outageType == LrgsConstants.systemOutageType
			 || rhs.outageType == LrgsConstants.realTimeOutageType
			 || rhs.outageType == LrgsConstants.domsatGapOutageType)
				return 1;
			else if (rhs.outageType == LrgsConstants.damsntOutageType)
			{
				int dt = -(this.beginTime.compareTo(rhs.beginTime));
				if (dt != 0)
					return dt;
				return rhs.outageId - this.outageId;
			}
			else
				return -1;
		}
		else if (this.outageType == LrgsConstants.missingDCPMsgOutageType)
		{
			if (rhs.outageType == LrgsConstants.systemOutageType
			 || rhs.outageType == LrgsConstants.realTimeOutageType
			 || rhs.outageType == LrgsConstants.domsatGapOutageType
			 || rhs.outageType == LrgsConstants.damsntOutageType)
				return 1;
			else if (rhs.outageType == LrgsConstants.missingDCPMsgOutageType)
			{
				int dt = -(this.beginTime.compareTo(rhs.beginTime));
				if (dt != 0)
					return dt;
				return rhs.outageId - this.outageId;
			}
			else
				return -1;
		}
		// Shouldn't get here, all possibilities are handled above.
Logger.instance().warning("Failure in comparing outages this=("
+ this + ") rhs=(" + rhs + ")");
		return 0;
	}

	/**
	 * @return a printable string description of this outage.
	 */
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append(LrgsConstants.outageTypeName(outageType)
			+ " Outage, id=" + outageId 
			+ ", status=" + LrgsConstants.outageStatusName(statusCode)
			+ ", begin=" + sdf.format(beginTime));
		if (endTime != null)
			sb.append(", end=" + sdf.format(endTime));
		if (outageType == LrgsConstants.domsatGapOutageType)
			sb.append(", seqRange=" + beginSeq + "..." + endSeq);
		else if (outageType == LrgsConstants.damsntOutageType)
			sb.append(", source=" + sourceId);
		else if (outageType == LrgsConstants.missingDCPMsgOutageType)
			sb.append(", dcp=" + new DcpAddress(dcpAddress));

		return sb.toString();
	}

	/** @return true if this outage has been saved to the database. */
	public boolean getInDb() { return _inDb; }

	/** Sets the flag indicating this outage exists in the database. */
	public void setInDb(boolean inDb) { _inDb = inDb; }

	/** Sets the data source name */
	public void setDataSourceName(String n) { dataSourceName = n; }

	/** Gets the data source name */
	public String getDataSourceName() { return dataSourceName; }
}
