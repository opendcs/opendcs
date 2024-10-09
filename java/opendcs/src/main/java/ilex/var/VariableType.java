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
*  Revision 1.6  2004/08/30 14:50:37  mjmaloney
*  Javadocs
*
*  Revision 1.5  2001/09/14 21:19:37  mike
*  dev
*
*  Revision 1.4  2001/09/09 17:38:30  mike
*  Added CharDelegate & support functions.
*
*  Revision 1.3  2000/11/24 22:20:57  mike
*  Added support for boolean variables.
*
*  Revision 1.2  2000/11/16 21:45:22  mike
*  dev
*
*  Revision 1.1  2000/11/16 02:36:24  mike
*  dev
*
*
*/
package ilex.var;

/**
* Constants for the type values returned by Variable.getNativeTypeId();
*/
public class VariableType
{
	// Note all array types are upper case. Non-arrays are lower case.
	// Type-checking code depends on this!!

	public static final char BYTE = 'b';
	public static final char BYTE_ARRAY = 'B';
	public static final char DOUBLE = 'd';
	public static final char DOUBLE_ARRAY = 'D';
	public static final char FLOAT = 'f';
	public static final char FLOAT_ARRAY = 'F';
	public static final char INT = 'i';
	public static final char INT_ARRAY = 'I';
	public static final char LONG = 'l';
	public static final char LONG_ARRAY = 'L';
	public static final char CHAR = 'c';
	public static final char STRING = 'C';
	public static final char BOOLEAN = 't';
	public static final char BOOLEAN_ARRAY = 'T';
	public static final char DATE = 'm';

	// Type used by no-argument constructor:
	public static final char DEFAULT_TYPE = INT;
}

