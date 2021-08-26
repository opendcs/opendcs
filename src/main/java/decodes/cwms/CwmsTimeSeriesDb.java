/*
*
* $Id: CwmsTimeSeriesDb.java,v 1.57 2020/05/02 12:44:06 mmaloney Exp $
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
*  $Log: CwmsTimeSeriesDb.java,v $
*  Revision 1.57  2020/05/02 12:44:06  mmaloney
*  Add stack trace on connection failure.
*
*  Revision 1.56  2019/12/11 14:35:32  mmaloney
*  set module in ctor.
*
*  Revision 1.55  2019/07/02 13:53:26  mmaloney
*  Added flags2display
*
*  Revision 1.54  2019/06/10 19:22:17  mmaloney
*  Added getStorageUnitsForDataType
*
*  Revision 1.53  2019/03/15 11:58:54  mmaloney
*  dev
*
*  Revision 1.52  2019/02/25 20:02:55  mmaloney
*  HDB 660 Allow Computation Parameter Site and Datatype to be set independently in group comps.
*
*  Revision 1.51  2019/02/19 13:00:49  mmaloney
*  Add Michael Neilson's improvement for CWMS-14213
*
*  Revision 1.50  2019/01/29 16:45:17  mmaloney
*  dev
*
*  Revision 1.49  2019/01/18 15:06:48  mmaloney
*  dev
*
*  Revision 1.48  2019/01/11 14:39:17  mmaloney
*  Move JavaLoggerAdapter to ApplicationSettings
*
*  Revision 1.47  2019/01/03 15:04:55  mmaloney
*  changed prepareStatement to prepareCall
*
*  Revision 1.46  2018/12/21 17:26:21  mmaloney
*  dev
*
*  Revision 1.45  2018/12/21 17:09:01  mmaloney
*  dev
*
*  Revision 1.44  2018/12/21 17:00:45  mmaloney
*  dev
*
*  Revision 1.43  2018/12/20 21:25:11  mmaloney
*  dev
*
*  Revision 1.42  2018/12/20 15:52:21  mmaloney
*  dev
*
*  Revision 1.41  2018/12/18 20:48:52  mmaloney
*  dev
*
*  Revision 1.40  2018/12/18 16:15:06  mmaloney
*  Only capture specific loggers, otherwise you get tons of messages from X.
*
*  Revision 1.39  2018/12/18 15:21:02  mmaloney
*  Updates for jOOQ
*
*  Revision 1.38  2018/12/17 16:11:18  mmaloney
*  jOOQ Mods
*
*  Revision 1.37  2018/12/05 20:17:11  mmaloney
*  Use new connection pool to obtain connection.
*
*  Revision 1.36  2018/12/04 21:46:34  mmaloney
*  Removed unneeded import.
*
*  Revision 1.35  2018/11/28 21:18:48  mmaloney
*  CWMS JOOQ Migration Mods
*
*  Revision 1.34  2018/09/11 21:32:16  mmaloney
*  Modify morph() method to allow a way to create a mask that specifies to lop off the remainder of a parameter.
*
*  Revision 1.33  2018/05/23 19:59:01  mmaloney
*  OpenTSDB Initial Release
*
*  Revision 1.32  2018/05/01 17:34:13  mmaloney
*  Code cleanup
*
*  Revision 1.31  2018/02/21 14:34:19  mmaloney
*  Set autocommit true always.
*
*  Revision 1.30  2018/02/21 14:33:03  mmaloney
*  Set autocommit true always.
*
*  Revision 1.29  2018/02/19 16:22:30  mmaloney
*  Attempt to reclaim tasklist space if tasklist is empty and feature is enabled.
*
*  Revision 1.28  2018/02/14 17:02:37  mmaloney
*  CWMS-11849 Use prepared statement for the 2 queries that read the tasklist.
*
*  Revision 1.27  2017/11/14 21:50:11  mmaloney
*  Handle out of range ratings that return Const.UNDEFINED_DOUBLE.
*
*  Revision 1.26  2017/11/14 20:46:48  mmaloney
*  Handle out of range ratings that return Const.UNDEFINED_DOUBLE.
*
*  Revision 1.25  2017/08/22 19:31:18  mmaloney
*  Improve comments
*
*  Revision 1.24  2017/05/31 21:18:40  mmaloney
*  Added rating method to the TSDB object in order to remove dependencies to CWMS
*  from PythonAlgorithm.
*
*  Revision 1.23  2017/05/17 20:43:29  mmaloney
*  Remove ref to CwmsCatJdbc
*
*  Revision 1.22  2017/01/24 15:48:18  mmaloney
*  CWMS-9908 allow locationOverride to contain wildcard * char. Use same rules
*  as in the location part of the parameter mask.
*
*  Revision 1.21  2017/01/11 14:09:14  mmaloney
*  CompEdit CompParmDialog Lookup time Series should allow wildcards in the site name
*  for CWMS.
*
*  Revision 1.20  2017/01/10 21:46:11  mmaloney
*  Enhanced wildcard processing for CWMS as per punchlist for comp-depends project
*  for NWP.
*
*  Revision 1.19  2016/12/16 14:20:27  mmaloney
*  Enhanced resolver to allow triggering from a time series with unrelated location.
*
*  Revision 1.18  2016/11/29 00:56:02  mmaloney
*  Mods to transformUniqueString to handle wildcards.
*
*  Revision 1.17  2016/11/19 15:58:02  mmaloney
*  Support wildcards
*
*  Revision 1.16  2016/11/03 18:59:40  mmaloney
*  Implement wildcard evaluation for groups.
*
*  Revision 1.15  2016/08/13 17:40:31  mmaloney
*  DecodesSetting.cwmsVersionOverride to bypass Cwms 3 office privilege checks.
*
*  Revision 1.14  2016/08/05 14:43:37  mmaloney
*  cwmsVersionOverride to account for the fact that on some versions of CWMS there is no
*  reliable programmatic way to determine CWMS 2.1 vs CWMS 3.
*
*  Revision 1.13  2016/04/22 14:46:51  mmaloney
*  remove debug.
*
*  Revision 1.12  2016/03/24 19:00:45  mmaloney
*  Refactor: Have expandSDI return the TimeSeriesID that it uses. This saves the caller from
*  having to re-look it up. Needed for PythonAlgorithm.
*
*  Revision 1.11  2016/01/27 21:41:27  mmaloney
*  schedule_entry_status and dacq_event have their own sequences.
*
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
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.TimeZone;

import opendcs.dai.DataTypeDAI;
import opendcs.dai.IntervalDAI;
import opendcs.dai.ScheduleEntryDAI;
import opendcs.dai.SiteDAI;
import opendcs.dai.TimeSeriesDAI;
import opendcs.dao.DbObjectCache;
import lrgs.gui.DecodesInterface;

import java.sql.PreparedStatement;

import usace.cwms.db.dao.ifc.sec.CwmsDbSec;
import usace.cwms.db.dao.util.connection.ConnectionLoginInfo;
import usace.cwms.db.dao.util.connection.ConnectionLoginInfoImpl;
import usace.cwms.db.dao.util.connection.CwmsDbConnectionPool;
import usace.cwms.db.dao.util.services.CwmsDbServiceLookup;
import hec.data.RatingException;
import hec.data.cwmsRating.RatingSet;
import hec.lang.Const;
import ilex.util.Logger;
import ilex.util.StringPair;
import ilex.util.TextUtil;
import ilex.var.NamedVariable;
import ilex.var.TimedVariable;
import ilex.var.Variable;
import decodes.cwms.rating.CwmsRatingDao;
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
import decodes.sql.DecodesDatabaseVersion;
import decodes.sql.OracleSequenceKeyGenerator;
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
	private String dbOfficeId = null;
	private DbKey dbOfficeCode = Constants.undefinedId;

	private String[] currentlyUsedVersions = { "" };
	GregorianCalendar saveTsCal = new GregorianCalendar(
		TimeZone.getTimeZone("UTC"));

	CwmsGroupHelper cwmsGroupHelper = null;

	public boolean requireCcpTables = true;

	private String dbUri = null;

	private BaseParam baseParam = new BaseParam();
	private PreparedStatement getMinStmt = null, getTaskListStmt;
	String getMinStmtQuery = null, getTaskListStmtQuery = null;


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
		module = "CwmsTimeSeriesDb";
	}

	public static CwmsConnectionInfo getDbConnection(String dbUri, String username, String password, String dbOfficeId)
		throws BadConnectException
	{
		if (dbOfficeId == null)
			dbOfficeId = DecodesSettings.instance().CwmsOfficeId;

		CwmsConnectionInfo ret = new CwmsConnectionInfo();

		// Make a call to the new connection pool.
//		System.setProperty("oracle.jdbc.autoCommitSpecCompliant", "false");
//System.err.println("Connecting to '" + dbUri + "' as '" + username + "' with pw '" + password + "' and office '" + dbOfficeId + "'");
		ConnectionLoginInfo loginInfo = new ConnectionLoginInfoImpl(dbUri, username, password, dbOfficeId);
		CwmsDbConnectionPool connectionPool = CwmsDbConnectionPool.getInstance();
		try
		{
			ret.setConnection(connectionPool.getConnection(loginInfo));
		}
		catch (SQLException ex)
		{
			if (ret.getConnection() != null)
				try { CwmsDbConnectionPool.close(ret.getConnection()); } catch(Exception ex2) {}
			ret.setConnection(null);
			String msg = "Cannot get CWMS connection for user '" + username
				+ "', officeId='" + dbOfficeId + "' and dbURI='" + dbUri + ": " + ex;
			Logger.instance().failure(msg);
			PrintStream ps = Logger.instance().getLogOutput();
			if (ps != null)
				ex.printStackTrace(ps);
			else
				ex.printStackTrace(System.err);
			throw new BadConnectException(msg);
		}

		// MJM 2018-2/21 Force autoCommit on.
		try{ ret.getConnection().setAutoCommit(true); }
		catch(SQLException ex)
		{
			Logger.instance().warning("Cannot set SQL AutoCommit to true: " + ex);
		}

		// After connection set the context for VPD to work.
		ret.setDbOfficeCode(officeId2code(ret.getConnection(), dbOfficeId));

		ArrayList<StringPair> officePrivileges = null;
		String dbOfficePrivilege = null;
		try
		{
			officePrivileges = determinePrivilegedOfficeIds(ret.getConnection());
			// MJM 2018-12-05 now determine the highest privilege level that this user has in
			// the specified office ID:
Logger.instance().debug3("Office Privileges for user '" + username + "'");
			for(StringPair op : officePrivileges)
			{
				if (op == null)
				{
					Logger.instance().warning("Skipping null privilege string pair!");
					continue;
				}
				if (op.first == null)
				{
					Logger.instance().warning("Skipping null op.first privilege string pair!");
					continue;
				}
				if (op.second == null)
				{
					Logger.instance().warning("Skipping null op.first privilege string pair!");
					continue;
				}
				Logger.instance().debug3("Privilege: " + op.first + ":" + op.second);

				String priv = op.second.toLowerCase();
				if (TextUtil.strEqualIgnoreCase(op.first, dbOfficeId) && priv.startsWith("ccp"))
				{
					if (priv.contains("mgr"))
					{
						dbOfficePrivilege = op.second;
						break;
					}
					else if (priv.contains("proc"))
					{
						if (dbOfficePrivilege == null || !dbOfficePrivilege.toLowerCase().contains("mgr"))
							dbOfficePrivilege = op.second;
					}
					else if (dbOfficePrivilege == null)
						dbOfficePrivilege = op.second;
				}
			}
		}
		catch (Exception ex)
		{
			String msg = "Cannot determine privileged office IDs: " + ex;
			System.err.println(msg);
			ex.printStackTrace(System.err);
			Logger.instance().failure(msg);
			throw new BadConnectException(msg);
		}
		try
		{
			setCtxDbOfficeId(ret.getConnection(), dbOfficeId, ret.getDbOfficeCode(), dbOfficePrivilege);
		}
		catch (Exception ex)
		{
			String msg = "Cannot set VPD context username/officeId username='" + username
				+ "', officeId='" + dbOfficeId + "'";
			Logger.instance().failure(msg);
			try { CwmsDbConnectionPool.close(ret.getConnection()); } catch(Exception ex2) {}
			throw new BadConnectException(msg);
		}

		return ret;
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
		String dbUri = this.dbUri != null ? this.dbUri : DecodesSettings.instance().editDatabaseLocation;

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

		// MJM 2018-12-05 The new HEC/RMA connection facility requires that office ID
		// be known before getting a connection from the pool. Therefore I cannot set
		// it dynamically from the database or from user selection.
		dbOfficeId = DecodesSettings.instance().CwmsOfficeId;

		// CWMS is Always GMT.
		DecodesSettings.instance().sqlTimeZone = "GMT";

		try
		{
			CwmsConnectionInfo conInfo = getDbConnection(dbUri, username, password, dbOfficeId);
			setConnection(conInfo.getConnection());
			this.dbOfficeCode = conInfo.getDbOfficeCode();
			postConnectInit(appName);
			cgl.setLoginSuccess(true);
		}
		catch(BadConnectException ex)
		{
			String msg = "Cannot connect to database: " + ex;
			failure(msg);
			System.err.println(msg);
			ex.printStackTrace(System.err);
			cgl.setLoginSuccess(false);
			if (getConnection() != null)
				closeConnection();
		}

		// CWMS OPENDCS-16 for DB version >= 68, use old OracleSequenceKeyGenerator,
		// which assumes a separate sequence for each table. Do not use CWMS_SEQ for anything.
		int decodesDbVersion = getDecodesDatabaseVersion();
		keyGenerator = decodesDbVersion >= DecodesDatabaseVersion.DECODES_DB_68 ?
				new OracleSequenceKeyGenerator() :
				new CwmsSequenceKeyGenerator(decodesDbVersion);

		ResultSet rs = null;
		String q = "";
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
			" Connected to DECODES CWMS Database " + dbUri + " as user " + username
			+ " with officeID=" + dbOfficeId + " (dbOfficeCode=" + dbOfficeCode + ")");

		cgl.setLoginSuccess(true);

		try
		{
			hec.data.Units.getAvailableUnits();
		}
		catch (Exception ex)
		{
			Logger.instance().warning(module + " Exception in hec.data.Units.getAvailableUnits: " + ex);
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

		return appId;
	}


	/**
	 * Fills in the internal list of privileged office IDs.
	 * @return array of string pairs: officeId,Privilege
	 * @throws SQLException
	 */
	public static ArrayList<StringPair> determinePrivilegedOfficeIds(Connection conn)
		throws SQLException
	{

		CwmsDbSec dbSec = CwmsDbServiceLookup.buildCwmsDb(CwmsDbSec.class, conn);
		ResultSet rs = dbSec.getAssignedPrivGroups(conn, null);

		ArrayList<StringPair> ret = new ArrayList<StringPair>();
//
//		CwmsSecJdbc cwmsSec = new CwmsSecJdbc(conn);
		// 4/8/13 phone call with Pete Morris - call with Null. and the columns returned are:
		// username, user_db_office_id, db_office_id, user_group_type, user_group_owner, user_group_id,
		// is_member, user_group_desc
		while(rs != null && rs.next())
		{
			String username = rs.getString(1);
			String db_office_id = null;
			String user_group_id = null;
//			if (cwmsSchemaVersion <= CWMS_V_2_2)
//			{
//				db_office_id = rs.getString(3);
//				user_group_id = rs.getString(6);
//			}
//			else // CWMS 3.0 or later
//			{
				db_office_id = rs.getString(2);
				user_group_id = rs.getString(5);
//			}
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

	@Override
	public TimeSeriesIdentifier expandSDI(DbCompParm parm)
		throws DbIoException, NoSuchObjectException
	{
		DbKey sdi = parm.getSiteDataTypeId();
		DbKey siteId = parm.getSiteId();
		DbKey datatypeId = parm.getDataTypeId();

		TimeSeriesDAI timeSeriesDAO = makeTimeSeriesDAO();
		TimeSeriesIdentifier tsid = null;
		try
		{
			if (!DbKey.isNull(sdi))
			{
				tsid = timeSeriesDAO.getTimeSeriesIdentifier(parm.getSiteDataTypeId());
				parm.setSite(tsid.getSite());
				parm.setDataType(tsid.getDataType());
			}
			else
			{
				if (!DbKey.isNull(siteId))
					parm.setSite(this.getSiteById(siteId));
				if (!DbKey.isNull(datatypeId))
					parm.setDataType(DataType.getDataType(datatypeId));
			}
		}
		finally
		{
			timeSeriesDAO.close();
		}

		return tsid;
	}

	/**
	 * CWMS TSDB stores ParamType.Duration.Version in the tab selector.
	 */
	public String getTableSelectorLabel()
	{
		return "Type.Dur.Version";
	}

	/**
	 * For CWMS we show all 5 path components for the site.
	 *
	 * {@inheritDoc}
	 */
	@Override
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

	/** @return label to use for 'limit' column in tables. */
	public String getLimitLabel() { return "Qual Code"; }

	@Override
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

	/**
	 * Releases triggers associated with the new data in the passed collection.
	 * The implementation may use information contained in the collection's
	 * opaque handle.
	 * @param dc the data collection to be released.
	 */

	@Override
	public void releaseNewData(DataCollection dc)
		throws DbIoException
	{
		RecordRangeHandle rrh = dc.getTasklistHandle();
		if (rrh == null)
			return;

		int maxRetries = DecodesSettings.instance().maxComputationRetries;
		boolean doRetryFailed = DecodesSettings.instance().retryFailedComputations;


		try(
			PreparedStatement deleteNormal = conn.prepareStatement("delete from CP_COMP_TASKLIST where RECORD_NUM = ?");
			PreparedStatement deleteFailedAfterMaxRetries = conn.prepareStatement(
					  "delete from CP_COMP_TASKLIST "
					+ "where RECORD_NUM = ? " // failRecList
					+ "and (( current_timestamp - DATE_TIME_LOADED) > " // curTimeName
					+ "INTERVAL '? hour')" ); //String.format(maxCompRetryTimeFrmt, maxRetries) + ")"); //
			PreparedStatement updateFailedRetry = conn.prepareStatement(
				"update CP_COMP_TASKLIST set FAIL_TIME = ? where RECORD_NUM = ? and "
			+	"((current_timestamp - DATE_TIME_LOADED) <= INTERVAL '? hour')");
			PreparedStatement updateFailTime = conn.prepareStatement("UPDATE CP_COMP_TASKLIST SET FAIL_TIME = current_timestamp where record_num = ?");


		){
			while(rrh.size() > 0)
			{
				String []records = rrh.getRecNumList(250).split(",");
				for( String rec: records ){
					if( "".equalsIgnoreCase(rec) ) continue;
					deleteNormal.setLong(1, Long.parseLong(rec.trim()));
					deleteNormal.addBatch();
				}

				deleteNormal.executeBatch();
			}

			while(rrh.getFailedRecnums().size() > 0)
			{
				String failRecNumList = rrh.getFailedRecNumList(250);
				String records[] = failRecNumList.split(",");
				//Array failRecs = conn.createArrayOf("integer", failRecNumList.split(","));
				// Add the retry limit for failed computations
				if (doRetryFailed && maxRetries > 0)
				{
					debug3("updating failed records based on retry count");
					for( String rec: records ){
						if( "".equalsIgnoreCase(rec) ) continue;
						deleteFailedAfterMaxRetries.setLong(1,Long.parseLong(rec.trim()));
						//deleteFailedAfterMaxRetries.setString(2,curTimeName);
						deleteFailedAfterMaxRetries.setInt(2,maxRetries);
						deleteFailedAfterMaxRetries.addBatch();
					}
					deleteFailedAfterMaxRetries.executeBatch();
					info("deleted failed recs past retry in (" + failRecNumList +")" );
					for( String rec: records){
						if( "".equalsIgnoreCase(rec) ) continue;
						//updateFailedRetry.setString(1,curTimeName);
						updateFailedRetry.setLong(1,Long.parseLong(rec.trim()));
						updateFailedRetry.setInt(2, maxRetries);
						updateFailedRetry.addBatch();

					}
					updateFailedRetry.executeBatch();
					info("updated fail time on (" + failRecNumList + "))" );
				}
				else
				{
					// DB V5 handles failed computations by setting a FAIL_TIME
					// on the task list record. Previous version just delete record.
					if( tsdbVersion >= 4 && doRetryFailed ) {
						debug3("updating failed records");
						for( String rec: records ){
							//updateFailTime.setString(1, curTimeName);
							updateFailTime.setLong(1, Long.parseLong(rec.trim()));
							//updateFailTime.setArray(2, failRecs);
							updateFailTime.execute();
						}
						updateFailTime.executeBatch();
						info("updated fail time on (" + failRecNumList + "))" );
					} else {
						for( String rec: records ){
							if( "".equalsIgnoreCase(rec) ) continue;
							deleteNormal.setLong(1, Long.parseLong(rec.trim()));
							deleteNormal.addBatch();
						}

						deleteNormal.executeBatch();
						info("deleted failed records: (" +failRecNumList +" )" );
					}
					commit();
				}
			}

		} catch( SQLException err ){
					//warning(err.getLocalizedMessage());
					StringWriter sw = new StringWriter();
					PrintWriter pw = new PrintWriter(sw);
					err.printStackTrace(pw);
					warning("removing items from task list failed:");
					warning(sw.toString());
					throw new DbIoException(err.getLocalizedMessage());
		}



	}


	/**
	 * TSDB version 5 & above use a join with CP_COMP_DEPENDS to determine
	 * not only what the new data is, but what computations depend on it.
	 * The dependent computation IDs are stored inside each CTimeSeries.
	 */
	@Override
	public DataCollection getNewData(DbKey applicationId)
		throws DbIoException
	{
		debug3("CWMS GET NEW DATA");
		// Reload the TSID cache every hour.
		if (System.currentTimeMillis() - lastTsidCacheRead > 3600000L)
		{
			lastTsidCacheRead = System.currentTimeMillis();
			TimeSeriesDAI timeSeriesDAO = this.makeTimeSeriesDAO();
			try { timeSeriesDAO.reloadTsIdCache(); }
			finally { timeSeriesDAO.close(); }
		}

		DataCollection dataCollection = new DataCollection();

		// MJM 2/14/18 - From Dave Portin. Original failTimeClause was:
		//		" and (a.FAIL_TIME is null OR "
		//		+ "SYSDATE - to_date("
		//		+ "to_char(a.FAIL_TIME,'dd-mon-yyyy hh24:mi:ss'),"
		//		+ "'dd-mon-yyyy hh24:mi:ss') >= 1/24)";

		int minRecNum = -1;
		try
		{
			if (getMinStmt == null)
			{
				// 1st query gets min record num so that I can do a range query afterward.
				String failTimeClause =
					DecodesSettings.instance().retryFailedComputations
					? " and (a.FAIL_TIME is null OR SYSDATE - a.FAIL_TIME >= 1/24)"
					: "";

				getMinStmtQuery = "select min(a.record_num) from cp_comp_tasklist a "
					+ "where a.LOADING_APPLICATION_ID = ? "// + applicationId
					+ failTimeClause;
				getMinStmt = conn.prepareStatement(getMinStmtQuery);


				// 2nd query gets tasklist recs within record_num range.
				getTaskListStmtQuery =
					"select a.RECORD_NUM, a.SITE_DATATYPE_ID, ROUND(a.VALUE,8), a.START_DATE_TIME, "
					+ "a.DELETE_FLAG, a.UNIT_ID, a.VERSION_DATE, a.QUALITY_CODE, a.MODEL_RUN_ID "
					+ "from CP_COMP_TASKLIST a "
					+ "where a.LOADING_APPLICATION_ID = ?" // + applicationId
					+ " and ROWNUM < 20000"
					+ failTimeClause
					+ " ORDER BY a.site_datatype_id, a.start_date_time";
				getTaskListStmt = conn.prepareStatement(getTaskListStmtQuery);

			}

			// this may seems silly, but it allows Oracle to cache the query and it's plan for all instances
			getMinStmt.setLong(1,applicationId.getValue());
			getTaskListStmt.setLong(1,applicationId.getValue());

			debug3("Executing prepared stmt2 '" + getMinStmtQuery + "'");
			ResultSet rs = getMinStmt.executeQuery();

			if (rs == null || !rs.next())
			{
				debug1("No new data for appId=" + applicationId);
				reclaimTasklistSpace();
				return dataCollection;
			}
			else
				minRecNum = rs.getInt(1);
			if (rs.wasNull())
			{
				debug1("No new data for appId=" + applicationId);
				minRecNum = -1;
				reclaimTasklistSpace();
				return dataCollection;
			}
		}
		catch(SQLException ex)
		{
			warning("getNewDataSince: " + ex);
			return dataCollection;
		}

		ArrayList<TasklistRec> tasklistRecs = new ArrayList<TasklistRec>();
		ArrayList<Integer> badRecs = new ArrayList<Integer>();
		TimeSeriesDAI timeSeriesDAO = this.makeTimeSeriesDAO();
		try
		{
//			getTaskListStmt.setInt(1, minRecNum);
//
//			int maxRecNum = minRecNum + 10000;
//			if (maxRecNum < minRecNum)
//			{
//				// The 32-bit integer wrapped around. Set to max possible int.
//				maxRecNum = Integer.MAX_VALUE;
//			}
//
//			getTaskListStmt.setInt(2, maxRecNum);

			debug3("Executing '" + getTaskListStmtQuery + "'");
			ResultSet rs = getTaskListStmt.executeQuery();
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
					DbObjectCache<TimeSeriesIdentifier> cache = timeSeriesDAO.getCache();
					synchronized(cache)
					{
						TimeSeriesIdentifier tsid = cache.getByKey(sdi);
						if (tsid != null)
						{
							DbKey newCode = DbKey.createDbKey(rs, 9);
							cache.remove(sdi);
							tsid.setKey(newCode);
							cache.put(tsid);
							continue;
						}
					}
				}
				else
					deleted = TextUtil.str2boolean(df);

				String unitsAbbr = rs.getString(6);
				Date versionDate = getFullDate(rs, 7);
				BigDecimal qc = rs.getBigDecimal(8);
				long qualityCode = qc == null ? 0 : qc.longValue();

				TasklistRec rec = new TasklistRec(recordNum, sdi, value,
					valueWasNull, timeStamp, deleted,
					unitsAbbr, versionDate, qualityCode);
				tasklistRecs.add(rec);
			}

			RecordRangeHandle rrhandle = new RecordRangeHandle(applicationId);

			// Process the real-time records collected above.
			for(TasklistRec rec : tasklistRecs)
				processTasklistEntry(rec, dataCollection, rrhandle, badRecs, applicationId);

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
				String q = "delete from CP_COMP_TASKLIST "
					+ "where RECORD_NUM IN (" + inList.toString() + ")";
				doModify(q);
//				commit();
				for(int i=0; i<x; i++)
					badRecs.remove(0);
			}

			// Show each tasklist entry in the log if we're at debug level 3
			if (Logger.instance().getMinLogPriority() <= Logger.E_DEBUG3)
			{
				List<CTimeSeries> allts = dataCollection.getAllTimeSeries();
				debug3("getNewData, returning " + allts.size() + " TimeSeries.");
				for(CTimeSeries ts : allts)
					debug3("ts " + ts.getTimeSeriesIdentifier().getUniqueString() + " "
						+ ts.size() + " values.");
			}

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

	@Override
	public TimeSeriesIdentifier transformTsidByCompParm(
			TimeSeriesIdentifier tsid, DbCompParm parm, boolean createTS,
			boolean fillInParm, String timeSeriesDisplayName)
		throws DbIoException, NoSuchObjectException, BadTimeSeriesException
	{
		if (tsid == null)
			tsid = makeEmptyTsId();

		String origString = tsid.getUniqueString();
		TimeSeriesIdentifier tsidRet = tsid.copyNoKey();
		boolean transformed = transformUniqueString(tsidRet, parm);
//Site tssite = tsidRet.getSite();
//Logger.instance().debug3("After transformUniqueString, sitename=" + tsidRet.getSiteName()
//+ ", site=" + (tssite==null ? "null" : tssite.getDisplayName()));
		if (transformed)
		{
			String uniqueString = tsidRet.getUniqueString();
			debug3("CwmsTimeSeriesDb.transformTsid origString='" + origString + "', new string='"
				+ uniqueString + "', parm=" + parm);
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
	 * @param tsidRet the time-series id to transform
	 * @param parm the templeat db comp parameter
	 * @return true if changes were made.
	 */
	public boolean transformUniqueString(TimeSeriesIdentifier tsidRet,
		DbCompParm parm)
	{
		boolean transformed = false;
		if (!(tsidRet instanceof CwmsTsId))
			return false;
		CwmsTsId ctsid = (CwmsTsId) tsidRet;

		SiteName sn = parm.getSiteName();
		if (sn != null)
		{
			tsidRet.setSiteName(sn.getNameValue());
			transformed = true;
			if (sn.site != null)
				tsidRet.setSite(sn.site);
			else
			{
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
		}
		else if (this.tsdbVersion >= TsdbDatabaseVersion.VERSION_14
			  && parm.getLocSpec() != null && parm.getLocSpec().length() > 0)
		{
			String morphed = morph(ctsid.getSiteName(), parm.getLocSpec());
			debug2("TSID site name '" + ctsid.getSiteName() + "' with loc spec '"
				+ parm.getLocSpec() + "' morphed to '" + morphed + "'");
			if (morphed == null)
				morphed = parm.getLocSpec();
			tsidRet.setSite(null);
			tsidRet.setSiteName("");
			SiteDAI siteDAO = makeSiteDAO();
			try
			{
				DbKey siteId = siteDAO.lookupSiteID(morphed);
				if (!DbKey.isNull(siteId))
				{
					tsidRet.setSite(siteDAO.getSiteById(siteId));
					tsidRet.setSiteName(morphed);
				}
			}
			catch (Exception ex)
			{
				Logger.instance().warning("Cannot get site for sitename " + morphed + ": " + ex);
			}
			finally
			{
				siteDAO.close();
			}
			transformed = true;
		}
		DataType dt = parm.getDataType();
		if (dt != null)
		{
			tsidRet.setDataType(dt);
			transformed = true;
		}
		else if (this.tsdbVersion >= TsdbDatabaseVersion.VERSION_14
			  && parm.getParamSpec() != null && parm.getParamSpec().length() > 0)
		{
			String morphed = morph(ctsid.getPart("param"), parm.getParamSpec());
			if (morphed == null)
				debug2("Unable to morph param '" + ctsid.getPart("param") + "' with param spec '"
					+ parm.getParamSpec() + "'");
			else
			{
				tsidRet.setDataType(null);
				tsidRet.setDataType(DataType.getDataType(Constants.datatype_CWMS, morphed));
				transformed = true;
			}
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
			if (s.contains("*"))
			{
				String morphed = morph(ctsid.getPart("version"), s);
				if (morphed == null)
					debug2("Unable to morph param '" + ctsid.getPart("version")
						+ "' with version spec '" + s + "'");
				else
				{
					ctsid.setVersion(morphed);
					transformed = true;
				}
			}
			else
				tsidRet.setPart("Version", s);
			transformed = true;
		}
		return transformed;
	}

	/**
	 * For version 6.3, morph the tsid part by the computation param part.
	 * @param tsidComponent The component from the TSID
	 * @param parmComponent The component in the comp parm, which may contain wildcards.
	 * @return the tsid component masked by the parm component, or null if can't match.
	 */
	public static String morph(String tsidComponent, String parmComponent)
	{
		// Examples:
		// tsid: A-B-C   parm: D-*-F   result: D-B-F
		// tsid: A-B     parm: *-E-F   result: A-E-F
		// tsid: A-B-C   parm: D-*     result: D-B-C
		// tsid: A       parm: D-*     result: null
		// tsid: A-B-C   parm: *-D     result: A-D
		// tsid: A-B     parm: *-      result: A

		// Check for a partial location specification (OpenDCS 6.3)
		String tps[] = tsidComponent.split("-");
		String pps[] = parmComponent.split("-");
		StringBuilder sb = new StringBuilder();
		for(int idx = 0; idx < pps.length; )
		{
			if (pps[idx].equals("*"))
			{
				if (idx >= tps.length)
					return null;
				else
				{
					// A trailing asterisk in the mask means copy in rest of tsid.
					// However a trailing hyphen means lop off the rest of tsid.
					if (idx == pps.length - 1
					 && !parmComponent.endsWith("-"))
					{
						for(int tidx = idx; tidx < tps.length; tidx++)
							sb.append(tps[tidx] + (tidx < tps.length-1 ? "-" : ""));
					}
					else
						sb.append(tps[idx]);
				}
			}
			else
				sb.append(pps[idx]);
			if (++idx < pps.length)
				sb.append("-");
		}
		return sb.toString();

		/*
		 * Note: the table_selector in cp_comp_ts_parm will be empty for a component that is
		 * completely undefined. The syntax is ParamType.Duration.Version[.SiteSpec.ParmSpec],
		 * So "Total.1Hour." means that Version is undefined and shows as <var> in the gui.
		 * This is different from "Total.1Hour.Something-*". Meaning that the first part of
		 * the subversion can be anything.
		 */
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

	@Override
	public ArrayList<String> listParamTypes()
		throws DbIoException
	{
		ArrayList<String> ret = new ArrayList<String>();
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

//	public void printCat()
//	{
//		try
//		{
//			CwmsCatJdbc cwmsCatJdbc = new CwmsCatJdbc(getConnection());
//
//			ResultSet rs = cwmsCatJdbc.catTsId(dbOfficeId, null, null, null, null, null);
//			while(rs != null && rs.next())
//			{
//				System.out.println(rs.getString(2).trim() + ", active=" + rs.getString(5));
//			}
//		}
//		catch(Exception ex)
//		{
//			System.err.println("Error: " + ex);
//			ex.printStackTrace(System.err);
//		}
//
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
		DbKey dbOfficeCode, String dbOfficePrivilege)
		throws DbIoException
	{
//TODO No need to pass this method office code or privilege level. SQL code fixgures that out.
		String errMsg = null;
		PreparedStatement storeProcStmt = null;
		CallableStatement testStmt = null;

		try
		{
			String q = null;
			int privLevel =
				dbOfficeId == null ? 0 :
				dbOfficePrivilege.toUpperCase().contains("MGR") ? 1 :
				dbOfficePrivilege.toUpperCase().contains("PROC") ? 2 : 3;
			q =
				"begin cwms_ccp_vpd.set_ccp_session_ctx(" +
				":1 /* office code */, :2 /* priv level*/, :3 /* officeId */); end;";
			storeProcStmt  = conn.prepareCall(q);
			storeProcStmt.setInt(1, (int)dbOfficeCode.getValue());
			storeProcStmt.setInt(2, privLevel);
			storeProcStmt.setString(3, dbOfficeId);
			Logger.instance().debug1("Executing '" + q + "' with "
				+ "dbOfficeCode=" + dbOfficeCode
				+ ", privLevel=" + privLevel
				+ ", dbOfficeId='" + dbOfficeId + "'");
			storeProcStmt.execute();
//			conn.commit();

			q = "{ ? = call cwms_ccp_vpd.get_pred_session_office_code_v(?, ?) }";
			testStmt = conn.prepareCall(q);
			testStmt.registerOutParameter(1, Types.VARCHAR);
			testStmt.setString(2, "CC");
			testStmt.setString(3, "PLATFORMCONFIG");
			Logger.instance().info("Calling '" + q + "' with "
				+ "schema=CCP and table=PLATFORMCONFIG");
			testStmt.execute();
			String pred = testStmt.getString(1);
			Logger.instance().info("Predicate for table PLATFORMCONFIG is '" + pred + "'");

		}
		catch (SQLException ex)
		{
			errMsg = "Error setting VPD context for '" + dbOfficeId + "': " + ex;
			Logger.instance().failure(errMsg);
			System.err.println(errMsg);
			ex.printStackTrace(System.err);
			throw new DbIoException(errMsg);
		}
		finally
		{
			if (storeProcStmt != null)
			{
				try { storeProcStmt.close(); }
				catch(Exception ex) {}
			}
			if (testStmt != null)
			{
				try { testStmt.close(); }
				catch(Exception ex) {}
			}
		}
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

//	public static int determineCwmsSchemaVersion(Connection con, int tsdbVersion)
//	{
//		if (DecodesSettings.instance().cwmsVersionOverride == 2)
//		{
//			Logger.instance().info("cwmsVersionOverride==2");
//			return CWMS_V_2_1;
//		}
//
//		String q = "select count(*) from all_synonyms where owner='PUBLIC' " +
//			"and SYNONYM_NAME = 'CWMS_ENV'";
//		ResultSet rs = null;
//		Statement stmt = null;
//		try
//        {
//			stmt = con.createStatement();
//			rs = stmt.executeQuery(q);
//			if (rs.next())
//			{
//				int count = rs.getInt(1);
//				if (count == 0)
//					return CWMS_V_2_1;
//				Logger.instance().debug3("Response to '" + q + "' is " + count);
//				return CWMS_V_3_0;
//			}
//			Logger.instance().warning("Cannot determine CWMS Version. No results to query '"
//				+ q + "' -- assuming 3.0");
//			return CWMS_V_3_0;
//		}
//		catch (Exception ex)
//		{
//			Logger.instance().warning("Error Determing Schema Version: " + ex
//			+ " -- assuming 3.0");
//			return CWMS_V_3_0;
//		}
//		finally
//		{
//			if (rs != null)
//			{
//				try { rs.close(); } catch(Exception ex) {}
//			}
//			if (stmt != null)
//			{
//				try { stmt.close(); } catch(Exception ex) {}
//			}
//		}
//	}

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

	public void setDbUri(String dbUri)
	{
		this.dbUri = dbUri;
	}

	public DbKey getDbOfficeCode()
	{
		return dbOfficeCode;
	}

	public BaseParam getBaseParam()
	{
		return baseParam;
	}

	@Override
	public ScreeningDAI makeScreeningDAO()
		throws DbIoException
	{
		return new ScreeningDAO(this);
	}

	@Override
	public GroupHelper makeGroupHelper()
	{
		return new CwmsGroupHelper(this);
	}

	@Override
	public double rating(String specId, Date timeStamp, double... indeps)
		throws DbCompException, RangeException
	{
		// int nIndeps = indeps.length;
		// NOTE: indeps is already an array of doubles. I can pass
		// it directly to the rateOne function.

		CwmsRatingDao crd = new CwmsRatingDao(this);
		try
		{
			RatingSet ratingSet = crd.getRatingSet(specId);
			double d = ratingSet.rateOne(indeps, timeStamp.getTime());
			if (d == Const.UNDEFINED_DOUBLE)
			{
				StringBuilder sb = new StringBuilder();
				for(double x : indeps)
					sb.append(x + ",");
				sb.deleteCharAt(sb.length()-1);
				String msg = "Input values (" + sb.toString() + ") outside rating range.";
				warning(msg);
				throw new RangeException(msg);
			}
			return d;
		}
		catch (RatingException ex)
		{
			String msg = "rating(" + specId + ") failed: " + ex;
			System.err.println(msg);
			ex.printStackTrace(System.err);
			throw new RangeException(msg);
		}
		finally
		{
			crd.close();
		}
	}

	@Override
	public void closeConnection()
	{
		// Close prepared statements
		if (getMinStmt != null)
		{
			try { getMinStmt.close(); }
			catch(Exception ex) {}
			getMinStmt = null;
		}
		if (getTaskListStmt != null)
		{
			try { getTaskListStmt.close(); }
			catch(Exception ex) {}
			getTaskListStmt = null;
		}

		// Return the connection to the CWMS connection pool. Do not close directly.
		doCloseConnection(getConnection());
	}

	public static void doCloseConnection(Connection con)
	{
		// Return connection to CWMS connection pool.
		try
		{
			CwmsDbConnectionPool.close(con);
		}
		catch (Exception ex)
		{
			Logger.instance().warning("Exception closing CWmS connection: " + ex);
		}

	}

	@Override
	public ArrayList<String> listVersions()
		throws DbIoException
	{
		ArrayList<String> ret = new ArrayList<String>();
		String q = "select distinct version_id from cwms_v_ts_id order by version_id;";

		ResultSet rs = doQuery(q);
		try
		{
			while (rs != null && rs.next())
				ret.add(rs.getString(1));
		}
		catch (SQLException ex)
		{
			throw new DbIoException("CwmsTimeSeriesDb.listVersions: " + ex);
		}

		return ret;
	}

	@Override
	public String getStorageUnitsForDataType(DataType dt)
	{
		String cwmsParam = null;
		if (dt.getStandard().equalsIgnoreCase(Constants.datatype_CWMS))
			cwmsParam = dt.getCode();
		else
		{
			DataType equiv = dt.findEquivalent(Constants.datatype_CWMS);
			if (equiv == null)
				return null;
			cwmsParam = equiv.getCode();
		}

		// Truncate to just base param
		int hyphen = cwmsParam.indexOf('-');
		if (hyphen > 0)
			cwmsParam = cwmsParam.substring(0, hyphen);
		return baseParam.getStoreUnits4Param(cwmsParam);
	}

	@Override
	public String flags2display(int flags)
	{
		return CwmsFlags.flags2Display(flags);
	}

}
