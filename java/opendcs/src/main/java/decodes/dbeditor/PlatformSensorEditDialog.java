/*
* $Id$
*/
package decodes.dbeditor;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import java.util.Properties;
import java.util.Vector;
import java.util.ResourceBundle;

import ilex.util.LoadResourceBundle;
import ilex.util.PropertiesUtil;

import decodes.db.Constants;
import decodes.db.ConfigSensor;
import decodes.db.DataType;
import decodes.db.PlatformSensor;
import decodes.db.Site;
import decodes.db.SiteName;
import decodes.gui.GuiDialog;
import decodes.gui.PropertiesEditPanel;
import decodes.gui.properties.PropertiesEditPanelController;
import decodes.util.DecodesSettings;

/**
This class is the dialog for editing platform sensor info. It is called
from the "Edit Sensor Info" button on the platform edit panel.
*/
public class PlatformSensorEditDialog extends GuiDialog
{
	static ResourceBundle genericLabels = DbEditorFrame.getGenericLabels();
	static ResourceBundle dbeditLabels = DbEditorFrame.getDbeditLabels();

	JPanel panel1 = new JPanel();
	BorderLayout borderLayout1 = new BorderLayout();
	JPanel sensorIdPanel = new JPanel();
	JLabel sensorNumLabel = new JLabel();
	JTextField sensorNumField = new JTextField();
	JLabel sensorNameLabel = new JLabel();
	JTextField sensorNameField = new JTextField();
	JLabel paramCodeLabel = new JLabel();
	JTextField paramCodeField = new JTextField();
	GridBagLayout gridBagLayout1 = new GridBagLayout();
	JPanel buttonPanel = new JPanel();
	FlowLayout flowLayout1 = new FlowLayout();
	JButton okButton = new JButton();
	JButton cancelButton = new JButton();
	JPanel centerPanel = new JPanel();
	JPanel sensorParamPanel = new JPanel();
	JLabel actualSiteLabel = new JLabel();
	JTextField actualSiteField = new JTextField();
	JButton selectSiteButton = new JButton();
    JLabel minMaxExplanationLabel = new JLabel();
	JLabel minLabel = new JLabel();
	JTextField platformMinField = new JTextField();
	JLabel configMinLabel = new JLabel();
	JTextField configMinField = new JTextField();
	JLabel maxLabel = new JLabel();
	JTextField platformMaxField = new JTextField();
	JLabel configMaxLabel = new JLabel();
	JTextField configMaxField = new JTextField();
	JLabel usgsDdnoLabel = new JLabel();
	JComboBox usgsDdnoCombo = new JComboBox();
	GridBagLayout gridBagLayout2 = new GridBagLayout();
	PropertiesEditPanel propsPanel = PropertiesEditPanel.from(new Properties());
	GridBagLayout gridBagLayout3 = new GridBagLayout();
	Border border5 = BorderFactory.createEtchedBorder(Color.white,
		new Color(165, 163, 151));
	Border border6 = new TitledBorder(border5,
		dbeditLabels.getString("PlatformSensorEditDialog.additionalProps"));
	TitledBorder titledBorder3 = new TitledBorder("");
	TitledBorder titledBorder4 = new TitledBorder("");
	JButton clearButton = new JButton();

	//===============================================
	private Site selectedSite;
	private PlatformSensor platformSensor;
	private Properties theProperties;

	/**
	 * Constructor.
	 */
	public PlatformSensorEditDialog(Frame owner, String title, boolean modal)
	{
		super(owner, title, modal);
		try
		{
			setDefaultCloseOperation(DISPOSE_ON_CLOSE);
			jbInit();
			pack();
		}
		catch (Exception exception)
		{
			exception.printStackTrace();
		}
		selectedSite = null;
		theProperties = null;
	}

	public PlatformSensorEditDialog()
	{
		this(new Frame(), 
			dbeditLabels.getString("PlatformSensorEditDialog.title"), true);
	}

