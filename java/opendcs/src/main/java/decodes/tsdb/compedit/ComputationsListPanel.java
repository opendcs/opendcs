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
*  Revision 1.3  2016/07/20 15:43:56  mmaloney
*  remove unneeded loading app dao.
*
*  Revision 1.2  2016/01/27 22:06:16  mmaloney
*  Optimizations for filter.
*
*  Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
*  OPENDCS 6.0 Initial Checkin
*
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


import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.ResourceBundle;
import java.util.Vector;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;

import java.util.List;

import opendcs.dai.ComputationDAI;
import opendcs.dai.LoadingAppDAI;
import ilex.util.*;
import decodes.db.Constants;
import decodes.gui.SortingListTable;
import decodes.gui.TopFrame;
import decodes.sql.DbKey;
import decodes.tsdb.*;
import decodes.tsdb.compedit.computations.ComputationsListPanelTableModel;

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
			filterPanel.getRefresh().addActionListener(e -> doRefresh());

			jContentPane.add(scrollPane, java.awt.BorderLayout.CENTER);
			jContentPane.add(filterPanel, BorderLayout.NORTH);
		}
		return jContentPane;
	}

	protected JTable getCompListTable() 
	{
		if (compListTableModel == null) 
		{
			String columnNames[] = 
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
			compListTableModel = new ComputationsListPanelTableModel(columnNames);
			compListTable = new JTable(compListTableModel);
			compListTable.setAutoCreateRowSorter(true);
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
		int rowModel = compListTable.convertRowIndexToModel(r);
		if (rowModel== -1)
		{
			parentFrame.showError(
				compLabels.getString("ComputationsFilterPanel.OpenError"));
			return;
		}
		DbComputation toOpen = compListTableModel.getCompAt(rowModel);
		
		openEditTab(toOpen);
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
				if (eo.getId() != Constants.undefinedId && eo.getId().equals(dc.getId()))
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
		int rowModel = compListTable.convertRowIndexToModel(r);
		DbComputation dc = compListTableModel.getCompAt(rowModel);

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
		
		DbComputation copydc = dc.copyNoId();
		copydc.setName(newName);
		openEditTab(copydc);
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
		int rowModel = compListTable.convertRowIndexToModel(r);
		DbComputation dc = compListTableModel.getCompAt(rowModel);

		int ok = JOptionPane.showConfirmDialog(this,
			LoadResourceBundle.sprintf(
				compLabels.getString("ComputationListPanel.VerifyDelete"),
				dc.getName()));
		if (ok != JOptionPane.YES_OPTION)
			return;

		SwingUtilities.invokeLater(() ->
		{
			try (ComputationDAI computationDAO = tsdb.makeComputationDAO())
			{
				computationDAO.deleteComputation(dc.getId());
				doRefresh();
			}
			catch(Exception ex)
			{
				CAPEdit.instance().getFrame().showError(
					compLabels.getString(
						"ComputationsFilterPanel.DeleteError2") 
						+ dc.getName() + "': " + ex);
			}
		});
		
	}

	void doRefresh()
	{
		Logger.instance().debug1("ComputationListPanel.doRefresh() ------------");
		
		SwingUtilities.invokeLater(() -> 
		{
			CompFilter compFilter = new CompFilter();
			
			compFilter.setFilterLowIds(filterLowIds);
			filterPanel.setFilterParams(compFilter, tsdb);
			try (ComputationDAI computationDAO = tsdb.makeComputationDAO())
			{
				List<DbComputation> displayComps = computationDAO.listComps(c -> compFilter.passes(c));
				compListTableModel.setContents(displayComps);
				
			}
			catch(Exception ex)
			{
				String msg = "Cannot refresh computation list: " + ex;
				System.err.println(msg);
				ex.printStackTrace(System.err);
				parentFrame.showError(msg);
			}
		});
	}

	public Vector<DbComputation> getSelectedComputations()
	{
		Vector<DbComputation> ret = new Vector<DbComputation>();
		int[] selected = compListTable.getSelectedRows();
		
		for(int x : selected)
		{
			int modelRow = compListTable.convertRowIndexToModel(x);
			DbComputation dc = compListTableModel.getCompAt(modelRow);
			ret.add(dc);
		}
		return ret;
	}
}
