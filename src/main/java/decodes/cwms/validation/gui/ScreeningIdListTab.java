/**
 * $Id$
 * 
 * Copyright 2015 U.S. Army Corps of Engineers, Hydrologic Engineering Center.
 * 
 * $Log$
 * Revision 1.2  2016/11/03 18:58:48  mmaloney
 * Force reload when assessing TSIDs.
 *
 * Revision 1.1  2015/11/12 15:12:38  mmaloney
 * Initial release.
 *
 */
package decodes.cwms.validation.gui;

import ilex.util.AsciiUtil;
import ilex.util.TextUtil;

import java.awt.BorderLayout;
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
import java.util.List;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.table.AbstractTableModel;

import opendcs.dai.TimeSeriesDAI;
import decodes.gui.SortingListTable;
import decodes.gui.SortingListTableModel;
import decodes.tsdb.DbIoException;
import decodes.tsdb.TimeSeriesIdentifier;
import decodes.tsdb.groupedit.TimeSeriesSelectDialog;
import decodes.cwms.validation.Screening;
import decodes.cwms.validation.ScreeningCriteria;
import decodes.cwms.validation.dao.ScreeningDAI;


public class ScreeningIdListTab extends JPanel
{
	SortingListTable screeningIdTable = null;
	ScreeningIdTableModel model = null;
	ScreeningEditFrame frame = null;
	private ArrayList<TimeSeriesIdentifier> allTsids = null;
	
	public ScreeningIdListTab(ScreeningEditFrame frame)
	{
		this.frame = frame;
		guiInit();
	}
	
	private void guiInit()
	{
		this.setLayout(new BorderLayout());
		model = new ScreeningIdTableModel();
		screeningIdTable = new SortingListTable(model, ScreeningIdTableModel.columnWidths);
		JScrollPane scrollPane = new JScrollPane();
		scrollPane.getViewport().add(screeningIdTable);
		this.add(scrollPane, BorderLayout.CENTER);
		
		JPanel south = new JPanel(new GridBagLayout());
		this.add(south, BorderLayout.SOUTH);
		JButton editButton = new JButton("Edit");
		editButton.addActionListener(
			new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					editPressed();
				}
			});
		south.add(editButton,
			new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.NONE,
				new Insets(4, 10, 4, 5), 0, 0));
		
		JButton newButton = new JButton("New");
		newButton.addActionListener(
			new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					newPressed();
				}
			});
		south.add(newButton,
			new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.NONE,
				new Insets(4, 5, 4, 5), 0, 0));
	
		JButton deleteButton = new JButton("Delete");
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
			new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.NONE,
				new Insets(4, 5, 4, 10), 0, 0));

		JButton assignButton = new JButton("Assign to TS");
		assignButton.addActionListener(
			new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					assignPressed();
				}
			});
		south.add(assignButton,
			new GridBagConstraints(3, 0, 1, 1, 0.0, 0.0,
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
			new GridBagConstraints(4, 0, 1, 1, 1.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(4, 5, 4, 10), 0, 0));

		
		screeningIdTable.addMouseListener(new MouseAdapter()
		{
			public void mouseClicked(MouseEvent e)
			{
				if (e.getClickCount() == 2)
				{
					editPressed();
				}
			}
		});

	}

	protected void assignPressed()
	{
		int n = screeningIdTable.getSelectedRowCount();
		if (n != 1)
		{
			frame.showError("Select a single screening. Then press Assign to TS to assign that "
				+ "screening to one or more time series.");
			return;
		}
		
		Screening scr = (Screening)model.getRowObject(screeningIdTable.getSelectedRow());
		TimeSeriesSelectDialog dlg = new TimeSeriesSelectDialog(frame.getTheDb(), false, frame);

		try (TimeSeriesDAI tsDAO = frame.getTheDb().makeTimeSeriesDAO();)
		{
			if (allTsids == null)
				allTsids = tsDAO.listTimeSeries();
			ArrayList<TimeSeriesIdentifier> allowable = new ArrayList<TimeSeriesIdentifier>();
			for(TimeSeriesIdentifier tsid : allTsids)
			{
				if (TextUtil.strEqualIgnoreCase(tsid.getPart("param"), scr.getParamId())
				 && TextUtil.strEqualIgnoreCase(tsid.getPart("paramtype"), scr.getParamTypeId())
				 && TextUtil.strEqualIgnoreCase(tsid.getPart("duration"), scr.getDurationId())
				 && !frame.getTsidAssignTab().assignmentExistsFor(tsid))
				 allowable.add(tsid);
			}
			if (allowable.size() == 0)
			{
				frame.showError("There are no time series that are currently candidates for this screening. "
					+ "Time Series must have a param, param type, and duration that match the screening"
					+ " and must not already have a screening assignment.");
				return;
			}
			dlg.setTimeSeriesList(allowable);
		}
		catch (DbIoException ex)
		{
			frame.showError("Error reading TSIDs from the database: " + ex);
			return;
		}

		dlg.setMultipleSelection(true);
		frame.launchDialog(dlg);
		TimeSeriesIdentifier[] selections = dlg.getSelectedDataDescriptors();


		TimeSeriesIdentifier tsidT = null;
		try (ScreeningDAI screeningDAO = frame.getTheDb().makeScreeningDAO();)
		{
			if (screeningDAO == null)
			{
				frame.showError("This database does not support screening.");
				return;
			}
			for(TimeSeriesIdentifier tsid : selections)
			{
				tsidT = tsid;
				screeningDAO.assignScreening(scr, tsid, true);
				frame.getTsidAssignTab().addAssignment(scr, tsid, true);
			}
			frame.getTsidAssignTab().resort();
		}
		catch (DbIoException ex)
		{
			String msg = "Error assigning screening '" + scr.getScreeningName() + "' to TSID '" + tsidT
				+ ": " + ex;
			frame.showError(msg);
			System.err.println(msg);
			ex.printStackTrace(System.err);
		}
	}

	public void refresh()
	{
		try (ScreeningDAI screeningDAO = frame.getTheDb().makeScreeningDAO();)
		{
			if (screeningDAO == null)
			{
				frame.showError("This database does not support screening.");
				return;
			}
			model.setScreenings(screeningDAO.getAllScreenings());
		}
		catch (DbIoException ex)
		{
			String msg = "DbIo error reading screening info: " + ex;
			frame.showError(msg);
			System.err.println(msg);
			ex.printStackTrace(System.err);
		}

		frame.getTsidAssignTab().doRefresh();


		try (TimeSeriesDAI tsDAO = frame.getTheDb().makeTimeSeriesDAO();)
		{
			allTsids = tsDAO.listTimeSeries(true);
		}
		catch (DbIoException ex)
		{
			frame.showError("Error reading TSIDs from the database: " + ex);
		}
	}

	protected void deletePressed()
	{
		int row = screeningIdTable.getSelectedRow();
		if (row == -1)
		{	
			frame.showError("Select table row, then press Edit");
			return;
		}
		Screening scr = (Screening)model.getRowObject(row);
		
		// If there are any assignments, issue error
		if (frame.getTsidAssignTab().assignmentsExistFor(scr.getScreeningName()))
		{
			frame.showError("Assignments exist for this screening. You can remove these "
				+ "assignments on the TS Assignments tab. Then retry delete.");
			return;
		}
		
		int res = JOptionPane.showConfirmDialog(frame,
			AsciiUtil.wrapString(
			"This permanently delete the screening from the database. "
			+ "ARE YOU SURE you want to proceed?", 60),
			"Confirm Overwrite Edit Database", JOptionPane.YES_NO_OPTION);
		if (res != JOptionPane.YES_OPTION)
			return;
		

		try (ScreeningDAI screeningDAO = frame.getTheDb().makeScreeningDAO();)
		{
			screeningDAO.deleteScreening(scr);
			refresh();
		}
		catch (DbIoException ex)
		{
			String msg = "Error deleting screening '" + scr.getScreeningName() + "': " + ex;
			frame.showError(msg);
			System.err.println(msg);
			ex.printStackTrace(System.err);
		}
	}

	protected void newPressed()
	{
		String id = JOptionPane.showInputDialog("Enter new Screening ID");
		if (id == null)
			return;
		for(Screening scr : model.screenings)
			if (scr.getScreeningName().equalsIgnoreCase(id))
			{
				frame.showError("A screening already exists with that name.");
				return;
			}
		
		Screening scr = new Screening();
		scr.setScreeningName(id);
		scr.add(new ScreeningCriteria());
		frame.open(scr);
	}

	protected void editPressed()
	{
		int row = screeningIdTable.getSelectedRow();
		if (row == -1)
		{	
			frame.showError("Select table row, then press Edit");
			return;
		}
		System.out.println("Edit pressed for row " + row);
		Screening scr = (Screening)model.getRowObject(row);
		frame.open(scr);
	}

	/**
	 * @param name
	 * @return true if a screening with the passed name already exists.
	 */
	public boolean nameExists(String name)
	{
		for(Screening scr : model.screenings)
			if (scr.getScreeningName().equalsIgnoreCase(name))
				return true;
		return false;
	}

}

