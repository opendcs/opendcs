/*
*  $Id$
*
*  $State$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:21:10  cvs
*  Added legacy code to repository
*
*  Revision 1.3  2004/08/30 14:50:33  mjmaloney
*  Javadocs
*
*  Revision 1.2  2001/09/18 00:46:50  mike
*  Working implementation of DateDelegate
*
*  Revision 1.1  2001/09/09 17:38:30  mike
*  Added CharDelegate & support functions.
*
*/
package ilex.var;

import java.util.Date;
import ilex.var.DelegateVariable;
import ilex.var.NoConversionException;
import ilex.var.IVariable;
import ilex.var.BadArgumentException;

/**
* Class CharDelegate holds a character value.
*/
public class CharDelegate extends DelegateVariable
{
	char value;

	/**
	* Default Constructor
	*/
	public CharDelegate( )
	{
		super();
		value = ' ';
	}

	/**
	* Constructor.
	* @param v the value.
	*/
	public CharDelegate( char v )
	{
		this();
		value = v;
	}

	/**
	* @return clone of this object.
	*/
	public Object clone( )
	{
		return new CharDelegate(value);
	}

	/**
	* @return constant from VariableType class indicating this type of value.
	*/
	public char getNativeType( )
	{
		return VariableType.CHAR;
	}

	//========== Type Specific Get Methods Overridden by Subclasses =======

	/**
	* @return value as a byte.
	*/
	public byte getByteValue( )
	{
		return (byte)value;
	}

	/**
	* @return value as a double float
	*/
	public double getDoubleValue( )
	{
		return (double)value;
	}

	/**
	* @return value as a double float
	*/
	public float getFloatValue( )
	{
		return (float)value;
	}

	/**
	* @return value as an int
	*/
	public int getIntValue( )
	{
		return (int)value;
	}

	/**
	* @return value as a long
	*/
	public long getLongValue( )
	{
		return (long)value;
	}

	/**
	* @return value as a char
	*/
	public char getCharValue( )
	{
		return value;
	}

	/**
	* @return value as a string
	*/
	public String getStringValue( )
	{
		return "" + value;
	}

	/**
	* @return value as a date
	* @throws NoConversionException
	*/
	public Date getDateValue( ) throws NoConversionException
	{
		throw new NoConversionException("Cannot convert char to Date.");
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
		throw new BadArgumentException(
			"Cannot divide character variable");
	}

	/**
	* Subtract a variable from this variable.
	* @param v the other variable
	* @return Delegate holding result
	* @throws NoConversionException
	* @throws BadArgumentException
	*/
	public DelegateVariable minus( IVariable v ) throws NoConversionException, BadArgumentException
	{
		throw new BadArgumentException(
			"Cannot subtract character variable");
	}

	/**
	* Multiply a variable by this variable.
	* @param v the other variable
	* @return Delegate holding result
	* @throws NoConversionException
	* @throws BadArgumentException
	*/
	public DelegateVariable multiplyBy( IVariable v ) throws NoConversionException, BadArgumentException
	{
		throw new BadArgumentException(
			"Cannot multiply character variable");
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
		return new StringDelegate("" + value + v.getStringValue());
	}
}
