/*
 *  $Id$
 */
package decodes.dbeditor;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.io.StringReader;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Properties;
import java.util.ResourceBundle;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import lrgs.common.DcpAddress;
import lrgs.common.DcpMsgFlag;
import lrgs.common.SearchCriteria;
import lrgs.common.SearchSyntaxException;
import lrgs.gui.SearchCriteriaEditPanel;

import ilex.util.LoadResourceBundle;
import ilex.util.Logger;
import ilex.util.PropertiesUtil;
import ilex.util.TextUtil;

import decodes.consumer.DataConsumer;
import decodes.consumer.DataConsumerException;
import decodes.consumer.OutputFormatter;
import decodes.datasource.DataSourceExec;
import decodes.datasource.HotBackupGroup;
import decodes.datasource.LrgsDataSource;
import decodes.db.Constants;
import decodes.db.DataSource;
import decodes.db.Database;
import decodes.db.DatabaseException;
import decodes.db.DbEnum;
import decodes.db.EnumValue;
import decodes.db.InvalidDatabaseException;
import decodes.db.PresentationGroup;
import decodes.db.RoutingSpec;
import decodes.gui.EnumComboBox;
import decodes.gui.PropertiesEditPanel;
import decodes.util.DecodesSettings;
import decodes.util.PropertiesOwner;
import decodes.util.PropertySpec;

/**
 * This panel edits an open routing spec. Opened from the RoutingSpecListPanel.
 */
