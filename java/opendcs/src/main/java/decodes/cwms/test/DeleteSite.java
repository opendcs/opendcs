package decodes.cwms.test;

import opendcs.dai.SiteDAI;
import ilex.cmdline.StringToken;
import ilex.cmdline.TokenOptions;
import ilex.util.Logger;
import decodes.cwms.CwmsSiteDAO;
import decodes.db.Constants;
import decodes.db.Site;
import decodes.db.SiteName;
import decodes.tsdb.TsdbAppTemplate;
import decodes.util.CmdLineArgs;

public class DeleteSite extends TsdbAppTemplate
{
	private StringToken siteNameArg = new StringToken("", "CWMS-SiteName", "",
		TokenOptions.optArgument|TokenOptions.optRequired, "");

	public DeleteSite()
	{
		super("util.log");
	}

	@Override
	protected void runApp() throws Exception
	{
		System.out.println("Setting debug to most verbose. See log files for details after run.");
		Logger.instance().setMinLogPriority(Logger.E_DEBUG3);
		
		String sn = siteNameArg.getValue();
		if (sn == null || sn.trim().length() == 0)
			throw new Exception("Missing required argument for CWMS Site Name");

		SiteDAI siteDAO = theDb.makeSiteDAO();
		if (!(siteDAO instanceof CwmsSiteDAO))
			log("siteDAO is not an instance of CwmsSiteDAO!!!");
		SiteName siteName = new SiteName(null, Constants.snt_CWMS, sn);
		Site site = siteDAO.getSiteBySiteName(siteName);
		if (site == null)
		{
			log("\nNo such site with CWMS name '" + sn + "'");
			System.exit(1);
		}
		else
		{
			log("\nSuccessfully read Site:");
			printSite(site);
		}
		log("\nDeleting...");
		siteDAO.deleteSite(site.getKey());
		log("\nAfter deleting. Will reread.");
		siteName = new SiteName(null, Constants.snt_CWMS, sn);
		site = siteDAO.getSiteBySiteName(siteName);
		if (site == null)
			log("\nSite is gone. Read returned null.");
		else
		{
			log("\nSite is still present.");
			printSite(site);
		}
	}
	
	private void printSite(Site site)
	{
		log("\tCWMS name = '" + site.getName(Constants.snt_CWMS) + "'");
		log("\tDescription = " + site.getDescription());
		log("\tNumber of names = " + site.getNameCount());
		if (site.getNameCount() > 1)
			for(int idx = 0; idx<site.getNameCount(); idx++)
				if (!site.getNameAt(idx).getNameType().equalsIgnoreCase(Constants.snt_CWMS))
					log("\tOther Name: " + site.getNameAt(idx));
	}
	
	@Override
	protected void addCustomArgs(CmdLineArgs cmdLineArgs)
	{
		cmdLineArgs.addToken(siteNameArg);
	}


	/**
	 * @param args
	 */
	public static void main(String[] args)
		throws Exception
	{
		DeleteSite deleteSite = new DeleteSite();
		deleteSite.execute(args);
	}
	
	private void log(String msg)
	{
		Logger.instance().info(msg);
		System.out.println(msg);
	}

}
