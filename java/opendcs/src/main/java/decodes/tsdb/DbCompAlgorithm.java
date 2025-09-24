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
package decodes.tsdb;

import java.util.Collection;
import java.util.HashMap;
import java.util.Properties;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;

import opendcs.dao.CachableHasProperties;
import ilex.util.PropertiesUtil;
import ilex.util.TextUtil;
import decodes.db.Constants;
import decodes.sql.DbKey;
import decodes.tsdb.xml.CompXioTags;

/**
This data structure class holds the meta-data for an algorithm.
*/
public class DbCompAlgorithm implements CompMetaData, CachableHasProperties
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

	private HashMap<ScriptType, DbCompAlgorithmScript> algoScripts =
		new HashMap<ScriptType, DbCompAlgorithmScript>();


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

		for(DbCompAlgorithmScript script : getScripts())
			dca.putScript(script.copy(dca));

		return dca;
	}

	/**
	 * For use by the editor to detect changes, return true if the passed
	 * object is equal to this one.
	 */
	public boolean equalsNoId(DbCompAlgorithm rhs)
	{
		// Oracle treats empty strings as nulls. Prevent bogus "Are You Sure"
		// messages when comment is empty and user hasn't changed anything.
		if (rhs.comment != null && rhs.comment.length() == 0 && comment == null)
			rhs.comment = null;

		if (!TextUtil.strEqual(name, rhs.name)
		 || !TextUtil.strEqual(execClass, rhs.execClass)
		 || !TextUtil.strEqual(comment, rhs.comment))
		{
			return false;
		}
		if (!PropertiesUtil.propertiesEqual(props, rhs.props))
		{
			return false;
		}

		if (parms.size() != rhs.parms.size())
		{
			return false;
		}
		for(int i=0; i<parms.size(); i++)
		{
			DbAlgoParm p1 = parms.get(i);
			DbAlgoParm p2 = rhs.parms.get(i);
			if (!TextUtil.strEqual(p1.getRoleName(), p2.getRoleName())
			 || !TextUtil.strEqual(p1.getParmType(), p2.getParmType()))
			{
				return false;
			}
		}
		if (!this.algoScripts.equals(rhs.algoScripts))
		{
			return false;
		}
		return true;
	}


	@Override
	public DbKey getKey()
	{
		return algorithmId;
	}


	@Override
	public String getUniqueName()
	{
		return name;
	}

	/**
	 * @param scriptType
	 * @return the script for the given type or null if none is defined.
	 */
	public DbCompAlgorithmScript getScript(ScriptType scriptType)
	{
		return algoScripts.get(scriptType);
	}

	/**
	 * Add or replace the script of a given type.
	 * @param script
	 */
	public void putScript(DbCompAlgorithmScript script)
	{
		algoScripts.put(script.getScriptType(), script);
	}

	public void clearScripts()
	{
		algoScripts.clear();
	}

	public Collection<DbCompAlgorithmScript> getScripts()
	{
		return algoScripts.values();
	}
}
