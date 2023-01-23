package opendcs.dao;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
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
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.opendcs.utils.ClasspathIO;

import decodes.cwms.validation.dao.ScreeningDAI;
import decodes.db.DatabaseException;
import decodes.db.UnitConverter;
import decodes.dcpmon.XmitMediumType;
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
import lrgs.common.DcpAddress;
import lrgs.common.DcpMsg;
import lrgs.common.DcpMsgFlag;
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


/**
 * NOTE: This test maintains it's own copy of the dcp_trans_xx tables
 * instead of just using the database schema the schema directory.
 * it will need to be kept up to date manually for the moment.
 */
public class XmitRecordDAOTest {
    private static Logger log = Logger.getLogger(XmitRecordDAOTest.class.getName());

    private Connection conn = null;
    private DatabaseConnectionOwner db = null;
    

    @BeforeEach
    public void setup_database(TestInfo info) throws SQLException, IOException
    {
        String dbName = info.getDisplayName()
                            .replace(" ","_")
                            .replace("(","")
                            .replace(")","")
                        +".db";
        /* don't care = */ new File(dbName).delete();
        conn = DriverManager.getConnection("jdbc:sqlite:"+dbName);
        conn.setAutoCommit(true);
        db = new FakeDbOwner(conn);
        Statement stmt = conn.createStatement();
        // build the basic database structure
        String baseSql = readStream(this.getClass().getResourceAsStream("/opendcs/dao/XmitDAOTestBase.sql"));
        String template = readStream(this.getClass().getResourceAsStream("/opendcs/dao/XmitDAOTestTemplate.sql"));
        
        for(String sql: baseSql.split(";"))
        {
            stmt.executeUpdate(sql);
        }
        
        String templates[] = template.split(";");
        for( int i = 1; i <= 31; i++)
        {
            for(String t: templates)
            {
                stmt.executeUpdate(t.replace("SUFFIX",String.format("%02d",i)));
            }
        }                
    }

    @AfterEach
    public void close_database() throws SQLException 
    {
        conn.close();
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
    public void save_restore_small_message() throws Exception
    {
        final String smallAddr = "smalladdr";
        try(XmitRecordDAI dai = db.makeXmitRecordDao(10);)
        {
            Date carrierStart = new Date();
            Date carrierEnd = new Date(carrierStart.getTime()+5000 /*seconds*/);


            String msg = readStream(this.getClass().getResourceAsStream("/opendcs/dao/XmitDAOSmallMsg.txt"));
            DcpMsg smallMsg = new DcpMsg(DcpMsgFlag.MSG_TYPE_OTHER,msg.getBytes(),msg.length(),0);
            smallMsg.setDcpAddress(new DcpAddress(smallAddr));
            smallMsg.setCarrierStart(carrierStart);
            smallMsg.setCarrierStop(carrierEnd);
            smallMsg.setDataSourceId(1);
            smallMsg.setFailureCode('G');
            smallMsg.setXmitTime(carrierStart);


            dai.saveDcpTranmission(smallMsg);

            List<DcpMsg> records = ((DaoBase)dai).getResults("select * from dcp_trans_01 where medium_id=?", 
                                rs -> {
                                    DcpMsg m = new DcpMsg();
                                    m.setDcpAddress(new DcpAddress(rs.getString("medium_id")));
                                    m.setXmitTime(new Date (rs.getLong("transmit_time")));
                                    return m;
                                },smallAddr);
            assertFalse(records.isEmpty(), "no records were stored.");
            records.forEach(r->{
                System.err.println(r.getDcpAddress().toString() + ", Data length:" +r.getMessageLength());
            });

            DcpMsg returned = dai.findDcpTranmission(XmitMediumType.LOGGER, smallAddr, carrierEnd);

            assertNotNull(returned, "Could not retrieve saved message.");

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
            this.keyGen = new MemoryKeyGenerator();
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

    public static class MemoryKeyGenerator implements KeyGenerator
    {

        private HashMap<String,Long> sequences = new HashMap<>();
        @Override
        public DbKey getKey(String tableName, Connection conn) throws DatabaseException
        {
            if( !sequences.containsKey(tableName))
            {
                sequences.put(tableName,0L);
            }
            Long sequence = sequences.get(tableName);
            sequence = sequence + 1;
            sequences.put(tableName,sequence);
            
            return DbKey.createDbKey(sequence);
        }

        @Override
        public void reset(String tableName, Connection conn) throws DatabaseException
        {        
            sequences.remove(tableName);
        }
        
    }
}
