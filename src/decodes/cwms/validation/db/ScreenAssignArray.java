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

public class ScreenAssignArray implements ORAData, ORADataFactory
{
  public static final String _SQL_NAME = "CWMS_20.SCREEN_ASSIGN_ARRAY";
  public static final int _SQL_TYPECODE = OracleTypes.ARRAY;

  MutableArray _array;

private static final ScreenAssignArray _ScreenAssignArrayFactory = new ScreenAssignArray();

  public static ORADataFactory getORADataFactory()
  { return _ScreenAssignArrayFactory; }
  /* constructors */
  public ScreenAssignArray()
  {
    this((ScreenAssignType[])null);
  }

  public ScreenAssignArray(ScreenAssignType[] a)
  {
    _array = new MutableArray(2002, a, ScreenAssignType.getORADataFactory());
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
    ScreenAssignArray a = new ScreenAssignArray();
    a._array = new MutableArray(2002, (ARRAY) d, ScreenAssignType.getORADataFactory());
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
  public ScreenAssignType[] getArray() throws SQLException
  {
    return (ScreenAssignType[]) _array.getObjectArray(
      new ScreenAssignType[_array.length()]);
  }

  public ScreenAssignType[] getArray(long index, int count) throws SQLException
  {
    return (ScreenAssignType[]) _array.getObjectArray(index,
      new ScreenAssignType[_array.sliceLength(index, count)]);
  }

  public void setArray(ScreenAssignType[] a) throws SQLException
  {
    _array.setObjectArray(a);
  }

  public void setArray(ScreenAssignType[] a, long index) throws SQLException
  {
    _array.setObjectArray(a, index);
  }

  public ScreenAssignType getElement(long index) throws SQLException
  {
    return (ScreenAssignType) _array.getObjectElement(index);
  }

  public void setElement(ScreenAssignType a, long index) throws SQLException
  {
    _array.setObjectElement(a, index);
  }

  public String toString()
  { try { String r = "CWMS_20.SCREEN_ASSIGN_ARRAY" + "(";
     ScreenAssignType[] a = (ScreenAssignType[])getArray();
     for (int i=0; i<a.length; ) {
       r = r + a[i];
       i++; if (i<a.length) r = r + ","; }
     r = r + ")"; return r;
    } catch (SQLException e) { return e.toString(); }
  }

}
