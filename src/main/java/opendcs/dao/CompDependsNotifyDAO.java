package opendcs.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import decodes.sql.DbKey;
import decodes.tsdb.CpDependsNotify;
import decodes.tsdb.DbIoException;
import decodes.tsdb.TsdbDatabaseVersion;
import opendcs.dai.CompDependsNotifyDAI;

public class CompDependsNotifyDAO extends DaoBase implements CompDependsNotifyDAI
{
    public CompDependsNotifyDAO(DatabaseConnectionOwner db)
    {
        super(db, "CompDependsNotify");
    }

    private CpDependsNotify rs2cdn(ResultSet rs) throws SQLException
    {
        CpDependsNotify cdn = new CpDependsNotify();
        cdn.setRecordNum(rs.getLong("record_num"));
        String s = rs.getString("event_type");
        if (s != null && s.length() >= 1)
        {
            cdn.setEventType(s.charAt(0));
        }
        cdn.setKey(DbKey.createDbKey(rs, "key"));
        cdn.setDateTimeLoaded(db.getFullDate(rs, "date_time_loaded"));
        return cdn;
    }

    @Override
    public List<CpDependsNotify> getAllNotifyRecords() throws DbIoException
    {
        String q = "select RECORD_NUM, EVENT_TYPE, KEY, DATE_TIME_LOADED from CP_DEPENDS_NOTIFY";
        try
        {
            return getResults(q, rs -> rs2cdn(rs));
        }
        catch (SQLException ex)
        {
            throw new DbIoException("Unable to retrieve notification records.", ex);
        }
    }

    @Override
    public CpDependsNotify getNextRecord() throws DbIoException
    {
        if (db.getTsdbVersion() < TsdbDatabaseVersion.VERSION_8)
        {
            return null;
        }
        String q = "select RECORD_NUM, EVENT_TYPE, KEY, DATE_TIME_LOADED "
                 + "from CP_DEPENDS_NOTIFY "
                 + "where DATE_TIME_LOADED = "
                 + "(select min(DATE_TIME_LOADED) from CP_DEPENDS_NOTIFY)";
        try
        {
            final CpDependsNotify ret = getSingleResult(q, rs -> rs2cdn(rs));
            deleteNotifyRecord(ret);
            return ret;
        }
        catch(Exception ex)
        {
            warning("Error CpCompDependsNotify: " + ex);
        }
        return null;
    }

    @Override
    public void deleteNotifyRecord(CpDependsNotify record) throws DbIoException
    {
        if (record == null)
        {
            return;
        }
        try
        {
            doModify("delete from CP_DEPENDS_NOTIFY where RECORD_NUM = ?",record.getRecordNum());
        }
        catch (SQLException ex)
        {
            throw new DbIoException("Unable to delete record", ex);
        }
    }

    @Override
    public void saveRecord(CpDependsNotify record) throws DbIoException
    {
        String q = "insert into cp_depends_notify(record_num, event_type, key, date_time_loaded) "
                 + "values(?,?,?,?)";
        try
        {
            doModify(q,getKey("cp_depends_notify"),record.getEventType(), record.getKey(), new Date());
        }
        catch (SQLException ex)
        {
            throw new DbIoException("Unable to save record.", ex);
        }
    }

}
