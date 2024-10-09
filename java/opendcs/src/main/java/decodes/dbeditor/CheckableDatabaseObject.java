package decodes.dbeditor;

import ilex.gui.CheckableItem;
import decodes.db.*;

/**
* This class is used with CheckBoxList to implement a list of
* database items that can be checked.
*/
class CheckableDatabaseObject extends CheckableItem
{
	private DatabaseObject dbo;
	private boolean indent;

	/**
	 * Constructor.
	 * @param dbo the database object
	 * @param indent true if this item should be indented in the list.
	 */
	CheckableDatabaseObject(DatabaseObject dbo, boolean indent)
	{
		super();
		this.dbo = dbo;
		this.indent = indent;
	}

	/**
	 * @return a displayable string representing this database object.
	 */
	public String getDisplayString()
	{
		if (dbo instanceof Platform)
		{
			Platform p = (Platform)dbo;
			return (indent ? "    " : "") +
				"Platform: " + p.makeFileName();
		}
		else if (dbo instanceof Site)
		{
			Site s = (Site)dbo;
			return (indent ? "    " : "") +
				"Site: " + s.getDisplayName();
		}
		else if (dbo instanceof PlatformConfig)
		{
			PlatformConfig p = (PlatformConfig)dbo;
			return (indent ? "    " : "") +
				"Config: " + p.makeFileName();
		}
		else if (dbo instanceof PresentationGroup)
		{
			PresentationGroup pg = (PresentationGroup)dbo;
			return (indent ? "    " : "") +
				"Presentation Group: " + pg.groupName;
		}
		else if (dbo instanceof DataSource)
		{
			DataSource ds = (DataSource)dbo;
			return (indent ? "    " : "") +
				"Data Source: " + ds.getName();
		}
		else if (dbo instanceof RoutingSpec)
		{
			RoutingSpec rs = (RoutingSpec)dbo;
			return (indent ? "    " : "") +
				"RoutingSpec: " + rs.getName();
		}
		else if (dbo instanceof NetworkList)
		{
			NetworkList nl = (NetworkList)dbo;
			return (indent ? "    " : "") +
				"Network List: " + nl.name;
		}
		else return "invalid";
	}

	public DatabaseObject getDBO() { return dbo; }
}
