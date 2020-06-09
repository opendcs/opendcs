package decodes.tsdb.alarm.editor;

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
import java.util.Iterator;
import java.util.TimeZone;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.table.AbstractTableModel;

import decodes.db.Site;
import decodes.gui.SortingListTable;
import decodes.gui.SortingListTableModel;
import decodes.sql.DbKey;
import decodes.sql.SqlDatabaseIO;
import decodes.tsdb.DbIoException;
import decodes.tsdb.NoSuchObjectException;
import decodes.tsdb.alarm.AlarmGroup;
import decodes.tsdb.alarm.AlarmLimitSet;
import decodes.tsdb.alarm.AlarmScreening;
import decodes.util.DecodesSettings;
import ilex.util.LoadResourceBundle;
import ilex.util.Logger;
import ilex.util.TextUtil;
import opendcs.dai.AlarmDAI;
import opendcs.dai.DataTypeDAI;
import opendcs.dai.SiteDAI;
import opendcs.dao.AlarmDAO;

@SuppressWarnings("serial")
public class ScreeningListPanel extends JPanel
{
	AlarmEditFrame parentFrame = null;
	private SortingListTable screeningTable = null;
	private ScreeningListTableModel model = null;


	public ScreeningListPanel(AlarmEditFrame parent)
	{
		super(new BorderLayout());
		this.parentFrame = parent;
		
		guiInit();
	}
	
