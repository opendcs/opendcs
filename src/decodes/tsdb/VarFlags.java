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
*
*  $Log$
*  Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
*  OPENDCS 6.0 Initial Checkin
*
*  Revision 1.2  2010/08/20 19:19:31  mmaloney
*  Code to handle NO-OVERWRITE feature in Tempest and CWMS.
*
*  Revision 1.1  2008/04/04 18:21:06  cvs
*  Added legacy code to repository
*
*  Revision 1.6  2007/08/27 18:33:24  mmaloney
*  dev
*
*  Revision 1.5  2007/08/19 15:09:54  mmaloney
*  Use timezone from decodes.properties by default.
*
*  Revision 1.4  2007/07/17 15:09:41  mmaloney
*  dev
*
*  Revision 1.3  2006/05/23 20:37:24  mmaloney
*  dev
*
*  Revision 1.2  2006/04/04 19:07:14  mmaloney
*  HDB write/delete methods implemented.
*
*  Revision 1.1  2005/12/10 21:40:41  mmaloney
*  Created.
*
*/
package decodes.tsdb;

import ilex.var.Variable;
import ilex.var.IFlags;

/**
This class defines the bits used in the flag words on each variable.
We use only the bits defined to be part of APPLICATION_MASK in the
IFlags.java file define in the ilex.var package.
@see ilex.var.IFlags
*/
public class VarFlags
{
	/** Set on variables that were recently added to the database and should
	    therefore be used to trigger computations. */
	public static final int DB_ADDED     = 0x00000001;

	/** Set on variables that were recently deleted from the database and should
	    therefore be used to trigger computations. */
	public static final int DB_DELETED   = 0x00000002;

	/** Set on variables that are the output of computations and need to
	    be written back to the database. */
	public static final int TO_WRITE     = 0x00000004;
	
	/** Set on variables that were deleted by a computation and need to
	    be delted from the database. */
	public static final int TO_DELETE    = 0x00000008;
	
	/** If both TO_WRITE and TO_DELETE are set, it means to write only if it
	 * doesn't already exist in the database. It means NO_OVERWRITE.
	 */
	public static final int NO_OVERWRITE = TO_WRITE | TO_DELETE;

	/**
	 * Used by comps that modify input flags, telling them to NOT refire
	 * the trigger. This is a pseudo-flag, used to communicate between
	 * the java computation and the trigger stored procedure. The trigger
	 * removes it so it is not written to the database.
	 */
	// Obsolete.
	//public static final int NO_TRIGGER   = 0x08000000;

	/** Computations use the 4 low-order bits */
	public static final int RESERVED_4_COMP               = 0x0000000F;

	/**
	 * Return true if the variable was recently added to the database.
	 * @param v the variable
	 * @return true if the variable was recently added to the database.
	 */
	public static boolean wasAdded(Variable v)
	{
		return (v.getFlags() & DB_ADDED) != 0;
	}

	/**
	 * Sets the DB_ADDED flag.
	 * @param v the variable
	 */
	public static void setWasAdded(Variable v)
	{
		int f = v.getFlags();
		f |= DB_ADDED;
		v.setFlags(f);
	}

	/**
	 * Return true if the variable was recently deleted from the database.
	 * @param v the variable
	 * @return true if the variable was recently deleted from the database.
	 */
	public static boolean wasDeleted(Variable v)
	{
		return (v.getFlags() & DB_DELETED) != 0;
	}

	/**
	 * Sets the DB_DELETED flag.
	 * @param v the variable
	 */
	public static void setWasDeleted(Variable v)
	{
		int f = v.getFlags();
		f |= DB_DELETED;
		v.setFlags(f);
	}

	/**
	 * Return true if the variable must be written to the database.
	 * @param v the variable
	 * @return true if the variable must be written to the database.
	 */
	public static boolean mustWrite(Variable v)
	{
		int f = v.getFlags();
		return (f & TO_WRITE) != 0 && (f & IFlags.IS_MISSING) == 0;
	}

	/**
	 * Return true if the variable must be deleted from the database.
	 * @param v the variable
	 * @return true if the variable must be deleted from the database.
	 */
	public static boolean mustDelete(Variable v)
	{
		return (v.getFlags() & NO_OVERWRITE) == TO_DELETE;
	}

	/**
	 * Sets the TO_WRITE flag so that this variable will be written
	 * to the database after all computations are finished.
	 * @param v the variable
	 */
	public static void setToWrite(Variable v)
	{
		int f = v.getFlags();
		f &= (~TO_DELETE);
		f |= TO_WRITE;
		v.setFlags(f);
	}

	/**
	 * Sets the TO_DELETE flag so that this variable will be deleted
	 * from the database after all computations are finished.
	 * @param v the variable
	 */
	public static void setToDelete(Variable v)
	{
		int f = v.getFlags();
		f &= (~TO_WRITE);
		f |= TO_DELETE;
		v.setFlags(f);
	}

	/**
	 * Clears the TO_WRITE flag after writing is accomplished.
	 * @param v the variable
	 */
	public static void clearToWrite(Variable v)
	{
		int f = v.getFlags();
		f &= (~TO_WRITE);
		v.setFlags(f);
	}

	/**
	 * Clears the TO_DELETE flag from this variable. 
	 * Call after it has been deleted.
	 * @param v the variable
	 */
	public static void clearToDelete(Variable v)
	{
		int f = v.getFlags();
		f &= (~TO_DELETE);
		f |= DB_DELETED;
		v.setFlags(f);
	}
	
	/**
	 * @return true if the variable flags indicate write, but don't overwrite.
	 */
	public static boolean isNoOverwrite(Variable v)
	{
		return (v.getFlags() & NO_OVERWRITE) == NO_OVERWRITE;
	}
	
	/**
	 * Sets the no-overwrite flag. This means write the variable to theDB
	 * but only if there is no current value at that time-slice.
	 */
	public static void setNoOverwrite(Variable v)
	{
		int f = v.getFlags();
		f |= NO_OVERWRITE;
		v.setFlags(f);
	}
	
	/**
	 * Clears the non-reserved bits in the passed variable's flags.
	 * @param v
	 */
	public static void clearNonReserved(Variable v)
	{
		v.setFlags(v.getFlags() & (IFlags.RESERVED_MASK | RESERVED_4_COMP));
	}
}
