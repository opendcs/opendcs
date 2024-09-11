package decodes.dbeditor.routing;

import java.util.Collections;
import java.util.ResourceBundle;
import java.util.Vector;

import javax.swing.table.AbstractTableModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import decodes.db.Constants;
import decodes.db.Database;
import decodes.db.DatabaseException;
import decodes.db.RoutingSpec;
import decodes.db.RoutingSpecList;
import decodes.dbeditor.DbEditorFrame;
import decodes.dbeditor.RoutingSpecSelectPanel;

public class RSListTableModel extends AbstractTableModel
{
	private static final Logger log = LoggerFactory.getLogger(RSListTableModel.class);
	private static ResourceBundle generic = ResourceBundle.getBundle("decodes/resources/generic");    
    private static ResourceBundle dbedit = ResourceBundle.getBundle("decodes/resources/dbedit");
	static String columnNames[] =
	{
		generic.getString("name"),
		dbedit.getString("RoutingSpecListPanel.dataSource"),
		dbedit.getString("RoutingSpecListPanel.consumer"),
		generic.getString("lastMod")
	};

	private int lastSortColumn = -1;
	private RoutingSpecList theList;
	private final Database db;

	public RSListTableModel(Database db)
	{
		super();
		this.db = db;
		theList = db.routingSpecList;
		refill();
	}

	public void deleteObjectAt(int r)
	{
		theList.remove(getObjectAt(r));
		fireTableDataChanged();
	}

	public void refill()
	{
		theList.clear();
		try
		{
			db.getDbIo().readRoutingSpecList(theList);
		}
		catch (DatabaseException ex)
		{
			log.atError()
			   .setCause(ex)
			   .log("Cannot read routing spec list.");
		}
		fireTableDataChanged();
	}

	public int getRowCount()
	{
		return theList.size();
	}

	public int getColumnCount()
	{
		return columnNames.length;
	}

	public String getColumnName(int col)
	{
		return columnNames[col];
	}

	public RoutingSpec getObjectAt(int r)
	{
		return (RoutingSpec) getRowObject(r);
	}

	public Object getRowObject(int r)
	{
		if (r >= 0 && r < getRowCount())
			return theList.getList().elementAt(r);
		else
			return null;
	}

	public void addObject(RoutingSpec ob)
	{
		theList.add(ob);
		fireTableDataChanged();
	}

	public void deleteObject(RoutingSpec ob)
	{
		try
		{
			db.getDbIo().deleteRoutingSpec(ob);
			theList.remove(ob);
			fireTableDataChanged();
		}
		catch (DatabaseException e)
		{
			final String msg = dbedit.getString("RoutingSpecListPanel.errorDelete");
			log.atError()
			   .setCause(e)
			   .log();
			DbEditorFrame.instance().showError(msg + e.toString());
		}
		
	}

	public Object getValueAt(int r, int c)
	{
		RoutingSpec ob = getObjectAt(r);
		if (ob == null)
			return "";
		else
			return getRsColumn(ob, c);
	}

	private static Object getRsColumn(RoutingSpec ob, int c)
	{
		switch (c)
		{
		case 0:
			return ob.getName();
		case 1:
			return ob.dataSource == null ? "" : (ob.dataSource.getName() + " ("
				+ ob.dataSource.dataSourceType + ")");
		case 2:
			return ob.consumerType + "(" + 
				(ob.consumerArg == null ? "" : ob.consumerArg) + ")";
		case 3:
			if (ob.lastModifyTime == null)
				return "";
			else
				return ob.lastModifyTime;
		default:
			return "";
		}
	}
}
