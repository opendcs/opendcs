package decodes.cwms.validation.db;

import java.sql.SQLException;
import java.sql.Connection;
import oracle.jdbc.OracleTypes;
import oracle.sql.ORAData;
import oracle.sql.ORADataFactory;
import oracle.sql.Datum;
import oracle.sql.STRUCT;
import oracle.jpub.runtime.MutableStruct;

public class ScreenCritType implements ORAData, ORADataFactory
{
  public static final String _SQL_NAME = "CWMS_20.SCREEN_CRIT_TYPE";
  public static final int _SQL_TYPECODE = OracleTypes.STRUCT;

  protected MutableStruct _struct;

  protected static int[] _sqlType =  { 2,2,2,2,2,2,2,2,2,2,12,2,2,2,12,2,2,2,12,2003 };
  protected static ORADataFactory[] _factory = new ORADataFactory[20];
  static
  {
    _factory[19] = ScreenDurMagArray.getORADataFactory();
  }
  protected static final ScreenCritType _ScreenCritTypeFactory = new ScreenCritType();

  public static ORADataFactory getORADataFactory()
  { return _ScreenCritTypeFactory; }
  /* constructors */
  protected void _init_struct(boolean init)
  { if (init) _struct = new MutableStruct(new Object[20], _sqlType, _factory); }
  public ScreenCritType()
  { _init_struct(true); }
  public ScreenCritType(oracle.sql.NUMBER seasonStartDay, oracle.sql.NUMBER seasonStartMonth, oracle.sql.NUMBER rangeRejectLo, oracle.sql.NUMBER rangeRejectHi, oracle.sql.NUMBER rangeQuestionLo, oracle.sql.NUMBER rangeQuestionHi, oracle.sql.NUMBER rateChangeRejectRise, oracle.sql.NUMBER rateChangeRejectFall, oracle.sql.NUMBER rateChangeQuestRise, oracle.sql.NUMBER rateChangeQuestFall, oracle.sql.CHAR constRejectDurationId, oracle.sql.NUMBER constRejectMin, oracle.sql.NUMBER constRejectTolerance, oracle.sql.NUMBER constRejectNMiss, oracle.sql.CHAR constQuestDurationId, oracle.sql.NUMBER constQuestMin, oracle.sql.NUMBER constQuestTolerance, oracle.sql.NUMBER constQuestNMiss, oracle.sql.CHAR estimateExpression, ScreenDurMagArray durMagArray) throws SQLException
  { _init_struct(true);
    setSeasonStartDay(seasonStartDay);
    setSeasonStartMonth(seasonStartMonth);
    setRangeRejectLo(rangeRejectLo);
    setRangeRejectHi(rangeRejectHi);
    setRangeQuestionLo(rangeQuestionLo);
    setRangeQuestionHi(rangeQuestionHi);
    setRateChangeRejectRise(rateChangeRejectRise);
    setRateChangeRejectFall(rateChangeRejectFall);
    setRateChangeQuestRise(rateChangeQuestRise);
    setRateChangeQuestFall(rateChangeQuestFall);
    setConstRejectDurationId(constRejectDurationId);
    setConstRejectMin(constRejectMin);
    setConstRejectTolerance(constRejectTolerance);
    setConstRejectNMiss(constRejectNMiss);
    setConstQuestDurationId(constQuestDurationId);
    setConstQuestMin(constQuestMin);
    setConstQuestTolerance(constQuestTolerance);
    setConstQuestNMiss(constQuestNMiss);
    setEstimateExpression(estimateExpression);
    setDurMagArray(durMagArray);
  }

  /* ORAData interface */
  public Datum toDatum(Connection c) throws SQLException
  {
    return _struct.toDatum(c, _SQL_NAME);
  }


  /* ORADataFactory interface */
  public ORAData create(Datum d, int sqlType) throws SQLException
  { return create(null, d, sqlType); }
  protected ORAData create(ScreenCritType o, Datum d, int sqlType) throws SQLException
  {
    if (d == null) return null; 
    if (o == null) o = new ScreenCritType();
    o._struct = new MutableStruct((STRUCT) d, _sqlType, _factory);
    return o;
  }
  /* accessor methods */
  public oracle.sql.NUMBER getSeasonStartDay() throws SQLException
  { return (oracle.sql.NUMBER) _struct.getOracleAttribute(0); }

