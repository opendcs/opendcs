/*
*  $Id$
*
*  $Source$
*
*  $State$
*
*  $Log$
*  Revision 1.2  2008/09/05 12:53:53  mjmaloney
*  LRGS 7 dev
*
*  Revision 1.1  2008/04/04 18:21:09  cvs
*  Added legacy code to repository
*
*  Revision 1.5  2005/04/06 12:21:18  mjmaloney
*  dev
*
*  Revision 1.4  2004/08/30 14:50:24  mjmaloney
*  Javadocs
*
*  Revision 1.3  1999/11/16 13:26:20  mike
*  In resize() fill remainder of returned array with 0 or null.
*
*  Revision 1.2  1999/09/27 20:18:01  mike
*  9/27/1999
*
*  Revision 1.1  1999/09/23 12:33:41  mike
*  Initial implementation
*
*
*/
package ilex.util;

/**
This class contains static utilities for manipulating arrays. These
methods supplement the ones in java.util.Arrays.
*/
public class ArrayUtil
{
	/**
	* The getField methods return a subset of the data as a new array.
	* They return null if either of the indices are outside the array
	* bounds.
	* @param data the complete data string
	* @param start the start of the desired field (0 == first byte)
	* @param length the length of the desired field.
	* @return desired field, or null if outside bounds of data.
	*/
	public static final byte[] getField( byte[] data, int start, int length )
	{
		if (data == null || start < 0
		 || length < 0 || (start+length) > data.length)
			return null;

		byte ret[] = new byte[length];
		for(int i = 0; length-- > 0; ret[i++] = data[start++]);
		return ret;
	}

	/**
	* Returns a portion of an integer array.
	* @param data the complete data
	* @param start the start of the desired field (0 == first array value)
	* @param length the length of the desired field.
	* @return requested portion of the array
	*/
	public static final int[] getField( int[] data, int start, int length )
	{
		if (data == null || start < 0
		 || length < 0 || (start+length) > data.length)
			return null;

		int ret[] = new int[length];
		for(int i = 0; length-- > 0; ret[i++] = data[start++]);
		return ret;
	}

	/**
	* Returns a portion of an character array.
	* @param data the complete data
	* @param start the start of the desired field (0 == first array value)
	* @param length the length of the desired field.
	* @return requested portion of the array
	*/
	public static final char[] getField( char[] data, int start, int length )
	{
		if (data == null || start < 0
		 || length < 0 || (start+length) > data.length)
			return null;

		char ret[] = new char[length];
		for(int i = 0; length-- > 0; ret[i++] = data[start++]);
		return ret;
	}

	/**
	* Returns a portion of an long-integer array.
	* @param data the complete data
	* @param start the start of the desired field (0 == first array value)
	* @param length the length of the desired field.
	* @return requested portion of the array
	*/
	public static final long[] getField( long[] data, int start, int length )
	{
		if (data == null || start < 0
		 || length < 0 || (start+length) > data.length)
			return null;

		long ret[] = new long[length];
		for(int i = 0; length-- > 0; ret[i++] = data[start++]);
		return ret;
	}

	/**
	* Returns a portion of an float array.
	* @param data the complete data
	* @param start the start of the desired field (0 == first array value)
	* @param length the length of the desired field.
	* @return requested portion of the array
	*/
	public static final float[] getField( float[] data, int start, int length )
	{
		if (data == null || start < 0
		 || length < 0 || (start+length) > data.length)
			return null;

		float ret[] = new float[length];
		for(int i = 0; length-- > 0; ret[i++] = data[start++]);
		return ret;
	}

	/**
	* Returns a portion of an double precision float array.
	* @param data the complete data
	* @param start the start of the desired field (0 == first array value)
	* @param length the length of the desired field.
	* @return requested portion of the array
	*/
	public static final double[] getField( double[] data, int start, int length )
	{
		if (data == null || start < 0
		 || length < 0 || (start+length) > data.length)
			return null;

		double ret[] = new double[length];
		for(int i = 0; length-- > 0; ret[i++] = data[start++]);
		return ret;
	}

