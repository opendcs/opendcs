package decodes.tsdb;

import ilex.util.Logger;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import lrgs.gui.DecodesInterface;
import decodes.db.Database;
import decodes.db.Platform;
import decodes.sql.SqlDatabaseIO;

/**
 * This is primarily for large DCP Monitor databases.
 * It will remove any platform that ...
 * - Has no transport media
 * - Has no config
 * - Is expired (i.e. has an expiration date)
 * - Has the same site/designator combination as another platform
 *   (Note, which platform is kept is indeterminate).
 */
public class CleanupPlatforms extends TsdbAppTemplate
{

	public CleanupPlatforms()
	{
		super("util.log");
		DecodesInterface.silent = true;
	}

	@Override
	protected void runApp() throws Exception
	{
		SqlDatabaseIO dbio = (SqlDatabaseIO)Database.getDb().getDbIo();
		
		HashMap<String, Platform> platnames = new HashMap<String, Platform>();
		for(Iterator<Platform> pit = Database.getDb().platformList.iterator(); pit.hasNext(); )
		{
			Platform p = pit.next();
			String platname = p.makeFileName();
			if (p.transportMedia.size() == 0)
			{
				Logger.instance().info("Removing platform " + p.getKey() + " '" + platname 
					+ "' from agency '" + p.getAgency() 
					+ "' because it has no transport media.");
				dbio.deletePlatform(p);
				continue;
			}
			if (p.getConfig() == null)
			{
				Logger.instance().info("Removing platform " + p.getKey() + " '" + platname 
					+ "' from agency '" + p.getAgency() 
					+ "' because it has no configuration.");
				dbio.deletePlatform(p);
				continue;
			}
			if (p.expiration != null)
			{
					Logger.instance().info("Removing platform " + p.getKey() + " '" + platname 
						+ "' from agency '" + p.getAgency() 
						+ "' it has an expiration date of " + p.expiration);
					dbio.deletePlatform(p);
					continue;
			}
			Platform existingPlat = platnames.get(platname);
			if (existingPlat != null)
			{
				Logger.instance().info("Removing platform " + p.getKey() + " '" + platname 
					+ "' from agency '" + p.getAgency() 
					+ "' because there is already a platform with that name. The"
					+ "existing platform has ID="
					+ existingPlat.getId() + "  and  agency '" + existingPlat.getAgency() + "'");
				dbio.deletePlatform(p);
			}
			
			// Fell through -- we're keeping this one.
			platnames.put(platname, p);
		}
	}

	public static void main(String[] args)
		throws Exception
	{
		CleanupPlatforms to = new CleanupPlatforms();
		to.execute(args);
	}

}
