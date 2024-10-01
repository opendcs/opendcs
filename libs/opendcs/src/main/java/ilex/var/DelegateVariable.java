/*
*  $Id$
*
*  $Source$
*
*  $State$
*
*  $Log$
*  Revision 1.2  2013/04/03 18:57:58  shweta
*  added bytearray delegate
*
*  Revision 1.1  2008/04/04 18:21:10  cvs
*  Added legacy code to repository
*
*  Revision 1.9  2004/08/31 16:35:47  mjmaloney
*  javadoc
*
*  Revision 1.8  2004/08/30 14:50:34  mjmaloney
*  Javadocs
*
*  Revision 1.7  2001/09/18 00:46:50  mike
*  Working implementation of DateDelegate
*
*  Revision 1.6  2001/09/14 21:19:37  mike
*  dev
*
*  Revision 1.5  2000/11/24 22:20:57  mike
*  Added support for boolean variables.
*
*  Revision 1.4  2000/11/22 15:14:17  mike
*  dev
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

/**
* Class DelegateVariable is used by Variable to provide all the type-
* specific functionality. All type-specific Variable classes will extend
* DelegateVariable. This class also provides default behavior where
* appropriate so that every type-specific class doesn't have to override
* every method.
* <p>
* See class Variable for detailed documentation on each method.
*/
public abstract class DelegateVariable implements IVariable
{
	/** Default constructor. */
	public DelegateVariable( )
	{
	}

	/**
	* Copy constructor factory
	* @param dv delegate to copy
	* @return copy of passed variable.
	* @throws NoConversionException if unknown variable type
	*/
	public static DelegateVariable newDelegateVariable( DelegateVariable dv ) throws NoConversionException
	{
		if (dv instanceof ByteDelegate)
			return new ByteDelegate(dv.getByteValue());
		else if (dv instanceof BooleanDelegate)
			return new BooleanDelegate(dv.getBooleanValue());
		else if (dv instanceof LongDelegate)
			return new LongDelegate(dv.getLongValue());
		else if (dv instanceof DoubleDelegate)
			return new DoubleDelegate(dv.getDoubleValue());
		else if (dv instanceof StringDelegate)
			return new StringDelegate(dv.getStringValue());
		else if (dv instanceof DateDelegate)
			return new DateDelegate(dv.getDateValue());
		else if (dv instanceof ByteArrayDelegate)
			return new ByteArrayDelegate(dv.getByteArrayValue());
		else
			throw new NoConversionException("Unknown variable type");
	}

	// Do not implement getNativeType() - implemented by concrete subclass.

	/**
	* @return true if this is an array type
	*/
	public boolean isArray( )
	{
		return Character.isUpperCase(getNativeType());
	}

	/**
	* @return constant from VariableType class indicating this type of value.
	*/
	public abstract char getNativeType( );

	//========== Type Specific Get Methods Overridden by Subclasses =======

	/**
	* @return @throws NoConversionException
	*/
	public byte[] getByteArrayValue( ) throws NoConversionException
	{
		// Array types will override this method. Simple types need not.
		if (!isArray())
		{
			byte[] r = new byte[1];
			r[0] = getByteValue();
			return r;
		}
		throw new NoConversionException(
			"Code error: No override in array type Variable.");
	}

	/**
	* @return the first element of array types.
	*/
	public byte getByteValue( ) throws NoConversionException
	{
		// Simple types will override this method. Array types need not.
		if (isArray())
		{
			try { return getByteElement(0); }
			catch(VariableException bie)
			{
				throw new NoConversionException(
					"Cannot get value for zero-length array");
			}
		}
		throw new NoConversionException(
			"Code error: No override in simple type Variable.");
	}

	/**
	* @return the first element of array types.
	*/
	public boolean[] getBooleanArrayValue( ) throws NoConversionException
	{
		// Array types will override this method. Simple types need not.
		if (!isArray())
		{
			boolean[] r = new boolean[1];
			r[0] = getBooleanValue();
			return r;
		}
		throw new NoConversionException(
			"Code error: No override in array type Variable.");
	}

	/**
	* @return the first element of array types.
	*/
	public boolean getBooleanValue( ) throws NoConversionException
	{
		// Simple types will override this method. Array types need not.
		if (isArray())
		{
			try { return getBooleanElement(0); }
			catch(VariableException bie)
			{
				throw new NoConversionException(
					"Cannot get value for zero-length array");
			}
		}
		throw new NoConversionException(
			"Code error: No override in simple type Variable.");
	}

	/**
	* @return @throws NoConversionException
	*/
	public double[] getDoubleArrayValue( ) throws NoConversionException
	{
		// Array types will override this method. Simple types need not.
		if (!isArray())
		{
			double[] r = new double[1];
			r[0] = getDoubleValue();
			return r;
		}
		throw new NoConversionException(
			"Code error: No override in array type Variable.");
	}

	/**
	* @return the first element of array types.
	*/
	public double getDoubleValue( ) throws NoConversionException
	{
		// Simple types will override this method. Array types need not.
		if (isArray())
		{
			try { return getDoubleElement(0); }
			catch(VariableException bie)
			{
				throw new NoConversionException(
					"Cannot get value for zero-length array");
			}
		}
		throw new NoConversionException(
			"Code error: No override in simple type Variable.");
	}

