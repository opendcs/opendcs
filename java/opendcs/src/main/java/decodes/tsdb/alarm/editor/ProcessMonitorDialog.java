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
 * Revision 1.1  2017/05/17 20:36:56  mmaloney
 * First working version.
 *
 */
package decodes.tsdb.alarm.editor;

import ilex.util.Logger;
import ilex.util.StringPair;
import ilex.util.TextUtil;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.ResourceBundle;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.table.AbstractTableModel;

import opendcs.dai.LoadingAppDAI;
import decodes.db.Database;
import decodes.gui.GuiDialog;
import decodes.gui.SortingListTable;
import decodes.gui.SortingListTableModel;
import decodes.sql.DbKey;
import decodes.tsdb.CompAppInfo;
import decodes.tsdb.DbIoException;
import decodes.tsdb.alarm.AlarmEvent;
import decodes.tsdb.alarm.ProcessMonitor;


@SuppressWarnings("serial")
public class ProcessMonitorDialog 
	extends GuiDialog
{
	private AlarmEditPanel parentPanel;
	@SuppressWarnings("rawtypes")
	private JComboBox processCombo = new JComboBox();
	private AlarmDefTableModel model;
	private SortingListTable alarmDefTable;
	private ArrayList<CompAppInfo> appsInCombo = null;
	private boolean changed = false;
	private ProcessMonitor theProcMon = null;
	private JCheckBox enabledCheck = new JCheckBox("Enabled?");


	public ProcessMonitorDialog(AlarmEditPanel parentPanel)
	{
		super(parentPanel.parentFrame, 
			parentPanel.parentFrame.eventmonLabels.getString("procMonitor"), true);
		this.parentPanel = parentPanel;
		guiInit();
	}

	@SuppressWarnings("unchecked")
	private void guiInit()
	{
		ResourceBundle labels = parentPanel.parentFrame.eventmonLabels;
		Container contpane = getContentPane();
		contpane.setLayout(new BorderLayout());
		
		JPanel northPanel = new JPanel(new FlowLayout());
		contpane.add(northPanel, BorderLayout.NORTH);
		northPanel.add(new JLabel(labels.getString("process")));

		LoadingAppDAI loadingAppDAO = Database.getDb().getDbIo().makeLoadingAppDAO();
		try
		{
			appsInCombo = loadingAppDAO.listComputationApps(false);
			processCombo.addItem(""); // blank selection at top of list.
			for (CompAppInfo cai : appsInCombo)
				processCombo.addItem(cai.getAppName());
		}
		catch (DbIoException ex)
		{
			parentPanel.parentFrame.showError("Cannot list Routing Scheduler apps: " + ex);
		}
		finally
		{
			loadingAppDAO.close();
		}
		northPanel.add(processCombo);
		northPanel.add(new JLabel("    ")); // spacer
		northPanel.add(enabledCheck);
		
		model = new AlarmDefTableModel(parentPanel);
		alarmDefTable = new SortingListTable(model, model.widths);
		JScrollPane alarmDefScrollPane = new JScrollPane(alarmDefTable,
			JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		JPanel tablePanel = new JPanel(new BorderLayout());
		tablePanel.add(alarmDefScrollPane, BorderLayout.CENTER);
		contpane.add(tablePanel, BorderLayout.CENTER);
		
		JPanel buttonPanel = new JPanel(new GridBagLayout());
		tablePanel.add(buttonPanel, BorderLayout.EAST);
		JButton addAlarmDefButton = new JButton(
			parentPanel.parentFrame.genericLabels.getString("add"));
		addAlarmDefButton.addActionListener(
			new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					addAlarmDefPressed();
				}
			});
		buttonPanel.add(addAlarmDefButton,
			new GridBagConstraints(0, 0, 1, 1, 1.0, 0.5,
				GridBagConstraints.SOUTH, GridBagConstraints.HORIZONTAL,
				new Insets(2, 2, 2, 2), 0, 0));
		
		JButton editAlarmDefButton = new JButton(
			parentPanel.parentFrame.genericLabels.getString("edit"));
		editAlarmDefButton.addActionListener(
			new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					editAlarmDefPressed();
				}
			});
		buttonPanel.add(editAlarmDefButton,
			new GridBagConstraints(0, 1, 1, 1, 1.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
				new Insets(2, 2, 2, 2), 0, 0));
		
		JButton delAlarmDefButton = new JButton(
			parentPanel.parentFrame.genericLabels.getString("delete"));
		delAlarmDefButton.addActionListener(
			new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					delAlarmDefPressed();
				}
			});
		buttonPanel.add(delAlarmDefButton,
			new GridBagConstraints(0, 2, 1, 1, 1.0, 0.5,
				GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL,
				new Insets(2, 2, 2, 2), 0, 0));

		// South will contain 'OK' and 'Cancel' buttons.
		JPanel south = new JPanel(new FlowLayout(FlowLayout.CENTER, 7, 7));
		contpane.add(south, BorderLayout.SOUTH);

		JButton okButton = new JButton(parentPanel.parentFrame.genericLabels.getString("OK"));
		okButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent av)
			{
				okPressed();
			}
		});
		south.add(okButton);

		JButton cancelButton = new JButton(parentPanel.parentFrame.genericLabels.getString("cancel"));
		cancelButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent av)
			{
				cancelPressed();
			}
		});
		south.add(cancelButton);
		contpane.add(south, BorderLayout.SOUTH);
		
		enabledCheck.addActionListener(
			new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					changed = true;
				}
			});

		
		alarmDefTable.addMouseListener(
			new MouseAdapter()
			{
				public void mouseClicked(MouseEvent e)
				{
					if (e.getClickCount() == 2)
					{
						editAlarmDefPressed();
					}
				}
			});


		pack();
	}
	
	protected void delAlarmDefPressed()
	{
		int idx = alarmDefTable.getSelectedRow();
		if (idx == -1)
		{
			parentPanel.parentFrame.showError(
				parentPanel.parentFrame.eventmonLabels.getString("selectADBeforeDelete"));
			return;
		}
		model.delete(idx);
		changed = true;
		
	}

	protected void editAlarmDefPressed()
	{
		int idx = alarmDefTable.getSelectedRow();
		if (idx == -1)
		{
			parentPanel.parentFrame.showError(
				parentPanel.parentFrame.eventmonLabels.getString("selectADBeforeDelete"));
			return;
		}
		StringPair sp = (StringPair)model.getRowObject(idx);

		AlarmDefDialog dlg = new AlarmDefDialog(parentPanel);
		dlg.setData(sp);
		parentPanel.parentFrame.launchDialog(dlg);
		if (dlg.isOK())
		{
			if (!sp.first.equals(dlg.getPriority()))
			{
				changed = true;
				sp.first = dlg.getPriority();
			}
			if (!sp.second.equals(dlg.getPattern()))
			{
				changed = true;
				sp.second = dlg.getPattern();
			}
		}

	}

	protected void addAlarmDefPressed()
	{
		StringPair sp = new StringPair(Logger.priorityName[Logger.E_WARNING], "");
		AlarmDefDialog dlg = new AlarmDefDialog(parentPanel);
		dlg.setData(sp);
		parentPanel.parentFrame.launchDialog(dlg);
		if (dlg.isOK())
		{
			sp.first = dlg.getPriority();
			sp.second = dlg.getPattern();
			model.add(sp);
			changed = true;
		}
	}

	public void setData(ProcessMonitor pm)
	{
		this.theProcMon = pm;
		boolean found = false;
		for(int idx = 0; idx < appsInCombo.size(); idx++)
		{
			if (appsInCombo.get(idx).getAppId().equals(pm.getAppId()))
			{
				// item 0 is blank meaning no selection
				processCombo.setSelectedIndex(idx+1);
				found = true;
				break;
			}
		}
		if (!found)
			processCombo.setSelectedIndex(0);
		enabledCheck.setSelected(pm.isEnabled());
		
		model.set(pm);
		changed = false;
	}

	protected void cancelPressed()
	{
		changed = false;
		closeDlg();
	}

	protected void okPressed()
	{
		if (changed)
		{
			int idx = processCombo.getSelectedIndex();
			if (idx == 0)
			{
				parentPanel.parentFrame.showError(
					parentPanel.parentFrame.eventmonLabels.getString("selectProcRequired"));
				return;
			}
			theProcMon.setAppInfo(appsInCombo.get(idx-1));
			theProcMon.setEnabled(enabledCheck.isSelected());
			idx = 0;
			for(StringPair sp : model.data)
			{
				boolean isNewDef = idx >= theProcMon.getDefs().size();
				if (isNewDef)
				{
					AlarmEvent def = new AlarmEvent(DbKey.NullKey);
					def.setPriority(str2pri(sp.first));
					def.setPattern(sp.second);
					theProcMon.getDefs().add(def);
					theProcMon.setChanged(true);
				}
				else // Existing definition
				{
					AlarmEvent def = theProcMon.getDefs().get(idx);
					int pri =  str2pri(sp.first);
					if (def.getPriority() != pri || !TextUtil.strEqual(def.getPattern(), sp.second))
					{
						def.setPriority(pri);
						def.setPattern(sp.second);
						theProcMon.setChanged(true);
					}
				}
				
				idx++;
			}
		}
		closeDlg();
	}
	
	private int str2pri(String s)
	{
		s = s.trim();
		if (s.equalsIgnoreCase("INFO")) return Logger.E_INFORMATION;
		else if (s.equalsIgnoreCase("WARNING")) return Logger.E_WARNING;
		else if (s.equalsIgnoreCase("FAILURE")) return Logger.E_FAILURE;
		else if (s.equalsIgnoreCase("FATAL")) return Logger.E_FATAL;
		return Logger.E_WARNING;
	}
	
	/** Closes the dialog */
	private void closeDlg()
	{
		setVisible(false);
		dispose();
	}

	public boolean isChanged()
	{
		return changed;
	}

}




