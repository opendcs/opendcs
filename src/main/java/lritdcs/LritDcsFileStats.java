package lritdcs;

import java.io.File;
import java.util.Date;

import lrgs.common.DcpMsg;

public class LritDcsFileStats
{
	/** H=High, M=Medium, L=Low */
	private char priority = 'R'; // retransmit
	private File file = null;
	private int numMessages = 0;
	private Date earliestCarrierEndTime = null;
	private Date earliestLocalRcvTime = null;
	private Date latestCarrierEndTime = null;
	private Date latestLocalRcvTime = null;
	private Date latestAppRcvTime = null;
	private Date fileSaveTime = null;
	private Date dom2AXferCompleteTime = null;
	private Date dom2ARenameCompleteTime = null;
	private Date dom2BXferCompleteTime = null;
	private Date dom2BRenameCompleteTime = null;
	private Date dom2CXferCompleteTime = null;
	private Date dom2CRenameCompleteTime = null;
	private Date allTransfersCompleteTime = null;
	
	public void clear()
	{
		file = null;
		numMessages = 0;
		earliestCarrierEndTime = null;
		earliestLocalRcvTime = null;
		latestCarrierEndTime = null;
		latestLocalRcvTime = null;
		fileSaveTime = null;
		dom2AXferCompleteTime = null;
		dom2ARenameCompleteTime = null;
		dom2BXferCompleteTime = null;
		dom2BRenameCompleteTime = null;
		dom2CXferCompleteTime = null;
		dom2CRenameCompleteTime = null;
		allTransfersCompleteTime = null;
	}
	
	public void messageAdded(DcpMsg msg)
	{
		Date carrierEnd = msg.getCarrierStop();
		if (carrierEnd == null)
			carrierEnd = new Date(0L);
		Date localRcv = msg.getLocalReceiveTime();
		if (localRcv == null)
			localRcv = new Date(0L);

		if (numMessages == 0)
		{
			earliestLocalRcvTime = localRcv;
			earliestCarrierEndTime = carrierEnd;
		}
		
		// Since we receive in time order, the last one added is the latest.
		latestCarrierEndTime = carrierEnd;
		latestLocalRcvTime = localRcv;
		latestAppRcvTime = new Date();

		numMessages++;
	}

	public char getPriority()
	{
		return priority;
	}

	public void setPriority(char priority)
	{
		this.priority = priority;
	}

	public File getFile()
	{
		return file;
	}

	public void setFile(File file)
	{
		this.file = file;
	}

	public Date getFileSaveTime()
	{
		return fileSaveTime;
	}

	public void setFileSaveTime(Date fileSaveTime)
	{
		this.fileSaveTime = fileSaveTime;
	}

	public int getNumMessages()
	{
		return numMessages;
	}

	public Date getEarliestCarrierEndTime()
	{
		return earliestCarrierEndTime;
	}

	public Date getEarliestLocalRcvTime()
	{
		return earliestLocalRcvTime;
	}

	public Date getLatestCarrierEndTime()
	{
		return latestCarrierEndTime;
	}

	public Date getLatestLocalRcvTime()
	{
		return latestLocalRcvTime;
	}

	public Date getDom2AXferCompleteTime()
	{
		return dom2AXferCompleteTime;
	}

	public void setDom2AXferCompleteTime(Date dom2aXferCompleteTime)
	{
		dom2AXferCompleteTime = dom2aXferCompleteTime;
	}

	public Date getDom2ARenameCompleteTime()
	{
		return dom2ARenameCompleteTime;
	}

	public void setDom2ARenameCompleteTime(Date dom2aRenameCompleteTime)
	{
		dom2ARenameCompleteTime = dom2aRenameCompleteTime;
	}

	public Date getDom2BXferCompleteTime()
	{
		return dom2BXferCompleteTime;
	}

	public void setDom2BXferCompleteTime(Date dom2bXferCompleteTime)
	{
		dom2BXferCompleteTime = dom2bXferCompleteTime;
	}

	public Date getDom2BRenameCompleteTime()
	{
		return dom2BRenameCompleteTime;
	}

	public void setDom2BRenameCompleteTime(Date dom2bRenameCompleteTime)
	{
		dom2BRenameCompleteTime = dom2bRenameCompleteTime;
	}

	public Date getDom2CXferCompleteTime()
	{
		return dom2CXferCompleteTime;
	}

	public void setDom2CXferCompleteTime(Date dom2cXferCompleteTime)
	{
		dom2CXferCompleteTime = dom2cXferCompleteTime;
	}

	public Date getDom2CRenameCompleteTime()
	{
		return dom2CRenameCompleteTime;
	}

	public void setDom2CRenameCompleteTime(Date dom2cRenameCompleteTime)
	{
		dom2CRenameCompleteTime = dom2cRenameCompleteTime;
	}

	public Date getAllTransfersCompleteTime()
	{
		return allTransfersCompleteTime;
	}

	public void setAllTransfersCompleteTime(Date allTransfersCompleteTime)
	{
		this.allTransfersCompleteTime = allTransfersCompleteTime;
	}

	public Date getLatestAppRcvTime()
	{
		return latestAppRcvTime;
	}

	public void setLatestAppRcvTime(Date latestAppRcvTime)
	{
		this.latestAppRcvTime = latestAppRcvTime;
	}
}
