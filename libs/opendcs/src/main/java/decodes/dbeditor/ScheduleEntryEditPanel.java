package decodes.dbeditor;

import ilex.gui.DateTimeCalendar;
import ilex.gui.Help;
import ilex.util.LoadResourceBundle;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.ResourceBundle;
import java.util.TimeZone;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import opendcs.dai.LoadingAppDAI;

import decodes.db.Constants;
import decodes.db.Database;
import decodes.db.DatabaseException;
import decodes.db.RoutingSpec;
import decodes.db.ScheduleEntry;
import decodes.tsdb.CompAppInfo;
import decodes.tsdb.DbIoException;
import decodes.tsdb.IntervalIncrement;

@SuppressWarnings("serial")
public class ScheduleEntryEditPanel 
	extends DbEditorTab 
	implements ChangeTracker, EntityOpsController
{
	static ResourceBundle genericLabels = DbEditorFrame.getGenericLabels();
	static ResourceBundle dbeditLabels = DbEditorFrame.getDbeditLabels();
	private DbEditorFrame parent = null;
	private ScheduleEntry origObject = null;
	
	private JTextField nameField = new JTextField();
	private JComboBox schedulerDaemonCombo = new JComboBox();
	private JTextField routingSpecField = new JTextField();
	private JTextField lastModifiedField = new JTextField();
	private JCheckBox enabledCheck = new JCheckBox();
	private JRadioButton runOnceRadio = new JRadioButton();
	private JRadioButton runContinuouslyRadio = new JRadioButton();
	private JRadioButton runPeriodicallyRadio = new JRadioButton();
	private JTextField timeIncField = new JTextField(5);
	private JComboBox timeUnitCombo = new JComboBox(
		new String[] { "Minutes", "Hours", "Days" });
	private DateTimeCalendar startDateTimeCal = null;
	private Calendar userTzCal = Calendar.getInstance();
	private Calendar utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
	private ArrayList<CompAppInfo> appsInCombo = new ArrayList<CompAppInfo>();
	private TimeZoneSelector tzSelector = new TimeZoneSelector();
	private RoutingSpec selectedRS = null;


	public ScheduleEntryEditPanel(DbEditorFrame parent, ScheduleEntry se)
	{
		super();
		this.parent = parent;
		this.origObject = se;
		guiInit();
		fillFields();
	}
	
	private void guiInit()
	{
		JPanel mainPanel = new JPanel(new GridBagLayout());
		this.setLayout(new BorderLayout());
		this.add(mainPanel, BorderLayout.CENTER);
		this.add(new EntityOpsPanel(this), BorderLayout.SOUTH);
		
		// Schedule Entry Name
		mainPanel.add(new JLabel(dbeditLabels.getString("ScheduleEntryPanel.EntityName")
			+ " " + genericLabels.getString("nameLabel")),
			new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(3, 25, 3, 2), 0, 0));
		nameField.setEditable(false);
		mainPanel.add(nameField,
			new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
				new Insets(3, 0, 3, 20), 0, 0));
		
		// Enabled Checkbox
		enabledCheck.setText(dbeditLabels.getString("ScheduleEntryPanel.EnabledFor"));
		mainPanel.add(enabledCheck,
			new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(3, 20, 3, 0), 0, 0));

		// Loading App Name
		// Make a list of loading apps with app-type
		appsInCombo.clear();
		LoadingAppDAI loadingAppDAO = Database.getDb().getDbIo().makeLoadingAppDAO();
		try
		{
			for(CompAppInfo cai : loadingAppDAO.listComputationApps(false))
			{
				String type = cai.getProperty("appType");
				if (type == null)
					appsInCombo.add(cai);
				else if (type.equalsIgnoreCase("routingscheduler")
					  || type.equalsIgnoreCase("routing-scheduler"))
					// Sort RoutingScheduler apps to the front.
					appsInCombo.add(0, cai);
			}
			schedulerDaemonCombo.addItem(""); // blank selection at top of list.
			for (CompAppInfo cai : appsInCombo)
				schedulerDaemonCombo.addItem(cai.getAppName());
		}
		catch (DbIoException ex)
		{
			parent.showError("Cannot list Routing Scheduler apps: " + ex);
		}
		finally
		{
			loadingAppDAO.close();
		}

		mainPanel.add(schedulerDaemonCombo,
			new GridBagConstraints(1, 1, 1, 1, 1.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
				new Insets(3, 0, 3, 5), 0, 0));
		
		// Routing Spec Name
		routingSpecField.setEditable(false);
		mainPanel.add(new JLabel(dbeditLabels.getString("ScheduleEntryPanel.TableColumn3") + ":"),
			new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(3, 25, 3, 2), 0, 0));
		mainPanel.add(routingSpecField,
			new GridBagConstraints(1, 2, 1, 1, 1.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
				new Insets(3, 0, 3, 5), 0, 0));
		JButton selectRSButton = new JButton(genericLabels.getString("select"));
		selectRSButton.addActionListener(
			new java.awt.event.ActionListener()
			{
	            public void actionPerformed(ActionEvent e) 
	            {
	                selectRoutingSpecPressed();
	            }
	        });
		mainPanel.add(selectRSButton,
			new GridBagConstraints(2, 2, 1, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(3, 0, 3, 20), 0, 0));
		
		// Last Modified
		lastModifiedField.setEditable(false);
		mainPanel.add(new JLabel(dbeditLabels.getString("NetlistEditPanel.LastModified")),
			new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(3, 25, 3, 2), 0, 0));
		mainPanel.add(lastModifiedField,
			new GridBagConstraints(1, 3, 1, 1, 1.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
				new Insets(3, 0, 3, 5), 0, 0));
	
		// Start Time choices
		JPanel scheduleArea = new JPanel(new GridBagLayout());
		mainPanel.add(scheduleArea,
			new GridBagConstraints(1, 5, 3, 1, 1.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
				new Insets(3, 5, 3, 5), 0, 0));
		scheduleArea.setBorder(
			new TitledBorder(dbeditLabels.getString("ScheduleEntryPanel.RunSchedule")));
		ButtonGroup schedRadios = new ButtonGroup();
		schedRadios.add(runOnceRadio);
		schedRadios.add(runContinuouslyRadio);
		schedRadios.add(runPeriodicallyRadio);

		runContinuouslyRadio.setText(
			dbeditLabels.getString("ScheduleEntryPanel.RunContinuously"));
		scheduleArea.add(runContinuouslyRadio,
			new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(5, 5, 5, 15), 0, 0));
		runContinuouslyRadio.addActionListener(
			new java.awt.event.ActionListener()
			{
	            public void actionPerformed(ActionEvent e) 
	            {
	            	runContinuouslyRadioPressed();
	            }
	        });

		runOnceRadio.setText(
			dbeditLabels.getString("ScheduleEntryPanel.RunOnce"));
		scheduleArea.add(runOnceRadio,
			new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(5, 5, 5, 15), 0, 0));
		runOnceRadio.addActionListener(
			new java.awt.event.ActionListener()
			{
	            public void actionPerformed(ActionEvent e) 
	            {
	            	runOnceRadioPressed();
	            }
	        });
		
		runPeriodicallyRadio.setText(
			dbeditLabels.getString("ScheduleEntryPanel.RunPeriodially"));
		JPanel runPeriodicallyPanel = new JPanel(new FlowLayout());
		runPeriodicallyPanel.add(runPeriodicallyRadio);
		runPeriodicallyPanel.add(timeIncField);
		runPeriodicallyPanel.add(timeUnitCombo);
		scheduleArea.add(runPeriodicallyPanel,
			new GridBagConstraints(0, 2, 2, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(0, 0, 0, 15), 0, 0));
		runPeriodicallyRadio.addActionListener(
			new java.awt.event.ActionListener()
			{
	            public void actionPerformed(ActionEvent e) 
	            {
	            	runPeriodicallyRadioPressed();
	            }
	        });
		
		// Start Date/Time & TZ are active for Run Once and Run Periodically
		userTzCal.setTime(new Date());
		userTzCal.set(Calendar.HOUR_OF_DAY, 0);
		userTzCal.set(Calendar.MINUTE, 0);
		userTzCal.set(Calendar.SECOND, 0);
		copyCalFields(userTzCal, utcCal);
		startDateTimeCal = new DateTimeCalendar(
			dbeditLabels.getString("ScheduleEntryPanel.StartingAt"),
			utcCal.getTime(), "dd/MMM/yyyy", "UTC");
		scheduleArea.add(startDateTimeCal,
			new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(5, 15, 5, 2), 0, 0));
		String tzs = TimeZone.getDefault().getID();
		tzSelector.setSelectedItem(tzs);
		scheduleArea.add(tzSelector,
			new GridBagConstraints(1, 3, 1, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(2, 1, 5, 15), 0, 0));
	}
	
	protected void runPeriodicallyRadioPressed()
	{
		startDateTimeCal.setEnabled(true);
		tzSelector.setEnabled(true);
		timeIncField.setEditable(true);
		timeUnitCombo.setEnabled(true);
	}

	protected void runOnceRadioPressed()
	{
		startDateTimeCal.setEnabled(true);
		tzSelector.setEnabled(true);
		timeIncField.setEditable(false);
		timeUnitCombo.setEnabled(false);
	}

	protected void runContinuouslyRadioPressed()
	{
		startDateTimeCal.setEnabled(false);
		tzSelector.setEnabled(false);
		timeIncField.setEditable(false);
		timeUnitCombo.setEnabled(false);
	}

	protected void selectRoutingSpecPressed()
	{
		RoutingSpecSelectDialog dlg = new RoutingSpecSelectDialog(parent);
		parent.launchDialog(dlg);
		if (dlg.isCancelled())
			return;
		
		selectedRS = dlg.getSelection();
		routingSpecField.setText(selectedRS == null ? "" : selectedRS.getName());
	}

	private void fillFields()
	{
		nameField.setText(origObject.getName());
		String appName = origObject.getLoadingAppName();
		if (appName != null && appName.trim().length() > 0)
			schedulerDaemonCombo.setSelectedItem(appName);
		else
			schedulerDaemonCombo.setSelectedIndex(0); // blank selection at top of list.
		
		if (origObject.getRoutingSpecName() == null 
		 || origObject.getRoutingSpecName().trim().length() == 0)
		{
			routingSpecField.setText("");
			selectedRS = null;
		}
		else
		{
			selectedRS = Database.getDb().routingSpecList.find(origObject.getRoutingSpecName());
			routingSpecField.setText(origObject.getRoutingSpecName());
		}
		
		enabledCheck.setSelected(origObject.isEnabled());
		lastModifiedField.setText(
			origObject.getLastModified() == null ? "(never)" : 
			origObject.getLastModified().toString());

		if (origObject.getStartTime() == null)
		{
			runContinuouslyRadio.setSelected(true);
			runContinuouslyRadioPressed();
			runOnceRadio.setSelected(false);
			runPeriodicallyRadio.setSelected(false);
		}
		else // start time != null
		{
			String tzs = origObject.getTimezone();
			if (tzs == null || tzs.trim().length() == 0)
				tzs = TimeZone.getDefault().getID();
			tzSelector.setSelectedItem(tzs);
			
			// The GUI startDateTimeCal will always have UTC.
			// So copy the fields individually into it.
			userTzCal.setTimeZone(TimeZone.getTimeZone(tzs));
			userTzCal.setTime(origObject.getStartTime());
			copyCalFields(userTzCal, utcCal);
			startDateTimeCal.setDate(utcCal.getTime());

			if (origObject.getRunInterval() == null
			 || origObject.getRunInterval().length() == 0)
			{
				runContinuouslyRadio.setSelected(false);
				runOnceRadio.setSelected(true);
				runPeriodicallyRadio.setSelected(false);
				runOnceRadioPressed();
			}
			else
			{
				runContinuouslyRadio.setSelected(false);
				runOnceRadio.setSelected(false);
				runPeriodicallyRadio.setSelected(true);
				IntervalIncrement ii = IntervalIncrement.parse(origObject.getRunInterval());
				timeIncField.setText("" + ii.getCount());
				if (ii.getCalConstant() == Calendar.DAY_OF_MONTH)
					timeUnitCombo.setSelectedIndex(2);
				else if (ii.getCalConstant() == Calendar.HOUR_OF_DAY)
					timeUnitCombo.setSelectedIndex(1);
				else
					timeUnitCombo.setSelectedIndex(0);
				runPeriodicallyRadioPressed();
			}
		}
	}
	
	@Override
	public String getEntityName()
	{
		return dbeditLabels.getString("ScheduleEntryPanel.EntityName");
	}

	@Override
	public void commitEntity()
	{
		saveChanges();
	}

	@Override
	public void closeEntity()
	{
		if (hasChanged())
		{
			int r = JOptionPane.showConfirmDialog(this,
					genericLabels.getString("saveChanges"));
			if (r == JOptionPane.CANCEL_OPTION)
				return;
			else if (r == JOptionPane.YES_OPTION)
			{
				if (!saveChanges())
					return;
			}
			else if (r == JOptionPane.NO_OPTION)

			{
			}
		}
		DbEditorTabbedPane scheduleTabbedPane 
			= parent.getScheduleListTabbedPane();
		scheduleTabbedPane.remove(this);
	}

	@Override
	public void help()
	{
		Help.open();
	}

	@Override
	public boolean hasChanged()
	{
		ScheduleEntry testObj = new ScheduleEntry(origObject.getName());
		testObj.forceSetId(origObject.getId());
		getDataFromFields(testObj);
		return !testObj.equals(origObject);
	}

	@Override
	public boolean saveChanges()
	{
		if (runPeriodicallyRadio.isSelected())
		{
			try
			{
				if (Integer.parseInt(timeIncField.getText().trim()) < 0)
					throw new Exception("negative time increment!");
			}
			catch(Exception ex)
			{
				parent.showError(
					LoadResourceBundle.sprintf(
						dbeditLabels.getString("ScheduleEntryPanel.BadTimeIncr"),
						timeIncField.getText().trim()));
				return false;
			}
		}
		if (selectedRS == null)
		{
			parent.showError(dbeditLabels.getString("ScheduleEntryPanel.NoRsSelected"));
			return false;
		}
		getDataFromFields(null);
		try
		{
			origObject.write();
			lastModifiedField.setText(
				origObject.getLastModified().toString());
		}
		catch (DatabaseException ex)
		{
			parent.showError(
				LoadResourceBundle.sprintf(
					dbeditLabels.getString("ScheduleEntryPanel.WriteError"),
					ex.toString()));
			return false;
		}
		ScheduleListPanel slp = parent.getScheduleListPanel();
		if (slp != null)
		{
			slp.addScheduleEntry(origObject);
			slp.resort();
		}
		return true;
	}
	
	/**
	 * Pull data from the gui controls and store back into the object.
	 * If entry == null, use the origObject that was used to populate the panel.
	 */
	private void getDataFromFields(ScheduleEntry entry)
	{
		if (entry == null)
			entry = origObject;
		
		// Note: Can't change the name field.
		
		// The selected loading app is in the combo.
		int idx = schedulerDaemonCombo.getSelectedIndex();
		if (idx == 0)
		{
			entry.setLoadingAppId(Constants.undefinedId);
			entry.setLoadingAppName(null);
		}
		else
		{
			entry.setLoadingAppName(appsInCombo.get(idx-1).getAppName());
			entry.setLoadingAppId(appsInCombo.get(idx-1).getAppId());
		}
		
		if (selectedRS == null)
		{
			entry.setRoutingSpecId(Constants.undefinedId);
			entry.setRoutingSpecName(null);
		}
		else
		{
			entry.setRoutingSpecId(selectedRS.getId());
			entry.setRoutingSpecName(selectedRS.getName());
		}
		
		entry.setEnabled(enabledCheck.isSelected());

		// Note: Don't set last modified. It will be set by the DAO that writes
		// to the database or the XML file.

		String tzs = (String)tzSelector.getSelectedItem();
		if (tzs.length() == 0)
			tzs = "UTC";
		entry.setTimezone(tzs);

		if (runContinuouslyRadio.isSelected())
		{
			entry.setStartTime(null);
			entry.setRunInterval(null);
		}
		else
		{
			utcCal.setTime(startDateTimeCal.getDate());
			userTzCal.setTimeZone(TimeZone.getTimeZone(tzs));
			copyCalFields(utcCal, userTzCal);
			entry.setStartTime(userTzCal.getTime());
			
			if (runOnceRadio.isSelected())
				entry.setRunInterval(null);
			else
			{
				String tunit = timeUnitCombo.getSelectedIndex() == 0 ? "minute" :
					timeUnitCombo.getSelectedIndex() == 1 ? "hour" : "day";
				entry.setRunInterval(timeIncField.getText() + " " + tunit);
			}
		}
	}

	@Override
	public void forceClose()
	{
		// TODO Auto-generated method stub

	}
	
	private void copyCalFields(Calendar from, Calendar to)
	{
		to.set(Calendar.YEAR, from.get(Calendar.YEAR));
		to.set(Calendar.MONTH, from.get(Calendar.MONTH));
		to.set(Calendar.DAY_OF_MONTH, from.get(Calendar.DAY_OF_MONTH));
		to.set(Calendar.HOUR_OF_DAY, from.get(Calendar.HOUR_OF_DAY));
		to.set(Calendar.MINUTE, from.get(Calendar.MINUTE));
		to.set(Calendar.SECOND, from.get(Calendar.SECOND));
	}

}
