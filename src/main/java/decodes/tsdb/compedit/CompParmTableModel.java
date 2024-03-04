package decodes.tsdb.compedit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.ResourceBundle;

import javax.swing.table.AbstractTableModel;

import decodes.db.Constants;
import decodes.db.DataType;
import decodes.db.SiteName;
import decodes.gui.SortingListTableModel;
import decodes.tsdb.DbCompParm;
import decodes.tsdb.DbComputation;
import decodes.tsdb.TimeSeriesDb;

public class CompParmTableModel extends AbstractTableModel implements SortingListTableModel 
{
	ArrayList<DbCompParm> myvector = new ArrayList<DbCompParm>();

	ResourceBundle rb = CAPEdit.instance().compeditDescriptions;
	String columnNames[];
	int columnWidths[];
	ComputationsEditPanel parent = null;

	CompParmTableModel(ComputationsEditPanel parent)
	{
		this.parent = parent;
		TimeSeriesDb tsdb = CAPEdit.instance().getTimeSeriesDb();
		String tabSelLab = tsdb.getTableSelectorLabel();
		if (tabSelLab.length() > 10)
			tabSelLab = tabSelLab.substring(0, 10);
		if (tsdb.isHdb())
		{
			columnNames = 
				new String[]
				{ 
					rb.getString("ComputationsEditPanel.TableColumnRole"), 
					rb.getString("ComputationsEditPanel.TableColumnSite"), 
					rb.getString("ComputationsEditPanel.TableColumnDatatype"), 
					rb.getString("ComputationsEditPanel.TableColumnInterval"), 
					"Real/Model",
					"Model ID",
					rb.getString("ComputationsEditPanel.TableColumnDeltaT")
				};
				columnWidths = new int[]{ 15, 15, 15, 15, 15, 15, 10 };
		}
		else
		{
			columnNames = 
				new String[]
				{ 
					rb.getString("ComputationsEditPanel.TableColumnRole"),
					rb.getString("ComputationsEditPanel.TableColumnLocation"),
					rb.getString("ComputationsEditPanel.TableColumnParam"),
					rb.getString("ComputationsEditPanel.TableColumnParamType"),
					rb.getString("ComputationsEditPanel.TableColumnInterval"), 
					rb.getString("ComputationsEditPanel.TableColumnDuration"), 
					rb.getString("ComputationsEditPanel.TableColumnVersion")
				};
			columnWidths = new int[]{ 15, 15, 15, 15, 15, 15, 10 };
		}
	}

	public void sortByColumn(int c) 
	{
		Collections.sort(myvector, new ComputationsEditComparator(c, this));
		fireTableDataChanged();
	}

	public Object getRowObject(int arg0) {
		return myvector.get(arg0);
	}

	public int getRowCount() {
		return myvector.size();
	}

	public int getColumnCount() 
	{
		return columnWidths.length;
	}

	public void deleteAt(int r)
	{
		myvector.remove(r);
		fireTableDataChanged();
	}

	public void removeParm(String pname)
	{
		for(int i=0; i<myvector.size(); i++)
			if (myvector.get(i).getRoleName().equals(pname))
			{
				deleteAt(i);
				return;
			}
	}

	public void fill(DbComputation dc)
	{
		for(Iterator<DbCompParm> pit = dc.getParms(); pit.hasNext(); )
		{
			DbCompParm dcp = pit.next();
			myvector.add(dcp);
		}
		Collections.sort(myvector, new ComputationsEditComparator(-1, this));
		fireTableDataChanged();
	}

	public void add(DbCompParm dcp)
	{
		myvector.add(dcp);
		fireTableDataChanged();
	}

	public String getColumnName(int col) 
	{
		return columnNames[col];
	}

	public Object getValueAt(int rowIndex, int columnIndex) 
	{
		if (myvector.get(rowIndex) != null)
			return getNlColumn(myvector.get(rowIndex), columnIndex);
		else
			return "";
	}

	public String getNlColumn(DbCompParm compParm, int columnIndex) 
	{
		TimeSeriesDb tsdb = CAPEdit.instance().theDb;

		switch (columnIndex) 
		{
		case 0:
			return compParm.getRoleName();
		case 1:
		  {
			SiteName sn = compParm.getSiteName();
//if(tsdb.isCwms())System.out.println("locspec='" + compParm.getLocSpec() + "'");
			return sn != null ? sn.getNameValue() :
				parent.hasGroupInput() ? 
				((tsdb.isCwms()||tsdb.isOpenTSDB()) 
					&& compParm.getLocSpec().length() > 0 ? compParm.getLocSpec() : "<var>") : "";
		  }
		case 2:
		  {
			DataType dt = compParm.getDataType();
			if (dt == null)
				return parent.hasGroupInput() ? 
					((tsdb.isCwms()||tsdb.isOpenTSDB()) 
						&& compParm.getParamSpec().length() > 0 ? compParm.getParamSpec() : "<var>") : "";
			else
				return dt.getCode();
		  }
		case 3:
			if (!tsdb.isCwms() && !tsdb.isOpenTSDB())
			{
				String s = compParm.getInterval();
				if (s == null || s.trim().length() == 0)
					return parent.hasGroupInput() ? "<var>" : "";
				return s;
			}
			else // CWMS - return 6-part time series Identifier
			{
				String s = compParm.getParamType();
				if (s == null || s.trim().length() == 0)
					return parent.hasGroupInput() ? "<var>" : "";
				return s;
			}
		case 4:
			if (!tsdb.isCwms() && !tsdb.isOpenTSDB())
			{
				String s = compParm.getTableSelector();
				if (s == null || s.trim().length() == 0)
					return parent.hasGroupInput() ? "<var>" : "";
				return s;
			}
			else
			{
				String s = compParm.getInterval();
				if (s == null || s.trim().length() == 0)
					return parent.hasGroupInput() ? "<var>" : "";
				return s;
			}
		case 5:
			if (tsdb.isCwms() || tsdb.isOpenTSDB())
			{
				String s = compParm.getDuration();
				if (s == null || s.trim().length() == 0)
					return parent.hasGroupInput() ? "<var>" : "";
				return s;
			}
			else if (tsdb.isHdb())
			{
				int modelId = compParm.getModelId();
				if (modelId != Constants.undefinedIntKey)
					return "" + compParm.getModelId();
				else 
					return parent.hasGroupInput() ? "<var>" : "N/A";
			}
		case 6:
			if (tsdb.isHdb())
			{
				String s = "" + compParm.getDeltaT();
				if (compParm.getDeltaTUnits() != null)
					s = s + " " + compParm.getDeltaTUnits();
				return s;
			}
			else // CWMS
			{
				String s = compParm.getVersion();
				if (s == null || s.trim().length() == 0)
					return parent.hasGroupInput() ? "<var>" : "";
				return s;
			}
		default:
			return "";
		}
	}

	void saveTo(DbComputation dc)
	{
		dc.clearParms();
		for(DbCompParm dcp : myvector)
			dc.addParm(dcp);
	}

	DbCompParm findByName(String roleName, int otherThan)
	{
		for(int r=0; r < myvector.size(); r++)
		{
			DbCompParm dcp = myvector.get(r);
			if (roleName.equalsIgnoreCase(dcp.getRoleName())
			 && r != otherThan)
				return dcp;
		}
		return null;
	}
}
