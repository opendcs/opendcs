/**
 * $Id$
 * 
 * Copyright 2015 U.S. Army Corps of Engineers, Hydrologic Engineering Center.
 * 
 * $Log$
 */
package decodes.cwms.validation.gui;

import ilex.util.TextUtil;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.table.AbstractTableModel;

import decodes.cwms.validation.Screening;
import decodes.cwms.validation.dao.ScreeningDAI;
import decodes.cwms.validation.dao.TsidScreeningAssignment;
import decodes.gui.SortingListTable;
import decodes.gui.SortingListTableModel;
import decodes.tsdb.DbIoException;
import decodes.tsdb.TimeSeriesIdentifier;

public class TsidAssignTab extends JPanel
{	
	SortingListTable tsidAssignTable = null;
	TsidAssignTableModel model = null;
	ScreeningEditFrame frame = null;

	
	public TsidAssignTab(ScreeningEditFrame frame)
	{
		this.frame = frame;
		guiInit();
	}
	
	private void guiInit()
	{
		this.setLayout(new BorderLayout());
		model = new TsidAssignTableModel();
		tsidAssignTable = new SortingListTable(model, TsidAssignTableModel.columnWidths);
		JScrollPane scrollPane = new JScrollPane();
		scrollPane.getViewport().add(tsidAssignTable);
		this.add(scrollPane, BorderLayout.CENTER);
		
		JPanel south = new JPanel(new GridBagLayout());
		this.add(south, BorderLayout.SOUTH);
	
		JButton editScreeningButton = new JButton("Edit Screening");
		editScreeningButton.addActionListener(
			new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					editScreeningPressed();
				}
			});
		south.add(editScreeningButton,
			new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.NONE,
				new Insets(4, 5, 4, 5), 0, 0));

		JButton deleteButton = new JButton("Delete Assignment");
		deleteButton.addActionListener(
			new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					deletePressed();
				}
			});
		south.add(deleteButton,
			new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.NONE,
				new Insets(4, 5, 4, 10), 0, 0));

		JButton activeButton = new JButton("Set/Clear Active");
		activeButton.addActionListener(
			new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					activePressed();
				}
			});
		south.add(activeButton,
			new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.NONE,
				new Insets(4, 5, 4, 10), 0, 0));

		
		JButton refreshButton = new JButton("Refresh");
		refreshButton.addActionListener(
			new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					refresh();
				}
			});
		south.add(refreshButton,
			new GridBagConstraints(3, 0, 1, 1, 1.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(4, 5, 4, 10), 0, 0));
	}

	protected void activePressed()
	{
		String result = (String)JOptionPane.showInputDialog(frame, 
			"Set selected assignments to", "Activate/Deactivate Assignments", 
			JOptionPane.QUESTION_MESSAGE, null, new String[]{"Active", "Inactive"}, "Active");
		if (result == null)
			return;
		
		ScreeningDAI screeningDAO = null;
		try
		{
			screeningDAO = frame.getTheDb().makeScreeningDAO();
			int idxs[] = tsidAssignTable.getSelectedRows();
			for(int n = 0; n < idxs.length; n++)
			{
				int modelRow = tsidAssignTable.convertRowIndexToModel(idxs[n]);
				TsidScreeningAssignment tsa = (TsidScreeningAssignment)model.getRowObject(modelRow);
				if (tsa.isActive() && result.equals("Active"))
				{
					continue;
				}
				else if (!tsa.isActive() && result.equals("Inactive"))
				{
					continue;
				}
				tsa.setActive(result.equals("Active"));
				screeningDAO.assignScreening(tsa.getScreening(), tsa.getTsid(), tsa.isActive());
			}
		}
		catch(Exception ex)
		{
			frame.showError("Error writing screening assignments: " + ex);
		}
		finally
		{
			screeningDAO.close();
		}
	}

	public void refresh()
	{
		// Refresh the screening ID list first for efficiency.
		frame.getScreeningIdListTab().refresh();
	}
	
	void doRefresh()
	{

		ScreeningDAI screeningDAO = null;
		try
		{
			screeningDAO = frame.getTheDb().makeScreeningDAO();
			if (screeningDAO == null)
			{
				frame.showError("This database does not support screening.");
				return;
			}
			model.setAssignments(screeningDAO.getTsidScreeningAssignments(false));
		}
		catch (DbIoException ex)
		{
			String msg = "DbIo error reading screening info: " + ex;
			frame.showError(msg);
			System.err.println(msg);
			ex.printStackTrace(System.err);
		}
		finally
		{
			if (screeningDAO != null)
				screeningDAO.close();
		}
	}

	protected void deletePressed()
	{
		int idxs[] = tsidAssignTable.getSelectedRows();
		if (idxs.length == 0)
		{
			frame.showError("There are no rows selected. Nothing to delete.");
			return;
		}
		int res = JOptionPane.showConfirmDialog(frame, 
			"Are you sure you want to delete the selected " + idxs.length + " assignment(s)?", 
			"Confirm Delete", JOptionPane.YES_NO_OPTION);
		if (res != JOptionPane.YES_OPTION)
		{
			return;
		}

		try (ScreeningDAI screeningDAO = frame.getTheDb().makeScreeningDAO();)
		{
			for(int n = 0; n < idxs.length; n++)
			{
				int modelRow = tsidAssignTable.convertRowIndexToModel(idxs[n]);
				TsidScreeningAssignment tsa = (TsidScreeningAssignment)model.getRowObject(modelRow);
				screeningDAO.unassignScreening(tsa.getScreening(), tsa.getTsid());
			}
		}
		catch(Exception ex)
		{
			frame.showError("Error deleting screening assignments: " + ex);
		}
		refresh();
	}

	protected void editScreeningPressed()
	{
		int idx = tsidAssignTable.getSelectedRow();
		if (idx == -1)
		{
			frame.showError("No assignment selected.");
			return;
		}
		int modelRow = tsidAssignTable.convertRowIndexToModel(idx);
		TsidScreeningAssignment tsa = (TsidScreeningAssignment)model.getRowObject(modelRow);
		frame.open(tsa.getScreening());
	}

	public boolean assignmentsExistFor(String screenignId)
	{
		for(TsidScreeningAssignment assign : model.assignments)
			if (assign.getScreening().getScreeningName().equalsIgnoreCase(screenignId))
				return true;
		return false;
	}
	
	public boolean assignmentExistsFor(TimeSeriesIdentifier tsid)
	{
		for(TsidScreeningAssignment assign : model.assignments)
			if (assign.getTsid().equals(tsid))
				return true;
		return false;
	}

	public void addAssignment(Screening scr, TimeSeriesIdentifier tsid, boolean active)
	{
		model.assignments.add(new TsidScreeningAssignment(tsid, scr, active));
	}

	public void resort() { model.resort(); }
	
}


