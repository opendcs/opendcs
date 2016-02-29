/**
 * $Id$
 * 
 * Copyright 2015 U.S. Army Corps of Engineers, Hydrologic Engineering Center.
 * 
 * $Log$
 * Revision 1.1  2015/11/12 15:12:39  mmaloney
 * Initial release.
 *
 */
package decodes.cwms.validation.gui;

import ilex.gui.DateCalendar;
import ilex.util.AsciiUtil;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.TimeZone;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import opendcs.dai.IntervalDAI;
import decodes.cwms.CwmsTimeSeriesDb;
import decodes.cwms.validation.Screening;
import decodes.cwms.validation.ScreeningCriteria;
import decodes.cwms.validation.dao.ScreeningDAI;
import decodes.db.Constants;
import decodes.db.DataType;
import decodes.db.Database;
import decodes.db.NoConversionException;
import decodes.sql.DbKey;
import decodes.util.DecodesSettings;

public class ScreeningEditTab extends JPanel
{
	private ScreeningEditFrame frame = null;
	private Screening screening = null;
	private JTextField screeningIdField = new JTextField(5);
	private JComboBox unitSystemCombo = new JComboBox(new String[] {"SI", "English"});
	private JTextField paramField = new JTextField(5);
	private JComboBox paramTypeCombo = new JComboBox(
		new String[] { "Inst", "Total", "Ave", "Max", "Min" });
	private JComboBox durationCombo = new JComboBox(new String[] { "" });
	private JTextArea descArea = new JTextArea("", 4, 60);
	private JTabbedPane seasonsPane = new JTabbedPane();
	private SimpleDateFormat seasonTitleSdf = new SimpleDateFormat("MMM dd");
	
	private String[] durationArray = new String[]{""};
	private String[] paramTypes = null;
	private String[] paramIds = null;
	private JLabel unitsLabel = new JLabel("(undefined)");
	private int currentUnitsSystem = 0;
	private SimpleDateFormat sdf = new SimpleDateFormat("MMM dd");
	private boolean committed = false;
	private JButton paramButton = null;

	
	public ScreeningEditTab(ScreeningEditFrame frame)
	{
		this.frame = frame;
		guiInit();
	}
	
	public void setScreening(Screening screening)
	{
		this.screening = screening;
		screeningIdField.setText(screening.getScreeningName());
		paramField.setEditable(false);
		paramField.setText(screening.getParamId());
		paramTypeCombo.setSelectedItem(
			screening.getParamTypeId() == null || screening.getParamTypeId().trim().length()==0
			? "Inst" : screening.getParamTypeId());
		durationCombo.setSelectedItem(screening.getDurationId());
		descArea.setText(screening.getScreeningDesc());
		
		String unitsAbbr = screening.getCheckUnitsAbbr();
		if (unitsAbbr != null && unitsAbbr.trim().length() > 0
		 && frame.getTheDb() instanceof CwmsTimeSeriesDb)
		{
			unitsLabel.setText("(" + unitsAbbr + ")");
			CwmsTimeSeriesDb ctsdb = (CwmsTimeSeriesDb)frame.getTheDb();
			if (unitsAbbr.equalsIgnoreCase(
				ctsdb.getBaseParam().getStoreUnits4Param(screening.getParamId())))
				unitSystemCombo.setSelectedIndex(currentUnitsSystem = 0);
			else if (unitsAbbr.equalsIgnoreCase(
				ctsdb.getBaseParam().getEnglishUnits4Param(screening.getParamId())))
				unitSystemCombo.setSelectedIndex(currentUnitsSystem = 1);
		}
		
		seasonsPane.removeAll();
		for(ScreeningCriteria season : screening.getCriteriaSeasons())
		{
			SeasonCheckPanel seasonPanel = new SeasonCheckPanel(frame, this);
			seasonPanel.setScreeningCriteria(season);
			if (season.getSeasonStart() == null)
				season.setSeasonStart(Calendar.JANUARY, 1);
			seasonsPane.add(seasonPanel, seasonTitleSdf.format(season.getSeasonStart().getTime()));
			
		}
		
		if (!DbKey.isNull(screening.getKey()))
		{
			// This is an existing screening. Disable the fields which cannot be changed.
			durationCombo.setEnabled(false);
			paramField.setEnabled(false);
			paramTypeCombo.setEnabled(false);
			paramButton.setEnabled(false);
		}
	}
	
