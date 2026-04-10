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
/*
*
*  This is open-source software written by ILEX Engineering, Inc., under
*  contract to the federal government. You are free to copy and use this
*  source code for your own purposes, except that no part of the information
*  contained in this file may be claimed to be proprietary.
*
*  Except for specific contractual terms between ILEX and the federal
*  government, this source code is provided completely without warranty.
*  For more information contact: info@ilexeng.com
*/
package decodes.tsdb.compedit;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import java.util.*;

import javax.swing.*;
import javax.swing.table.TableRowSorter;

import org.opendcs.gui.SearchPanel;
import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import opendcs.dai.AlgorithmDAI;

import decodes.db.Constants;

import decodes.tsdb.*;
import decodes.tsdb.compedit.algotab.LoadNewDialog;

public class AlgorithmsListPanel extends ListPanel
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();

	private JPanel jContentPane = null;

	JTable algoListTable = null;

	AlgoListTableModel algoListTableModel = null;


	private String openErr;
	private String newInputText;
	private String newError;
	private String cpyErr1;
	private String cpyErr2;
	private String cpyInput;
	private String deleteErr1;
	private String deleteErr2;

	AlgorithmsListPanel() {
		setLayout(new BorderLayout());
		fillLabels();
		this.add(getJContentPane(), java.awt.BorderLayout.CENTER);
	}

	private void fillLabels()
	{
		openErr = CAPEdit.instance().compeditDescriptions.getString("AlgorithmsListPanel.OpenError");
		newInputText = CAPEdit.instance().compeditDescriptions.getString("AlgorithmsListPanel.NewInputText");
		newError = CAPEdit.instance().compeditDescriptions.getString("AlgorithmsListPanel.NewError");
		cpyErr1 = CAPEdit.instance().compeditDescriptions.getString("AlgorithmsListPanel.CopyError1");
		cpyErr2 = CAPEdit.instance().compeditDescriptions.getString("AlgorithmsListPanel.CopyError2");
		cpyInput = CAPEdit.instance().compeditDescriptions.getString("AlgorithmsListPanel.CopyInput");
		deleteErr1 = CAPEdit.instance().compeditDescriptions.getString("AlgorithmsListPanel.DeleteError1");
		deleteErr2 = CAPEdit.instance().compeditDescriptions.getString("AlgorithmsListPanel.DeleteError2");
	}

	/**
	 * This method initializes jContentPane
	 *
	 * @return javax.swing.JPanel
	 */
	protected JPanel getJContentPane()
	{
		if (jContentPane == null)
		{
			jContentPane = new JPanel();
			jContentPane.setLayout(new BorderLayout());
			JPanel buttonPanel = getButtonPanel();
			JButton checkForNew = new JButton(ResourceBundle.getBundle("ilex/resources/gui").getString("EditorMenuSet.checkForNew"));
			checkForNew.addActionListener(e -> checkForNew());
			buttonPanel.add(checkForNew);
			jContentPane.add(buttonPanel, java.awt.BorderLayout.SOUTH);

			getAlgoListTable();
			JScrollPane scrollPane = new JScrollPane(algoListTable);
			TableRowSorter<AlgoListTableModel> sorter;
			sorter = new TableRowSorter<>(algoListTableModel);
			algoListTable.setRowSorter(sorter);
			sorter.setSortKeys(
					Collections.singletonList(
							new RowSorter.SortKey(1, SortOrder.ASCENDING)
					)
			);
			algoListTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

			SearchPanel searchPanel = new SearchPanel(sorter, algoListTableModel);
			this.add(searchPanel,BorderLayout.NORTH);

			jContentPane.add(scrollPane, java.awt.BorderLayout.CENTER);
		}
		return jContentPane;
	}

	private void checkForNew()
	{

		LoadNewDialog loadNew = new LoadNewDialog(CAPEdit.instance().getFrame(), CAPEdit.theDb);
		log.info("Starting Load new dialog");
		if (loadNew.importNew())
		{
			new SwingWorker<Void,Void>()
			{
				@Override
				protected Void doInBackground() throws Exception
				{
					AlgorithmsListPanel.this.algoListTableModel.fill();
					return null;
				}
			}.execute();
		}
	}


	protected JTable getAlgoListTable()
	{
		if (algoListTableModel == null)
		{
			algoListTableModel = new AlgoListTableModel();
			algoListTableModel.fill();
			algoListTable = new JTable(algoListTableModel);


			algoListTable.addMouseListener(
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
		return algoListTable;
	}

	protected void doOpen()
	{
		int r = algoListTable.getSelectedRow();
		if (r == -1)
		{
			showError(openErr);
			return;
		}
		//Get the correct row from the table model
		int modelrow = algoListTable.convertRowIndexToModel(r);
		AlgoListTableModel tablemodel = (AlgoListTableModel)algoListTable.getModel();
		DbCompAlgorithm dca = tablemodel.getRowAlgorithm(modelrow);
		openEditTab(dca);
	}

	private void openEditTab(DbCompAlgorithm dca)
	{
		JTabbedPane tabbedPane =
			CAPEdit.instance().getAlgorithmsTab();
		int n = tabbedPane.getTabCount();
		for(int idx = 1; idx<n; idx++)
		{
			Component c = tabbedPane.getComponentAt(idx);
			if (c instanceof AlgorithmsEditPanel)
			{
				AlgorithmsEditPanel aep = (AlgorithmsEditPanel)c;
				DbCompAlgorithm eo = aep.getEditedObject();
				if (eo.getId() != Constants.undefinedId && eo.getId() == dca.getId())
				{
					tabbedPane.setSelectedIndex(idx);
					return;
				}
			}
		}
		AlgorithmsEditPanel aep = new AlgorithmsEditPanel();
		aep.setEditedObject(dca);
		tabbedPane.addTab(dca.getName(), null, aep);
		tabbedPane.setSelectedIndex(n);
	}

	protected void doNew()
	{
	    String newName = JOptionPane.showInputDialog(newInputText);
		if (newName == null)
			return;
		if (algoListTableModel.existsInList(newName))
		{
			CAPEdit.instance().getFrame().showError(
				newError);
			return;
		}
		DbCompAlgorithm dca = new DbCompAlgorithm(newName);
		openEditTab(dca);
	}

	protected void doCopy()
	{
		int r = algoListTable.getSelectedRow();
		if (r == -1)
		{
			CAPEdit.instance().getFrame().showError(cpyErr1);
			return;
		}
		//Get the correct row from the table model
		int modelrow = algoListTable.convertRowIndexToModel(r);
		AlgoListTableModel tablemodel = (AlgoListTableModel)algoListTable.getModel();
		DbCompAlgorithm dca = tablemodel.getRowAlgorithm(modelrow);

	    String newName = JOptionPane.showInputDialog(cpyInput);
		if (newName == null)
			return;
		if (algoListTableModel.existsInList(newName))
		{
			showError(cpyErr2);
			return;
		}

		DbCompAlgorithm copydca = dca.copyNoId();
		copydca.setName(newName);
		openEditTab(copydca);
	}

	protected void doDelete()
	{
		int r = algoListTable.getSelectedRow();
		if (r == -1)
		{
			CAPEdit.instance().getFrame().showError(
				deleteErr1);
			return;
		}
		//Get the correct row from the table model
		int modelrow = algoListTable.convertRowIndexToModel(r);
		AlgoListTableModel tablemodel = (AlgoListTableModel)algoListTable.getModel();
		DbCompAlgorithm dca = tablemodel.getRowAlgorithm(modelrow);

		AlgorithmDAI algorithmDao = CAPEdit.instance().theDb.makeAlgorithmDAO();
		try
		{
			algorithmDao.deleteAlgorithm(dca.getId());
			algoListTableModel.fill();
		}
		catch(Exception ex)
		{
			CAPEdit.instance().getFrame().showError(
				deleteErr2 + dca.getName() + "': "
				+ ex.getMessage());
		}
		finally
		{
			algorithmDao.close();
		}
	}

}
