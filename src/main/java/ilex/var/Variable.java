/*
*  $Id$
*
*  This is open-source software written by ILEX Engineering, Inc., under
*  contract to the federal government. You are free to copy and use this
*  source code for your own purposes, except that no part of the information
*  contained in this file may be claimed to be proprietary.
*
*  Except for specific contractual terms between ILEX and the federal 
*  government, this source code is provided completely without warranty.
*  For more information contact: info@ilexeng.com
*/
package ilex.var;

import java.util.Date;

import decodes.sql.DbKey;

/**
* Class Variable provides a common interface for all of the concrete
* variable classes of specific type. It also contains the functions for
* accessing the 32-bit integer flag.
*/
public class Variable implements IVariable, IFlags
{
	/**
	* The flag holds several bit fields used to track the status of this
	* variable. See IFlags.java for bit constants and masks. Several
	* bits are reserved by the ilex.var package. Several are available for
	* application specific meaning.
	*/
	private int flags;

	/**
	* All type-specific functionality is delegated to a sub-class.
	*/
	private DelegateVariable delegate;

	/**
	* The following value is used as the initial flags value for all
	* newly created variables.
	*/
	public static int defaultFlags = 0;

	/** App may use this to track the source of each variable. */
	private DbKey sourceId = DbKey.NullKey;

	/**
	* Constructs a default (integer) Variable with a zero flag.
	*/
	public Variable( )
	{
		flags = defaultFlags;
		delegate = new LongDelegate(0L);
	}

	/**
	* Copy constructor
	* @param v the value
	*/
	public Variable( Variable v )
	{
		flags = v.flags;
		sourceId = v.getSourceId();
		try { delegate = DelegateVariable.newDelegateVariable(v.delegate); }
		catch(NoConversionException e)
		{
			// Shouldn't happen - assuming we were passed a good variable
			delegate = new BooleanDelegate(false);
		}
	}

	/**
	* Used by arithmetic operators to return new variables.
	* @param dv the delegate
	*/
	private Variable( DelegateVariable dv )
	{
		flags = defaultFlags;
		delegate = dv;
	}

	/**
	* Constructs a new byte variable
	* @param v the value
	*/
	public Variable( byte v )
	{
		flags = defaultFlags;
		delegate = new ByteDelegate(v);
	}

	
	/**
	* Constructs a new byte array variable
	* @param v the value
	*/
	public Variable( byte[] v )
	{
		flags = defaultFlags;
		delegate = new ByteArrayDelegate(v);
	}
	
	/**
	* Constructs a new boolean variable
	* @param v the value
	*/
	public Variable( boolean v )
	{
		flags = defaultFlags;
		delegate = new BooleanDelegate(v);
	}

	/** Constructs a new byte array variable */
/*
	public Variable(byte[] v)
	{
		flags = defaultFlags;
		delegate = new ByteArrayDelegate(v);
	}
*/

	/**
	* Constructs a new integer variable
	* @param v the value
	*/
	public Variable( int v )
	{
		this((long)v);
	}

	/** Constructs a new integer array variable */
/*
	public Variable(int v[])
	{
		this();
		setValue(v);
	}
*/

	/**
	* Constructs a new long integer variable
	* @param v the value
	*/
	public Variable( long v )
	{
		flags = defaultFlags;
		delegate = new LongDelegate(v);
	}

	/** Constructs a new long integer array variable */
/*
	public Variable(long[] v)
	{
		flags = defaultFlags;
		delegate = new LongArrayDelegate(v);
	}
*/

	/**
	* Constructs a new floating point variable
	* @param v the value
	*/
	public Variable( float v )
	{
		this((double)v);
	}

	/** Constructs a new float array variable */
/*
	public Variable(float[] v)
	{
		this();
		setValue(v);
	}
*/

	/**
	* Constructs a new double variable
	* @param v the value
	*/
	public Variable( double v )
	{
		flags = defaultFlags;
		delegate = new DoubleDelegate(v);
	}

	/** Constructs a new double array variable */
/*
	public Variable(double[] v)
	{
		flags = defaultFlags;
		delegate = new DoubleArrayDelegate(v);
	}
*/

	/**
	* Constructs a new string variable
	* @param v the value
	*/
	public Variable( String v )
	{
		flags = defaultFlags;
		delegate = new StringDelegate(v);
	}

