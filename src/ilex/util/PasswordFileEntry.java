/*
*  $Id$
*/
package ilex.util;

import java.security.*;
import java.util.Enumeration;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.NoSuchElementException;
import java.io.*;

/**
Represent a single entry in a PasswordFile.
*/
public class PasswordFileEntry 
	implements HasProperties, Cloneable, Serializable
{
	/** the user name */
	private String username;

	/** the roles assigned to this user */
	private String[] roles;

	/** SHA hash of the password */
	private byte[] ShaPassword;

	/** Additional properties of this entry */
	private Properties properties;

	//=================================================================
	// Constructors & Parsers
	//=================================================================

	/**
	* Constructs a PasswordFileEntry given complete arguments.
	* 
	* @param username the user name
	* @param roles the role names
	* @param ShaPassword the SHA password
	* @throws AuthException  if username is null or zero length.
	*/
	public PasswordFileEntry( String username, String[] roles, 
		byte[] ShaPassword ) 
		throws AuthException
	{
		this(username);
		this.roles = roles == null ? null : (String[])roles.clone();
		this.ShaPassword = (byte[])ShaPassword.clone();
	}

	/**
	* Constructs a password file entry with just the username assigned.
	* 
	* @param username the user name
	* @throws AuthException  if username is null or zero length.
	*/
	public PasswordFileEntry( String username ) throws AuthException
	{
		this();
		if (username == null || username.length() == 0)
			throw new AuthException("Username may not be blank.");
		this.username = username;
	}

	/**
	* Constructs a password file entry with username and entered password.
	* 
	* @param username the user name
	* @param password the password, the SHA hash will be stored.
	* @throws AuthException  if username is null or zero length.
	*/
	public PasswordFileEntry( String username, String password ) 
		throws AuthException
	{
		this(username);
		setPassword(password);
	}

	/**
	* Construct an empty password file entry.
	*/
	public PasswordFileEntry( )
	{
		this.username = null;
		roles = null;
		ShaPassword = null;
		properties = new Properties();
	}

	/**
	* Parses a line from a password file into this entry.
	* Lines are of the following format:
	* <pa>
	* username:role1,role2,...:sha-password:prop=value,prop=value,...
	* <pa>
	* Username, and password must be non-blank. At least one role must
	* be listed. The final field is a list of optional properties.
	* 
	* @param file_line the line from the file
	* @throws AuthException  if the line is improperly formatted.
	*/
	public void parseLine( String file_line ) 
		throws AuthException
	{
		StringTokenizer tokenizer = new StringTokenizer(file_line,":");
		if (tokenizer.countTokens() < 3)
			throw new AuthException("Only " + tokenizer.countTokens()
				+ " tokens, need at least 3 for username, roles, password");
		try
		{
			// Extract the tokens from the line.
			username = tokenizer.nextToken();
			String roles_str = tokenizer.nextToken();
			String passwd_str = tokenizer.nextToken();
			String prop_str = null;
			if (tokenizer.hasMoreTokens())
				prop_str = tokenizer.nextToken();

			// Special string 'none' for roles:
			if (roles_str.compareToIgnoreCase("none") == 0)
				roles = null;
			else
			{
				// Parse the individual roles.
				tokenizer = new StringTokenizer(roles_str, ",");
				int n = tokenizer.countTokens();
				roles = new String[n];
				for(int i = 0; i < n; i++)
					roles[i] = tokenizer.nextToken();
			}

			// Convert the hex password string to a byte array.
			ShaPassword = ByteUtil.fromHexString(passwd_str);

			if (prop_str != null)
				properties = PropertiesUtil.string2props(prop_str);
		}
		catch(NoSuchElementException e)
		{
			throw new AuthException(e.toString());
		}
	}

	/**
	* @return String formatted for storage in a password file
	* @see PasswordFileEntry.parseLine(String)
	*/
	public String toString( )
	{
		StringBuffer sb = new StringBuffer(username);
		sb.append(':');
		if (roles != null)
			for(int i = 0; i<roles.length; i++)
			{
				sb.append(roles[i]);
				if (i < roles.length-1)
					sb.append(',');
			}
		else
			sb.append("none");
		sb.append(':');
		if (ShaPassword != null)
			sb.append(ByteUtil.toHexString(ShaPassword));
		else // All zeros will never match anything but won't cause parse error.
			sb.append("0000000000000000000000000000000000000000");

		sb.append(":" + getPropertiesString());
		return sb.toString();
	}

	public String getPropertiesString()
	{
		StringBuilder sb = new StringBuilder();
		int n = 0;
		for(Enumeration en = properties.propertyNames(); en.hasMoreElements();)
		{
			String pname = (String)en.nextElement();
			if (n++ > 0)
				sb.append(',');
			sb.append(pname + "=" + properties.getProperty(pname));
		}
		return sb.toString();
	}

	/**
	* @return a deep-copy clone of this entry.
	* @throws CloneNotSupportedException
	*/
	public Object clone( ) throws CloneNotSupportedException
	{
		try
		{
			PasswordFileEntry pfe = new PasswordFileEntry(username);
			pfe.roles = roles == null ? null : (String[])roles.clone();
			pfe.ShaPassword = ShaPassword == null ? null :
				(byte[])ShaPassword.clone();
			return pfe;
		}
		catch(AuthException e)
		{
			throw new CloneNotSupportedException(e.toString());
		}
	}

	//=================================================================
	// Accessor methods:
	//=================================================================

	/**
	* @return the username component of this entry.
	*/
	public String getUsername( )
	{
		return username;
	}

	/**
	* @return the array of strings containing the roles assigned to this
	* entry (may be null).
	*/
	public String[] getRoles( )
	{
		return roles;
	}

	/**
	* Determines if a specific role is assigned to this entry.
	* @param role the role name
	* @return true if this entry has that role
	*/
	public boolean isRoleAssigned( String role )
	{
		if (roles == null)
			return false;
		for(int i=0; i<roles.length; i++)
			if (role.compareToIgnoreCase(roles[i]) == 0)
				return true;
		return false;
	}

	/**
	* Determines if an entered password matches the hash contained in this
	* entry.
	* @param passwd the password to check
	* @return true if SHA results match
	*/
	public boolean matchesPassword( String passwd )
	{
		byte test[] = buildShaPassword(username, passwd);
		if (ShaPassword.length != test.length)
			return false;
		for(int i = 0; i < test.length; i++)
			if (test[i] != ShaPassword[i])
				return false;
		return true;
	}

	/**
	* @return the hashed password for this entry.
	*/
	public byte[] getShaPassword( )
	{
		return ShaPassword;
	}

	//=================================================================
	// Modifier methods:
	//=================================================================

	// No method to modify username - this is illegal.

	/**
	* Assigns a new role to this entry.
	* @param role role name to add.
	*/
	public void assignRole( String role )
	{
		if (roles == null)
		{
			roles = new String[1];
			roles[0] = role;
		}
		else
		{
			String nroles[] = new String[roles.length+1];
			int i=0;
			for(; i<roles.length; i++)
				nroles[i] = roles[i];
			nroles[i] = role;
			roles = nroles;
		}
	}

	/**
	* Removes a role from this entry.
	* Returns true if the role was found, false if not.
	* @param role role name to remove
	* @return true if role was present.
	*/
	public boolean removeRole( String role )
	{
		if (roles == null)
			return false;
		for(int i=0; i<roles.length; i++)
			if (role.compareToIgnoreCase(roles[i]) == 0)
			{
				// 'i' is now the index of the element to be deleted.
				if (roles.length <= 1)
					roles = null;
				else
				{
					String nroles[] = new String[roles.length-1];
					int j=0;
					for(; j<i; j++)
						nroles[j] = roles[j];
					j++;   // 'j' now == 'i', skip it.
					for(; j<roles.length; j++)
						nroles[j-1] = roles[j];
					roles = nroles;
				}
				return true;
			}
		return false;
	}

	/**
	* Removes all roles from this entry.
	*/
	public void removeAllRoles( )
	{
		roles = null;
	}

	/**
	* Set the SHA password from the passed string. Note that the SHA
	* byte array will incorporate both the username and the passed password.
	* @param passwd the password to hash. Only the SHA result is stored.
	*/
	public void setPassword( String passwd )
	{
		ShaPassword = buildShaPassword(username, passwd);
	}

	/**
	* Explicitly set the SHA hash of the password.
	* @param pw the SHA hash of the password.
	*/
	public void setShaPassword( byte[] pw )
	{
		ShaPassword = pw;
	}

	/**
	* Builds a SHA hash from a user name and a password.
	* @param username the user name
	* @param password the password
	* @return the SHA hash
	*/
	private static final byte[] 
		buildShaPassword( String username, String password )
	{
		try
		{
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			MessageDigest md = MessageDigest.getInstance("SHA");
			DigestOutputStream dos = new DigestOutputStream(baos, md);

			dos.write(username.getBytes());
			dos.write(password.getBytes());
			dos.write(username.getBytes());
			dos.write(password.getBytes());

			return dos.getMessageDigest().digest();
		}
		catch(Exception e)
		{
			return null;
		}
	}

	/** 
	 * Adds a property to this object's meta-data.
	 * @param name the property name.
	 * @param value the property value.
	 */
	public void setProperty(String name, String value)
	{
		properties.setProperty(name, value);
	}

	/**
	 * Retrieve a property by name.
	 * @param name the property name.
	 * @return value of name property, or null if not defined.
	 */
	public String getProperty(String name)
	{
		return PropertiesUtil.getIgnoreCase(properties, name);
	}

	/**
	 * @return enumeration of all names in the property set.
	 */
	public Enumeration getPropertyNames()
	{
		return properties.propertyNames();
	}

	/**
	 * Removes a property assignment.
	 * @param name the property name.
	 */
	public void rmProperty(String name)
	{
		properties.remove(name);
	}

	/**
	 * Sets all the properties at once.
	 */
	public void setProperties(Properties properties)
	{
		this.properties = properties;
	}

	/**
	 * Gets the properties all at once.
	 * @return the properites
	 */
	public Properties getProperties()
	{
		return properties;
	}
}
