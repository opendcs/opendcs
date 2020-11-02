package decodes.tsdb.alarm.editor;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.TimeZone;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import decodes.cwms.CwmsTimeSeriesDb;
import decodes.db.Constants;
import decodes.db.DataType;
import decodes.db.Database;
import decodes.db.DbEnum;
import decodes.db.EnumValue;
import decodes.db.Site;
import decodes.db.SiteName;
import decodes.dbeditor.SiteSelectDialog;
import decodes.decoder.FieldParseException;
import decodes.decoder.Season;
import decodes.hdb.HdbTimeSeriesDb;
import decodes.sql.DbKey;
import decodes.tsdb.BadScreeningException;
import decodes.tsdb.CompAppInfo;
import decodes.tsdb.DbIoException;
import decodes.tsdb.TimeSeriesDb;
import decodes.tsdb.TsdbAppTemplate;
import decodes.tsdb.alarm.AlarmGroup;
import decodes.tsdb.alarm.AlarmLimitSet;
import decodes.tsdb.alarm.AlarmScreening;
import decodes.tsdb.compedit.CAPEdit;
import decodes.tsdb.groupedit.HdbDatatypeSelectDialog;
import decodes.tsdb.groupedit.LocSelectDialog;
import decodes.tsdb.groupedit.ParamSelectDialog;
import decodes.tsdb.groupedit.SelectionMode;
import decodes.util.DecodesSettings;
import ilex.gui.DateTimeCalendar;
import ilex.util.StringPair;
import ilex.util.TextUtil;
import opendcs.dai.AlarmDAI;
import opendcs.dai.LoadingAppDAI;

