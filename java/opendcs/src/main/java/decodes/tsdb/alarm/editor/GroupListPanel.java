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
package decodes.tsdb.alarm.editor;

import ilex.util.LoadResourceBundle;
import ilex.util.TextUtil;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.TimeZone;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.table.AbstractTableModel;

import opendcs.dao.AlarmDAO;
import decodes.gui.SortingListTable;
import decodes.gui.SortingListTableModel;
import decodes.sql.DbKey;
import decodes.sql.SqlDatabaseIO;
import decodes.tsdb.DbIoException;
import decodes.tsdb.alarm.AlarmConfig;
import decodes.tsdb.alarm.AlarmGroup;
import decodes.util.DecodesSettings;

@SuppressWarnings("serial")
public class GroupListPanel	extends JPanel
{
	AlarmEditFrame parentFrame = null;
	private SortingListTable alarmGroupTable = null;
	GroupListTableModel model = null;

	public GroupListPanel(AlarmEditFrame parent)
	{
		super(new BorderLayout());
		this.parentFrame = parent;
		
		guiInit();
	}
	
	private void guiInit()
	{
		model = new GroupListTableModel(this);
		alarmGroupTable = new SortingListTable(model, model.widths);
		JScrollPane scrollPane = new JScrollPane(alarmGroupTable, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
			JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		this.add(scrollPane, BorderLayout.CENTER);
		
		JPanel buttonPanel = new JPanel(new GridBagLayout());
		this.add(buttonPanel, BorderLayout.SOUTH);

		JButton openButton = new JButton(parentFrame.genericLabels.getString("open"));
		openButton.addActionListener(
			new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					openPressed();
				}
			});
		buttonPanel.add(openButton, 
			new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(4, 10, 4, 4), 0, 0));

		JButton newButton = new JButton(parentFrame.genericLabels.getString("new"));
		newButton.addActionListener(
			new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					newPressed();
				}
			});
		buttonPanel.add(newButton, 
			new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(4, 4, 4, 4), 0, 0));
		
		JButton copyButton = new JButton(parentFrame.genericLabels.getString("copy"));
		copyButton.addActionListener(
			new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					copyPressed();
				}
			});
		buttonPanel.add(copyButton, 
			new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(4, 4, 4, 4), 0, 0));

		JButton deleteButton = new JButton(parentFrame.genericLabels.getString("delete"));
		deleteButton.addActionListener(
			new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					deletePressed();
				}
			});
		buttonPanel.add(deleteButton, 
			new GridBagConstraints(3, 0, 1, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(4, 4, 4, 4), 0, 0));

		JButton refreshButton = new JButton(parentFrame.genericLabels.getString("refresh"));
		refreshButton.addActionListener(
			new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					refreshPressed();
				}
			});
		buttonPanel.add(refreshButton, 
			new GridBagConstraints(5, 0, 1, 1, 1.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(4, 4, 4, 10), 0, 0));
		
		
		alarmGroupTable.addMouseListener(
			new MouseAdapter()
			{
				public void mouseClicked(MouseEvent e)
				{
					if (e.getClickCount() == 2)
					{
						openPressed();
					}
				}
			});

	}
	
	
	protected void deletePressed()
	{
		int row = alarmGroupTable.getSelectedRow();
		if (row == -1)
		{
			parentFrame.showError(
				parentFrame.eventmonLabels.getString("alarmEdit.selectFirst")
				+ " " + parentFrame.genericLabels.getString("delete") + ".");
			return;
		}

		AlarmGroup grp = model.getGroupAt(row);
		
		if (parentFrame.isBeingEdited(grp))
		{
			parentFrame.showError(parentFrame.eventmonLabels.getString("isBeingEdited"));
			return;
		}
		
		int choice = parentFrame.showConfirm(parentFrame.genericLabels.getString("confirm"),
			LoadResourceBundle.sprintf(
				parentFrame.genericLabels.getString("confirmDelete"), 
				parentFrame.eventmonLabels.getString("alarmGroup")), 
				JOptionPane.YES_NO_OPTION);
		if (choice != JOptionPane.YES_OPTION)
			return;
		
		model.delete(grp);
		model.reload();
	}

	protected void copyPressed()
	{
		int row = alarmGroupTable.getSelectedRow();
		if (row == -1)
		{
			parentFrame.showError(
				parentFrame.eventmonLabels.getString("alarmEdit.selectFirst")
				+ " " + parentFrame.genericLabels.getString("copy") + ".");
			return;
		}
		String name = askUniqueName();
		if (name == null)
			return;
		
		AlarmGroup grp = model.getGroupAt(row);
		AlarmGroup copy = grp.noIdCopy();
		copy.setName(name);
		
		parentFrame.editAlarmGroup(copy);
	}
	
	
	

	protected void newPressed()
	{
		String name = askUniqueName();
		if (name == null)
			return;
		
		AlarmGroup grp = new AlarmGroup(DbKey.NullKey);
		grp.setName(name);

		parentFrame.editAlarmGroup(grp);
	}
	
	/**
	 * Ask user for unique group name.
	 * Show an error message if name already exists.
	 * 
	 * @return null if not successful, unique name if ok.
	 */
	String askUniqueName()
	{
		String name = JOptionPane.showInputDialog(parentFrame,
			parentFrame.eventmonLabels.getString("enterGroupName"));
		if (name == null || name.trim().length() == 0)
			return null;
		
		for(AlarmGroup grp : model.getAlarmConfig().getGroups())
			if (grp.getName().equalsIgnoreCase(name))
			{
				parentFrame.showError(parentFrame.eventmonLabels.getString("groupAlreadyExists"));
				return null;
			}
		return name;
	}

	protected void openPressed()
	{
		int row = alarmGroupTable.getSelectedRow();
		if (row == -1)
		{
			parentFrame.showError(
				parentFrame.eventmonLabels.getString("alarmEdit.selectFirst")
				+ " " + parentFrame.genericLabels.getString("open") + ".");
			return;
		}
		AlarmGroup grp = model.getGroupAt(row);
		parentFrame.editAlarmGroup(grp);
	}

	protected void refreshPressed()
	{
		model.reload();
	}
	
	public AlarmGroup getGroupById(DbKey groupId)
	{
		return model.getGroupById(groupId);
	}

}