	private void guiInit()
	{
		IntervalDAI intervalDAO = frame.getTheDb().makeIntervalDAO();
		try
		{
			durationArray = intervalDAO.getValidDurationCodes();
			durationCombo = new JComboBox(durationArray);
			paramTypes = frame.getTheDb().getParamTypes();
			paramTypeCombo = new JComboBox(paramTypes);
			
			ArrayList<DataType> dtarray = new ArrayList<DataType>();
			for(Iterator<DataType> dtit = Database.getDb().dataTypeSet.iterator();
				dtit.hasNext(); )
			{
				DataType dt = dtit.next();
				if (dt.getStandard().equalsIgnoreCase(Constants.datatype_CWMS))
					dtarray.add(dt);
			}
			paramIds = new String[dtarray.size()];
			for(int i=0; i<dtarray.size(); i++)
				paramIds[i] = dtarray.get(i).getCode();
			Arrays.sort(paramIds);

		}
		catch(Exception ex)
		{
			String msg = "Error reading durations & Param Types: " + ex;
			frame.showError(msg);
			System.err.println(msg);
			ex.printStackTrace(System.err);
		}
		finally
		{
			intervalDAO.close();
		}
		
		
		setLayout(new BorderLayout());
		
		JPanel north = new JPanel(new GridBagLayout());
		this.add(north, BorderLayout.NORTH);
		
		// LINE 1: Screening ID and Duration
		north.add(new JLabel("Screening ID:"),
			new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(4, 10, 2, 2), 0, 0));
		screeningIdField.setEditable(false);
		north.add(screeningIdField,
			new GridBagConstraints(1, 0, 4, 1, 1.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
				new Insets(4, 0, 2, 2), 0, 0));
		JButton renameButton = new JButton("Rename");
		renameButton.addActionListener(
			new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					renamePressed();
				}
			});
		north.add(renameButton,
			new GridBagConstraints(5, 0, 1, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(4, 2, 2, 10), 0, 0));

		north.add(new JLabel("Duration:"),
			new GridBagConstraints(6, 0, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(2, 10, 2, 2), 0, 0));
		north.add(durationCombo,
			new GridBagConstraints(7, 0, 1, 1, 0.1, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(2, 0, 2, 2), 0, 0));

		// Line 2 Param, Param Type, Units
		north.add(new JLabel("Param ID:"),
			new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(2, 10, 2, 2), 0, 0));
		north.add(paramField,
			new GridBagConstraints(1, 1, 1, 1, 1.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
				new Insets(2, 0, 2, 2), 0, 0));
		paramButton = new JButton("Select");
		paramButton.addActionListener(
			new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					paramPressed();
				}
			});
		north.add(paramButton,
			new GridBagConstraints(2, 1, 1, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(2, 2, 2, 10), 0, 0));
		north.add(new JLabel("Param Type:"),
			new GridBagConstraints(3, 1, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(2, 10, 2, 2), 0, 0));
		north.add(paramTypeCombo,
			new GridBagConstraints(4, 1, 1, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(2, 2, 2, 10), 0, 0));
		
		north.add(new JLabel("Unit System:"),
			new GridBagConstraints(5, 1, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(4, 10, 2, 2), 0, 0));
		unitSystemCombo.addActionListener(
			new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					unitSystemSelected();
				}
			});
		north.add(unitSystemCombo,
			new GridBagConstraints(6, 1, 1, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(4, 2, 2, 10), 0, 0));
		north.add(unitsLabel,
			new GridBagConstraints(7, 1, 1, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(4, 2, 2, 10), 0, 0));
		
		// Line 4: Text Area for Description
		JScrollPane scrollPane = new JScrollPane();
		scrollPane.getViewport().add(descArea);
		scrollPane.setBorder(new TitledBorder("Description"));
		descArea.setWrapStyleWord(true);
		descArea.setLineWrap(true);
		north.add(scrollPane,
			new GridBagConstraints(0, 2, 8, 1, 1.0, 1.0,
				GridBagConstraints.NORTH, GridBagConstraints.BOTH,
				new Insets(2, 10, 4, 10), 0, 0));
	
		
		// Center is a Tabbed pane of Seasons
		seasonsPane.setBorder(new TitledBorder("Seasons"));
		this.add(seasonsPane, BorderLayout.CENTER);
		
		// East contains buttons to add & delete seasons.
		JPanel east = new JPanel(new GridBagLayout());
		this.add(east, BorderLayout.EAST);
		JButton addSeasonButton = new JButton("Add Season");
		addSeasonButton.addActionListener(
			new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					addSeasonPressed();
				}
			});
		east.add(addSeasonButton,
			new GridBagConstraints(0, 0, 1, 1, 0.0, 0.5,
				GridBagConstraints.SOUTH, GridBagConstraints.HORIZONTAL,
				new Insets(4, 4, 2, 4), 0, 0));
		JButton delSeasonButton = new JButton("Delete Season");
		delSeasonButton.addActionListener(
			new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					delSeasonPressed();
				}
			});
		east.add(delSeasonButton,
			new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
				new Insets(2, 4, 2, 4), 0, 0));
		JButton sortSeasonsButton = new JButton("Sort Seasons");
		sortSeasonsButton.addActionListener(
			new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					sortSeasonsPressed();
				}
			});
		east.add(sortSeasonsButton,
			new GridBagConstraints(0, 2, 1, 1, 0.0, 0.5,
				GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL,
				new Insets(2, 4, 2, 4), 0, 0));

		
		// South is Commit and Close buttons
		JPanel south = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
		this.add(south, BorderLayout.SOUTH);
		
		JButton commitButton = new JButton("Commit");
		commitButton.addActionListener(
			new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					commitPressed();
				}
			});
		south.add(commitButton);

		JButton closeButton = new JButton("Close");
		closeButton.addActionListener(
			new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					closePressed();
				}
			});
		south.add(closeButton);

	}

	protected void sortSeasonsPressed()
	{
		ArrayList<SeasonCheckPanel> scps = new ArrayList<SeasonCheckPanel>();
		for(int idx = 0; idx < seasonsPane.getComponentCount(); idx++)
			scps.add((SeasonCheckPanel)seasonsPane.getComponentAt(idx));
		Collections.sort(scps,
			new Comparator<SeasonCheckPanel>()
			{
				@Override
				public int compare(SeasonCheckPanel o1, SeasonCheckPanel o2)
				{
					int ret = o1.getSeason().getSeasonStart().get(Calendar.MONTH)
						- o2.getSeason().getSeasonStart().get(Calendar.MONTH);
					if (ret != 0)
						return ret;
					return o1.getSeason().getSeasonStart().get(Calendar.DAY_OF_MONTH)
						- o2.getSeason().getSeasonStart().get(Calendar.DAY_OF_MONTH);
				}
			});
		seasonsPane.removeAll();
		for(SeasonCheckPanel scp : scps)
			seasonsPane.add(scp, sdf.format(scp.getSeason().getSeasonStart().getTime()));
	}

	protected void unitSystemSelected()
	{
		if (!(frame.getTheDb() instanceof CwmsTimeSeriesDb))
			return;
		CwmsTimeSeriesDb ctsdb = (CwmsTimeSeriesDb)frame.getTheDb();

		String paramId = paramField.getText().trim();
		if (paramId.length() == 0)
			return;
		
		String paramUnits = 
			unitSystemCombo.getSelectedIndex() == 0 ? ctsdb.getBaseParam().getStoreUnits4Param(paramId)
			: ctsdb.getBaseParam().getEnglishUnits4Param(paramId);
		
		if (screening.getCheckUnitsAbbr() != null
		 && screening.getCheckUnitsAbbr().trim().length() > 0
		 && !paramUnits.equalsIgnoreCase(screening.getCheckUnitsAbbr()))
		{
			int res = JOptionPane.showConfirmDialog(frame,
				AsciiUtil.wrapString(
				"Units were previously set to '" + screening.getCheckUnitsAbbr()
				+ "'. Convert already-entered limits to '" + paramUnits + "'?", 60),
				"Convert Existing Units", JOptionPane.YES_NO_CANCEL_OPTION);
			if (res == JOptionPane.CANCEL_OPTION)
			{
				unitSystemCombo.setSelectedIndex(currentUnitsSystem);
				return;
			}
			if (res == JOptionPane.YES_OPTION)
			{
				try { screening.convertUnits(paramUnits); }
				catch(NoConversionException ex)
				{
					frame.showError(ex.getMessage());
				}
				setScreening(screening);
			}
			else // Leave selection but don't convert units.
			{
				screening.setCheckUnitsAbbr(paramUnits);
			}
		}
		unitsLabel.setText("(" + paramUnits + ")");
		currentUnitsSystem = unitSystemCombo.getSelectedIndex();
	}

	protected void delSeasonPressed()
	{
		
		if (seasonsPane.getComponentCount() <= 1)
		{
			frame.showError("A screening must have at least one screening.");
			return;
		}
//		int idx = seasonsPane.getSelectedIndex();
		SeasonCheckPanel scp = (SeasonCheckPanel)seasonsPane.getSelectedComponent();
		ScreeningCriteria sc = scp.getSeason();
		int res = JOptionPane.showConfirmDialog(frame,
			"Confirm delete of season starting at " + sdf.format(sc.getSeasonStart().getTime()),
			"Confirm Season Delete", JOptionPane.YES_NO_OPTION);
		if (res != JOptionPane.YES_OPTION)
			return;
		seasonsPane.remove(scp);
	}

	protected void addSeasonPressed()
	{
		// TODO Auto-generated method stub
		int month = Calendar.JANUARY;
		int day = 1;
		if (seasonsPane.getComponentCount() >= 0)
		{
			SeasonCheckPanel scp = (SeasonCheckPanel)
				seasonsPane.getComponentAt(seasonsPane.getComponentCount()-1);
			ScreeningCriteria sc = scp.getSeason();
			month = sc.getSeasonStart().get(Calendar.MONTH);
			day = sc.getSeasonStart().get(Calendar.DAY_OF_MONTH);
		}
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.MONTH, month);
		cal.set(Calendar.DAY_OF_MONTH, day);
		cal.add(Calendar.DAY_OF_MONTH, 1);
		DateCalendar dc = new DateCalendar("Season Start", cal.getTime(), "MMM dd", 
			TimeZone.getDefault());
		int res = JOptionPane.showConfirmDialog(frame, dc, "Select Start Date", JOptionPane.OK_CANCEL_OPTION);
		if (res == JOptionPane.CANCEL_OPTION)
			return;
		cal.setTime(dc.getDate());
		
		ScreeningCriteria sc = new ScreeningCriteria(cal);
		SeasonCheckPanel scp = new SeasonCheckPanel(frame, this);
		scp.setScreeningCriteria(sc);
		seasonsPane.add(scp, sdf.format(dc.getDate()));
	}

	protected void closePressed()
	{
		if (!committed)
		{
			int r = JOptionPane.showConfirmDialog(frame, "Save Changes?", "Save Changes?", 
				JOptionPane.YES_NO_CANCEL_OPTION);
			if (r == JOptionPane.CANCEL_OPTION)
				return;
			if (r == JOptionPane.YES_OPTION)
				commitPressed();
		}
		frame.closeScreening(this);
	}

	protected void commitPressed()
	{
		for(int idx = 0; idx < seasonsPane.getComponentCount(); idx++)
			if (!((SeasonCheckPanel)seasonsPane.getComponentAt(idx)).validateFields())
				return;
		
		for(int idx = 0; idx < seasonsPane.getComponentCount(); idx++)
			((SeasonCheckPanel)seasonsPane.getComponentAt(idx)).saveFields();
		
		screening.setParamId(paramField.getText().trim());
		screening.setDurationId((String)durationCombo.getSelectedItem());
		screening.setParamTypeId((String)paramTypeCombo.getSelectedItem());
		String s = unitsLabel.getText();
		screening.setCheckUnitsAbbr(s.substring(1, s.length()-1));
		screening.setScreeningDesc(descArea.getText());

		ScreeningDAI screeningDAO = null;
		try
		{
			screeningDAO = frame.getTheDb().makeScreeningDAO();
			screeningDAO.writeScreening(screening);
		}
		catch(Exception ex)
		{
			frame.showError("Error writing screening '" + screening.getScreeningName()
				+ "': " + ex);
		}
		finally
		{
			if (screeningDAO != null)
				screeningDAO.close();
		}
		committed = true;
	}


	protected void paramPressed()
	{
		String param = (String)JOptionPane.showInputDialog(this, 
			"Select Param: ", "Select Param", JOptionPane.PLAIN_MESSAGE, null,
			paramIds, null);
		if (param == null)
			return;
		
		paramField.setText(param);
		CwmsTimeSeriesDb ctsdb = (CwmsTimeSeriesDb)frame.getTheDb();
		String unitsAbbr = 
			unitSystemCombo.getSelectedIndex() == 0 ? ctsdb.getBaseParam().getStoreUnits4Param(param) :
				ctsdb.getBaseParam().getEnglishUnits4Param(param);
		unitsLabel.setText("(" + unitsAbbr + ")");
		screening.setCheckUnitsAbbr(unitsAbbr);
	}

	protected void renamePressed()
	{
		String newName = JOptionPane.showInputDialog("Enter new unique name:");
		if (newName == null)
			return;
		if (frame.getScreeningIdListTab().nameExists(newName))
		{
			frame.showError("A screening already exists with that name. Screening IDs must be unique.");
			return;
		}
		screeningIdField.setText(newName);
		frame.setTabLabel(this, newName);
	}

	public Screening getScreening()
	{
		return screening;
	}

	public JTabbedPane getSeasonsPane()
	{
		return seasonsPane;
	}

}
