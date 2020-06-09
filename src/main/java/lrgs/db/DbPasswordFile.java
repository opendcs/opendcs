package lrgs.db;

import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.Properties;

import lrgs.lrgsmain.LrgsConfig;
import ilex.util.AuthException;
import ilex.util.ByteUtil;
import ilex.util.EnvExpander;
import ilex.util.Logger;
import ilex.util.PasswordFile;
import ilex.util.PasswordFileEntry;
import ilex.util.PropertiesUtil;
import ilex.util.TextUtil;
import ilex.util.UserAuthFile;

public class DbPasswordFile extends PasswordFile
{
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
		Statement stat = null;
		String q = select + " order by username";
		try
		{
			stat = lrgsDb.createStatement();
			Logger.instance().debug3(module + " query: " + q);
			ResultSet rs = stat.executeQuery(q);
			while(rs != null && rs.next())
				rs2pfe(rs); // will also add to the cache.
		}
		catch (Exception ex)
		{
			String msg = module + ".read() error in query '" + q + "': " + ex;
			Logger.instance().warning(msg);
			throw new IOException(msg);
		}
		finally
		{
			if (stat != null)
				try { stat.close(); } catch(Exception ex) {}
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
		Statement stat = null;
		String q = "";
//Logger.instance().info("DbPasswordFile.write() #entries=" + entries.size());
		try
		{
			stat = lrgsDb.createStatement();
			for(PasswordFileEntry pfe : entries.values())
				if (pfe.isChanged())
				{
//Logger.instance().info("Entry for " + pfe.getUsername() + " is changed.");
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
//Logger.instance().info("Old Entry: " + oldPfe.toString());
//Logger.instance().info("New Entry: " + pfe.toString());

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
//Logger.instance().info("DbPasswordFile.write: " + q);
					stat.executeUpdate(q);
				}
		}
		catch (Exception ex)
		{
			String msg = module + ".write() Error in query '" + q + "': " + ex;
			Logger.instance().warning(msg);
			throw new IOException(msg);
		}
		finally
		{
			if (stat != null)
				try { stat.close(); } catch(Exception ex) {}
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
		String q = select + " where username = '" + username + "'";
		Statement stat = null;
		try
		{
			stat = lrgsDb.createStatement();
			Logger.instance().debug3(module + " query: " + q);
			ResultSet rs = stat.executeQuery(q);
			if (rs != null && rs.next())
				return rs2pfe(rs);
			else
				return null;
		}
		catch (Exception ex)
		{
			String msg = module + ".readSingle() error in query '" + q + "': " + ex;
			Logger.instance().warning(msg);
			return null;
		}
		finally
		{
			if (stat != null)
				try { stat.close(); } catch(Exception ex) {}
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
		String q = "delete from dds_user where username = '" + username + "'";
		Statement stat = null;
		try
		{
			stat = lrgsDb.createStatement();
			Logger.instance().debug3(module + " query: " + q);
			stat.executeUpdate(q);
		}
		catch (Exception ex)
		{
			String msg = module + ".rmEntryByName() error in query '" + q + "': " + ex;
			Logger.instance().warning(msg);
		}
		finally
		{
			if (stat != null)
				try { stat.close(); } catch(Exception ex) {}
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
		String password = "";
		String authFileName = EnvExpander.expand("$LRGSHOME/.lrgsdb.auth");
		UserAuthFile authFile = new UserAuthFile(authFileName);
		try 
		{
			authFile.read();
			username = authFile.getUsername();
			password = authFile.getPassword();
		}
		catch(Exception ex)
		{
			String msg = module + " Cannot read DB auth from file '" 
				+ authFileName+ "': " + ex;
			Logger.instance().warning(msg);
		}

		Properties credentials = new Properties();
		credentials.setProperty("username", username);
        credentials.setProperty("password", password);

		Logger.instance().info("Attempting connection to db at '"
			+ LrgsConfig.instance().dbUrl + "' as user '" + username + "'");
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
