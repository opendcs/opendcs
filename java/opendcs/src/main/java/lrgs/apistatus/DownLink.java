package lrgs.apistatus;
public final class DownLink
{
	public java.lang.String name;
	public short type;
	public boolean hasBER;
	public boolean hasSeqNum;
	public short statusCode;
	public int lastMsgRecvTime;
	public int lastSeqNum;
	public java.lang.String BER;
	public java.lang.String statusString;
	public String group=""; //DDS Recieve group
	public DownLink(){}
	public DownLink(java.lang.String name, short type, boolean hasBER, boolean hasSeqNum, short statusCode, int lastMsgRecvTime, int lastSeqNum, java.lang.String BER, java.lang.String statusString)
	{
		this.name = name;
		this.type = type;
		this.hasBER = hasBER;
		this.hasSeqNum = hasSeqNum;
		this.statusCode = statusCode;
		this.lastMsgRecvTime = lastMsgRecvTime;
		this.lastSeqNum = lastSeqNum;
		this.BER = BER;
		this.statusString = statusString;
		group= ""; 
	}
}