@SuppressWarnings("serial")
public class ScreeningEditPanel 
	extends JPanel
{
	private AlarmEditFrame parentFrame = null;
	private SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd-HH:mm:ss");
	private AlarmScreening screening = null;
	private JTextField screeningNameField = new JTextField(30);
	private JCheckBox enabledCheck = new JCheckBox();
	private JTextField datatypeField = new JTextField(10);
	private JTextField siteNameField = new JTextField(10);
	private JTextField emailGroupField = new JTextField(10);
	private JTextField unitsField = new JTextField();
	private JTextArea descArea = new JTextArea("", 4, 60);
	private JTextField screeningIdField = new JTextField(5);
	private JTextField lastModifiedField = new JTextField(19);
	private JCheckBox startTimeCheck = new JCheckBox();
	private DateTimeCalendar startDateTimeCal = null;
	private String prevDatatypeValue = "";
	private Season defaultSeason = new Season();
	private JComboBox appCombo = new JComboBox();
	private ArrayList<CompAppInfo> compApps = null;
	private JTabbedPane seasonsPane = new JTabbedPane();
	private boolean committed = false;
	private String seasonNames[];
	private Season seasons[];
	
	
	public ScreeningEditPanel(AlarmEditFrame parentFrame)
	{
		super(new BorderLayout());
		this.parentFrame = parentFrame;
		
		TimeZone guiTimeZone = TimeZone.getTimeZone(DecodesSettings.instance().guiTimeZone);
		sdf.setTimeZone(guiTimeZone);
		
		Calendar cal = Calendar.getInstance(guiTimeZone);
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);

		startDateTimeCal = new DateTimeCalendar("("+guiTimeZone.getID()+")", cal.getTime(), "dd/MMM/yyyy", 
			guiTimeZone.getID());
		
		defaultSeason.setAbbr("(default)");
		guiInit();
	}
	


	
	private void guiInit()
	{
		JPanel north = new JPanel(new GridBagLayout());
		this.add(north, BorderLayout.NORTH);
		
		// LINE 1: Screening Name and Enabled
		north.add(new JLabel(parentFrame.eventmonLabels.getString("screening") + ":"),
			new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(2, 5, 1, 2), 0, 0));
		screeningNameField.setEditable(false);
		north.add(screeningNameField,
			new GridBagConstraints(1, 0, 1, 1, 0.4, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
				new Insets(2, 0, 1, 2), 0, 0));
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
			new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
				new Insets(2, 2, 1, 10), 0, 0));
		enabledCheck.setText(parentFrame.genericLabels.getString("enable"));
		north.add(enabledCheck,
			new GridBagConstraints(4, 0, 3, 1, 0.0, 0.0,
					GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
					new Insets(2, 2, 1, 2), 0, 0));

		
		
		// LINE 2 Datatype & Site
		north.add(new JLabel(parentFrame.genericLabels.getString("dataType")+":"),
			new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(1, 5, 1, 2), 0, 0));
		north.add(datatypeField,
			new GridBagConstraints(1, 1, 1, 1, 0.4, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
				new Insets(1, 0, 1, 2), 0, 0));
		
		datatypeField.addFocusListener(
			new FocusListener()
			{
				@Override
				public void focusGained(FocusEvent e)
				{
				}

				@Override
				public void focusLost(FocusEvent e)
				{
					if (!datatypeField.getText().equals(prevDatatypeValue))
						datatypeEntered();
				}
				
			});
		
		datatypeField.addActionListener(
			new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					datatypeEntered();
				}
			});
				
		JButton datatypeButton = new JButton(parentFrame.genericLabels.getString("select"));
		datatypeButton.addActionListener(
			new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					datatypePressed();
				}
			});
		north.add(datatypeButton,
			new GridBagConstraints(2, 1, 1, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(1, 2, 1, 10), 0, 0));
		
		north.add(new JLabel(parentFrame.genericLabels.getString("site") + ":"),
			new GridBagConstraints(3, 1, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(1, 10, 1, 2), 0, 0));
		north.add(siteNameField,
			new GridBagConstraints(4, 1, 1, 1, 0.3, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
				new Insets(1, 2, 1, 5), 0, 0));
		JButton siteButton = new JButton(parentFrame.genericLabels.getString("select"));
		siteButton.addActionListener(
			new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					sitePressed();
				}
			});
		north.add(siteButton,
			new GridBagConstraints(5, 1, 1, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(1, 2, 1, 5), 0, 0));

		// LINE 3 Email Group and Units
		north.add(new JLabel(parentFrame.eventmonLabels.getString("emailGroup")+":"),
			new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(1, 5, 1, 2), 0, 0));
		north.add(emailGroupField,
			new GridBagConstraints(1, 2, 1, 1, 0.4, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
				new Insets(1, 0, 1, 2), 0, 0));
		emailGroupField.setEditable(false);
		JButton emailGroupButton = new JButton(parentFrame.genericLabels.getString("select"));
		emailGroupButton.addActionListener(
			new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					emailGroupPressed();
				}
			});
		north.add(emailGroupButton,
			new GridBagConstraints(2, 2, 1, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(1, 2, 1, 10), 0, 0));
		
		north.add(new JLabel(parentFrame.genericLabels.getString("units") + ":"),
			new GridBagConstraints(3, 2, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(1, 10, 1, 2), 0, 0));
		unitsField.setEditable(false);
		north.add(unitsField,
			new GridBagConstraints(4, 2, 1, 1, 0.3, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
				new Insets(1, 2, 1, 5), 0, 0));

		// LINE 4: Effective Start Date/Time widget
		startTimeCheck.setText("Effective Start:");
		startTimeCheck.addActionListener(
			new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					startTimeChecked();
				}
			});

		north.add(startTimeCheck,
			new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(1, 10, 1, 0), 0, 0));
		north.add(startDateTimeCal,
			new GridBagConstraints(1, 3, 2, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
				new Insets(1, 0, 1, 2), 0, 0));
		
		// Fill in the Process combo with list of apps.
		LoadingAppDAI appDAO = TsdbAppTemplate.theDb.makeLoadingAppDAO();
		try
		{
			compApps = appDAO.listComputationApps(false);
			appCombo.addItem("<any>");
			for(int i=0; i<compApps.size(); i++)
			{
				CompAppInfo cai = compApps.get(i);
				appCombo.addItem("" + cai.getAppId() + ": " + cai.getAppName());
			}
		}
		catch (DbIoException e1)
		{
			parentFrame.showError("Cannot list computation apps: " + e1);
			e1.printStackTrace();
		}
		north.add(new JLabel("Loading App:"),
			new GridBagConstraints(3, 3, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(1, 10, 1, 2), 0, 0));
		north.add(appCombo,
			new GridBagConstraints(4, 3, 1, 1, 0.3, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
				new Insets(1, 2, 1, 5), 0, 0));

		// LINE 5 & 6: Text Area for Description. On right, Database ID and Last Modified
		JScrollPane scrollPane = new JScrollPane();
		scrollPane.getViewport().add(descArea);
		scrollPane.setBorder(new TitledBorder(parentFrame.genericLabels.getString("description")));
		descArea.setWrapStyleWord(true);
		descArea.setLineWrap(true);
		north.add(scrollPane,
			new GridBagConstraints(0, 4, 4, 3, 1.0, 0.0,
				GridBagConstraints.NORTH, GridBagConstraints.BOTH,
				new Insets(1, 5, 1, 5), 0, 0));

		north.add(new JLabel("ID:"),
				new GridBagConstraints(4, 4, 1, 1, 0.0, 0.0,
					GridBagConstraints.EAST, GridBagConstraints.NONE,
					new Insets(1, 10, 1, 2), 0, 0));
		north.add(screeningIdField,
				new GridBagConstraints(5, 4, 1, 1, 0.3, 0.0,
					GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
					new Insets(1, 1, 1, 5), 0, 0));
		screeningIdField.setEditable(false);

		north.add(new JLabel(parentFrame.genericLabels.getString("lastMod") + ":"),
				new GridBagConstraints(4, 5, 1, 1, 0.0, 0.0,
					GridBagConstraints.EAST, GridBagConstraints.NONE,
					new Insets(1, 5, 1, 2), 0, 0));
		north.add(lastModifiedField,
				new GridBagConstraints(5, 5, 1, 1, 0.3, 0.0,
					GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
					new Insets(1, 1, 1, 5), 0, 0));
		lastModifiedField.setEditable(false);
		
		north.add(new JLabel(""),
			new GridBagConstraints(5, 6, 1, 1, 0.0, 1.0,
					GridBagConstraints.NORTHWEST, GridBagConstraints.BOTH,
					new Insets(2, 2, 1, 5), 0, 0));

	
		// Center is a Tabbed pane of Seasons
		seasonsPane.setBorder(new TitledBorder("Seasons"));
		this.add(seasonsPane, BorderLayout.CENTER);
		
		
		// East contains buttons to add & delete seasons.
		JPanel east = new JPanel(new GridBagLayout());
		this.add(east, BorderLayout.EAST);
		JButton addSeasonButton = new JButton(parentFrame.eventmonLabels.getString("addSeason"));
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
		JButton delSeasonButton = new JButton(parentFrame.eventmonLabels.getString("deleteSeason"));
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
		JButton sortSeasonsButton = new JButton(parentFrame.eventmonLabels.getString("sortSeasons"));
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

	
	protected void startTimeChecked()
	{
		// TODO Auto-generated method stub
		startDateTimeCal.setEnabled(startTimeCheck.isSelected());
			
	}


	protected void datatypeEntered()
	{
		// TODO Auto-generated method stub
		prevDatatypeValue = datatypeField.getText();
		TimeSeriesDb tsdb = parentFrame.parentTsdbApp.theDb;
		String dtStd = tsdb.isHdb() ? Constants.datatype_HDB :
			tsdb.isCwms() ? Constants.datatype_CWMS :
			DecodesSettings.instance().dataTypeStdPreference;
		DataType dt = DataType.getDataType(dtStd, prevDatatypeValue);
		String units = parentFrame.parentTsdbApp.theDb.getStorageUnitsForDataType(dt);
		if (units == null || units.trim().length() == 0)
			unitsField.setText("");
		else
			unitsField.setText(units);	
	}


