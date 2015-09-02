package decodes.cwms.validation.db;

import java.sql.SQLException;
import java.sql.Connection;
import oracle.jdbc.OracleTypes;
import oracle.sql.ORAData;
import oracle.sql.ORADataFactory;
import oracle.sql.Datum;
import oracle.sql.ARRAY;
import oracle.sql.ArrayDescriptor;
import oracle.jpub.runtime.MutableArray;

public class CwmsTsIdArray implements ORAData, ORADataFactory
{
  public static final String _SQL_NAME = "CWMS_20.CWMS_TS_ID_ARRAY";
  public static final int _SQL_TYPECODE = OracleTypes.ARRAY;

  MutableArray _array;

private static final CwmsTsIdArray _CwmsTsIdArrayFactory = new CwmsTsIdArray();

  public static ORADataFactory getORADataFactory()
  { return _CwmsTsIdArrayFactory; }
  /* constructors */
  public CwmsTsIdArray()
  {
    this((CwmsTsIdType[])null);
  }

  public CwmsTsIdArray(CwmsTsIdType[] a)
  {
    _array = new MutableArray(2002, a, CwmsTsIdType.getORADataFactory());
  }

  /* ORAData interface */
  public Datum toDatum(Connection c) throws SQLException
  {
    return _array.toDatum(c, _SQL_NAME);
  }

  /* ORADataFactory interface */
  public ORAData create(Datum d, int sqlType) throws SQLException
  {
    if (d == null) return null; 
    CwmsTsIdArray a = new CwmsTsIdArray();
    a._array = new MutableArray(2002, (ARRAY) d, CwmsTsIdType.getORADataFactory());
    return a;
  }

  public int length() throws SQLException
  {
    return _array.length();
  }

  public int getBaseType() throws SQLException
  {
    return _array.getBaseType();
  }

  public String getBaseTypeName() throws SQLException
  {
    return _array.getBaseTypeName();
  }

  public ArrayDescriptor getDescriptor() throws SQLException
  {
    return _array.getDescriptor();
  }

  /* array accessor methods */
  public CwmsTsIdType[] getArray() throws SQLException
  {
    return (CwmsTsIdType[]) _array.getObjectArray(
      new CwmsTsIdType[_array.length()]);
  }

  public CwmsTsIdType[] getArray(long index, int count) throws SQLException
  {
    return (CwmsTsIdType[]) _array.getObjectArray(index,
      new CwmsTsIdType[_array.sliceLength(index, count)]);
  }

  public void setArray(CwmsTsIdType[] a) throws SQLException
  {
    _array.setObjectArray(a);
  }

  public void setArray(CwmsTsIdType[] a, long index) throws SQLException
  {
    _array.setObjectArray(a, index);
  }

  public CwmsTsIdType getElement(long index) throws SQLException
  {
    return (CwmsTsIdType) _array.getObjectElement(index);
  }

  public void setElement(CwmsTsIdType a, long index) throws SQLException
  {
    _array.setObjectElement(a, index);
  }

  public String toString()
  { try { String r = "CWMS_20.CWMS_TS_ID_ARRAY" + "(";
     CwmsTsIdType[] a = (CwmsTsIdType[])getArray();
     for (int i=0; i<a.length; ) {
       r = r + a[i];
       i++; if (i<a.length) r = r + ","; }
     r = r + ")"; return r;
    } catch (SQLException e) { return e.toString(); }
  }

}
