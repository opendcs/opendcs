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
*  Revision 1.6  2007/04/09 20:01:36  mmaloney
*  dev
*
*  Revision 1.5  2004/08/30 14:50:34  mjmaloney
*  Javadocs
*
*  Revision 1.4  2004/08/11 21:52:15  mjmaloney
*  Added limit bit defs
*
*  Revision 1.3  2002/04/17 21:41:41  mike
*  Added flags for IS_MISSING and IS_ERROR.
*  Variable toString() method returns "" if either is set.
*
*  Revision 1.2  2000/11/22 16:06:36  mike
*  dev
*
*  Revision 1.1  2000/11/22 15:14:17  mike
*  dev
*
*/
package ilex.var;

/**
* This interface defines constant bit-field values used by the flag values
* in Variable and NamedVariableList. It also defines an interface for
* setting & getting the flag values as a whole, and specific bits.
*/
public abstract interface IFlags
{
	/**
	* Do not define any application-specific bits in the reserved mask.
	* These bits are defined herein or are reserved for future use.
	*/
	int RESERVED_MASK = 0xf0000000; 

	/**
	* You can define any bits within this mask for application-specific
	* meaning. The Variable family of classes do not use these bits.
	*/
	int APPLICATION_MASK = 0x0fffffff;

 	/**
	* If the IS_CHANGED bit is set, this variable (or list) has been modified
	* since the last time resetChanged() was called. You can check the value
	* of this bit by calling isChanged().
	*/
	int IS_CHANGED = 0x10000000; 

	/**
	* If the IS_ERROR flag is set, it means the value contains an error,
	* and should probably not be used.
	*/
	int IS_ERROR = 0x20000000;

	/**
	* The IS_MISSING flag means that this variable is a place-holder and
	* actually contains no usable data. The delegate is most-likely an
	* integer.
	*/
	int IS_MISSING = 0x40000000;
	
	/**
	* Value is out of preset limits
	*/
	int LIMIT_VIOLATION = 0x80000000;

	/**
	* @return the complete 32-bit integer flag value.
	*/
	int getFlags( );

	/**
	* Sets the 32-bit integer flag value.
	* @param f the flag containing bits to set
	*/
	void setFlags( int f );

	/**
	* @return true if the value of this variable has been changed.
	*/
	boolean isChanged( );

	/**
	* Resets the IS_CHANGED bit so we can detect future modifications.
	*/
	void resetChanged( );

	/**
	* Sets the flag so that subsequent calls to isChanged will return true.
	*/
	void setChanged( );
}
