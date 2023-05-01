/*
*  $Id$
*/
package ilex.var;

import java.util.*;
import ilex.var.IFlags;
import ilex.var.NamedVariable;

/**
* NamedVariableList is a collection of named variables.
*/
public class NamedVariableList implements IFlags
{
	private Vector<NamedVariable> nvars;
	private int flags;

	/**
	* Constructs a new empty named variable list with a zero flag value.
	*/
	public NamedVariableList( )
	{
		nvars = new Vector<NamedVariable>();
		flags = 0;
	}

	/**
	* Constructs a new empty named variable list with a specific flag value.
	* @param flags
	*/
	public NamedVariableList( int flags )
	{
		this();
		this.flags = flags;
	}

	//======== Methods for getting/setting/checking the flags ===========

	/**
	* Gets the list flags value.
	* These are flags on the list as a whole. Each NamedVariable in the
	* list also has its own flags value.
	* @return
	*/
	public int getFlags( )
	{
		return flags;
	}

	/**
	* Sets the list flags value.
	* These are flags on the list as a whole. Each NamedVariable in the
	* list also has its own flags value.
	* @param f
	*/
	public void setFlags( int f )
	{
		flags = f;
	}

	/**
	* Returns true if this list has been changed.
	* A list has been changed if a variable has been added or removed, or
	* if any variable in the list has had its value modified.
	* @return
	*/
	public boolean isChanged( )
	{
		if ((flags & IFlags.IS_CHANGED)!=0)
			return true;
		for(int i = 0; i<nvars.size(); i++)
		{
			NamedVariable nv = nvars.elementAt(i);
			if (nv.isChanged())
				return true;
		}
		return false;
	}

	/**
	* Resets the flags in this list and named variables so we can detect
	* future modifications.
	*/
	public void resetChanged( ) 
	{
		flags &= (~IFlags.IS_CHANGED); 
		for(int i = 0; i<nvars.size(); i++)
		{
			NamedVariable nv = nvars.elementAt(i);
			nv.resetChanged();
		}
	}

	/**
	* Sets the internal flag so that subsequent calls to isChanged return
	* true.
	* It is rarely necessary to call this method directly. Changes by adding,
	* removing, and modifying variables are tracked automatically.
	*/
	public void setChanged( ) { flags |= IFlags.IS_CHANGED; }


	//========== Methods for adding and removing component variables =====

	/**
	* Adds or replaces a named variable to this list.
	* The named variable is not copied before it is placed in the list.
	* Subsequent changes to the named variable will be reflected in the
	* list. To shield the list copy, call add(new NamedVariable(nv));
	* @param nv
	*/
	public void add( NamedVariable nv )
	{
		rmByNameIgnoreCase(nv.getName());
		nvars.add(nv);
		setChanged();
	}

	/**
	* Removes a named variable from the list.
	* The variable passed to this method should have previously been
	* retrieved by one of the find method. This method searches for
	* a copy of the passed object in the list, and removes it.
	* @param nv
	*/
	public void rm( NamedVariable nv )
	{
		for(int i = 0; i<nvars.size(); i++)
			if (nv == nvars.elementAt(i))
			{
				nvars.remove(i);
				setChanged();
				return;
			}
	}

	/**
	* Removes a variable from the list by its index.
	* @param i
	* @throws ArrayIndexOutOfBounds if the index is invalid.
	*/
	public void rm( int i )
	{
		nvars.remove(i);
		setChanged();
	}

	/**
	* Removes all variables from the list with a matching name.
	* Comparisons are case-sensitive. See also rmByNameIgnoreCase().
	* @param name
	*/
	public void rmByName( String name )
	{
		int idx;
		while((idx = getIndexByName(name)) != -1)
		{
			nvars.remove(idx);
			setChanged();
		}
	}

	/**
	* Removes all variables from the list with a matching name.
	* Comparisons are case-insensitive. See also rmByName().
	* @param name
	*/
	public void rmByNameIgnoreCase( String name )
	{
		int idx;
		while((idx = getIndexByNameIgnoreCase(name)) != -1)
		{
			nvars.remove(idx);
			setChanged();
		}
	}


	//============ Methods for searching the list by name ================

	/**
	* Returns the index of the first variable in the list with a matching
	* name.
	* If no match is found, returns -1.
	* @param name
	* @return
	*/
	public int getIndexByName( String name )
	{
		for(int i = 0; i<nvars.size(); i++)
			if (name.equals( nvars.elementAt(i).getName()))
				return i;
		return -1;
	}

	/**
	* Returns the index of the first variable in the list with a matching
	* name. A case-insensitive string compare is used.
	* If no match is found, returns -1.
	* @param name
	* @return
	*/
	public int getIndexByNameIgnoreCase( String name )
	{
		for(int i = 0; i<nvars.size(); i++)
			if (name.equalsIgnoreCase( 
				nvars.elementAt(i).getName()))
				return i;
		return -1;
	}

	/**
	* Finds a named variable in the list by its name.
	* Returns null if no such variable is found.
	* @param name
	* @return
	*/
	public NamedVariable findByName( String name )
	{
		int idx = getIndexByName(name);
		if (idx != -1)
			return nvars.elementAt(idx);
		return null;
	}

	/**
	* Finds a named variable in the list by its name. A case-insensitive
	* compare is used.
	* Returns null if no such variable is found.
	* @param name
	* @return
	*/
	public NamedVariable findByNameIgnoreCase( String name )
	{
		int idx = getIndexByNameIgnoreCase(name);
		if (idx != -1)
			return nvars.elementAt(idx);
		return null;
	}


	/**
	* Prints out each variable in the form name=value. The flag values
	* are not printed.
	* @return
	*/
	public String toString( )
	{
		if (nvars.size() == 0)
			return "";
		StringBuffer buf = new StringBuffer();
		buf.append(nvars.elementAt(0).toString());

		for(int i=1; i<nvars.size(); i++)
		{
			buf.append(", ");
			buf.append(nvars.elementAt(i).toString());
		}

		return buf.toString();
	}

	/** Remove all variables from the list. */
	public void clear()
	{
		nvars.clear();
		flags = 0;
	}
}

