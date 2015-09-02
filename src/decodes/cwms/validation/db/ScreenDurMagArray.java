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

public class ScreenDurMagArray implements ORAData, ORADataFactory
{
  public static final String _SQL_NAME = "CWMS_20.SCREEN_DUR_MAG_ARRAY";
  public static final int _SQL_TYPECODE = OracleTypes.ARRAY;

  MutableArray _array;

private static final ScreenDurMagArray _ScreenDurMagArrayFactory = new ScreenDurMagArray();

  public static ORADataFactory getORADataFactory()
  { return _ScreenDurMagArrayFactory; }
  /* constructors */
  public ScreenDurMagArray()
  {
    this((ScreenDurMagType[])null);
  }

  public ScreenDurMagArray(ScreenDurMagType[] a)
  {
    _array = new MutableArray(2002, a, ScreenDurMagType.getORADataFactory());
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
    ScreenDurMagArray a = new ScreenDurMagArray();
    a._array = new MutableArray(2002, (ARRAY) d, ScreenDurMagType.getORADataFactory());
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
  public ScreenDurMagType[] getArray() throws SQLException
  {
    return (ScreenDurMagType[]) _array.getObjectArray(
      new ScreenDurMagType[_array.length()]);
  }

  public ScreenDurMagType[] getArray(long index, int count) throws SQLException
  {
    return (ScreenDurMagType[]) _array.getObjectArray(index,
      new ScreenDurMagType[_array.sliceLength(index, count)]);
  }

  public void setArray(ScreenDurMagType[] a) throws SQLException
  {
    _array.setObjectArray(a);
  }

  public void setArray(ScreenDurMagType[] a, long index) throws SQLException
  {
    _array.setObjectArray(a, index);
  }

  public ScreenDurMagType getElement(long index) throws SQLException
  {
    return (ScreenDurMagType) _array.getObjectElement(index);
  }

  public void setElement(ScreenDurMagType a, long index) throws SQLException
  {
    _array.setObjectElement(a, index);
  }

  public String toString()
  { try { String r = "CWMS_20.SCREEN_DUR_MAG_ARRAY" + "(";
     ScreenDurMagType[] a = (ScreenDurMagType[])getArray();
     for (int i=0; i<a.length; ) {
       r = r + a[i];
       i++; if (i<a.length) r = r + ","; }
     r = r + ")"; return r;
    } catch (SQLException e) { return e.toString(); }
  }

}
