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

import java.util.Properties;
import java.util.ServiceLoader;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

/**
Base class for all performance-measurements parsers.
Depending on how we retrieve the message, it may have a different kind of
header. A subclass of this class exists for each of the known header types.
A {@link PMParser.Provider} must also be created for each type.
*/
public interface PMParser
{
	static final Logger log = OpenDcsLoggerFactory.getLogger();

	/**
	  Factory method, returns concrete subclass for a given header type.
	  @return the PM Parser for the specified header type.
	  @throws HeaderParseException if type unknown.
	*/
	static PMParser getPMParser(String headerType) throws HeaderParseException
	{
		log.trace("Constructing PMParser for headerType='{}'.", headerType);
		var loaders = ServiceLoader.load(PMParser.Provider.class);
		for (var loader: loaders)
		{
			var parser = loader.create(headerType);
			if (parser != null)
			{
				return parser;
			}
		}

		headerType = headerType.toLowerCase();

		// Finally, if failure, cannot parse the header.
		throw new HeaderParseException("Unknown header type '" + headerType + "'");
	}

	/** @return a string describing this type of header, e.g. goes, vitel */
	String getHeaderType();

	/**
	  Returns one of the valid mediumType's in Constants.java,
	  used to determine how to handle the mediumID to make
	  the platform association.
	  @return the medium type constant.
	*/
	String getMediumType();

	/**
	 * Some PMPs might use properties to affect behavior.
	 * Default implementation of setProperties does nothing.
	 */
	default void setProperties(Properties routingSpecProps)
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
	void parsePerformanceMeasurements(RawMessage msg) throws HeaderParseException;

	/**
	  @return the header length for this type of data source.
	*/
	int getHeaderLength();


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
	default boolean containsExplicitLength()
	{
		return true;
	}

	/**
	 * Provider interface for service loader lookup.
	 * NOTE: The provider may return <b>any</b> PMParser type. For example
	 * see {@link TauPmParser} for one that creates a CsvPMParser with different
	 * defaults.
	 * Provider
	 */
	public static interface Provider
	{
		/**
		 * Parser instance if this provider
		 * matches the header type. null otherwise
		 * Generally providers should perform case-insensitive matching.
		 *
		 * type name for providers not built in to OpenDCS *SHOULD NOT*
		 * be plain. E.G. give the header a prefix. For example a custom
		 * USACE parser would be of type "usace.OurHeader"
		 * @return
		 */
		PMParser create(String headerType);
	}
}

