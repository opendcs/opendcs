/*
*  $Id$
*
*  $State$
*
*  $Log$
*  Revision 1.4  2017/08/22 19:33:46  mmaloney
*  Refactor
*
*  Revision 1.3  2016/10/07 14:49:24  mmaloney
*  Updates for Web Report for Gail Monds, LRD.
*
*  Revision 1.2  2014/10/02 14:31:05  mmaloney
*  Encapsulated execClassName
*
*  Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
*  OPENDCS 6.0 Initial Checkin
*
*  Revision 1.9  2012/04/09 15:28:28  mmaloney
*  Added EUMETSAT medium type.
*
*  Revision 1.8  2012/02/16 20:42:07  mmaloney
*  Added support for SutronLoggerCsvPMParser
*
*  Revision 1.7  2011/11/29 16:05:14  mmaloney
*  Added MetarPMParser
*
*  Revision 1.6  2011/09/27 01:23:08  mmaloney
*  Enhancements to StreamDataSource for SHEF and NOS Decoding.
*
*  Revision 1.5  2010/06/15 16:06:48  mjmaloney
*  Updates for Alberta Env
*
*  Revision 1.4  2008/11/20 18:49:18  mjmaloney
*  merge from usgs mods
*
*  Revision 1.1  2008/11/15 01:03:12  mmaloney
*  Moved from separate trees to common parent
*
*  Revision 1.12  2008/09/30 01:09:43  satin
*  *** empty log message ***
*
*  Revision 1.11  2004/08/24 23:52:45  mjmaloney
*  Added javadocs.
*
*  Revision 1.10  2003/12/12 17:55:33  mjmaloney
*  Working implementation of DirectoryDataSource.
*
*  Revision 1.9  2003/12/07 20:36:48  mjmaloney
*  First working implementation of EDL time stamping.
*
*  Revision 1.8  2003/06/17 00:34:00  mjmaloney
*  StreamDataSource implemented.
*  FileDataSource re-implemented as a subclass of StreamDataSource.
*
*  Revision 1.7  2002/12/08 20:21:01  mjmaloney
*  Updates
*
*  Revision 1.6  2002/10/11 01:27:01  mjmaloney
*  Added SocketStreamDataSource and NoaaportPMParser stuff.
*
*  Revision 1.5  2002/06/03 15:38:59  mjmaloney
*  DR fixes.
*
*  Revision 1.4  2002/06/03 00:54:43  mjmaloney
*  dev
*
*  Revision 1.3  2002/05/21 20:50:10  mjmaloney
*  dev
*
*  Revision 1.2  2001/11/26 22:22:42  mike
*  Call hard-coded GOES PM Parser from LrgsDataSource.
*  Skip DAPS Status messages in LrgsDataSource.
*
*  Revision 1.1  2001/08/24 19:31:41  mike
*  Moved PMParser stuff to datasource package.
*  Added reference in RawMessage to performance measurements.
*  Created FileDataSource.
*
*/
package decodes.datasource;

import ilex.util.Logger;

import java.util.HashMap;
import java.util.Properties;

import decodes.datasource.RawMessage;
import decodes.db.Constants;
import decodes.db.Database;
import decodes.db.DbEnum;
import decodes.db.EnumValue;

/**
Base class for all performance-measurements parsers.
Depending on how we retrieve the message, it may have a different kind of
header. A subclass of this class exists for each of the known header types.
*/
public abstract class PMParser
{
	private static HashMap parsers = new HashMap();

	/** default constructor */
	protected PMParser()
	{
	}

