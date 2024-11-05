/**
 * $Id$
 * 
 * Copyright 2017 Cove Software, LLC. All rights reserved.
 * 
 * $Log$
 * Revision 1.1  2019/03/05 14:52:59  mmaloney
 * Checked in partial implementation of Alarm classes.
 *
 * Revision 1.3  2018/03/23 20:12:20  mmaloney
 * Added 'Enabled' flag for process and file monitors.
 *
 * Revision 1.2  2017/05/18 12:29:00  mmaloney
 * Code cleanup. Remove System.out debugs.
 *
 * Revision 1.1  2017/05/17 20:36:57  mmaloney
 * First working version.
 *
 */
package decodes.tsdb.alarm.editor;

import java.awt.*;
import java.awt.event.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.TimeZone;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;
import javax.swing.table.AbstractTableModel;

import opendcs.dai.AlarmDAI;
import opendcs.dao.AlarmDAO;
import ilex.util.Logger;
import ilex.util.TextUtil;
import decodes.gui.SortingListTable;
import decodes.gui.SortingListTableModel;
import decodes.sql.DbKey;
import decodes.sql.SqlDatabaseIO;
import decodes.tsdb.DbIoException;
import decodes.tsdb.alarm.AlarmGroup;
import decodes.tsdb.alarm.EmailAddr;
import decodes.tsdb.alarm.FileMonitor;
import decodes.tsdb.alarm.ProcessMonitor;
import decodes.util.DecodesSettings;
import decodes.db.Database;