	/**
	* Constructs a char variable
	* @param v the value
	*/
	public Variable( char v )
	{
		flags = defaultFlags;
		delegate = new CharDelegate(v);
	}

	/**
	* Constructs a date variable
	* @param v the value
	*/
	public Variable( Date v )
	{
		flags = defaultFlags;
		delegate = new DateDelegate(v);
	}

	//======== Methods for getting/setting/checking the flags ===========

	/**
	* @return the 32-bit integer flag value.
	*/
	public int getFlags( ) { return flags; }

	/**
	* Sets the 32-bit integer flag value.
	* @param f new flags value
	*/
	public void setFlags( int f ) { flags = f; }

	/**
	* @return true if the value of this variable has been changed.
	*/
	public boolean isChanged( ) { return (flags & IFlags.IS_CHANGED)!=0; }

	/**
	* Resets the IS_CHANGED bit so we can detect future modifications.
	*/
	public void resetChanged( ) { flags &= (~IFlags.IS_CHANGED); }

	/**
	* It is rarely necessary to call this method directly because
	* modifications via setValue() are tracked automatically.
	*/
	public void setChanged( ) { flags |= IFlags.IS_CHANGED; }

	//========= Type-Specific methods are delegated ======================

	/**
	* @return true if the type of this variable is an array.
	*/
	public boolean isArray( )
	{
		return delegate.isArray();
	}

	/**
	* @return one of the constants in class VariableType, representing
	* the native type for this variable.
	* The Native type is the type in which the value is represent internally.
	*/
	public char getNativeType( )
	{
		return delegate.getNativeType();
	}

	/**
	* Returns value of this Variable as a byte array.
	* <p>
	* Calling this method on a non-array Variable will return an array of
	* length 1.
	* <p>
	* @return value of this Variable as a byte array.
	* @throws NoConversionException if the native value cannot be
	* represented this way.
	*/
	public byte[] getByteArrayValue( ) throws NoConversionException
	{
		return delegate.getByteArrayValue();
	}

	/**
	* Returns value of this Variable as a byte.
	* <p>
	* Calling this method on an array Variable will return the first
	* array element.
	* <p>
	* @return value of this Variable as a byte.
	* @throws NoConversionException if the native value cannot be
	* represented this way.
	*/
	public byte getByteValue( ) throws NoConversionException
	{
		return delegate.getByteValue();
	}

	/**
	* Returns value of this Variable as a boolean array.
	* <p>
	* Calling this method on a non-array Variable will return an array of
	* length 1.
	* <p>
	* @return value of this Variable as a boolean array.
	* @throws NoConversionException if the native value cannot be
	* represented this way.
	*/
	public boolean[] getBooleanArrayValue( ) throws NoConversionException
	{
		return delegate.getBooleanArrayValue();
	}

	/**
	* return value of this Variable as a boolean.
	* <p>
	* Calling this method on an array Variable will return the first
	* array element.
	* <p>
	* @return value of this Variable as a boolean.
	* @throws NoConversionException if the native value cannot be
	* represented this way.
	*/
	public boolean getBooleanValue( ) throws NoConversionException
	{
		return delegate.getBooleanValue();
	}

	/**
	* return value of this Variable as an array of doubles.
	* <p>
	* Calling this method on a non-array Variable will return an array of
	* length 1.
	* <p>
	* @return value of this Variable as an array of doubles.
	* @throws NoConversionException if the native value cannot be
	* represented this way.
	*/
	public double[] getDoubleArrayValue( ) throws NoConversionException
	{
		return delegate.getDoubleArrayValue();
	}

	/**
	* return value of this Variable as a double.
	* <p>
	* Calling this method on an array Variable will return the first
	* array element.
	* <p>
	* @return value of this Variable as a double.
	* @throws NoConversionException if the native value cannot be
	* represented this way.
	*/
	public double getDoubleValue( ) throws NoConversionException
	{
		return delegate.getDoubleValue();
	}

	/**
	* return value of this Variable as an array of floats.
	* Single valued variables will return an array of length 1.
	* @return value of this Variable as an array of floats.
	* @throws NoConversionException if the native value cannot be
	* represented this way.
	*/
	public float[] getFloatArrayValue( ) throws NoConversionException
	{
		return delegate.getFloatArrayValue();
	}

