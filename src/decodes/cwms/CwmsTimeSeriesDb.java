/*
*
* $Id$
* 
*  This is open-source software written by Sutron Corporation, under
*  contract to the federal government. You are free to copy and use this
*  source code for your own purposes, except that no part of the information
*  contained in this file may be claimed to be proprietary.
*
*  Except for specific contractual terms between ILEX and the federal 
*  government, this source code is provided completely without warranty.
*  For more information contact: info@ilexeng.com
*  
*  $Log$
*  Revision 1.10  2015/09/10 21:18:29  mmaloney
*  Development on Screening
*
*  Revision 1.9  2015/05/14 13:52:19  mmaloney
*  RC08 prep
*
*  Revision 1.8  2015/04/02 18:10:03  mmaloney
*  Store dbURI and jdbcDriver so that CwmsConsumer can override them and
*  make them different from the definitions in DecodesSettings.
*  Fix bug in getNewDataSince whereby cache was getting refreshed on every
*  call rather than only once per hour.
*
*  Revision 1.7  2015/01/23 19:15:53  mmaloney
*  Improved debugs on CWMS qualcode in getNewData
*
*  Revision 1.6  2014/12/23 14:11:57  mmaloney
*  Explicitly reference hec.data.Units after connect to pre-initialize it.
*
*  Revision 1.5  2014/12/17 21:37:20  mmaloney
*  Failsafe check to make sure that tasklist record doesn't have null units.
*
*  Revision 1.4  2014/08/29 18:24:50  mmaloney
*  6.1 Schema Mods
*
*  Revision 1.3  2014/08/15 16:22:19  mmaloney
*  When reading tasklist, if mutliple recs for same time series with different units,
*  then convert subsequent recs to the same units as the first rec seen.
*
*  Revision 1.2  2014/05/22 12:17:15  mmaloney
*  Fix bug. After creating TS, set the site attribute of the tsid.
*
*  Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
*  OPENDCS 6.0 Initial Checkin
*
*  Revision 1.171  2013/08/06 14:02:49  mmaloney
*  tasklist queue bug fix.
*
*  Revision 1.170  2013/07/31 15:26:58  mmaloney
*  Added methods to check for questionable and set questionable.
*
*  Revision 1.169  2013/07/29 14:21:20  mmaloney
*  removed debugs
*
*  Revision 1.168  2013/07/24 18:11:27  mmaloney
*  dev
*
*  Revision 1.167  2013/07/24 13:35:20  mmaloney
*  Cleanup the listCompsForGui stuff. This is all now portable across databases.
*
*  Revision 1.166  2013/07/12 18:13:34  mmaloney
*  dev
*
*  Revision 1.165  2013/07/12 17:46:29  mmaloney
*  dev
*
*  Revision 1.164  2013/07/12 11:51:19  mmaloney
*  Added tasklist queue stuff.
*
*  Revision 1.163  2013/06/26 13:42:36  mmaloney
*  Implement CCP Privilege check.
*
*  Revision 1.162  2013/06/18 04:09:33  gchen
*  Modify the CWMS_CCP_VPD procedure name from set_ctx_db_office_id to set_session_office_id
*
*  Revision 1.161  2013/05/28 20:01:34  mmaloney
*  Fix for CWMS-3010
*
*  Revision 1.160  2013/04/26 15:07:59  mmaloney
*  dev
*
*  Revision 1.159  2013/04/26 15:07:03  mmaloney
*  Recover from bad login by allowing user to re-enter username/password.
*
*  Revision 1.158  2013/04/26 14:58:40  mmaloney
*  Dynamically determine CWMS version from TSDB Version.
*  TSDB Version 8 = CWMS Version 2.2
*
*  Revision 1.157  2013/04/24 23:49:28  mmaloney
*  dev
*
*  Revision 1.156  2013/04/23 13:25:23  mmaloney
*  Office ID filtering put back into Java.
*
*  Revision 1.155  2013/04/16 15:08:34  mmaloney
*  Use new createTs methods from Peter Morris.
*
*  Revision 1.154  2013/04/11 20:47:33  mmaloney
*  Update to call Gang's new setCtxUserId method
*
*  Revision 1.153  2013/04/11 12:48:11  gchen
*  *** empty log message ***
*
*  Revision 1.152  2013/04/11 12:13:17  gchen
*  Implement the setCtxDbOfficeId method to call the Oracle DB procedure cwms_ccp_vpd.set_ctx_db_office_code and to reset the VPD context variable with user specified office code.
*
*  Revision 1.151  2013/04/10 15:53:07  mmaloney
*  Stub for setting context office ID.
*
*  Revision 1.150  2013/04/08 17:57:46  mmaloney
*  determining privileged office IDs.
*
*  Revision 1.149  2013/04/04 19:24:56  mmaloney
*  CWMS connection stuff for both DECODES and TSDB.
*
*  Revision 1.148  2013/04/01 22:15:34  mmaloney
*  dev
*
*  Revision 1.147  2013/03/27 18:42:29  mmaloney
*  CWMS 2.2 Mods
*
*  Revision 1.146  2013/03/22 20:02:15  mmaloney
*  Added DST versions of 3, 4, 6, 8, and 12 hours.
*
*  Revision 1.145  2013/03/21 18:27:40  mmaloney
*  DbKey Implementation
*
*  Revision 1.144  2013/02/20 15:07:24  gchen
*  Enhance a new feature to allow to use the maxComputationRetries property 
*  to limit the number of retries for those failed computations. There will 
*  be unlimited retries if maxComputationRetires=0.
*
*  This feature will apply to Tempest DB, CWMS, and HDB.
*
*  Revision 1.143  2012/11/13 15:14:49  mmaloney
*  dev
*
*  Revision 1.142  2012/10/22 14:31:02  mmaloney
*  User upper() on officeID
*
*  Revision 1.141  2012/10/09 12:42:03  mmaloney
*  removed old debugs.
*
*  Revision 1.140  2012/10/02 18:04:28  mmaloney
*  Don't set display name on default CwmsTsId constructor.
*
*  Revision 1.139  2012/09/17 21:13:28  mmaloney
*  fillTimeSeriesMetadat should call CTimeSeries.setTimeSeriesIdentifier if necessary.
*
*  Revision 1.138  2012/09/12 21:32:17  mmaloney
*  Get full site when getting TSID.
*
*  Revision 1.137  2012/09/12 21:15:14  mmaloney
*  use site cache.
*  In setParmSDI, fill in all site names.
*
*  Revision 1.136  2012/09/11 13:03:16  mmaloney
*  dev
*
*  Revision 1.135  2012/09/11 12:57:25  mmaloney
*  dev
*
*  Revision 1.134  2012/09/11 12:40:41  mmaloney
*  set display name.
*
*  Revision 1.133  2012/09/11 12:34:25  mmaloney
*  debug
*
*  Revision 1.132  2012/09/11 00:51:17  mmaloney
*  dev
*
*  Revision 1.131  2012/09/11 00:46:31  mmaloney
*  Add parenthesized display name capability.
*
*  Revision 1.130  2012/09/10 18:57:04  mmaloney
*  If cancel on login dialog, don't try to connect to DB.
*
*  Revision 1.129  2012/08/13 15:21:10  mmaloney
*  added makeEmptyTsId().
*
*  Revision 1.128  2012/07/30 13:22:22  mmaloney
*  code cleanup.
*
*  Revision 1.127  2012/07/24 14:30:04  mmaloney
*  getDataTypesByStandard moved to base class decodes.tsdb.TimeSeriesDb.
*
*  Revision 1.126  2012/07/23 15:20:44  mmaloney
*  Refactor group evaluation for HDB.
*
*  Revision 1.125  2012/07/15 19:54:10  mmaloney
*  Refactor read/write DateFmt. For HDB, always use GMT/UTC.
*
*  Revision 1.124  2012/07/12 19:24:58  mmaloney
*  timestamp debugs.
*
*  Revision 1.123  2012/07/12 17:23:07  mmaloney
*  debugDateFmt when saving ts data.
*
*  Revision 1.122  2012/07/05 18:24:02  mmaloney
*  CWMS location names may contain spaces and multiple hyphens.
*  ts key is stored as a long.
*
*  Revision 1.121  2012/06/18 15:14:55  mmaloney
*  Moved TS ID cache to base class.
*
*  Revision 1.120  2012/06/18 13:20:43  mmaloney
*  minRecNum in cp_comp_tasklist must be defined as long
*
*  Revision 1.119  2012/06/06 15:15:34  mmaloney
*  reduced a debug
*
*  Revision 1.118  2012/05/25 17:22:53  mmaloney
*  Omaha problem with protected-flag.
*
*  Revision 1.117  2012/05/17 15:09:37  mmaloney
*  Clean up imports and remove proprietary dependencies from OS code.
*
*  Revision 1.116  2012/05/15 14:28:14  mmaloney
*  1. createTimeSeries calls checkValid, which can throw BadTimeSeriesException.
*  2. transformTsidByCompParm can throw BadTimeSeriesException because
*  it calls createTimeSeries if create flag == true.
*
*  Revision 1.115  2012/05/09 21:34:00  mmaloney
*  Read CWMS quality_code as a long int.
*
*  Revision 1.114  2012/04/30 18:52:11  mmaloney
*  fillDependentCompIds must filter on loadingAppId.
*
*  Revision 1.113  2012/02/27 16:55:50  gchen
*  Modified the getNewDataSince() function to separate the tasklist table records with 'Y' and "N' delete flag, which would temporarily solved the failed computation issues while using Resample and Subsample algorithms.
*
*  Revision 1.112  2012/01/26 17:40:03  mmaloney
*  Check for nul return on quality code.
*
*  Revision 1.111  2012/01/17 17:43:50  mmaloney
*  Call DecodesInterface.setGUI(true).
*
*  Revision 1.110  2011/12/16 20:25:09  mmaloney
*  dev
*
*  Revision 1.109  2011/12/16 20:21:06  mmaloney
*  GUI Apps must login via dialog.
*
*  Revision 1.108  2011/11/07 21:56:10  mmaloney
*  Check for INFINITE when reading CWMS_V_TSV, otherwise, the ROUND method throws a SQL overflow error.
*
*  Revision 1.107  2011/10/19 22:21:12  gchen
*  Modify the CwmsTimeSeriesDb with adding the writeTsGroup() to avoid using the one in TimeSeriesDb. This will solve the issue that cp_comp_depends gets updated while a TS group is saved into CWMS DB.
*
*  Revision 1.106  2011/09/21 19:19:22  gchen
*  Fix the bug in using XML as DECODES DB against CWMS DB (no CCP DB objects).
*
*  Revision 1.105  2011/09/09 06:42:16  mmaloney
*  For CWMS Consumer, don't fail connect if the CCP tables don't exist.
*
*  Revision 1.104  2011/09/01 21:34:37  mmaloney
*  dev
*
*  Revision 1.103  2011/07/12 18:55:14  mmaloney
*  dev
*
*  Revision 1.102  2011/06/17 13:31:22  gchen
*  Fix the null pointer exception when starting the chain computations
*
*  Revision 1.101  2011/06/16 18:15:40  mmaloney
*  Bad quality code logic in getprev & get next.
*
*  Revision 1.100  2011/06/16 15:25:38  mmaloney
*  dev
*
*  Revision 1.99  2011/06/16 14:12:59  mmaloney
*  dev
*
*  Revision 1.98  2011/06/16 14:07:25  mmaloney
*  fix
*
*  Revision 1.97  2011/06/16 13:30:24  mmaloney
*  Initialize & refresh tsid cache every hour.
*
*  Revision 1.96  2011/05/16 13:56:28  mmaloney
*  Allow 300 dates per query in fillTimeSeries
*
*  Revision 1.95  2011/05/03 16:37:57  mmaloney
*  Optimize fillTimeSeriesMetadata - read meta-data out of the cache.
*
*  Revision 1.94  2011/04/27 19:12:03  mmaloney
*  rs2TimedVariable: DO return REJECTED values. Only MISSING values are discarded.
*
*  Revision 1.93  2011/04/19 19:14:10  gchen
*  (1) Add a line to set Site.explicitList = true in cwmsTimeSeriesDb.java to fix the multiple location entries on Location Selector in TS Group GUI.
*
*  (2) Fix a bug in getDataType(String standard, String code, int id) method in decodes.db.DataType.java because the data id wasn't set up previously.
*
*  (3) Fix the null point exception in line 154 in cwmsGroupHelper.java.
*
*  Revision 1.92  2011/04/14 15:52:10  mmaloney
*  dev
*
*  Revision 1.91  2011/04/14 15:47:04  mmaloney
*  dev
*
*  Revision 1.90  2011/04/14 15:17:37  mmaloney
*  In comprun gui show alpha codes SQRM, rather than hex.
*
*  Revision 1.89  2011/03/24 19:10:20  mmaloney
*  Added codes for tilde-intervals for CWMS
*
*  Revision 1.88  2011/03/23 15:19:53  mmaloney
*  expandSDI use the TimeSeriesId method (& cache) to avoid unnecessary db reads.
*
*  Revision 1.87  2011/03/22 17:34:39  mmaloney
*  Instantiate cwms tsid cache.
*
*  Revision 1.86  2011/03/22 16:45:27  mmaloney
*  caching improvements.
*
*  Revision 1.85  2011/03/22 14:13:25  mmaloney
*  Added caching for DbComputations and CWMS Time Series Identifiers.
*
*  Revision 1.84  2011/03/18 14:48:42  mmaloney
*  bug in listTimeSeries
*
*  Revision 1.83  2011/03/18 14:15:45  mmaloney
*  transform tsid method split off.
*
*  Revision 1.82  2011/03/17 15:21:56  mmaloney
*  bugfix
*
*  Revision 1.81  2011/03/17 14:55:58  mmaloney
*  When reading tsid, DON'T join with the datatype table. Do that in separate step.
*
*  Revision 1.80  2011/03/04 20:39:55  mmaloney
*  created
*
*  Revision 1.79  2011/03/03 13:45:08  mmaloney
*  Code must not assume that decodes db is the same as CWMS db. We must accommodate
*  districts that are using XML decodes.
*
*  Revision 1.78  2011/03/01 15:55:30  mmaloney
*  Implement deleteTimeSeries
*
*  Revision 1.77  2011/02/18 14:42:22  mmaloney
*  Don't put null TimedVariables into CTimeSeries.
*
*  Revision 1.76  2011/02/17 13:50:35  mmaloney
*  When creating time-series, use the correct HecConstant for UTC Offset.
*
*  Revision 1.75  2011/02/17 13:38:17  mmaloney
*  Guard against overflow exception when reading time-series values.
*
*  Revision 1.74  2011/02/17 13:05:13  mmaloney
*  Must retrieve CWMS quality code as a BigDecimal and then convert to long.
*
*  Revision 1.73  2011/02/16 14:56:47  mmaloney
*  Timeout mechanism for re-subscribing to the CWMS queue.
*
*  Revision 1.72  2011/02/15 17:00:01  mmaloney
*  sql syntax bug fix.
*
*  Revision 1.71  2011/02/15 16:52:34  mmaloney
*  Defensive programming: Don't join the task list with any other tables because the
*  join might fail, leaving bogus tasklist entries on the queue forever.
*
*  Revision 1.70  2011/02/11 20:44:13  mmaloney
*  Do not through DbIoException when call to store fails. CWMS has so many business rules
*  that can cause a store to fail. It does not mean the connection is bad.
*
*  Revision 1.69  2011/02/11 20:13:00  mmaloney
*  Only receive 10,000 records at a time in getNewData.
*
*  Revision 1.68  2011/02/11 18:54:51  mmaloney
*  Only receive 10,000 records at a time in getNewData.
*
*  Revision 1.67  2011/02/08 13:29:27  mmaloney
*  All tsdb reads must be units-savvy.
*
*  Revision 1.66  2011/02/08 00:24:25  mmaloney
*  MUST convert units on output so that the store_ts hook will store units in the
*  database-storage units. Otherwise compproc will have the wrong units.
*
*  Revision 1.65  2011/02/08 00:11:58  mmaloney
*  MUST convert units on output so that the store_ts hook will store units in the
*  database-storage units. Otherwise compproc will have the wrong units.
*
*  Revision 1.64  2011/02/08 00:03:47  mmaloney
*  MUST convert units on output so that the store_ts hook will store units in the
*  database-storage units. Otherwise compproc will have the wrong units.
*
*  Revision 1.63  2011/02/07 23:52:09  mmaloney
*  MUST convert units on output so that the store_ts hook will store units in the
*  database-storage units. Otherwise compproc will have the wrong units.
*
*  Revision 1.62  2011/02/07 18:34:34  mmaloney
*  Got rid of PgTimeSeriesDb intermediate class.
*  TempestTsdb now extends TimeSeriesDb directly.
*
*  Revision 1.61  2011/02/03 20:00:23  mmaloney
*  Time Series Group Editor Mods
*
*  Revision 1.60  2011/02/02 20:42:28  mmaloney
*  bug fixes
*
*  Revision 1.59  2011/02/02 14:32:30  mmaloney
*  debugs
*
*  Revision 1.58  2011/02/01 15:31:10  gchen
*  *** empty log message ***
*
*  Revision 1.57  2011/01/31 15:21:29  mmaloney
*  debug
*
*  Revision 1.56  2011/01/31 14:03:04  mmaloney
*  For Cwms, where datatype can be any odd combination of Param-SubParam,
*  when importing a computation, if datatype doesn't already exist, create it.
*
*  Revision 1.55  2011/01/28 20:01:46  gchen
*  *** empty log message ***
*
*  Revision 1.54  2011/01/27 23:27:32  gchen
*  Add the listTimeSeries method to retrieve TS Id data
*
*  Revision 1.53  2011/01/26 21:34:08  mmaloney
*  Check for null TV
*
*  Revision 1.52  2011/01/24 18:59:02  mmaloney
*  DataType.getDataType was being called with arguments swapped.
*
*  Revision 1.51  2011/01/21 20:39:49  mmaloney
*  With new store method, must never put a null value in the value array.
*
*  Revision 1.50  2011/01/21 20:30:36  mmaloney
*  debug
*
*  Revision 1.49  2011/01/21 20:20:00  mmaloney
*  debug
*
*  Revision 1.48  2011/01/20 13:09:17  mmaloney
*  Added stub for listTimeSeries
*
*  Revision 1.47  2011/01/19 15:44:30  mmaloney
*  Set display name to unique string only.
*
*  Revision 1.46  2011/01/18 19:20:48  mmaloney
*  dev
*
*  Revision 1.45  2011/01/13 13:04:44  mmaloney
*  Call refresh_mv_cwms_ts_id after creating a time series in the database.
*
*  Revision 1.44  2011/01/13 12:28:24  mmaloney
*  fix getNewData
*
*  Revision 1.43  2011/01/12 22:06:12  mmaloney
*  getNewData must create CwmsTsIds for each time series returned.
*
*  Revision 1.42  2011/01/12 21:46:38  mmaloney
*  dev
*
*  Revision 1.41  2011/01/12 20:59:03  mmaloney
*  dev
*
*  Revision 1.40  2011/01/12 20:49:30  mmaloney
*  dev
*
*  Revision 1.39  2011/01/11 18:46:06  mmaloney
*  dev
*
*  Revision 1.38  2011/01/10 20:57:16  mmaloney
*  dev
*
*  Revision 1.37  2011/01/10 20:43:19  mmaloney
*  connect issues
*
*  Revision 1.36  2011/01/10 20:25:58  mmaloney
*  debug
*
*  Revision 1.35  2011/01/10 19:32:08  mmaloney
*  Don't overload methods to evaluate cp_comp_depends. This is all done in the base class now.
*
*  Revision 1.34  2011/01/10 18:48:57  mmaloney
*  dataType ID is NOT the same as parameter_code. We keep our own copy of the CWMS parameter ids in our datatype table. The ID refers to our own table.
*
*  Revision 1.33  2011/01/10 18:24:27  mmaloney
*  Removed lookupDataType and getDataTypeById from the CWMS sub-class.
*
*  Revision 1.32  2011/01/05 21:03:21  mmaloney
*  bugfix
*
*  Revision 1.31  2011/01/05 20:50:38  mmaloney
*  bugfix
*
*  Revision 1.30  2011/01/05 13:25:17  mmaloney
*  dev
*
*  Revision 1.29  2011/01/01 21:28:53  mmaloney
*  CWMS Testing
*
*  Revision 1.28  2010/12/21 19:31:04  mmaloney
*  group computations
*
*  Revision 1.27  2010/12/05 15:48:10  mmaloney
*  Cleanup and creation of HecConstants.
*
*  Revision 1.26  2010/11/28 21:05:25  mmaloney
*  Refactoring for CCP Time-Series Groups
*
*  Revision 1.25  2010/11/05 18:19:59  mmaloney
*  Use RMA methods for writing time-series data and sites.
*
*  Revision 1.24  2010/10/29 15:08:23  mmaloney
*  Several CWMS updates.
*
*  Revision 1.23  2010/10/22 18:03:32  mmaloney
*  CCP Refactoring
*
*  Revision 1.22  2010/09/30 19:01:15  mmaloney
*  Moved determineTsdbVersion to base class TimeSeriesDb
*
*  Revision 1.21  2010/08/20 19:24:37  mmaloney
*  Added CVS Log to header
*
*/
package decodes.cwms;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Date;
import java.util.Iterator;
import java.util.GregorianCalendar;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.TimeZone;

