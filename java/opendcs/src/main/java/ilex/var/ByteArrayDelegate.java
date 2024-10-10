/*
*  $Id$
*
*  $Source$
*
*  $State$
*
*  $Log$
*  Revision 1.1  2012/12/28 20:06:44  shweta
*  created
*
*  Revision 1.1  2008/04/04 18:21:10  cvs
*  Added legacy code to repository
*
*  Revision 1.5  2004/08/30 14:50:33  mjmaloney
*  Javadocs
*
*  Revision 1.4  2001/09/18 00:46:50  mike
*  Working implementation of DateDelegate
*
*  Revision 1.3  2001/09/09 17:38:30  mike
*  Added CharDelegate & support functions.
*
*  Revision 1.2  2000/11/17 14:13:59  mike
*  dev
*
*  Revision 1.1  2000/11/16 21:45:21  mike
*  dev
*
*  Revision 1.1  2000/11/16 02:36:24  mike
*  dev
*
*
*/
package ilex.var;

import java.util.Date;
import ilex.var.DelegateVariable;
import ilex.var.NoConversionException;
import ilex.var.IVariable;
import ilex.var.BadArgumentException;

/**
* Class IntVariable holds a long integer value.
*/
public class ByteArrayDelegate extends DelegateVariable
{
	byte[] value;

	/** Default constructor */
	public ByteArrayDelegate(int length )
	{
		super();
		value = new byte[length];
		
	}

	/**
	* Constructor.
	* @param v the value.
	*/
	public ByteArrayDelegate( byte[] v )
	{
		this(v.length);
		value = v;
	}

	/**
	* @return constant from VariableType class indicating this type of value.
	*/
	public char getNativeType( )
	{
		return VariableType.BYTE_ARRAY;
	}

	/**
	* @return clone of this object.
	*/
	public Object clone( )
	{
		return new ByteArrayDelegate(value);
	}

	//========== Type Specific Get Methods Overridden by Subclasses =======

	/**
	* @return @throws NoConversionException
	*/
	public byte[] getByteArrayValue( ) throws NoConversionException
	{
		
			return value;
		
	}

	@Override
	public char getCharValue() throws NoConversionException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Date getDateValue() throws NoConversionException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DelegateVariable divideBy(IVariable v) throws NoConversionException,
			BadArgumentException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DelegateVariable minus(IVariable v) throws NoConversionException,
			BadArgumentException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DelegateVariable multiplyBy(IVariable v)
			throws NoConversionException, BadArgumentException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DelegateVariable plus(IVariable v) throws NoConversionException,
			BadArgumentException {
		// TODO Auto-generated method stub
		return null;
	}

	//========== Type Specific Set Methods Overridden by Subclasses =======

/* Don't need setValue -- Variable will just create a new delegate!
	public void setValue(byte v)
	{
		value = v;
	}

	public void setValue(double v)
	{
		value = (byte)v;
	}

	public void setValue(float v)
	{
		value = (byte)v;
	}

	public void setValue(int v)
	{
		value = (byte)v;
	}

	public void setValue(long v)
	{
		value = (byte)v;
	}

	public void setValue(String v)
		throws BadArgumentException
	{
		try
		{
			value = Long.parseLong(v);
		}
		catch(NumberFormatException nfe)
		{
			throw new BadArgumentException(
				"Cannot set long integer value from non-numeric string");
		}
	}
*/

	//======== Arithmetic Operation on Variable =========================

	
}