	private void guiInit()
	{
		model = new ScreeningListTableModel(this);
		screeningTable = new SortingListTable(model, model.widths);
		JScrollPane scrollPane = new JScrollPane(screeningTable, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
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
		
		
		screeningTable.addMouseListener(
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
		int row = screeningTable.getSelectedRow();
		if (row == -1)
		{
			parentFrame.showError(
				parentFrame.eventmonLabels.getString("alarmEdit.selectFirst")
				+ " " + parentFrame.genericLabels.getString("delete") + ".");
			return;
		}

		AlarmScreening scrn = model.getScreeningAt(row);
		
		if (parentFrame.isBeingEdited(scrn))
		{
			parentFrame.showError(parentFrame.eventmonLabels.getString("isBeingEdited"));
			return;
		}
		
		int choice = parentFrame.showConfirm(parentFrame.genericLabels.getString("confirm"),
			LoadResourceBundle.sprintf(
				parentFrame.genericLabels.getString("confirmDelete"), 
				parentFrame.eventmonLabels.getString("screening")), 
				JOptionPane.YES_NO_OPTION);
		if (choice != JOptionPane.YES_OPTION)
			return;
		
		model.delete(row);
	}

	protected void copyPressed()
	{
		int row = screeningTable.getSelectedRow();
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
		
		AlarmScreening scrn = model.getScreeningAt(row);
		AlarmScreening copy = new AlarmScreening();
		copy.copyFrom(scrn);
		for(AlarmLimitSet als : copy.getLimitSets())
			als.setLimitSetId(DbKey.NullKey);
		
		copy.setScreeningName(name);
		
		parentFrame.editAlarmScreening(copy);
	}
	
	protected void newPressed()
	{
		String name = askUniqueName();
		if (name == null)
			return;
		
		AlarmScreening scrn = new AlarmScreening();
		scrn.setScreeningName(name);

		parentFrame.editAlarmScreening(scrn);
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
			parentFrame.eventmonLabels.getString("enterScreeningName"));
		if (name == null || name.trim().length() == 0)
			return null;
		
		for(AlarmScreening scrn : model.screenings)
			if (scrn.getScreeningName().equalsIgnoreCase(name))
			{
				parentFrame.showError(parentFrame.eventmonLabels.getString("screeningAlreadyExists"));
				return null;
			}
		return name;
	}

	protected void openPressed()
	{
		int row = screeningTable.getSelectedRow();
		if (row == -1)
		{
			parentFrame.showError(
				parentFrame.eventmonLabels.getString("alarmEdit.selectFirst")
				+ " " + parentFrame.genericLabels.getString("new") + ".");
			return;
		}
		AlarmScreening scrn = model.getScreeningAt(row);
		parentFrame.editAlarmScreening(scrn);
	}

	protected void refreshPressed()
	{
		model.reload();
	}
	
	public boolean nameExists(String screeningName)
	{
		for(AlarmScreening scrn : model.screenings)
			if (scrn.getScreeningName().equalsIgnoreCase(screeningName))
				return true;
		return false;
	}
}

@SuppressWarnings("serial")
class ScreeningListTableModel extends AbstractTableModel
	implements SortingListTableModel
{
	String[] colnames = new String[7];
	int [] widths = { 8, 18, 12, 15, 15, 16, 16 };
	private int sortColumn = 0;
	ArrayList<AlarmScreening> screenings = new ArrayList<AlarmScreening>();
//	private AlarmConfig alarmConfig = new AlarmConfig();
	private ScreeningListPanel parentPanel = null;
	private SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd-HH:mm:ss");

	public ScreeningListTableModel(ScreeningListPanel parentPanel)
	{
		this.parentPanel = parentPanel;
		colnames[0] = parentPanel.parentFrame.genericLabels.getString("ID");
		colnames[1] = parentPanel.parentFrame.genericLabels.getString("name");
		colnames[2] = parentPanel.parentFrame.genericLabels.getString("dataType");
		colnames[3] = parentPanel.parentFrame.genericLabels.getString("site");
		colnames[4] = parentPanel.parentFrame.eventmonLabels.getString("alarmGroup");
		colnames[5] = parentPanel.parentFrame.eventmonLabels.getString("effectiveDate");
		colnames[6] = parentPanel.parentFrame.genericLabels.getString("lastMod") + " "
			+ DecodesSettings.instance().guiTimeZone;
		sdf.setTimeZone(TimeZone.getTimeZone(DecodesSettings.instance().guiTimeZone));
	}

	public void delete(int row)
	{
		if (row < 0 || row >= screenings.size())
			return;
		
		AlarmScreening scrn = screenings.get(row);
		SqlDatabaseIO sqldbio = (SqlDatabaseIO)decodes.db.Database.getDb().getDbIo();
		AlarmDAO alarmDAO = new AlarmDAO(sqldbio);
		
		try
		{
			
			alarmDAO.deleteScreening(scrn.getScreeningId());
			screenings.remove(row);
			fireTableDataChanged();
		}
		catch (DbIoException ex)
		{
			parentPanel.parentFrame.showError("Cannot delete alarm screening: " + ex);
		}
		finally
		{
			alarmDAO.close();
		}
	}

	public void reload()
	{
		SqlDatabaseIO sqldbio = (SqlDatabaseIO)decodes.db.Database.getDb().getDbIo();
		AlarmDAI alarmDAO = sqldbio.makeAlarmDAO();
		SiteDAI siteDAO = sqldbio.makeSiteDAO();
		
		try
		{
			ArrayList<AlarmScreening> tscrns = alarmDAO.getAllScreenings();
			for(AlarmScreening tscrn : tscrns)
			{
				if (!DbKey.isNull(tscrn.getSiteId()))
				{
					Site site = null;
					try { site = siteDAO.getSiteById(tscrn.getSiteId()); }
					catch (NoSuchObjectException ex)
					{
						Logger.instance().warning("Screening with id=" + tscrn.getKey() + " '"
							+ tscrn.getScreeningName() + "' has invalid site ID=" + tscrn.getSiteId()
							+ " -- will set to null.");
						tscrn.setSiteId(DbKey.NullKey);
						site = null;
					}
					if (site != null)
						tscrn.getSiteNames().add(site.getPreferredName());
				}
				
				if (!DbKey.isNull(tscrn.getDatatypeId()))
					tscrn.setDataType(decodes.db.Database.getDb().dataTypeSet.getById(tscrn.getDatatypeId()));
				
				if (!DbKey.isNull(tscrn.getAlarmGroupId()))
				{
					AlarmGroup grp = parentPanel.parentFrame.groupListPanel.getGroupById(tscrn.getAlarmGroupId());
					if (grp != null)
						tscrn.setGroupName(grp.getName());
				}
			}
			
			// Remove anything with null key == a new screening not yet saved.
			for(Iterator<AlarmScreening> scrit = screenings.iterator(); scrit.hasNext(); )
			{
				AlarmScreening scrn = scrit.next();
				if (!DbKey.isNull(scrn.getScreeningId()))
					scrit.remove();
			}
			// Now add in all the existing screenings
			screenings.addAll(tscrns);
			
			// TODO what about if a screening has an edit panel and is open. Changes will be lost.

			sortByColumn(sortColumn);
		}
		catch (DbIoException ex)
		{
			parentPanel.parentFrame.showError("Cannot read screenings: " + ex);
		}
		finally
		{
			siteDAO.close();
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
		return screenings.size();
	}

	@Override
	public Object getValueAt(int row, int col)
	{
		return getColumnValue(getScreeningAt(row), col);
	}
	
	public AlarmScreening getScreeningAt(int row)
	{
		return (AlarmScreening)getRowObject(row);
	}


	public String getColumnValue(AlarmScreening scrn, int col)
	{
		switch(col)
		{
		case 0: return scrn.getScreeningId().toString();
		case 1: return scrn.getScreeningName();
		case 2: return scrn.getDataType() == null ? "null" : scrn.getDataType().getCode();
		case 3: return scrn.getSiteNames().size()==0 ? "(none)" : scrn.getSiteNames().get(0).getNameValue();
		case 4: return scrn.getGroupName() == null ? "" : scrn.getGroupName();
		case 5: return scrn.getStartDateTime() == null ? " " : sdf.format(scrn.getStartDateTime());
		case 6: return sdf.format(scrn.getLastModified());
		default: return "";
		}
	}

	@Override
	public synchronized void sortByColumn(int column)
	{
		this.sortColumn = column;
		Collections.sort(screenings, new ScreeningComparator(sortColumn, this));
		fireTableDataChanged();
	}

	@Override
	public Object getRowObject(int row)
	{
		return screenings.get(row);
	}

}


class ScreeningComparator implements Comparator<AlarmScreening>
{
	private int sortColumn = 0;
	private ScreeningListTableModel model = null;
	
	ScreeningComparator(int sortColumn, ScreeningListTableModel model)
	{
		this.sortColumn = sortColumn;
		this.model = model;
	}
	
	@Override
	public int compare(AlarmScreening evt1, AlarmScreening evt2)
	{
		return TextUtil.strCompareIgnoreCase(
			model.getColumnValue(evt1, sortColumn),
			model.getColumnValue(evt2, sortColumn));
	}
}