	/**
	* @return value of this Variable as a float.
	* For array variables, this will return the first value in the array.
	* @throws NoConversionException if the native value cannot be
	* represented this way.
	*/
	public float getFloatValue( ) throws NoConversionException
	{
		return delegate.getFloatValue();
	}

	/**
	* @return value of this Variable as an array of ints.
	* Single valued variables will return an array of length 1.
	* @throws NoConversionException if the native value cannot be
	* represented this way.
	*/
	public int[] getIntArrayValue( ) throws NoConversionException
	{
		return delegate.getIntArrayValue();
	}

	/**
	* @return value of this Variable as an int.
	* For array variables, this will return the first value in the array.
	* Floating point types will be truncated.
	* @throws NoConversionException if the native value cannot be
	* represented this way.
	*/
	public int getIntValue( ) throws NoConversionException
	{
		return delegate.getIntValue();
	}

	/**
	* @return value of this Variable as an array of long integers.
	* Single valued variables will return an array of length 1.
	* @throws NoConversionException if the native value cannot be
	* represented this way.
	*/
	public long[] getLongArrayValue( ) throws NoConversionException
	{
		return delegate.getLongArrayValue();
	}

	/**
	* @return value of this Variable as a long integer.
	* For array variables, this will return the first value in the array.
	* Floating point types will be truncated.
	* @throws NoConversionException if the native value cannot be
	* represented this way.
	*/
	public long getLongValue( ) throws NoConversionException
	{
		return delegate.getLongValue();
	}

	/**
	* @return value of this Variable as a String.
	* No thrown exceptions. All variables can be represented as a String.
	*/
	public String getStringValue( )
	{
		if ((flags & IFlags.IS_MISSING) != 0)
			return "";
		return delegate.getStringValue();
	}

	/**
	* @throws NoConversionException
	*/
	public char getCharValue( ) throws NoConversionException
	{
		return delegate.getCharValue();
	}

	/**
	* @return value as a string.
	*/
	public String toString( )
	{
		return getStringValue();
	}

	/**
	* @return value as a date.
	* @throws NoConversionException
	*/
	public Date getDateValue( ) throws NoConversionException
	{
		return delegate.getDateValue();
	}

	//========== Type Specific Set Methods Overridden by Subclasses =======
	// Note: the set methods can change the native type of a variable.

//	/**
//	 * Sets this Variable's value from a byte array.
//	 * This will change the native type of the variable to BYTE_ARRAY.
//	 * The passed array will be cloned and stored inside the Variable.
//	 */
/*
	public void setValue(byte[] v)
	{
		if (delegate instanceof ByteArrayDelegate)
			((ByteArrayDelegate)delegate).value = v.clone();
		else
			delegate = new ByteArrayDelegate(v.clone());
		setChanged();
	}
*/

	/**
	* Sets this Variable's value from a byte.
	* This will change the native type of the variable to BYTE.
	* @param v the value
	*/
	public void setValue( byte v )
	{
		if (delegate instanceof ByteDelegate)
			((ByteDelegate)delegate).value = v;
		else
			delegate = new ByteDelegate(v);
		setChanged();
	}

//	/**
//	 * Sets this Variable's value from a double array.
//	 * This will change the native type of the variable to DOUBLE_ARRAY.
//	 * The passed array will be cloned and stored inside the Variable.
//	 */
/*
	public void setValue(double[] v)
	{
		if (delegate instanceof DoubleArrayDelegate)
			((DoubleArrayDelegate)delegate).value = v.clone();
		else
			delegate = new DoubleArrayDelegate(v.clone());
		setChanged();
	}
*/

	/**
	* Sets this Variable's value from a double.
	* This will change the native type of the variable to DOUBLE.
	* The passed array will be cloned and stored inside the Variable.
	* @param v the value
	*/
	public void setValue( double v )
	{
		if (delegate instanceof DoubleDelegate)
			((DoubleDelegate)delegate).value = v;
		else
			delegate = new DoubleDelegate(v);
		setChanged();
	}

//	/**
//	 * Sets this Variable's value from a float array.
//	 * This will change the native type of the variable to DOUBLE_ARRAY.
//	 * The passed array will be cloned and stored inside the Variable.
//	 */
/*
	public void setValue(float[] v)
		throws BadArgumentException
	{
		// Up-convert the array then call double-array method.
		double[] nv = new double[v.length];
		for(int i=0; i<v.length; i++)
			nv[i] = (double)v[i];

		setValue(nv);
	}
*/

