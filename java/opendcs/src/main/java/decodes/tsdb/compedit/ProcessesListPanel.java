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
import java.io.PrintStream;
import java.util.Collections;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.Enumeration;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import opendcs.dai.LoadingAppDAI;
import decodes.gui.SortingListTable;
import decodes.gui.SortingListTableModel;
import decodes.sql.DbKey;
import decodes.tsdb.*;
import decodes.util.DecodesSettings;


public class ProcessesListPanel extends ListPanel
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	private JPanel jContentPane = null;

	private JTable procTable = null;

	ProcessesListPanelTableModel procTableModel = null;

	ProcessesListPanel() {
		setLayout(new BorderLayout());
		this.add(getJContentPane(), java.awt.BorderLayout.CENTER);
	}

	/**
	 * This method initializes jContentPane
	 *
	 * @return javax.swing.JPanel
	 */
	protected JPanel getJContentPane() {
		if (jContentPane == null) {
			jContentPane = new JPanel();
			jContentPane.setLayout(new BorderLayout());
			jContentPane.add(getButtonPanel(), java.awt.BorderLayout.SOUTH);
			JScrollPane scrollPane = new JScrollPane(getProcTable());

			jContentPane.add(scrollPane, java.awt.BorderLayout.CENTER);
		}
		return jContentPane;
	}

	protected JTable getProcTable()
	{
		if (procTableModel == null)
		{
			procTableModel = new ProcessesListPanelTableModel();
			procTableModel.fill();
			procTable = new SortingListTable(procTableModel,
				procTableModel.columnWidths);
			procTable.addMouseListener(
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
		return procTable;
	}

	protected void doOpen()
	{
		int r = procTable.getSelectedRow();
		if (r == -1)
		{
			CAPEdit.instance().getFrame().showError(
					CAPEdit.instance().compeditDescriptions
					.getString("ProcessListPanel.OpenError"));
			return;
		}
		//Get the correct row from the table model
		int modelrow = procTable.convertRowIndexToModel(r);
		ProcessesListPanelTableModel tablemodel = (ProcessesListPanelTableModel)procTable.getModel();
		CompAppInfo cai = (CompAppInfo)tablemodel.getRowObject(modelrow);
		openEditTab(cai);
	}


	protected void doNew()
	{
	    String newName = JOptionPane.showInputDialog(
	    		CAPEdit.instance().compeditDescriptions
				.getString("ProcessListPanel.NewInput"));
		if (newName == null)
			return;
		if (procTableModel.findByName(newName) != null)
		{
			CAPEdit.instance().getFrame().showError(
					CAPEdit.instance().compeditDescriptions
					.getString("ProcessListPanel.NewError"));
			return;
		}
		CompAppInfo cai = new CompAppInfo();
		cai.setAppName(newName);
		openEditTab(cai);
	}

	protected void doCopy()
	{
		int r = procTable.getSelectedRow();
		if (r == -1)
		{
			CAPEdit.instance().getFrame().showError(
					CAPEdit.instance().compeditDescriptions
					.getString("ProcessListPanel.CopyError1"));
			return;
		}
		CompAppInfo existingCai = (CompAppInfo)procTableModel.getRowObject(r);

	    String newName = JOptionPane.showInputDialog(
	    		CAPEdit.instance().compeditDescriptions
				.getString("ProcessListPanel.CopyInput"));
		if (newName == null)
			return;
		if (procTableModel.findByName(newName) != null)
		{
			CAPEdit.instance().getFrame().showError(
					CAPEdit.instance().compeditDescriptions
					.getString("ProcessListPanel.CopyError2"));
			return;
		}
		CompAppInfo cai = new CompAppInfo();
		cai.setAppName(newName);
		cai.setComment(existingCai.getComment());
		for(Enumeration propen = existingCai.getPropertyNames(); propen.hasMoreElements();)
		{
			String nm = (String)propen.nextElement();
			cai.setProperty(nm, existingCai.getProperty(nm));
		}
		openEditTab(cai);
	}

	private void openEditTab(CompAppInfo cai)
	{
		JTabbedPane tabbedPane =
			CAPEdit.instance().getProcessesTab();
		int n = tabbedPane.getTabCount();
		for(int idx = 1; idx<n; idx++)
		{
			Component c = tabbedPane.getComponentAt(idx);
			if (c instanceof ProcessEditPanel)
			{
				ProcessEditPanel pep = (ProcessEditPanel)c;
				CompAppInfo eo = pep.getEditedObject();
				if (eo == cai)
				{
					tabbedPane.setSelectedIndex(idx);
					return;
				}
			}
		}
		ProcessEditPanel pep = new ProcessEditPanel(this);
		pep.setTopFrame(CAPEdit.instance().getFrame());
		pep.setEditedObject(cai);
		tabbedPane.addTab(cai.getAppName(), null, pep);
		tabbedPane.setSelectedIndex(n);
	}

	protected void doDelete()
	{
		int r = procTable.getSelectedRow();
		if (r == -1)
		{
			CAPEdit.instance().getFrame().showError(
					CAPEdit.instance().compeditDescriptions
					.getString("ProcessListPanel.CopyError1"));
			return;
		}

		LoadingAppDAI loadingAppDao = CAPEdit.instance().getTimeSeriesDb().makeLoadingAppDAO();
		try
		{
			CompAppInfo app = (CompAppInfo)procTableModel.getRowObject(r);
			loadingAppDao.deleteComputationApp(app);
		}
		catch (Exception ex)
		{
			final String msg = "Error attempting to delete process";
			log.atWarn().setCause(ex).log(msg);
			CAPEdit.instance().getFrame().showError(msg + ": " + ex);
		}
		finally
		{
			loadingAppDao.close();
		}
	}

	CompAppInfo findByName(String procname)
	{
		return procTableModel.findByName(procname);
	}
}

class ProcessesListPanelTableModel extends AbstractTableModel implements SortingListTableModel
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	ArrayList<CompAppInfo> myvector = new ArrayList<CompAppInfo>();
	static String columnNames[] = {
		CAPEdit.instance().compeditDescriptions
		.getString("ProcessListPanel.TableColumn1"),
		CAPEdit.instance().compeditDescriptions
		.getString("ProcessListPanel.TableColumn2"),
		CAPEdit.instance().compeditDescriptions
		.getString("ProcessListPanel.TableColumn3"),
		CAPEdit.instance().compeditDescriptions
		.getString("ProcessListPanel.TableColumn4") };
	static int columnWidths[] = { 10, 25, 10, 55 };

	static Class[] sortType = {Integer.class, String.class, Integer.class, String.class};
	int sortedBy = -1;
	int minProcId = -1;

	public ProcessesListPanelTableModel()
	{
		super();
		//Get the DbComp config - tsdb.conf file. This file will contain
		//the minProcId property which is used to determine the processes
		//that will be displayed on the list
		minProcId = DecodesSettings.instance().minProcId;
	}

	public void sortByColumn(int c)
	{
		Collections.sort(myvector, new ProcessesListComparator(c, sortType[c]));
		sortedBy = c;
		fireTableDataChanged();
	}

	@Override
	public Class<?> getColumnClass(int columnIndex) {
		if (myvector.isEmpty()) {
			return Object.class;
		}
		return getValueAt(0, columnIndex).getClass();
	}

	public void fill()
	{
		TimeSeriesDb tsdb = CAPEdit.instance().getTimeSeriesDb();
		myvector.clear();
		LoadingAppDAI loadingAppDao = tsdb.makeLoadingAppDAO();
		try
		{
			ArrayList<CompAppInfo> tmpList = loadingAppDao.listComputationApps(false);
			for(CompAppInfo cai : tmpList)
			{
				//Verify if this process will be displayed on the list
				if (cai.getAppId().getValue() >= minProcId)
					myvector.add(cai);
			}

			if (sortedBy != -1)
				sortByColumn(sortedBy);
		}
		catch(DbIoException ex)
		{
			log.atError().setCause(ex).log(CAPEdit.instance().compeditDescriptions
					.getString("ProcessListPanel.FillError"));
			CAPEdit.instance().getFrame().showError(CAPEdit.instance().compeditDescriptions
					.getString("ProcessListPanel.FillError") + " " + ex);
		}
		finally
		{
			loadingAppDao.close();
		}

		fireTableDataChanged();
	}

	public Object getRowObject(int arg0) {
		return myvector.get(arg0);
	}

	public int getRowCount() {
		return myvector.size();
	}

	public int getColumnCount() {
		return columnNames.length;
	}

	public String getColumnName(int col)
	{
		return columnNames[col];
	}

	public Object getValueAt(int rowIndex, int columnIndex) {
		if(myvector.get(rowIndex)!=null)
			return getNlColumn(myvector.get(rowIndex), columnIndex);
		else
			return "";
	}

	public CompAppInfo findByName(String procname)
	{
		for(CompAppInfo cai : myvector)
		{
			if (procname.equalsIgnoreCase(cai.getAppName()))
				return cai;
		}
		return null;
	}

	public String getNameById(DbKey appId)
	{
		for(CompAppInfo cai : myvector)
		{
			if (cai.getAppId().equals(appId))
				return cai.getAppName();
		}
		return "";

	}


	public static Object getNlColumn(CompAppInfo obj, int columnIndex) {
		switch (columnIndex) {
		case 0:
			return obj.getAppId().getValue();
		case 1:
			return obj.getAppName();
		case 2:
			return Integer.valueOf(obj.getNumComputations());
		case 3:
			return obj.getComment();
		default:
			return "";
		}
	}

	private static String getFirstLine(String tmp)
	{
		if (tmp == null)
			return "";
		int len = tmp.length();
		int ci = len;
		if (ci > 60)
			ci = 60;
		int i = tmp.indexOf('\r');
		if (i > 0 && i < ci)
			ci = i;
		i = tmp.indexOf('\n');
		if (i > 0 && i < ci)
			ci = i;
		i = tmp.indexOf('.');
		if (i > 0 && i < ci)
			ci = i;

		if (ci < len)
			return tmp.substring(0,ci);
		else
			return tmp;
	}

}

class ProcessesListComparator implements Comparator<CompAppInfo>
{
	int column;
	Class<?> sortType;

	public ProcessesListComparator(int column, Class<?> sortType)
	{
		this.column = column;
		this.sortType = sortType;
	}

	/**
	 * Compare the eqMod names of the specified type.
	 */
	public int compare(CompAppInfo ds1, CompAppInfo ds2)
	{
		if (ds1 == ds2)
			return 0;

		Object s1 = ProcessesListPanelTableModel.getNlColumn(ds1, column);
		Object s2 = ProcessesListPanelTableModel.getNlColumn(ds2, column);
		if (sortType == Integer.class)
		{
			try
			{
				int i1 = (Integer)s1;
				int i2 = (Integer)s2;
				return i1 - i2;
			}
			catch(Exception ex)
			{
				/* fall through to string compare */
			}
		}

		return ((String)s1).compareToIgnoreCase((String)s2);
	}

	public boolean equals(Object ob)
	{
		return false;
	}
}