@SuppressWarnings("serial")
class GroupListTableModel extends AbstractTableModel implements SortingListTableModel
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	String[] colnames = new String[3];
	int [] widths = { 15, 60, 25 };
	private int sortColumn = 0;
	private AlarmConfig alarmConfig = new AlarmConfig();
	private GroupListPanel parentPanel = null;
	private SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd-HH:mm:ss");

	public GroupListTableModel(GroupListPanel parentPanel)
	{
		this.parentPanel = parentPanel;
		colnames[0] = parentPanel.parentFrame.genericLabels.getString("ID");
		colnames[1] = parentPanel.parentFrame.genericLabels.getString("name");
		colnames[2] = parentPanel.parentFrame.genericLabels.getString("lastMod") + " "
			+ DecodesSettings.instance().guiTimeZone;
		sdf.setTimeZone(TimeZone.getTimeZone(DecodesSettings.instance().guiTimeZone));
	}

	public AlarmGroup getGroupById(DbKey groupId)
	{
		return alarmConfig.getGroupById(groupId);
	}
	
	public ArrayList<AlarmGroup> getGroupList()
	{
		return alarmConfig.getGroups();
	}

	public void delete(AlarmGroup grp)
	{
		SqlDatabaseIO sqldbio = (SqlDatabaseIO)decodes.db.Database.getDb().getDbIo();
		AlarmDAO alarmDAO = new AlarmDAO(sqldbio);
		
		try
		{
			alarmDAO.deleteAlarmGroup(grp.getAlarmGroupId());
		}
		catch (DbIoException ex)
		{
			log.atError().setCause(ex).log("Unable to delete alarm group.");
			parentPanel.parentFrame.showError("Cannot delete alarm group: " + ex);
		}
		finally
		{
			alarmDAO.close();
		}
	}

	public void reload()
	{
		SqlDatabaseIO sqldbio = (SqlDatabaseIO)decodes.db.Database.getDb().getDbIo();
		AlarmDAO alarmDAO = new AlarmDAO(sqldbio);
		
		try
		{
			alarmDAO.check(alarmConfig);
			sortByColumn(sortColumn);
			log.debug("After reload there are {} groups.", alarmConfig.getGroups().size());
			if (log.isDebugEnabled())
			{
				for(AlarmGroup grp : alarmConfig.getGroups())
				{
					log.debug("ID={}, {},{}",
							  grp.getAlarmGroupId(), grp.getName(), new Date(grp.getLastModifiedMsec()));
				}
			}
		}
		catch (DbIoException ex)
		{
			log.atError().setCause(ex).log("Unable to read alarm config.");
			parentPanel.parentFrame.showError("Cannot read alarm config: " + ex);
		}
		finally
		{
			alarmDAO.close();
		}
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
		return alarmConfig.getGroups().size();
	}

	@Override
	public Object getValueAt(int row, int col)
	{
		return getColumnValue(getGroupAt(row), col);
	}
	
	public AlarmGroup getGroupAt(int row)
	{
		return (AlarmGroup)getRowObject(row);
	}


	public String getColumnValue(AlarmGroup grp, int col)
	{
		switch(col)
		{
		case 0: return grp.getAlarmGroupId().toString();
		case 1: return grp.getName();
		case 2: return sdf.format(new Date(grp.getLastModifiedMsec()));
		default: return "";
		}
	}

	@Override
	public synchronized void sortByColumn(int column)
	{
		this.sortColumn = column;
		Collections.sort(alarmConfig.getGroups(), new GroupComparator(sortColumn, this));
		fireTableDataChanged();
	}

	@Override
	public Object getRowObject(int row)
	{
		return alarmConfig.getGroups().get(row);
	}

	public AlarmConfig getAlarmConfig()
	{
		return alarmConfig;
	}
}


class GroupComparator implements Comparator<AlarmGroup>
{
	private int sortColumn = 0;
	private GroupListTableModel model = null;
	
	GroupComparator(int sortColumn, GroupListTableModel model)
	{
		this.sortColumn = sortColumn;
		this.model = model;
	}
	
	@Override
	public int compare(AlarmGroup evt1, AlarmGroup evt2)
	{
		return TextUtil.strCompareIgnoreCase(
			model.getColumnValue(evt1, sortColumn),
			model.getColumnValue(evt2, sortColumn));
	}
}
