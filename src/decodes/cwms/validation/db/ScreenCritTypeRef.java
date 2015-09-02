package decodes.cwms.validation.db;

import java.sql.SQLException;
import java.sql.Connection;
import oracle.jdbc.OracleTypes;
import oracle.sql.ORAData;
import oracle.sql.ORADataFactory;
import oracle.sql.Datum;
import oracle.sql.REF;
import oracle.sql.STRUCT;

public class ScreenCritTypeRef implements ORAData, ORADataFactory
{
  public static final String _SQL_BASETYPE = "CWMS_20.SCREEN_CRIT_TYPE";
  public static final int _SQL_TYPECODE = OracleTypes.REF;

  REF _ref;

private static final ScreenCritTypeRef _ScreenCritTypeRefFactory = new ScreenCritTypeRef();

  public static ORADataFactory getORADataFactory()
  { return _ScreenCritTypeRefFactory; }
  /* constructor */
  public ScreenCritTypeRef()
  {
  }

  /* ORAData interface */
  public Datum toDatum(Connection c) throws SQLException
  {
    return _ref;
  }

  /* ORADataFactory interface */
  public ORAData create(Datum d, int sqlType) throws SQLException
  {
    if (d == null) return null; 
    ScreenCritTypeRef r = new ScreenCritTypeRef();
    r._ref = (REF) d;
    return r;
  }

  public static ScreenCritTypeRef cast(ORAData o) throws SQLException
  {
     if (o == null) return null;
     try { return (ScreenCritTypeRef) getORADataFactory().create(o.toDatum(null), OracleTypes.REF); }
     catch (Exception exn)
     { throw new SQLException("Unable to convert "+o.getClass().getName()+" to ScreenCritTypeRef: "+exn.toString()); }
  }

  public ScreenCritType getValue() throws SQLException
  {
     return (ScreenCritType) ScreenCritType.getORADataFactory().create(
       _ref.getSTRUCT(), OracleTypes.REF);
  }

  public void setValue(ScreenCritType c) throws SQLException
  {
    _ref.setValue((STRUCT) c.toDatum(_ref.getJavaSqlConnection()));
  }
  public String toString()
  { try {
      return "REF " + _ref.getBaseTypeName() + "(" + _ref + ")";
    } catch (SQLException e) { return e.toString(); }
  }

}
