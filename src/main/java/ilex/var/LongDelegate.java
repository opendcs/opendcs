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
*  Revision 1.5  2004/08/30 14:50:34  mjmaloney
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
*/
package ilex.var;

import java.util.Date;
import ilex.var.DelegateVariable;
import ilex.var.IVariable;
import ilex.var.NoConversionException;
import ilex.var.BadArgumentException;

/**
* Class LongDelegate holds a long integer value.
*/
public class LongDelegate extends DelegateVariable
{
	long value;

	/** Default constructor */
	public LongDelegate( )
	{
		super();
		value = 0L;
	}

	/**
	* Constructor.
	* @param v the value.
	*/
	public LongDelegate( long v )
	{
		this();
		value = v;
	}

	/**
	* @return clone of this object.
	*/
	public Object clone( )
	{
		return new LongDelegate(value);
	}

	/**
	* @return constant from VariableType class indicating this type of value.
	*/
	public char getNativeType( )
	{
		return VariableType.LONG;
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
		return value;
	}

	/**
	* @return value as a char
	*/
	public char getCharValue( )
	{
		return (char)value;
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
	public Date getDateValue( )
	{
		return new Date(value);
	}


	//======== Arithmetic Operation on Variable =========================

	/**
	* Divide this variable by another
	* @param v the other variable
	* @return a delegate holding the result
	*/
	public DelegateVariable divideBy( IVariable v ) throws NoConversionException, BadArgumentException
	{
		int vt = v.getNativeType();
		if (vt == VariableType.BYTE
		 || vt == VariableType.INT
		 || vt == VariableType.LONG)
		{
			// Integer division
			long div = v.getLongValue();
			if (div == 0L)
				throw new BadArgumentException("Divide by zero");
			long rv = (long)value / div;
			return new LongDelegate(rv);
		}
		else if (vt == VariableType.DOUBLE
		 || vt == VariableType.FLOAT)
		{
			// float division
			double rv = (double)value / v.getDoubleValue();
			return new DoubleDelegate(rv);
		}
		throw new BadArgumentException(
			"Cannot divide non-numeric variable");
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
		int vt = v.getNativeType();
		if (vt == VariableType.BYTE
		 || vt == VariableType.INT
		 || vt == VariableType.LONG)
		{
			// Integer subtraction
			long rv = (long)value - v.getLongValue();
			return new LongDelegate(rv);
		}
		else if (vt == VariableType.DOUBLE
		 || vt == VariableType.FLOAT)
		{
			// float subtraction
			double rv = (double)value - v.getDoubleValue();
			return new DoubleDelegate(rv);
		}
		throw new BadArgumentException(
			"Cannot subtract non-numeric variable");
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
		int vt = v.getNativeType();
		if (vt == VariableType.BYTE
		 || vt == VariableType.INT
		 || vt == VariableType.LONG)
		{
			// Integer multiplication
			long rv = (long)value * v.getLongValue();
			return new LongDelegate(rv);
		}
		else if (vt == VariableType.DOUBLE
		 || vt == VariableType.FLOAT)
		{
			// float multiplication
			double rv = (double)value * v.getDoubleValue();
			return new DoubleDelegate(rv);
		}
		throw new BadArgumentException(
			"Cannot multiply non-numeric variable");
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
		int vt = v.getNativeType();
		if (vt == VariableType.BYTE
		 || vt == VariableType.INT
		 || vt == VariableType.LONG)
		{
			// Integer addition
			long rv = (long)value + v.getLongValue();
			return new LongDelegate(rv);
		}
		else if (vt == VariableType.DOUBLE
		 || vt == VariableType.FLOAT)
		{
			// float addition
			double rv = (double)value + v.getDoubleValue();
			return new DoubleDelegate(rv);
		}
		else if (vt == VariableType.STRING)
		{
			// String addition
			String rv = getStringValue() + v.getStringValue();
			return new StringDelegate(rv);
		}
		else
			throw new NoConversionException("Unknown variable type");
	}
}