@SuppressWarnings("serial")
public class AlarmEditPanel 
	extends JPanel
{
	private JTextField nameField = new JTextField();
	private JTextField lastModifiedField = new JTextField();
	AlarmEditFrame parentFrame = null;
	private SortingListTable fileMonTable = null, procMonTable = null;
	private FileMonTableModel fileMonModel = null;
	private ProcMonTableModel procMonModel = null;
	private DefaultListModel<String> emailListModel = new DefaultListModel<String>();
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private JList emailList = new JList(emailListModel);
	private AlarmGroup editedGroup = null;
	private SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd-HH:mm:ss");
	boolean changed = false;
	
	public AlarmEditPanel(AlarmEditFrame parentFrame)
	{
		this.parentFrame = parentFrame;
		setLayout(new BorderLayout());
		sdf.setTimeZone(TimeZone.getTimeZone(DecodesSettings.instance().guiTimeZone));

		guiInit();
	}
	
	private void guiInit()
	{
		JPanel northPanel = new JPanel(new GridBagLayout());
		northPanel.add(new JLabel(parentFrame.genericLabels.getString("name") + ":"),
			new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(2, 10, 2, 1), 0, 0));
		northPanel.add(nameField,
			new GridBagConstraints(1, 0, 1, 1, 0.8, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
				new Insets(2, 0, 2, 5), 0, 0));
		nameField.setEditable(false);
		JButton changeNameButton = new JButton(parentFrame.genericLabels.getString("change"));
		changeNameButton.addActionListener(
			new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					changeNamePressed();
				}
			});
		northPanel.add(changeNameButton,
			new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(2, 5, 2, 20), 0, 0));
		northPanel.add(new JLabel(parentFrame.genericLabels.getString("lastMod") + ":"),
			new GridBagConstraints(3, 0, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(2, 5, 2, 1), 0, 0));
		northPanel.add(lastModifiedField,
			new GridBagConstraints(4, 0, 1, 1, 0.2, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
				new Insets(2, 0, 2, 2), 0, 0));
		lastModifiedField.setEditable(false);
		northPanel.add(new JLabel(DecodesSettings.instance().guiTimeZone),
			new GridBagConstraints(5, 0, 1, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(2, 0, 2, 10), 0, 0));
		this.add(northPanel, BorderLayout.NORTH);
		
		JPanel southPanel = new JPanel(new FlowLayout());
		JButton commitButton = new JButton(parentFrame.genericLabels.getString("commit"));
		commitButton.addActionListener(
			new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					commitPressed();
				}
			});
		southPanel.add(commitButton);
		JButton closeButton = new JButton(parentFrame.genericLabels.getString("close"));
		closeButton.addActionListener(
			new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					closePressed();
				}
			});
		southPanel.add(closeButton);
		this.add(southPanel, BorderLayout.SOUTH);

		JPanel centerPanel = new JPanel(new GridBagLayout());
		this.add(centerPanel, BorderLayout.CENTER);
		
		JPanel emailListPanel = new JPanel(new BorderLayout());
		emailListPanel.setBorder(new TitledBorder(parentFrame.eventmonLabels.getString("emailAddrs")));
		centerPanel.add(emailListPanel,
			new GridBagConstraints(0, 0, 1, 1, 1.0, 0.3,
				GridBagConstraints.WEST, GridBagConstraints.BOTH,
				new Insets(2, 2, 2, 2), 0, 0));
		JScrollPane emailScrollPane = new JScrollPane(emailList,
			JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		emailListPanel.add(emailScrollPane, BorderLayout.CENTER);
		
		JPanel emailButtonPanel = new JPanel(new GridBagLayout());
		emailListPanel.add(emailButtonPanel, BorderLayout.EAST);
		JButton addEmailButton = new JButton(parentFrame.genericLabels.getString("add"));
		addEmailButton.addActionListener(
			new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					addEmailPressed();
				}
			});
		emailButtonPanel.add(addEmailButton,
			new GridBagConstraints(0, 0, 1, 1, 1.0, 0.5,
				GridBagConstraints.SOUTH, GridBagConstraints.HORIZONTAL,
				new Insets(2, 2, 2, 2), 0, 0));
		
		JButton editEmailButton = new JButton(parentFrame.genericLabels.getString("edit"));
		editEmailButton.addActionListener(
			new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					editEmailPressed();
				}
			});
		emailButtonPanel.add(editEmailButton,
			new GridBagConstraints(0, 1, 1, 1, 1.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
				new Insets(2, 2, 2, 2), 0, 0));
		
		JButton delEmailButton = new JButton(parentFrame.genericLabels.getString("delete"));
		delEmailButton.addActionListener(
			new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					delEmailPressed();
				}
			});
		emailButtonPanel.add(delEmailButton,
			new GridBagConstraints(0, 2, 1, 1, 1.0, 0.5,
				GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL,
				new Insets(2, 2, 2, 2), 0, 0));

		emailList.addMouseListener(
			new MouseAdapter()
			{
				public void mouseClicked(MouseEvent e)
				{
					if (e.getClickCount() == 2)
					{
						editEmailPressed();
					}
				}
			});

		// ===========
		JPanel fileMonPanel = new JPanel(new BorderLayout());
		fileMonPanel.setBorder(new TitledBorder(parentFrame.eventmonLabels.getString("fileMonitors")));
		centerPanel.add(fileMonPanel,
			new GridBagConstraints(0, 1, 1, 1, 1.0, 0.3,
				GridBagConstraints.WEST, GridBagConstraints.BOTH,
				new Insets(2, 2, 2, 2), 0, 0));
		fileMonModel = new FileMonTableModel(this);
		fileMonTable = new SortingListTable(fileMonModel, fileMonModel.widths);
		JScrollPane fileMonScrollPane = new JScrollPane(fileMonTable,
			JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		fileMonPanel.add(fileMonScrollPane, BorderLayout.CENTER);
		
		JPanel fileMonButtonPanel = new JPanel(new GridBagLayout());
		fileMonPanel.add(fileMonButtonPanel, BorderLayout.EAST);
		JButton addFileMonButton = new JButton(parentFrame.genericLabels.getString("add"));
		addFileMonButton.addActionListener(
			new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					addFileMonPressed();
				}
			});
		fileMonButtonPanel.add(addFileMonButton,
			new GridBagConstraints(0, 0, 1, 1, 1.0, 0.5,
				GridBagConstraints.SOUTH, GridBagConstraints.HORIZONTAL,
				new Insets(2, 2, 2, 2), 0, 0));
		
		JButton editFileMonButton = new JButton(parentFrame.genericLabels.getString("edit"));
		editFileMonButton.addActionListener(
			new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					editFileMonPressed();
				}
			});
		fileMonButtonPanel.add(editFileMonButton,
			new GridBagConstraints(0, 1, 1, 1, 1.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
				new Insets(2, 2, 2, 2), 0, 0));
		
		JButton delFileMonButton = new JButton(parentFrame.genericLabels.getString("delete"));
		delFileMonButton.addActionListener(
			new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					delFileMonPressed();
				}
			});
		fileMonButtonPanel.add(delFileMonButton,
			new GridBagConstraints(0, 2, 1, 1, 1.0, 0.5,
				GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL,
				new Insets(2, 2, 2, 2), 0, 0));
		
		fileMonTable.addMouseListener(
			new MouseAdapter()
			{
				public void mouseClicked(MouseEvent e)
				{
					if (e.getClickCount() == 2)
					{
						editFileMonPressed();
					}
				}
			});
		
		//==============================
		JPanel procMonPanel = new JPanel(new BorderLayout());
		procMonPanel.setBorder(new TitledBorder(
			parentFrame.eventmonLabels.getString("procMonitors")));
		centerPanel.add(procMonPanel,
			new GridBagConstraints(0, 2, 1, 1, 1.0, 0.3,
				GridBagConstraints.WEST, GridBagConstraints.BOTH,
				new Insets(2, 2, 2, 2), 0, 0));
		procMonModel = new ProcMonTableModel(this);
		procMonTable = new SortingListTable(procMonModel, procMonModel.widths);
		JScrollPane procMonScrollPane = new JScrollPane(procMonTable,
			JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		procMonPanel.add(procMonScrollPane, BorderLayout.CENTER);
		
		JPanel procMonButtonPanel = new JPanel(new GridBagLayout());
		procMonPanel.add(procMonButtonPanel, BorderLayout.EAST);
		JButton addProcMonButton = new JButton(parentFrame.genericLabels.getString("add"));
		addProcMonButton.addActionListener(
			new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					addProcMonPressed();
				}
			});
		procMonButtonPanel.add(addProcMonButton,
			new GridBagConstraints(0, 0, 1, 1, 1.0, 0.5,
				GridBagConstraints.SOUTH, GridBagConstraints.HORIZONTAL,
				new Insets(2, 2, 2, 2), 0, 0));
		
		JButton editProcMonButton = new JButton(parentFrame.genericLabels.getString("edit"));
		editProcMonButton.addActionListener(
			new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					editProcMonPressed();
				}
			});
		procMonButtonPanel.add(editProcMonButton,
			new GridBagConstraints(0, 1, 1, 1, 1.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
				new Insets(2, 2, 2, 2), 0, 0));
		
		JButton delProcMonButton = new JButton(parentFrame.genericLabels.getString("delete"));
		delProcMonButton.addActionListener(
			new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					delProcMonPressed();
				}
			});
		procMonButtonPanel.add(delProcMonButton,
			new GridBagConstraints(0, 2, 1, 1, 1.0, 0.5,
				GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL,
				new Insets(2, 2, 2, 2), 0, 0));
		
		procMonTable.addMouseListener(
			new MouseAdapter()
			{
				public void mouseClicked(MouseEvent e)
				{
					if (e.getClickCount() == 2)
					{
						editProcMonPressed();
					}
				}
			});

	}
	
	protected void changeNamePressed()
	{
		String name = parentFrame.groupListPanel.askUniqueName();
		if (name == null)
			return;
		
		nameField.setText(name);
		changed = true;
	}

	public void setData(AlarmGroup grp)
	{
		this.editedGroup = grp;
		nameField.setText(grp.getName());
		lastModifiedField.setText(
			editedGroup.getLastModifiedMsec() == 0L ? "(never)"
			: sdf.format(new Date(editedGroup.getLastModifiedMsec())));
		emailListModel.clear();
		for(EmailAddr addr : grp.getEmailAddrs())
			emailListModel.addElement(addr.getAddr());
		fileMonModel.set(grp);
		procMonModel.set(grp);
		changed = false;
	}
	
	
	protected void delProcMonPressed()
	{
		int idx = procMonTable.getSelectedRow();
		if (idx == -1)
		{
			parentFrame.showError(parentFrame.eventmonLabels.getString("selectPmBeforeDelete"));
			return;
		}
		procMonModel.delete(idx);
		changed = true;
	}

	protected void editProcMonPressed()
	{
		int idx = procMonTable.getSelectedRow();
		if (idx == -1)
		{
			parentFrame.showError(parentFrame.eventmonLabels.getString("selectPmBeforeEdit"));
			return;
		}
		ProcessMonitor pm = procMonModel.getProcMonAt(idx);
		ProcessMonitorDialog dlg = new ProcessMonitorDialog(this);
		dlg.setData(pm);
		parentFrame.launchDialog(dlg);
		if (dlg.isChanged())
		{
			procMonModel.sortByColumn(procMonModel.sortColumn);
			changed = true;
		}
	}

	protected void addProcMonPressed()
	{
		ProcessMonitor pm = new ProcessMonitor(DbKey.NullKey);
		ProcessMonitorDialog dlg = new ProcessMonitorDialog(this);
		dlg.setData(pm);
		parentFrame.launchDialog(dlg);
		if (dlg.isChanged())
		{
			procMonModel.add(pm);
			changed = true;
		}
	}

	protected void delEmailPressed()
	{
		int idx = emailList.getSelectedIndex();
		if (idx < 0)
		{
			parentFrame.showError(parentFrame.eventmonLabels.getString("selectBeforeDelete") + "");
			return;
		}
		
		emailListModel.remove(idx);
		changed = true;
	}

	protected void editEmailPressed()
	{
		int idx = emailList.getSelectedIndex();
		if (idx == -1)
		{
			parentFrame.showError(
				parentFrame.eventmonLabels.getString("selectBeforeEdit"));
			return;
		}
		String oldAddr = emailListModel.get(idx);
		String addr = JOptionPane.showInputDialog(parentFrame,
			parentFrame.eventmonLabels.getString("enterEmail"), oldAddr);
		if (addr == null || addr.trim().length() == 0 || oldAddr.equals(addr))
			return;
		for(int tidx = 0; tidx < emailListModel.getSize(); tidx++)
		{
			String s = (String)emailListModel.getElementAt(tidx);
			if (s.equalsIgnoreCase(addr))
			{
				parentFrame.showError(parentFrame.eventmonLabels.getString("emailAlreadyPresent"));
				return;
			}
		}
		
		emailListModel.setElementAt(addr, idx);
		changed = true;
	}

	protected void addEmailPressed()
	{
		String addr = JOptionPane.showInputDialog(parentFrame,
			parentFrame.eventmonLabels.getString("enterEmail"));
		if (addr == null || addr.trim().length() == 0)
			return;
		for(int idx = 0; idx < emailListModel.getSize(); idx++)
		{
			String s = (String)emailListModel.getElementAt(idx);
			if (s.equalsIgnoreCase(addr))
			{
				parentFrame.showError(parentFrame.eventmonLabels.getString("emailAlreadyPresent"));
				return;
			}
		}
		emailListModel.addElement(addr);
		changed = true;
	}

	protected void delFileMonPressed()
	{
		int idx = fileMonTable.getSelectedRow();
		if (idx == -1)
		{
			parentFrame.showError(parentFrame.eventmonLabels.getString("selectFmBeforeDelete"));
			return;
		}
		fileMonModel.delete(idx);
		changed = true;
	}

	protected void editFileMonPressed()
	{
		int idx = fileMonTable.getSelectedRow();
		if (idx == -1)
		{
			parentFrame.showError(parentFrame.eventmonLabels.getString("selectFmBeforeEdit"));
			return;
		}
		FileMonitor fm = fileMonModel.getFileMonAt(idx);
		FileMonitorDialog dlg = new FileMonitorDialog(this);
		dlg.setData(fm);
		parentFrame.launchDialog(dlg);
		if (dlg.isChanged())
		{
			fileMonModel.sortByColumn(fileMonModel.sortColumn);
			changed = true;
		}
	}

	protected void addFileMonPressed()
	{
		FileMonitor fm = new FileMonitor(null);
		FileMonitorDialog dlg = new FileMonitorDialog(this);
		dlg.setData(fm);
		parentFrame.launchDialog(dlg);
		if (dlg.isChanged())
		{
			fileMonModel.add(fm);
			changed = true;
		}
	}

	protected void closePressed()
	{
		if (changed)
		{
			int answer = JOptionPane.showConfirmDialog(parentFrame, 
				parentFrame.eventmonLabels.getString("saveBeforeClose"));
			if (answer == JOptionPane.CANCEL_OPTION)
				return;
			else if (answer == JOptionPane.YES_OPTION)
				commitPressed();
		}
		parentFrame.closeEditPanel(this);
		parentFrame.groupListPanel.refreshPressed();
	}

	protected void commitPressed()
	{
		String name = nameField.getText().trim();
		editedGroup.setName(name);
		editedGroup.getEmailAddrs().clear();
		for(int idx = 0; idx < emailListModel.getSize(); idx++)
		{
			String ea = emailListModel.elementAt(idx);
			editedGroup.getEmailAddrs().add(new EmailAddr(ea));
		}
		
		AlarmDAI alarmDAO = new AlarmDAO((SqlDatabaseIO)Database.getDb().getDbIo());
		try
		{
			alarmDAO.write(editedGroup);
			changed = false;
		}
		catch (DbIoException ex)
		{
			parentFrame.showError(parentFrame.eventmonLabels.getString("dbWriteError")
				+ ": " + ex);
		}
		finally
		{
			alarmDAO.close();
		}
		parentFrame.setTitleFor(this, name);
		lastModifiedField.setText(
			editedGroup.getLastModifiedMsec() == 0L ? "(never)"
			: sdf.format(new Date(editedGroup.getLastModifiedMsec())));
	}

	public AlarmGroup getEditedGroup()
	{
		return editedGroup;
	}

}

