package lrgs.apistatus;
public final class AttachedProcess
{
	public int pid;
	public java.lang.String name;
	public java.lang.String type;
	public java.lang.String user;
	public int lastSeqNum;
	public int lastPollTime;
	public int lastMsgTime;
	public java.lang.String status;
	public short stale_count;

	public String ddsVersion = null;

	public AttachedProcess(){}
	public AttachedProcess(int pid, java.lang.String name, java.lang.String type, java.lang.String user, int lastSeqNum, int lastPollTime, int lastMsgTime, java.lang.String status, short stale_count)
	{
		this.pid = pid;
		this.name = name;
		this.type = type;
		this.user = user;
		this.lastSeqNum = lastSeqNum;
		this.lastPollTime = lastPollTime;
		this.lastMsgTime = lastMsgTime;
		this.status = status;
		this.stale_count = stale_count;
	}
}
