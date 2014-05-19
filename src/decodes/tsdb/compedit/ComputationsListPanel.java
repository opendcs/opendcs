/*
*  $Id$
*
*  This is open-source software written by ILEX Engineering, Inc., under
*  contract to the federal government. You are free to copy and use this
*  source code for your own purposes, except that no part of the information
*  contained in this file may be claimed to be proprietary.
*
*  Except for specific contractual terms between ILEX and the federal 
*  government, this source code is provided completely without warranty.
*  For more information contact: info@ilexeng.com
*  
*  $Log$
*  Revision 1.16  2013/07/25 18:42:24  mmaloney
*  dev
*
*  Revision 1.15  2013/07/24 13:45:50  mmaloney
*  New comp filtering code. Portable across database implementations
*
*  Revision 1.14  2012/08/17 13:35:27  mmaloney
*  Don't assume running inside compedit. This panel is also used in comprun GUI and
*  maybe elsewhere. When listing comps, fetch our own list of algorithms and apps.
*
*  Revision 1.13  2012/08/17 13:22:38  mmaloney
*  Added stack trace on null pointer in refresh.
*
*  Revision 1.12  2012/07/31 16:58:54  mmaloney
*  dev
*
*  Revision 1.11  2012/07/31 15:03:18  mmaloney
*  Allow double-click for selection from list panels.
*
*/
package decodes.tsdb.compedit;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collections;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.ResourceBundle;
import java.util.Vector;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;

import opendcs.dai.ComputationDAI;
import opendcs.dai.LoadingAppDAI;

import ilex.util.*;
import decodes.db.Constants;
import decodes.gui.SortingListTable;
import decodes.gui.SortingListTableModel;
import decodes.gui.TopFrame;
import decodes.sql.DbKey;
import decodes.tsdb.*;

@SuppressWarnings("serial")
public class ComputationsListPanel extends ListPanel 
{
	private JPanel jContentPane = null;
	private JTable compListTable = null;
	private ComputationsFilterPanel filterPanel;
	ComputationsListPanelTableModel compListTableModel = null;
	boolean isDialog=false;
	private TimeSeriesDb tsdb = null;
	boolean filterLowIds = false;
	static ResourceBundle compLabels = null;
	private TopFrame parentFrame = null;

	public ComputationsListPanel(TimeSeriesDb theDb, boolean filterLowIds,
		boolean isDialog, TopFrame parentFrame)
	{
		
		tsdb=theDb;
		this.parentFrame = parentFrame;
		this.filterLowIds = filterLowIds;
		this.isDialog = isDialog;
		setLayout(new BorderLayout());
		compLabels = CAPEdit.instance().compeditDescriptions;
		this.add(getJContentPane(), java.awt.BorderLayout.CENTER);
	}

	public void setIsDialog(boolean tf) { isDialog = tf; }
	

	/**
	 * This method initializes jContentPane
	 * 
	 * @return javax.swing.JPanel
	 */
	protected JPanel getJContentPane() 
	{
		if (jContentPane == null) {
			jContentPane = new JPanel();
			jContentPane.setLayout(new BorderLayout());
			if(!isDialog)
			{
				jContentPane.add(getButtonPanel(), java.awt.BorderLayout.SOUTH);
			}
			
			
			JScrollPane scrollPane = new JScrollPane(getCompListTable());
			
			filterPanel = new ComputationsFilterPanel(tsdb, parentFrame);
			filterPanel.getRefresh().addActionListener(
					new java.awt.event.ActionListener()
					{
						public void actionPerformed(ActionEvent e) {
							doRefresh();
						}
					});
			
			jContentPane.add(scrollPane, java.awt.BorderLayout.CENTER);
			jContentPane.add(filterPanel, BorderLayout.NORTH);
		}
		return jContentPane;
	}

	protected JTable getCompListTable() 
	{
		if (compListTableModel == null) 
		{
			compListTableModel = new ComputationsListPanelTableModel();
			compListTable = new SortingListTable(compListTableModel, 
				ComputationsListPanelTableModel.columnWidths);
			compListTable.addMouseListener(
				new MouseAdapter()
				{
					public void mouseClicked(MouseEvent e)
					{
						if (e.getClickCount() == 2)
						{
							doOpen();
						}
					}
				});

		}
		return compListTable;
	}