//========================
@SuppressWarnings("serial")
class AlarmDefTableModel extends AbstractTableModel
implements SortingListTableModel
{
	String[] colnames = new String[2];
	int [] widths = { 30, 70 };
	private int sortColumn = 0;
	ArrayList<StringPair> data = new ArrayList<StringPair>();

	public AlarmDefTableModel(AlarmEditPanel parentPanel)
	{
		colnames[0] = parentPanel.parentFrame.eventmonLabels.getString("priority");
		colnames[1] = parentPanel.parentFrame.eventmonLabels.getString("pattern");
	}
	
	public void set(ProcessMonitor pm)
	{
		for(AlarmEvent def : pm.getDefs())
		{
			int p = def.getPriority();
			String priority = (p >= 0 && p < Logger.priorityName.length) ? 
				Logger.priorityName[p] : "";
			String pattern = def.getPattern();
			if (pattern == null)
				pattern = "";
			data.add(new StringPair(priority, pattern));
		}
		sortByColumn(sortColumn);
	}

	public void delete(int idx)
	{
		if (idx >=0 && idx < data.size())
			data.remove(idx);
		sortByColumn(sortColumn);
	}
	
	public void add(StringPair sp)
	{
		data.add(sp);
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
		StringPair sp = (StringPair)getRowObject(row);
		
		return getColumnValue(sp, col);
	}

	public static String getColumnValue(StringPair sp, int col)
	{
		switch(col)
		{
		case 0: return sp.first;
		case 1: return sp.second;
		default: return "";
		}
	}

	@Override
	public synchronized void sortByColumn(int column)
	{
		this.sortColumn = column;
		Collections.sort(data, new AlarmDefComparator(sortColumn));
		fireTableDataChanged();
	}

	@Override
	public Object getRowObject(int row)
	{
		return data.get(row);
	}
}


class AlarmDefComparator implements Comparator<StringPair>
{
	private int sortColumn = 0;

	AlarmDefComparator(int sortColumn)
	{
		this.sortColumn = sortColumn;
	}

	@Override
	public int compare(StringPair sp1, StringPair sp2)
	{
		int r = TextUtil.strCompareIgnoreCase(
			AlarmDefTableModel.getColumnValue(sp1, sortColumn),
			AlarmDefTableModel.getColumnValue(sp2, sortColumn));
		if (r != 0)
			return r;
		int sc = sortColumn == 0 ? 1 : 0;
		return TextUtil.strCompareIgnoreCase(
			AlarmDefTableModel.getColumnValue(sp1, sc),
			AlarmDefTableModel.getColumnValue(sp2, sc));
	}
}

