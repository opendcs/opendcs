/*
* Where Applicable, Copyright 2025 OpenDCS Consortium and/or its contributors
*
* Licensed under the Apache License, Version 2.0 (the "License"); you may not
* use this file except in compliance with the License. You may obtain a copy
* of the License at
*
*   http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
* License for the specific language governing permissions and limitations
* under the License.
*/

package decodes.tsdb.compedit;


import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ResourceBundle;
import java.util.Vector;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.TableRowSorter;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import java.util.List;

import opendcs.dai.ComputationDAI;
import decodes.db.Constants;
import decodes.gui.TopFrame;import decodes.tsdb.*;
import decodes.tsdb.compedit.computations.ComputationsListPanelTableModel;
import ilex.util.LoadResourceBundle;

@SuppressWarnings("serial")
public class ComputationsListPanel extends ListPanel
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	private JPanel jContentPane = null;
	private JTable compListTable = null;
	private TableRowSorter<ComputationsListPanelTableModel> sorter = null;
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
		doRefresh();
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

			filterPanel = new ComputationsFilterPanel(tsdb, parentFrame, compListTableModel,
													  sorter, filterLowIds);
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
			sorter = new TableRowSorter<>(compListTableModel);
			compListTable.setRowSorter(sorter);
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
				String msg = compLabels.getString(
						"ComputationsFilterPanel.DeleteError2");
				log.atError().setCause(ex).log(msg + dc.getName() + "'");
				CAPEdit.instance().getFrame().showError(msg	+ dc.getName() + "': " + ex);
			}
		});

	}

	void doRefresh()
	{
		log.debug("ComputationListPanel.doRefresh() ------------");

		SwingUtilities.invokeLater(() ->
		{
			try (ComputationDAI computationDAO = tsdb.makeComputationDAO())
			{
				List<DbComputation> displayComps = computationDAO.listComps(c -> true);
				compListTableModel.setContents(displayComps);
			}
			catch(Exception ex)
			{
				String msg = "Cannot refresh computation list";
				log.atError().setCause(ex).log(msg);
				parentFrame.showError(msg + ": " + ex);
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