	protected void doOpen()
	{
		int r = compListTable.getSelectedRow();
		if (r == -1)
		{
			parentFrame.showError(
				compLabels.getString("ComputationsFilterPanel.OpenError"));
			return;
		}
		ComputationInList dc = (ComputationInList)compListTableModel.getRowObject(r);
		ComputationDAI computationDAO = tsdb.makeComputationDAO();
		try
		{
			DbComputation toOpen = computationDAO.getComputationById(dc.getComputationId());
			openEditTab(toOpen);
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			parentFrame.showError(
				"Cannot read computation with id=" + dc.getComputationId() 
				+ ": " + ex);
		}
		finally
		{
			computationDAO.close();
		}
	}

	private void openEditTab(DbComputation dc)
	{
		JTabbedPane tabbedPane = CAPEdit.instance().getComputationsTab();
		int n = tabbedPane.getTabCount();
		for(int idx = 1; idx<n; idx++)
		{
			Component c = tabbedPane.getComponentAt(idx);
			if (c instanceof ComputationsEditPanel)
			{
				ComputationsEditPanel cep = (ComputationsEditPanel)c;
				DbComputation eo = cep.getEditedObject();
				if (eo.getId() != Constants.undefinedId && eo.getId() == dc.getId())
				{
					tabbedPane.setSelectedIndex(idx);
					return;
				}
			}
		}
		ComputationsEditPanel cep = new ComputationsEditPanel();
		cep.setEditedObject(dc);
		tabbedPane.addTab(dc.getName(), null, cep);
		tabbedPane.setSelectedIndex(n);
	}

	protected void doNew()
	{
	    String newName = JOptionPane.showInputDialog(
	    	compLabels.getString("ComputationsFilterPanel.NewInput"));
		if (newName == null)
			return;
		if (compListTableModel.compExists(newName))
		{
			CAPEdit.instance().getFrame().showError(
				compLabels.getString("ComputationsFilterPanel.NewError"));
			return;
		}
		DbComputation dc = new DbComputation(Constants.undefinedId, newName);
		openEditTab(dc);
	}

	protected void doCopy()
	{
		int r = compListTable.getSelectedRow();
		if (r == -1)
		{
			CAPEdit.instance().getFrame().showError(
				compLabels.getString("ComputationsFilterPanel.CopyError1"));
			return;
		}
		ComputationInList dc = (ComputationInList)compListTableModel.getRowObject(r);

	    String newName = JOptionPane.showInputDialog(
	    	compLabels.getString("ComputationsFilterPanel.NewInput"));
		if (newName == null)
			return;
		if (compListTableModel.compExists(newName))
		{
			showError(
				compLabels.getString("ComputationsFilterPanel.CopyError2"));
			return;
		}

		ComputationDAI computationDAO = tsdb.makeComputationDAO();
		try
		{
			DbComputation toCopy = computationDAO.getComputationById(dc.getComputationId());
			DbComputation copydc = toCopy.copyNoId();
			copydc.setName(newName);
			openEditTab(copydc);
		}
		catch(Exception ex)
		{
			showError("Cannot open copy of computation with id="
				+ dc.getComputationId() + ": " + ex);
		}
		finally
		{
			computationDAO.close();
		}
	}

	protected void doDelete()
	{
		int r = compListTable.getSelectedRow();
		if (r == -1)
		{
			CAPEdit.instance().getFrame().showError(
				compLabels.getString("ComputationsFilterPanel.DeleteError1"));
			return;
		}

		ComputationInList dc = (ComputationInList)compListTableModel.getRowObject(r);

		int ok = JOptionPane.showConfirmDialog(this,
			LoadResourceBundle.sprintf(
				compLabels.getString("ComputationListPanel.VerifyDelete"),
				dc.getComputationName()));
		if (ok != JOptionPane.YES_OPTION)
			return;

		ComputationDAI computationDAO = tsdb.makeComputationDAO();
		try
		{
			computationDAO.deleteComputation(dc.getComputationId());
			doRefresh();
		}
		catch(Exception ex)
		{
			CAPEdit.instance().getFrame().showError(
				compLabels.getString(
					"ComputationsFilterPanel.DeleteError2") 
					+ dc.getComputationName() + "': " + ex);
		}
		finally
		{
			computationDAO.close();
		}
	}

