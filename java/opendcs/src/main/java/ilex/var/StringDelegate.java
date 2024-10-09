/*
*  $Id$
*
*  $Source$
*
*  $State$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:21:10  cvs
*  Added legacy code to repository
*
*  Revision 1.6  2004/08/30 14:50:36  mjmaloney
*  Javadocs
*
*  Revision 1.5  2001/09/18 00:46:50  mike
*  Working implementation of DateDelegate
*
*  Revision 1.4  2001/09/09 17:38:30  mike
*  Added CharDelegate & support functions.
*
*  Revision 1.3  2000/11/17 14:13:59  mike
*  dev
*
*  Revision 1.2  2000/11/17 02:53:39  mike
*  dev
*
*  Revision 1.1  2000/11/16 21:45:21  mike
*  dev
*
*/
package ilex.var;

import java.util.Date;
import ilex.var.DelegateVariable;
import ilex.var.NoConversionException;
import ilex.var.BadIndexException;
import ilex.var.NotAnArrayException;
import ilex.var.IVariable;
import ilex.var.BadArgumentException;

public class StringDelegate extends DelegateVariable
{
	String value;

	/** Default constructor */
	public StringDelegate( )
	{
		super();
		value = "";
	}

	/**
	* Constructor.
	* @param v the value.
	*/
	public StringDelegate( String v )
	{
		this();
		value = v;
	}

	/**
	* @return clone of this object.
	*/
	public Object clone( )
	{
		return new StringDelegate(value);
	}

	/**
	* @return constant from VariableType class indicating this type of value.
	*/
	public char getNativeType( )
	{
		return VariableType.STRING;
	}

	/**
	* @return value as a byte array.
	*/
	public byte[] getByteArrayValue( )
	{
		return value.getBytes();
	}

	/**
	* @return byte value of first character in the String.
	*/
	public byte getByteValue( ) throws NoConversionException
	{
		if (value.length() == 0)
			throw new NoConversionException(
				"Cannot convert zero-length string to byte.");
		return (byte)value.charAt(0);
	}

	/**
	* @throws NoConversionException always
	*/
	public double[] getDoubleArrayValue( ) throws NoConversionException
	{
		throw new NoConversionException("Cannot convert string to array.");
	}

	/**
	* Attempts to parse string & return double value
	* @throws NoConversionException if non-numeric string.
	*/
	public double getDoubleValue( ) throws NoConversionException
	{
		try
		{
			double d = Double.parseDouble(value);
			return d;
		}
		catch(NumberFormatException nfe) {}

		throw new NoConversionException(
			"Cannot convert non-numeric string to double.");
	}

	/**
	* @return 
	* @throws NoConversionException
	*/
	public float[] getFloatArrayValue( ) throws NoConversionException
	{
		throw new NoConversionException(
			"Cannot convert string to array.");
	}

	/**
	* @return value as a double float
	*/
	public float getFloatValue( ) throws NoConversionException
	{
		try
		{
			float v = Float.parseFloat(value);
			return v;
		}
		catch(NumberFormatException nfe){}

		throw new NoConversionException(
			"Cannot convert non-numeric string to float.");
	}

	/**
	* @return @throws NoConversionException
	*/
	public int[] getIntArrayValue( ) throws NoConversionException
	{
		throw new NoConversionException(
			"Cannot convert string to array.");
	}

	/**
	* @return value as an int
	*/
	public int getIntValue( ) throws NoConversionException
	{
		try
		{
			int v = Integer.parseInt(value);
			return v;
		}
		catch(NumberFormatException nfe){}

		throw new NoConversionException(
			"Cannot convert non-numeric string to integer.");
	}

	/**
	* @return @throws NoConversionException
	*/
	public long[] getLongArrayValue( ) throws NoConversionException
	{
		throw new NoConversionException("Cannot convert string to array.");
	}

	/**
	* @return value as a long
	*/
	public long getLongValue( ) throws NoConversionException
	{
		try
		{
			long v = Long.parseLong(value);
			return v;
		}
		catch(NumberFormatException nfe){}

		throw new NoConversionException(
			"Cannot convert non-numeric string to long integer.");
	}