@SuppressWarnings("serial")
class FileMonTableModel extends AbstractTableModel
implements SortingListTableModel
{
	String[] colnames = new String[4];
	int [] widths = { 30, 10, 10, 50 };
	int sortColumn = 0;
	private ArrayList<FileMonitor> data = new ArrayList<FileMonitor>();

	public FileMonTableModel(AlarmEditPanel parentPanel)
	{
		colnames[0] = parentPanel.parentFrame.eventmonLabels.getString("path");
		colnames[1] = parentPanel.parentFrame.eventmonLabels.getString("priority");
		colnames[2] = parentPanel.parentFrame.genericLabels.getString("enable") + "?";
		colnames[3] = parentPanel.parentFrame.genericLabels.getString("description");
	}
	
	public void set(AlarmGroup grp)
	{
		data = grp.getFileMonitors();
	}
	
	public void add(FileMonitor fm)
	{
		data.add(fm);
		sortByColumn(sortColumn);
	}

	public void delete(int idx)
	{
		if (idx >=0 && idx < data.size())
			data.remove(idx);
		sortByColumn(sortColumn);
	}

	@Override
	public int getColumnCount()
	{
		return colnames.length;
	}
	
	public String getColumnName(int col)
	{
		return colnames[col];
	}
	
	@Override
	public int getRowCount()
	{
		return data.size();
	}
	
	@Override
	public Object getValueAt(int row, int col)
	{
		return getColumnValue(getFileMonAt(row), col);
	}

	public FileMonitor getFileMonAt(int row)
	{
		return (FileMonitor)getRowObject(row);
	}


	public String getColumnValue(FileMonitor fm, int col)
	{
		switch(col)
		{
		case 0: return fm.getPath();
		case 1: return Logger.priorityName[fm.getPriority()];
		case 2: return "" + fm.isEnabled();
		case 3: return fm.getDescription();
		default: return "";
		}
	}

	@Override
	public synchronized void sortByColumn(int column)
	{
		this.sortColumn = column;
		Collections.sort(data, new FileMonComparator(sortColumn, this));
		fireTableDataChanged();
	}

	@Override
	public Object getRowObject(int row)
	{
		return data.get(row);
	}
}