  public void setSeasonStartDay(oracle.sql.NUMBER seasonStartDay) throws SQLException
  { _struct.setOracleAttribute(0, seasonStartDay); }


  public oracle.sql.NUMBER getSeasonStartMonth() throws SQLException
  { return (oracle.sql.NUMBER) _struct.getOracleAttribute(1); }

  public void setSeasonStartMonth(oracle.sql.NUMBER seasonStartMonth) throws SQLException
  { _struct.setOracleAttribute(1, seasonStartMonth); }


  public oracle.sql.NUMBER getRangeRejectLo() throws SQLException
  { return (oracle.sql.NUMBER) _struct.getOracleAttribute(2); }

  public void setRangeRejectLo(oracle.sql.NUMBER rangeRejectLo) throws SQLException
  { _struct.setOracleAttribute(2, rangeRejectLo); }


  public oracle.sql.NUMBER getRangeRejectHi() throws SQLException
  { return (oracle.sql.NUMBER) _struct.getOracleAttribute(3); }

  public void setRangeRejectHi(oracle.sql.NUMBER rangeRejectHi) throws SQLException
  { _struct.setOracleAttribute(3, rangeRejectHi); }


  public oracle.sql.NUMBER getRangeQuestionLo() throws SQLException
  { return (oracle.sql.NUMBER) _struct.getOracleAttribute(4); }

  public void setRangeQuestionLo(oracle.sql.NUMBER rangeQuestionLo) throws SQLException
  { _struct.setOracleAttribute(4, rangeQuestionLo); }


  public oracle.sql.NUMBER getRangeQuestionHi() throws SQLException
  { return (oracle.sql.NUMBER) _struct.getOracleAttribute(5); }

  public void setRangeQuestionHi(oracle.sql.NUMBER rangeQuestionHi) throws SQLException
  { _struct.setOracleAttribute(5, rangeQuestionHi); }


  public oracle.sql.NUMBER getRateChangeRejectRise() throws SQLException
  { return (oracle.sql.NUMBER) _struct.getOracleAttribute(6); }

  public void setRateChangeRejectRise(oracle.sql.NUMBER rateChangeRejectRise) throws SQLException
  { _struct.setOracleAttribute(6, rateChangeRejectRise); }


  public oracle.sql.NUMBER getRateChangeRejectFall() throws SQLException
  { return (oracle.sql.NUMBER) _struct.getOracleAttribute(7); }

  public void setRateChangeRejectFall(oracle.sql.NUMBER rateChangeRejectFall) throws SQLException
  { _struct.setOracleAttribute(7, rateChangeRejectFall); }


  public oracle.sql.NUMBER getRateChangeQuestRise() throws SQLException
  { return (oracle.sql.NUMBER) _struct.getOracleAttribute(8); }

  public void setRateChangeQuestRise(oracle.sql.NUMBER rateChangeQuestRise) throws SQLException
  { _struct.setOracleAttribute(8, rateChangeQuestRise); }


  public oracle.sql.NUMBER getRateChangeQuestFall() throws SQLException
  { return (oracle.sql.NUMBER) _struct.getOracleAttribute(9); }

  public void setRateChangeQuestFall(oracle.sql.NUMBER rateChangeQuestFall) throws SQLException
  { _struct.setOracleAttribute(9, rateChangeQuestFall); }


  public oracle.sql.CHAR getConstRejectDurationId() throws SQLException
  { return (oracle.sql.CHAR) _struct.getOracleAttribute(10); }

  public void setConstRejectDurationId(oracle.sql.CHAR constRejectDurationId) throws SQLException
  { _struct.setOracleAttribute(10, constRejectDurationId); }


  public oracle.sql.NUMBER getConstRejectMin() throws SQLException
  { return (oracle.sql.NUMBER) _struct.getOracleAttribute(11); }

  public void setConstRejectMin(oracle.sql.NUMBER constRejectMin) throws SQLException
  { _struct.setOracleAttribute(11, constRejectMin); }


  public oracle.sql.NUMBER getConstRejectTolerance() throws SQLException
  { return (oracle.sql.NUMBER) _struct.getOracleAttribute(12); }

  public void setConstRejectTolerance(oracle.sql.NUMBER constRejectTolerance) throws SQLException
  { _struct.setOracleAttribute(12, constRejectTolerance); }


  public oracle.sql.NUMBER getConstRejectNMiss() throws SQLException
  { return (oracle.sql.NUMBER) _struct.getOracleAttribute(13); }