	/**
	* @return @throws NoConversionException
	*/
	public float[] getFloatArrayValue( ) throws NoConversionException
	{
		// Array types will override this method. Simple types need not.
		if (!isArray())
		{
			float[] r = new float[1];
			r[0] = getFloatValue();
			return r;
		}
		throw new NoConversionException(
			"Code error: No override in array type Variable.");
	}

	/**
	* @return @throws NoConversionException
	*/
	public float getFloatValue( ) throws NoConversionException
	{
		// Simple types will override this method. Array types need not.
		if (isArray())
		{
			try { return getFloatElement(0); }
			catch(VariableException bie)
			{
				throw new NoConversionException(
					"Cannot get value for zero-length array");
			}
		}
		throw new NoConversionException(
			"Code error: No override in simple type Variable.");
	}

	/**
	* @return @throws NoConversionException
	*/
	public int[] getIntArrayValue( ) throws NoConversionException
	{
		// Array types will override this method. Simple types need not.
		if (!isArray())
		{
			int[] r = new int[1];
			r[0] = getIntValue();
			return r;
		}
		throw new NoConversionException(
			"Code error: No override in array type Variable.");
	}

	/**
	* @return @throws NoConversionException
	*/
	public int getIntValue( ) throws NoConversionException
	{
		// Simple types will override this method. Array types need not.
		if (isArray())
		{
			try { return getIntElement(0); }
			catch(VariableException bie)
			{
				throw new NoConversionException(
					"Cannot get value for zero-length array");
			}
		}
		throw new NoConversionException(
			"Code error: No override in simple type Variable.");
	}

	/**
	* @return @throws NoConversionException
	*/
	public long[] getLongArrayValue( ) throws NoConversionException
	{
		// Array types will override this method. Simple types need not.
		if (!isArray())
		{
			long[] r = new long[1];
			r[0] = getLongValue();
			return r;
		}
		throw new NoConversionException(
			"Code error: No override in array type Variable.");
	}

	/**
	* @return @throws NoConversionException
	*/
	public long getLongValue( ) throws NoConversionException
	{
		// Simple types will override this method. Array types need not.
		if (isArray())
		{
			try { return getLongElement(0); }
			catch(VariableException bie)
			{
				throw new NoConversionException(
					"Cannot get value for zero-length array");
			}
		}
		throw new NoConversionException(
			"Code error: No override in simple type Variable.");
	}

	/**
	* @return
	*/
	public String getStringValue( )
	{
		// Simple types will override this method. Array types need not.
		if (isArray())
		{
			StringBuffer r = new StringBuffer();
			try // No exceptions should be thrown, but we have to catch them.
			{
				r.append(getStringElement(0));
				for(int i=1; i<getNumElements(); i++)
				{
					r.append(", ");
					r.append(getStringElement(i));
				}
			}
			catch (VariableException nce) {}

			return r.toString();
		}
		else
			return ""; // Should not happen, simple types will override.
	}

	/**
	* @return @throws NoConversionException
	*/
	public abstract char getCharValue( ) throws NoConversionException;

	/**
	* @return @throws NoConversionException
	*/
	public abstract Date getDateValue( ) throws NoConversionException;


	/**
	* @return
	*/
	public String toString( )
	{
		return getStringValue();
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
		// Array types will override, simple types need not.
		if (!isArray())
			throw new NotAnArrayException("Cannot get element of non-array");
		else
			throw new NoConversionException(
				"Code Error: array type did not override getByteElement()");
	}

	/**
	* @param idx
	* @return @throws NoConversionException
	* @throws BadIndexException
	* @throws NotAnArrayException
	*/
	public boolean getBooleanElement( int idx ) throws NoConversionException, BadIndexException, NotAnArrayException
	{
		// Array types will override, simple types need not.
		if (!isArray())
			throw new NotAnArrayException("Cannot get element of non-array");
		else
			throw new NoConversionException(
				"Code Error: array type did not override getBooleanElement()");
	}

	/**
	* @param idx
	* @return @throws NoConversionException
	* @throws BadIndexException
	* @throws NotAnArrayException
	*/
	public double getDoubleElement( int idx ) throws NoConversionException, BadIndexException, NotAnArrayException
	{
		// Array types will override, simple types need not.
		if (!isArray())
			throw new NotAnArrayException("Cannot get element of non-array");
		else
			throw new NoConversionException(
				"Code Error: array type did not override getDoubleElement()");
	}

	/**
	* @param idx
	* @return @throws NoConversionException
	* @throws BadIndexException
	* @throws NotAnArrayException
	*/
	public float getFloatElement( int idx ) throws NoConversionException, BadIndexException, NotAnArrayException
	{
		// Array types will override, simple types need not.
		if (!isArray())
			throw new NotAnArrayException("Cannot get element of non-array");
		else
			throw new NoConversionException(
				"Code Error: array type did not override getFloatElement()");
	}

