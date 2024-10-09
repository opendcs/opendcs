/*
*  $Id$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:21:16  cvs
*  Added legacy code to repository
*
*  Revision 1.2  2004/05/21 18:27:45  mjmaloney
*  Release prep.
*
*  Revision 1.1  2004/05/11 20:46:26  mjmaloney
*  LQM Impl
*
*/
package lritdcs;

/**
Objects of this type are stored on the pending queue.
*/
public class SentFile
{
	/// Filename that was sent.
	String filename;

	/// System time when file will expire & need to be retransmitted
	long expireTime;

	public SentFile(String name, long tm)
	{
		filename = name;
		expireTime = tm;
	}
}
