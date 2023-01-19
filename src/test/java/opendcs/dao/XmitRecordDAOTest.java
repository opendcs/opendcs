package opendcs.dao;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opendcs.utils.ClasspathIO;

import decodes.cwms.validation.dao.ScreeningDAI;
import decodes.db.UnitConverter;
import decodes.sql.DbKey;
import decodes.sql.DecodesDatabaseVersion;
import decodes.sql.KeyGenerator;
import decodes.sql.SequenceKeyGenerator;
import decodes.tsdb.BadTimeSeriesException;
import decodes.tsdb.CTimeSeries;
import decodes.tsdb.DbCompParm;
import decodes.tsdb.DbIoException;
import decodes.tsdb.NoSuchObjectException;
import decodes.tsdb.TimeSeriesIdentifier;
import decodes.tsdb.TsGroup;
import decodes.tsdb.TsdbDatabaseVersion;
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

public class XmitRecordDAOTest {

    //private Connection conn = null;
    private DatabaseConnectionOwner db = null;

    @BeforeEach
    public void setup_database() throws SQLException, IOException
    {
        Connection conn = DriverManager.getConnection("jdbc:sqlite::memory:");
        db = new FakeDbOwner(conn);
        Statement stmt = conn.createStatement();
        // build the basic database structure
        String baseSql = readStream(this.getClass().getResourceAsStream("/opendcs/dao/XmitDAOTestBase.sql"));
        String template = readStream(this.getClass().getResourceAsStream("/opendcs/dao/XmitDAOTestTemplate.sql"));
        try
        {
            for(String sql: baseSql.split(";"))
            {
                stmt.executeUpdate(sql);
            }
            
            String templates[] = template.split(";");
            for( int i = 0; i < 31; i++)
            {
                for(String t: templates)
                {
                    stmt.executeUpdate(t.replace("SUFFIX",String.format("%02d",i)));
                }
            }
        }
        catch(SQLException ex)
        {
            System.out.println(ex.getMessage());
            throw ex;

        }
    
    }

    private String readStream(InputStream is) throws IOException
    {
        StringBuilder sb = new StringBuilder();
        try(BufferedReader br = new BufferedReader(new InputStreamReader(is));)
        {
            String line = null;
            while( (line = br.readLine()) != null)
            {
                sb.append(line).append(System.lineSeparator());
            }
            return sb.toString();
        }
        
    }
    
    @Test
    public void save_restore_large_message() 
    {
        try(XmitRecordDAI dai = db.makeXmitRecordDao(1);)
        {

        }
        // generate message
        // save message
        // read message
        // verify message
    }

    private static class FakeDbOwner implements DatabaseConnectionOwner
    {
        private static Logger log = Logger.getLogger(FakeDbOwner.class.getName());
        private final Connection conn;
        private final KeyGenerator keyGen;

        private static final SimpleDateFormat sdf = new SimpleDateFormat("YYYY-mm-DD'T'HH:mm:ss");

        public FakeDbOwner(Connection conn)
        {
            this.conn = conn;
            this.keyGen = new SequenceKeyGenerator();
        }
        @Override
        public Connection getConnection()
        {
            return conn;
        }

        @Override
        public void freeConnection(Connection conn) { }

        @Override
        public int getDecodesDatabaseVersion()
        {
            // we only care about mimicking the newest features
            return DecodesDatabaseVersion.DECODES_DB_68; 
        }

        @Override
        public void setDecodesDatabaseVersion(int version, String options) {}

        @Override
        public int getTsdbVersion() 
        {
            // TODO Auto-generated method stub
            return TsdbDatabaseVersion.VERSION_68;
        }

        @Override
        public void setTsdbVersion(int version, String description) {}

        @Override
        public boolean isOracle()
        {
            return false;
        }

        @Override
        public boolean isHdb()
        {
            return false;
        }

        @Override
        public boolean isCwms()
        {
            return false;
        }

        @Override
        public boolean isOpenTSDB()
        {
            return true;
        }

        @Override
        public KeyGenerator getKeyGenerator()
        {
            return keyGen;
        }

        @Override
        public SimpleDateFormat getLogDateFormat()
        {
            return sdf;
        }

        @Override
        public String sqlDate(Date d)
        {
            return sdf.format(d);
        }

        @Override
        public Date getFullDate(ResultSet rs, int column)
        {
            try
            {
                long t = rs.getLong(column);
                return new Date(t);
            }
            catch (SQLException ex)
            {
                log.log(Level.SEVERE,"Unable to convert date: ",ex);
                return null;
            }
        }

        @Override
        public String sqlBoolean(boolean v)
        {
            // TODO Auto-generated method stub
            return v ? "T": "F";
        }

        @Override
        public int findMaxModelRunId(int modelId) throws DbIoException
        {
            return 0;
        }

        @Override
        public int getWriteModelRunId()
        {
            return 0;
        }

        @Override
        public DbKey getAppId()
        {
            
            return DbKey.createDbKey(1L);
        }

        @Override
        public String getDatabaseTimezone()
        {
            return "UTC";
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
            return new XmitRecordDAO(this, maxDays);
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
        public TimeSeriesIdentifier transformTsidByCompParm(TimeSeriesIdentifier tsid, DbCompParm parm,
                boolean createTS, boolean fillInParm, String timeSeriesDisplayName)
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
}
