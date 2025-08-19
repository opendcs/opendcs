/*
* Where Applicable, Copyright 2025 OpenDCS Consortium and/or its contributors
* 
* Licensed under the Apache License, Version 2.0 (the "License"); you may not
* use this file except in compliance with the License. You may obtain a copy
* of the License at
* 
*   http://www.apache.org/licenses/LICENSE-2.0
* 
* Unless required by applicable law or agreed to in writing, software 
* distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
* License for the specific language governing permissions and limitations 
* under the License.
*/
package decodes.datasource;

import java.util.HashMap;
import java.util.Objects;
import java.util.Properties;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

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
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
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
	// TODO: This should really just be a service provider (though the use of the db will certainly cause some other issues.)
	public static PMParser getPMParser(String headerType)
		throws HeaderParseException
	{
		/** 
		 * If we get here and the database and enum list is null, something is very wrong.
		 * Be more drastic. Nothing downstream is likely to work anyways.
		 * 
		 */
		Objects.requireNonNull(Database.getDb(), "The database is not available and " +
											     " we're parsing a header. Check configuration.");
		Objects.requireNonNull(Database.getDb().enumList, "The database enum list is not available and " +
														  " we're parsing a header. Check configuration.");
		headerType = headerType.toLowerCase();
		log.trace("Constructing PMParser for headerType='{}'.", headerType);

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
					log.atWarn()
					   .setCause(ex)
					   .log("Cannot instantiate header parser from class name '{}'.", htev.getExecClassName());
				}
			}
		}
		
		if (headerType.startsWith("goes"))
			headerType = Constants.medium_Goes;

		if (headerType.equalsIgnoreCase(Constants.medium_Goes))
		{
			PMParser pmp = new GoesPMParser();
			return pmp;
		}
		else if (headerType.equalsIgnoreCase("iridium"))
		{
			return new IridiumPMParser();
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
		throw new HeaderParseException("Unknown header type '" + headerType + "'");
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