	void doRefresh()
	{
		Logger.instance().debug1("ComputationListPanel.doRefresh() ------------");
		ComputationDAI computationDAO = tsdb.makeComputationDAO();
		LoadingAppDAI loadingAppDao = tsdb.makeLoadingAppDAO();
		
		CompFilter compFilter = tsdb.getCompFilter();
		
		compFilter.setFilterLowIds(filterLowIds);
		filterPanel.setFilterParams(compFilter, tsdb);
		try
		{
			ArrayList<AlgorithmInList> algorithms = tsdb.listAlgorithmsForGui();
			ArrayList<CompAppInfo> apps = loadingAppDao.listComputationApps(true);
			ArrayList<DbComputation> comps = computationDAO.listCompsForGUI(compFilter);
			
			for(Iterator<DbComputation> compit = comps.iterator(); compit.hasNext(); )
			{
				DbComputation comp = compit.next();
				TsGroup group = comp.getGroup();

				boolean passes = false;
				if (group == null) // Single, non-group comp?
					passes = compFilter.passes(comp);
				else if (!compFilter.hasParamConditions())
					passes = true;
				else // group computation
				{	
					// Group object may be shared by multiple comps. Only expand it once.
					if (!group.getIsExpanded())
						tsdb.expandTsGroup(group);

					ArrayList<DbComputation> expandedGroupComps = new ArrayList<DbComputation>();
					
					for(TimeSeriesIdentifier tsid : group.getExpandedList())
						try 
						{
							expandedGroupComps.add(
								DbCompResolver.makeConcrete(tsdb, tsid, comp, false));
						}
						catch(NoSuchObjectException ex)
						{
							Logger.instance().debug1("Cannot expand comp(" + comp.getId()
								+ ") " + comp.getName() + ": " + ex);
						}

					if (expandedGroupComps.size() == 0) 
						// No expansions succeeded. Check the abstract comp.
						passes = compFilter.passes(comp);
					else
						for(DbComputation exComp : expandedGroupComps)
							if (passes = compFilter.passes(exComp))
								break;
				}
				if (!passes)
					compit.remove();
			}
			
			ArrayList<ComputationInList> displayComps = new ArrayList<ComputationInList>();
			for(DbComputation comp : comps)
				displayComps.add(
					new ComputationInList(comp.getId(), comp.getName(),
						comp.getAlgorithmId(), comp.getAppId(), comp.isEnabled(), 
						comp.getComment()));

			for(ComputationInList cil : displayComps)
			{
				for(AlgorithmInList algo : algorithms)
					if (cil.getAlgorithmId() == algo.getAlgorithmId())
					{
						cil.setAlgorithmName(algo.getAlgorithmName());
						break;
					}
				for(CompAppInfo app : apps)
					if (cil.getProcessId() == app.getAppId())
					{
						cil.setProcessName(app.getAppName());
						break;
					}
			}
			compListTableModel.setContents(displayComps);
			if (compListTableModel.sortedBy != -1)
				compListTableModel.sortByColumn(compListTableModel.sortedBy);
		}
		catch(Exception ex)
		{
			String msg = "Cannot refresh computation list: " + ex;
			System.err.println(msg);
			ex.printStackTrace(System.err);
			parentFrame.showError(msg);
		}
		finally
		{
			loadingAppDao.close();
			computationDAO.close();
		}

	}

	public Vector<DbComputation> getSelectedComputations()
	{
		Vector<DbComputation> ret = new Vector<DbComputation>();
		int[] selected = compListTable.getSelectedRows();
		
		for(int x : selected)
		{
			ComputationInList dc = (ComputationInList)compListTableModel.getRowObject(x);
			ComputationDAI computationDAO = tsdb.makeComputationDAO();
			try { ret.add(computationDAO.getComputationById(dc.getComputationId())); }
			catch(Exception ex)
			{
				parentFrame.showError("Cannot read computation with id=" + 
					dc.getComputationId() + ": " + ex);
			}
			finally
			{
				computationDAO.close();
			}
		}
		return ret;
	}
}

