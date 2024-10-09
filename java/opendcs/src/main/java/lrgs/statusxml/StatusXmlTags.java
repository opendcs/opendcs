/*
*  $Id$
*
*  $Log$
*  Revision 1.3  2010/01/07 21:47:53  shweta
*  Enhancements for multiple DDS Receive  group.
*
*  Revision 1.2  2008/09/24 13:59:01  mjmaloney
*  network DCPs
*
*  Revision 1.1  2008/04/04 18:21:16  cvs
*  Added legacy code to repository
*
*  Revision 1.7  2008/01/14 14:57:36  mmaloney
*  dev
*
*  Revision 1.6  2007/08/08 17:46:40  mmaloney
*  *** empty log message ***
*
*  Revision 1.5  2005/10/20 18:01:07  mmaloney
*  event nums
*
*  Revision 1.4  2005/07/28 20:22:14  mjmaloney
*  LRGS Monitor backward compatibility with LRGS 4.0.
*
*  Revision 1.3  2005/06/30 15:15:29  mjmaloney
*  Java Archive Development.
*
*  Revision 1.2  2004/09/02 13:09:06  mjmaloney
*  javadoc
*
*  Revision 1.1  2004/05/04 18:03:58  mjmaloney
*  Moved from statusgui package to here.
*
*/
package lrgs.statusxml;

/**
This class contains the public static final String tags found in the
XML data. They are defined here so they can be referenced consistently
from different places in the code.
<p>
In each case the Java variable name is the same as the String element name.
*/
public class StatusXmlTags
{
	public static final String LrgsStatusSnapshot = "LrgsStatusSnapshot";
	public static final String systemStatus = "systemStatus";
	public static final String isUsable = "isUsable";
	public static final String SystemTime = "SystemTime";
	public static final String MaxClients = "MaxClients";
	public static final String CurrentNumClients = "CurrentNumClients";
	public static final String ArchiveStatistics = "ArchiveStatistics";
	public static final String dirOldest = "dirOldest";
	public static final String dirNext = "dirNext";
	public static final String dirWrap = "dirWrap";
	public static final String oldestOffset = "oldestOffset";
	public static final String oldestMsgTime = "oldestMsgTime";
	public static final String lastSeqNum = "lastSeqNum";
	public static final String maxMessages = "maxMessages";
	public static final String maxBytes = "maxBytes";
	public static final String Process = "Process";
	public static final String name = "name";
	public static final String type = "type";
	public static final String user = "user";
	public static final String status = "status";
	public static final String LastSeqNum = "LastSeqNum";
	public static final String LastPollTime = "LastPollTime";
	public static final String LastMsgTime = "LastMsgTime";
	public static final String MaxDownlinks = "MaxDownlinks";
	public static final String DownLink = "DownLink";
	public static final String StatusCode = "StatusCode";
	public static final String LastMsgRecvTime = "LastMsgRecvTime";
	public static final String Quality = "Quality";
	public static final String numGood = "numGood";
	public static final String numDropped = "numDropped";
	public static final String numRecovered = "numRecovered";
	public static final String hostname = "hostname";
	public static final String BER = "BER";
	public static final String dirSize = "dirSize";
	public static final String staleCount = "staleCount";
	public static final String majorVersion = "majorVersion";
	public static final String minorVersion = "minorVersion";
	public static final String domsatDropped = "domsatDropped";
	public static final String ddsVersion = "ddsVersion";
	public static final String fullVersion = "fullVersion";
	
	public static final String networkDcpList = "NetworkDcpList";
	public static final String networkDcp = "NetworkDcp";
	public static final String host = "host";
	public static final String port = "port";
	public static final String displayName = "DisplayName";
	public static final String pollingMinutes = "PollingMinutes";
	public static final String lastPollAttempt = "LastPollAttempt";
	public static final String lastContact = "LastContact";
	public static final String numGoodPolls = "NumGoodPolls";
	public static final String numFailedPolls = "NumFailedPolls";
	public static final String numMessages = "NumMessages";
	public static final String group = "group";
}
