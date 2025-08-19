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
*  Revision 1.3  2017/10/03 12:33:42  mmaloney
*  Handle constraint exceptions
*
*  Revision 1.2  2015/06/04 21:43:22  mmaloney
*  Some refactoring to allow ProcessEditPanel under new Proc Monitor GUI
*
*  Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
*  OPENDCS 6.0 Initial Checkin
*
*  Revision 1.9  2013/04/18 19:03:39  mmaloney
*  When copying a loading app, also copy the properties.
*
*  Revision 1.8  2013/04/18 19:01:13  mmaloney
*  When copying a loading app, also copy the properties.
*
*  Revision 1.7  2013/03/21 18:27:39  mmaloney
*  DbKey Implementation
*
*/
package decodes.tsdb.compedit;

import ilex.util.Logger;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.PrintStream;
import java.util.Collections;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.Enumeration;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;

import opendcs.dai.LoadingAppDAI;
import decodes.gui.SortingListTable;
import decodes.gui.SortingListTableModel;
import decodes.sql.DbKey;
import decodes.tsdb.*;
import decodes.util.DecodesSettings;


public class ProcessesListPanel extends ListPanel 
{
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
			CAPEdit.instance().getFrame().showError(
				"Error attempting to delete process: " + ex);
			Logger.instance().warning("Error attempting to delete process: " + ex);
			PrintStream ps = Logger.instance().getLogOutput();
			if (ps != null)
				ex.printStackTrace(ps);
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

class ProcessesListPanelTableModel extends AbstractTableModel implements
		SortingListTableModel {

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
			CAPEdit.instance().getFrame().showError(CAPEdit.instance().compeditDescriptions
					.getString("ProcessListPanel.FillError")+ " " + ex);
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
			catch(Exception ex) {}
		}

		return ((String)s1).compareToIgnoreCase((String)s2);
	}

	public boolean equals(Object ob)
	{
		return false;
	}
}
