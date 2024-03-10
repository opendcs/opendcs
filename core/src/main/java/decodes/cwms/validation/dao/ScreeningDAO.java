/**
 * $Id$
 *
 * Copyright 2015 U.S. Army Corps of Engineers, Hydrologic Engineering Center.
 *
 * $Log$
 * Revision 1.10  2019/01/16 15:31:19  mmaloney
 * test
 *
 * Revision 1.9  2018/11/28 21:18:48  mmaloney
 * CWMS JOOQ Migration Mods
 *
 * Revision 1.8  2016/03/09 16:46:50  mmaloney
 * CWMS-7822 It was trying to save Double Negative/Positive infinity for ROC checks.
 *
 * Revision 1.7  2016/02/29 22:16:46  mmaloney
 * Bug fix -- on update it was not saving the new description.
 *
 * Revision 1.6  2015/11/18 14:04:37  mmaloney
 * Remove debugs
 *
 * Revision 1.5  2015/11/12 15:13:41  mmaloney
 * Added HEC header.
 *
 */
package decodes.cwms.validation.dao;

import ilex.util.Logger;
import ilex.util.TextUtil;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.io.PrintStream;
import java.math.BigDecimal;

import decodes.cwms.CwmsTimeSeriesDb;
import decodes.cwms.validation.AbsCheck;
import decodes.cwms.validation.ConstCheck;
import decodes.cwms.validation.DurCheckPeriod;
import decodes.cwms.validation.RocPerHourCheck;
import decodes.cwms.validation.Screening;
import decodes.cwms.validation.ScreeningCriteria;
import decodes.db.NoConversionException;
import decodes.sql.DbKey;
import decodes.tsdb.DbIoException;
import decodes.tsdb.NoSuchObjectException;
import decodes.tsdb.TimeSeriesIdentifier;
import decodes.util.DecodesSettings;
import opendcs.dai.TimeSeriesDAI;
import opendcs.dao.DaoBase;
import opendcs.dao.DatabaseConnectionOwner;
import opendcs.dao.DbObjectCache;
import usace.cwms.db.dao.ifc.vt.CwmsDbVt;
import usace.cwms.db.dao.ifc.vt.ScreenAssignT;
import usace.cwms.db.dao.ifc.vt.ScreenCritType;
import usace.cwms.db.dao.ifc.vt.ScreenDurMagType;
import usace.cwms.db.dao.ifc.vt.ScreeningControlT;
import usace.cwms.db.dao.util.services.CwmsDbServiceLookup;

/**
 * This DAO translates between normal Java types and the Oracle-specific types
 * used by the JPub-Generated CwmsScreeningDbIo module.
 */
