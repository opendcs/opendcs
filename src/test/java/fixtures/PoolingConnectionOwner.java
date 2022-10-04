package fixtures;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import javax.sql.DataSource;

import decodes.cwms.validation.dao.ScreeningDAI;
import decodes.db.UnitConverter;
import decodes.sql.DbKey;
import decodes.sql.KeyGenerator;
import decodes.tsdb.BadTimeSeriesException;
import decodes.tsdb.CTimeSeries;
import decodes.tsdb.DbCompParm;
import decodes.tsdb.DbIoException;
import decodes.tsdb.NoSuchObjectException;
import decodes.tsdb.TimeSeriesIdentifier;
import decodes.tsdb.TsGroup;
import opendcs.dai.AlarmDAI;
import opendcs.dai.AlgorithmDAI;
import opendcs.dai.CompDependsDAI;
import opendcs.dai.ComputationDAI;
import opendcs.dai.DacqEventDAI;
import opendcs.dai.DataTypeDAI;
import opendcs.dai.DeviceStatusDAI;
import opendcs.dai.EnumDAI;
import opendcs.dai.IntervalDAI;
import opendcs.dai.LoadingAppDAI;
import opendcs.dai.PlatformStatusDAI;
import opendcs.dai.PropertiesDAI;
import opendcs.dai.ScheduleEntryDAI;
import opendcs.dai.SiteDAI;
import opendcs.dai.TimeSeriesDAI;
import opendcs.dai.TsGroupDAI;
import opendcs.dai.XmitRecordDAI;
import opendcs.dao.DatabaseConnectionOwner;
import opendcs.util.sql.WrappedConnection;

public class PoolingConnectionOwner implements DatabaseConnectionOwner{

    private DataSource ds = null;

    public PoolingConnectionOwner(DataSource ds)
    {
        this.ds = ds;
    }

    @Override
    public Connection getConnection() {
        try
        {
            return new WrappedConnection(ds.getConnection(), this);
        }
        catch(SQLException ex)
        {
            return null;
        }
    }

    @Override
    public void freeConnection(Connection conn) {
        try
        {
            conn.close();
        }
        catch(SQLException ex)
        {
            throw new RuntimeException("cannot close",ex);
        }
    }

    @Override
    public int getDecodesDatabaseVersion() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void setDecodesDatabaseVersion(int version, String options) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public int getTsdbVersion() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void setTsdbVersion(int version, String description) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public boolean isOracle() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isHdb() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isCwms() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isOpenTSDB() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public KeyGenerator getKeyGenerator() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public SimpleDateFormat getLogDateFormat() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String sqlDate(Date d) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Date getFullDate(ResultSet rs, int column) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String sqlBoolean(boolean v) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int findMaxModelRunId(int modelId) throws DbIoException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getWriteModelRunId() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public DbKey getAppId() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getDatabaseTimezone() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public EnumDAI makeEnumDAO() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public PropertiesDAI makePropertiesDAO() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public DataTypeDAI makeDataTypeDAO() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public SiteDAI makeSiteDAO() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public XmitRecordDAI makeXmitRecordDao(int maxDays) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public LoadingAppDAI makeLoadingAppDAO() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public AlgorithmDAI makeAlgorithmDAO() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public TsGroupDAI makeTsGroupDAO() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ComputationDAI makeComputationDAO() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public TimeSeriesDAI makeTimeSeriesDAO() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public TimeSeriesIdentifier expandSDI(DbCompParm parm) throws DbIoException, NoSuchObjectException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public UnitConverter makeUnitConverterForRead(CTimeSeries cts) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public CompDependsDAI makeCompDependsDAO() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IntervalDAI makeIntervalDAO() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ScheduleEntryDAI makeScheduleEntryDAO() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public PlatformStatusDAI makePlatformStatusDAO() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ArrayList<TimeSeriesIdentifier> expandTsGroup(TsGroup tsGroup) throws DbIoException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public TimeSeriesIdentifier transformTsidByCompParm(TimeSeriesIdentifier tsid, DbCompParm parm, boolean createTS,
            boolean fillInParm, String timeSeriesDisplayName)
            throws DbIoException, NoSuchObjectException, BadTimeSeriesException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public DeviceStatusDAI makeDeviceStatusDAO() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public DacqEventDAI makeDacqEventDAO() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ScreeningDAI makeScreeningDAO() throws DbIoException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public AlarmDAI makeAlarmDAO() {
        // TODO Auto-generated method stub
        return null;
    }
    
}