	/**
	* @param idx
	* @return @throws NoConversionException
	* @throws BadIndexException
	* @throws NotAnArrayException
	*/
	public int getIntElement( int idx ) throws NoConversionException, BadIndexException, NotAnArrayException
	{
		// Array types will override, simple types need not.
		if (!isArray())
			throw new NotAnArrayException("Cannot get element of non-array");
		else
			throw new NoConversionException(
			"Code Error: array type did not override getIntElement()");
	}

	/**
	* @param idx
	* @return @throws NoConversionException
	* @throws BadIndexException
	* @throws NotAnArrayException
	*/
	public long getLongElement( int idx ) throws NoConversionException, BadIndexException, NotAnArrayException
	{
		// Array types will override, simple types need not.
		if (!isArray())
			throw new NotAnArrayException("Cannot get element of non-array");
		else
			throw new NoConversionException(
			"Code Error: array type did not override getLongElement()");
	}

	/**
	* @param idx
	* @return @throws NoConversionException
	* @throws BadIndexException
	* @throws NotAnArrayException
	*/
	public String getStringElement( int idx ) throws NoConversionException, BadIndexException, NotAnArrayException
	{
		// Array types will override, simple types need not.
		if (!isArray())
			throw new NotAnArrayException("Cannot get element of non-array");
		else
			throw new NoConversionException(
			"Code Error: array type did not override getStringElement()");
	}


	//========== Type Specific SetElement Methods for array types =======

	/**
	* @param idx
	* @param v
	* @throws NotAnArrayException
	* @throws BadArgumentException
	* @throws BadIndexException
	*/
	public void setElement( int idx, byte v ) throws NotAnArrayException, BadArgumentException, BadIndexException
	{
		// Array types will override, simple types need not.
		if (!isArray())
			throw new NotAnArrayException(
				"Cannot set element of non-array");
		else
			throw new NotAnArrayException(
				"Code Error: array type did not override setElement(idx,byte)");
	}

	/**
	* @param idx
	* @param v
	* @throws NotAnArrayException
	* @throws BadArgumentException
	* @throws BadIndexException
	*/
	public void setElement( int idx, double v ) throws NotAnArrayException, BadArgumentException, BadIndexException
	{
		// Array types will override, simple types need not.
		if (!isArray())
			throw new NotAnArrayException(
				"Cannot set element of non-array");
		else
			throw new NotAnArrayException(
			"Code Error: array type did not override setElement(idx,double)");
	}

	/**
	* @param idx
	* @param v
	* @throws NotAnArrayException
	* @throws BadArgumentException
	* @throws BadIndexException
	*/
	public void setElement( int idx, float v ) throws NotAnArrayException, BadArgumentException, BadIndexException
	{
		// Array types will override, simple types need not.
		if (!isArray())
			throw new NotAnArrayException(
				"Cannot set element of non-array");
		else
			throw new NotAnArrayException(
			"Code Error: array type did not override setElement(idx,float)");
	}

	/**
	* @param idx
	* @param v
	* @throws NotAnArrayException
	* @throws BadArgumentException
	* @throws BadIndexException
	*/
	public void setElement( int idx, int v ) throws NotAnArrayException, BadArgumentException, BadIndexException
	{
		// Array types will override, simple types need not.
		if (!isArray())
			throw new NotAnArrayException(
				"Cannot set element of non-array");
		else
			throw new NotAnArrayException(
			"Code Error: array type did not override setElement(idx,int)");
	}

	/**
	* @param idx
	* @param v
	* @throws NotAnArrayException
	* @throws BadArgumentException
	* @throws BadIndexException
	*/
	public void setElement( int idx, long v ) throws NotAnArrayException, BadArgumentException, BadIndexException
	{
		// Array types will override, simple types need not.
		if (!isArray())
			throw new NotAnArrayException(
				"Cannot set element of non-array");
		else
			throw new NotAnArrayException(
			"Code Error: array type did not override setElement(idx,long)");
	}

	/**
	* @return
	*/
	public int getNumElements( )
	{
		// Array types need to override, simple types need not.
		if (!isArray())
			return 1;
		else
			return 0; // should not happen.
	}

	//======== Arithmetic Operation on Variable =========================
	// Signatures for arithmetic methods differ from those defined in
	// class Variable.

	/**
	* @param v
	* @return @throws NoConversionException
	* @throws BadArgumentException
	*/
	public abstract DelegateVariable divideBy( IVariable v ) throws NoConversionException, BadArgumentException;
	
	/**
	* @param v
	* @return @throws NoConversionException
	* @throws BadArgumentException
	*/
	public abstract DelegateVariable minus( IVariable v ) throws NoConversionException, BadArgumentException;

	/**
	* @param v
	* @return @throws NoConversionException
	* @throws BadArgumentException
	*/
	public abstract DelegateVariable multiplyBy( IVariable v ) throws NoConversionException, BadArgumentException;
	
	/**
	* @param v
	* @return @throws NoConversionException
	* @throws BadArgumentException
	*/
	public abstract DelegateVariable plus( IVariable v ) throws NoConversionException, BadArgumentException;
}