public class ScreeningDAO
    extends DaoBase
    implements ScreeningDAI
{
    public static final String module = "ScreeningDAO";

    // Screenings are allowed to stay in the cache for an hour
    protected static DbObjectCache<Screening> cache = new DbObjectCache<Screening>(60 * 60 * 1000L, false);

    // Associates a time series with a screening
//    private static HashMap<TimeSeriesIdentifier, Screening> ts2screening = new HashMap<TimeSeriesIdentifier, Screening>();

    /** Columns to read from cwms_v_screening_id to tell if a screening exists. */
    public static final String screenIdTable = "CWMS_V_SCREENING_ID";
    public static final String screenIdColumns = "SCREENING_CODE, SCREENING_ID, SCREENING_ID_DESC, "
        + "PARAMETER_ID, PARAMETER_TYPE_ID, DURATION_ID";

    /** Columns in cwms_v_screening_criteria to read screening criteria info */
    public static final String screenCritTable = "CWMS_V_SCREENING_CRITERIA";
    public static final String screenCritColumns = "SCREENING_CODE, "
        + "SEASON_START_DAY, SEASON_START_MONTH, "
        + "ESTIMATE_EXPRESSION, "
        + "RANGE_REJECT_LO, RANGE_REJECT_HI, RANGE_QUESTION_LO, RANGE_QUESTION_HI, "
        + "RATE_CHANGE_REJECT_FALL, RATE_CHANGE_REJECT_RISE, "
        + "RATE_CHANGE_QUEST_FALL, RATE_CHANGE_QUEST_RISE, "
        + "CONST_REJECT_DURATION, CONST_REJECT_MIN, CONST_REJECT_TOLERANCE, CONST_REJECT_N_MISS, "
        + "CONST_QUEST_DURATION, CONST_QUEST_MIN, CONST_QUEST_TOLERANCE, CONST_QUEST_N_MISS";
//        + "RANGE_ACTIVE_FLAG, RATE_CHANGE_ACTIVE_FLAG, CONST_ACTIVE_FLAG, DUR_MAG_ACTIVE_FLAG";

    public static final String screenControlTable = "CWMS_V_SCREENING_CONTROL";
    public static final String screenControlColumns = "SCREENING_CODE, "
        + "RANGE_ACTIVE_FLAG, RATE_CHANGE_ACTIVE_FLAG, CONST_ACTIVE_FLAG, DUR_MAG_ACTIVE_FLAG";

    public static final String durMagTable = "CWMS_V_SCREENING_DUR_MAG";
    public static final String durMagColumns = "SCREENING_CODE, SEASON_START_DAY, SEASON_START_MONTH, "
        + "DUR_MAG_DURATION_ID, REJECT_LO, REJECT_HI, QUESTION_LO, QUESTION_HI";

    private CwmsDbVt csdbio = null;

    public ScreeningDAO(DatabaseConnectionOwner tsdb)
        throws DbIoException
    {
        super(tsdb, module);
        try(Connection conn = getConnection())
        {
            csdbio = CwmsDbServiceLookup.buildCwmsDb(CwmsDbVt.class, conn);
        }
        catch(Exception ex)
        {
            String msg = module + " cannot create CwmsScreeningDbIo: " + ex;
            Logger.instance().failure(msg);
            System.err.println(msg);
            ex.printStackTrace(System.err);
            throw new DbIoException(msg,ex);
        }
    }

    @Override
    public void writeScreening(Screening screening)
        throws DbIoException
    {
        Logger.instance().debug1(module + ".writeScreening(" + screening.getScreeningName() + ") key=" + screening.getScreeningCode());

        // if screening does not have a DbKey, try to get key through the unique id.
        if (DbKey.isNull(screening.getScreeningCode()))
            screening.setScreeningCode(getKeyForId(screening.getScreeningName()));

        try(Connection conn = getConnection();)
        {
            String officeId = ((CwmsTimeSeriesDb)db).getDbOfficeId();
            String P_SCREENING_ID = screening.getScreeningName();
            String P_DB_OFFICE_ID = officeId;
            String P_SCREENING_ID_DESC = screening.getScreeningDesc();

            // If it still does not have a key, create the screening id and assign a surrogate key.
            if (DbKey.isNull(screening.getScreeningCode()))
            {
                if (screening.getScreeningName() == null || screening.getScreeningName().length() > 16)
                    throw new DbIoException(module + " invalid screeningId '" + screening.getScreeningName()
                        + "' must be a string of 16 chars or less");

                String P_PARAMETER_ID = screening.getParamId();
                String P_PARAMETER_TYPE_ID = screening.getParamTypeId();
                String P_DURATION_ID = screening.getDurationId();

                Logger.instance().info("Creating screening '" + screening.getScreeningName() + "' param="
                    + screening.getParamId() + ", paramType=" + screening.getParamTypeId() + ", dur="
                    + screening.getDurationId() + ", officeId=" + officeId);
//System.out.println("Calling create Screening with P_PARAMETER_ID=" + P_PARAMETER_ID);
                csdbio.createScreeningId(conn,
                    P_SCREENING_ID, P_SCREENING_ID_DESC, P_PARAMETER_ID,
                    P_PARAMETER_TYPE_ID, P_DURATION_ID, P_DB_OFFICE_ID);

                screening.setScreeningCode(getKeyForId(screening.getScreeningName()));
                Logger.instance().info("after creation, screening code = " + screening.getScreeningCode());

                if (DbKey.isNull(screening.getScreeningCode()))
                    throw new DbIoException("Key for screening '" + screening.getScreeningName()
                        + "' does not exist and cannot be created.");
            }
            else // Screening exists. The only thing that can be changed is the description.
            {
                Logger.instance().info("Screening already exists with key=" + screening.getScreeningCode()
                    + ", updating description.");
                csdbio.updateScreeningIdDesc(conn,
                    P_SCREENING_ID, P_SCREENING_ID_DESC, P_DB_OFFICE_ID);
            }

            // Translate the CCP's array list of ScreeningCriteria into an Oracle ScreenCritArray
            ArrayList<ScreeningCriteria> cpCriteriaArray = screening.getCriteriaSeasons();
            if (cpCriteriaArray.size() == 0)
            {
                warning("Screening " + screening.getScreeningName() + " has no criteria sets.");
                return; // No criteria to write
            }

            ScreenCritType oracleScreenCrit[] = new ScreenCritType[cpCriteriaArray.size()];
            for(int critIdx = 0; critIdx < cpCriteriaArray.size(); critIdx++)
            {
                ScreeningCriteria cpCrit = cpCriteriaArray.get(critIdx);

                ArrayList<DurCheckPeriod> cpDurCheckPeriods = cpCrit.getDurCheckPeriods();
                HashMap<String, ScreenDurMagType> season_oracleDurMag = new HashMap<String, ScreenDurMagType>();

                for(int durIdx = 0; durIdx < cpDurCheckPeriods.size(); durIdx++)
                {
                    DurCheckPeriod cpDCP = cpDurCheckPeriods.get(durIdx);

                    ScreenDurMagType oracleDurMagType = season_oracleDurMag.get(cpDCP.getDuration());
                    if (oracleDurMagType == null)
                    {
                        oracleDurMagType = new ScreenDurMagType();
                        oracleDurMagType.setDurationId(cpDCP.getDuration());
                        season_oracleDurMag.put(cpDCP.getDuration(), oracleDurMagType);
                    }
                    if (cpDCP.getFlag() == 'Q')
                    {
                        oracleDurMagType.setQuestionLo(BigDecimal.valueOf(cpDCP.getLow()));
                        oracleDurMagType.setQuestionHi(BigDecimal.valueOf(cpDCP.getHigh()));
                    }
                    else if (cpDCP.getFlag() == 'R')
                    {
                        oracleDurMagType.setRejectLo(BigDecimal.valueOf(cpDCP.getLow()));
                        oracleDurMagType.setRejectHi(BigDecimal.valueOf(cpDCP.getHigh()));
                    }
                }

                ScreenDurMagType oracleDurMagTypeArray[] = new ScreenDurMagType[season_oracleDurMag.size()];
                season_oracleDurMag.values().toArray(oracleDurMagTypeArray);

                AbsCheck qAbsCheck = cpCrit.getAbsCheckFor('Q');
                AbsCheck rAbsCheck = cpCrit.getAbsCheckFor('R');
                RocPerHourCheck qRocCheck = cpCrit.getRocCheckFor('Q');
                RocPerHourCheck rRocCheck = cpCrit.getRocCheckFor('R');
                ConstCheck qConstCheck = cpCrit.getConstCheckFor('Q');
                ConstCheck rConstCheck = cpCrit.getConstCheckFor('R');

                int startDay = cpCrit.getSeasonStart() == null ? 0 : cpCrit.getSeasonStart().get(Calendar.DAY_OF_MONTH);
                int startMonth = cpCrit.getSeasonStart() == null ? 0 : (cpCrit.getSeasonStart().get(Calendar.MONTH)+1);
                info("Preparing crit with season " + startMonth + "/" + startDay);

                oracleScreenCrit[critIdx] = new ScreenCritType(
                    BigDecimal.valueOf(startDay),
                    BigDecimal.valueOf(startMonth),
                    rAbsCheck == null ? null : rAbsCheck.getLow() == Double.NEGATIVE_INFINITY ? null : BigDecimal.valueOf(rAbsCheck.getLow()),
                    rAbsCheck == null ? null : rAbsCheck.getHigh() == Double.POSITIVE_INFINITY ? null : BigDecimal.valueOf(rAbsCheck.getHigh()),
                    qAbsCheck == null ? null : qAbsCheck.getLow() == Double.NEGATIVE_INFINITY ? null : BigDecimal.valueOf(qAbsCheck.getLow()),
                    qAbsCheck == null ? null : qAbsCheck.getHigh() == Double.POSITIVE_INFINITY ? null : BigDecimal.valueOf(qAbsCheck.getHigh()),
                    rRocCheck == null ? null : rRocCheck.getRise() == Double.POSITIVE_INFINITY ? null : BigDecimal.valueOf(rRocCheck.getRise()),
                    rRocCheck == null ? null : rRocCheck.getFall() == Double.NEGATIVE_INFINITY ? null : BigDecimal.valueOf(rRocCheck.getFall()),
                    qRocCheck == null ? null : qRocCheck.getRise() == Double.POSITIVE_INFINITY ? null : BigDecimal.valueOf(qRocCheck.getRise()),
                    qRocCheck == null ? null : qRocCheck.getFall() == Double.NEGATIVE_INFINITY ? null : BigDecimal.valueOf(qRocCheck.getFall()),

                    rConstCheck == null ? null : rConstCheck.getDuration(),
                    rConstCheck == null ? null : BigDecimal.valueOf(rConstCheck.getMinToCheck()),
                    rConstCheck == null ? null : BigDecimal.valueOf(rConstCheck.getTolerance()),
                    rConstCheck == null ? null : BigDecimal.valueOf(rConstCheck.getAllowedMissing()),

                    qConstCheck == null ? null : qConstCheck.getDuration(),
                    qConstCheck == null ? null : BigDecimal.valueOf(qConstCheck.getMinToCheck()),
                    qConstCheck == null ? null : BigDecimal.valueOf(qConstCheck.getTolerance()),
                    qConstCheck == null ? null : BigDecimal.valueOf(qConstCheck.getAllowedMissing()),

                    cpCrit.getEstimateExpression(),
                    Arrays.asList(oracleDurMagTypeArray)
                );
            }

            List<ScreenCritType> oracleScreenCritArray = Arrays.asList(oracleScreenCrit);

            ScreeningControlT screenControlType = new ScreeningControlT(
                screening.isRangeActive() ? "T" : "F",
                screening.isRocActive() ? "T" : "F",
                screening.isConstActive() ? "T" : "F",
                screening.isDurMagActive() ? "T" : "F");

            info("Calling storeScreeningCriteria with " + oracleScreenCrit.length + " seasons.");
            csdbio.storeScreeningCriteria(conn,
                P_SCREENING_ID,
                oracleScreenCritArray,
                "1Hour",
                screenControlType,
                "DELETE INSERT",
                true,
                P_DB_OFFICE_ID,
                screening.getCheckUnitsAbbr());
        }
        catch(SQLException ex)
        {
            String msg = module + ".writeScreening(" + screening.getScreeningName() + ") Error: " + ex;
            Logger.instance().failure(msg);
            System.err.println(msg);
            ex.printStackTrace(System.err);
            throw new DbIoException(msg,ex);
        }

    }

    @Override
    public void deleteScreening(Screening screening)
        throws DbIoException
    {
        try (Connection conn = getConnection();)
        {
            csdbio.deleteScreeningId(conn,
                screening.getScreeningName(),
                screening.getParamId(),
                screening.getParamTypeId(),
                screening.getDurationId(),
                true,
                ((CwmsTimeSeriesDb)db).getDbOfficeId());
        }
        catch (SQLException ex)
        {
            String msg = module + ".deleteScreening() error deleting screening '" + screening.getScreeningName()
                + "': " + ex;
            warning(msg);
            throw new DbIoException(msg);
        }
    }

    @Override
    public Screening getByKey(DbKey screeningCode)
        throws DbIoException, NoSuchObjectException
    {
        if (DbKey.isNull(screeningCode))
            return null;

        Screening tmp = cache.getByKey(screeningCode);
        if (tmp != null)
            return tmp;

        String q = "select distinct " + screenIdColumns + " from " + screenIdTable
            + " where screening_code = ?"
            + " ORDER BY SCREENING_ID";

        try
        {
            final Screening ret = getSingleResult(q,rs-> {
                    return rs2Screening(rs);
                }, screeningCode);

            if (ret == null)
            {
                throw new NoSuchObjectException("No screening with code=" + screeningCode);
            }

            q = "select distinct " + screenControlColumns + " from " + screenControlTable
                + " where screening_code = ?";
            doQuery(q, rs -> rsAddControl(rs, ret),screeningCode);

            q = "select distinct " + screenCritColumns + " from " + screenCritTable
                + " where screening_code = ?"
                + " and unit_system = 'SI'";
            doQuery(q, rs -> rsAddCriteria(rs, ret),screeningCode);

            q = "select distinct " + durMagColumns + " from " + durMagTable
                + " where screening_code = ?"// + screeningCode
                + " and unit_system = 'SI'"
                + " and unit_id = ?"; // + sqlString(ret.getCheckUnitsAbbr());
            doQuery(q, rs -> {
                int day = rs.getInt(2);
                int month = rs.getInt(3);
                ScreeningCriteria crit = getCritFor(ret, day, month);
                if (crit == null)
                {
                    Calendar cal = makeCal(day,month);
                    crit = new ScreeningCriteria(cal);
                    ret.add(crit);
                }
                rsAddDurMag(rs, crit);
            }, screeningCode,ret.getCheckUnitsAbbr());

            checkUnits(ret);
            cache.put(ret);
            return ret;
        }
        catch (SQLException ex)
        {
            String msg = module + ".getByKey(): Error parsing results for query '" + q + "': " + ex;
            throw new DbIoException(msg,ex);
        }
    }

    /*
     * TODO: This can probably be converted to a join and be cleaner.
     */
    @Override
    public List<Screening> getAllScreenings() throws DbIoException
    {
        String q = "select distinct " + screenIdColumns + " from cwms_v_screening_id "
            + " ORDER BY SCREENING_ID";

        try
        {
            final List<Screening> ret = getResults(q, rs -> rs2Screening(rs));
            for(Screening s: ret)
            {
                cache.put(s);
            }

            q = "select distinct " + screenCritColumns + " from " + screenCritTable
                + " where upper(db_office_id) = ?"// + sqlString(((CwmsTimeSeriesDb)db).getDbOfficeId())
                + " and (unit_system = 'SI' or unit_system is null)";
            doQuery(q, rs -> {
                DbKey screeningCode = DbKey.createDbKey(rs, 1);
                Screening screening = null;
                for(Screening s : ret)
                    if (s.getScreeningCode().equals(screeningCode))
                    {
                        screening = s;
                        break;
                    }
                if (screening == null)
                {
                    warning("Screening criteria with code=" + screeningCode + " but there is no "
                        + "matching screening id");
                    return; // shouldn't happen
                }
                rsAddCriteria(rs, screening);
            }, ((CwmsTimeSeriesDb)db).getDbOfficeId());

            q = "select distinct " + screenControlColumns + " from " + screenControlTable;
            doQuery(q, rs -> {
                DbKey screeningCode = DbKey.createDbKey(rs, 1);
                Screening screening = null;
                for(Screening s : ret)
                    if (s.getScreeningCode().equals(screeningCode))
                    {
                        screening = s;
                        break;
                    }
                if (screening == null)
                {
                    warning("Screening criteria with code=" + screeningCode + " but there is no "
                        + "matching screening id");
                    return; // shouldn't happen
                }
                rsAddControl(rs, screening);
            });

            q = "select distinct " + durMagColumns + " from " + durMagTable
                + " where upper(db_office_id) = ? " + sqlString(((CwmsTimeSeriesDb)db).getDbOfficeId())
                + " and (unit_system = 'SI' or unit_system is null)";
            doQuery(q, rs -> {
                DbKey screeningCode = DbKey.createDbKey(rs, 1);
                Screening screening = null;
                for(Screening s : ret)
                    if (s.getScreeningCode().equals(screeningCode))
                    {
                        screening = s;
                        break;
                    }
                if (screening == null)
                    return; // shouldn't happen

                int day = rs.getInt(2);
                int month = rs.getInt(3);
                ScreeningCriteria crit = getCritFor(screening, day, month);
                if (crit == null)
                {
                    Calendar cal = makeCal(day,month);
                    crit = new ScreeningCriteria(cal);
                    screening.add(crit);
                }
                rsAddDurMag(rs, crit);
            },((CwmsTimeSeriesDb)db).getDbOfficeId());

            for(Screening scr : ret)
            {
                checkUnits(scr);
            }

            return ret;
        }
        catch (SQLException ex)
        {
            String msg = module + ".getAllScreenings(): Error parsing results for query '" + q + "': " + ex;
            throw new DbIoException(msg,ex);
        }
    }

    /**
     * Called after freshly reading a screening in SI units from the database.
     * @param scr
     */
    private void checkUnits(Screening scr)
    {
        if (DecodesSettings.instance().screeningUnitSystem.equalsIgnoreCase("SI"))
            return;

        String engUnits =
            ((CwmsTimeSeriesDb)this.db).getBaseParam().getEnglishUnits4Param(
                scr.getParamId());
        if (engUnits != null && !engUnits.equalsIgnoreCase(scr.getCheckUnitsAbbr()))
            try
            {
                scr.convertUnits(engUnits);
            }
            catch (NoConversionException ex)
            {
                warning("Screening '" + scr.getScreeningName() + "': " + ex);
            }
    }


    @Override
    public void clearCache()
    {
        cache.clear();
    }

    @Override
    public TsidScreeningAssignment getScreeningForTS(TimeSeriesIdentifier tsid)
        throws DbIoException
    {
        String q = "select distinct screening_code, active_flag "
            + "from CWMS_V_SCREENED_TS_IDS "
            + "where DB_OFFICE_ID = " + sqlString(((CwmsTimeSeriesDb)db).getDbOfficeId())
            + " and TS_CODE = " + tsid.getKey();
        ResultSet rs = doQuery(q);

        try
        {
            if (rs.next())
            {
                DbKey screeningCode = DbKey.createDbKey(rs, 1);
                boolean active = TextUtil.str2boolean(rs.getString(2));

                Screening screening = null;
                try { screening = getByKey(screeningCode); }
                catch(NoSuchObjectException ex)
                {
                    warning("Screening Code " + screeningCode + " refers to non-existent screening.");
                    return null;
                }

                return new TsidScreeningAssignment(tsid, screening, active);
            }
            else
                return null;
        }
        catch (SQLException ex)
        {
            String msg = module + ".getScreeningForTS() Error reading screening assignment: " + ex;
            throw new DbIoException(msg);
        }
    }



    /**
     * Passed a result set with screenIdColumns, parse & return a Screening object with no criteria.
     * @param rs the result set
     * @return the screening
     * @throws SQLException on error extracting resultset info
     */
    private Screening rs2Screening(ResultSet rs)
        throws SQLException
    {

        DbKey screeningCode = DbKey.createDbKey(rs, 1);
        String id = rs.getString(2);
        String desc = rs.getString(3);
        String paramId = rs.getString(4);

        // Note the screening is created with the storage units ID for the base param ID
        Screening ret = new Screening(screeningCode, id, desc,
            ((CwmsTimeSeriesDb)db).getBaseParam().getStoreUnits4Param(paramId));

        ret.setParamId(paramId);
        ret.setParamTypeId(rs.getString(5));
        ret.setDurationId(rs.getString(6));

        // Now since we always query for SI units, set the check units to this param's
        // SI units
        ret.setCheckUnitsAbbr(((CwmsTimeSeriesDb)db).getBaseParam().getStoreUnits4Param(paramId));

        return ret;
    }


    /**
     * Passed a resultSet with ScreenCritColumns, parse a ScreeningCriteria object
     * and add it to the passed Screening object.
     * @param rs the result set
     * @param screening the screening to which to add
     * @throws SQLException on result set access error
     */
    private void rsAddCriteria(ResultSet rs, Screening screening)
        throws SQLException
    {
//        public static final String screenCritColumns = "SCREENING_CODE, "
//    2        + "SEASON_START_DAY, SEASON_START_MONTH, "
//    4        + "ESTIMATE_EXPRESSION, "
//    5        + "RANGE_REJECT_LO, RANGE_REJECT_HI, RANGE_QUESTION_LO, RANGE_QUESTION_HI, "
//    9        + "RATE_CHANGE_REJECT_RISE, RATE_CHANGE_REJECT_FALL, "
//    11        + "RATE_CHANGE_QUEST_RISE, RATE_CHANGE_QUEST_FALL, "
//    13        + "CONST_REJECT_DURATION, CONST_REJECT_MIN, CONST_REJECT_TOLERANCE, CONST_REJECT_N_MISS, "
//    17        + "CONST_QUEST_DURATION, CONST_QUEST_MIN, CONST_QUEST_TOLERANCE, CONST_QUEST_N_MISS, ";

        int seasonStartDay = rs.getInt(2);
        int seasonStartMonth = rs.getInt(3);
        Calendar seasonStart = seasonStartDay == 0 || seasonStartMonth == 0 ? null :
            makeCal(seasonStartDay, seasonStartMonth);

        ScreeningCriteria crit = new ScreeningCriteria(seasonStart);
        crit.setEstimateExpression(rs.getString(4));

        double lo = rs.getDouble(5);
        if (rs.wasNull())
            lo = Double.NEGATIVE_INFINITY;
        double hi = rs.getDouble(6);
        if (rs.wasNull())
            hi = Double.POSITIVE_INFINITY;
        if (lo != Double.NEGATIVE_INFINITY || hi != Double.POSITIVE_INFINITY)
        {
            crit.addAbsCheck(new AbsCheck('R', lo, hi));
        }

        lo = rs.getDouble(7);
        if (rs.wasNull())
            lo = Double.NEGATIVE_INFINITY;
        hi = rs.getDouble(8);
        if (rs.wasNull())
            hi = Double.POSITIVE_INFINITY;
        if (lo != Double.NEGATIVE_INFINITY || hi != Double.POSITIVE_INFINITY)
        {
            crit.addAbsCheck(new AbsCheck('Q', lo, hi));
        }

        double fall = rs.getDouble(9);
        if (!rs.wasNull())
        {
            double rise = rs.getDouble(10);
            if (!rs.wasNull())
            {
                crit.addRocPerHourCheck(new RocPerHourCheck('R', fall, rise));
            }
        }

        fall = rs.getDouble(11);
        if (!rs.wasNull())
        {
            double rise = rs.getDouble(12);
            if (!rs.wasNull())
            {
                crit.addRocPerHourCheck(new RocPerHourCheck('Q', fall, rise));
            }
        }

        String dur = rs.getString(13);
        if (dur != null && !dur.toLowerCase().startsWith("u"))
        {
            // ctor args: check, duration, min2Check, tolerance, allowedMissing
            crit.addConstCheck(new ConstCheck('R', dur, rs.getDouble(14), rs.getDouble(15), rs.getInt(16)));
        }

        dur = rs.getString(17);
        if (dur != null && !dur.toLowerCase().startsWith("u"))
        {
            // ctor args: check, duration, min2Check, tolerance, allowedMissing
            crit.addConstCheck(new ConstCheck('Q', dur, rs.getDouble(18), rs.getDouble(19), rs.getInt(20)));
        }

// In CCP view, these are moved to separate view.
//        // These flags really should be in the ID view but they are not.
//        // They come from at_screening_criteria and are only available in the criteria view.
//        screening.setRangeActive(TextUtil.str2boolean(rs.getString(21)));
//        screening.setRocActive(TextUtil.str2boolean(rs.getString(22)));
//        screening.setConstActive(TextUtil.str2boolean(rs.getString(23)));
//        screening.setDurMagActive(TextUtil.str2boolean(rs.getString(24)));

        screening.add(crit);
    }


    private void rsAddControl(ResultSet rs, Screening screening)
        throws SQLException
    {
//        public static final String screenControlColumns = "SCREENING_CODE, "
//            + "RANGE_ACTIVE_FLAG, RATE_CHANGE_ACTIVE_FLAG, CONST_ACTIVE_FLAG, DUR_MAG_ACTIVE_FLAG";
        screening.setRangeActive(TextUtil.str2boolean(rs.getString(2)));
        screening.setRocActive(TextUtil.str2boolean(rs.getString(3)));
        screening.setConstActive(TextUtil.str2boolean(rs.getString(4)));
        screening.setDurMagActive(TextUtil.str2boolean(rs.getString(5)));
    }



    /**
     * Passed a result set for durMagColumns and a criteria object, parse the coefficients and
     * add the appropriate DurCheckPeriod objects
     * @param rs the result set
     * @param crit the ScreeningCriteria
     * @throws SQLException on parsing result set
     */
    private void rsAddDurMag(ResultSet rs, ScreeningCriteria crit)
        throws SQLException
    {
//        public static final String durMagColumns = "SCREENING_CODE, SEASON_START_DAY, SEASON_START_MONTH, "
//            + "DUR_MAG_DURATION_ID, REJECT_LO, REJECT_HI, QUESTION_LO, QUESTION_HI";

        String dur = rs.getString(4);
        double lo = rs.getDouble(5);
        if (rs.wasNull())
            lo = Double.NEGATIVE_INFINITY;
        double hi = rs.getDouble(6);
        if (rs.wasNull())
            hi = Double.POSITIVE_INFINITY;
        if (lo != Double.NEGATIVE_INFINITY || hi != Double.POSITIVE_INFINITY)
            crit.addDurCheckPeriod(new DurCheckPeriod('R', dur, lo, hi));

        lo = rs.getDouble(7);
        if (rs.wasNull())
            lo = Double.NEGATIVE_INFINITY;
        hi = rs.getDouble(8);
        if (rs.wasNull())
            hi = Double.POSITIVE_INFINITY;
        if (lo != Double.NEGATIVE_INFINITY || hi != Double.POSITIVE_INFINITY)
            crit.addDurCheckPeriod(new DurCheckPeriod('Q', dur, lo, hi));
    }

    /**
     * Pass screening and day & month as they exist in the table.
     * @param screening the screening
     * @param day the day in the table (0 means null)
     * @param month the month in the table (0 means null)
     * @return the matching criteria object, or null if not found
     */
    private ScreeningCriteria getCritFor(Screening screening, int day, int month)
    {
        for (ScreeningCriteria crit : screening.getCriteriaSeasons())
            if (crit.getSeasonStart() == null)
            {
                if (day == 0 || month == 0)
                    return crit;
            }
            else if (crit.getSeasonStart().get(Calendar.MONTH)+1 == month
                  && crit.getSeasonStart().get(Calendar.DAY_OF_MONTH) == day)
                return crit;
        return null;
    }

    /**
     * Given a string screening ID, return the surrogate key, or null if none exists
     * @param screeningId the unique string screening ID
     * @return surrogate key or DbKey.NullKey if no match in database
     * @throws DbIoException on database SQL error
     */
    @Override
    public DbKey getKeyForId(String screeningId)
        throws DbIoException
    {
        try (Connection conn = getConnection();)
        {
            String P_SCREENING_ID = screeningId;
            long P_DB_OFFICE_CODE = ((CwmsTimeSeriesDb)db).getDbOfficeCode().getValue();
            BigDecimal O_RET = csdbio.getScreeningCode(conn, P_SCREENING_ID, (double)P_DB_OFFICE_CODE);
            return O_RET == null ? DbKey.NullKey : DbKey.createDbKey(O_RET.longValue());
        }
        catch (SQLException ex)
        {
            if (ex.toString().contains("DOES_NOT_EXIST"))
                return DbKey.NullKey;

            String msg = module + " getKeyForId(" + screeningId + ") error: " + ex;
            Logger.instance().failure(msg);
            System.err.println(msg);
            ex.printStackTrace(System.err);
            throw new DbIoException(msg,ex);
        }
    }

    /**
     * Passed day & month as they are in the table, return a calendar set to
     * midnight on the specified day/month. Return null if day or month are out of range.
     * Day and month should be 0 (meaning they are null in the table) for criteria that
     * should apply all year.
     * @param day the day, or 0 if all-year
     * @param month the month, or 0 if all-year
     * @return the calender or null
     */
    private Calendar makeCal(int day, int month)
    {
        Calendar cal = null;
        if (day >=1 && day <= 31
         && month >= 1 && month <= 12)
        {
            cal = Calendar.getInstance();
            cal.set(Calendar.MONTH, month-1);
            cal.set(Calendar.DAY_OF_MONTH, day);
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
        }
Logger.instance().debug1("ScreeningDAO.makeCal day=" + day + ", mon=" + month
+ ", calendar day=" + cal.get(Calendar.DAY_OF_MONTH) + ", cal.mon=" + cal.get(Calendar.MONTH));
        return cal;
    }

    @Override
    public ArrayList<TsidScreeningAssignment> getTsidScreeningAssignments(boolean activeOnly)
        throws DbIoException
    {
        String q = "select distinct screening_code, ts_code, active_flag "
            + "from CWMS_V_SCREENED_TS_IDS "
            + "where DB_OFFICE_ID = ?";
        if (activeOnly)
            q = q + " and (upper(ACTIVE_FLAG) = 'T' or upper(ACTIVE_FLAG) = 'Y')";

        final ArrayList<TsidScreeningAssignment> ret = new ArrayList<TsidScreeningAssignment>();

        class TSA
        {
            DbKey screeningCode; DbKey tsCode; boolean active;
            TSA(DbKey sc, DbKey tc, boolean ac) { screeningCode = sc; tsCode = tc; active=ac; }
        };

        ArrayList<TSA> results = new ArrayList<TSA>();

        try (TimeSeriesDAI timeSeriesDAO = db.makeTimeSeriesDAO();)
        {
            doQuery(q,rs -> {
                DbKey screeningCode = DbKey.createDbKey(rs, 1);
                DbKey tsCode = DbKey.createDbKey(rs, 2);
                boolean active = TextUtil.str2boolean(rs.getString(3));
                results.add(new TSA(screeningCode, tsCode, active));
            }, ((CwmsTimeSeriesDb)db).getDbOfficeId());
            // Collect results first, then lookup objects to avoid nested queries.
            for(TSA p : results)
            {
                Screening screening = null;
                try { screening = getByKey(p.screeningCode); }
                catch(NoSuchObjectException ex)
                {
                    warning("Screening Code " + p.screeningCode + " refers to non-existent screening.");
                    continue;
                }
                TimeSeriesIdentifier tsid = null;
                try
                {
                    tsid = timeSeriesDAO.getTimeSeriesIdentifier(p.tsCode);
                }
                catch (NoSuchObjectException ex)
                {
                    warning("Time Series Code " + p.tsCode + " for screening Screening Code " + p.screeningCode
                        + " refers to non-existent time series.");
                    continue;
                }
                ret.add(new TsidScreeningAssignment(tsid, screening, p.active));
            }
        }
        catch(SQLException ex)
        {
            warning(module + ".getTsidScreeningAssociations() error in query '" + q + "': " + ex);
        }
        return ret;
    }

    @Override
    public void assignScreening(Screening screening, TimeSeriesIdentifier tsid, boolean active)
        throws DbIoException
    {
        // Note: CCP ignores the resultant TS ID in the table. Results are assigned via CCP output params.

        try (Connection conn = getConnection())
        {
            List<ScreenAssignT> screenAssignTS = Collections.singletonList(
                new ScreenAssignT(tsid.getUniqueString(), active, null));

            csdbio.assignScreeningId(conn,
                screening.getScreeningName(),
                screenAssignTS, ((CwmsTimeSeriesDb)db).getDbOfficeId());
        }
        catch (SQLException ex)
        {
            String msg = module + ".assignScreening() error assigning screening '"
                + screening.getScreeningName() + "' to tsid '" + tsid.getUniqueString() + " with activeFlag="
                + active + ": " + ex;
            warning(msg);
            PrintStream ps = Logger.instance().getLogOutput();
            if (ps != null)
            {
                ps.println("Cause is: " + ex);
                ex.printStackTrace(ps);
            }
            throw new DbIoException(msg,ex);
        }
    }

    @Override
    public void unassignScreening(Screening screening, TimeSeriesIdentifier tsid) throws DbIoException
    {
        try (Connection conn = getConnection();)
        {
            csdbio.unassignScreeningId(conn,
                screening.getScreeningName(),
                Collections.singletonList(tsid != null ? tsid.getUniqueString() : null),
                tsid == null,
                ((CwmsTimeSeriesDb)db).getDbOfficeId());
        }
        catch (SQLException ex)
        {
            throw new DbIoException("Unable to unassign screening", ex);
        }
    }

    @Override
    public void renameScreening(String oldId, String newId) throws DbIoException
    {
        try (Connection conn = getConnection();)
        {
            Logger.instance().info("Renaming screening '" + oldId + "' to '" + newId + "'");
            csdbio.renameScreeningId(conn, oldId, newId, ((CwmsTimeSeriesDb)db).getDbOfficeId());
        }
        catch (SQLException ex)
        {
            throw new DbIoException(module + ".renameScreening(" + oldId + ", " + newId
                + ": Error: " + ex, ex);
        }
    }

    @Override
    public void updateScreeningDescription(String screeningId, String desc) throws DbIoException
    {

        try (Connection conn = getConnection();)
        {
            csdbio.updateScreeningIdDesc(conn,
                screeningId,
                desc,
                ((CwmsTimeSeriesDb)db).getDbOfficeId());
        }
        catch (SQLException ex)
        {
            throw new DbIoException(module + ".renameScreening(" + screeningId + ", " + desc
                + ": Error: " + ex,ex);
        }
    }
}
