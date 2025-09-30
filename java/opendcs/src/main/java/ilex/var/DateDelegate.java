/*
* Where Applicable, Copyright 2025 OpenDCS Consortium and/or its contributors
* 
* Licensed under the Apache License, Version 2.0 (the "License"); you may not
* use this file except in compliance with the License. You may obtain a copy
* of the License at
* 
*   http://www.apache.org/licenses/LICENSE-2.0
* 
* Unless required by applicable law or agreed to in writing, software 
* distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
* License for the specific language governing permissions and limitations 
* under the License.
*/
package ilex.var;

import java.util.Date;
import java.util.TimeZone;
import java.util.Calendar;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.text.ParseException;

/**
* Class DateDelegate holds a Date/Time stamp.
*/
public class DateDelegate extends DelegateVariable
{
	Date value;
	private static DateFormat dateFormat;

	static
	{
		dateFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss z");
		TimeZone jtz=java.util.TimeZone.getTimeZone("UTC");
		dateFormat.setCalendar(Calendar.getInstance(jtz));
	}

	/**
	* Sets the static DateFormat used when a String value is requested.
	* @param df
	*/
	public static void setDateFormat( DateFormat df ) { dateFormat = df; }

	/**
	* Parses a date using the set DateFormat.
	* @param s String
	* @return Date
	* @throws NoConversionException if bad date format
	*/
	public static Date parseDate( String s ) throws NoConversionException
	{
		try { return dateFormat.parse(s); }
		catch(ParseException ex)
		{
			throw new NoConversionException("Invalid date format '" + s + "'", ex);
		}
	}

	/** Default constructor */
	public DateDelegate( )
	{
		super();
		value = new Date();
	}

	/**
	* Constructor.
	* @param v the value.
	*/
	public DateDelegate( Date v )
	{
		this();
		value = v;
	}

	/**
	* @return clone of this object.
	*/
	public Object clone( )
	{
		return new DateDelegate(value);
	}

	/**
	* @return constant from VariableType class indicating this type of value.
	*/
	public char getNativeType( )
	{
		return VariableType.DATE;
	}

	//========== Type Specific Get Methods Overridden by Subclasses =======

	/**
	* @throws NoConversionException always
	*/
	public byte getByteValue( ) throws NoConversionException
	{
		throw new NoConversionException("Cannot convert Date to byte.");
	}

	/**
	* @return msec value of Date as a double.
	*/
	public double getDoubleValue( )
	{
		return (double)value.getTime();
	}

	/**
	* @return msec value of Date as a float.
	*/
	public float getFloatValue( )
	{
		return (float)value.getTime();
	}

	/**
	* @return msec value of Date as an int (probably out of range!).
	*/
	public int getIntValue( )
	{
		return (int)value.getTime();
	}

	/**
	* @return msec value of Date as an long.
	*/
	public long getLongValue( )
	{
		return value.getTime();
	}

	/**
	* @throws NoConversionException always
	*/
	public char getCharValue( ) throws NoConversionException
	{
		throw new NoConversionException("Cannot convert Date to char.");
	}

	/**
	* @return Date formatted into a String.
	*/
	public String getStringValue( )
	{
		return dateFormat.format(value);
	}

	/**
	* @return value as a date
	* @throws NoConversionException
	*/
	public Date getDateValue( )
	{
		return value;
	}

	//======== Arithmetic Operation on Variable =========================

	/**
	* Divide this variable by another
	* @param v the other variable
	* @return a delegate holding the result
	* @throws BadArgumentException always
	*/
	public DelegateVariable divideBy( IVariable v ) throws NoConversionException, BadArgumentException
	{
		throw new BadArgumentException("Cannot divide Date values.");
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
		 || vt == VariableType.LONG
		 || vt == VariableType.DOUBLE
		 || vt == VariableType.FLOAT)
		{
			// Subtract number (milliseconds) from date, result is a date.
			long delta = v.getLongValue();
			return new DateDelegate(new Date(value.getTime() - delta));
		}
		else if (vt == VariableType.DATE)
		{
			// Subtracting two dates yields a long # of seconds.
			long rv = value.getTime() - v.getLongValue();
			return new LongDelegate(rv);
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
		throw new BadArgumentException(
			"Cannot multiply Date variable");
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
		 || vt == VariableType.LONG
		 || vt == VariableType.DOUBLE
		 || vt == VariableType.FLOAT)
		{
			// Integer addition
			long rv = value.getTime() + v.getLongValue();
			return new DateDelegate(new Date(rv));
		}
		else
			throw new NoConversionException("cannot add dates.");
	}
}
