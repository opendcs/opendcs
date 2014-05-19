/*
*  $Id$
*
*  $Source$
*
*  $State$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:21:08  cvs
*  Added legacy code to repository
*
*  Revision 1.1  2000/12/18 02:59:20  mike
*  *** empty log message ***
*
*
*/
package ilex.db;

import java.util.Vector;
import java.util.Enumeration;
import ilex.util.StringPair;

/**
This class represents a single table in a hierarchy of tables within a
database view.
*/
public class DatabaseViewTable
{
	private String tableName;     // Immutable table name
	private Vector masterKeys;    // StringPairs relating master keys to mine
	private Vector detailKeys;    // String key-column names
	private Vector exportKeys;    // String values to export as keys in XML
	private Vector exportSuppress; // String column names to exclude from XML
	private Vector nameSubstitute; // StringPairs: DB columns <> XML names
	private boolean exportElide;  // If true, do not output this table to XML.

	private Vector detailTables; // lower-level tables in the hierarchy.

	public DatabaseViewTable(String tableName)
	{
		this.tableName = tableName;
		masterKeys = new Vector();
		detailKeys = new Vector();
		exportKeys = new Vector();
		exportSuppress = new Vector();
		nameSubstitute = new Vector();
		exportElide = false;
		detailTables = new Vector();
	}

	public String getTableName()
	{
		return tableName;
	}

	/**
	  Returns the set of master-detail key relationships as an enumeration.
	  Each element in the enumeration is of type ilex.util.StringPair. The
	  first string is the name of a column in this table's master. The
	  second string is the name of a column in this (detail) table. When
	  querying the database and constructing the record hierarchy, the
	  value of the specified master column should equal the value of the
	  specified column in this table.
	*/
	public Enumeration getMasterKeys()
	{
		return masterKeys.elements();
	}

	public void addMasterKey(String masterColumn, String thisColumn)
	{
		masterKeys.add(new StringPair(masterColumn, thisColumn));
	}

	/**
	  Returns the set of column names that are considered keys to this table
	  in the database.
	*/
	public Enumeration getDetailKeys()
	{
		return detailKeys.elements();
	}

	public void addDetailKey(String column)
	{
		detailKeys.add(column);
	}

	/**
	  Returns the set of column names that are exported as keys to an XML
	  file. These are in addition to the detailKeys.
	  <p>
	  In the XML file, key values are written as attributes to the element,
	  whereas non-key fields are written as subordinate elements within the
	  body.
	*/
	public Enumeration getExportKeys()
	{
		return exportKeys.elements();
	}

	public void addExportKey(String column)
	{
		exportKeys.add(column);
	}

	/**
	  Returns the set of column names that are to be suppressed in the XML
	  output file.
	*/
	public Enumeration getExportSuppress()
	{
		return exportSuppress.elements();
	}

	public void addExportSuppress(String column)
	{
		exportSuppress.add(column);
	}

	/**
	  Returns the set of StringPairs representing name substitutions for
	  the XML output file.
	  The first String in each pair is the database column name. The second
	  string is to be used as an XML element tag when outputting a value
	  for this column.
	*/
	public Enumeration getNameSubsitute()
	{
		return nameSubstitute.elements();
	}

	public void addNameSubstitute(String dbColumn, String xmlTag)
	{
		nameSubstitute.add(new StringPair(dbColumn, xmlTag));
	}

	public boolean getExportElide() { return exportElide; }

	public void setExportElide(boolean tf) { exportElide = tf; }

	/**
	  Returns the set of detail tables.
	  Each element in the enumeration is of type DatabaseViewTable. These
	  represent detail tables to this (master) table.
	*/
	public Enumeration getDetails()
	{
		return detailTables.elements();
	}

	public void addDetail(DatabaseViewTable dbvt)
	{
		detailTables.add(dbvt);
	}

	public boolean hasDetails()
	{
		return detailTables.size() > 0;
	}

}

