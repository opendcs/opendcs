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
*  For more information contact: tempest@sutron.com
*  
*  $Log$
*  Revision 1.2  2013/03/21 18:27:39  mmaloney
*  DbKey Implementation
*
*/
package decodes.tsdb;

import java.util.Properties;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;

import ilex.util.HasProperties;
import ilex.util.PropertiesUtil;

import ilex.util.TextUtil;
import decodes.db.Constants;
import decodes.sql.DbKey;
import decodes.tsdb.xml.CompXioTags;

/**
This data structure class holds the meta-data for an algorithm.
*/
public class DbCompAlgorithm
	implements CompMetaData, HasProperties
{
	/** Surrogate key for this algorithm in the time series database.  */
	private DbKey algorithmId;

	/** Name of this algorithm */
	private String name;

	/** Fully qualified Java class name to execut this algorithm. */
	private String execClass;

	/** Free form multi-line comment */
	private String comment;

	/** Properties associated with this algorithm. */
	private Properties props;
	
	/** parameters to this algorithm */
	private ArrayList<DbAlgoParm> parms;

	/** For use in the editor -- the number of computations using this algo. */
	private int numCompsUsing;

	/** 
	 * Constructor. 
	 * @param id surrogate database key for this record.
	 * @param name unique name for this algorithm (no embedded blanks).
	 * @param execClass full Java class name
	 * @param comment comment
	 */
	public DbCompAlgorithm(DbKey id, String name, String execClass, 
		String comment)
	{
		this.algorithmId = id;
		this.name = name;
		this.execClass = execClass;
		this.comment = comment;
		props = new Properties();
		parms = new ArrayList<DbAlgoParm>();
		numCompsUsing = 0;
	}


	/**
	 * For use by editor.
	 */
	public DbCompAlgorithm(String name)
	{
		this(Constants.undefinedId, name, null, null);
	}

	/**
	 * Retrieves an algorithm parameter by its role-name.
	 * Note: Role names are not case sensitive.
	 * @param role the role name
	 * @return the DbAlgoParm object or null if no matching role.
	 */
	public DbAlgoParm getParm(String role)
	{
		for(DbAlgoParm ret : parms)
		{
			if (role.equalsIgnoreCase(ret.getRoleName()))
				return ret;
		}
		return null;
	}

	/**
	 * Adds (or replaces) a parameter to this algorithm. 
	 * If the name already exists, the old parameter's contents are overwritten.
	 * @param parm the parameter
	 */
	public void addParm(DbAlgoParm parm)
	{
		DbAlgoParm old = getParm(parm.getRoleName());
		if (old != null)
			old.setParmType(parm.getParmType());
		else
			parms.add(parm);
	}

	public void clearParms()
	{
		parms.clear();
	}

	/** Sets the algorithm ID. */
	public void setId(DbKey id) { algorithmId = id; }

	/** @return the algorithm ID. */
	public DbKey getId() { return algorithmId; }

	/** @return the algorithm name. */
	public String getName() { return name; }

	/** Sets algorithm name. */
	public void setName(String name) { this.name = name; }

	/** @return the algorithm executive class name. */
	public String getExecClass() { return execClass; }

	/** @return the algorithm comment. */
	public String getComment() { return comment; }

	/** Set the algorithm executive class name. */
	public void setExecClass(String ec) { execClass = ec; }

	/** Set the algorithm comment. */
	public void setComment(String cm) { comment = cm; }

	/** 
	 * Adds a property to this algorithm's meta-data.
	 * @param name the property name.
	 * @param value the property value.
	 */
	public void setProperty(String name, String value)
	{
		props.setProperty(name, value);
	}

	/**
	 * Retrieve a property by name.
	 * @param name the property name.
	 * @return value of name property, or null if not defined.
	 */
	public String getProperty(String name)
	{
		String ret = PropertiesUtil.getIgnoreCase(props, name);
		return ret;
	}

	/**
	 * Removes a property assignment.
	 * @param name the property name.
	 */
	public void rmProperty(String name)
	{
		PropertiesUtil.rmIgnoreCase(props, name);
	}

	/**
	 * @return enumeration of all names in the property set.
	 */
	public Enumeration getPropertyNames()
	{
		return props.propertyNames();
	}

	/**
	 * @return the entire properties set.
	 */
	public Properties getProperties()
	{
		return props;
	}

	/**
	 * @return an iterator into the list of parameters.
	 */
	public Iterator<DbAlgoParm> getParms()
	{
		return parms.listIterator();
	}

	/** @see decodes.tsdb.CompMetaData */
	public String getObjectType() { return CompXioTags.algorithm; }

	/** @see decodes.tsdb.CompMetaData */
	public String getObjectName() { return name; }

	public int getNumCompsUsing() { return numCompsUsing; }

	public void setNumCompsUsing(int ncu) { numCompsUsing = ncu; }

	/**
	 * For use by the editor, create a copy of this object, but with no ID.
	 */
	public DbCompAlgorithm copyNoId()
	{
		DbCompAlgorithm dca = new DbCompAlgorithm(Constants.undefinedId, 
			name, execClass, comment);

		PropertiesUtil.copyProps(dca.props, this.props);
		for(DbAlgoParm parm : this.parms)
			dca.addParm(new DbAlgoParm(parm.getRoleName(), parm.getParmType()));

		return dca;
	}

	/**
	 * For use by the editor to detect changes, return true if the passed
	 * object is equal to this one.
	 */
	public boolean equalsNoId(DbCompAlgorithm rhs)
	{
		if (!TextUtil.strEqual(name, rhs.name)
		 || !TextUtil.strEqual(execClass, rhs.execClass)
		 || !TextUtil.strEqual(comment, rhs.comment)
		 || !PropertiesUtil.propertiesEqual(props, rhs.props))
			return false;

		if (parms.size() != rhs.parms.size())
			return false;
		for(int i=0; i<parms.size(); i++)
		{
			DbAlgoParm p1 = parms.get(i);
			DbAlgoParm p2 = rhs.parms.get(i);
			if (!TextUtil.strEqual(p1.getRoleName(), p2.getRoleName())
			 || !TextUtil.strEqual(p1.getParmType(), p2.getParmType()))
				return false;
		}
		return true;
	}
}
