/*
*  $Id$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:21:00  cvs
*  Added legacy code to repository
*
*  Revision 1.5  2007/12/11 01:05:16  mmaloney
*  javadoc cleanup
*
*  Revision 1.4  2004/08/31 16:31:18  mjmaloney
*  javadoc
*
*  Revision 1.3  2004/08/24 23:52:46  mjmaloney
*  Added javadocs.
*
*  Revision 1.2  2003/12/07 20:36:49  mjmaloney
*  First working implementation of EDL time stamping.
*
*  Revision 1.1  2002/12/09 20:03:42  mjmaloney
*  Added
*
*/
package decodes.datasource;

import java.util.HashMap;
import java.util.Date;
import java.util.Calendar;
import java.text.ParsePosition;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import ilex.util.ArrayUtil;
import ilex.util.Logger;
import ilex.util.ByteUtil;
import ilex.var.Variable;

import decodes.db.Constants;

/**
  Concrete subclass of PMParser for parsing performance measurements
  out of GOES DCP messages.
*/
public class VaisalaPMParser extends GoesPMParser
{
	/** default constructor */
	public VaisalaPMParser()
	{
		super();
	}

	/**
	  Vaisala DRGS puts a platform description on the header line,
	  immediately following the 37-byte DOMSAT header.
	  We need to set the headerLength variable in 'msg' to point to
	  just past this added description.

	  @param msg The message to parse.
	*/
	public void parsePerformanceMeasurements(RawMessage msg)
		throws HeaderParseException
	{
		// Super class handles DOMSAT header, sets medium ID & length.
		super.parsePerformanceMeasurements(msg);

		byte data[] = msg.getData();
		int i = 0;
		for(; i<data.length && data[i] != (byte)'\n'; i++);

		if (i < data.length && data[i] == '\n')
		{
			Logger.instance().log(Logger.E_DEBUG3, 
				"Setting header length to " + i);
			msg.setHeaderLength(i);
		}
	}

	/** @return "vaisala" */
	public String getHeaderType()
	{
		return "vaisala";
	}

	/** @return constant medium type for this type of header. */
	public String getMediumType()
	{
		return Constants.medium_Goes;
	}
}

