package lrgs.apistatus;
public final class AttachedProcess
{
	public int pid;
	private String name;
	public String type;
	public String user;
	public int lastSeqNum;
	public int lastPollTime;
	public int lastMsgTime;
	public String status;
	public short stale_count;

	public String ddsVersion = null;

	public AttachedProcess(){}
	public AttachedProcess(int pid, String name, String type, String user, int lastSeqNum, 
		int lastPollTime, int lastMsgTime, String status, short stale_count)
	{
		this.pid = pid;
		this.setName(name);
		this.type = type;
		this.user = user;
		this.lastSeqNum = lastSeqNum;
		this.lastPollTime = lastPollTime;
		this.lastMsgTime = lastMsgTime;
		this.status = status;
		this.stale_count = stale_count;
	}
	public String getName()
	{
		return name;
	}
	public void setName(String name)
	{
		this.name = name;
	}
}
