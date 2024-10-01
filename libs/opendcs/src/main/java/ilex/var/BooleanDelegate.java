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
*  Revision 1.4  2004/08/30 14:50:33  mjmaloney
*  Javadocs
*
*  Revision 1.3  2001/09/18 00:46:50  mike
*  Working implementation of DateDelegate
*
*  Revision 1.2  2001/09/09 17:38:30  mike
*  Added CharDelegate & support functions.
*
*  Revision 1.1  2001/01/03 02:56:14  mike
*  created
*
*/
package ilex.var;

import java.util.Date;
import ilex.var.DelegateVariable;
import ilex.var.NoConversionException;
import ilex.var.IVariable;
import ilex.var.BadArgumentException;

/**
Delegate for a boolean-type Variable.
*/
public class BooleanDelegate extends DelegateVariable
{
	/** The value */
	boolean value;

	/** Default constructor */
	public BooleanDelegate( )
	{
		super();
		value = false;
	}

	/**
	* Constructor.
	* @param v the value.
	*/
	public BooleanDelegate( boolean v )
	{
		this();
		value = v;
	}

	/**
	* @return constant from VariableType class indicating this type of value.
	*/
	public char getNativeType( )
	{
		return VariableType.BOOLEAN;
	}

	/**
	* @return clone of this object.
	*/
	public Object clone( )
	{
		return new BooleanDelegate(value);
	}

	//========== Type Specific Get Methods Overridden by Subclasses =======

	/**
	* @return value as a boolean.
	*/
	public boolean getBooleanValue( )
	{
		return value;
	}

	/**
	* @return value as a byte.
	*/
	public byte getByteValue( )
	{
		return value ? (byte)1 : (byte)0;
	}

	/**
	* @return value as a double float
	*/
	public double getDoubleValue( )
	{
		return value ? (double)1 : (double)0;
	}

	/**
	* @return value as a double float
	*/
	public float getFloatValue( )
	{
		return value ? (float)1 : (float)0;
	}

	/**
	* @return value as an int
	*/
	public int getIntValue( )
	{
		return value ? (int)1 : (int)0;
	}

	/**
	* @return value as a long
	*/
	public long getLongValue( )
	{
		return value ? (long)1 : (long)0;
	}

	/**
	* @return value as a char
	*/
	public char getCharValue( )
	{
		return value ? 'Y' : 'N';
	}

	/**
	* @return value as a string
	*/
	public String getStringValue( )
	{
		return value ? "true" : "false";
	}

	/**
	* @return value as a date
	* @throws NoConversionException
	*/
	public Date getDateValue( ) throws NoConversionException
	{
		throw new NoConversionException("Cannot convert boolean to Date.");
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

	/**
	* Divide this variable by another
	* @param v the other variable
	* @return a delegate holding the result
	* @throws NoConversionException always
	*/
	public DelegateVariable divideBy( IVariable v ) throws NoConversionException, BadArgumentException
	{
		throw new NoConversionException("Cannot divide boolean variable");
	}

	/**
	* @param v
	* @return @throws NoConversionException
	* @throws BadArgumentException
	*/
	public DelegateVariable minus( IVariable v ) throws NoConversionException, BadArgumentException
	{
		throw new NoConversionException("Cannot subtract boolean variable");
	}

	/**
	* @param v
	* @return @throws NoConversionException
	* @throws BadArgumentException
	*/
	public DelegateVariable multiplyBy( IVariable v ) throws NoConversionException, BadArgumentException
	{
		throw new NoConversionException("Cannot multiply boolean variable");
	}
	
	/**
	* @param v
	* @return @throws NoConversionException
	* @throws BadArgumentException
	*/
	public DelegateVariable plus( IVariable v ) throws NoConversionException, BadArgumentException
	{
		throw new NoConversionException("Cannot add boolean variable");
	}
}