	/**
	* Sets this Variable's value from a float.
	* This will change the native type of the variable to DOUBLE.
	* @param v the value
	*/
	public void setValue( float v )
	{
		setValue((double)v);
	}

//	/**
//	 * Sets this Variable's value from an int array.
//	 * This will change the native type of the variable to LONG_ARRAY.
//	 * The passed array will be cloned and stored inside the Variable.
//	 */
/*
	public void setValue(int[] v)
	{
		// Up-convert the array then call double-array method.
		long[] nv = new long[v.length];
		for(int i=0; i<v.length; i++)
			nv[i] = (long)v[i];

		setValue(nv);
	}
*/

	/**
	* Sets this Variable's value from an int.
	* This will change the native type of the variable to LONG.
	* @param v the value
	*/
	public void setValue( int v )
	{
		setValue((long)v);
	}

//	/**
//	 * Sets this Variable's value from a long integer array.
//	 * This will change the native type of the variable to LONG_ARRAY.
//	 * The passed array will be cloned and stored inside the Variable.
//	 */
/*
	public void setValue(long[] v)
	{
		if (delegate instanceof LongArrayDelegate)
			((LongArrayDelegate)delegate).value = v.clone();
		else
			delegate = new LongArrayDelegate(v.clone());
		setChanged();
	}
*/

	/**
	* Sets this Variable's value from a long integer.
	* This will change the native type of the variable to LONG.
	* @param v the value
	*/
	public void setValue( long v )
	{
		if (delegate instanceof LongDelegate)
			((LongDelegate)delegate).value = v;
		else
			delegate = new LongDelegate(v);
		setChanged();
	}

	/**
	* Sets this Variable's value from a String.
	* This will change the native type of the variable to STRING.
	* @param v the value
	*/
	public void setValue( String v )
	{
		if (delegate instanceof StringDelegate)
			((StringDelegate)delegate).value = v;
		else
			delegate = new StringDelegate(v);
		setChanged();
	}


	/**
	* Set value to a char.
	* @param v the value
	*/
	public void setValue( char v )
	{
		if (delegate instanceof CharDelegate)
			((CharDelegate)delegate).value = v;
		else
			delegate = new CharDelegate(v);
		setChanged();
	}

	/**
	* Sets this Variable's value from another Variable.
	* @param v the value
	*/
	public void setValue( Variable v ) throws NoConversionException
	{
		delegate = v.delegate.newDelegateVariable(v.delegate);
		setChanged();
	}

	//========== Type specific GetXXXElement methods for array types ====

	/**
	* Gets an element from this Variable and returns it as a byte.
	* This method should only be called for array-type Variables.
	* <p>
	* @param idx
	* @return @throws NoConversionException if the passed value cannot be converted
	* to this Variable's native element type.
	* @throws NotAnArrayException if this is not an array variable.
	* @throws BadIndexException if index<0 or if index > array length.
	*/
	public byte getByteElement( int idx ) throws NoConversionException, NotAnArrayException, BadIndexException
	{
		return delegate.getByteElement(idx);
	}

	/**
	* Gets an element from this Variable and returns it as a boolean.
	* This method should only be called for array-type Variables.
	* <p>
	* @param idx
	* @return @throws NoConversionException if the passed value cannot be converted
	* to this Variable's native element type.
	* @throws NotAnArrayException if this is not an array variable.
	* @throws BadIndexException if index<0 or if index > array length.
	*/
	public boolean getBooleanElement( int idx ) throws NoConversionException, NotAnArrayException, BadIndexException
	{
		return delegate.getBooleanElement(idx);
	}

	/**
	* Gets an element from this Variable and returns it as a double.
	* This method should only be called for array-type Variables.
	* <p>
	* @param idx
	* @return @throws NoConversionException if the passed value cannot be converted
	* to this Variable's native element type.
	* @throws NotAnArrayException if this is not an array variable.
	* @throws BadIndexException if index<0 or if index > array length.
	*/
	public double getDoubleElement( int idx ) throws NoConversionException, NotAnArrayException, BadIndexException
	{
		return delegate.getDoubleElement(idx);
	}

