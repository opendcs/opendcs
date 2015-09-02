package decodes.cwms.validation.db;

import java.sql.SQLException;
import java.sql.Connection;
import oracle.jdbc.OracleTypes;
import oracle.sql.ORAData;
import oracle.sql.ORADataFactory;
import oracle.sql.Datum;
import oracle.sql.STRUCT;
import oracle.jpub.runtime.MutableStruct;

public class ScreenDurMagType implements ORAData, ORADataFactory
{
  public static final String _SQL_NAME = "CWMS_20.SCREEN_DUR_MAG_TYPE";
  public static final int _SQL_TYPECODE = OracleTypes.STRUCT;

  protected MutableStruct _struct;

  protected static int[] _sqlType =  { 12,2,2,2,2 };
  protected static ORADataFactory[] _factory = new ORADataFactory[5];
  protected static final ScreenDurMagType _ScreenDurMagTypeFactory = new ScreenDurMagType();

  public static ORADataFactory getORADataFactory()
  { return _ScreenDurMagTypeFactory; }
  /* constructors */
  protected void _init_struct(boolean init)
  { if (init) _struct = new MutableStruct(new Object[5], _sqlType, _factory); }
  public ScreenDurMagType()
  { _init_struct(true); }
  public ScreenDurMagType(oracle.sql.CHAR durationId, oracle.sql.NUMBER rejectLo, oracle.sql.NUMBER rejectHi, oracle.sql.NUMBER questionLo, oracle.sql.NUMBER questionHi) throws SQLException
  { _init_struct(true);
    setDurationId(durationId);
    setRejectLo(rejectLo);
    setRejectHi(rejectHi);
    setQuestionLo(questionLo);
    setQuestionHi(questionHi);
  }

  /* ORAData interface */
  public Datum toDatum(Connection c) throws SQLException
  {
    return _struct.toDatum(c, _SQL_NAME);
  }


  /* ORADataFactory interface */
  public ORAData create(Datum d, int sqlType) throws SQLException
  { return create(null, d, sqlType); }
  protected ORAData create(ScreenDurMagType o, Datum d, int sqlType) throws SQLException
  {
    if (d == null) return null; 
    if (o == null) o = new ScreenDurMagType();
    o._struct = new MutableStruct((STRUCT) d, _sqlType, _factory);
    return o;
  }
  /* accessor methods */
  public oracle.sql.CHAR getDurationId() throws SQLException
  { return (oracle.sql.CHAR) _struct.getOracleAttribute(0); }

  public void setDurationId(oracle.sql.CHAR durationId) throws SQLException
  { _struct.setOracleAttribute(0, durationId); }


  public oracle.sql.NUMBER getRejectLo() throws SQLException
  { return (oracle.sql.NUMBER) _struct.getOracleAttribute(1); }

  public void setRejectLo(oracle.sql.NUMBER rejectLo) throws SQLException
  { _struct.setOracleAttribute(1, rejectLo); }


  public oracle.sql.NUMBER getRejectHi() throws SQLException
  { return (oracle.sql.NUMBER) _struct.getOracleAttribute(2); }

  public void setRejectHi(oracle.sql.NUMBER rejectHi) throws SQLException
  { _struct.setOracleAttribute(2, rejectHi); }


  public oracle.sql.NUMBER getQuestionLo() throws SQLException
  { return (oracle.sql.NUMBER) _struct.getOracleAttribute(3); }

  public void setQuestionLo(oracle.sql.NUMBER questionLo) throws SQLException
  { _struct.setOracleAttribute(3, questionLo); }


  public oracle.sql.NUMBER getQuestionHi() throws SQLException
  { return (oracle.sql.NUMBER) _struct.getOracleAttribute(4); }

  public void setQuestionHi(oracle.sql.NUMBER questionHi) throws SQLException
  { _struct.setOracleAttribute(4, questionHi); }

  public String toString()
  { try {
     return "CWMS_20.SCREEN_DUR_MAG_TYPE" + "(" +
       ((getDurationId()==null)?"null": "'" + getDurationId()+"'" ) + "," +
       ((getRejectLo()==null)?"null": getRejectLo().stringValue()) + "," +
       ((getRejectHi()==null)?"null": getRejectHi().stringValue()) + "," +
       ((getQuestionLo()==null)?"null": getQuestionLo().stringValue()) + "," +
       ((getQuestionHi()==null)?"null": getQuestionHi().stringValue()) +
     ")";
    } catch (Exception e) { return e.toString(); }
  }

}
