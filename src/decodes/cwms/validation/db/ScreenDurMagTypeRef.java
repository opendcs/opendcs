package decodes.cwms.validation.db;

import java.sql.SQLException;
import java.sql.Connection;
import oracle.jdbc.OracleTypes;
import oracle.sql.ORAData;
import oracle.sql.ORADataFactory;
import oracle.sql.Datum;
import oracle.sql.REF;
import oracle.sql.STRUCT;

public class ScreenDurMagTypeRef implements ORAData, ORADataFactory
{
  public static final String _SQL_BASETYPE = "CWMS_20.SCREEN_DUR_MAG_TYPE";
  public static final int _SQL_TYPECODE = OracleTypes.REF;

  REF _ref;

private static final ScreenDurMagTypeRef _ScreenDurMagTypeRefFactory = new ScreenDurMagTypeRef();

  public static ORADataFactory getORADataFactory()
  { return _ScreenDurMagTypeRefFactory; }
  /* constructor */
  public ScreenDurMagTypeRef()
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
    ScreenDurMagTypeRef r = new ScreenDurMagTypeRef();
    r._ref = (REF) d;
    return r;
  }

  public static ScreenDurMagTypeRef cast(ORAData o) throws SQLException
  {
     if (o == null) return null;
     try { return (ScreenDurMagTypeRef) getORADataFactory().create(o.toDatum(null), OracleTypes.REF); }
     catch (Exception exn)
     { throw new SQLException("Unable to convert "+o.getClass().getName()+" to ScreenDurMagTypeRef: "+exn.toString()); }
  }

  public ScreenDurMagType getValue() throws SQLException
  {
     return (ScreenDurMagType) ScreenDurMagType.getORADataFactory().create(
       _ref.getSTRUCT(), OracleTypes.REF);
  }

  public void setValue(ScreenDurMagType c) throws SQLException
  {
    _ref.setValue((STRUCT) c.toDatum(_ref.getJavaSqlConnection()));
  }
  public String toString()
  { try {
      return "REF " + _ref.getBaseTypeName() + "(" + _ref + ")";
    } catch (SQLException e) { return e.toString(); }
  }

}