	/**
	* Gets an element from this Variable and returns it as a float.
	* This method should only be called for array-type Variables.
	* <p>
	* @param idx
	* @return @throws NoConversionException if the passed value cannot be converted
	* to this Variable's native element type.
	* @throws NotAnArrayException if this is not an array variable.
	* @throws BadIndexException if index<0 or if index > array length.
	*/
	public float getFloatElement( int idx ) throws NoConversionException, NotAnArrayException, BadIndexException
	{
		return delegate.getFloatElement(idx);
	}

	/**
	* Gets an element from this Variable and returns it as an int.
	* This method should only be called for array-type Variables.
	* <p>
	* @param idx
	* @return @throws NoConversionException if the passed value cannot be converted
	* to this Variable's native element type.
	* @throws NotAnArrayException if this is not an array variable.
	* @throws BadIndexException if index<0 or if index > array length.
	*/
	public int getIntElement( int idx ) throws NoConversionException, NotAnArrayException, BadIndexException
	{
		return delegate.getIntElement(idx);
	}

	/**
	* Gets an element from this Variable and returns it as a long.
	* This method should only be called for array-type Variables.
	* <p>
	* @param idx
	* @return @throws NoConversionException if the passed value cannot be converted
	* to this Variable's native element type.
	* @throws NotAnArrayException if this is not an array variable.
	* @throws BadIndexException if index<0 or if index > array length.
	*/
	public long getLongElement( int idx ) throws NoConversionException, NotAnArrayException, BadIndexException
	{
		return delegate.getLongElement(idx);
	}

	/**
	* Gets an element from this Variable and returns it as a String.
	* This method should only be called for array-type Variables.
	* <p>
	* @param idx
	* @return @throws NoConversionException if the passed value cannot be converted
	* to this Variable's native element type.
	* @throws NotAnArrayException if this is not an array variable.
	* @throws BadIndexException if index<0 or if index > array length.
	*/
	public String getStringElement( int idx ) throws NoConversionException, NotAnArrayException, BadIndexException
	{
		return delegate.getStringElement(idx);
	}

	//========== Type Specific SetElement Methods for array types =======

	/**
	* Sets an element of this Variable to the specified value.
	* This method should only be called for array-type Variables.
	* <p>
	* @param idx
	* @param v the value
	* @throws BadArgumentException if the passed value cannot be converted
	* to this Variable's native element type.
	* @throws NotAnArrayException if index>0 and this is not an array variable.
	* @throws BadIndexException if index<0 or if index > array length.
	*/
	public void setElement( int idx, byte v ) throws NotAnArrayException, BadArgumentException, BadIndexException
	{
		delegate.setElement(idx, v);
		setChanged();
	}

	/**
	* Sets an element of this Variable to the specified value.
	* This method should only be called for array-type Variables.
	* <p>
	* @param idx
	* @param v the value
	* @throws NotAnArrayException if this Variable is not an array and
	* idx is anything other than 0,
	* @throws BadArgumentException if the passed value cannot be converted
	* to this Variable's native element type.
	* @throws BadIndexException if index<0 or if index > array length.
	*/
	public void setElement( int idx, double v ) throws NotAnArrayException, BadArgumentException, BadIndexException
	{
		delegate.setElement(idx, v);
		setChanged();
	}

	/**
	* Sets an element of this Variable to the specified value.
	* This method should only be called for array-type Variables.
	* <p>
	* @param idx
	* @param v the value
	* @throws NotAnArrayException if this Variable is not an array and
	* idx is anything other than 0,
	* @throws BadArgumentException if the passed value cannot be converted
	* to this Variable's native element type.
	* @throws BadIndexException if index<0 or if index > array length.
	*/
	public void setElement( int idx, float v ) throws NotAnArrayException, BadArgumentException, BadIndexException
	{
		delegate.setElement(idx, v);
		setChanged();
	}

	/**
	* Sets an element of this Variable to the specified value.
	* This method should only be called for array-type Variables.
	* <p>
	* @param idx
	* @param v the value
	* @throws NotAnArrayException if this Variable is not an array and
	* idx is anything other than 0,
	* @throws BadArgumentException if the passed value cannot be converted
	* to this Variable's native element type.
	* @throws BadIndexException if index<0 or if index > array length.
	*/
	public void setElement( int idx, int v ) throws NotAnArrayException, BadArgumentException, BadIndexException
	{
		delegate.setElement(idx, v);
		setChanged();
	}