import opendcs.dai.DataTypeDAI;
import opendcs.dai.IntervalDAI;
import opendcs.dai.ScheduleEntryDAI;
import opendcs.dai.SiteDAI;
import opendcs.dai.TimeSeriesDAI;
import lrgs.gui.DecodesInterface;
import cwmsdb.CwmsSecJdbc;
import cwmsdb.CwmsCatJdbc;
import oracle.jdbc.OraclePreparedStatement;
import ilex.util.Logger;
import ilex.util.StringPair;
import ilex.util.TextUtil;
import ilex.var.NamedVariable;
import ilex.var.TimedVariable;
import ilex.var.Variable;
import decodes.cwms.validation.dao.ScreeningDAI;
import decodes.cwms.validation.dao.ScreeningDAO;
import decodes.db.Constants;
import decodes.db.Database;
import decodes.db.EngineeringUnit;
import decodes.db.Site;
import decodes.db.SiteName;
import decodes.db.DataType;
import decodes.db.UnitConverter;
import decodes.sql.DbKey;
import decodes.tsdb.*;
import decodes.util.DecodesSettings;

/**
This is the base class for the time-series database implementation.
Sub classes must override all the abstract methods and provide
a mechanism to persistently store time series and computational meta
data.
*/
public class CwmsTimeSeriesDb
	extends TimeSeriesDb
{
//	private String[] creds = { "username", "password" };

	private String dbOfficeId = null;
	private DbKey dbOfficeCode = Constants.undefinedId;
	private String dbOfficePrivilege = null;

//	private SimpleDateFormat rwdf;
//	private OraclePreparedStatement storeTsStmt = null;
//	private TasklistQueueFile tasklistQueueFile = null;
//	private int tasklistQueueThresholdHours = 8;
//	private int numTasklistQueueErrors = 0;


	private String[] currentlyUsedVersions = { "" };
	GregorianCalendar saveTsCal = new GregorianCalendar(
		TimeZone.getTimeZone("UTC"));
	
	CwmsGroupHelper cwmsGroupHelper = null;
//	private long lastTaskListRecords = 0L;
//	private long taskListTimeoutMillisec = 300000L;
	
	public boolean requireCcpTables = true;
	
	public static final int CWMS_V_2_1 = 21;
	public static final int CWMS_V_2_2 = 22;
	public static final int CWMS_V_3_0 = 30;
	private int cwmsSchemaVersion = 0;
	
	private String dbUri = null;
	private String jdbcOracleDriver = null;
	
	private BaseParam baseParam = new BaseParam();
	
	
	/**
	 * No args constructor required because this is instantiated from
	 * the class name.
	 */
	public CwmsTimeSeriesDb()
	{
		super();
		
		Site.explicitList = true;

		// CWMS uses ts_code as a unique identifier of a time-series
		// Internally our SDI (site datatype id) is equivalent to CWMS ts_code
		sdiIsUnique = true;

		curTimeName = "sysdate";
		maxCompRetryTimeFrmt = "%d*1/24";
	}

	/**
	 * Connect this app to the database and return appID. 
	 * The credentials property set contains username, password,
	 * etc, for connecting to database.
	 * @param appName must match an application in the database.
	 * @param credentials must contain all needed login parameters.
	 * @return application ID.
	 * @throws BadConnectException if failure to connect.
	 */
	public DbKey connect( String appName, Properties credentials )
		throws BadConnectException
	{
		String driverClass = this.jdbcOracleDriver != null ? this.jdbcOracleDriver :
			DecodesSettings.instance().jdbcDriverClass;
		String dbUri = this.dbUri != null ? this.dbUri :
			DecodesSettings.instance().editDatabaseLocation;
		
		String username = credentials.getProperty("username");
		String password = credentials.getProperty("password");

		CwmsGuiLogin cgl = CwmsGuiLogin.instance();
		if (DecodesInterface.isGUI())
		{
			try 
			{
				if (!cgl.isLoginSuccess())
				{
					cgl.doLogin(null);
					if (!cgl.isLoginSuccess()) // user hit cancel
						throw new BadConnectException("Login aborted by user.");

				}
				
				username = cgl.getUserName();
				password = new String(cgl.getPassword());
			}
			catch(Exception ex)
			{
				throw new BadConnectException(
					"Cannot display login dialog: " + ex);
			}
		}
		
		try 
		{
			debug3("Getting class for '" + driverClass + "'");
			Class.forName(driverClass);
			debug3("Using DriverManager to connect as user '" + username + "'");
			conn = DriverManager.getConnection(dbUri, username, password);
			debug3("Connection established.");
		}
		catch (Exception ex) 
		{
			conn = null;
			cgl.setLoginSuccess(false);
			throw new BadConnectException(
				"Error getting JDBC connection using driver '"
				+ driverClass + "' to database at '" + dbUri
				+ "' for user '" + username + "': " + ex.toString());
		}

		String q = "ALTER SESSION SET time_zone = "
			+ sqlString(DecodesSettings.instance().sqlTimeZone);
		Statement st = null;
		ResultSet rs = null;
		try
		{
			debug3("setting session tz with '" + q + "'");
			st = conn.createStatement();
			st.execute(q);
			st.close();
		}
		catch(SQLException ex)
		{
			String msg = "Error in SQL Statement '" + q + "': " + ex;
			cgl.setLoginSuccess(false);
			failure(msg);
		}
		
		// CWMS is Always GMT.
		DecodesSettings.instance().sqlTimeZone = "GMT";
		determineTsdbVersion();

		cwmsSchemaVersion = determineCwmsSchemaVersion(getConnection(), tsdbVersion);
		
		if (cwmsSchemaVersion >= CWMS_V_2_2)
		{
			Logger.instance().debug1(
				"Connected to CWMS " + cwmsSchemaVersion + " database. Will set office ID context.");
			dbOfficeId = null;
			ArrayList<StringPair> officePrivileges = null;
			try
			{
				officePrivileges = determinePrivilegedOfficeIds(getConnection(), cwmsSchemaVersion);
			}
			catch (SQLException ex)
			{
				String msg = "Cannot determine privileged office IDs: " + ex;
				failure(msg);
				cgl.setLoginSuccess(false);
				throw new BadConnectException(msg);
			}
			// Make sure office  matches for case with one of the privileged
			for(StringPair op : officePrivileges)
				if (TextUtil.strEqualIgnoreCase(op.first, DecodesSettings.instance().CwmsOfficeId))
				{
					dbOfficeId = op.first;
					dbOfficePrivilege = op.second;
					break;
				}
			// If GUI, allow user to select from the privileged offices
			if (DecodesInterface.isGUI() && officePrivileges.size() > 0)
			{
				if (!cgl.isOfficeIdSelected())
					cgl.selectOfficeId(null, officePrivileges, dbOfficeId);
				dbOfficeId = cgl.getDbOfficeId();
				dbOfficePrivilege = cgl.getDbOfficePrivilege();
			}
			else if (officePrivileges.size() > 0 && dbOfficeId == null)
			{
				// Not a GUI and not selected in properties.
				dbOfficeId = officePrivileges.get(0).first;
				dbOfficePrivilege = officePrivileges.get(0).second;
			}

			if (dbOfficeId == null)
			{
				cgl.setLoginSuccess(false);
				throw new BadConnectException("No office ID with any CCP Privilege!");
			}
			dbOfficeCode = officeId2code(getConnection(), dbOfficeId);
			try
			{
				setCtxDbOfficeId(getConnection(), dbOfficeId,
					dbOfficeCode, dbOfficePrivilege, tsdbVersion);
			}
			catch (Exception ex)
			{
				closeConnection();
				String msg = "Cannot set username/officeId username='" + username
					+ "', officeId='" + dbOfficeId + "'";
				failure(msg);
				cgl.setLoginSuccess(false);
				throw new BadConnectException(msg);
			}
			try
			{
				baseParam.load(this);
			}
			catch (Exception ex)
			{
				String msg = "Cannot load baseParam Units Map: " + ex;
				failure(msg);
			}
		}
		else // CWMS 2.1 or earlier
		{
			dbOfficeId = DecodesSettings.instance().CwmsOfficeId;
			dbOfficeCode = officeId2code(conn, dbOfficeId);
		}

		postConnectInit(appName);
		
		keyGenerator = new CwmsSequenceKeyGenerator(cwmsSchemaVersion, getDecodesDatabaseVersion());

		if (dbOfficeId != null && dbOfficeId.length() > 0)
		{
			try
			{
				q = "SELECT DISTINCT VERSION_ID FROM CWMS_V_TS_ID "
					+ "WHERE upper(DB_OFFICE_ID) = " + sqlString(dbOfficeId.toUpperCase());
				ArrayList<String> versionIds = new ArrayList<String>();
				rs = doQuery(q);
				while(rs != null && rs.next())
					versionIds.add(rs.getString(1));
				currentlyUsedVersions = new String[versionIds.size()];
				for(int i = 0; i<currentlyUsedVersions.length; i++)
					currentlyUsedVersions[i] = versionIds.get(i);
			}
			catch(Exception ex)
			{
				warning("Error executing '" + q + "':" + ex);
			}

		}

		Logger.instance().info(module + 
			" Connected to DECODES CWMS " + cwmsSchemaVersion 
			+ " Database " + dbUri + " as user " + username
			+ " with officeID=" + dbOfficeId + " (dbOfficeCode=" + dbOfficeCode + ")");

		cgl.setLoginSuccess(true);
		
		try
		{
			Logger.instance().info(module + " calling hec.data.Units.getAvailableUnits()");
			hec.data.Units.getAvailableUnits();
		}
		catch(Exception ex)
		{
			Logger.instance().warning(module + " Exception in hec.data.Units.getAvailableUnits: "
				+ ex);
		}
		
		return appId;
	}
	
	
	/**
	 * Fills in the internal list of privileged office IDs.
	 * @return array of string pairs: officeId,Privilege
	 * @throws SQLException
	 */
	public static ArrayList<StringPair> determinePrivilegedOfficeIds(Connection conn, 
		int cwmsSchemaVersion)
		throws SQLException
	{
		ArrayList<StringPair> ret = new ArrayList<StringPair>();
		CwmsSecJdbc cwmsSec = new CwmsSecJdbc(conn);
		// 4/8/13 phone call with Pete Morris - call with Null. and the columns returned are:
		// username, user_db_office_id, db_office_id, user_group_type, user_group_owner, user_group_id,
		// is_member, user_group_desc
		ResultSet rs = cwmsSec.getAssignedPrivGroups(null);
		while(rs != null && rs.next())
		{
			String username = rs.getString(1);
			String db_office_id = null;
			String user_group_id = null;
			if (cwmsSchemaVersion <= CWMS_V_2_2)
			{
				db_office_id = rs.getString(3);
				user_group_id = rs.getString(6);
			}
			else // CWMS 3.0 or later
			{
				db_office_id = rs.getString(2);
				user_group_id = rs.getString(5);
			}
//			String user_db_office_id = rs.getString(2);
//			String user_group_type = rs.getString(4);
//			String user_group_owner = rs.getString(5);
//			String is_member = rs.getString(7);
//			String user_group_desc = rs.getString(8);

			Logger.instance().debug1("privilegedOfficeId: username='" + username + "' "
//				+ "user_db_office_id='" + user_db_office_id + "' "
				+ "db_office_id='" + db_office_id + "' "
//				+ "user_group_type='" + user_group_type + "' "
//				+ "user_group_owner='" + user_group_owner + "' "
				+ "user_group_id='" + user_group_id + "' "
//				+ "is_member='" + is_member + "' "
//				+ "user_group_desc='" + user_group_desc + "'"
				);
			
			// We look for groups "CCP Proc", "CCP Mgr", and "CCP Reviewer".
			// Ignore anything else.
			String gid = user_group_id.trim();
			if (!TextUtil.startsWithIgnoreCase(gid, "CCP"))
				continue;
			
			// See if we have an existing privilege for this office ID.
			int existingIdx = 0;
			String existingPriv = null;
			for(; existingIdx < ret.size(); existingIdx++)
			{
				StringPair sp = ret.get(existingIdx);
				if (sp.first.equalsIgnoreCase(db_office_id))
				{
					existingPriv = sp.second;
					break;
				}
			}
			// If we do have an existing privilege, determine whether to keep this
			// one or the existing one (keep the one with more privilege).
			if (existingPriv != null)
			{
				if (existingPriv.toUpperCase().contains("MGR"))
					continue; // We are already manager in this office. Discard this item.
				else if (gid.toUpperCase().contains("MGR"))
				{
					// This item is MGR, replace existing one.
					ret.get(existingIdx).second = gid;
					continue;
				}
				else if (gid.toUpperCase().contains("PROC"))
				{
					// This is for PROC, existing must be PROC or Reviewer. Replace.
					ret.get(existingIdx).second = gid;
					continue;
				}
			}
			else // this item is first privilege seen for this office.
				ret.add(new StringPair(db_office_id, gid));
		}
		return ret;
	}


	public void setParmSDI(DbCompParm parm, DbKey siteId, String dtcode)
		throws DbIoException, NoSuchObjectException
	{
		debug3("setParmSDI siteId=" + siteId + 
			", dtcode=" + dtcode);
		ResultSet rs = null;

		DataType dt = null;
		try { lookupDataType(dtcode); }
		catch(NoSuchObjectException ex)
		{
			// This combo of CWMS Param-SubParam doesn't exist yet in the
			// database as a 'datatype' object. Create it.
			dt = DataType.getDataType(Constants.datatype_CWMS, dtcode);
			DataTypeDAI dataTypeDao = makeDataTypeDAO();
			try { dataTypeDao.writeDataType(dt); }
			finally { dataTypeDao.close(); }
		}
		try
		{
			String q = 
				"SELECT TS_CODE, LOCATION_ID FROM CWMS_V_TS_ID "
				+ "WHERE LOCATION_CODE = " + siteId
				+ " AND TS_ACTIVE_FLAG = 'T'"
				+ " AND upper(PARAMETER_ID) = " 
						+ sqlString(dtcode.toUpperCase())
				+ " AND INTERVAL_ID = " + sqlString(parm.getInterval())
				+ " AND VERSION_ID = " + sqlString(parm.getVersion())
				+ " AND DURATION_ID = " + sqlString(parm.getDuration())
				+ " AND PARAMETER_TYPE_ID = " + sqlString(parm.getParamType());
			// Don't need to select on office id. It is implied by location code.

			rs = doQuery(q);
			if (rs != null && rs.next())
			{
				DbKey sdi = DbKey.createDbKey(rs, 1);
				parm.setSiteDataTypeId(sdi);
				parm.addSiteName(new SiteName(null, Constants.snt_CWMS,
					rs.getString(2)));
			}
			else
				throw new NoSuchObjectException(
					"No Time Series with specified identifiers.");
			Site site = this.getSiteById(siteId);
			if (site != null)
				for(Iterator<SiteName> snit = site.getNames(); snit.hasNext(); )
					parm.addSiteName(snit.next());
		}
		catch(SQLException ex)
		{
			throw new DbIoException("setParmSDI: " + ex);
		}
		finally
		{
			if (rs != null) 
				try { rs.close(); }
				catch(Exception ex) {}
		}
	}

	/**
	 * Given a SiteDatatype object containing an SDI read from the SQL database,
	 * expand it into all known datatype and site names. Store these back into
	 * the passed object.
	 * @param siteDatatype the association between sdi, names, & datatypes
	 */
	public void expandSDI(DbCompParm parm)
		throws DbIoException, NoSuchObjectException
	{
		TimeSeriesDAI timeSeriesDAO = makeTimeSeriesDAO();
		try
		{
			TimeSeriesIdentifier tsid = timeSeriesDAO.getTimeSeriesIdentifier(
				parm.getSiteDataTypeId());
			parm.setSite(tsid.getSite());
			parm.setDataType(tsid.getDataType());
		}
		finally
		{
			timeSeriesDAO.close();
		}
	}
	
	/**
	 * CWMS TSDB stores ParamType.Duration.Version in the tab selector.
	 */
	public String getTableSelectorLabel()
	{
		return "Type.Dur.Version";
	}

	/**
	 * Used to present user with a list of valid datatypes 
	 * for a given site. Returns 2-dimensional array of Strings suitable
	 * for populating a table from which the user can select.
	 * The first row of the table (i.e. r[0]) must contain the column
	 * header strings.
	 * MJM - For CWMS we show all 5 path components for the site.
	 * @param siteId the ID of the site
	 * @return 2-dimensional array of strings, containing data types.
	 */
	public ArrayList<String[]> getDataTypesForSite(DbKey siteId)
		throws DbIoException
	{
		String header[] = new String[5];
		header[0] = "Param";
		header[1] = "Param Type";
		header[2] = "Interval";
		header[3] = "Duration";
		header[4] = "Version";
		
		ArrayList<String[]> ret = new ArrayList<String[]>();
		ret.add(header);

		String q = "SELECT PARAMETER_ID, PARAMETER_TYPE_ID, INTERVAL_ID, "
			+ "DURATION_ID, VERSION_ID "
			+ "FROM CWMS_V_TS_ID "
			+ " where location_code = " + siteId
			+ " order by PARAMETER_ID, PARAMETER_TYPE_ID, INTERVAL_ID";
		try
		{
			ResultSet rs = doQuery(q);
			while(rs.next())
			{
				String dtl[] = new String[5];
				for(int i=0; i<5; i++)
					dtl[i] = rs.getString(i+1);
				ret.add(dtl);
			}
		}
		catch(SQLException ex)
		{
			warning("Error reading Time series types for Location Code="
				+ siteId + ": " + ex);
		}
		return ret;
	}
	
	/**
	 * Validate the passed information to make sure it represents a valid
	 * parameter within this database. If not, throw ConstraintException.
	 */
	public void validateParm(DbKey siteId, String dtcode, String interval, 
		String tabSel, int modelId)
		throws ConstraintException, DbIoException
	{
	}

	/**
	 * Returns the modelID for a given modelRunId.
	 * @param modelRunId the model run ID
	 * @return the modelID for a given modelRunId, or -1 if not found.
	 */
	public int findModelId(int modelRunId)
		throws DbIoException
	{
		// Model IDs not used in CWMS.
		return Constants.undefinedIntKey;
	}

	/**
	 * Returns the maximum valid run-id for the specified model.
	 * @param modelId the ID of the model
	 * @return the maximum valid run-id for the specified model.
	 */
	public int findMaxModelRunId(int modelId)
		throws DbIoException
	{
		// Model IDs not used in CWMS.
		return Constants.undefinedIntKey;
	}

	/** @return label to use for 'limit' column in tables. */
	public String getLimitLabel() { return "Qual Code"; }

	public String flags2LimitCodes(int flags)
	{
		StringBuilder sb = new StringBuilder();
		if ((flags & CwmsFlags.SCREENED) != 0)
		{
			sb.append('S');
			if ((flags & CwmsFlags.VALIDITY_MISSING) != 0)
				sb.append('M');
			if ((flags & CwmsFlags.VALIDITY_REJECTED) != 0)
				sb.append('R');
			if ((flags & CwmsFlags.VALIDITY_QUESTIONABLE) != 0)
				sb.append('Q');
		}
		return sb.toString();
	}

	/** @return label to use for 'revision' column in tables. */
	public String getRevisionLabel() { return ""; }

	public String flags2RevisionCodes(int flags)
	{
		return null;
	}
	
	/**
	 * TSDB version 5 & above use a join with CP_COMP_DEPENDS to determine
	 * not only what the new data is, but what computations depend on it.
	 * The dependent computation IDs are stored inside each CTimeSeries.
	 */
	public DataCollection getNewDataSince(DbKey applicationId, Date sinceTime)
		throws DbIoException
	{
		// Reload the TSID cache every hour.
		if (System.currentTimeMillis() - lastTsidCacheRead > 3600000L)
		{
			lastTsidCacheRead = System.currentTimeMillis();
			TimeSeriesDAI timeSeriesDAO = this.makeTimeSeriesDAO();
			try { timeSeriesDAO.reloadTsIdCache(); }
			finally { timeSeriesDAO.close(); }
		}

		DataCollection dataCollection = new DataCollection();

		String failTimeClause =
			" and (a.FAIL_TIME is null OR "
			+ "SYSDATE - to_date("
				+ "to_char(a.FAIL_TIME,'dd-mon-yyyy hh24:mi:ss'),"
				+ "'dd-mon-yyyy hh24:mi:ss') >= 1/24)";
		//MJM: If retries are disallowed, just set the clause to nothing.
		if (!DecodesSettings.instance().retryFailedComputations)
			failTimeClause = "";
		
		String q = "select min(a.record_num) from cp_comp_tasklist a "
			+ "where a.LOADING_APPLICATION_ID = " + applicationId
			+ failTimeClause;

		int minRecNum = -1;
		try
		{
			ResultSet rs = doQuery(q);
			if (rs == null || !rs.next())
			{
				debug1("No new data for appId=" + applicationId);
				// DON'T return here. Need to check the tasklist queue file below!
				// return dataCollection;
			}
			else
				minRecNum = rs.getInt(1);
			if (rs.wasNull())
			{
				debug1("No new data for appId=" + applicationId);
				minRecNum = -1;
				// DON'T return here. Need to check the tasklist queue file below!
				// return dataCollection;
			}
		}
		catch(SQLException ex)
		{
			warning("getNewDataSince: " + ex);
			return dataCollection;
		}

		ArrayList<TasklistRec> tasklistRecs = new ArrayList<TasklistRec>();
		int numWrittenToTasklistQueue = 0;
		ArrayList<Integer> badRecs = new ArrayList<Integer>();
		TimeSeriesDAI timeSeriesDAO = this.makeTimeSeriesDAO();
		try
		{
			if (minRecNum != -1)
			{
				// MJM: It's very important to NOT join the tasklist to other
				// tables in this query. Otherwise, the join could fail, leaving
				// records in the tasklist.
				q = "select a.RECORD_NUM, a.SITE_DATATYPE_ID, ROUND(a.VALUE,8), a.START_DATE_TIME, "
				  + "a.DELETE_FLAG, a.UNIT_ID, a.VERSION_DATE, a.QUALITY_CODE, a.MODEL_RUN_ID "
				  + "from CP_COMP_TASKLIST a "
				  + "where a.LOADING_APPLICATION_ID = " + applicationId;
		
				int maxRecNum = minRecNum + 10000;
				if (maxRecNum < minRecNum)
				{
					// The 32-bit integer wrapped around. Set to max possible int.
					maxRecNum = Integer.MAX_VALUE;
				}
		
				if (sinceTime != null)
					q = q + " AND a.START_DATE_TIME > " + sqlDate(sinceTime);
				else
					q = q + " and a.record_num between " + minRecNum + " and " + maxRecNum;
		
				q = q + failTimeClause;
		
				q = q + " ORDER BY a.RECORD_NUM";
			
				ResultSet rs = doQuery(q);
				while (rs.next())
				{
					// Extract the info needed from the result set row.
					int recordNum = rs.getInt(1);
					DbKey sdi = DbKey.createDbKey(rs, 2);
					double value = rs.getDouble(3);
					boolean valueWasNull = rs.wasNull();
					Date timeStamp = getFullDate(rs, 4);
					String df = rs.getString(5);
					char c = df.toLowerCase().charAt(0);
					boolean deleted = false;
					if (c == 'u')
					{
						// msg handler will send this when he gets
						// TsCodeChanged. It tells me to update my cache.
						TimeSeriesIdentifier tsid = timeSeriesDAO.getCache().getByKey(sdi);
						if (tsid != null)
						{
							DbKey newCode = DbKey.createDbKey(rs, 9);
							timeSeriesDAO.getCache().remove(sdi);
							tsid.setKey(newCode);
							timeSeriesDAO.getCache().put(tsid);
							continue;
						}
					}
					else 
						deleted = TextUtil.str2boolean(df);
						
					String unitsAbbr = rs.getString(6);
					Date versionDate = getFullDate(rs, 7);
					BigDecimal qc = rs.getBigDecimal(8);
					long qualityCode = qc == null ? 0 : qc.longValue();
Logger.instance().debug3("Tasklist rec: sdi=" + sdi + ", valueWasNull="+valueWasNull
+", deleteFlag=" + df + ", qualityCode=" + qc + ", qc from getLong=" + rs.getLong(8));
					
					TasklistRec rec = new TasklistRec(recordNum, sdi, value,
						valueWasNull, timeStamp, deleted,
						unitsAbbr, versionDate, qualityCode);
//					lastTaskListRecords = System.currentTimeMillis();
	
//					// If we are using a tasklist queue for records older than a threshold,
//					// and this record is older than the threshold, then add this to the queue file.
//					if (tasklistQueueFile != null
//					 && System.currentTimeMillis() - timeStamp.getTime() > 
//						tasklistQueueThresholdHours * 3600000L)
//					{
//						try
//						{
//							tasklistQueueFile.writeRec(rec);
//							numWrittenToTasklistQueue++;
//							// Add recnum to badRecs list so it will be deleted right away from
//							// CP_COMP_TASKLIST.
//							badRecs.add(rec.getRecordNum());
//							continue; // skip adding to the 'tasklistRecs' array below
//						}
//						catch (IOException ex)
//						{
//							if (++numTasklistQueueErrors > 10)
//							{
//								warning("Disabling tasklist queue -- too many errors.");
//								tasklistQueueFile.close();
//								tasklistQueueFile = null;
//							}
//							else
//							{
//								String msg = "Error writing to tasklist queue file: " + ex;
//								warning(msg);
//								System.err.println(msg);
//								ex.printStackTrace(System.err);
//							}
//							// On exception in the queue, fall through & process the record now.
//						}
//					}
					tasklistRecs.add(rec);
				}
			}
			if (numWrittenToTasklistQueue > 0)
				debug1("Num written to tasklist queue: " + numWrittenToTasklistQueue);
			
			RecordRangeHandle rrhandle = new RecordRangeHandle(applicationId);
			
			// Process the real-time records collected above.
			for(TasklistRec rec : tasklistRecs)
				processTasklistEntry(rec, dataCollection, rrhandle, badRecs, applicationId);
			
//			if (tasklistRecs.isEmpty()          // No realtime recs
//			 && tasklistQueueFile != null       // We are using a queue file
//			 && numWrittenToTasklistQueue == 0) // No tasklists read into the queue
//			{
//				int n = 0;
//				TasklistRec rec = null;
//				try
//				{
//					while((rec = tasklistQueueFile.readRec()) != null && n++ < 1000)
//					{
//						processTasklistEntry(rec, dataCollection, null, null, applicationId);
//					}
//
//					// Note -- pass badRecs and rrhandle as null to tell process method that we don't 
//					// want to save the record num for later deletion, because it came out of the
//					// queue and the record is already gone from the database.
//					debug1("Extracted and processed " + n + " records from tasklist queue.");
//				}
//				catch(IOException ex)
//				{
//					warning("Error reading tasklist queue: " + ex);
//					++numTasklistQueueErrors;
//				}
//			}

			dataCollection.setTasklistHandle(rrhandle);
			
			// Delete the bad tasklist recs, 250 at a time.
			if (badRecs.size() > 0)
				Logger.instance().debug1("getNewDataSince deleting " + badRecs.size()
					+ " bad tasklist records.");
			while (badRecs.size() > 0)
			{
				StringBuilder inList = new StringBuilder();
				int n = badRecs.size();
				int x=0;
				for(; x<250 && x<n; x++)
				{
					if (x > 0)
						inList.append(", ");
					inList.append(badRecs.get(x).toString());
				}
				q = "delete from CP_COMP_TASKLIST "
					+ "where RECORD_NUM IN (" + inList.toString() + ")";
				doModify(q);
				commit();
				for(int i=0; i<x; i++)
					badRecs.remove(0);
			}
			
List<CTimeSeries> allts = dataCollection.getAllTimeSeries();
debug3("getNewData, returning " + allts.size() + " TimeSeries.");
for(CTimeSeries ts : allts)
  debug3("ts " + ts.getTimeSeriesIdentifier().getUniqueString() + " " + ts.size() + " values.");
			return dataCollection;
		}
		catch(SQLException ex)
		{
			System.err.println("Error reading new data: " + ex);
			ex.printStackTrace();
			throw new DbIoException("Error reading new data: " + ex);
		}
		finally
		{
			timeSeriesDAO.close();
		}
	}


	private void processTasklistEntry(TasklistRec rec,
		DataCollection dataCollection, RecordRangeHandle rrhandle,
		ArrayList<Integer> badRecs, DbKey applicationId)
		throws DbIoException
	{
		// Find time series if already in data collection.
		// If not construct one and add it.
		CTimeSeries cts = dataCollection.getTimeSeriesByUniqueSdi(rec.getSdi());
		if (cts == null)
		{
			TimeSeriesDAI timeSeriesDAO = this.makeTimeSeriesDAO();
			try
			{
				TimeSeriesIdentifier tsid = 
					timeSeriesDAO.getTimeSeriesIdentifier(rec.getSdi());
				String tabsel = tsid.getPart("paramtype") + "." + 
					tsid.getPart("duration") + "." + tsid.getPart("version");
				cts = new CTimeSeries(rec.getSdi(), tsid.getInterval(),
					tabsel);
				cts.setModelRunId(-1);
				cts.setTimeSeriesIdentifier(tsid);
				cts.setUnitsAbbr(rec.getUnitsAbbr());
				if (fillDependentCompIds(cts, applicationId) == 0)
				{
					warning("Deleting tasklist rec for '"
						+ tsid.getUniqueString() 
						+ "' because no dependent comps.");
					if (badRecs != null)
						badRecs.add(rec.getRecordNum());
					return;
				}

				try { dataCollection.addTimeSeries(cts); }
				catch(decodes.tsdb.DuplicateTimeSeriesException ex)
				{ // won't happen -- already verified it's not there.
				}
			}
			catch(NoSuchObjectException ex)
			{
				warning("Deleting tasklist rec for non-existent ts_code "
					+ rec.getSdi());
				if (badRecs != null)
					badRecs.add(rec.getRecordNum());
				return;
			}
			finally
			{
				timeSeriesDAO.close();
			}
		}
		else
		{
			// The time series already existed from a previous tasklist rec in this run.
			// Make sure this rec's unitsAbbr matches the CTimeSeries.getUnitsAbbr().
			// If not, convert it to the CTS units.
			String recUnitsAbbr = rec.getUnitsAbbr();
			String ctsUnitsAbbr = cts.getUnitsAbbr();
			if (ctsUnitsAbbr == null) // no units yet assigned?
				cts.setUnitsAbbr(ctsUnitsAbbr = recUnitsAbbr);
			else if (recUnitsAbbr == null) // for some reason, this tasklist record doesn't have units
				recUnitsAbbr = ctsUnitsAbbr;
			else if (!TextUtil.strEqualIgnoreCase(recUnitsAbbr, ctsUnitsAbbr))
			{
				EngineeringUnit euOld =	EngineeringUnit.getEngineeringUnit(recUnitsAbbr);
				EngineeringUnit euNew = EngineeringUnit.getEngineeringUnit(ctsUnitsAbbr);

				UnitConverter converter = Database.getDb().unitConverterSet.get(euOld, euNew);
				if (converter != null)
				{
					try { rec.setValue(converter.convert(rec.getValue())); }
					catch (Exception ex)
					{
						Logger.instance().warning(
							"Tasklist for '" + cts.getTimeSeriesIdentifier().getUniqueString()
							+ "' exception converting " + rec.getValue() + " " + rec.getUnitsAbbr()
							+ " to " + cts.getUnitsAbbr() + ": " + ex
							+ " -- will use as-is.");
					}
				}
				else
				{
					Logger.instance().warning(
						"Tasklist for '" + cts.getTimeSeriesIdentifier().getUniqueString()
						+ "' cannot convert " + rec.getValue() + " " + rec.getUnitsAbbr()
						+ " to " + cts.getUnitsAbbr() + ". -- will use as-is.");
				}
			}
		}
		if (rrhandle != null)
			rrhandle.addRecNum(rec.getRecordNum());

		// Construct timed variable with appropriate flags & add it.
		TimedVariable tv = new TimedVariable(rec.getValue());
		tv.setTime(rec.getTimeStamp());
		tv.setFlags(CwmsFlags.cwmsQuality2flag(rec.getQualityCode()));
		
		if (!rec.isDeleted() && !rec.isValueWasNull())
		{
			VarFlags.setWasAdded(tv);
			cts.addSample(tv);
			// Remember which tasklist records are in this timeseries.
			cts.addTaskListRecNum(rec.getRecordNum());
			Logger.instance().debug3("Added value " + tv + " to time series "
				+ cts.getTimeSeriesIdentifier().getUniqueString()
				+ " flags=0x" + Integer.toHexString(tv.getFlags())
				+ " cwms qualcode=0x" + Long.toHexString(rec.getQualityCode()));
		}
		else
		{
			VarFlags.setWasDeleted(tv);
			Logger.instance().warning("Discarding deleted value " + tv.toString()
				+ " for time series " + cts.getTimeSeriesIdentifier().getUniqueString()
				+ " flags=0x" + Integer.toHexString(tv.getFlags())
				+ " cwms qualcode=0x" + Long.toHexString(rec.getQualityCode()));
		}
	}

	public boolean isCwms() { return true; }

	@Override
	public String[] getTsIdParts()
	{
		return CwmsTsId.tsIdParts;
	}

	/**
	 * Recursively expand groups to find all data-descriptors under the 
	 * specified group. This method would be called by report-generator
	 * programs that are given a group and must process all time-series
	 * contained within it or within its sub-groups.
	 * @param tsGroup the top-level group to expand
	 * @return list of all data-descriptors under this group or sub-groups
	 */
	@Override
	public ArrayList<TimeSeriesIdentifier> expandTsGroup(TsGroup tsGroup)
		throws DbIoException
	{
		if (cwmsGroupHelper == null)
			cwmsGroupHelper = new CwmsGroupHelper(this);
		cwmsGroupHelper.expandTsGroupDescriptors(tsGroup);
		return tsGroup.getExpandedList();
	}

	@Override
	public TimeSeriesIdentifier transformTsidByCompParm(
			TimeSeriesIdentifier tsid, DbCompParm parm, boolean createTS,
			boolean fillInParm, String timeSeriesDisplayName)
		throws DbIoException, NoSuchObjectException, BadTimeSeriesException
	{
		if (tsid == null)
			tsid = makeEmptyTsId();

		TimeSeriesIdentifier tsidRet = tsid.copyNoKey();
		boolean transformed = transformUniqueString(tsidRet, parm);
//Site tssite = tsidRet.getSite();
//Logger.instance().debug3("After transformUniqueString, sitename=" + tsidRet.getSiteName()
//+ ", site=" + (tssite==null ? "null" : tssite.getDisplayName()));
		if (transformed)
		{
			String uniqueString = tsidRet.getUniqueString();
			debug3("CwmsTimeSeriesDb.transformTsid new string='"
				+ uniqueString);
			TimeSeriesDAI timeSeriesDAO = makeTimeSeriesDAO();

			try 
			{
				tsidRet = timeSeriesDAO.getTimeSeriesIdentifier(uniqueString);
				debug3("CwmsTimeSeriesDb.transformTsid "
					+ "time series '" + uniqueString + "' exists OK.");
			}
			catch(NoSuchObjectException ex)
			{
				if (createTS)
				{
					if (timeSeriesDisplayName != null)
						tsidRet.setDisplayName(timeSeriesDisplayName);
					timeSeriesDAO.createTimeSeries(tsidRet);
					fillInParm = true;
				}
				else
				{
					debug3("CwmsTimeSeriesDb.transformTsid "
						+ "no such time series '" + uniqueString + "'");
					return null;
				}
			}
			finally
			{
				timeSeriesDAO.close();
			}
		}
		else
			tsidRet = tsid;
		
		if (fillInParm)
		{
			parm.setSiteDataTypeId(tsidRet.getKey());
			parm.setInterval(tsidRet.getInterval());
			parm.setTableSelector(
				tsidRet.getPart("ParamType") + "."
				+ tsidRet.getPart("Duration") + "."
				+ tsidRet.getPart("Version"));
			parm.setDataType(tsidRet.getDataType());
			parm.setSite(tsidRet.getSite());
		}

		return tsidRet;
	}

	/**
	 * Overloaded from base class, transform the TSID unique string.
	 * DOES NO DATABASE IO.
	 * @param tsidRet the time-series id to transform
	 * @param parm the templeat db comp parameter
	 * @return true if changes were made.
	 */
	public boolean transformUniqueString(TimeSeriesIdentifier tsidRet,
		DbCompParm parm)
	{
		boolean transformed = false;
		SiteName sn = parm.getSiteName();
		if (sn != null)
		{
			tsidRet.setSiteName(sn.getNameValue());
			transformed = true;
			// Also lookup the site and set the ID and site object.
			SiteDAI siteDAO = makeSiteDAO();
			try
			{
				DbKey siteId = siteDAO.lookupSiteID(sn);
				tsidRet.setSite(siteDAO.getSiteById(siteId));
			}
			catch (Exception ex)
			{
				Logger.instance().warning("Cannot get site for sitename " + sn + ": " + ex);
			}
			finally
			{
				siteDAO.close();
			}
		}
		DataType dt = parm.getDataType();
		if (dt != null)
		{
			tsidRet.setDataType(dt);
			transformed = true;
		}
		String s = parm.getParamType();
		if (s != null && s.trim().length() > 0)
		{
			tsidRet.setPart("ParamType", s);
			transformed = true;
		}
		s = parm.getInterval();
		if (s != null && s.trim().length() > 0)
		{
			tsidRet.setPart("Interval", s);
			transformed = true;
		}
		s = parm.getDuration();
		if (s != null && s.trim().length() > 0)
		{
			tsidRet.setPart("Duration", s);
			transformed = true;
		}
		s = parm.getVersion();
		if (s != null && s.trim().length() > 0)
		{
			tsidRet.setPart("Version", s);
			transformed = true;
		}
		return transformed;
	}
	
	
	public String getDbOfficeId()
	{
		return dbOfficeId;
	}

	/**
	 * @return the parameter types as String[]
	 */
	@Override
	public String[] getParamTypes() throws DbIoException
	{
		String retStr[];

		try
		{
			ArrayList<String> ret = listParamTypes();
			retStr = new String[ret.size()];
			for (int i=0; i<ret.size(); i++)
				retStr[i] = ret.get(i);
		} catch(DbIoException ex)
		{
			retStr = null;
			throw new DbIoException("CwmsTimeSeriesDb.getParamTypes: " + ex);
		}
		return retStr;
	}
	
	public ArrayList<String> listParamTypes()
		throws DbIoException
	{
		ArrayList<String> ret = new ArrayList<String>();
		// TODO: Replace this with a call to the CATALOG function for Param Types
		String q = "select distinct parameter_type_id FROM CWMS_V_TS_ID"
			+ " WHERE upper(DB_OFFICE_ID) = " + sqlString(dbOfficeId.toUpperCase());

		ResultSet rs = doQuery(q);
		try
		{
			while (rs != null && rs.next())
				ret.add(rs.getString(1));
		}
		catch (SQLException ex)
		{
			throw new DbIoException("CwmsTimeSeriesDb.listParamTypes: " + ex);
		}

		// MJM - these are the ones we know about for sure:
		if (!ret.contains("Inst"))
			ret.add("Inst");
		if (!ret.contains("Ave"))
			ret.add("Ave");
		if (!ret.contains("Max"))
			ret.add("Max");
		if (!ret.contains("Min"))
			ret.add("Min");
		if (!ret.contains("Total"))
			ret.add("Total");
		return ret;
		
	}
	
	@Override
	public String[] getValidPartChoices(String part)
	{
		if (part.equalsIgnoreCase("version"))
			return currentlyUsedVersions;
		return super.getValidPartChoices(part);
	}
	
	public void printCat()
	{
		try
		{
			CwmsCatJdbc cwmsCatJdbc = new CwmsCatJdbc(getConnection());

			ResultSet rs = cwmsCatJdbc.catTsId(dbOfficeId, null, null, null, null, null);
			while(rs != null && rs.next())
			{
				System.out.println(rs.getString(2).trim() + ", active=" + rs.getString(5));
			}
		}
		catch(Exception ex)
		{
			System.err.println("Error: " + ex);
			ex.printStackTrace(System.err);
		}
		
	}
	
// CWMS-3010 Don't overload this method. The base class does the write and then
// recursively re-evaluates any computations that depend on this group.
//	/**
//	 * Writes a group to the database.
//	 * @param group the group
//	 */
//	public void writeTsGroup(TsGroup group)
//		throws DbIoException
//	{
//		if (groupHelper == null)
//			groupHelper = new GroupHelper(this);
//
//		groupHelper.writeTsGroup(group);
//	}

	@Override
	public TimeSeriesIdentifier makeEmptyTsId()
	{
		return new CwmsTsId();
	}
	
	
	/**
	 * Reset the VPD context variable with user specified office Id
	 * @throws DbIoException 
	 */
	public static void setCtxDbOfficeId(Connection conn, String dbOfficeId,
		DbKey dbOfficeCode, String dbOfficePrivilege, int tsdbVersion)
		throws DbIoException
	{
		String errMsg = null;
		OraclePreparedStatement storeProcStmt = null;
		
		try
		{
			String q = null;
			if (tsdbVersion >= TsdbDatabaseVersion.VERSION_9)
			{
				int privLevel = 
					dbOfficeId == null ? 0 :
					dbOfficePrivilege.toUpperCase().contains("MGR") ? 1 :
					dbOfficePrivilege.toUpperCase().contains("PROC") ? 2 : 3;
				q = 
					"begin cwms_ccp_vpd.set_ccp_session_ctx(" +
					":1 /* office code */, :2 /* priv level*/, :3 /* officeId */); end;";
				storeProcStmt  = (OraclePreparedStatement)conn.prepareStatement(q);
				storeProcStmt.setInt(1, (int)dbOfficeCode.getValue());
				storeProcStmt.setInt(2, privLevel);
				storeProcStmt.setString(3, dbOfficeId);
				Logger.instance().debug1("Executing '" + q + "' with "
					+ "dbOfficeCode=" + dbOfficeCode
					+ ", privLevel=" + privLevel
					+ ", dbOfficeId='" + dbOfficeId + "'");
			}
			else
			{
				q = "begin cwms_ccp_vpd.set_session_office_id(:1  /*Office ID */ ); end;"; 
				storeProcStmt  = (OraclePreparedStatement)conn.prepareStatement(q);
				storeProcStmt.setString(1, dbOfficeId);
				Logger.instance().debug1("Executing '" + q + "' with "
					+ "dbOfficeId='" + dbOfficeId + "'");
			}
			storeProcStmt.execute();
			conn.commit();
		}
		catch (SQLException ex)
		{
			errMsg = "Error setting VPD context for '" + dbOfficeId + "': " + ex;
			Logger.instance().failure(errMsg);
			throw new DbIoException(errMsg);
		}
		finally
		{
			if (storeProcStmt != null)
			{
				try { storeProcStmt.close(); }
				catch(Exception ex) {}
			}
		}
		
	}
	
	/**
	 * Default implementation does nothing. Tasklist queuing must be handled
	 * by the underlying subclass.
	 */
	public void useTasklistQueue(TasklistQueueFile tqf, int thresholdHours)
	{
//		this.tasklistQueueFile = tqf;
//		this.tasklistQueueThresholdHours = thresholdHours;
	}
	
	public void closeTasklistQueue()
	{
//		if (tasklistQueueFile != null)
//			tasklistQueueFile.close();
//		tasklistQueueFile = null;
	}

	/**
	 * Use database-specific flag definitions to determine whether the
	 * passed variable should be considered 'questionable'.
	 * @param v the variable whose flags to check
	 * @return true if flags are questionable, false if okay.
	 */
	public boolean isQuestionable(NamedVariable v)
	{
		return (v.getFlags() & CwmsFlags.VALIDITY_MASK) == CwmsFlags.VALIDITY_QUESTIONABLE;
	}
	
	/**
	 * Use database-specific flag definitions to set the passed variable
	 * as 'questionable'.
	 * @param v the variable whose flags to set
	 */
	public void setQuestionable(Variable v)
	{
		v.setFlags( (v.getFlags() & (~CwmsFlags.VALIDITY_MASK)) | CwmsFlags.VALIDITY_QUESTIONABLE);
	}

	public boolean isOracle()
	{
		return true;
	}

	@Override
	public SiteDAI makeSiteDAO()
	{
		return new CwmsSiteDAO(this, dbOfficeId);
	}

	@Override
	public TimeSeriesDAI makeTimeSeriesDAO()
	{
		return new CwmsTimeSeriesDAO(this, dbOfficeId);
	}

	@Override
	public IntervalDAI makeIntervalDAO()
	{
		return new CwmsIntervalDAO(this, dbOfficeId);
	}

	@Override
	public ScheduleEntryDAI makeScheduleEntryDAO()
	{
		return null;
	}

	public static int determineCwmsSchemaVersion(Connection con, int tsdbVersion)
	{
		String q = "select count(*) from all_synonyms where owner='PUBLIC' " +
			"and SYNONYM_NAME = 'CWMS_ENV'";
		ResultSet rs = null;
		Statement stmt = null;
		try
        {
			stmt = con.createStatement();
			rs = stmt.executeQuery(q);
			if (rs.next())
			{
				int count = rs.getInt(1);
				if (count == 0)
					return CWMS_V_2_1;
				Logger.instance().debug3("Response to '" + q + "' is " + count
					+ ", tsdbVersion=" + tsdbVersion);
				if (tsdbVersion <= TsdbDatabaseVersion.VERSION_8)
				{
					return CWMS_V_2_2;
				}
				else
					return CWMS_V_3_0;
			}
			Logger.instance().warning("Cannot determine CWMS Version. No results to query '"
				+ q + "' -- assuming 3.0");
			return CWMS_V_3_0;
		}
		catch (Exception ex)
		{
			Logger.instance().warning("Error Determing Schema Version: " + ex
			+ " -- assuming 3.0");
			return CWMS_V_3_0;
		}
		finally
		{
			if (rs != null)
			{
				try { rs.close(); } catch(Exception ex) {}
			}
			if (stmt != null)
			{
				try { stmt.close(); } catch(Exception ex) {}
			}
		}
	}
	
	/**
	 * Converts a CWMS String Office ID to the numeric office Code.
	 * @param con the Connection
	 * @param officeId the String office ID
	 * @return the office code as a DbKey or Constants.undefinedId if no match.
	 */
	public static DbKey officeId2code(Connection con, String officeId)
	{
		String q = "select cwms_util.get_office_code('" +
				officeId + "') from dual";
			
		ResultSet rs = null;
		Statement stmt = null;
		try
        {
			stmt = con.createStatement();
			rs = stmt.executeQuery(q);
			Logger.instance().debug3(q);
			if (rs.next())
				return DbKey.createDbKey(rs, 1);
			else
				return Constants.undefinedId;
        }
		catch (Exception ex)
		{
			Logger.instance().warning("Error getting office code for id '" 
				+ officeId + "': " + ex);
			return Constants.undefinedId;
		}
		finally
		{
			if (rs != null) { try { rs.close(); } catch(Exception ex) {} }
			if (stmt != null) { try { stmt.close(); } catch(Exception ex) {} }
		}
	}

	public int getCwmsSchemaVersion()
	{
		return cwmsSchemaVersion;
	}

	public void setDbUri(String dbUri)
	{
		this.dbUri = dbUri;
	}

	public void setJdbcOracleDriver(String jdbcOracleDriver)
	{
		this.jdbcOracleDriver = jdbcOracleDriver;
	}

	public DbKey getDbOfficeCode()
	{
		return dbOfficeCode;
	}

	public BaseParam getBaseParam()
	{
		return baseParam;
	}

	public ScreeningDAI makeScreeningDAO() 
		throws DbIoException
	{
		return new ScreeningDAO(this);
	}
}
