/*
 * $Id$
 *
 * Open source software
 *
 * $Log$
 * Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
 * OPENDCS 6.0 Initial Checkin
 *
 * Revision 1.4  2013/03/21 18:27:39  mmaloney
 * DbKey Implementation
 *
 * Revision 1.3  2010/12/23 18:23:36  mmaloney
 * udpated for groups
 *
 * Revision 1.2  2010/10/29 15:14:55  mmaloney
 * Guard against null format statement.
 *
 * Revision 1.1  2008/04/04 18:21:04  cvs
 * Added legacy code to repository
 *
 * Revision 1.5  2007/04/25 13:25:06  ddschwit
 * Changed SELECT * to SELECT columns
 *
 * Revision 1.4  2004/09/02 12:15:27  mjmaloney
 * javadoc
 *
 * Revision 1.3  2002/12/18 17:57:33  mjmaloney
 * Don't abort on format statement error, print message and go on.
 *
 * Revision 1.2  2002/09/20 12:59:07  mjmaloney
 * SQL Dev.
 *
 * Revision 1.1  2002/08/28 13:11:36  chris
 * SQL database I/O development.
 */

package decodes.sql;

import ilex.util.Logger;

import decodes.db.DatabaseException;
import decodes.db.DecodesScript;
import decodes.db.FormatStatement;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Vector;


/**
 * This class takes care of reading and writing the SQL database
 * FormatStatement table.
 */
public class FormatStatementIO extends SqlDbObjIo
{
  
	/**
	  Constructor. 
	  @param dbio the SqlDatabaseIO to which this IO object belongs
	*/
	FormatStatementIO(SqlDatabaseIO dbio)
	{
		super(dbio);
	}


	/**
	* Reads all the FormatStatements for a particular DecodesScript.
	* The DecodesScript must have already had its SQL database ID set.
	* Note that we aren't guaranteed that these FormatStatements haven't
	* already been read.
	* @param ds the DecodesScript
	*/
	public void readFormatStatements(DecodesScript ds)
		throws DatabaseException, SQLException
	{
		Statement stmt = createStatement();
		ResultSet rs = stmt.executeQuery(
			"SELECT decodesScriptId, sequenceNum, " +
			"label, format " +
			"FROM FormatStatement " +
			"WHERE DecodesScriptId = " + ds.getId() + " " +
			"ORDER BY SequenceNum"
		);

		if (rs != null) {
			while (rs.next()) {
				int seqNum = rs.getInt(2);
				String label = rs.getString(3);
				String format = rs.getString(4);
				if (format == null) format = "";

				FormatStatement fmt = new FormatStatement(ds, seqNum);
				fmt.label = label;
				fmt.format = format;

				ds.formatStatements.add(fmt);
			}
		}

		stmt.close();
	}


	/**
	* Insert all the FormatStatements associated with a particular
	* DecodesScript.  The DecodesScript must have already had its
	* SQL database IO set.
	* @param ds the DecodesScript
	*/
	public void insertFormatStatements(DecodesScript ds)
		throws DatabaseException, SQLException
	{

		Vector<FormatStatement> v = ds.formatStatements;
		for (int i = 0; i < v.size(); ++i) 
		{
			FormatStatement fs = v.get(i);
			if (fs.label == null || fs.label.trim().length() == 0)
			{
				if (fs.format == null || fs.format.trim().length() == 0)
					continue; // ignore empty format statement.
				else
					fs.label = "nolabel_" + i;
			}
			try { insert(fs, ds.getId()); }
			catch(SQLException ex)
			{
				Logger.instance().log(Logger.E_FAILURE,
					"SQLException trying to insert format statement: " + ex);
			}
		}
	}

	/**
	* Insert a single FormatStatement into the SQL database, using
	* the DecodesScript ID provided as the second argument.
	* @param fs the FormatStatement
	* @param dsId the database ID of the DecodesScript
	*/
	public void insert(FormatStatement fs, DbKey dsId)
		throws DatabaseException, SQLException
	{
		String q = "INSERT INTO FormatStatement VALUES (" +
					 dsId + ", " +
					 fs.sequenceNum + ", " +
					 sqlReqString(fs.label) + ", " +
					 escapeString(fs.format) +
				   ")";
		executeUpdate(q);
	}

	/**
	* Delete all the FormatStatements associated with a particular
	* DecodesScript, if any.
	* This DecodesScript must have already had its SQL database ID set.
	* @param ds the DecodesScript
	*/
	public void deleteFormatStatements(DecodesScript ds)
		throws DatabaseException, SQLException
	{
		String q = "DELETE FROM FormatStatement " +
				   "WHERE DecodesScriptID = " + ds.getId();
		tryUpdate(q);
	}
}