class ScreeningIdTableModel
	extends AbstractTableModel
	implements SortingListTableModel
{
	static String columnNames[] = { "Screening ID", "Description", "Param", "Param Type", "Duration" };
	static int columnWidths[] = { 20, 41, 13, 13, 13 };
	List<Screening> screenings = new ArrayList<>();
	private int sortColumn = 0;

	@Override
	public int getRowCount()
	{
		return screenings.size();
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
		return getColumnValue(screenings.get(rowIndex), columnIndex);
	}
	
	private String getColumnValue(Screening scr, int columnIndex)
	{
		switch(columnIndex)
		{
		case 0: return scr.getUniqueName();
		case 1: return briefDesc(scr.getScreeningDesc());
		case 2: return scr.getParamId() != null ? scr.getParamId() : "";
		case 3: return scr.getParamTypeId() != null ? scr.getParamTypeId() : "";
		case 4: return scr.getDurationId() != null ? scr.getDurationId() : "";
		}
		return scr.getUniqueName(); // won't happen
	}
	
	private String briefDesc(String desc)
	{
		if (desc == null)
			return "";
		if (desc.length() < 30)
			return desc;
		return desc.substring(0,30);
	}

	@Override
	public void sortByColumn(final int column)
	{
		Collections.sort(screenings, 
			new Comparator<Screening>()
			{
				@Override
				public int compare(Screening o1, Screening o2)
				{
					return getColumnValue(o1, column).compareTo(getColumnValue(o2, column));
				}
			});
		sortColumn = column;
		this.fireTableDataChanged();
	}

	@Override
	public Object getRowObject(int row)
	{
		return screenings.get(row);
	}

	public void setScreenings(List<Screening> screenings)
	{
		this.screenings = screenings;
		sortByColumn(sortColumn);
	}
	
}
