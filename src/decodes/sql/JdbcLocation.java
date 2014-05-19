/*
 * $Id$
 *
 * $State$
 *
 * $Log$
 * Revision 1.1  2008/04/04 18:21:04  cvs
 * Added legacy code to repository
 *
 * Revision 1.2  2004/09/02 12:15:27  mjmaloney
 * javadoc
 *
 * Revision 1.1  2002/08/29 05:48:50  chris
 * Added RCS keyword headers.
 *
 *
 */

package decodes.sql;
import ilex.util.TextUtil;
import java.sql.SQLException;

/**
* @deprecated DO NOT USE THIS CLASS.
* This class encapsulates a string that combines a JDBC URL with a
* username, as expected in the DECODES databaseLocation properties.
* The normal form is, for example,
* 'jdbc:postgresql:decodessample:testuser'.
* <p>
*   Note:  this normalizes the entire string to lowercase;  I'm not sure
*   that's correct.  But I was having trouble when the database name
*   was mixed case.
* </p>
*/
public class JdbcLocation
{
  // These are the parts of the JDBC URL
  // For example, 'jdbc:postgresql:decodessample:testuser'.

    /** This should always be 'jdbc', I think  */

      String _jdbc;

    /** For example, 'postgresql'  */

      String _serverName;

    /** For example, 'decodessample'  */

      String _dbName;

    /** The PostgreSQL user name  */

      String _user;


  /**
   * Construct from a String.
   * The String should be of the correct form, with four parts
   * separated by colons.
   */

    public JdbcLocation(String s)
        throws SQLException
    {
        // Normalize to lower-case (see the class comment above)
        s = s.toLowerCase();

        // Chop up the string

        String[] parts = TextUtil.split(s, ':');
        if (parts.length != 4)
            throw new SQLException("Invalid JDBC database location " +
                "string:  '" + s + "'");

        _jdbc = parts[0];
        _serverName = parts[1];
        _dbName = parts[2];
        _user = parts[3];
    }

  /** Copy constructor.  */

    public JdbcLocation(JdbcLocation src)
    {
        _jdbc = src._jdbc;
        _serverName = src._serverName;
        _dbName = src._dbName;
        _user = src._user;
    }


  // Getter methods

    /** Get just the server name portion, with no colons.  */

      public String getServerName() {
          return _serverName;
      }

    /** Get just the database name portion, with no colons.  */

      public String getDbName() {
          return _dbName;
      }

    /** Get just the username portion, with no colons.  */

      public String getUser() {
          return _user;
      }

    /**
     * This returns just the first three parts as a colon-delimited
     * string, suitable for passing to the JDBC
     * DriverManager.getConnection() method.  This is called, in those
     * Javadocs, the "URL".
     */

      public String getUrl()
      {
          return _jdbc + ':' + _serverName + ':' + _dbName;
      }



  // Setter methods

    /**
     * Set the server name.
     * This normalizes it to lowercase.
     */

      public void setServerName(String sn) {
          _serverName = sn.toLowerCase();
      }

    /**
     * Set the database name.
     * This normalizes it to lowercase.
     */

      public void setDbName(String n)
      {
          _dbName = n.toLowerCase();
      }

    /**
     * Set the user name.
     * This normalizes it to lowercase.
     */

      public void setUser(String un)
      {
          _user = un.toLowerCase();
      }



  // Other methods

    /** Convert to a standard, colon-delimited, four-part string.  */

      public String toString()
      {
          return _jdbc + ':' + _serverName + ':' +
                 _dbName + ':' + _user;
      }

}
