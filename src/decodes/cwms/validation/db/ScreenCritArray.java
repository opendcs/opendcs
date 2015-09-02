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

public class ScreenCritArray implements ORAData, ORADataFactory
{
  public static final String _SQL_NAME = "CWMS_20.SCREEN_CRIT_ARRAY";
  public static final int _SQL_TYPECODE = OracleTypes.ARRAY;

  MutableArray _array;

private static final ScreenCritArray _ScreenCritArrayFactory = new ScreenCritArray();

  public static ORADataFactory getORADataFactory()
  { return _ScreenCritArrayFactory; }
  /* constructors */
  public ScreenCritArray()
  {
    this((ScreenCritType[])null);
  }

  public ScreenCritArray(ScreenCritType[] a)
  {
    _array = new MutableArray(2002, a, ScreenCritType.getORADataFactory());
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
    ScreenCritArray a = new ScreenCritArray();
    a._array = new MutableArray(2002, (ARRAY) d, ScreenCritType.getORADataFactory());
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
  public ScreenCritType[] getArray() throws SQLException
  {
    return (ScreenCritType[]) _array.getObjectArray(
      new ScreenCritType[_array.length()]);
  }

  public ScreenCritType[] getArray(long index, int count) throws SQLException
  {
    return (ScreenCritType[]) _array.getObjectArray(index,
      new ScreenCritType[_array.sliceLength(index, count)]);
  }

  public void setArray(ScreenCritType[] a) throws SQLException
  {
    _array.setObjectArray(a);
  }

  public void setArray(ScreenCritType[] a, long index) throws SQLException
  {
    _array.setObjectArray(a, index);
  }

  public ScreenCritType getElement(long index) throws SQLException
  {
    return (ScreenCritType) _array.getObjectElement(index);
  }

  public void setElement(ScreenCritType a, long index) throws SQLException
  {
    _array.setObjectElement(a, index);
  }

  public String toString()
  { try { String r = "CWMS_20.SCREEN_CRIT_ARRAY" + "(";
     ScreenCritType[] a = (ScreenCritType[])getArray();
     for (int i=0; i<a.length; ) {
       r = r + a[i];
       i++; if (i<a.length) r = r + ","; }
     r = r + ")"; return r;
    } catch (SQLException e) { return e.toString(); }
  }

}
