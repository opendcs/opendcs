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
package lrgs.db;

import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.Properties;

import org.opendcs.authentication.AuthSourceService;
import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import lrgs.lrgsmain.LrgsConfig;
import ilex.util.AuthException;
import ilex.util.ByteUtil;
import ilex.util.EnvExpander;
import ilex.util.PasswordFile;
import ilex.util.PasswordFileEntry;
import ilex.util.PropertiesUtil;
import ilex.util.TextUtil;

public class DbPasswordFile extends PasswordFile
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	private LrgsDatabase lrgsDb = null;
	public static final String module = "DbPasswordFile";
	private String columns = "username, pw_hash, dds_perm, is_admin, is_local, props, last_modified";
	private String select = "select " + columns + " from dds_user";

	public DbPasswordFile(File passwordFile, LrgsDatabase lrgsDb)
	{
		super(passwordFile);
		this.lrgsDb = lrgsDb;
	}

	/**
	 * {@inheritDoc}
	 * This method overrides the read method to read entries from the database.
	 * Note that in the database, both local and shared entries are read, whereas
	 * in the file implementation they reside in separate files.
	 */
	@Override
	public int read( )
		throws IOException
	{
		entries.clear();

		String q = select + " order by username";
		log.trace("query: {}", q);
		try (Statement stat = lrgsDb.createStatement();
		 	ResultSet rs = stat.executeQuery(q);)
		{
			while (rs.next())
			{
				rs2pfe(rs); // will also add to the cache.
			}
		}
		catch (Exception ex)
		{
			throw new IOException("error in query '" + q + "'", ex);
		}

		return entries.size();
	}

	private PasswordFileEntry rs2pfe(ResultSet rs)
		throws AuthException, SQLException
	{
		PasswordFileEntry ret = new PasswordFileEntry(rs.getString(1));
		ret.setOwner(this);
		String pw = rs.getString(2);
		if (!rs.wasNull())
			ret.setShaPassword(ByteUtil.fromHexString(pw));
		if (TextUtil.str2boolean(rs.getString(3)))
			ret.assignRole("dds");
		if (TextUtil.str2boolean(rs.getString(4)))
			ret.assignRole("admin");
		ret.setLocal(TextUtil.str2boolean(rs.getString(5)));
		String prop_str = rs.getString(6);
		if (prop_str != null)
			ret.setProperties(PropertiesUtil.string2props(prop_str));
		ret.setLastModified(lrgsDb.getFullDate(rs, 7));
		entries.put(ret.getUsername(), ret);

		return ret;
	}

	@Override
	public void write( ) throws IOException
	{

		String q = "";

		try (Statement stat = lrgsDb.createStatement();)
		{
			for(PasswordFileEntry pfe : entries.values())
				if (pfe.isChanged())
				{
					PasswordFileEntry oldPfe = readSingle(pfe.getUsername());
					if (oldPfe == null)
					{
						Properties props = pfe.getProperties();
						q = "insert into dds_user(" + columns + ") values("
							+ lrgsDb.sqlString(pfe.getUsername()) + ", "
							+ (pfe.getShaPassword() == null ? "null" :
								lrgsDb.sqlString(ByteUtil.toHexString(pfe.getShaPassword()))) + ", "
							+ (pfe.isRoleAssigned("dds") ? "'Y'" : "'N'") + ", "
							+ (pfe.isRoleAssigned("admin") ? "'Y'" : "'N'") + ", "
							+ (pfe.isLocal() ? "'Y'" : "'N'") + ", "
							+ (props == null ? "null" : lrgsDb.sqlString(PropertiesUtil.props2string(props))) + ", "
							+ lrgsDb.sqlDate(new Date())
							+ ")";
					}
					else
					{
						// update
						int nmods = 0;
						q = "update dds_user set ";

						StringBuilder sets = new StringBuilder();

						if (!ByteUtil.equals(pfe.getShaPassword(), oldPfe.getShaPassword()))
						{
							addSet(sets, "pw_hash = " +
								(pfe.getShaPassword()==null ? "null" :
									lrgsDb.sqlString(ByteUtil.toHexString(pfe.getShaPassword()))));
							addSet(sets, "last_modified = " + lrgsDb.sqlDate(new Date()));
						}

						if (pfe.isRoleAssigned("dds") != oldPfe.isRoleAssigned("dds"))
							addSet(sets, "dds_perm = " + (pfe.isRoleAssigned("dds") ? "'Y'" : "'N'"));

						if (pfe.isRoleAssigned("admin") != oldPfe.isRoleAssigned("admin"))
							addSet(sets, "is_admin = " + (pfe.isRoleAssigned("admin") ? "'Y'" : "'N'"));

						if (pfe.isLocal() != oldPfe.isLocal())
							addSet(sets, "is_local = " + (pfe.isLocal() ? "'Y'" : "'N'"));

						if (!PropertiesUtil.propertiesEqual(pfe.getProperties(), oldPfe.getProperties()))
							addSet(sets, "props = " +
								(pfe.getProperties() == null ? "null" :
								lrgsDb.sqlString(PropertiesUtil.props2string(pfe.getProperties()))));

						if (sets.length() == 0)
							continue; // nothing to change.

						q = q + sets.toString() + " where username = " + lrgsDb.sqlString(pfe.getUsername());
					}
					stat.executeUpdate(q);
				}
		}
		catch (Exception ex)
		{
			throw new IOException("Error in query '" + q + "'", ex);
		}
	}

	private void addSet(StringBuilder sets, String setclause)
	{
		if (sets.length() > 0)
			sets.append(", ");
		sets.append(setclause);
	}

	/**
	 * Reads a single entry from the database and does not store it in the entries hash.
	 * @param username
	 * @return PFE if one exists for username, or null if not.
	 */
	public PasswordFileEntry readSingle(String username)
	{
		String q = select + " where username = " + lrgsDb.sqlString(username);
		log.trace("query: '{}'", q);
		try (Statement stat = lrgsDb.createStatement();
			 ResultSet rs = stat.executeQuery(q);)
		{

			if (rs.next())
				return rs2pfe(rs);
			else
				return null;
		}
		catch (Exception ex)
		{
			log.atWarn().setCause(ex).log("error in query '{}'", q);
			return null;
		}
	}

	public void print(boolean local)
	{
		for(PasswordFileEntry pfe : entries.values())
			if (pfe.isLocal() == local)
				System.out.println(pfe.toString());
	}

	public void importFile(String filename, boolean local)
		throws IOException
	{
		super.passwordFile = new File(filename);
		super.read();
		for(PasswordFileEntry pfe : entries.values())
		{
			pfe.setChanged(true);
			pfe.setLocal(local);
		}
		this.write();
	}

	@Override
	public boolean rmEntryByName( String username )
	{
		if (entries != null)
			entries.remove(username);
		String q = "delete from dds_user where username = " + lrgsDb.sqlString(username);
		log.trace("query: '{}'", q);
		try (Statement stat = lrgsDb.createStatement())
		{

			stat.executeUpdate(q);
		}
		catch (Exception ex)
		{
			log.atWarn().setCause(ex).log("error in query '{}'", q);
		}
		return true;
	}


	private static String usage = "java ... DbPasswordFile args, where args can be: \n"
		+ "    il filename -- import local password file to database\n"
		+ "    is filename -- import shared password file to database\n"
		+ "    xl          -- export local users to stdout\n"
		+ "    xs          -- export shared (non-local) users to stdout\n";

	public static void main(String args[])
		throws Exception
	{
		if (args.length == 0 || args.length > 2)
		{
			System.err.println(usage);
			System.exit(1);
		}

		LrgsConfig.instance().setConfigFileName(EnvExpander.expand("$LRGSHOME/lrgs.conf"));
		LrgsConfig.instance().loadConfig();
		LrgsDatabase lrgsDb = new LrgsDatabase();
		String username = "lrgs_adm";
		String authFileName = "$LRGSHOME/.lrgsdb.auth";
		Properties credentials = null;
		try
		{
			credentials = AuthSourceService.getFromString(authFileName)
													  .getCredentials();
			username = credentials.getProperty("username");
		}
		catch(AuthException ex)
		{
			String msg = "Cannot read DB auth from file '"
					   + authFileName+ "', using default username and empty password";
			throw new AuthException(msg, ex);
		}

		log.info("Attempting connection to db at '{}' as user '{}'",
				 LrgsConfig.instance().dbUrl, username);
		lrgsDb.connect(credentials);
		DbPasswordFile dpf = new DbPasswordFile(null, lrgsDb);

		if (args.length == 1 && (args[0].equals("xl") || args[0].equals("xs")))
		{
			dpf.read();
			dpf.print(args[0].charAt(1) == 'l');
		}
		else if (args.length == 2 && (args[0].equals("il") || args[0].equals("is")))
		{
			dpf.importFile(args[1], args[0].charAt(1) == 'l');
			dpf.write();
		}
	}
}