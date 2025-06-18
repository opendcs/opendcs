package decodes.dbeditor;

import java.util.Comparator;

import ilex.util.Logger;
import ilex.util.TextUtil;

import decodes.db.Site;
import decodes.db.SiteName;

/**
 * Used for sorting and searching for site names.
 */
class SiteNameComparator implements Comparator
{
	String nameType;
	String nameValue;

	public SiteNameComparator(String type, String value)
	{
		nameType = type;
		nameValue = value;
	}

	/**
	 * Compare the site names of the specified type.
	 */
	public int compare(Object ob1, Object ob2)
	{
		if (ob1 == ob2)
			return 0;
		Site site1 = (Site)ob1;
		Site site2 = (Site)ob2;
		if (nameType == SiteSelectPanel.descriptionLabel)
			return TextUtil.strCompareIgnoreCase(site1.getDescription(),
				site2.getDescription());
		SiteName sn1 = site1.getName(nameType);
		String ss1 = sn1 != null ? sn1.getDisplayName() : "";
		SiteName sn2 = site2.getName(nameType);
		String ss2 = sn2 != null ? sn2.getDisplayName() : "";
		if (ss1.length() > 0 && ss2.length() == 0)
			return -1;
		else if (ss1.length() == 0 && ss2.length() > 0)
			return 1;
		if (nameType != null && nameType.equalsIgnoreCase("hdb"))
		{
			if (!ss1.equals("") && !ss2.equals(""))
			{
				int i1 = 0;
				int i2 = 0;
				try 
				{
					i1 = Integer.parseInt(ss1);
				}
				catch (Exception ex)
				{
					Logger.instance().warning(
						" SiteSelectPanel - SiteNameComparator " +
						" Can not sort column by hdb site name type." +
						" HDB site name is not a number. Site: " + ss1);
					return 1;
				}
				try 
				{
					i2 = Integer.parseInt(ss2);
				}
				catch (Exception ex)
				{
					Logger.instance().warning(
						" SiteSelectPanel - SiteNameComparator " +
						" Can not sort column by hdb site name type." +
						" HDB site name is not a number. Site: " + ss2);
					return -1;
				}
				return i1 - i2;
			}
		}
		return ss1.compareToIgnoreCase(ss2);
	}

	public boolean equals(Object ob)
	{
		return false;
	}
}
