/*
 *  Copyright 2025 OpenDCS Consortium and its Contributors
 *
 *  Licensed under the Apache License, Version 2.0 (the "License")
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.opendcs.odcsapi.opendcs_dep;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import javax.servlet.http.HttpServletResponse;

import decodes.consumer.StringBufferConsumer;
import decodes.cwms.rating.CwmsRatingSingleIndep;
import decodes.datasource.FtpDataSource;
import decodes.datasource.WebDirectoryDataSource;
import decodes.db.ConfigSensor;
import decodes.db.DataSource;
import decodes.db.Database;
import decodes.dbeditor.RoutingSpecEditPanel;
import decodes.tsdb.algo.PythonAlgorithm;
import decodes.util.PropertiesOwner;
import decodes.util.PropertySpec;
import org.opendcs.odcsapi.beans.ApiPropSpec;
import org.opendcs.odcsapi.errorhandling.WebAppException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PropSpecHelper
{
	private static final Logger LOGGER = LoggerFactory.getLogger(PropSpecHelper.class);

	public enum ClassName
	{
		AREA_RATING_COMP_RESOLVER("decodes.comp.AreaRatingCompResolver"),
		RDB_RATING_COMP_RESOLVER("decodes.comp.RdbRatingCompResolver"),
		STATION_EXCLUDE_COMP_RESOLVER("decodes.comp.StationExcludeCompResolver"),
		TAB_RATING_COMP_RESOLVER("decodes.comp.TabRatingCompResolver"),
		DIRECTORY_CONSUMER("decodes.consumer.DirectoryConsumer"),
		FILE_APPEND_CONSUMER("decodes.consumer.FileAppendConsumer"),
		FILE_CONSUMER("decodes.consumer.FileConsumer"),
		PIPE_CONSUMER("decodes.consumer.PipeConsumer"),
		STRING_BUFFER_CONSUMER("decodes.consumer.StringBufferConsumer"),
		TCP_CLIENT_CONSUMER("decodes.consumer.TcpClientConsumer"),
		ALBERTA_LOADER_FORMATTER("decodes.consumer.AlbertaLoaderFormatter"),
		CSV_FORMATTER("covesw.azul.consumer.CsvFormatter"),
		EMIT_ASCII_FORMATTER("decodes.consumer.EmitAsciiFormatter"),
		EMIT_ORACLE_FORMATTER("decodes.consumer.EmitOracleFormatter"),
		HEADER_FORMATTER("decodes.consumer.HeaderFormatter"),
		HTML_FORMATTER("decodes.consumer.HtmlFormatter"),
		HUMAN_READABLE_FORMATTER("decodes.consumer.HumanReadableFormatter"),
		HYDRO_JSON_FORMATTER("decodes.consumer.HydroJSONFormatter"),
		KHYDSTRA_FORMATTER("decodes.consumer.KHydstraFormatter"),
		KISTERS_FORMATTER("decodes.consumer.KistersFormatter"),
		NULL_FORMATTER("decodes.consumer.NullFormatter"),
		RAW_FORMATTER("decodes.consumer.RawFormatter"),
		SHEF_FORMATTER("decodes.consumer.ShefFormatter"),
		SHEFIT_FORMATTER("decodes.consumer.ShefitFormatter"),
		TRANSMIT_MONITOR_FORMATTER("decodes.consumer.TransmitMonitorFormatter"),
		TS_IMPORT_FORMATTER("decodes.consumer.TsImportFormatter"),
		CWMS_CONSUMER("decodes.cwms.CwmsConsumer"),
		DIRECTORY_DATA_SOURCE("decodes.datasource.DirectoryDataSource"),
		FTP_DATA_SOURCE("decodes.datasource.FtpDataSource"),
		HOT_BACKUP_GROUP("decodes.datasource.HotBackupGroup"),
		LRGS_DATA_SOURCE("decodes.datasource.LrgsDataSource"),
		ROUND_ROBIN_GROUP("decodes.datasource.RoundRobinGroup"),
		SCP_DATA_SOURCE("decodes.datasource.ScpDataSource"),
		SOCKET_STREAM_DATA_SOURCE("decodes.datasource.SocketStreamDataSource"),
		USGS_WEB_DATA_SOURCE("decodes.datasource.UsgsWebDataSource"),
		WEB_ABSTRACT_DATA_SOURCE("decodes.datasource.WebAbstractDataSource"),
		WEB_DATA_SOURCE("decodes.datasource.WebDataSource"),
		WEB_DIRECTORY_DATA_SOURCE("decodes.datasource.WebDirectoryDataSource"),
		POLLING_DATA_SOURCE("decodes.polling.PollingDataSource"),
		PLATFORM("decodes.db.Platform"),
		PLATFORM_SENSOR("decodes.db.PlatformSensor"),
		CONFIG_SENSOR("decodes.db.ConfigSensor"),
		ROUTING_SCHEDULER("decodes.routing.RoutingScheduler"),
		COMP_APP_INFO("decodes.tsdb.CompAppInfo"),
		HDB_RATING("decodes.tsdb.algo.HdbRating"),
		RUNNING_AVERAGE_ALGORITHM("decodes.tsdb.algo.RunningAverageAlgorithm"),
		AVERAGE_ALGORITHM("decodes.tsdb.algo.AverageAlgorithm"),
		BIG_ADDER("decodes.tsdb.algo.BigAdder"),
		MULTIPLICATION("decodes.tsdb.algo.Multiplication"),
		SUM_OVER_TIME_ALGORITHM("decodes.tsdb.algo.SumOverTimeAlgorithm"),
		EXPRESSION_PARSER_ALGORITHM("decodes.tsdb.algo.ExpressionParserAlgorithm"),
		FILL_FORWARD("decodes.tsdb.algo.FillForward"),
		HDB_RESERVOIR_MASS_BALANCE("decodes.tsdb.algo.HdbReservoirMassBalance"),
		ADD_TO_PREVIOUS("decodes.tsdb.algo.AddToPrevious"),
		STAT("decodes.tsdb.algo.Stat"),
		HDB_EVAPORATION("decodes.tsdb.algo.HdbEvaporation"),
		ESTIMATED_INFLOW("decodes.tsdb.algo.EstimatedInflow"),
		GROUP_ADDER("decodes.tsdb.algo.GroupAdder"),
		RESERVOIR_FULL("decodes.tsdb.algo.ReservoirFull"),
		SCALER_ADDER("decodes.tsdb.algo.ScalerAdder"),
		HDB_ACAPS_RATING("decodes.tsdb.algo.HdbACAPSRating"),
		CHOOSE_ONE("decodes.tsdb.algo.ChooseOne"),
		BRIDGE_CLEARANCE("decodes.tsdb.algo.BridgeClearance"),
		USGS_EQUATION("decodes.tsdb.algo.UsgsEquation"),
		INCREMENTAL_PRECIP("decodes.tsdb.algo.IncrementalPrecip"),
		FLOW_RES_IN("decodes.tsdb.algo.FlowResIn"),
		COPY_ALGORITHM("decodes.tsdb.algo.CopyAlgorithm"),
		DIS_AGGREGATE("decodes.tsdb.algo.DisAggregate"),
		WEIGHTED_WATER_TEMPERATURE("decodes.tsdb.algo.WeightedWaterTemperature"),
		PYTHON_ALGORITHM("decodes.tsdb.algo.PythonAlgorithm"),
		RESAMPLE("decodes.tsdb.algo.Resample"),
		SHOW_ALGO_PROPS("decodes.tsdb.algo.ShowAlgoProps"),
		PERIOD_TO_DATE("decodes.tsdb.algo.PeriodToDate"),
		COPY_NO_OVERWRITE("decodes.tsdb.algo.CopyNoOverwrite"),
		SUB_SAMPLE("decodes.tsdb.algo.SubSample"),
		CENTRAL_RUNNING_AVERAGE_ALGORITHM("decodes.tsdb.algo.CentralRunningAverageAlgorithm"),
		TAB_RATING("decodes.tsdb.algo.TabRating"),
		RDB_RATING("decodes.tsdb.algo.RdbRating"),
		DIVISION("decodes.tsdb.algo.Division"),
		VIRTUAL_GAGE("decodes.tsdb.algo.VirtualGage"),
		ALARM_SCREENING_ALGORITHM("decodes.tsdb.alarm.AlarmScreeningAlgorithm"),
		INFLOW_ADVANCED_ALG("decodes.hdb.algo.InflowAdvancedAlg"),
		EQUATION_SOLVER_ALG("decodes.hdb.algo.EquationSolverAlg"),
		EST_GLDA_INFLOW("decodes.hdb.algo.EstGLDAInflow"),
		BEGIN_OF_PERIOD_ALG("decodes.hdb.algo.BeginofPeriodAlg"),
		DYNAMIC_AGGREGATES_ALG("decodes.hdb.algo.DynamicAggregatesAlg"),
		HDB_SHIFT_RATING("decodes.hdb.algo.HdbShiftRating"),
		HDB_LOOKUP_TIME_SHIFT_RATING("decodes.hdb.algo.HdbLookupTimeShiftRating"),
		SIDE_INFLOW_ALG("decodes.hdb.algo.SideInflowAlg"),
		VOLUME_TO_FLOW_ALG("decodes.hdb.algo.VolumeToFlowAlg"),
		NVRN_UNREG("decodes.hdb.algo.NVRNUnreg"),
		POWER_TO_ENERGY_ALG("decodes.hdb.algo.PowerToEnergyAlg"),
		GLDA_UNREG("decodes.hdb.algo.GLDAUnreg"),
		CALL_PROC_ALG("decodes.hdb.algo.CallProcAlg"),
		END_OF_PERIOD_ALG("decodes.hdb.algo.EndofPeriodAlg"),
		GLDA_EVAP("decodes.hdb.algo.GLDAEvap"),
		BMDC_UNREG("decodes.hdb.algo.BMDCUnreg"),
		CRRC_UNREG("decodes.hdb.algo.CRRCUnreg"),
		PARSHALL_FLUME("decodes.hdb.algo.ParshallFlume"),
		FLOW_TO_VOLUME_ALG("decodes.hdb.algo.FlowToVolumeAlg"),
		SIMPLE_DISAGG_ALG("decodes.hdb.algo.SimpleDisaggAlg"),
		EOP_INTERP_ALG("decodes.hdb.algo.EOPInterpAlg"),
		TIME_WEIGHTED_AVERAGE_ALG("decodes.hdb.algo.TimeWeightedAverageAlg"),
		INFLOW_BASIC_ALG("decodes.hdb.algo.InflowBasicAlg"),
		MPRC_UNREG("decodes.hdb.algo.MPRCUnreg"),
		GLEN_DELTA_BSMB_ALG("decodes.hdb.algo.GlenDeltaBSMBAlg"),
		FLGU_UNREG("decodes.hdb.algo.FLGUUnreg"),
		CWMS_RATING_SINGLE_INDEP("decodes.cwms.rating.CwmsRatingSingleIndep"),
		CWMS_RATING_MULT_INDEP("decodes.cwms.rating.CwmsRatingMultIndep"),
		SCREENING_ALGORITHM("decodes.cwms.validation.ScreeningAlgorithm"),
		DECODES_SETTINGS("decodes.util.DecodesSettings"),
		LRGS_CONFIG("lrgs.lrgsmain.LrgsConfig"),
		OPEN_TSDB_SETTINGS("opendcs.opentsdb.OpenTsdbSettings");

		private final String className;

		ClassName(String className)
		{
			this.className = className;
		}

		@Override
		public String toString()
		{
			return className;
		}
	}

	private PropSpecHelper()
	{
		throw new AssertionError("Utility class");
	}


	public static ApiPropSpec[] getPropSpecs(String className)
			throws WebAppException
	{
		PropertySpec[] ps = getDecodesPropSpecs(className);
		ApiPropSpec[] ret = new ApiPropSpec[ps.length];
		for(int i = 0; i < ps.length; i++)
			ret[i] = new ApiPropSpec(ps[i].getName(), ps[i].getType(), ps[i].getDescription());
		return ret;
	}


	private static PropertySpec[] getDecodesPropSpecs(String className) throws WebAppException
	{
		//className is user controlled, so it is logged at trace level.
		LOGGER.trace("PropSpecHelper.getPropSpecs class='{}'", className);
		if (FtpDataSource.class.getName().equalsIgnoreCase(className))
		{
			// Can't instantiate the class because it requires Apache FTP libs.
			// Kludge: copy the specs here.
			return new PropertySpec[]{
				new PropertySpec("host", PropertySpec.HOSTNAME,
					"FTP Data Source: Host name or IP Address of FTP Server"),
				new PropertySpec("port", PropertySpec.INT,
					"FTP Data Source: Listening port on FTP Server (default = 21)"),
				new PropertySpec("username", PropertySpec.STRING,
					"FTP Data Source: User name with which to connect to FTP server"),
				new PropertySpec("password", PropertySpec.STRING,
					"FTP Data Source: Password on the FTP server"),
				new PropertySpec("remoteDir", PropertySpec.STRING,
					"FTP Data Source: remote directory - blank means root"),
				new PropertySpec("localDir", PropertySpec.DIRECTORY,
					"FTP Data Source: optional local directory in which to store downloaded"
					+ " file. If not supplied, received file is processed by DECODES but not"
					+ " saved locally."),
				new PropertySpec("filenames", PropertySpec.STRING,
					"Space-separated list of files to download from server"),
				new PropertySpec("xferMode",
					PropertySpec.JAVA_ENUM + "decodes.datasource.FtpMode",
					"FTP Data Source: FTP transfer mode"),
				new PropertySpec("deleteFromServer", PropertySpec.BOOLEAN,
					"FTP Data Source: (default=false) Set to true to delete file from server "
					+ "after retrieval. (May be disallowed on some servers.)"),
				new PropertySpec("ftpActiveMode", PropertySpec.BOOLEAN,
					"FTP Data Source: (default=false for passive mode) Set to true to " +
					"use FTP active mode."),
				new PropertySpec("nameIsMediumId", PropertySpec.BOOLEAN,
					"Use with OneMessageFile=true if the downloaded filename is to be treated as a medium ID"
					+ " in order to link this data with a platform."),
				new PropertySpec("ftps", PropertySpec.BOOLEAN, "(default=false) Use Secure FTP."),
				new PropertySpec("newerThan", PropertySpec.STRING,
					"Either a Date/Time in the format [[[CC]YY] DDD] HH:MM[:SS], "
					+ "or a string of the form 'now - N incr',"
					+ " where N is an integer and incr is minutes, hours, or days."),

			};
		}
		else if (WebDirectoryDataSource.class.getName().equalsIgnoreCase(className))
		{
			return new PropertySpec[]{
				new PropertySpec("directoryUrl", PropertySpec.STRING,
					"(required) URL of the directory that lists the file names containing messages"),
				new PropertySpec("urlFieldDelimiter", PropertySpec.STRING,
					"(default = underscore) Delimiter for fields within the filenames."),
				new PropertySpec("urlTimePos", PropertySpec.INT,
					"(default=3) Position of time within the delimited file name (1=first pos)"),
				new PropertySpec("urlIdPos", PropertySpec.INT,
					"(default=5) Position of the transport medium ID within the file name (1=first pos)"),
				new PropertySpec("urlTimeFormat", PropertySpec.STRING,
						"(default = HHmmss) SimpleDateFormat format string for time in the filename"),
				new PropertySpec("urlTimeZone", PropertySpec.STRING,
						"(default = UTC) Time Zone for the time within the directory and file names")
			};

		}
		else if (RoutingSpecEditPanel.class.getName().equalsIgnoreCase(className))
		{
			// Properties implemented directly by RoutingSpecThread:
			return new PropertySpec[]{
				// Properties implemented directly by RoutingSpecThread:
				new PropertySpec("noLimits", PropertySpec.BOOLEAN,
					"Do NOT Apply Sensor min/max limits."),
				new PropertySpec("removeRedundantData", PropertySpec.BOOLEAN,
					"Remove Redundant DCP Message Data."),
				new PropertySpec("compConfig", PropertySpec.FILENAME,
					"Name of in-line computations config file"),
				new PropertySpec("usgsSummaryFile", PropertySpec.FILENAME,
					"Optional USGS-Format Summary File"),
				new PropertySpec("RawArchivePath", PropertySpec.STRING,
					"Path to raw archive file. Defining this turns on the raw-archive function. " +
					"Example: $DCSTOOL_HOME/raw-archive/fts/$DATE(yyMMdd).fts"),
				new PropertySpec("RawArchiveStartDelim", PropertySpec.STRING,
					"String placed before each message in the file"),
				new PropertySpec("RawArchiveEndDelim", PropertySpec.STRING,
					"String placed after each message in the file"),
				new PropertySpec("RawArchiveMaxAge", PropertySpec.STRING,
					"Example: '1 year'. Files older than this are deleted."),
				new PropertySpec("debugLevel", PropertySpec.INT,
					"(default=0) Set to 1, 2, 3 for increasing levels of debug information" +
					" when this routing spec is run."),
				new PropertySpec("updatePlatformStatus", PropertySpec.BOOLEAN,
					"(default=true) set to false to NOT update platform status records as messages are processed."),
				new PropertySpec("purgeOldEvents", PropertySpec.BOOLEAN,
					"(default=true) Set to false to tell this routing spec to NOT attempt to "
					+ "purge expired events from the database. Also see DecodesSettings.eventPurgeDays")
			};
		}
		else if (PythonAlgorithm.class.getName().equalsIgnoreCase(className))
		{
			return new PropertySpec[0];
		}
		else if (CwmsRatingSingleIndep.class.getName().equalsIgnoreCase(className))
		{
			return new PropertySpec[]{
				new PropertySpec("templateVersion", PropertySpec.STRING,
					"(default=USGS-EXSA) Used as the version part of the rating template string"),
				new PropertySpec("specVersion", PropertySpec.STRING,
					"(default=Production) Used as the version part of the rating spec."),
				new PropertySpec("useDepLocation", PropertySpec.BOOLEAN,
					"(default=false) false means use location from first INdep param. "
					+ "True means use location from DEP param.")
			};
		}
		return getPropertySpecsReflect(className);
	}

	private static PropertySpec[] getPropertySpecsReflect(String className) throws WebAppException
	{
		// The above special cases failed. Try to instantiate an object and ask it for
		// its prop specs.
		PropertiesOwner propertiesOwner = null;
		try
		{
			if(ConfigSensor.class.getName().equalsIgnoreCase(className))
			{
				propertiesOwner = new ConfigSensor(null, 0);
			}
			else if(StringBufferConsumer.class.getName().equalsIgnoreCase(className))
			{
				propertiesOwner = new StringBufferConsumer(new StringBuffer());
			}
			else
			{
				Class<?> c = Class.forName(className);
				Constructor<?>[] constructors = c.getConstructors();
				for(Constructor<?> ctor : constructors)
				{
					if(ctor.getParameterCount() == 0)
					{

						propertiesOwner = (PropertiesOwner) ctor.newInstance();
					}
					else if(ctor.getParameterCount() == 2
							&& ctor.getParameterTypes()[0] == DataSource.class
							&& ctor.getParameterTypes()[1] == Database.class)
					{
						propertiesOwner = (PropertiesOwner) ctor.newInstance(null, null);
					}
					if(propertiesOwner != null)
					{
						break;
					}
				}
			}
			if(propertiesOwner == null)
			{
				throw new WebAppException(HttpServletResponse.SC_CONFLICT, "Cannot get property specs for '" + className + "'");
			}
			return propertiesOwner.getSupportedProps();
		}
		catch (RuntimeException | InstantiationException | IllegalAccessException | InvocationTargetException | ClassNotFoundException ex)
		{
			throw new WebAppException(HttpServletResponse.SC_CONFLICT, "Cannot get property specs for '" + className + "'", ex);
		}
	}
}