class FileMonComparator implements Comparator<FileMonitor>
{
	private int sortColumn = 0;
	private FileMonTableModel model = null;

	FileMonComparator(int sortColumn, FileMonTableModel model)
	{
		this.sortColumn = sortColumn;
		this.model = model;
	}

	@Override
	public int compare(FileMonitor fm1, FileMonitor fm2)
	{
		return TextUtil.strCompareIgnoreCase(
			model.getColumnValue(fm1, sortColumn),
			model.getColumnValue(fm2, sortColumn));
	}
}



//========================
@SuppressWarnings("serial")
class ProcMonTableModel extends AbstractTableModel
implements SortingListTableModel
{
	String[] colnames = new String[4];
	int [] widths = { 10, 20, 10, 60 };
	int sortColumn = 0;
	private ArrayList<ProcessMonitor> data = new ArrayList<ProcessMonitor>();

	public ProcMonTableModel(AlarmEditPanel parentPanel)
	{
		colnames[0] = parentPanel.parentFrame.genericLabels.getString("ID");
		colnames[1] = parentPanel.parentFrame.genericLabels.getString("name");
		colnames[2] = parentPanel.parentFrame.genericLabels.getString("enable") + "?";
		colnames[3] = parentPanel.parentFrame.eventmonLabels.getString("summary");
	}
	
	public void set(AlarmGroup grp)
	{
		data = grp.getProcessMonitors();
	}

	public void delete(int idx)
	{
		if (idx >=0 && idx < data.size())
			data.remove(idx);
		sortByColumn(sortColumn);
	}
	
	public void add(ProcessMonitor pm)
	{
		data.add(pm);
		sortByColumn(sortColumn);
	}

	@Override
	public int getColumnCount()
	{
		return colnames.length;
	}
	
	public String getColumnName(int col)
	{
		return colnames[col];
	}
	
	@Override
	public int getRowCount()
	{
		return data.size();
	}
	
	@Override
	public Object getValueAt(int row, int col)
	{
		return getColumnValue(getProcMonAt(row), col);
	}

	public ProcessMonitor getProcMonAt(int row)
	{
		return (ProcessMonitor)getRowObject(row);
	}


	public String getColumnValue(ProcessMonitor pm, int col)
	{
		switch(col)
		{
		case 0: return pm.getAppId().toString();
		case 1: return pm.getProcName();
		case 2: return "" + pm.isEnabled();
		case 3: return pm.getSummary();
		default: return "";
		}
	}

	@Override
	public synchronized void sortByColumn(int column)
	{
		this.sortColumn = column;
		Collections.sort(data, new ProcMonComparator(sortColumn, this));
		fireTableDataChanged();
	}

	@Override
	public Object getRowObject(int row)
	{
		return data.get(row);
	}
}


class ProcMonComparator implements Comparator<ProcessMonitor>
{
	private int sortColumn = 0;
	private ProcMonTableModel model = null;

	ProcMonComparator(int sortColumn, ProcMonTableModel model)
	{
		this.sortColumn = sortColumn;
		this.model = model;
	}

	@Override
	public int compare(ProcessMonitor pm1, ProcessMonitor pm2)
	{
		return TextUtil.strCompareIgnoreCase(
			model.getColumnValue(pm1, sortColumn),
			model.getColumnValue(pm2, sortColumn));
	}
}

