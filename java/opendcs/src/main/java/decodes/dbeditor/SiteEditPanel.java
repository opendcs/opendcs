/*
*  $Id$
*/
package decodes.dbeditor;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import decodes.gui.*;
import java.awt.event.*;
import java.util.Iterator;
import java.util.ResourceBundle;

import ilex.gui.Help;
import ilex.util.LoadResourceBundle;
import ilex.util.TextUtil;
import ilex.util.Logger;

import decodes.db.*;
import decodes.gui.UnitsComboBox;
import decodes.util.DecodesSettings;
import decodes.cwms.CwmsSqlDatabaseIO;

/**
Panel for editing a Site object.
Opened from the SiteListPanel.
*/
public class SiteEditPanel extends DbEditorTab
	implements ChangeTracker, EntityOpsController
{
	static ResourceBundle genericLabels = DbEditorFrame.getGenericLabels();
	static ResourceBundle dbeditLabels = DbEditorFrame.getDbeditLabels();

    private JScrollPane jScrollPane1 = new JScrollPane();
	private SiteNamesTableModel model = new SiteNamesTableModel();
    private JTable siteNameTable = new JTable(model);
    private JPanel locInfoPanel = new JPanel();
    private TitledBorder locInfoBorder;
    private JLabel latitudeLabel = new JLabel();
    private JTextField latitudeField = new JTextField();
    private JLabel longitudeLabel = new JLabel();
    private JTextField longitudeField = new JTextField();
    private JLabel cityLabel = new JLabel();
    private JTextField cityField = new JTextField();
    private JLabel timeZoneLabel = new JLabel();
    private TimeZoneSelector timeZoneSelector = new TimeZoneSelector();
    private JLabel stateLabel = new JLabel();
    private JTextField stateField = new JTextField();
    private JLabel countryLabel = new JLabel();
    private JTextField countryField = new JTextField();
    private JLabel regionLabel = new JLabel();
    private JTextField regionField = new JTextField();
    private JLabel publicNameLabel = new JLabel();
    private JTextField publicNameField = new JTextField();
    private JPanel overallPanel = new JPanel();
    private BorderLayout borderLayout1 = new BorderLayout();
    private EntityOpsPanel entityOpsPanel;
    private JPanel namesPanel = new JPanel();
    private TitledBorder titledBorder3;
    private BorderLayout borderLayout2 = new BorderLayout();
    private JPanel nameOpsPanel = new JPanel();
    private JButton addNameButton = new JButton();
    private JButton editNameButton = new JButton();
    private JButton deleteNameButton = new JButton();
    private FlowLayout nameOpsFlowLayout = new FlowLayout();
    private JTextField elevationField = new JTextField();
    private JLabel elevLabel = new JLabel();
    private UnitsComboBox elevUnitsCombo = new UnitsComboBox("length", "ft");
    private JLabel elevUnitsLabel = new JLabel();
    private GridBagLayout gridBagLayout1 = new GridBagLayout();
    private JPanel descPanel = new JPanel();
    private TitledBorder descriptionBorder;
    private JTextArea descriptionArea = new JTextArea();
    private BorderLayout borderLayout4 = new BorderLayout();
    private GridBagLayout gridBagLayout2 = new GridBagLayout();
	private PropertiesEditPanel propsPanel;

	/** The site being edited */
	Site theSite;

	/** The parent frame */
	DbEditorFrame parent;

	private boolean namesChanged = false;

	private boolean noEditSites = false;
	private boolean isCwms = false;

	/** No-args constructor for JBuilder. */
    public SiteEditPanel()
	{
		super();
	    entityOpsPanel = new EntityOpsPanel(this);
		propsPanel = PropertiesEditPanel.from(null);
        try {
            jbInit();
			TableColumnAdjuster.adjustColumnWidths(siteNameTable,
				new int[] { 20, 80 });
//			TableColumn tc = siteNameTable.getColumnModel().getColumn(0);
//			tc.setCellEditor(new SiteNameTypeEditor());
		    siteNameTable.getTableHeader().setReorderingAllowed(false);
		    
		    siteNameTable.addMouseListener(
    			new MouseAdapter()
    			{
    				public void mouseClicked(MouseEvent e)
    				{
    					if (e.getClickCount() == 2)
    					{
    						editNameButtonPressed();
    					}
    				}
    			});

        }
        catch(Exception ex) {
            ex.printStackTrace();
        }
    }

	/**
	  Construct new panel to edit specified object.
	  @param site the object to edit in this panel.
	*/
	public SiteEditPanel(Site site)
	{
		this();
		setSite(site);
	}

	/**
	  Sets the object being edited in this panel.
	  @param site the object to edit in this panel.
	*/
	public void setSite(Site site)
	{
		theSite = site;
		setTopObject(site);
		if (site == null)
			disableFields();
		else
		{
			DatabaseIO dbio = site.getDatabase().getDbIo();
			isCwms = dbio instanceof CwmsSqlDatabaseIO;
			enableFields();
			propsPanel.getModel().setProperties(theSite.getProperties());
			fillFields(theSite);
		}
	}

	/** 
	  This method only called in dbedit.
	  Associates this panel with enclosing frame.
	  @param parent   Enclosing frame
	*/
	void setParent(DbEditorFrame parent)
	{
		this.parent = parent;
        this.add(entityOpsPanel, BorderLayout.SOUTH);
	}

	/** Fills the GUI controls with values from the object. */
	public void fillFields(Site st)
	{
		model.fillInValues(st);
		latitudeField.setText(st.latitude);
		longitudeField.setText(st.longitude);
		cityField.setText(st.nearestCity);
		timeZoneSelector.setTZ(st.timeZoneAbbr);
		stateField.setText(st.state);
		countryField.setText(st.country);
		regionField.setText(st.region);
		String s = st.getDescription();
		descriptionArea.setText(s == null ? "" : s);
		double d = st.getElevation();
		elevationField.setText(d == Constants.undefinedDouble ? ""
			: ("" + st.getElevation()));
		String eu = st.getElevationUnits();
		if (eu != null && eu.length() > 0)
    		elevUnitsCombo.setSelectedAbbr(eu);
		String dbno = st.getUsgsDbno();
		String pn = st.getPublicName();
		if (pn == null)
			pn = "";
		publicNameField.setText(pn);
	}

	/** Initializes GUI components */
    private void jbInit() throws Exception {
        locInfoBorder = new TitledBorder(
			BorderFactory.createLineBorder(
				new Color(153, 153, 153),2),
				dbeditLabels.getString("SiteEditPanel.localInfo"));
        titledBorder3 = new TitledBorder(
			BorderFactory.createLineBorder(
				new Color(153, 153, 153),2),
				dbeditLabels.getString("SiteEditPanel.namesForSite"));
        descriptionBorder = new TitledBorder(
			BorderFactory.createLineBorder(
				new Color(153, 153, 153),2),
				dbeditLabels.getString("SiteEditPanel.descEtc"));
        this.setLayout(borderLayout1);
        jScrollPane1.setBorder(BorderFactory.createEtchedBorder());
        //jScrollPane1.setPreferredSize(new Dimension(454, 130));
        locInfoPanel.setBorder(locInfoBorder);
        locInfoPanel.setLayout(gridBagLayout1);
        latitudeLabel.setText(
			dbeditLabels.getString("SiteEditPanel.latitude"));
        longitudeLabel.setText(
			dbeditLabels.getString("SiteEditPanel.longitude"));
        cityLabel.setText(
			dbeditLabels.getString("SiteEditPanel.nearestCity"));
        timeZoneLabel.setText(
			genericLabels.getString("timeZoneLabel"));
        stateLabel.setText(
			dbeditLabels.getString("SiteEditPanel.state"));
        countryLabel.setText(
			dbeditLabels.getString("SiteEditPanel.country"));
        regionLabel.setText(
			dbeditLabels.getString("SiteEditPanel.region"));
        publicNameLabel.setText(
			dbeditLabels.getString("SiteEditPanel.publicName"));

        overallPanel.setLayout(gridBagLayout2);
        namesPanel.setBorder(titledBorder3);
        namesPanel.setLayout(borderLayout2);
        addNameButton.setText(
			genericLabels.getString("add"));
        addNameButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                addNameButton_actionPerformed(e);
            }
        });
        editNameButton.setText(
			genericLabels.getString("edit"));
        editNameButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                editNameButtonPressed();
            }
        });
        deleteNameButton.setText(
			dbeditLabels.getString("SiteEditPanel.deleteButton"));
        deleteNameButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                deleteNameButton_actionPerformed(e);
            }
        });
        nameOpsPanel.setLayout(nameOpsFlowLayout);
        nameOpsFlowLayout.setHgap(5);
        elevLabel.setText(
			dbeditLabels.getString("SiteEditPanel.elevation"));
        elevUnitsLabel.setText(
			dbeditLabels.getString("SiteEditPanel.elevUnits"));
        descPanel.setBorder(descriptionBorder);
        descPanel.setMinimumSize(new Dimension(52, 140));
        descPanel.setPreferredSize(new Dimension(12, 140));
        descPanel.setLayout(borderLayout4);
        descriptionArea.setPreferredSize(new Dimension(0, 100));
        descriptionArea.setMinimumSize(new Dimension(40, 47));
        this.add(overallPanel, BorderLayout.CENTER);

        locInfoPanel.add(latitudeLabel,
			new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(4, 0, 0, 3), 0, 0));
        locInfoPanel.add(latitudeField,
			new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
				new Insets(4, 0, 0, 20), 0, 0));
        locInfoPanel.add(longitudeLabel,
			new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(4, 24, 0, 3), 0, 0));
        locInfoPanel.add(longitudeField,
			new GridBagConstraints(1, 1, 1, 1, 1.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
				new Insets(4, 0, 0, 20), 0, 0));
        locInfoPanel.add(elevLabel,
			new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(4, 29, 3, 0), 0, 0));
        locInfoPanel.add(elevationField,
			new GridBagConstraints(1, 2, 1, 1, 1.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
				new Insets(4, 0, 0, 20), 0, 0));
        locInfoPanel.add(elevUnitsLabel,
			new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(4, 24, 0, 3), 0, 0));
        locInfoPanel.add(elevUnitsCombo,
			new GridBagConstraints(1, 3, 1, 1, 1.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
				new Insets(4, 0, 0, 20), 0, 0));
        locInfoPanel.add(cityLabel,
			new GridBagConstraints(0, 4, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(4, 0, 0, 3), 0, 0));
        locInfoPanel.add(cityField,
			new GridBagConstraints(1, 4, 1, 1, 1.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
				new Insets(4, 0, 0, 20), 0, 0));
        locInfoPanel.add(timeZoneLabel,
			new GridBagConstraints(0, 5, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(4, 22, 0, 3), 0, 0));
        locInfoPanel.add(timeZoneSelector,
			new GridBagConstraints(1, 5, 1, 1, 1.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
				new Insets(4, 0, 0, 20), 0, 0));
        locInfoPanel.add(stateLabel,
			new GridBagConstraints(0, 6, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(4, 51, 0, 3), 0, 0));
        locInfoPanel.add(stateField,
			new GridBagConstraints(1, 6, 1, 1, 1.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
				new Insets(4, 0, 0, 20), 0, 0));
        locInfoPanel.add(countryLabel,
			new GridBagConstraints(0, 7, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(4, 37, 0, 3), 0, 0));
        locInfoPanel.add(countryField,
			new GridBagConstraints(1, 7, 1, 1, 1.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
				new Insets(4, 0, 0, 20), 0, 0));
        locInfoPanel.add(regionLabel,
			new GridBagConstraints(0, 8, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(4, 37, 0, 3), 0, 0));
        locInfoPanel.add(regionField,
			new GridBagConstraints(1, 8, 1, 1, 1.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
				new Insets(4, 0, 0, 20), 0, 0));
        locInfoPanel.add(publicNameLabel,
			new GridBagConstraints(0, 9, 1, 1, 0.0, 0.0,
				GridBagConstraints.NORTHEAST, GridBagConstraints.NONE,
				new Insets(4, 37, 0, 3), 0, 0));
        locInfoPanel.add(publicNameField,
			new GridBagConstraints(1, 9, 1, 1, 1.0, 1.0,
				GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL,
				new Insets(4, 0, 0, 20), 0, 0));

		namesPanel.setPreferredSize(new Dimension(500, 100));
        namesPanel.add(jScrollPane1, BorderLayout.CENTER);
        namesPanel.add(nameOpsPanel, BorderLayout.SOUTH);
        nameOpsPanel.add(addNameButton, null);
        nameOpsPanel.add(editNameButton, null);
        nameOpsPanel.add(deleteNameButton, null);
        descPanel.add(descriptionArea, BorderLayout.CENTER);


        overallPanel.add(namesPanel, 
			new GridBagConstraints(0, 0, 1, 1, 1.0, 0.2,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH, 
			new Insets(0, 0, 0, 0), 0, 0));

        overallPanel.add(locInfoPanel,
			new GridBagConstraints(1, 0, 1, 1, 1.0, 0.2,
				GridBagConstraints.CENTER, GridBagConstraints.BOTH,
				new Insets(0, 0, 0, 0), 0, 0));

        overallPanel.add(descPanel, 
			new GridBagConstraints(0, 1, 2, 1, 1.0, 1.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH, 
			new Insets(0, 0, 0, 0), 0, 0));

		 overallPanel.add(propsPanel,
			new GridBagConstraints(0, 2, 2, 1, 1.0, 1.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH, 
			new Insets(0, 0, 0, 0), 0, 0));

        jScrollPane1.getViewport().add(siteNameTable, null);
    }

    void addNameButton_actionPerformed(ActionEvent e)
	{
		DbEnum nameTypes =
			Database.getDb().getDbEnum(Constants.enum_SiteName);
		if (theSite.getNameCount() >= nameTypes.size())
		{
			TopFrame.instance().showError(
				dbeditLabels.getString("SiteEditPanel.hasAllNames"));
			return;
		}

		SiteNameEntryDialog dlg = new SiteNameEntryDialog();
		dlg.setSite(theSite);
		launchDialog(dlg);
		model.redisplay();
		if (dlg.okPressed)
			namesChanged = true;
    }

	void editNameButtonPressed()
	{
		int idx = siteNameTable.getSelectedRow();
		if (idx < 0)
		{
			TopFrame.instance().showError(
				dbeditLabels.getString("SiteEditPanel.selectNameEdit"));
			return;
		}
		
		SiteName sn = theSite.getNameAt(idx);
		if (sn != null && isCwms 
		 && sn.getNameType().equalsIgnoreCase(Constants.snt_CWMS))
		{
			((TopFrame)getParentFrame()).showError(
				"Cannot modify the CWMS name.");
			return;
		}
		
		SiteNameEntryDialog dlg = new SiteNameEntryDialog();
		dlg.setSite(theSite);
		dlg.setSiteName(model.getRowObject(idx));
		launchDialog(dlg);
		model.redisplay();
		if (dlg.okPressed)
			namesChanged = true;
	}

    void deleteNameButton_actionPerformed(ActionEvent e)
	{
		if (model.getRowCount() <= 1)
		{
			TopFrame.instance().showError(
				dbeditLabels.getString("SiteEditPanel.mustHaveName"));
			return;
		}
		int idx = siteNameTable.getSelectedRow();
		if (idx < 0)
		{
			TopFrame.instance().showError(
				dbeditLabels.getString("SiteEditPanel.selectNameDelete"));
			return;
		}

		SiteName sn = theSite.getNameAt(idx);
		if (sn != null && isCwms 
		 && sn.getNameType().equalsIgnoreCase(Constants.snt_CWMS))
		{
			((TopFrame)getParentFrame()).showError(
				"Cannot delete the CWMS name.");
			return;
		}
		theSite.removeNameAt(idx);
		model.redisplay();
		namesChanged = true;
    }

	/**
	 * From ChangeTracker interface.
	 * @return true if changes have been made to this
	 * screen since the last time it was saved.
	 */
	public boolean hasChanged()
	{
		if (siteNameTable.isEditing())
		{
			TableCellEditor tc = siteNameTable.getCellEditor();
			tc.stopCellEditing();
		}

		String tza = timeZoneSelector.getTZ();
		if (tza == null && theSite.timeZoneAbbr != null)
			tza = "";

		String lat = theSite.latitude == null ? "" : theSite.latitude;
		String lon = theSite.longitude == null ? "" : theSite.longitude;
		String nc = theSite.nearestCity == null ? "" : theSite.nearestCity;

		if (!latitudeField.getText().trim().equals(lat)
		 || !longitudeField.getText().trim().equals(lon)
		 || !cityField.getText().trim().equals(nc)
		 || !TextUtil.strEqual(tza, theSite.timeZoneAbbr))
			return true;

		String st = theSite.state == null ? "" : theSite.state;
		String co = theSite.country == null ? "" : theSite.country;
		String rg = theSite.region == null ? "" : theSite.region;
		String ds = theSite.getDescription();
		if (ds == null) ds = "";
		String pn = theSite.getPublicName() == null ? "" : theSite.getPublicName();

		if (!stateField.getText().equals(st)
		 || !countryField.getText().equals(co)
		 || !regionField.getText().equals(rg)
		 || !descriptionArea.getText().equals(ds)
		 || !publicNameField.getText().equals(pn))
		    return true;

		try
		{
			double d = getEnteredElevation();
			if (d != theSite.getElevation())
				return true;
		}
		catch(NumberFormatException ex)
		{
			TopFrame.instance().showError(
				dbeditLabels.getString("SiteEditPanel.badElev"));
			return true;
		}

		String eu = elevUnitsCombo.getSelectedAbbr();
		if (!TextUtil.strEqualIgnoreCase(eu, theSite.getElevationUnits()))
			return true;

		if (propsPanel.getModel().hasChanged())
			return true;

		return namesChanged;
	}

	private double getEnteredElevation()
		throws NumberFormatException
	{
		String s = elevationField.getText().trim();
		if (s.length() == 0)
			return Constants.undefinedDouble;
		else
			return Double.parseDouble(s);
	}


	/**
	* Copies the edited values back to the Site object.
	* @return true if validation and copy was successful.
	*/
	public boolean copyValuesToObject()
	{
		try { theSite.setElevation(getEnteredElevation()); }
		catch(NumberFormatException ex)
		{
			TopFrame.instance().showError(
				dbeditLabels.getString("SiteEditPanel.badElev"));
			return false;
		}

		namesChanged = false;
		theSite.latitude = latitudeField.getText();
		theSite.longitude = longitudeField.getText();
		theSite.nearestCity = cityField.getText();

        // debugging:
        String tza = timeZoneSelector.getTZ();
        if (tza == null) tza = "";
		theSite.timeZoneAbbr = tza;

        theSite.state = stateField.getText();
		theSite.country = countryField.getText();
		theSite.region = regionField.getText();
		theSite.setDescription(descriptionArea.getText());
		theSite.setElevationUnits(elevUnitsCombo.getSelectedAbbr());
		String pn = publicNameField.getText().trim();
		if (pn.length() == 0)
			pn = null;
		theSite.setPublicName(pn);

		propsPanel.getModel().saveChanges();

		return true;
	}

	/**
	 * From ChangeTracker interface, save the changes back to the database 
	 * &amp; reset the hasChanged flag.
	 * @return true if object was successfully saved.
	 */
	public boolean saveChanges()
	{
		if (!copyValuesToObject())
			return false;

		try
		{
			theSite.write();
		}
		catch(DatabaseException e)
		{
			TopFrame.instance().showError(e.toString());
			return false;
		}
		return true;
	}

	/** @see EntityOpsController */
	public String getEntityName()
	{
		return "Site";
	}

	/** @see EntityOpsController */
	public void commitEntity()
	{
		saveChanges();
	}

	/** @see EntityOpsController */
	public void closeEntity()
	{
		// If editing in progress, stop & save the latest change.
		if (siteNameTable.isEditing())
		{
			TableCellEditor tc = siteNameTable.getCellEditor();
			tc.stopCellEditing();
		}
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
				if (theSite.isNew)
					Database.getDb().siteList.removeSite(theSite);
			}
		}
		if (parent != null)
		{
			DbEditorTabbedPane sitesTabbedPane = parent.getSitesTabbedPane();
			sitesTabbedPane.remove(this);
		}
	}

	/**
	 * Called from File - CloseAll to close this tab, abandoning any changes.
	 */
	public void forceClose()
	{
		DbEditorTabbedPane sitesTabbedPane = parent.getSitesTabbedPane();
		sitesTabbedPane.remove(this);
	}

	/** Called when this panel is activated with a null site. */
	private void disableFields()
	{
    	siteNameTable.setEnabled(false);
    	latitudeField.setEnabled(false);
    	longitudeField.setEnabled(false);
    	cityField.setEnabled(false);
    	timeZoneSelector.setEnabled(false);
    	stateField.setEnabled(false);
    	countryField.setEnabled(false);
    	regionField.setEnabled(false);
    	addNameButton.setEnabled(false);
    	deleteNameButton.setEnabled(false);
    	elevationField.setEnabled(false);
    	elevUnitsCombo.setEnabled(false);
    	descriptionArea.setEnabled(false);
    	publicNameField.setEnabled(false);
	}

	/** Called when this panel is activated with a non-null site. */
	private void enableFields()
	{
    	siteNameTable.setEnabled(true);
    	latitudeField.setEnabled(!noEditSites);
    	longitudeField.setEnabled(!noEditSites);
    	cityField.setEnabled(!noEditSites);
    	timeZoneSelector.setEnabled(!noEditSites);
    	stateField.setEnabled(!noEditSites);
    	countryField.setEnabled(!noEditSites);
    	regionField.setEnabled(!noEditSites);
    	addNameButton.setEnabled(true);
    	deleteNameButton.setEnabled(true);
    	elevationField.setEnabled(!noEditSites);
    	elevUnitsCombo.setEnabled(!noEditSites);
    	descriptionArea.setEnabled(!noEditSites);
       	publicNameField.setEnabled(true);
	}

	@Override
	public void help()
	{
		Help.open();
	}

	public void redisplayModel()
	{
		model.redisplay();
	}
	
	public SiteNamesTableModel getModel()
	{
		return model;
	}

	public void setModel(SiteNamesTableModel model)
	{
		this.model = model;
	}
}


class SiteNamesTableModel extends AbstractTableModel
{
	private Site site;
	private String colNames[] = 
	{
		SiteEditPanel.genericLabels.getString("type"),
		SiteEditPanel.genericLabels.getString("identifier")
	};

	public SiteNamesTableModel()
	{
		super();
	}

    public int getColumnCount() { return colNames.length; }

    public String getColumnName(int c)
    {
        return colNames[c];
    }

	public int getRowCount()
	{
		return site == null ? 0 : site.getNameCount();
	}


	void fillInValues(Site site)
	{
		this.site = site;
	}

	SiteName getRowObject(int idx)
	{
		return site.getNameAt(idx);
	}

	public Object getValueAt(int row, int col)
	{
		SiteName sn = getRowObject(row);
		if ( col == 0 ) {
			return sn.getNameType();
		} else {
 			if ( sn.getNameType().equals("USGS") && this.site != null )
				return (sn.getNameValue()+" (DBNO: "+this.site.getUsgsDbno()+")");
			else
				return(sn.getNameValue());
		}
	}

	public boolean isCellEditable(int r, int c) { return false; }

	public void redisplay()
	{
		fireTableDataChanged();
	}
}