	/**
	* @return first char in the String
	*/
	public char getCharValue( )
	{
		if (value.length() == 0)
			return '\0';
		return value.charAt(0);
	}

	/**
	* @return value as a string
	*/
	public String getStringValue( )
	{
		return value;
	}

	/**
	* @return value as a date
	* @throws NoConversionException
	*/
	public Date getDateValue( ) throws NoConversionException
	{
		return DateDelegate.parseDate(value);
	}

	//========== Type specific GetXXXElement methods for array types ====

	/**
	* @param idx
	* @return @throws NoConversionException
	* @throws BadIndexException
	* @throws NotAnArrayException
	*/
	public byte getByteElement( int idx ) throws NoConversionException, BadIndexException, NotAnArrayException
	{
		return (byte)getIntElement(idx);
	}

	/**
	* @param idx
	* @return @throws NoConversionException
	* @throws BadIndexException
	* @throws NotAnArrayException
	*/
	public int getIntElement( int idx ) throws NoConversionException, BadIndexException, NotAnArrayException
	{
		if (idx < 0 || idx >= value.length())
			throw new BadIndexException("String index out of bounds");
		return (int)value.charAt(idx);
	}

	/**
	* @param idx
	* @return @throws NoConversionException
	* @throws BadIndexException
	* @throws NotAnArrayException
	*/
	public long getLongElement( int idx ) throws NoConversionException, BadIndexException, NotAnArrayException
	{
		return (long)getIntElement(idx);
	}

	//========== Type Specific SetElement Methods for array types =======

/*
	public void setElement(int idx, byte v)
		throws NotAnArrayException, BadArgumentException
	{
		// Array types will override, simple types need not.
		if (!isArray())
			throw new NotAnArrayException(
				"Cannot set element of non-array");
		else
			throw new NotAnArrayException(
			"Code Error: array type did not override setElement(idx,byte)");
	}

	public void setElement(int idx, int v)
		throws NotAnArrayException, BadArgumentException
	{
		// Array types will override, simple types need not.
		if (!isArray())
		{
			if (idx != 0)
				throw new NotAnArrayException(
					"Cannot set element of non-array where index != 0");
			setValue(v);
			return;
		}
		throw new NotAnArrayException(
			"Code Error: array type did not override setElement(idx,int)");
	}

	public void setElement(int idx, long v)
		throws NotAnArrayException, BadArgumentException
	{
		// Array types will override, simple types need not.
		if (!isArray())
		{
			if (idx != 0)
				throw new NotAnArrayException(
					"Cannot set element of non-array where index != 0");
			setValue(v);
			return;
		}
		throw new NotAnArrayException(
			"Code Error: array type did not override setElement(idx,long)");
	}
*/

	/**
	* @return
	*/
	public int getNumElements( )
	{
		return value.length();
	}

	//======== Arithmetic Operation on Variable =========================

	/**
	* Divide this variable by another
	* @param v the other variable
	* @return a delegate holding the result
	* @throws NoConversionException always
	*/
	public DelegateVariable divideBy( IVariable v ) throws NoConversionException, BadArgumentException
	{
		throw new NoConversionException("Cannot divide strings");
	}

	/**
	* Subtract a variable from this variable.
	* @param v the other variable
	* @return Delegate holding result
	* @throws NoConversionException always
	*/
	public DelegateVariable minus( IVariable v ) throws NoConversionException, BadArgumentException
	{
		throw new NoConversionException("Cannot subtract strings.");
	}

	/**
	* Multiply a variable by this variable.
	* @param v the other variable
	* @return Delegate holding result
	* @throws NoConversionException always
	*/
	public DelegateVariable multiplyBy( IVariable v ) throws NoConversionException, BadArgumentException
	{
		throw new NoConversionException("Cannot multiply strings");
	}

	/**
	* Add a variable to this variable.
	* @param v the other variable
	* @return Delegate holding result
	* @throws NoConversionException
	* @throws BadArgumentException
	*/
	public DelegateVariable plus( IVariable v ) throws NoConversionException, BadArgumentException
	{
		return new StringDelegate(value + v);
	}
}