  public void setConstRejectNMiss(oracle.sql.NUMBER constRejectNMiss) throws SQLException
  { _struct.setOracleAttribute(13, constRejectNMiss); }


  public oracle.sql.CHAR getConstQuestDurationId() throws SQLException
  { return (oracle.sql.CHAR) _struct.getOracleAttribute(14); }

  public void setConstQuestDurationId(oracle.sql.CHAR constQuestDurationId) throws SQLException
  { _struct.setOracleAttribute(14, constQuestDurationId); }


  public oracle.sql.NUMBER getConstQuestMin() throws SQLException
  { return (oracle.sql.NUMBER) _struct.getOracleAttribute(15); }

  public void setConstQuestMin(oracle.sql.NUMBER constQuestMin) throws SQLException
  { _struct.setOracleAttribute(15, constQuestMin); }


  public oracle.sql.NUMBER getConstQuestTolerance() throws SQLException
  { return (oracle.sql.NUMBER) _struct.getOracleAttribute(16); }

  public void setConstQuestTolerance(oracle.sql.NUMBER constQuestTolerance) throws SQLException
  { _struct.setOracleAttribute(16, constQuestTolerance); }


  public oracle.sql.NUMBER getConstQuestNMiss() throws SQLException
  { return (oracle.sql.NUMBER) _struct.getOracleAttribute(17); }

  public void setConstQuestNMiss(oracle.sql.NUMBER constQuestNMiss) throws SQLException
  { _struct.setOracleAttribute(17, constQuestNMiss); }


  public oracle.sql.CHAR getEstimateExpression() throws SQLException
  { return (oracle.sql.CHAR) _struct.getOracleAttribute(18); }

  public void setEstimateExpression(oracle.sql.CHAR estimateExpression) throws SQLException
  { _struct.setOracleAttribute(18, estimateExpression); }


  public ScreenDurMagArray getDurMagArray() throws SQLException
  { return (ScreenDurMagArray) _struct.getAttribute(19); }

  public void setDurMagArray(ScreenDurMagArray durMagArray) throws SQLException
  { _struct.setAttribute(19, durMagArray); }

  public String toString()
  { try {
     return "CWMS_20.SCREEN_CRIT_TYPE" + "(" +
       ((getSeasonStartDay()==null)?"null": getSeasonStartDay().stringValue()) + "," +
       ((getSeasonStartMonth()==null)?"null": getSeasonStartMonth().stringValue()) + "," +
       ((getRangeRejectLo()==null)?"null": getRangeRejectLo().stringValue()) + "," +
       ((getRangeRejectHi()==null)?"null": getRangeRejectHi().stringValue()) + "," +
       ((getRangeQuestionLo()==null)?"null": getRangeQuestionLo().stringValue()) + "," +
       ((getRangeQuestionHi()==null)?"null": getRangeQuestionHi().stringValue()) + "," +
       ((getRateChangeRejectRise()==null)?"null": getRateChangeRejectRise().stringValue()) + "," +
       ((getRateChangeRejectFall()==null)?"null": getRateChangeRejectFall().stringValue()) + "," +
       ((getRateChangeQuestRise()==null)?"null": getRateChangeQuestRise().stringValue()) + "," +
       ((getRateChangeQuestFall()==null)?"null": getRateChangeQuestFall().stringValue()) + "," +
       ((getConstRejectDurationId()==null)?"null": "'" + getConstRejectDurationId()+"'" ) + "," +
       ((getConstRejectMin()==null)?"null": getConstRejectMin().stringValue()) + "," +
       ((getConstRejectTolerance()==null)?"null": getConstRejectTolerance().stringValue()) + "," +
       ((getConstRejectNMiss()==null)?"null": getConstRejectNMiss().stringValue()) + "," +
       ((getConstQuestDurationId()==null)?"null": "'" + getConstQuestDurationId()+"'" ) + "," +
       ((getConstQuestMin()==null)?"null": getConstQuestMin().stringValue()) + "," +
       ((getConstQuestTolerance()==null)?"null": getConstQuestTolerance().stringValue()) + "," +
       ((getConstQuestNMiss()==null)?"null": getConstQuestNMiss().stringValue()) + "," +
       ((getEstimateExpression()==null)?"null": "'" + getEstimateExpression()+"'" ) + "," +
       getDurMagArray() +
     ")";
    } catch (Exception e) { return e.toString(); }
  }

}
