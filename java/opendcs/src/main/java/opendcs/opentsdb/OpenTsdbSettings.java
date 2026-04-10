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
package opendcs.opentsdb;

import java.util.Properties;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import decodes.util.PropertiesOwner;
import decodes.util.PropertySpec;
import ilex.util.PropertiesUtil;

/**
 * Set from TSDB_PROPERTIES
 * @author mmaloney Mike Maloney, Cove Software LLC
 *
 */
public class OpenTsdbSettings implements PropertiesOwner
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	private static OpenTsdbSettings _instance = null;

	/** Allow the UTC Offset of time series to vary by an hour over DST change. */
	public boolean allowDstOffsetVariation = true;

	public OffsetErrorAction offsetErrorActionEnum = OffsetErrorAction.ROUND;

	/** Determines how time series with an offset error are handled. */
	public String offsetErrorAction = OffsetErrorAction.ROUND.name();

	/** Name of presentation group that determines storage units */
	public String storagePresentationGroup = "CWMS-English";

	public boolean traceConnections = false;

	public Properties props = new Properties();

	private static PropertySpec propSpecs[] =
	{
		new PropertySpec("allowDstOffsetVariation", PropertySpec.BOOLEAN,
			"(default=true) allows daylight time offset variation in time series data."),
		new PropertySpec("offsetErrorAction", PropertySpec.JAVA_ENUM+"opendcs.opentsdb.OffsetErrorAction",
			"Action when UTC Offset is detected when storing data. One of IGNORE, REJECT, ROUND."),
		new PropertySpec("storagePresentationGroup", PropertySpec.STRING,
			"Name of presentation group that determines storage units for each data type."),
		new PropertySpec("traceConnections", PropertySpec.BOOLEAN,
			"(default=false) Set to true to enable debugs on database connection management."),
	};

	public void setFromProperties(Properties props)
	{
		this.props = props;
		PropertiesUtil.loadFromProps(this, props);

		try
		{
			offsetErrorActionEnum = OffsetErrorAction.valueOf(offsetErrorAction);
		}
		catch(IllegalArgumentException ex)
		{
			log.atWarn()
			   .setCause(ex)
			   .log("OpenTsdbSettings: Bad offsetErrorAction '{}': defaulting to round.", offsetErrorAction);
			offsetErrorActionEnum = OffsetErrorAction.ROUND;
		}
	}

	public Properties getPropertiesSet()
	{
		Properties ret = new Properties();
		PropertiesUtil.copyProps(ret, props);

		ret.setProperty("allowDstOffsetVariation", "" + allowDstOffsetVariation);
		ret.setProperty("offsetErrorAction", offsetErrorAction);
		ret.setProperty("storagePresentationGroup", storagePresentationGroup);
		ret.setProperty("traceConnections", "" + traceConnections);
		return ret;
	}

	public static OpenTsdbSettings instance()
	{
		if (_instance == null)
			_instance = new OpenTsdbSettings();
		return _instance;
	}

	@Override
	public PropertySpec[] getSupportedProps()
	{
		return propSpecs;
	}

	@Override
	public boolean additionalPropsAllowed()
	{
		return true;
	}
}