@SuppressWarnings("serial")
public class RoutingSpecEditPanel 
	extends DbEditorTab 
	implements ChangeTracker, EntityOpsController, PropertiesOwner
{
	static ResourceBundle genericLabels = DbEditorFrame.getGenericLabels();
	static ResourceBundle dbeditLabels = DbEditorFrame.getDbeditLabels();

	private EntityOpsPanel entityOpsPanel = new EntityOpsPanel(this);
	private PropertiesEditPanel propertiesEditPanel = new PropertiesEditPanel(new Properties());
	private SearchCriteriaEditPanel scEditPanel = new SearchCriteriaEditPanel();
	private JTextField nameField = new JTextField();
	private JLabel consumerArgLabel = new JLabel();
	private JTextField consumerArgsField = new JTextField();
	private EnumComboBox outputFormatCombo = new EnumComboBox(Constants.enum_OutputFormat);
	private EnumComboBox consumerTypeCombo = new EnumComboBox(Constants.enum_DataConsumer);
	private JCheckBox enableEquationsCheck = new JCheckBox();
	private DataSourceCombo dataSourceCombo = new DataSourceCombo();
	private TimeZoneSelector outputTimezoneCombo = new TimeZoneSelector();
	private PresentationGroupCombo presentationGroupCombo = new PresentationGroupCombo();
	private JCheckBox productionCheck = new JCheckBox();
	private JPanel paramPanel = new JPanel(new GridBagLayout());


	private DbEditorFrame parent = null;
	private RoutingSpec theObject, origObject;
	private DataSource selectedDataSource = null;
	private DataConsumer selectedConsumer = null;
	private OutputFormatter selectedFormatter = null;
	private Properties editProps = new Properties();


	private static PropertySpec rsPropSpecs[] = 
	{
		// Properties implemented directly by RoutingSpecThread:
		new PropertySpec("noLimits", PropertySpec.BOOLEAN,
			"Do NOT Apply Sensor min/max limits."),
		new PropertySpec("removeRedundantData", PropertySpec.BOOLEAN,
			"Remove Redundant DCP Message Data."),
		new PropertySpec("compConfig", PropertySpec.FILENAME,
			"Name of in-line computations config file"),
		new PropertySpec("usgsSummaryFile", PropertySpec.FILENAME,
			"Optional USGS-Format Summary File"),
		new PropertySpec("RawArchivePath", PropertySpec.STRING, 
			"Path to raw archive file. Defining this turns on the raw-archive function. " +
			"Example: $DCSTOOL_HOME/raw-archive/fts/$DATE(yyMMdd).fts"),
		new PropertySpec("RawArchiveStartDelim", PropertySpec.STRING, 
			"String placed before each message in the file"),
		new PropertySpec("RawArchiveEndDelim", PropertySpec.STRING, 
			"String placed after each message in the file"),
		new PropertySpec("RawArchiveMaxAge", PropertySpec.STRING, 
			"Example: '1 year'. Files older than this are deleted."),
		new PropertySpec("debugLevel", PropertySpec.INT,
			"(default=0) Set to 1, 2, 3 for increasing levels of debug information" +
			" when this routing spec is run."),
		new PropertySpec("updatePlatformStatus", PropertySpec.BOOLEAN,
			"(default=true) set to false to NOT update platform status records as messages are processed."),
		new PropertySpec("purgeOldEvents", PropertySpec.BOOLEAN,
			"(default=true) Set to false to tell this routing spec to NOT attempt to "
			+ "purge expired events from the database. Also see DecodesSettings.eventPurgeDays")
	};
	
	private PropertySpec combinedProps[] = rsPropSpecs;


	/**
	 * Construct new panel to edit specified object.
	 * @param ob the object to edit in this panel.
	 */
	public RoutingSpecEditPanel(RoutingSpec ob)
	{
		origObject = ob;
		theObject = origObject.copy();
		setTopObject(origObject);

		try
		{
			guiInit();
			fillFields();
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}

	/**
	 * This method only called in dbedit. Associates this panel with enclosing
	 * frame.
	 * 
	 * @param parent
	 *            Enclosing frame
	 */
	void setParent(DbEditorFrame parent)
	{
		Logger.instance().debug3("RoutingSpecEditPanel.setParent("
			+ (parent == null ? "NULL" : "") + ")");
		this.parent = parent;
		scEditPanel.setTopFrame(parent);
	}

	/** Fills the GUI controls with values from the object. */
	private void fillFields()
	{
		nameField.setText(theObject.getName());
		if (theObject.dataSource != null && theObject.dataSource.getName() != null)
			dataSourceCombo.setSelection(theObject.dataSource.getName());

		if (theObject.consumerType != null)
			consumerTypeCombo.setSelection(theObject.consumerType);
		if (theObject.consumerArg != null)
			consumerArgsField.setText(theObject.consumerArg);
		
		productionCheck.setSelected(theObject.isProduction);
		enableEquationsCheck.setSelected(theObject.enableEquations);

		if (theObject.outputFormat != null)
			outputFormatCombo.setSelection(theObject.outputFormat);
		if (theObject.outputTimeZoneAbbr != null)
			outputTimezoneCombo.setTZ(theObject.outputTimeZoneAbbr);
		
		if (theObject.presentationGroupName != null)
			presentationGroupCombo.setSelection(theObject.presentationGroupName);

		// Map sinceTime, untilTime, netlists, and properties into searchcrit object.
		StringBuilder scString = new StringBuilder();
		editProps.clear();
		PropertiesUtil.copyProps(editProps, theObject.getProperties());
		String timeApplyTo = PropertiesUtil.getIgnoreCase(theObject.getProperties(), "rs.timeApplyTo");
		if (timeApplyTo == null)
			timeApplyTo = "l"; // default to local
		if (theObject.sinceTime != null && theObject.sinceTime.trim().length() > 0)
		{
			if (timeApplyTo.equalsIgnoreCase("l") || timeApplyTo.equalsIgnoreCase("b"))
				scString.append("LRGS_SINCE: " + theObject.sinceTime + "\n");
			if (timeApplyTo.equalsIgnoreCase("m") || timeApplyTo.equalsIgnoreCase("b"))
				scString.append("DAPS_SINCE: " + theObject.sinceTime + "\n");
		}
		if (theObject.untilTime != null && theObject.untilTime.trim().length() > 0)
		{
			if (timeApplyTo.equalsIgnoreCase("l") || timeApplyTo.equalsIgnoreCase("b"))
				scString.append("LRGS_UNTIL: " + theObject.untilTime + "\n");
			if (timeApplyTo.equalsIgnoreCase("m") || timeApplyTo.equalsIgnoreCase("b"))
				scString.append("DAPS_UNTIL: " + theObject.untilTime + "\n");
		}
		PropertiesUtil.rmIgnoreCase(editProps, "rs.timeApplyTo");
		for(String nlName : theObject.networkListNames)
			scString.append("NETWORKLIST: " + nlName + "\n");

		for(Object key : theObject.getProperties().keySet())
		{
			String propName = (String)key;
			if (TextUtil.startsWithIgnoreCase(propName, "sc:CHANNEL"))
				scString.append("CHANNEL: " + editProps.getProperty(propName) + "\n");
			else if (propName.equalsIgnoreCase("sc:DAPS_STATUS"))
				scString.append("DAPS_STATUS: " + editProps.getProperty(propName) + "\n");
			else if (TextUtil.startsWithIgnoreCase(propName, "sc:DCP_ADDRESS"))
				scString.append("DCP_ADDRESS: " + editProps.getProperty(propName) + "\n");
			else if (TextUtil.startsWithIgnoreCase(propName, "sc:DCP_NAME"))
				scString.append("DCP_NAME: " + editProps.getProperty(propName) + "\n");
			else if (propName.equalsIgnoreCase("sc:SPACECRAFT"))
				scString.append("SPACECRAFT: " + editProps.getProperty(propName) + "\n");
			else if (TextUtil.startsWithIgnoreCase(propName, "sc:SOURCE"))
				scString.append("SOURCE: " + editProps.getProperty(propName) + "\n");
			else if (propName.equalsIgnoreCase("sc:PARITY_ERROR"))
				scString.append("PARITY_ERROR: " + editProps.getProperty(propName) + "\n");
			else if (propName.equalsIgnoreCase("sc:ASCENDING_TIME"))
				scString.append("ASCENDING_TIME: " + editProps.getProperty(propName) + "\n");
			else if (propName.equalsIgnoreCase("sc:RT_SETTLE_DELAY"))
				scString.append("RT_SETTLE_DELAY: " + editProps.getProperty(propName) + "\n");
			else
				continue; // without removing the propname from editProps!
			PropertiesUtil.rmIgnoreCase(editProps, propName);
		}
		try
		{
			SearchCriteria searchcrit = new SearchCriteria();
			searchcrit.parseFile(new StringReader(scString.toString()));
			scEditPanel.setSearchCrit(searchcrit);
		}
		catch (Exception ex)
		{
			Logger.instance().warning("Cannot parse searchcrit: " + ex);
			Logger.instance().warning("searchcrit image was: \n" + scString.toString());
		}

		// Now the properties edit panel will edit the props with the SC stuff removed.
		dataSourceSelected();
		consumerTypeSelected();
		outputFormatSelected();

		propertiesEditPanel.setProperties(editProps);
		propertiesEditPanel.setPropertiesOwner(this);
	}

	/**
	 * Gets the data from the fields & puts it back into the object.
	 */
	private void getDataFromFields()
	{
		theObject.dataSource = dataSourceCombo.getSelection();
		theObject.consumerType = consumerTypeCombo.getSelection();
		theObject.consumerArg = consumerArgsField.getText().trim();
		if (theObject.consumerArg.length() == 0)
			theObject.consumerArg = null;
		theObject.isProduction = productionCheck.isSelected();
		theObject.enableEquations = enableEquationsCheck.isSelected();
		theObject.outputFormat = outputFormatCombo.getSelection();
		theObject.outputTimeZoneAbbr = outputTimezoneCombo.getTZ();
		theObject.presentationGroupName = 
			presentationGroupCombo.getSelectedIndex() == 0 ? null :
				(String)presentationGroupCombo.getSelectedItem();

		// Get the properties
		propertiesEditPanel.saveChanges(); // saves to editProps
		
		theObject.getProperties().clear();
		PropertiesUtil.copyProps(theObject.getProperties(), editProps);
		SearchCriteria sc = new SearchCriteria();
		scEditPanel.fillSearchCrit(sc);

		String timeApplyTo = "l";
		if ((theObject.sinceTime = sc.getLrgsSince()) != null) // LRGS since time specified
		{
			if (sc.getDapsSince() != null) // Daps Since also specified
				timeApplyTo = "b";
			else // only LRGS since time specified 
				timeApplyTo = "l";
		}
		else if ((theObject.sinceTime = sc.getDapsSince()) != null)
			timeApplyTo = "m";
		
		theObject.untilTime = sc.getLrgsUntil();
		if (theObject.untilTime == null)
			theObject.untilTime = sc.getDapsUntil();
		
		theObject.networkListNames.clear();
		theObject.networkLists.clear();
		theObject.getProperties().setProperty("rs.timeApplyTo", timeApplyTo);
		for(String nln : sc.NetlistFiles)
		{
Logger.instance().debug3("Added netlist name '" + nln + "'");
			theObject.networkListNames.add(nln);
		}
		
		NumberFormat nf = NumberFormat.getIntegerInstance();
		nf.setMinimumIntegerDigits(4);
		nf.setGroupingUsed(false);
		for(int i=0; sc.channels != null && i < sc.channels.length; i++)
			theObject.getProperties().setProperty("sc:CHANNEL_" + nf.format(i), "|"+sc.channels[i]);

		int i=0;
		for(DcpAddress addr : sc.ExplicitDcpAddrs)
			theObject.getProperties().setProperty("sc:DCP_ADDRESS_" + nf.format(i++), addr.toString());
		i=0;
		for(String nm : sc.DcpNames)
			theObject.getProperties().setProperty("sc:DCP_NAME_" + nf.format(i++), nm);

		i = 0;
		for(int idx = 0; idx < sc.sources.length; idx++)
			if (sc.sources[idx] != 0)
				theObject.getProperties().setProperty("sc:SOURCE_" + nf.format(i++),
					DcpMsgFlag.sourceValue2Name(sc.sources[idx]));
		
		if (sc.DapsStatus != SearchCriteria.UNSPECIFIED)
			theObject.getProperties().setProperty("sc:DAPS_STATUS", "" + sc.DapsStatus);
		if (sc.spacecraft != SearchCriteria.SC_ANY)
			theObject.getProperties().setProperty("sc:SPACECRAFT", "" + sc.spacecraft);
		if (sc.parityErrors != SearchCriteria.ACCEPT)
			theObject.getProperties().setProperty("sc:PARITY_ERROR", "" + sc.parityErrors);
		if (sc.getAscendingTimeOnly())
			theObject.getProperties().setProperty("sc:ASCENDING_TIME", "true");
		if (sc.getRealtimeSettlingDelay())
			theObject.getProperties().setProperty("sc:RT_SETTLE_DELAY", "true");
	}

	/** Initializes GUI components */
	private void guiInit() throws Exception
	{
		this.setLayout(new BorderLayout());
		this.add(entityOpsPanel, BorderLayout.SOUTH);
		JPanel rsPanel = new JPanel(new GridBagLayout());
		this.add(rsPanel, BorderLayout.CENTER);
		scEditPanel.setTopFrame(parent);
		scEditPanel.setAllowRealTime(true);
		
		rsPanel.add(paramPanel,
			new GridBagConstraints(0, 0, 1, 1, 0.0, 0.5,
			GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, 
			new Insets(4, 4, 4, 4), 0, 0));
		propertiesEditPanel.setOwnerFrame(parent);
		rsPanel.add(propertiesEditPanel,
			new GridBagConstraints(1, 0, 1, 1, 1.0, 0.5,
			GridBagConstraints.CENTER, GridBagConstraints.BOTH, 
			new Insets(4, 4, 4, 4), 0, 0));
		rsPanel.add(scEditPanel,
			new GridBagConstraints(0, 1, 2, 1, 1.0, 0.5,
			GridBagConstraints.CENTER, GridBagConstraints.BOTH, 
			new Insets(4, 4, 4, 4), 0, 0));

		paramPanel.add(new JLabel(genericLabels.getString("nameLabel")),
			new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, 
				GridBagConstraints.EAST, GridBagConstraints.NONE, 
				new Insets(2, 4, 1, 2), 0, 0));
		nameField.setEditable(false);
		paramPanel.add(nameField, 
			new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, 
				new Insets(2, 0, 1, 10), 0, 0));
	
		paramPanel.add(new JLabel(dbeditLabels.getString("RoutingSpecEditPanel.dataSource")),
			new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, 
				GridBagConstraints.EAST, GridBagConstraints.NONE, 
				new Insets(1, 4, 1, 2), 0, 0));
		paramPanel.add(dataSourceCombo, 
			new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, 
				new Insets(1, 0, 1, 10), 0, 0));
		dataSourceCombo.addActionListener(
			new java.awt.event.ActionListener()
			{
	            public void actionPerformed(ActionEvent e) 
	            {
	                dataSourceSelected();
	            }
	        });

		paramPanel.add(new JLabel(dbeditLabels.getString("RoutingSpecEditPanel.consumerType")),
			new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0, 
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(1, 4, 1, 2), 0, 0));
		paramPanel.add(consumerTypeCombo, 
			new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, 
				new Insets(1, 0, 1, 10), 0, 0));
		consumerTypeCombo.addActionListener(
			new java.awt.event.ActionListener()
			{
	            public void actionPerformed(ActionEvent e) 
	            {
	                consumerTypeSelected();
	            }
	        });

		// The generic label will be overwritten by the specific one when a consumer
		// type is selected.
		consumerArgLabel.setText(dbeditLabels.getString("RoutingSpecEditPanel.consumerArgs"));
		paramPanel.add(consumerArgLabel,
			new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0, 
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(1, 4, 1, 2), 0, 0));
		paramPanel.add(consumerArgsField, 
			new GridBagConstraints(1, 3, 1, 1, 1.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, 
				new Insets(1, 0, 1, 10), 0, 0));
	
		paramPanel.add(new JLabel(dbeditLabels.getString("RoutingSpecEditPanel.outFormat")),
			new GridBagConstraints(0, 4, 1, 1, 0.0, 0.0, 
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(1, 4, 1, 2), 0, 0));
		outputFormatCombo.addActionListener(
			new java.awt.event.ActionListener()
			{
	            public void actionPerformed(ActionEvent e) 
	            {
	                outputFormatSelected();
	            }
	        });
		paramPanel.add(outputFormatCombo, 
			new GridBagConstraints(1, 4, 1, 1, 1.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE, 
				new Insets(1, 0, 1, 10), 0, 0));
		
		paramPanel.add(new JLabel(genericLabels.getString("timeZoneLabel")),
			new GridBagConstraints(0, 5, 1, 1, 0.0, 0.0, 
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(1, 4, 1, 2), 0, 0));
		paramPanel.add(outputTimezoneCombo, 
			new GridBagConstraints(1, 5, 1, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE, 
				new Insets(1, 0, 1, 10), -30, 0));
	
		paramPanel.add(new JLabel(dbeditLabels.getString("RoutingSpecEditPanel.presGroup")),
			new GridBagConstraints(0, 6, 1, 1, 0.0, 0.0, 
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(2, 4, 2, 2), 0, 0));
		paramPanel.add(presentationGroupCombo, 
			new GridBagConstraints(1, 6, 1, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE, 
				new Insets(1, 0, 1, 10), 0, 0));

		JPanel checkPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 1));
		enableEquationsCheck.setText(
			dbeditLabels.getString("RoutingSpecEditPanel.enableEquations"));
		checkPanel.add(enableEquationsCheck);
		productionCheck.setText(genericLabels.getString("isProduction"));
		checkPanel.add(productionCheck);
		paramPanel.add(checkPanel, 
			new GridBagConstraints(0, 7, 4, 1, 0.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.NONE, 
				new Insets(1, 10, 1, 10), 0, 0));
	}


	private void dataSourceSelected()
	{
		selectedDataSource = dataSourceCombo.getSelection();
		rebuildCombinedProps();
		SwingUtilities.invokeLater(
		new Runnable()
		{
			public void run()
			{
				paramPanel.revalidate();
				paramPanel.repaint();
			}
		});
	}

	protected void outputFormatSelected()
	{
		String formatName = outputFormatCombo.getSelection();
		if (formatName != null && formatName.length() > 0)
		{
			try
			{
				selectedFormatter = 
					OutputFormatter.makeOutputFormatter(formatName, null, null, null, null);
				outputTimezoneCombo.setEnabled(selectedFormatter.usesTZ());
				// Update this PropertiesOwner
				rebuildCombinedProps();
			}
			catch (Exception ex)
			{
				Logger.instance().warning("Cannot instantiate formatter '" 
					+ formatName + "': " + ex);
			}
		}
		SwingUtilities.invokeLater(
		new Runnable()
		{
			public void run()
			{
				paramPanel.revalidate();
				paramPanel.repaint();
			}
		});

	}

	private void rebuildCombinedProps()
	{
		ArrayList<PropertySpec> propSpecs = new ArrayList<PropertySpec>();
		for(PropertySpec ps : rsPropSpecs)
			propSpecs.add(ps);
		
		if (selectedDataSource != null)
		{
			try
			{
				DataSourceExec currentDataSource = selectedDataSource.makeDelegate();
				for(PropertySpec ps : currentDataSource.getSupportedProps())
					propSpecs.add(ps);
				adjustSearchCritFor(currentDataSource);
			}
			catch (InvalidDatabaseException ex)
			{
				Logger.instance().warning("Cannot instantiate data source of type '"
					+ dataSourceCombo.getSelectedItem() + "': " + ex);
			}
		}
		
		if (selectedFormatter != null)
		{
			for(PropertySpec ps : selectedFormatter.getSupportedProps())
				propSpecs.add(ps);
		}

		if (selectedConsumer != null)
		{
			for(PropertySpec ps : selectedConsumer.getSupportedProps())
				propSpecs.add(ps);
		}
		
		combinedProps = new PropertySpec[propSpecs.size()];
		propSpecs.toArray(combinedProps);
		propertiesEditPanel.setPropertiesOwner(this);
	}

	private void adjustSearchCritFor(DataSourceExec currentDataSource)
	{
		boolean isLrgsType = currentDataSource != null
			&& (currentDataSource instanceof LrgsDataSource
				|| currentDataSource instanceof HotBackupGroup);
		scEditPanel.setIsLrgs(isLrgsType);
		if (!isLrgsType && currentDataSource.supportsTimeRanges())
			scEditPanel.enableSinceUntil();
	}

	@SuppressWarnings("rawtypes")
	protected void consumerTypeSelected()
	{
		String consumerName = consumerTypeCombo.getSelection();

		try
		{
			selectedConsumer = DataConsumer.makeDataConsumer(consumerName);
		}
		catch (Exception ex)
		{
			Logger.instance().warning("Cannot instantiate consumer '"
				+ consumerTypeCombo.getSelectedItem() + "': " + ex);
			selectedConsumer = null;
		}
		if (selectedConsumer != null)
			consumerArgLabel.setText(selectedConsumer.getArgLabel());
		
		// Update this PropertiesOwner
		rebuildCombinedProps();

		//TODO update this PropertiesOwner
		SwingUtilities.invokeLater(
		new Runnable()
		{
			public void run()
			{
				paramPanel.revalidate();
				paramPanel.repaint();
			}
		});
	}

	/**
	 * From ChangeTracker interface.
	 * 
	 * @return true if changes have been made to this screen since the last time
	 *         it was saved.
	 */
	public boolean hasChanged()
	{
		getDataFromFields();
		return !theObject.equals(origObject);
	}

	/**
	 * From ChangeTracker interface, save the changes back to the database &
	 * reset the hasChanged flag.
	 * 
	 * @return true if object was successfully saved.
	 */
	public boolean saveChanges()
	{
		getDataFromFields();
		try
		{
			theObject.lastModifyTime = new Date();
			theObject.write();
		}
		catch (DatabaseException e)
		{
			DbEditorFrame.instance().showError(
				LoadResourceBundle.sprintf(genericLabels.getString("cannotSave"), getEntityName(),
					e.toString()));
			return false;
		}

		Database.getDb().routingSpecList.remove(origObject);
		Database.getDb().routingSpecList.add(theObject);
		parent.getRoutingSpecListPanel().resort();

		// Make a new copy in case user wants to keep editing.
		origObject = theObject;
		theObject = origObject.copy();
		setTopObject(origObject);

		return true;
	}

	/** @see EntityOpsController */
	public String getEntityName()
	{
		return "RoutingSpec";
	}

	/** @see EntityOpsController */
	public void commitEntity()
	{
		saveChanges();
	}

	/** @see EntityOpsController */
	public void closeEntity()
	{
		if (hasChanged())
		{
			int r = JOptionPane.showConfirmDialog(this, genericLabels.getString("saveChanges"));
			if (r == JOptionPane.CANCEL_OPTION)
				return;
			else if (r == JOptionPane.YES_OPTION)
			{
				if (!saveChanges())
					return;
			}
			else if (r == JOptionPane.NO_OPTION)
				;
		}
		DbEditorTabbedPane tp = parent.getRoutingSpecTabbedPane();
		tp.remove(this);
	}

	/**
	 * Called from File - CloseAll to close this tab, abandoning any changes.
	 */
	public void forceClose()
	{
		DbEditorTabbedPane tp = parent.getRoutingSpecTabbedPane();
		tp.remove(this);
	}

	/** @see EntityOpsController */
	public void help()
	{
	}

	@Override
	public PropertySpec[] getSupportedProps()
	{
		return combinedProps;
	}

	@Override
	public boolean additionalPropsAllowed()
	{
		// Don't assume all output formats, sources, & consumers are up to date.
		// So DO allow additional unknown props.
		return true;
	}
}