class ComputationsListPanelTableModel extends AbstractTableModel 
	implements SortingListTableModel 
{
	private ArrayList<ComputationInList> comps = new ArrayList<ComputationInList>();
//	public ArrayList<DbComputation> computations = 
//		new ArrayList<DbComputation>();
	static String columnNames[] = 
	{ 
		ComputationsListPanel.compLabels.getString(
			"ComputationsFilterPanel.TableColumn1"), 
		ComputationsListPanel.compLabels.getString(
			"ComputationsFilterPanel.TableColumn2"), 
		ComputationsListPanel.compLabels.getString(
			"ComputationsFilterPanel.TableColumn3"), 
		ComputationsListPanel.compLabels.getString(
			"ComputationsFilterPanel.TableColumn4"),
		ComputationsListPanel.compLabels.getString(
			"ComputationsFilterPanel.TableColumn5"), 
		ComputationsListPanel.compLabels.getString(
			"ComputationsFilterPanel.TableColumn6")
	};
	static int columnWidths[] = { 10, 20, 15, 10, 5, 40};
	static char sortType[] = { 'i', 'S', 'S', 'S', 'S', 'S' };

	int sortedBy = -1;
	
	/**
	 * Construtor
	 */
	public ComputationsListPanelTableModel()
	{
		super();
	}
	
	public void sortByColumn(int c)
	{
		Collections.sort(comps, 
			new ComputationsListComparator(c, sortType[c]));
		sortedBy = c;
		fireTableDataChanged();
	}

	public void setContents(ArrayList<ComputationInList> comps)
	{
		this.comps = comps;
		fireTableDataChanged();
	}

	public Object getRowObject(int arg0) {
		return comps.get(arg0);
	}

	public int getRowCount() {
		return comps.size();
	}

	public int getColumnCount() {
		return columnNames.length;
	}
	
	public String getColumnName(int col)
	{
		return columnNames[col];
	}

	public Object getValueAt(int rowIndex, int columnIndex) 
	{
		ComputationInList cil = comps.get(rowIndex);
		if (cil != null)
			return getNlColumn(cil, columnIndex);
		else
			return "";
	}
		
	public static String getNlColumn(ComputationInList obj, int columnIndex) 
	{
		switch (columnIndex) {
		case 0:
			return "" + obj.getComputationId();
		case 1:
			return obj.getComputationName();
		case 2:
		  {
			String s = obj.getAlgorithmName();
			return s != null ? s :
				ComputationsListPanel.compLabels.getString(
					"ComputationsFilterPanel.N1ColumnNull");
		  }
		case 3:
			return obj.getProcessName();
		case 4:
			return String.valueOf(obj.isEnabled());
		case 5:
			return obj.getDescription();
		default:
			return "";
		}
	}

	public boolean compExists(String name)
	{
		for(ComputationInList dc : comps)
		{
			if (name.equalsIgnoreCase(dc.getComputationName()))
				return true;
		}
		return false;
	}
}

class ComputationsListComparator implements Comparator
{
	int column;
	char sortType;

	public ComputationsListComparator(int column, char sortType)
	{
		this.column = column;
		this.sortType = sortType;
	}

	/**
	 * Compare the eqMod names of the specified type.
	 */
	public int compare(Object ob1, Object ob2)
	{
		if (ob1 == ob2)
			return 0;
		ComputationInList ds1 = (ComputationInList)ob1;
		ComputationInList ds2 = (ComputationInList)ob2;

		String s1 = ComputationsListPanelTableModel.getNlColumn(ds1, column);
		String s2 = ComputationsListPanelTableModel.getNlColumn(ds2, column);
		if (sortType == 'i')
		{
			try
			{
				int i1 = Integer.parseInt(s1.trim());
				int i2 = Integer.parseInt(s2.trim());
				return i1 - i2;
			}
			catch(Exception ex) {}
		}
		if (s1 == null)
			s1 = "";
		if (s2 == null)
			s2 = "";	
		return s1.compareToIgnoreCase(s2);
	}

	public boolean equals(Object ob)
	{
		return false;
	}
}
