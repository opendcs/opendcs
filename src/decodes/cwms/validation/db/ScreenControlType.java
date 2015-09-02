package decodes.cwms.validation.db;

import java.sql.SQLException;
import java.sql.Connection;
import oracle.jdbc.OracleTypes;
import oracle.sql.ORAData;
import oracle.sql.ORADataFactory;
import oracle.sql.Datum;
import oracle.sql.STRUCT;
import oracle.jpub.runtime.MutableStruct;

public class ScreenControlType implements ORAData, ORADataFactory
{
  public static final String _SQL_NAME = "CWMS_20.SCREENING_CONTROL_T";
  public static final int _SQL_TYPECODE = OracleTypes.STRUCT;

  protected MutableStruct _struct;

  protected static int[] _sqlType =  { 12,12,12,12 };
  protected static ORADataFactory[] _factory = new ORADataFactory[4];
  protected static final ScreenControlType _ScreenControlTypeFactory = new ScreenControlType();

  public static ORADataFactory getORADataFactory()
  { return _ScreenControlTypeFactory; }
  /* constructors */
  protected void _init_struct(boolean init)
  { if (init) _struct = new MutableStruct(new Object[4], _sqlType, _factory); }
  public ScreenControlType()
  { _init_struct(true); }
  public ScreenControlType(oracle.sql.CHAR rangeActiveFlag, oracle.sql.CHAR rateChangeActiveFlag, oracle.sql.CHAR constActiveFlag, oracle.sql.CHAR durMagActiveFlag) throws SQLException
  { _init_struct(true);
    setRangeActiveFlag(rangeActiveFlag);
    setRateChangeActiveFlag(rateChangeActiveFlag);
    setConstActiveFlag(constActiveFlag);
    setDurMagActiveFlag(durMagActiveFlag);
  }

  /* ORAData interface */
  public Datum toDatum(Connection c) throws SQLException
  {
    return _struct.toDatum(c, _SQL_NAME);
  }


  /* ORADataFactory interface */
  public ORAData create(Datum d, int sqlType) throws SQLException
  { return create(null, d, sqlType); }
  protected ORAData create(ScreenControlType o, Datum d, int sqlType) throws SQLException
  {
    if (d == null) return null; 
    if (o == null) o = new ScreenControlType();
    o._struct = new MutableStruct((STRUCT) d, _sqlType, _factory);
    return o;
  }
  /* accessor methods */
  public oracle.sql.CHAR getRangeActiveFlag() throws SQLException
  { return (oracle.sql.CHAR) _struct.getOracleAttribute(0); }

  public void setRangeActiveFlag(oracle.sql.CHAR rangeActiveFlag) throws SQLException
  { _struct.setOracleAttribute(0, rangeActiveFlag); }


  public oracle.sql.CHAR getRateChangeActiveFlag() throws SQLException
  { return (oracle.sql.CHAR) _struct.getOracleAttribute(1); }

  public void setRateChangeActiveFlag(oracle.sql.CHAR rateChangeActiveFlag) throws SQLException
  { _struct.setOracleAttribute(1, rateChangeActiveFlag); }


  public oracle.sql.CHAR getConstActiveFlag() throws SQLException
  { return (oracle.sql.CHAR) _struct.getOracleAttribute(2); }

  public void setConstActiveFlag(oracle.sql.CHAR constActiveFlag) throws SQLException
  { _struct.setOracleAttribute(2, constActiveFlag); }


  public oracle.sql.CHAR getDurMagActiveFlag() throws SQLException
  { return (oracle.sql.CHAR) _struct.getOracleAttribute(3); }

  public void setDurMagActiveFlag(oracle.sql.CHAR durMagActiveFlag) throws SQLException
  { _struct.setOracleAttribute(3, durMagActiveFlag); }

  public String toString()
  { try {
     return "CWMS_20.SCREENING_CONTROL_T" + "(" +
       ((getRangeActiveFlag()==null)?"null": "'" + getRangeActiveFlag()+"'" ) + "," +
       ((getRateChangeActiveFlag()==null)?"null": "'" + getRateChangeActiveFlag()+"'" ) + "," +
       ((getConstActiveFlag()==null)?"null": "'" + getConstActiveFlag()+"'" ) + "," +
       ((getDurMagActiveFlag()==null)?"null": "'" + getDurMagActiveFlag()+"'" ) +
     ")";
    } catch (Exception e) { return e.toString(); }
  }

}
