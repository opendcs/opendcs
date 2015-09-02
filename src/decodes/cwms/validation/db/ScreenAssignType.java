package decodes.cwms.validation.db;

import java.sql.SQLException;
import java.sql.Connection;
import oracle.jdbc.OracleTypes;
import oracle.sql.ORAData;
import oracle.sql.ORADataFactory;
import oracle.sql.Datum;
import oracle.sql.STRUCT;
import oracle.jpub.runtime.MutableStruct;

public class ScreenAssignType implements ORAData, ORADataFactory
{
  public static final String _SQL_NAME = "CWMS_20.SCREEN_ASSIGN_T";
  public static final int _SQL_TYPECODE = OracleTypes.STRUCT;

  protected MutableStruct _struct;

  protected static int[] _sqlType =  { 12,12,12 };
  protected static ORADataFactory[] _factory = new ORADataFactory[3];
  protected static final ScreenAssignType _ScreenAssignTypeFactory = new ScreenAssignType();

  public static ORADataFactory getORADataFactory()
  { return _ScreenAssignTypeFactory; }
  /* constructors */
  protected void _init_struct(boolean init)
  { if (init) _struct = new MutableStruct(new Object[3], _sqlType, _factory); }
  public ScreenAssignType()
  { _init_struct(true); }
  public ScreenAssignType(oracle.sql.CHAR cwmsTsId, oracle.sql.CHAR activeFlag, oracle.sql.CHAR resultantTsId) throws SQLException
  { _init_struct(true);
    setCwmsTsId(cwmsTsId);
    setActiveFlag(activeFlag);
    setResultantTsId(resultantTsId);
  }

  /* ORAData interface */
  public Datum toDatum(Connection c) throws SQLException
  {
    return _struct.toDatum(c, _SQL_NAME);
  }


  /* ORADataFactory interface */
  public ORAData create(Datum d, int sqlType) throws SQLException
  { return create(null, d, sqlType); }
  protected ORAData create(ScreenAssignType o, Datum d, int sqlType) throws SQLException
  {
    if (d == null) return null; 
    if (o == null) o = new ScreenAssignType();
    o._struct = new MutableStruct((STRUCT) d, _sqlType, _factory);
    return o;
  }
  /* accessor methods */
  public oracle.sql.CHAR getCwmsTsId() throws SQLException
  { return (oracle.sql.CHAR) _struct.getOracleAttribute(0); }

  public void setCwmsTsId(oracle.sql.CHAR cwmsTsId) throws SQLException
  { _struct.setOracleAttribute(0, cwmsTsId); }


  public oracle.sql.CHAR getActiveFlag() throws SQLException
  { return (oracle.sql.CHAR) _struct.getOracleAttribute(1); }

  public void setActiveFlag(oracle.sql.CHAR activeFlag) throws SQLException
  { _struct.setOracleAttribute(1, activeFlag); }


  public oracle.sql.CHAR getResultantTsId() throws SQLException
  { return (oracle.sql.CHAR) _struct.getOracleAttribute(2); }

  public void setResultantTsId(oracle.sql.CHAR resultantTsId) throws SQLException
  { _struct.setOracleAttribute(2, resultantTsId); }

  public String toString()
  { try {
     return "CWMS_20.SCREEN_ASSIGN_T" + "(" +
       ((getCwmsTsId()==null)?"null": "'" + getCwmsTsId()+"'" ) + "," +
       ((getActiveFlag()==null)?"null": "'" + getActiveFlag()+"'" ) + "," +
       ((getResultantTsId()==null)?"null": "'" + getResultantTsId()+"'" ) +
     ")";
    } catch (Exception e) { return e.toString(); }
  }

}
