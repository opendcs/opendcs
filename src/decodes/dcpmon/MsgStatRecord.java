/*
*  $Id$
*
*  $Log$
*  Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
*  OPENDCS 6.0 Initial Checkin
*
*  Revision 1.4  2008/11/11 00:49:19  mjmaloney
*  Bug fixes.
*
*  Revision 1.3  2008/09/18 00:52:46  mjmaloney
*  dev
*
*  Revision 1.2  2008/09/08 19:14:02  mjmaloney
*  LRGS 7 dev
*
*  Revision 1.1  2008/04/04 18:21:01  cvs
*  Added legacy code to repository
*
*  Revision 1.6  2007/12/04 14:28:35  mmaloney
*  added code to download channels from url
*
*  Revision 1.5  2007/06/26 14:21:34  mmaloney
*  Added a new field Agency, it comes from Pdt entry files
*
*  Revision 1.4  2006/12/23 18:15:51  mmaloney
*  DCP Monitor development
*
*  Revision 1.3  2004/10/21 13:39:20  mjmaloney
*  javadoc improvements.
*
*  Revision 1.2  2004/03/18 16:18:44  mjmaloney
*  Working server version beta 01
*
*  Revision 1.1  2004/02/29 20:48:25  mjmaloney
*  Alpha version of server complete.
*
*/
package decodes.dcpmon;

import lrgs.common.DcpAddress;

/**
This class holds one line's worth of data on the USACE Message Status
report. This is one days-worth of message specs for a particular DCP.
*/
public class MsgStatRecord
{
	/** The DCP Address */
	DcpAddress dcpAddress;

	/** The DCP Name */
	String dcpName;

	/** Agency name from PDT */
	String agency;
	
	/** The second of the day when the first transmit window starts. */
	int firstMsgSecOfDay;

	/** GOES channel for this message */
	int goesChannel;

	/** 24 Strings, containing the codes for each hour. */
	String fcodes[];

	/** The basin that this DCP belongs to. */
	String basin;

	boolean isMine;
	
	boolean isUnexpected = false;
	
	int daynum = 0;

	public MsgStatRecord(int daynum)
	{
		this.daynum = daynum;
		dcpAddress = null;
		dcpName = "";
		agency = "";
		firstMsgSecOfDay = -1;
		goesChannel = -1;
		fcodes = new String[24];
		for(int i=0; i<24; i++)
			fcodes[i] = ".";
		basin = "-";
		isMine = false;
	}

	/**
	  Sets the failure code for a particular hour.
	  @param hour 0...23
	  @param codes String containing one or more failure codes.
	*/
	public void setCodes(int hour, String codes)
	{
		fcodes[hour] = codes;
	}
}
