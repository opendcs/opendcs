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
*  Revision 1.6  2004/08/30 14:50:34  mjmaloney
*  Javadocs
*
*  Revision 1.5  2001/10/26 17:55:03  mike
*  Bug fix: Added getDateValue to IVariable and Variable.
*
*  Revision 1.4  2001/09/09 17:38:30  mike
*  Added CharDelegate & support functions.
*
*  Revision 1.3  2000/11/24 22:20:57  mike
*  Added support for boolean variables.
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
import ilex.var.NoConversionException;
import ilex.var.NotAnArrayException;
import ilex.var.BadIndexException;
import ilex.var.BadArgumentException;

/**
* Interface IVariable defines a common interface for all of the concrete
* variable classes. It is implemented by Variable and DelegateVariable.
* See class Variable for detailed documentation on each method.
*/
public abstract interface IVariable
{
	/**
	* @return constant from VariableType class indicating this type of value.
	*/
	char getNativeType( );

	/**
	* @return true if this is an array type
	*/
	boolean isArray( );

	/**
	* @return  value as a byte array
	* @throws NoConversionException
	*/
	byte[] getByteArrayValue( ) throws NoConversionException;

	/**
	* @return value as a byte
	* @throws NoConversionException
	*/
	byte getByteValue( ) throws NoConversionException;

	/**
	* @return value as a boolean
	* @throws NoConversionException
	*/
	boolean getBooleanValue( ) throws NoConversionException;

	/**
	* @return value as a boolean array
	* @throws NoConversionException
	*/
	boolean[] getBooleanArrayValue( ) throws NoConversionException;

	/**
	* @return value as a double array
	* @throws NoConversionException
	*/
	double[] getDoubleArrayValue( ) throws NoConversionException;

	/**
	* @return value as a double 
	* @throws NoConversionException
	*/
	double getDoubleValue( ) throws NoConversionException;

	/**
	* @return value as a float array
	* @throws NoConversionException
	*/
	float[] getFloatArrayValue( ) throws NoConversionException;

	/**
	* @return value as a float 
	* @throws NoConversionException
	*/
	float getFloatValue( ) throws NoConversionException;

	/**
	* @return value as an int array
	* @throws NoConversionException
	*/
	int[] getIntArrayValue( ) throws NoConversionException;

	/**
	* @return value as an int 
	* @throws NoConversionException
	*/
	int getIntValue( ) throws NoConversionException;

	/**
	* @return value as a long array
	* @throws NoConversionException
	*/
	long[] getLongArrayValue( ) throws NoConversionException;

	/**
	* @return value as a long 
	* @throws NoConversionException
	*/
	long getLongValue( ) throws NoConversionException;

	/**
	* @return value as a char
	* @throws NoConversionException
	*/
	char getCharValue( ) throws NoConversionException;

	/**
	* Return value as a String. No throwing -- all variables are guaranteed
	* to be able to represent as string.
	* @return value as a String
	*/
	String getStringValue( );


	//========== Type specific GetXXXElement methods for array types ====

	/**
	* Get an array element.
	* @param idx the index of the desired element in the array
	* @return byte
	*/
	byte getByteElement( int idx ) throws NoConversionException, NotAnArrayException, BadIndexException;

	/**
	* Get an array element.
	* @param idx the index of the desired element in the array
	* @return boolean
	*/
	boolean getBooleanElement( int idx ) throws NoConversionException, NotAnArrayException, BadIndexException;

	/**
	* Get an array element.
	* @param idx the index of the desired element in the array
	* @return double
	*/
	double getDoubleElement( int idx ) throws NoConversionException, NotAnArrayException, BadIndexException;

	/**
	* Get an array element.
	* @param idx the index of the desired element in the array
	* @return float 
	*/
	float getFloatElement( int idx ) throws NoConversionException, NotAnArrayException, BadIndexException;

	/**
	* Get an array element.
	* @param idx the index of the desired element in the array
	* @return int
	*/
	int getIntElement( int idx ) throws NoConversionException, NotAnArrayException, BadIndexException;

	/**
	* Get an array element.
	* @param idx the index of the desired element in the array
	* @return long
	*/
	long getLongElement( int idx ) throws NoConversionException, NotAnArrayException, BadIndexException;

	/**
	* Get an array element.
	* @param idx the index of the desired element in the array
	* @return String
	*/
	String getStringElement( int idx ) throws NoConversionException, NotAnArrayException, BadIndexException;

	/**
	* Get value as a date.
	* @return Date
	* @throws NoConversionException
	*/
	Date getDateValue( ) throws NoConversionException;

	//========== Type Specific SetElement Methods for array types =======

	/**
	* Sets an array value.
	* @param idx the index of the desired element in the array
	* @param v the value
	* @throws NotAnArrayException
	* @throws BadArgumentException
	* @throws BadIndexException
	*/
	void setElement( int idx, byte v ) throws NotAnArrayException, BadArgumentException, BadIndexException;

	/**
	* Sets an array value.
	* @param idx the index of the desired element in the array
	* @param v the value
	* @throws NotAnArrayException
	* @throws BadArgumentException
	* @throws BadIndexException
	*/
	void setElement( int idx, double v ) throws NotAnArrayException, BadArgumentException, BadIndexException;

	/**
	* Sets an array value.
	* @param idx the index of the desired element in the array
	* @param v the value
	* @throws NotAnArrayException
	* @throws BadArgumentException
	* @throws BadIndexException
	*/
	void setElement( int idx, float v ) throws NotAnArrayException, BadArgumentException, BadIndexException;

	/**
	* Sets an array value.
	* @param idx the index of the desired element in the array
	* @param v the value
	* @throws NotAnArrayException
	* @throws BadArgumentException
	* @throws BadIndexException
	*/
	void setElement( int idx, int v ) throws NotAnArrayException, BadArgumentException, BadIndexException;

	/**
	* Sets an array value.
	* @param idx the index of the desired element in the array
	* @param v the value
	* @throws NotAnArrayException
	* @throws BadArgumentException
	* @throws BadIndexException
	*/
	void setElement( int idx, long v ) throws NotAnArrayException, BadArgumentException, BadIndexException;

	/**
	* @return number of array elements (1 if this isn't an array)
	*/
	int getNumElements( );

	// Arithmentic methods are defined separately (and slightly differently)
	// in Variable and DelegateVariable.
}
