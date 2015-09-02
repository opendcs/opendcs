package decodes.cwms.validation.db;

import java.sql.SQLException;
import java.sql.Connection;
import oracle.jdbc.OracleTypes;
import oracle.sql.ORAData;
import oracle.sql.ORADataFactory;
import oracle.sql.Datum;
import oracle.sql.REF;
import oracle.sql.STRUCT;

public class ScreenControlTypeRef implements ORAData, ORADataFactory
{
  public static final String _SQL_BASETYPE = "CWMS_20.SCREENING_CONTROL_T";
  public static final int _SQL_TYPECODE = OracleTypes.REF;

  REF _ref;

private static final ScreenControlTypeRef _ScreenControlTypeRefFactory = new ScreenControlTypeRef();

  public static ORADataFactory getORADataFactory()
  { return _ScreenControlTypeRefFactory; }
  /* constructor */
  public ScreenControlTypeRef()
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
    ScreenControlTypeRef r = new ScreenControlTypeRef();
    r._ref = (REF) d;
    return r;
  }

  public static ScreenControlTypeRef cast(ORAData o) throws SQLException
  {
     if (o == null) return null;
     try { return (ScreenControlTypeRef) getORADataFactory().create(o.toDatum(null), OracleTypes.REF); }
     catch (Exception exn)
     { throw new SQLException("Unable to convert "+o.getClass().getName()+" to ScreenControlTypeRef: "+exn.toString()); }
  }

  public ScreenControlType getValue() throws SQLException
  {
     return (ScreenControlType) ScreenControlType.getORADataFactory().create(
       _ref.getSTRUCT(), OracleTypes.REF);
  }

  public void setValue(ScreenControlType c) throws SQLException
  {
    _ref.setValue((STRUCT) c.toDatum(_ref.getJavaSqlConnection()));
  }
  public String toString()
  { try {
      return "REF " + _ref.getBaseTypeName() + "(" + _ref + ")";
    } catch (SQLException e) { return e.toString(); }
  }

}