//	protected void unitsPressed()
//	{
//		// TODO Auto-generated method stub
//		
//	}


	protected void emailGroupPressed()
	{
		ArrayList<AlarmGroup> grps = parentFrame.groupListPanel.model.getGroupList();
		String [] grpNames = new String[grps.size() + 1];
		grpNames[0] = "(none)";
		for(int idx = 0; idx < grps.size(); idx++)
			grpNames[idx+1] = grps.get(idx).getName();
		String sel = emailGroupField.getText().trim();
		if (sel.length() == 0)
			sel = grpNames[0];
		
		sel = (String)JOptionPane.showInputDialog(parentFrame, "Select Alarm Email Group:", 
			"Group Selection", JOptionPane.QUESTION_MESSAGE, null, grpNames, sel);
		if (sel == null)
			return;
		if (sel.equals(grpNames[0]))
			emailGroupField.setText("");
		else
			emailGroupField.setText(sel);
	}


	protected void sitePressed()
	{
		if (parentFrame.parentTsdbApp.theDb.isCwms())
		{
			LocSelectDialog locSelectDialog = 
					new LocSelectDialog(CAPEdit.instance().getFrame(), 
						(CwmsTimeSeriesDb)parentFrame.parentTsdbApp.theDb,
						SelectionMode.CompEditGroup);

			locSelectDialog.setCurrentValue(siteNameField.getText());
			parentFrame.launchDialog(locSelectDialog);
			if (!locSelectDialog.isCancelled())
			{
				StringPair result = locSelectDialog.getResult();
				if (result != null)
					siteNameField.setText(result.second);
			}
		}
		else
		{
			SiteSelectDialog siteSelectDialog = new SiteSelectDialog(this);
			parentFrame.launchDialog(siteSelectDialog);
			Site site = siteSelectDialog.getSelectedSite();
			if (site != null)
			{
				SiteName sn = site.getPreferredName();
				if (sn != null)
					siteNameField.setText(sn.getNameValue());
			}
		}
	}


	public void setScreening(AlarmScreening screening)
	{
		this.screening = screening;
		screeningNameField.setText(screening.getScreeningName());
		DataType dt = screening.getDataType();
		datatypeField.setText(dt == null ? "" : dt.getCode());
		prevDatatypeValue = datatypeField.getText();
		emailGroupField.setText(screening.getGroupName());
		enabledCheck.setSelected(screening.isEnabled());
		Date sdt = screening.getStartDateTime();
		if (sdt != null)
			startDateTimeCal.setDate(sdt);
		startTimeCheck.setSelected(sdt != null);
		startTimeChecked();
		
		ArrayList<SiteName> sns = screening.getSiteNames();
		if (sns.size() == 0)
		{
			siteNameField.setText("");
		}
		else
		{
			siteNameField.setText(sns.get(0).getNameValue());
		}
		
		screeningIdField.setText(DbKey.isNull(screening.getScreeningId()) ? "" 
			: screening.getScreeningId().toString());
		Date lmt = screening.getLastModified();
		lastModifiedField.setText(lmt == null ? "" : sdf.format(lmt));
		String desc = screening.getDescription();
		descArea.setText(desc == null ? "" : desc);
		
		seasonsPane.removeAll();
		for(AlarmLimitSet limitSet : screening.getLimitSets())
		{
			SeasonPanel seasonPanel = new SeasonPanel(parentFrame, this);
			seasonPanel.setLimitSet(limitSet);
			seasonsPane.add(seasonPanel, 
				limitSet.getSeason() == null ? "default" : limitSet.getSeason().getAbbr());
		}
		
		if (DbKey.isNull(screening.getAppId()))
			appCombo.setSelectedIndex(0);
		else
		{
			for(int i=0; i < compApps.size(); i++)
			{
				CompAppInfo app = compApps.get(i);
				if (screening.getAppId().equals(app.getAppId()))
				{
					appCombo.setSelectedIndex(i+1);
					break;
				}
			}
		}
	}
	

	protected void sortSeasonsPressed()
	{
		ArrayList<SeasonPanel> scps = new ArrayList<SeasonPanel>();
		for(int idx = 0; idx < seasonsPane.getComponentCount(); idx++)
			scps.add((SeasonPanel)seasonsPane.getComponentAt(idx));
		Collections.sort(scps,
			new Comparator<SeasonPanel>()
			{
				@Override
				public int compare(SeasonPanel o1, SeasonPanel o2)
				{
					Season s1 = o1.getLimitSet().getSeason();
					Season s2 = o2.getLimitSet().getSeason();
					String start1 = s1.getStart() == null ? "0" : s1.getStart();
					if (start1.length() > 2 && start1.charAt(1) == '/')
						start1 = "0" + s1;
					String start2 = s2.getStart() == null ? "0" : s2.getStart();
					if (start2.length() > 2 && start2.charAt(1) == '/')
						start2 = "0" + s1;
					return start1.compareTo(start2);
				}
			});
		seasonsPane.removeAll();
		for(SeasonPanel scp : scps)
			seasonsPane.add(scp, scp.getLimitSet().getSeason().getAbbr());
	}
	protected void delSeasonPressed()
	{
		if (seasonsPane.getComponentCount() <= 1)
		{
			parentFrame.showError("A screening must have at least one season.");
			return;
		}
		SeasonPanel scp = (SeasonPanel)seasonsPane.getSelectedComponent();
		AlarmLimitSet limitSet = scp.getLimitSet();
		
		String seasonName = limitSet.getSeasonName() == null ? "(default)" :
			limitSet.getSeasonName();
		int res = JOptionPane.showConfirmDialog(parentFrame,
			"Confirm delete of season '" + limitSet.getSeasonName() + "'",
			"Confirm Season Delete", JOptionPane.YES_NO_OPTION);
		if (res != JOptionPane.YES_OPTION)
			return;
		seasonsPane.remove(scp);
	}

	
	Season selectSeason(String curSelection)
	{
		if (seasonNames == null)
		{
			DbEnum seasonEnum = Database.getDb().getDbEnum("season");
			if (seasonEnum == null)
			{
				parentFrame.showError("No 'season' enumeration in this database. "
					+ "Run dbimport on $DCSTOOL_HOME/edit-db/enum/season.xml");
				return null;
			}
			seasonNames = new String[seasonEnum.size()+1];
			seasonNames[0] = defaultSeason.getAbbr();
			seasons = new Season[seasonEnum.size()+1];
			seasons[0] = defaultSeason;
	
			int idx = 1;
			for(Iterator<EnumValue> it = seasonEnum.values().iterator(); it.hasNext(); idx++)
			{
				EnumValue ev = it.next();
				Season season = new Season();
				try
				{
					season.setFromEnum(ev);
					seasons[idx] = season;
				}
				catch (FieldParseException ex)
				{
					// Auto-generated catch block
					System.err.println("Error setting season from enum value '" 
						+ ev.getValue() + "': " + ex);
					ex.printStackTrace(System.err);
					seasonNames[idx] = "bad season";
					continue;
				}
				seasonNames[idx] = season.getAbbr() + " - " + season.getStart();
			}
		}
		
		int curIdx = 0;
		for(int idx = 1; curSelection != null && idx < seasonNames.length; idx++)
			if (TextUtil.startsWithIgnoreCase(seasonNames[idx], curSelection))
				curIdx = idx;
		
		Object obj = JOptionPane.showInputDialog(parentFrame, 
			"Select Season:", "Select Season", 
			JOptionPane.QUESTION_MESSAGE, null, seasonNames, seasonNames[curIdx]);
		if (obj == null)
			return null;
		String s = (String)obj;
		Season selectedSeason = null;
		for(int idx = 0; idx < seasonNames.length; idx++)
		{
			if (s.equals(seasonNames[idx]))
			{
				selectedSeason = seasons[idx];
				break;
			}
		}
		return selectedSeason;
	}
	
	SeasonPanel getPanelFor(Season season)
	{
		for(int idx = 0; idx < seasonsPane.getComponentCount(); idx++)
		{
			SeasonPanel scp = (SeasonPanel)seasonsPane.getComponentAt(idx);
			Season panelSeason = scp.getLimitSet().getSeason();
			if (seasonEqual(season, panelSeason))
				return scp;
		}
		return null;
	}
	
	protected void addSeasonPressed()
	{
		Season selectedSeason = selectSeason(null);
		if (selectedSeason == null)
			return;
		
		SeasonPanel scp = getPanelFor(selectedSeason);
		if (scp != null)
		{
			seasonsPane.setSelectedComponent(scp);
			return;
		}
		
		SeasonPanel newPanel = new SeasonPanel(parentFrame, this);
		AlarmLimitSet limitSet = new AlarmLimitSet();
		limitSet.setSeason(selectedSeason);
		newPanel.setLimitSet(limitSet);
		
		seasonsPane.add(newPanel, selectedSeason.getAbbr());
		seasonsPane.setSelectedComponent(newPanel);
	}
	
	/**
	 * Compare seasons and allow for either to be null.
	 * @param s1
	 * @param s2
	 * @return
	 */
	private boolean seasonEqual(Season s1, Season s2)
	{
		if (s1 == null)
			return s2 == null;
		else if (s2 == null)
			return false;
		else
			return s1.getAbbr().equals(s2.getAbbr());
	}

	protected void closePressed()
	{
		try
		{
			if (changesMade())
			{
				int r = JOptionPane.showConfirmDialog(parentFrame, "Save Changes?", "Save Changes?", 
					JOptionPane.YES_NO_CANCEL_OPTION);
				if (r == JOptionPane.CANCEL_OPTION)
					return;
				if (r == JOptionPane.YES_OPTION)
					commitPressed();
			}
		}
		catch(BadScreeningException ex)
		{
			int r = JOptionPane.showConfirmDialog(parentFrame, 
				"There are errors in the unsaved fields on this panel. Exit without save?", 
				"Exit without save?", JOptionPane.YES_NO_CANCEL_OPTION);
			if (r != JOptionPane.YES_OPTION)
				return;
		}

		parentFrame.closeScreening(this);
	}
	
	/**
	 * @return true if changes made to any data.
	 */
	boolean changesMade()
		throws BadScreeningException
	{
		AlarmScreening scrn = new AlarmScreening();
		fieldsToScreening(scrn);
		return !scrn.equals(screening);
	}
	
	/**
	 * Transcribe the current field settings back to the passed (temporary) screening object.
	 * @throws BadScreeningException if any errors found in the screening or limit sets.
	 */
	private void fieldsToScreening(AlarmScreening scrn)
		throws BadScreeningException
	{
		if (screeningNameField.getText().trim().length() == 0)
			throw new BadScreeningException("Screening Name cannot be blank!");
		
		String dtcode = datatypeField.getText().trim();
		if (dtcode.length() == 0)
			throw new BadScreeningException("Data Type cannot be blank!");
		try
		{
			scrn.setDataType(parentFrame.parentTsdbApp.getTsdb().lookupDataType(dtcode));
		}
		catch (Exception ex)
		{
			throw new BadScreeningException("Invalid data type '" + dtcode + "': " + ex);
		}
		
		scrn.setScreeningName(screeningNameField.getText().trim());
		scrn.setEnabled(enabledCheck.isSelected());
		try
		{
			scrn.setDataType(parentFrame.parentTsdbApp.getTsdb().lookupDataType(
				datatypeField.getText().trim()));
		}
		catch (Exception ex)
		{
			// Shouldn't happen because Validate is called first.
		}
		
		String s = siteNameField.getText().trim();
		if (s.length() == 0)
		{
			scrn.setSiteId(DbKey.NullKey);
			scrn.getSiteNames().clear();
		}
		else
		{
			try
			{
				scrn.setSiteId(parentFrame.parentTsdbApp.getTsdb().lookupSiteID(s));
				
			}
			catch (DbIoException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		s = emailGroupField.getText().trim();
		if (s.length() == 0)
		{
			scrn.setGroupName(null);
			scrn.setAlarmGroupId(DbKey.NullKey);
		}
		else
		{
			scrn.setGroupName(s);
			for (AlarmGroup grp : parentFrame.groupListPanel.model.getGroupList())
				if (grp.getName().equals(s))
				{
					scrn.setAlarmGroupId(grp.getAlarmGroupId());
				}
		}
		
		s = descArea.getText().trim();
		scrn.setDescription(s.length() == 0 ? null : s);
		
		scrn.setStartDateTime(startTimeCheck.isSelected() ? startDateTimeCal.getDate() : null);
		
		scrn.getLimitSets().clear();
		for(int idx = 0; idx < seasonsPane.getComponentCount(); idx++)
		{
			SeasonPanel scp = (SeasonPanel)seasonsPane.getComponentAt(idx);
			AlarmLimitSet als = new AlarmLimitSet();
			als.setSeason(scp.getLimitSet().getSeason());
			scp.fieldsToLimitSet(als);
			scrn.addLimitSet(als);
		}
		
		int appIdx = appCombo.getSelectedIndex();
		if (appIdx == 0)
			scrn.setAppId(DbKey.NullKey);
		else
		{
			CompAppInfo compApp = compApps.get(appIdx - 1);
			scrn.setAppInfo(compApp);
			scrn.setAppId(compApp.getAppId());
		}
	}

	protected void commitPressed()
	{
		// Strategy is to create a temporary screening and try to parse all the
		// fields (including subordinate Limit Sets) into it.
		// If the parse succeeds, then there are no errors. Copy the data back into
		// the actual screening objects associated with this panel and season panels.
		// Then write the screening to the database.
		
		AlarmScreening scrn = new AlarmScreening();
		scrn.setScreeningId(screening.getScreeningId());
		try
		{
			fieldsToScreening(scrn);
		}
		catch(BadScreeningException ex)
		{
			parentFrame.showError(ex.toString());
			return;
		}
		
		if (DbKey.isNull(scrn.getAppId()))
		{
			if (parentFrame.showConfirm("Confirm No App", 
				"You have not associated this screening with a Loading App."
				+ " That means it cannot be executed by any computation. You should associate"
				+ " the screening the the comp-proc application ID that executes the computation."
				+ " Continue save with no app?", JOptionPane.YES_NO_OPTION)
					== JOptionPane.NO_OPTION)
				return;
		}
		
		// Write the screening to the database.
		AlarmDAI alarmDAO = parentFrame.parentTsdbApp.getTsdb().makeAlarmDAO();
		try
		{
			alarmDAO.writeScreening(scrn);
			screeningIdField.setText("" + scrn.getScreeningId());
			Date lmt = scrn.getLastModified();
			lastModifiedField.setText(lmt == null ? "" : sdf.format(lmt));
			parentFrame.screeningListPanel.refreshPressed();
		}
		catch(Exception ex)
		{
			parentFrame.showError("Error writing screening to database: " + ex);
		}
		finally
		{
			alarmDAO.close();
		}
		
		// Validation and parse passed. Data is sitting in the temporary screening.
		// Copy it back to the actual object being edited.
		screening.copyFrom(scrn);
		parentFrame.setTabLabel(this, screening.getScreeningName());

		committed = true;
	}


	protected void datatypePressed()
	{
		String newDT = null;
		if (parentFrame.parentTsdbApp.theDb.isCwms())
		{
			ParamSelectDialog paramSelectDialog = 
				new ParamSelectDialog(parentFrame, parentFrame.parentTsdbApp.theDb,
					SelectionMode.CompEditGroup);
			paramSelectDialog.setCurrentValue(datatypeField.getText());
	
			parentFrame.launchDialog(paramSelectDialog);
			if (!paramSelectDialog.isCancelled())
			{
				StringPair result = paramSelectDialog.getResult();
				newDT = result.second;
			}
		}
		else if (parentFrame.parentTsdbApp.theDb.isHdb())
		{
			HdbDatatypeSelectDialog dlg = new HdbDatatypeSelectDialog(parentFrame, 
				(HdbTimeSeriesDb)parentFrame.parentTsdbApp.theDb);
			dlg.setCurrentValue(datatypeField.getText());
			parentFrame.launchDialog(dlg);
			StringPair result = dlg.getResult();
			if (result != null)
				newDT = result.first;
		}
		else if (parentFrame.parentTsdbApp.theDb.isOpenTSDB())
		{
			newDT = JOptionPane.showInputDialog(this, "Enter Data Type:");
		}

		if (newDT != null && !newDT.equals(prevDatatypeValue))
		{
			datatypeField.setText(newDT);
			datatypeEntered();
		}
	}

	
	
	protected void renamePressed()
	{
		String newName = JOptionPane.showInputDialog("Enter new unique name:");
		if (newName == null)
			return;
		if (parentFrame.screeningListPanel.nameExists(newName))
		{
			parentFrame.showError("An Alarm Screening already exists with that name. Names must be unique.");
			return;
		}
		screeningNameField.setText(newName);
	}
	
	
	

	public AlarmScreening getScreening()
	{
		return screening;
	}
//
//	public JTabbedPane getSeasonsPane()
//	{
//		return seasonsPane;
//	}


	public void setSeasonTabLabel(SeasonPanel seasonPanel, String abbr)
	{
		int idx = seasonsPane.indexOfComponent(seasonPanel);
		if (idx < 0)
			return;
		seasonsPane.setTitleAt(idx, abbr);
	}

}
