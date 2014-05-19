/*
*  $Id$
*
*  This is open-source software written by ILEX Engineering, Inc., under
*  contract to the federal government. You are free to copy and use this
*  source code for your own purposes, except that no part of the information
*  contained in this file may be claimed to be proprietary.
*
*  Except for specific contractual terms between ILEX and the federal 
*  government, this source code is provided completely without warranty.
*  For more information contact: info@ilexeng.com
*
*  $Log$
*  Revision 1.3  2009/04/07 14:17:05  mjmaloney
*  Save Iridium Quality stats in quality.log
*
*  Revision 1.2  2008/05/21 21:10:33  cvs
*  dev
*
*  Revision 1.1  2008/04/04 18:21:11  cvs
*  Added legacy code to repository
*
*  Revision 1.3  2006/06/28 16:00:44  mmaloney
*  Bug fixes.
*
*  Revision 1.2  2005/07/20 20:18:55  mjmaloney
*  LRGS 5.0 Release preparation
*
*  Revision 1.1  2005/07/14 21:08:30  mjmaloney
*  dev
*
*/
package lrgs.archive;

import java.util.Date;

/**
This class represents an entry (line) in the qual_log LRGS file.
Each entry represents data for a minute.
*/
public class QualLogEntry
{
	/** Date/time of the start of the minute for this entry. */
	public Date timeStamp;

	/** # msgs good on DOMSAT */
	public int domsatGood;

	/** # msgs with parity errors on DOMSAT */
	public int domsatErr;

	/** DOMSAT Average BER for period */
	public String aveDomsatBER;

	/** DOMSAT Maximum BER for period */
	public String maxDomsatBER;

	/** # msgs good on DRGS */
	public int drgsGood;

	/** # msgs with parity errors on DRGS */
	public int drgsErr;

	/** # msgs good on DDSRecv */
	public int ddsGood;

	/** # msgs with parity errors on DDSRecv */
	public int ddsErr;

	/** # msgs good on NOAAPORT */
	public int noaaportGood;

	/** # msgs with parity errors on NOAAPORT */
	public int noaaportErr;

	/** # msgs good on LRIT */
	public int lritGood;

	/** # msgs with parity errors on LRIT */
	public int lritErr;

	/** # msgs good on Legacy network backup */
	public int netbackGood;

	/** # msgs with parity errors on Legacy network backup */
	public int netbackErr;

	/** # msgs good archived */
	public int archivedGood;

	/** # msgs with parity errors archived */
	public int archivedErr;

	/** # msgs dropped by DOMSAT (sequence errors) */
	public int domsatDropped;

	/** # msgs received on Vitel GR3110 interface */
	public int gr3110Count;
	
	/** # messages received on iridium interface */
	public int iridiumCount;

	/** Constructor. */
	public QualLogEntry()
	{
		long x = System.currentTimeMillis();
		x = (x / 60000L) * 60000L;
		timeStamp = new Date(x);
		domsatGood = 0;
		domsatErr = 0;
		aveDomsatBER = "-";
		maxDomsatBER = "-";
		drgsGood = 0;
		drgsErr = 0;
		ddsGood = 0;
		ddsErr = 0;
		noaaportGood = 0;
		noaaportErr = 0;
		lritGood = 0;
		lritErr = 0;
		archivedGood = 0;
		archivedErr = 0;
		domsatDropped = 0;
		gr3110Count = 0;
		iridiumCount = 0;
	}
}
