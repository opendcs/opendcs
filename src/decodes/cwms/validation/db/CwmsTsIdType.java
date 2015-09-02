package decodes.cwms.validation.db;

import java.sql.SQLException;
import java.sql.Connection;
import oracle.jdbc.OracleTypes;
import oracle.sql.ORAData;
import oracle.sql.ORADataFactory;
import oracle.sql.Datum;
import oracle.sql.STRUCT;
import oracle.jpub.runtime.MutableStruct;

public class CwmsTsIdType implements ORAData, ORADataFactory
{
  public static final String _SQL_NAME = "CWMS_20.CWMS_TS_ID_T";
  public static final int _SQL_TYPECODE = OracleTypes.STRUCT;

  protected MutableStruct _struct;

  protected static int[] _sqlType =  { 12 };
  protected static ORADataFactory[] _factory = new ORADataFactory[1];
  protected static final CwmsTsIdType _CwmsTsIdTypeFactory = new CwmsTsIdType();

  public static ORADataFactory getORADataFactory()
  { return _CwmsTsIdTypeFactory; }
  /* constructors */
  protected void _init_struct(boolean init)
  { if (init) _struct = new MutableStruct(new Object[1], _sqlType, _factory); }
  public CwmsTsIdType()
  { _init_struct(true); }
  public CwmsTsIdType(oracle.sql.CHAR cwmsTsId) throws SQLException
  { _init_struct(true);
    setCwmsTsId(cwmsTsId);
  }

  /* ORAData interface */
  public Datum toDatum(Connection c) throws SQLException
  {
    return _struct.toDatum(c, _SQL_NAME);
  }


  /* ORADataFactory interface */
  public ORAData create(Datum d, int sqlType) throws SQLException
  { return create(null, d, sqlType); }
  protected ORAData create(CwmsTsIdType o, Datum d, int sqlType) throws SQLException
  {
    if (d == null) return null; 
    if (o == null) o = new CwmsTsIdType();
    o._struct = new MutableStruct((STRUCT) d, _sqlType, _factory);
    return o;
  }
  /* accessor methods */
  public oracle.sql.CHAR getCwmsTsId() throws SQLException
  { return (oracle.sql.CHAR) _struct.getOracleAttribute(0); }

  public void setCwmsTsId(oracle.sql.CHAR cwmsTsId) throws SQLException
  { _struct.setOracleAttribute(0, cwmsTsId); }

  public String toString()
  { try {
     return "CWMS_20.CWMS_TS_ID_T" + "(" +
       ((getCwmsTsId()==null)?"null": "'" + getCwmsTsId()+"'" ) +
     ")";
    } catch (Exception e) { return e.toString(); }
  }

}
