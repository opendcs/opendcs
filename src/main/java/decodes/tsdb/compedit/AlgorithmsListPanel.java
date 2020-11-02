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
*/
package decodes.tsdb.compedit;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import java.util.*;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;

import opendcs.dai.AlgorithmDAI;

import decodes.db.Constants;
import decodes.gui.SortingListTable;
import decodes.gui.SortingListTableModel;

import decodes.tsdb.*;

public class AlgorithmsListPanel extends ListPanel 
{
	JPanel getFieldPanel() {
		return getJContentPane();
	}

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
			jContentPane.add(getButtonPanel(), java.awt.BorderLayout.SOUTH);
			JScrollPane scrollPane = new JScrollPane(getAlgoListTable());

			jContentPane.add(scrollPane, java.awt.BorderLayout.CENTER);
		}
		return jContentPane;
	}

	protected JTable getAlgoListTable() 
	{
		if (algoListTableModel == null) 
		{
			algoListTableModel = new AlgoListTableModel();
			algoListTableModel.fill();
			algoListTable = new SortingListTable(algoListTableModel, 
				AlgoListTableModel.columnWidths);
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
		DbCompAlgorithm dca = 
			(DbCompAlgorithm)algoListTableModel.getRowObject(r);
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
		DbCompAlgorithm dca = 
			(DbCompAlgorithm)algoListTableModel.getRowObject(r);

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
		DbCompAlgorithm dca = 
			(DbCompAlgorithm)algoListTableModel.getRowObject(r);
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