class TsidAssignTableModel
	extends AbstractTableModel
	implements SortingListTableModel
{
	static String columnNames[] = 
		{ "Screening ID", "Active?", "Location", "Param", "Param Type", "Interval", "Duration", "Version" };
	static int columnWidths[] = { 28, 10, 12, 10, 10, 10, 10, 10 };
	
	List<TsidScreeningAssignment> assignments = new ArrayList<TsidScreeningAssignment>();
	private int sortColumn = 0;

	@Override
	public int getRowCount()
	{
		return assignments.size();
	}

	public void setAssignments(List<TsidScreeningAssignment> tsidScreeningAssignments)
	{
		this.assignments = tsidScreeningAssignments;
		sortByColumn(sortColumn);
	}

	@Override
	public int getColumnCount()
	{
		return columnNames.length;
	}

	@Override
	public String getColumnName(int c)
	{
		return columnNames[c];
	}


	@Override
	public Object getValueAt(int rowIndex, int columnIndex)
	{
		return getColumnValue(assignments.get(rowIndex), columnIndex);
	}
	
	private String getColumnValue(TsidScreeningAssignment assign, int columnIndex)
	{
		switch(columnIndex)
		{
		case 0: return assign.getScreening().getUniqueName();
		case 1: return "" + assign.isActive();
		case 2: return assign.getTsid().getSiteName();
		case 3: return assign.getTsid().getPart("param");
		case 4: return assign.getTsid().getPart("paramtype");
		case 5: return assign.getTsid().getPart("interval");
		case 6: return assign.getTsid().getPart("duration");
		case 7: return assign.getTsid().getPart("version");
		}
		return ""; // won't happen
	}

	@Override
	public void sortByColumn(final int column)
	{
		Collections.sort(assignments, 
			new Comparator<TsidScreeningAssignment>()
			{
				@Override
				public int compare(TsidScreeningAssignment o1, TsidScreeningAssignment o2)
				{
					return getColumnValue(o1, column).compareTo(getColumnValue(o2, column));
				}
			});
		sortColumn = column;
		this.fireTableDataChanged();
	}
	
	void resort() { sortByColumn(sortColumn); }

	@Override
	public Object getRowObject(int row)
	{
		return assignments.get(row);
	}
	
}