	/**
	  Factory method, returns concrete subclass for a given header type.
	  @return the PM Parser for the specified header type.
	  @throws HeaderParseException if type unknown.
	*/
	public static PMParser getPMParser(String headerType)
		throws HeaderParseException
	{
		headerType = headerType.toLowerCase();
		Logger.instance().debug3("Constructing PMParser for headerType='" + headerType + "'");

		// First use enum to lookup headerType, retrieve class
		// name. Then instantiate the class.
		DbEnum tmTypeEnum = Database.getDb().enumList.getEnum(Constants.enum_TMType);
		if (tmTypeEnum != null)
		{
			EnumValue htev = tmTypeEnum.findEnumValue(headerType);
			if (htev != null && htev.getExecClassName() != null)
			{
				try
				{
					@SuppressWarnings("rawtypes")
					Class pmpClass = htev.getExecClass();
					return (PMParser)pmpClass.newInstance();
				}
				catch (Exception ex)
				{
					Logger.instance().warning("Cannot instantiate header parser "
						+ "from class name '" + htev.getExecClassName() + "': " + ex);
				}
			}
		}
		
		if (headerType.startsWith("goes"))
			headerType = Constants.medium_Goes;

		if (headerType.equalsIgnoreCase(Constants.medium_Goes))
		{
			PMParser pmp = new GoesPMParser();
//			parsers.put("goes", pmp);
			return pmp;
		}
		else if (headerType.equalsIgnoreCase("vitel"))
		{
			return new VitelDrgsPMParser();
		}
		else if (headerType.equalsIgnoreCase("noaaport"))
		{
			return new NoaaportPMParser();
		}
		else if (headerType.equalsIgnoreCase("vaisala"))
		{
			return new VaisalaPMParser();
		}
		else if (headerType.equalsIgnoreCase("edl")
		      || headerType.equalsIgnoreCase("data-logger") )
		{
			return new EdlPMParser();
		}
		else if (headerType.equalsIgnoreCase("noHeader")
		      || headerType.equalsIgnoreCase("other") )
		{
			return new NoHeaderPMParser();
		}
		else if (headerType.equalsIgnoreCase("mbfire"))
		{
			return new MBFirePMParser();
		}
		else if (headerType.equalsIgnoreCase("modem") )
		{
			return new ModemPMParser();
		}
		else if (headerType.equalsIgnoreCase("radio") )
		{
			return new RadioPMParser();
		}
		else if (headerType.equalsIgnoreCase("csv") )
		{
			return new CsvPMParser();
		}
		else if (headerType.equalsIgnoreCase("shef") )
		{
			return new ShefPMParser();
		}
		else if (headerType.equalsIgnoreCase("metar") )
		{
			return new MetarPMParser();
		}
		else if (headerType.equalsIgnoreCase("eumetsat"))
		{
			return new EumetsatPMParser();
		}
		else if (headerType.equalsIgnoreCase("tau") )
		{
			CsvPMParser ret = new CsvPMParser();
			ret.idcol = 1;
			ret.datetimecol=2;
			ret.datetimefmt = "dd-MMM-yyyy   ,HH:mm:ss";
			ret.timezone = "MST";
			ret.datacol = 4;
			ret.delim = ",";
//			ret.mediumType = Constants.medium_EDL;
			ret.headerType = "tau";
			ret.mediumType = "tau";
			ret.setProperties(new Properties());
			return ret;
		}
		else if (headerType.equalsIgnoreCase("idstart"))
		{
			IdStartPMParser ret = new IdStartPMParser();
			return ret;
		}


		Object obj = parsers.get(headerType);
		if (obj != null)
			return (PMParser)obj;

		// Finally, if failure, cannot parse the header.
		throw new HeaderParseException("Unknown header type '"
			+ headerType + "'");
	}

	/** @return a string describing this type of header, e.g. goes, vitel */
	public abstract String getHeaderType();

	/** 
	  Returns one of the valid mediumType's in Constants.java,
	  used to determine how to handle the mediumID to make
	  the platform association.
	  @return the medium type constant.
	*/
	public abstract String getMediumType();
	
	/**
	 * Some PMPs might use properties to affect behavior.
	 * Default implementation of setProperties does nothing.
	 */
	public void setProperties(Properties routingSpecProps)
	{
		// Default implementation does nothing.
	}

	/**
	  Parses performance measurements from raw message and populates
	  a hashmap (string - Variable) table of results.
	  <p>
	  Concrete sub classes must set the MESSAGE_LENGTH, the mediumId,
	  and the MESSAGE_TIME in the raw message.
	  @param msg the message to parse.
	*/
	public abstract void parsePerformanceMeasurements(RawMessage msg)
		throws HeaderParseException;

	/**
	  @return the header length for this type of data source.
	*/
	public abstract int getHeaderLength();


	/**
	  Most header types contain an explicit length field, so this method
	  defaults to true. Some formats (e.g. NOAAPORT) must derive the message
	  length via delimiters.
	  <p>
	  The real purpose of this variable is to determine if an end-delimiter
	  is needed in a stream of messages of this type. Headers that contain an
	  explicit length field do not require end-delimiters.
	  @return flag
	*/
	public boolean containsExplicitLength()
	{
		return true;
	}
}

