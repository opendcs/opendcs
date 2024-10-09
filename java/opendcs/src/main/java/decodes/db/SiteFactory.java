/*
 * $Id$
 */
package decodes.db;

/**
 * Programs that write site records (such as editors and importers) will have
 * one SiteFactory. This object validates a site name and constructs a
 * new site record.
 * <p>
 * The default implementation here ensures that a site with a matching
 * name doesn't already exist, and that the name-value has no embedded
 * blanks. Additional actions can be coded into a sub-class.
 */
public class SiteFactory
{
	/**
	 * Default constructor.
	 */
	public void SiteFactory()
	{
	}

	/**
	 * Makes and returns a new Site object with the given name.
	 * The 'db' arg is provided for validation purposes. The returned
	 * site will <i>not</i> be placed into the site list in the passed 
	 * database.
	 * @param siteName the SiteName object.
	 * @param db the database in which this site will reside.
	 * @throws ValueAlreadyDefinedException if a site with the passed
	 *         name already exists in the passed database.
	 * @throws ValueNotFoundException if the passed name is empty or invalid.
	 */
	public Site makeNewSite(SiteName siteName, Database db)
		throws ValueAlreadyDefinedException, ValueNotFoundException
	{
		validateName(siteName, db);

		Site site = new Site();
		siteName.setSite(site);
		site.addName(siteName);
		return site;
	}

	/**
	 * Validates a site name for syntax and to make sure that it isn't
	 * already used by another site.
	 * @param sn the SiteName object.
	 * @param db the database in which this site will reside.
	 * @throws ValueAlreadyDefinedException if name is used by a different site.
	 * @throws ValueNotFoundException on name syntax or logic error.
	 */
	public void validateName(SiteName sn, Database db)
        throws ValueAlreadyDefinedException, ValueNotFoundException
	{
		Site site = db.siteList.getSite(sn);
		if (site != null)
		{
			throw new ValueAlreadyDefinedException(
				"A " + sn.getNameType() + " site with the name " 
				+ sn.getDisplayName() + " already exists in this database.");
		}
		String nameValue = sn.getNameValue();
		if (nameValue.length() == 0)
			throw new ValueNotFoundException("Empty site name.");
		if (!sn.getNameType().equalsIgnoreCase(Constants.snt_CWMS) 
		 && nameValue.indexOf(' ') != -1)
			throw new ValueNotFoundException(
				"Site name may not have embedded blanks.");
	}

}