	/**
	* Returns a portion of an Object array.
	* @param data the complete data
	* @param start the start of the desired field (0 == first array value)
	* @param length the length of the desired field.
	* @return requested portion of the array
	*/
	public static final Object[] getField( Object[] data, int start, int length )
	{
		if (data == null || start < 0
		 || length < 0 || (start+length) > data.length)
			return null;

		Object ret[] = new Object[length];
		for(int i = 0; length-- > 0; ret[i++] = data[start++]);
		return ret;
	}

	/**
	* Resize will return an array with a new size containing the elements
	* from the original array. It can be used to either grow or shrink an
	* array. If the size is increased, the new unused space is 0-filled.
	* @param data the array
	* @param newsize the new size
	* @return resized array
	*/
	public static final byte[] resize( byte[] data, int newsize )
	{
		byte ret[] = new byte[newsize];

		int n = data.length < newsize ? data.length : newsize;
		int i;
		for(i = 0; i < n; i++)
			ret[i] = data[i];

		while(i < newsize)
			ret[i++] = (byte)0;
	
		return ret;
	}

	/**
	* Resizes a character array.
	* @param data the array
	* @param newsize the new size
	* @return resized array
	*/
	public static final char[] resize( char[] data, int newsize )
	{
		char ret[] = new char[newsize];

		int n = data.length < newsize ? data.length : newsize;
		int i;
		for(i = 0; i < n; i++)
			ret[i] = data[i];
	
		while(i < newsize)
			ret[i++] = (char)0;

		return ret;
	}

	/**
	* Resizes an integer array.
	* @param data the array
	* @param newsize the new size
	* @return resized array
	*/
	public static final int[] resize( int[] data, int newsize )
	{
		int ret[] = new int[newsize];

		int n = data.length < newsize ? data.length : newsize;
		int i;
		for(i = 0; i < n; i++)
			ret[i] = data[i];
	
		while(i < newsize)
			ret[i++] = 0;

		return ret;
	}

	/**
	* Resizes a long-integer array.
	* @param data the array
	* @param newsize the new size
	* @return resized array
	*/
	public static final long[] resize( long[] data, int newsize )
	{
		long ret[] = new long[newsize];

		int n = data.length < newsize ? data.length : newsize;
		int i;
		for(i = 0; i < n; i++)
			ret[i] = data[i];
	
		while(i < newsize)
			ret[i++] = (long)0;

		return ret;
	}

	/**
	* Resizes a float array.
	* @param data the array
	* @param newsize the new size
	* @return resized array
	*/
	public static final float[] resize( float[] data, int newsize )
	{
		float ret[] = new float[newsize];

		int n = data.length < newsize ? data.length : newsize;
		int i;
		for(i = 0; i < n; i++)
			ret[i] = data[i];
	
		while(i < newsize)
			ret[i++] = (float)0.0;

		return ret;
	}

	/**
	* Resizes a double-precision float array.
	* @param data the array
	* @param newsize the new size
	* @return resized array
	*/
	public static final double[] resize( double[] data, int newsize )
	{
		double ret[] = new double[newsize];

		int n = data.length < newsize ? data.length : newsize;
		int i;
		for(i = 0; i < n; i++)
			ret[i] = data[i];
	
		while(i < newsize)
			ret[i++] = (double)0.0;

		return ret;
	}


	/**
	* Resizes an Object array.
	* @param data the array
	* @param newsize the new size
	* @return resized array
	*/
	public static final Object[] resize( Object[] data, int newsize )
	{
		Object ret[] = new Object[newsize];

		int n = data.length < newsize ? data.length : newsize;
		int i;
		for(i = 0; i < n; i++)
			ret[i] = data[i];
	
		while(i < newsize)
			ret[i++] = null;

		return ret;
	}

	/**
	 * Combines two arrays. Returned array will be all values from
	 * first array followed by all values of the second.
	 */
	public static final Object[] combined(Object[] a1, Object[] a2)
	{
		Object ret[] = new Object[a1.length + a2.length];
		for(int i=0; i<a1.length; i++)
			ret[i] = a1[i];
		for(int i=0; i<a2.length; i++)
			ret[i+a1.length] = a2[i];
		return ret;
	}
}