	private void jbInit() throws Exception
	{
		panel1.setLayout(borderLayout1);
		sensorIdPanel.setLayout(gridBagLayout1);
		sensorNumLabel.setText(
			dbeditLabels.getString("PlatformSensorEditDialog.sensorNumLabel"));
		sensorNumField.setToolTipText(
			dbeditLabels.getString("PlatformSensorEditDialog.sensorNumTT"));
		sensorNumField.setEditable(false);
		sensorNumField.setEnabled(false);
		sensorNumField.setText("1");
		sensorNameLabel.setText(
			dbeditLabels.getString("PlatformSensorEditDialog.sensorNameLabel"));
		sensorNameField.setToolTipText(
			dbeditLabels.getString("PlatformSensorEditDialog.sensorNumTT"));
		sensorNameField.setEditable(false);
		sensorNameField.setEnabled(false);
		sensorNameField.setText("stage");
		paramCodeLabel.setText(
			dbeditLabels.getString("PlatformSensorEditDialog.paramCodeLabel"));
		paramCodeField.setToolTipText(
			dbeditLabels.getString("PlatformSensorEditDialog.paramCodeTT"));
		paramCodeField.setEditable(false);
		paramCodeField.setEnabled(false);
		paramCodeField.setText("00065");
		buttonPanel.setLayout(flowLayout1);
		okButton.setPreferredSize(new Dimension(100, 27));
		okButton.setText(genericLabels.getString("OK"));
		okButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				okButton_actionPerformed(e);
			}
		});
		cancelButton.setPreferredSize(new Dimension(100, 27));
		cancelButton.setText(
			genericLabels.getString("cancel"));
		cancelButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				cancelButton_actionPerformed(e);
			}
		});
		flowLayout1.setHgap(20);
		centerPanel.setLayout(gridBagLayout3);
		sensorParamPanel.setLayout(gridBagLayout2);
		actualSiteLabel.setText(
			dbeditLabels.getString("PlatformSensorEditDialog.actualSiteLabel"));
		actualSiteField.setToolTipText(
			dbeditLabels.getString("PlatformSensorEditDialog.actualSiteTT"));
		actualSiteField.setEditable(false);
		actualSiteField.setText("017765235");
		selectSiteButton.setText(
			genericLabels.getString("select"));
		selectSiteButton.setToolTipText(
			dbeditLabels.getString("PlatformSensorEditDialog.siteSelectTT"));
		selectSiteButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				selectSiteButton_actionPerformed();
			}
		});
		minMaxExplanationLabel.setHorizontalAlignment(SwingConstants.CENTER);
		minMaxExplanationLabel.setText(
			dbeditLabels.getString("PlatformSensorEditDialog.minMaxExplain"));
		minLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		minLabel.setText(
			dbeditLabels.getString("PlatformSensorEditDialog.minLabel"));
		platformMinField.setPreferredSize(new Dimension(80, 23));
		platformMinField.setToolTipText(
			dbeditLabels.getString("PlatformSensorEditDialog.minTT"));
		platformMinField.setText("");
		configMinLabel.setText(
			dbeditLabels.getString("PlatformSensorEditDialog.cfgMinLabel"));
		configMinField.setPreferredSize(new Dimension(80, 23));
		configMinField.setToolTipText(
			dbeditLabels.getString("PlatformSensorEditDialog.cfgMinTT"));
		configMinField.setEditable(false);
		configMinField.setEnabled(false);
		configMinField.setText("");
		maxLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		maxLabel.setText(
			dbeditLabels.getString("PlatformSensorEditDialog.maxLabel"));
		platformMaxField.setPreferredSize(new Dimension(80, 23));
		platformMaxField.setToolTipText(
			dbeditLabels.getString("PlatformSensorEditDialog.maxTT"));
		platformMaxField.setText("");
		configMaxLabel.setText(
			dbeditLabels.getString("PlatformSensorEditDialog.cfgMaxLabel"));
		configMaxField.setPreferredSize(new Dimension(80, 23));
		configMaxField.setToolTipText(
			dbeditLabels.getString("PlatformSensorEditDialog.cfgMaxTT"));
		configMaxField.setEditable(false);
		configMaxField.setEnabled(false);
		configMaxField.setText("");
		usgsDdnoLabel.setText("USGS DDNO:");
		usgsDdnoCombo.setToolTipText(
			dbeditLabels.getString("PlatformSensorEditDialog.usgsDdnoTT"));
		usgsDdnoCombo.setEditable(true);
		propsPanel.setBorder(border6);
		this.setTitle(
			dbeditLabels.getString("PlatformSensorEditDialog.title"));
		sensorIdPanel.setBorder(titledBorder3);
		sensorParamPanel.setBorder(titledBorder4);
		clearButton.setText(
			genericLabels.getString("clear"));
		clearButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				clearButton_actionPerformed(e);
			}
		});
		clearButton.setToolTipText(
			dbeditLabels.getString("PlatformSensorEditDialog.clearTT"));

		getContentPane().add(panel1);
		panel1.add(sensorIdPanel, java.awt.BorderLayout.NORTH);
		sensorIdPanel.add(paramCodeField,
						  new GridBagConstraints(5, 0, 1, 1, 1.0, 0.0
												 , GridBagConstraints.EAST,
												 GridBagConstraints.HORIZONTAL,
												 new Insets(5, 2, 5, 5), 29, 0));
		panel1.add(buttonPanel, java.awt.BorderLayout.SOUTH);
		buttonPanel.add(okButton);
		buttonPanel.add(cancelButton);
		panel1.add(centerPanel, java.awt.BorderLayout.CENTER);
		sensorIdPanel.add(paramCodeLabel,
						  new GridBagConstraints(4, 0, 1, 1, 1.0, 0.0
												 , GridBagConstraints.EAST,
												 GridBagConstraints.NONE,
												 new Insets(5, 20, 5, 0), 0, 0));
		sensorIdPanel.add(sensorNameField,
						  new GridBagConstraints(3, 0, 1, 1, 1.0, 0.0
												 , GridBagConstraints.WEST,
												 GridBagConstraints.HORIZONTAL,
												 new Insets(5, 2, 5, 10), 70, 0));
		sensorIdPanel.add(sensorNameLabel,
						  new GridBagConstraints(2, 0, 1, 1, 1.0, 0.0
												 , GridBagConstraints.EAST,
												 GridBagConstraints.NONE,
												 new Insets(5, 20, 5, 0), 0, 0));
		sensorIdPanel.add(sensorNumField,
						  new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0
												 , GridBagConstraints.WEST,
												 GridBagConstraints.HORIZONTAL,
												 new Insets(5, 2, 5, 10), 40, 0));
		sensorIdPanel.add(sensorNumLabel,
						  new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0
												 , GridBagConstraints.EAST,
												 GridBagConstraints.NONE,
												 new Insets(5, 5, 5, 0), 0, 0));
		centerPanel.add(sensorParamPanel,
						new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
											   , GridBagConstraints.CENTER,
											   GridBagConstraints.BOTH,
											   new Insets(0, 0, 0, 0), 0, 0));
		centerPanel.add(propsPanel, new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0
			, GridBagConstraints.CENTER, GridBagConstraints.BOTH,
			new Insets(0, 0, 0, 0), 0, 0));
	sensorParamPanel.add(actualSiteLabel,
			     new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0
	    , GridBagConstraints.EAST, GridBagConstraints.NONE,
	    new Insets(10, 20, 10, 2), 0, 0));
	sensorParamPanel.add(minLabel,
			     new GridBagConstraints(0, 3, 2, 1, 1.0, 0.0
	    , GridBagConstraints.EAST, GridBagConstraints.NONE,
	    new Insets(5, 10, 5, 2), 0, 0));
	sensorParamPanel.add(platformMinField,
			     new GridBagConstraints(2, 3, 1, 1, 1.0, 0.0
	    , GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
	    new Insets(5, 0, 5, 0), 0, 0));
	sensorParamPanel.add(configMinLabel,
			     new GridBagConstraints(3, 3, 1, 1, 1.0, 0.0
	    , GridBagConstraints.EAST, GridBagConstraints.NONE,
	    new Insets(5, 15, 5, 2), 0, 0));
	sensorParamPanel.add(configMinField,
			     new GridBagConstraints(4, 3, 2, 1, 1.0, 0.0
	    , GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
	    new Insets(5, 0, 5, 5), 0, 0));
	sensorParamPanel.add(maxLabel,
			     new GridBagConstraints(0, 4, 2, 1, 1.0, 0.0
	    , GridBagConstraints.EAST, GridBagConstraints.NONE,
	    new Insets(5, 10, 5, 2), 0, 0));
	sensorParamPanel.add(platformMaxField,
			     new GridBagConstraints(2, 4, 1, 1, 1.0, 0.0
	    , GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
	    new Insets(5, 0, 5, 0), 0, 0));
	sensorParamPanel.add(configMaxLabel,
			     new GridBagConstraints(3, 4, 1, 1, 1.0, 0.0
	    , GridBagConstraints.EAST, GridBagConstraints.NONE,
	    new Insets(5, 15, 5, 2), 0, 0));
	sensorParamPanel.add(configMaxField,
			     new GridBagConstraints(4, 4, 2, 1, 1.0, 0.0
	    , GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
	    new Insets(5, 0, 5, 5), 0, 0));
	sensorParamPanel.add(usgsDdnoLabel,
			     new GridBagConstraints(0, 5, 2, 1, 1.0, 0.0
	    , GridBagConstraints.EAST, GridBagConstraints.NONE,
	    new Insets(20, 2, 10, 15), 0, 0));
	sensorParamPanel.add(usgsDdnoCombo,
			     new GridBagConstraints(2, 5, 1, 1, 1.0, 0.0
	    , GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
	    new Insets(20, 0, 10, 0), 0, 0));
	sensorParamPanel.add(minMaxExplanationLabel,
			     new GridBagConstraints(0, 2, 5, 1, 1.0, 0.0
	    , GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
	    new Insets(20, 10, 3, 10), 0, 0));
	sensorParamPanel.add(selectSiteButton,
			     new GridBagConstraints(3, 0, 1, 1, 1.0, 0.0
	    , GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
	    new Insets(10, 5, 10, 5), 0, 0));
	sensorParamPanel.add(clearButton,
			     new GridBagConstraints(4, 0, 1, 1, 1.0, 0.0
	    , GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
	    new Insets(10, 5, 10, 5), 0, 0));
	sensorParamPanel.add(actualSiteField,
			     new GridBagConstraints(1, 0, 2, 1, 1.0, 0.0
	    , GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
	    new Insets(10, 0, 10, 5), 0, 0));
    }

	/**
	 * Fills the dialog with info from the platform sensor.
	 * @param ps the platform sensor object.
	 * @param cs the config sensor object.
	 */
	public void fillFields(PlatformSensor ps, ConfigSensor cs)
	{
		platformSensor = ps;
		theProperties = new Properties();
		PropertiesUtil.copyProps(theProperties, ps.getProperties());

		sensorNumField.setText("" + ps.sensorNumber);
		sensorNameField.setText(cs.sensorName);

		DataType dt = cs.getDataType(
			DecodesSettings.instance().dataTypeStdPreference);
		if (dt == null)
			dt = cs.getDataType();
		paramCodeField.setText(dt == null ? "unknown" : dt.getCode());

		selectedSite = ps.site;
		SiteName sn = selectedSite == null ? null : selectedSite.getPreferredName();
		actualSiteField.setText(
			sn == null ? dbeditLabels.getString("PlatformSensorEditDialog.inherited") :
			sn.getNameType() + ":" + sn.getNameValue());

//		actualSiteField.setText(ps.site == null ? 
//			dbeditLabels.getString("PlatformSensorEditDialog.inherited") :
//			ps.site.getPreferredName().makeFileName());

		String x = PropertiesUtil.rmIgnoreCase(theProperties, "min");
		platformMinField.setText(x == null ? "" : x);

		x = PropertiesUtil.rmIgnoreCase(theProperties, "max");
		platformMaxField.setText(x == null ? "" : x);

		configMinField.setText(
			cs.absoluteMin == Constants.undefinedDouble ? "" 
			: ("" + cs.absoluteMin));

		configMaxField.setText(
			cs.absoluteMax == Constants.undefinedDouble ? "" 
			: ("" + cs.absoluteMax));

		String validDdnos[] = ps.getValidDdnos();
		int currentDdno = ps.getUsgsDdno();
		Vector ddnos = new Vector();
		int curpos = -1;
		for (int i=0; validDdnos != null && i < validDdnos.length; i++)
		{
			ddnos.add("" + validDdnos[i]);
			int validDD = Integer.parseInt(validDdnos[i].substring(0,
				validDdnos[i].indexOf('-')).trim());
			if ( currentDdno == validDD )
				curpos = i;
		}
		if (curpos == -1 && (currentDdno == -1 || currentDdno > 0))
		{
			ddnos.add("" + currentDdno);
			curpos = ddnos.size() - 1;
		}

		for(int i=0; i<ddnos.size(); i++)
			usgsDdnoCombo.addItem(ddnos.get(i));

		if (curpos != -1)
			usgsDdnoCombo.setSelectedIndex(curpos);

		propsPanel.getModel().setProperties(theProperties);
		propsPanel.getModel().setPropertiesOwner(ps);
	}

	private boolean saveChanges()
	{
		// Save properties changes and copy them back to the sensor object.
		propsPanel.getModel().saveChanges();
		platformSensor.getProperties().clear();
		PropertiesUtil.copyProps(platformSensor.getProperties(), theProperties);

		platformSensor.site = selectedSite;

		String x = platformMinField.getText().trim();
		if (x.length() > 0)
		{
			try { double d = Double.parseDouble(x); }
			catch(NumberFormatException ex)
			{
				showError(
					dbeditLabels.getString("PlatformSensorEditDialog.badMinError"));
				return false;
			}
			platformSensor.getProperties().setProperty("min", x);
		}

		x = platformMaxField.getText().trim();
		if (x.length() > 0)
		{
			try { double d = Double.parseDouble(x); }
			catch(NumberFormatException ex)
			{
				showError(
					dbeditLabels.getString("PlatformSensorEditDialog.badMaxError"));
				return false;
			}
			platformSensor.getProperties().setProperty("max", x);
		}

		x = (String)usgsDdnoCombo.getSelectedItem();
		if (x == null || x.trim().length() == 0)
			platformSensor.setUsgsDdno(0);
		else
		{
			String ddno;
			if ( x.trim().equals("-1") )
				ddno = x;
			else if ( x.indexOf('-') != -1 )
				ddno = x.substring(0,x.indexOf('-')).trim();
			else
				ddno = x;
			try 
			{
				int i = Integer.parseInt(ddno);
				platformSensor.setUsgsDdno(i);
			}
			catch(NumberFormatException ex)
			{
				showError(
					dbeditLabels.getString("PlatformSensorEditDialog.badDdnoError"));
				return false;
			}
		}
		return true;
	}

	private void okButton_actionPerformed(ActionEvent e)
	{
		if (saveChanges())
			closeDlg();
	}

	private void cancelButton_actionPerformed(ActionEvent e)
	{
		closeDlg();
	}

	
	/** Closes the dialog. */
	void closeDlg()
	{
		setVisible(false);
		dispose();
	}


//	public static void main(String args[])
//	{
//		PlatformSensorEditDialog dlg = new PlatformSensorEditDialog();
//		dlg.setVisible(true);
//	}
//

	/**
	 * Called when the select-site button is pressed.
	 */
	public void selectSiteButton_actionPerformed()
	{
		SiteSelectDialog dlg = new SiteSelectDialog(this);
		launchDialog(dlg);
		Site site = dlg.getSelectedSite();
		if (site != null) // selection was made?
		{
			selectedSite = site;
			SiteName sn = selectedSite.getPreferredName();
			actualSiteField.setText(
				sn == null ? dbeditLabels.getString("PlatformSensorEditDialog.inherited") :
				sn.getNameType() + ":" + sn.getNameValue());
//				selectedSite.getPreferredName().makeFileName());
		}
	}

	public void clearButton_actionPerformed(ActionEvent e)
	{
		actualSiteField.setText(
			dbeditLabels.getString("PlatformSensorEditDialog.inherited"));
		selectedSite = null;
	}
}
