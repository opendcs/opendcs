package lrgs.apistatus;
public final class ArchiveStatistics
{
	public int dirOldest;
	public int dirNext;
	public short dirWrap;
	public int dirSize;
	public int oldestOffset;
	public int nextOffset;
	public int oldestMsgTime;
	public int lastSeqNum;
	public int maxMessages;
	public int maxBytes;
	public ArchiveStatistics(){}
	public ArchiveStatistics(int dirOldest, int dirNext, short dirWrap, int dirSize, int oldestOffset, int nextOffset, int oldestMsgTime, int lastSeqNum, int maxMessages, int maxBytes)
	{
		this.dirOldest = dirOldest;
		this.dirNext = dirNext;
		this.dirWrap = dirWrap;
		this.dirSize = dirSize;
		this.oldestOffset = oldestOffset;
		this.nextOffset = nextOffset;
		this.oldestMsgTime = oldestMsgTime;
		this.lastSeqNum = lastSeqNum;
		this.maxMessages = maxMessages;
		this.maxBytes = maxBytes;
	}
}
