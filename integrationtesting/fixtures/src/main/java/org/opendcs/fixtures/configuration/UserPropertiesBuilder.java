package org.opendcs.fixtures.configuration;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Properties;

import decodes.sql.KeyGenerator;

public class UserPropertiesBuilder
{
    private Properties props = new Properties();
    
    /**
     * Create the actual properties file.
     * @param out prepared output stream to write the properties
     * @throws IOException
     */
    public void build(OutputStream out) throws IOException
    {
        props.store(out,"Test Configuration");
    }

    /**
     * Set Database location
     * @param dbLocation directory or jdbc URL
     * @return
     */
    public UserPropertiesBuilder withDatabaseLocation(String dbLocation)
    {
        props.setProperty("editDatabaseLocation", dbLocation);
        return this;
    }

    /**
     * Set the required SQLDate format
     * @param format SimpleDateFormat used to generating or parsing certain results.
     * @return
     */
    public UserPropertiesBuilder withSqlDateFormat(String format)
    {
        props.setProperty("sqlDateFormat", format);
        return this;
    }

    /**
     * Sets what type of database we're using
     * @param type XML,OPENDCS,CWMS,HDB are currently valid
     * @return
     */
    public UserPropertiesBuilder withEditDatabaseType(String type)
    {
        props.setProperty("editDatabaseType", type);
        return this;
    }

    /**
     * Default site name style (CWMS,HDB,USGS,NWS, etc) to look for.
     * @param preference
     * @return
     */
    public UserPropertiesBuilder withSiteNameTypePreference(String preference)
    {
        props.setProperty("siteNameTypePreference", preference);
        return this;
    }

    /**
     * Pointer to authentication information. defaults to $DCSTOOL_USERDIR/.decodes.auth
     * Required for SQL based instances.
     * @param auth
     * @return
     */
    public UserPropertiesBuilder withDecodesAuth(String auth)
    {
        props.setProperty("DbAuthFile", auth);
        return this;
    }

    /**
     * Set the Cwms Office Id to use for connection setup.
     *
     * NOTE: only used by CWMS.
     * @param officeId
     * @return
     */
    public UserPropertiesBuilder withCwmsOffice(String officeId)
    {
        props.setProperty("cwmsOfficeId",officeId);
        return this;
    }

    /**
     * Default office id to use for certain interfaces.
     *
     * NOTE: CWMS Only
     * @param officeId
     * @return
     */
    public UserPropertiesBuilder withDbOffice(String officeId)
    {
        props.setProperty("dbOfficeId",officeId);
        return this;
    }

    /**
     * Default, if not set, is false.
     *
     * @param writeCwmsLocations true if you want to allow creating of CWMS locations (sites)
     * @return
     */
    public UserPropertiesBuilder withWriteCwmsLocations(boolean writeCwmsLocations)
    {
        props.setProperty("writeCwmsLocations",Boolean.toString(writeCwmsLocations));
        return this;
    }

    /**
     *
     * @param generator which KeyGenerator implementation to use.
     * @return
     */
    public UserPropertiesBuilder withSqlKeyGenerator(Class<? extends KeyGenerator> generator)
    {
        props.setProperty("sqlKeyGenerator",generator.getName());
        return this;
    }
/**
 * For reference
 
 
    #OPENDCS Toolkit Settings
#Thu Sep 05 19:06:53 UTC 2019
CpEffectiveStart=
minCompId=0
sqlDateFormat=yyyy-MM-dd HH\:mm\:ss
asciiSelfDescProcessMOFF=true
showGroupEditor=true
platformListDesignatorCol=false
editDatabaseType=CWMS
routingMonitorUrl=file\://$DECODES_INSTALL_DIR/routmon/routmon.html
SqlReadDateFormat=yyyy-MM-dd HH\:mm\:ss
showEventMonitor=false
maxComputationRetries=0
pyQuotedColor=0x00D000
pyKeywordColor=0x0000FF
minAlgoId=0
showAlarmEditor=false
eventPurgeDays=5
pakBusMaxTableDefAge=hour*48
maxMissingTimeForFill=10800
writeCwmsLocations=true
agency=
pyBuiltinColor=0xD2691E
siteNameTypePreference=CWMS
pyTsRoleColor=0x8B0000
decwizRawDataDir=$HOME/raw-done
retryFailedComputations=false
compCheckPeriod=120
showComputationEditor=true
showPlatformMonitor=true
showRoutingMonitor=true
showNetlistEditor=false
DbAuthFile=$DCSTOOL_USERDIR/.decodes.auth
CwmsOfficeId=SWT
pollMessageDir=$HOME/SHARED/dacq/edl-done
decwizDecodedDataDir=$HOME/decoded-done
decwizTimeZone=UTC
hdbSiteDescriptions=false
editTimeZone=UTC
scanPastEOL=false
setPlatformDesignatorName=false
tsidFetchSize=0
pyCpFuncColor=0x8B4513
autoDeleteOnImport=false
aggregateTimeZone=UTC
cdtLocalFile=$DCSTOOL_USERDIR/chans_by_baud.txt
datchkConfigFile=./datchk.cfg
maxMissingValuesForFill=3
pakBusTableDefDir=$DCSTOOL_USERDIR/pakbus
pollTcpTemplate=PollTcpTemplate
sqlKeyGenerator=decodes.sql.OracleSequenceKeyGenerator
nwsXrefUrl=http\://www.nws.noaa.gov/oh/hads/USGS/ALL_USGS-HADS_SITES.txt
language=en
nwsXrefLocalFile=$DCSTOOL_USERDIR/nwsxref.txt
pdtLocalFile=$DCSTOOL_USERDIR/pdt
pyCommentColor=0x808000
screeningUnitSystem=English
guiTimeZone=UTC
defaultDataSource=cdadata.wcda.noaa.gov
sqlTimeZone=UTC
showAlgorithmEditor=true
fontAdjust=0
dataTypeStdPreference=CWMS
jdbcDriverClass=oracle.jdbc.driver.OracleDriver
showTimeSeriesEditor=true
decwizOutputFormat=stdmsg
showPlatformWizard=false
#editDatabaseLocation=jdbc\:oracle\:thin\:@10.0.0.36\:1539\:V122SWT1811CCPT
editDatabaseLocation=jdbc\:oracle\:thin\:@192.168.4.217\:1539\:V122SWT1811CCPQ
#editDatabaseLocation=jdbc\:oracle\:thin\:@localhost\:11539\:V122SWT1811CCPQ
decodesConfigOwner=
defaultMaxDecimals=4
transportMediumTypePreference=goes
autoCommit=true
dbClassName=decodes.cwms.CwmsTimeSeriesDb
minProcId=0
pakBusMaxBaudRate=19200
pakBusTableName=Hourly
decwizDebugLevel=0
pakBusSecurityCode=8894
pyPropColor=0x4B0082
cdtUrl=https\://dcs1.noaa.gov/chans_by_baud.txt
pollScriptDir=$DCSTOOL_HOME/poll-scripts
pyNormalColor=0x000000
tsdbStoragePresGrp=CWMS-English
pollRoutingTemplate=PollGuiTemplate
rememberScreenPosition=true
cwmsVersionOverride=0
pdtUrl=https\://dcs1.noaa.gov/pdts_compressed.txt
showTestCmputations=true
location=
decwizSummaryLog=$HOME/summary.log
algoEditCompileOptions=
routingStatusDir=$DCSTOOL_USERDIR/routstat
decodesFormatLabelMode=f
*/

    public void withDatabaseDriver(String driverString)
    {
        this.props.put("jdbcDriverClass",driverString);
    }
}