	/**
	* Sets an element of this Variable to the specified value.
	* This method should only be called for array-type Variables.
	* <p>
	* @param idx
	* @param v the value
	* @throws NotAnArrayException if this Variable is not an array and
	* idx is anything other than 0,
	* @throws BadArgumentException if the passed value cannot be converted
	* to this Variable's native element type.
	* @throws BadIndexException if index<0 or if index > array length.
	*/
	public void setElement( int idx, long v ) throws NotAnArrayException, BadArgumentException, BadIndexException
	{
		delegate.setElement(idx, v);
		setChanged();
	}

	/**
	* @return the number of elements in an array variable.
	* Returns 1 if this is not an array (because simple types can always
	* be treated like arrays of length 1).
	* @return
	*/
	public int getNumElements( )
	{
		return delegate.getNumElements();
	}

	//======== Arithmetic Operation on Variable =========================

	/**
	* Divides this Variable's value by the passed Variable's value and returns
	* a new variable with the result.
	* The native type of the returned Variable
	* will depend on the native type of 'this' and of the argument.
	* Appropriate up-conversions will be made when necessary.
	* <p>
	* @param v the value
	* @return @throws NoConversionException if the native type of 'this' is not
	* appropriate for division and cannot be converted.
	* <p>
	* @throws BadArgumentException if the passed argument's type and
	* value is not appropriate for division, or if it's value is zero.
	*/
	public Variable divideBy( Variable v ) throws NoConversionException, BadArgumentException
	{
		return new Variable(delegate.divideBy(v));
	}

	/**
	* Subtracts the passed Variable's value from 'this' and returns
	* a new variable with the result.
	* The native type of the returned Variable
	* will depend on the native type of 'this' and of the argument.
	* Appropriate up-conversions will be made when necessary.
	* <p>
	* @param v the value
	* @return @throws NoConversionException if the native type of 'this' is not
	* appropriate for subtraction and cannot be converted.
	* <p>
	* @throws BadArgumentException if the passed argument's type and
	* value is not appropriate for subtraction.
	*/
	public Variable minus( Variable v ) throws NoConversionException, BadArgumentException
	{
		return new Variable(delegate.minus(v));
	}

	/**
	* Multiplies this Variable's value by the passed Variable's value and
	* returns a new variable with the result.
	* The native type of the returned Variable
	* will depend on the native type of 'this' and of the argument.
	* Appropriate up-conversions will be made when necessary.
	* <p>
	* @param v the value
	* @return @throws NoConversionException if the native type of 'this' is not
	* appropriate for multiplication and cannot be converted.
	* <p>
	* @throws BadArgumentException if the passed argument's type and
	* value is not appropriate for multiplication.
	*/
	public Variable multiplyBy( Variable v ) throws NoConversionException, BadArgumentException
	{
		return new Variable(delegate.multiplyBy(v));
	}

	/**
	* Adds the passed Variable's value to 'this' and returns
	* a new variable with the result.
	* The native type of the returned Variable
	* will depend on the native type of 'this' and of the argument.
	* Appropriate up-conversions will be made when necessary.
	* <p>
	* @param v the value
	* @return @throws NoConversionException if the native type of 'this' is not
	* appropriate for addition and cannot be converted.
	* <p>
	* @throws BadArgumentException if the passed argument's type and
	* value is not appropriate for addition.
	*/
	public Variable plus( Variable v ) throws NoConversionException, BadArgumentException
	{
		return new Variable(delegate.plus(v));
	}

	/**
	 * Sets the source ID for this variable.
	 * @param sourceId the source ID
	 */
	public void setSourceId(DbKey sourceId) { this.sourceId = sourceId; }

	/**
	 * @return the source ID for this variable (-1 means not set).
	 */
	public DbKey getSourceId() { return sourceId; }
	
	/**
	 * @return true if this variable holds a number
	 */
	public boolean isNumeric()
	{
		return
			delegate instanceof ByteDelegate
		 || delegate instanceof DoubleDelegate
		 || delegate instanceof LongDelegate;
	}
}
