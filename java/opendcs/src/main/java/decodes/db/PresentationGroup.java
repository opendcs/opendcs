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
package decodes.db;

import decodes.decoder.*;
import decodes.util.DecodesSettings;

import java.util.Date;
import java.util.Vector;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import java.util.Iterator;

import ilex.util.TextUtil;

/**
* A PresentationGroup is a named container of DataPresentation objects.
* It "owns" the DataPresentation objects, in that when the PresentationGroup
* is deleted from the database, they are also deleted.
*/
public class PresentationGroup extends IdDatabaseObject
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();

	/**
	* The name of this PresentationGroup.  Note that this is unique
	* among all the PresentationGroups in this database.
	*/
	public String groupName;

	/**
	* This is the name of this PresentationGroup's "parent", if there is
	* one.  If not, this will be null.
	*/
	public String inheritsFrom;

	/** Last time this object was modified. */
	public Date lastModifyTime;

	/** True means this object is read to be put in the production database. */
	public boolean isProduction;

	/**
	* This is the list of DataPresentation objects in this group.
	*/
	public Vector<DataPresentation> dataPresentations = new Vector<DataPresentation>();

	/**
	* This is a reference to this PresentationGroup's parent, if any.
	* This is the group whose name is stored in the inheritsFrom member.
	*/
	public PresentationGroup parent;

	/** internal transient flag to prevent infinite recursion. */
	private boolean preparing;

	/** Default DP */
	DataPresentation defaultPresentation = null;

	private boolean _wasRead = false;
	public boolean wasRead() { return _wasRead; }

	/**
	* Constructor.
	*/
	public PresentationGroup()
	{
		this(null, Database.getDb());
	}

	/**
	  Constructor with a name.
	  @param name the name of the group
	*/
	public PresentationGroup(String name)
	{
		this(name, Database.getDb());
	}

	public PresentationGroup(Database db)
	{
		this(null, db);
	}

	public PresentationGroup(String name, Database db)
	{
		super(db);
		clear();
		groupName = name;
		defaultPresentation = null;
	}

	/**
	  Zeros out all attributes as if object was newly created.
	*/
	public void clear()
	{
		inheritsFrom = null;
		lastModifyTime = null;
		isProduction = false;
		parent = null;
		clearList();
		preparing = false;
	}

	public void clearList()
	{
		dataPresentations.clear();
	}

	/**
	  Makes a string containing the suitable for use as a filename.
	  @return a string suitable as a file name for this group
	*/
	public String makeFileName()
	{
		StringBuffer ret = new StringBuffer(groupName);
		for(int i=0; i<ret.length(); i++)
			if (Character.isWhitespace(ret.charAt(i)))
				ret.setCharAt(i, '-');
		return ret.toString();
	}

	/**
	  Overrides the DatabaseObject method; this returns "PresentationGroup".
	  @return "PresentationGroup"
	*/
	public String getObjectType() {
		return "PresentationGroup";
	}

	/**
	  Adds a DataPresentation element to this group.
	  @param dp the DataPresentation to add
	*/
	public void addDataPresentation(DataPresentation dp)
	{
		// MJM If there is a duplicate, remove it first.
		int n = dataPresentations.size();
		for(int i = 0; i<n; i++)
		{
			DataPresentation edp = dataPresentations.get(i);

			if (edp.getDataType() == dp.getDataType())
			{
				dataPresentations.remove(i);
				break;
			}
		}
		dataPresentations.add(dp);
	}

	/**
	  Makes a deep copy of this presentation group.
	  @return the copy
	*/
	public PresentationGroup copy()
	{
		PresentationGroup ret = new PresentationGroup(groupName);
		try { ret.setId(getId()); }
		catch(DatabaseException ex)
		{
			// won't happen.
			throw new RuntimeException("Unable to set object id in a location where that should always work.", ex);
		}

		ret.inheritsFrom = this.inheritsFrom;
		ret.lastModifyTime = new Date();
		ret.isProduction = this.isProduction;

		for(Iterator<DataPresentation> it = dataPresentations.iterator(); it.hasNext(); )
		{
			DataPresentation dp = it.next();
			ret.addDataPresentation(dp.copy(ret));
		}

		ret.parent = this.parent;
		return ret;
	}

	/**
	  Makes a deep copy but with a null database ID.
	  @return the copy
	*/
	public PresentationGroup noIdCopy()
	{
		PresentationGroup ret = copy();
		ret.clearId();
		for(Iterator<DataPresentation> it = ret.dataPresentations.iterator(); it.hasNext(); )
		{
			DataPresentation dp = (DataPresentation)it.next();
			dp.clearId();
		}
		return ret;
	}

	/**
	* Compares two PresentationGroups.  They are considered equal if the
	* groupName and inheritsFrom members match, and if, for every
	* DataPresentation in this group's list, there is a matching
	* DataPresentation in the other group's list.
	*/
	public boolean equals(Object rhs)
	{
		if (!(rhs instanceof PresentationGroup))
			return false;
		PresentationGroup pg = (PresentationGroup)rhs;

		// Check for identity
		if (this == pg) return true;

		if (!groupName.equals(pg.groupName)
			|| isProduction != pg.isProduction)
			return false;
		if (!TextUtil.strEqual(inheritsFrom, pg.inheritsFrom))
			return false;

		if (dataPresentations.size() != pg.dataPresentations.size())
			return false;
		for(Iterator<DataPresentation> it = dataPresentations.iterator(); it.hasNext(); )
		{
			DataPresentation dp = it.next();
			Iterator<DataPresentation> pgit;
			boolean matchFound = false;
			for(pgit = pg.dataPresentations.iterator();
				!matchFound && pgit.hasNext(); )
			{
				DataPresentation pgdp = pgit.next();
				matchFound = dp.equals(pgdp);
			}
			if (!matchFound)
				return false;
		}
		return true;
	}

	/**
	* @return the number of DataPresentations in this group, not including
	* any DataPresentations in this group's ancestors, if any.
	*/
	public int size() {
		return dataPresentations.size();
	}

	/** @return an Iterator into collection of DataPresentation objects. */
	public Iterator<DataPresentation> iterator()
	{
		return dataPresentations.iterator();
	}

	/**
	* This overrides the DatabaseObject method.
	*/
	public void prepareForExec()
		throws InvalidDatabaseException
	{
		if (preparing)
		{
			throw new InvalidDatabaseException("Circular reference to presentation group '" + groupName + "'");
		}
		try
		{
			if (!wasRead())
				read();


			if (inheritsFrom == null || inheritsFrom.length() == 0)
				parent = null;
			else
			{
				// Get & recursively prepare parent(s).
				parent = myDatabase.presentationGroupList.find(inheritsFrom);
				parent.prepareForExec();

			}
		}
		catch(DatabaseException ex)
		{
			log.atError().setCause(ex).log("Cannot read presentation group {}", getDisplayName());
		}
		finally
		{
			preparing = false;
		}
	}

	/**
	* Overrides the DatabaseObject method.
	*/
	public boolean isPrepared()
	{
		return false;
	}

	/**
	* Overrides the DatabaseObject method.
	*/
	public void validate()
		throws IncompleteDatabaseException, InvalidDatabaseException
	{
	}

	/**
	* This overrides the DatabaseObject method.  This reads this
	* PresentationGroup from the database.
	*/
	public void read()
		throws DatabaseException
	{
		myDatabase.getDbIo().readPresentationGroup(this);
		_wasRead = true;
	}

	/**
	* This overrides the DatabaseObject method.  This writes the
	* PresentationGroup back out to the database.
	*/
	public void write()
		throws DatabaseException
	{
		lastModifyTime = new Date();
		preparing = true;
		if (parent != null && !parent.preparing)
			parent.write();
		myDatabase.getDbIo().writePresentationGroup(this);
		preparing = false;
	}

	/**
	* @return a DataPresentation element by data type,
	* or null if not found.
	*/
	public DataPresentation findDataPresentation(DataType dt)
	{
		if (dt != null)
		{
			for (DataPresentation dp : dataPresentations)
				if (dp.getDataType().equals(dt))
					return dp;

			// MJM OpenDCS 6.5 For CWMS, try to match Base Param only.
			int idx = -1;
			if (dt.getStandard().equalsIgnoreCase(Constants.datatype_CWMS)
			 && (idx = dt.getCode().indexOf('-')) > 0)
			{
				DataType paseParam = DataType.getDataType(Constants.datatype_CWMS, dt.getCode().substring(0, idx));
				for (DataPresentation dp : dataPresentations)
					if (dp.getDataType().equals(paseParam))
						return dp;
			}

			// Recursively search parent groups.
			if (parent != null)
				return parent.findDataPresentation(dt);
		}

		// No luck, use overall default.
		if (defaultPresentation == null)
			initDefaultPresentation();
		return defaultPresentation;
	}

	public DataPresentation findDataPresentation(Sensor sensor)
	{
		for (DataPresentation dp : dataPresentations)
		{
			for(Iterator<DataType> dtit = sensor.getAllDataTypes(); dtit.hasNext(); )
			{
				DataType dt = dtit.next();
				if (dt == null)
					continue;
				if (dp.getDataType().equals(dt))
					return dp;
			}
		}

		// MJM OpenDCS 6.5 For CWMS, try to match Base Param only.
		int idx = -1;
		DataType dt = sensor.getDataType(Constants.datatype_CWMS);
		if (dt != null && (idx = dt.getCode().indexOf('-')) > 0)
		{
			DataType paseParam = DataType.getDataType(Constants.datatype_CWMS, dt.getCode().substring(0, idx));
			for (DataPresentation dp : dataPresentations)
				if (dp.getDataType().equals(paseParam))
					return dp;
		}

		if (parent != null)
			return parent.findDataPresentation(sensor);

		if (defaultPresentation == null)
			initDefaultPresentation();
		return defaultPresentation;
	}

	/**
	  Initializes a default presentation used when there is none defined
	  in the database.
	  Reasonable rounding rules are implemented.
	*/
	private void initDefaultPresentation()
	{
		defaultPresentation = new DataPresentation();
		defaultPresentation.setMaxDecimals(DecodesSettings.instance().defaultMaxDecimals);

		defaultPresentation.prepareForExec();
	}

	public String getDisplayName()
	{
		return groupName;
	}
}
