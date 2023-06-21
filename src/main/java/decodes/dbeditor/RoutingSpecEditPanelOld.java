/*
*  $Id$
*/
package decodes.dbeditor;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.*;
import java.util.Date;
import java.util.Comparator;
import java.util.Vector;
import java.util.Iterator;
import java.util.Collections;
import java.util.ResourceBundle;

import ilex.util.LoadResourceBundle;
import ilex.util.TextUtil;

import decodes.gui.*;
import decodes.db.*;

/**
This panel edits an open routing spec.
Opened from the RoutingSpecListPanel.
*/
@SuppressWarnings("serial")
@Deprecated /*(forRemoval = true)*/
public class RoutingSpecEditPanelOld extends DbEditorTab
	implements ChangeTracker, EntityOpsController
{
	static ResourceBundle genericLabels = DbEditorFrame.getGenericLabels();
	static ResourceBundle dbeditLabels = DbEditorFrame.getDbeditLabels();

    TitledBorder titledBorder1;
    TitledBorder titledBorder2;
    TitledBorder titledBorder3;
    EntityOpsPanel entityOpsPanel = new EntityOpsPanel(this);
    JPanel jPanel6 = new JPanel();
    EnumComboBox outputFormatCombo = new EnumComboBox(Constants.enum_OutputFormat);
    JPanel jPanel1 = new JPanel();
    JTextField consumerArgsField = new JTextField();
    JLabel jLabel10 = new JLabel();
    EnumComboBox consumerTypeCombo = new EnumComboBox(Constants.enum_DataConsumer);
    JCheckBox enableEquationsCheck = new JCheckBox();
    DataSourceCombo dataSourceCombo = new DataSourceCombo();
    TimeZoneSelector outputTimezoneCombo = new TimeZoneSelector();
    PresentationGroupCombo presentationGroupCombo = new PresentationGroupCombo();
    JLabel jLabel6 = new JLabel();
    JLabel jLabel5 = new JLabel();
    JLabel jLabel4 = new JLabel();
    JLabel jLabel3 = new JLabel();
    JLabel jLabel2 = new JLabel();
    JPanel jPanel2 = new JPanel();
    JButton addNetListButton = new JButton();
    JPanel jPanel5 = new JPanel();
    JButton removeNetListButton = new JButton();
    JScrollPane jScrollPane1 = new JScrollPane();
    BorderLayout borderLayout2 = new BorderLayout();
    JCheckBox productionCheck = new JCheckBox();
    PropertiesEditPanel propertiesEditPanel;
    JLabel jLabel7 = new JLabel();
    JTextField sinceTimeField = new JTextField();
    JLabel jLabel8 = new JLabel();
    JTextField untilTimeField = new JTextField();
    GridBagLayout gridBagLayout1 = new GridBagLayout();
    GridBagLayout gridBagLayout2 = new GridBagLayout();
    BorderLayout borderLayout1 = new BorderLayout();
    GridBagLayout gridBagLayout3 = new GridBagLayout();
    JTextField nameField = new JTextField();
    JLabel jLabel1 = new JLabel();
    JCheckBox usePMsCheckBox = new JCheckBox();


	DbEditorFrame parent;
	RoutingSpec theObject, origObject;
	RsNlTableModel tableModel;
    JTable networkListTable;
    
	/** No-args constructor for JBuilder. */
    public RoutingSpecEditPanelOld() {
        try {
            jbInit();
        }
        catch(Exception ex) {
            ex.printStackTrace();
        }
    }

	/**
	  Construct new panel to edit specified object.
	  @param ob the object to edit in this panel.
	*/
	public RoutingSpecEditPanelOld(RoutingSpec ob)
	{
		origObject = ob;
		theObject = origObject.copy();
		setTopObject(origObject);

        try
		{
			tableModel = new RsNlTableModel(theObject);
		    networkListTable = new SortingListTable(tableModel,
				new int[] { 35, 25, 50 } );
			networkListTable.getSelectionModel().setSelectionMode(
			    ListSelectionModel.SINGLE_SELECTION);
		    propertiesEditPanel = new PropertiesEditPanel(theObject.getProperties());
			jbInit();
			fillFields();
        }
        catch(Exception ex) {
            ex.printStackTrace();
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
	}

	/** Fills the GUI controls with values from the object. */
	private void fillFields()
	{
		nameField.setText(theObject.getName());
		if (theObject.dataSource != null
		 && theObject.dataSource.getName() != null)
			dataSourceCombo.setSelection(theObject.dataSource.getName());

		if (theObject.consumerType != null)
			consumerTypeCombo.setSelection(theObject.consumerType);
		if (theObject.consumerArg != null)
			consumerArgsField.setText(theObject.consumerArg);
		productionCheck.setSelected(theObject.isProduction);
		enableEquationsCheck.setSelected(theObject.enableEquations);
		usePMsCheckBox.setSelected(theObject.usePerformanceMeasurements);
		if (theObject.outputFormat != null)
			outputFormatCombo.setSelection(theObject.outputFormat);
		if (theObject.outputTimeZoneAbbr != null)
			outputTimezoneCombo.setTZ(theObject.outputTimeZoneAbbr);
		if (theObject.presentationGroupName != null)
			presentationGroupCombo.setSelection(
				theObject.presentationGroupName);
		sinceTimeField.setText(
			theObject.sinceTime == null ? "" : theObject.sinceTime);
		untilTimeField.setText(
			theObject.sinceTime == null ? "" : theObject.untilTime);
	}

	/**
	  Gets the data from the fields & puts it back into the object.
	*/
	private void getDataFromFields()
	{
		tableModel.putTableDataInObject();
		theObject.dataSource = dataSourceCombo.getSelection();

		String s = (String)consumerTypeCombo.getSelectedItem();
		theObject.consumerType = s;
		theObject.consumerArg = consumerArgsField.getText();
		theObject.isProduction = productionCheck.isSelected();
		theObject.enableEquations = enableEquationsCheck.isSelected();
		theObject.usePerformanceMeasurements = usePMsCheckBox.isSelected();
		theObject.outputFormat = outputFormatCombo.getSelection();
		theObject.outputTimeZoneAbbr = outputTimezoneCombo.getTZ();

		PresentationGroup pg = presentationGroupCombo.getSelection();
		if (pg == null)
			theObject.presentationGroupName = null;
		else
			theObject.presentationGroupName = pg.groupName;

		s = sinceTimeField.getText();
		theObject.sinceTime = (s == null || TextUtil.isAllWhitespace(s))
			? null : s;
		s = untilTimeField.getText();
		theObject.untilTime = (s == null || TextUtil.isAllWhitespace(s))
			? null : s;

		theObject.isProduction = productionCheck.isSelected();
		tableModel.putTableDataInObject();
		propertiesEditPanel.saveChanges();
	}

	/** Initializes GUI components */
    private void jbInit() 
    	throws Exception 
    {
        titledBorder1 = new TitledBorder(
			BorderFactory.createLineBorder(new Color(153, 153, 153),2),
				genericLabels.getString("description"));
        titledBorder2 = new TitledBorder(
			BorderFactory.createLineBorder(new Color(153, 153, 153),2),
			dbeditLabels.getString("RoutingSpecEditPanel.timeRange"));
        titledBorder3 = new TitledBorder(
			BorderFactory.createLineBorder(new Color(153, 153, 153),2),
			dbeditLabels.getString("RoutingSpecEditPanel.netlists"));
        this.setLayout(borderLayout2);
        jPanel1.setLayout(gridBagLayout1);
        consumerArgsField.setText("");
        jLabel10.setText(
			dbeditLabels.getString("RoutingSpecEditPanel.dataSource"));
        enableEquationsCheck.setText("Enable Equations");
        jLabel6.setText(
			dbeditLabels.getString("RoutingSpecEditPanel.presGroup"));
        jLabel5.setText(
			dbeditLabels.getString("RoutingSpecEditPanel.outTZ"));
        jLabel4.setText(
			dbeditLabels.getString("RoutingSpecEditPanel.outFormat"));
        jLabel3.setText(
			dbeditLabels.getString("RoutingSpecEditPanel.consumerArgs"));
        jLabel2.setText(
			dbeditLabels.getString("RoutingSpecEditPanel.consumerType"));
        jPanel6.setLayout(gridBagLayout3);
        jPanel2.setLayout(borderLayout1);
        addNetListButton.setText(
			genericLabels.getString("add"));
        addNetListButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                addNetListButton_actionPerformed(e);
            }
        });
        jPanel5.setLayout(gridBagLayout2);
        jPanel5.setBorder(titledBorder3);
        removeNetListButton.setText(
			genericLabels.getString("remove"));
        removeNetListButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                removeNetListButton_actionPerformed(e);
            }
        });
        productionCheck.setText(
			genericLabels.getString("isProduction"));
        jLabel7.setText(
			dbeditLabels.getString("RoutingSpecEditPanel.since"));
        sinceTimeField.setText("now - 4 hours");
        jLabel8.setText(
			dbeditLabels.getString("RoutingSpecEditPanel.until"));
        untilTimeField.setText("now");
        nameField.setBackground(Color.white);
        nameField.setEditable(false);
        jLabel1.setText(
			genericLabels.getString("nameLabel"));
        usePMsCheckBox.setText(
			dbeditLabels.getString("RoutingSpecEditPanel.usePM"));
        jPanel2.setMinimumSize(new Dimension(200, 10));
        jPanel2.setPreferredSize(new Dimension(200, 10));
        this.add(entityOpsPanel, BorderLayout.SOUTH);
        this.add(jPanel6, BorderLayout.CENTER);
        jPanel6.add(jPanel2, new GridBagConstraints(1, 0, 1, 1, 0.6, 1.0
            ,GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 80, 0));
        jPanel2.add(propertiesEditPanel, BorderLayout.CENTER);
        jPanel6.add(jPanel1, new GridBagConstraints(0, 0, 1, 1, 0.4, 1.0
            ,GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
        jPanel1.add(dataSourceCombo, new GridBagConstraints(1, 1, 1, 1, 1.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(2, 2, 2, 15), 0, 0));
        jPanel1.add(jLabel10, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(2, 0, 2, 2), 0, 0));
        jPanel1.add(jLabel2, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(2, 0, 2, 2), 0, 0));
        jPanel1.add(consumerTypeCombo, new GridBagConstraints(1, 2, 1, 1, 1.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(2, 2, 2, 15), 71, 0));
        jPanel1.add(jLabel3, new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(2, 0, 2, 2), 0, 0));
        jPanel1.add(consumerArgsField, new GridBagConstraints(1, 3, 1, 1, 1.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(2, 2, 2, 15), 117, 0));
        jPanel1.add(jLabel4, new GridBagConstraints(0, 4, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(2, 0, 2, 2), 0, 0));
        jPanel1.add(outputFormatCombo, new GridBagConstraints(1, 4, 1, 1, 1.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(2, 2, 2, 15), 27, 0));
        jPanel1.add(jLabel5, new GridBagConstraints(0, 5, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(2, 0, 2, 2), 0, 0));
        jPanel1.add(outputTimezoneCombo, new GridBagConstraints(1, 5, 1, 1, 1.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(2, 2, 2, 15), 98, 0));
        jPanel1.add(jLabel6, new GridBagConstraints(0, 6, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(2, 0, 2, 2), 0, 0));
        jPanel1.add(presentationGroupCombo, new GridBagConstraints(1, 6, 1, 1, 1.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(2, 2, 2, 15), 28, 0));
        jPanel1.add(sinceTimeField, new GridBagConstraints(1, 7, 1, 1, 1.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(2, 2, 2, 15), 18, 0));
        jPanel1.add(jLabel7, new GridBagConstraints(0, 7, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(2, 0, 2, 2), 0, 0));
        jPanel1.add(jLabel8, new GridBagConstraints(0, 8, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(2, 0, 2, 2), 0, 0));
        jPanel1.add(untilTimeField, new GridBagConstraints(1, 8, 1, 1, 1.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(2, 2, 2, 15), 81, 0));
        jPanel1.add(enableEquationsCheck, new GridBagConstraints(1, 9, 1, 1, 0.0, 1.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(4, 4, 2, 0), 0, 0));
        jPanel1.add(nameField, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(6, 2, 2, 15), 0, 0));
        jPanel1.add(jLabel1, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(6, 2, 2, 2), 0, 0));
        jPanel1.add(usePMsCheckBox, new GridBagConstraints(1, 10, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(2, 4, 2, 0), 0, 0));
        jPanel1.add(productionCheck, new GridBagConstraints(1, 11, 1, 1, 0.0, 1.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(2, 4, 6, 0), 0, 0));
        jPanel6.add(jPanel5, new GridBagConstraints(0, 1, 2, 1, 1.0, 1.0
            ,GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
        jPanel5.add(jScrollPane1, new GridBagConstraints(0, 0, 1, 2, 1.0, 1.0
            ,GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 9, 2, 0), -9, -313));
        jPanel5.add(addNetListButton, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 12, 0, 7), 24, 0));
        jPanel5.add(removeNetListButton, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 12, 34, 7), 0, 0));
        jScrollPane1.getViewport().add(networkListTable, null);
    }

	/**
	  Called when the 'Add Network List' button is pressed.
	  Brings up a model NetworkListSelectDialog.
	  @param e ignored.
	*/
    void addNetListButton_actionPerformed(ActionEvent e)
	{
		NetworkListSelectDialog dlg = new NetworkListSelectDialog();
		for(int r=0; r<tableModel.getRowCount(); r++)
		{
			String nlName = (String)tableModel.getValueAt(r, 0);
			dlg.exclude(nlName);
		}
        if (dlg.count() == 0)
        {
            DbEditorFrame.instance().showError(
				dbeditLabels.getString("RoutingSpecEditPanel.noMoreNLs"));
            return;
        }
        launchDialog(dlg);
        if (dlg.okPressed())
        {
			String nlName = dlg.getSelection();
			if (nlName == null)
				return;
			NetworkList nl = null;
			if (nlName.equals("<all>"))
				nl = NetworkList.dummy_all;
			else if (nlName.equals("<production>"))
				nl = NetworkList.dummy_production;
			else
				nl = Database.getDb().networkListList.find(nlName);
			tableModel.addNetworkList(nl);
		}
	}

	/**
	  Called when the 'Remove Network List' button is pressed.
	  The selected network list is removed.
	  @param e ignored.
	*/
    void removeNetListButton_actionPerformed(ActionEvent e)
	{
		int r = networkListTable.getSelectedRow();
		if (r == -1)
		{
			DbEditorFrame.instance().showError(
				dbeditLabels.getString("RoutingSpecEditPanel.selectNLRemove"));
			return;
		}
	    NetworkList nl = tableModel.getObjectAt(r);
		tableModel.deleteObject(nl);
    }


	/**
	 * From ChangeTracker interface.
	 * @return true if changes have been made to this
	 * screen since the last time it was saved.
	 */
	public boolean hasChanged()
	{
		getDataFromFields();
		return !theObject.equals(origObject);
	}

	/**
	 * From ChangeTracker interface, save the changes back to the database 
	 * &amp; reset the hasChanged flag.
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
		catch(DatabaseException e)
		{
			DbEditorFrame.instance().showError(
				LoadResourceBundle.sprintf(
					genericLabels.getString("cannotSave"),
					getEntityName(), e.toString()));
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
	public String getEntityName() { return "RoutingSpec"; }

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
}


@SuppressWarnings("serial")
class RsNlTableModel extends AbstractTableModel
	implements SortingListTableModel
{
	static String columnNames[] =
	{
		RoutingSpecEditPanelOld.dbeditLabels.getString("RoutingSpecEditPanel.listName"),
		RoutingSpecEditPanelOld.dbeditLabels.getString("RoutingSpecEditPanel.mediumType"),
		RoutingSpecEditPanelOld.dbeditLabels.getString("RoutingSpecEditPanel.numSites")
	};
	private int lastSortColumn = -1;

	private RoutingSpec theObject;
	private Vector<NetworkList> theList;

	public RsNlTableModel(RoutingSpec rs)
	{
		super();
		theObject = rs;
		theList = new Vector<NetworkList>();

		for(Iterator<String> it = rs.networkListNames.iterator(); it.hasNext(); )
		{
			String name = it.next();
			NetworkList nl = Database.getDb().networkListList.find(name);
			if (nl == null)
			{
				// Not loaded yet? Try to load it.
				nl = new NetworkList(name);
				try
				{
					nl.read();
					Database.getDb().networkListList.add(nl);
				}
				catch(DatabaseException e)
				{
					DbEditorFrame.instance().showError(
						LoadResourceBundle.sprintf(
							RoutingSpecEditPanelOld.dbeditLabels.getString("RoutingSpecEditPanel.cantReadNL"),
							name, e.toString()));
				}
			}
			theList.add(nl);
		}

		refill();
	}

	void refill()
	{
	}

	public int getRowCount()
	{
		return theList.size();
	}

	public int getColumnCount() { return columnNames.length; }

	public String getColumnName(int col)
	{
		return columnNames[col];
	}

	NetworkList getObjectAt(int r)
	{
		return (NetworkList)getRowObject(r);
	}

	public Object getRowObject(int r)
	{
		if (r >= 0 && r < getRowCount())
			return theList.elementAt(r);
		else return null;
	}

	void addNetworkList(NetworkList ob)
	{
		theList.add(ob);
		fireTableDataChanged();
	}

	void deleteObject(NetworkList ob)
	{
		theList.remove(ob);
		fireTableDataChanged();
	}

	public Object getValueAt(int r, int c)
	{
		NetworkList ob = getObjectAt(r);
		if (ob == null)
			return "";
		else
			return getNlColumn(ob, c);
	}

	public static String getNlColumn(NetworkList ob, int c)
	{
		switch(c)
		{
		case 0: return ob.name;
		case 1: return ob.transportMediumType;
		case 2: return 
			(ob == NetworkList.dummy_all 
				|| ob == NetworkList.dummy_production) ? "" :
					"" + ob.networkListEntries.size();
		default: return "";
		}
	}

	public void resort()
	{
		if (lastSortColumn != -1)
			sortByColumn(lastSortColumn);
	}

	public void sortByColumn(int c)
	{
		lastSortColumn = c;
		Collections.sort(theList, new NetworkListComparator(c));
		fireTableDataChanged();
	}

	void replace(NetworkList oldOb, NetworkList newOb)
	{
		theList.remove(oldOb);
		theList.add(newOb);
		if (lastSortColumn != -1)
			sortByColumn(lastSortColumn);
		else
			fireTableDataChanged();
	}

	public void putTableDataInObject()
	{
		if (theObject.networkLists != null)
			theObject.networkLists.clear();
		theObject.networkListNames.clear();
		for(Iterator<NetworkList> it = theList.iterator(); it.hasNext(); )
		{
			NetworkList nl = it.next();
			theObject.networkListNames.add(nl.name);
		}
	}
}

class RsNlComparator implements Comparator<NetworkList>
{
	int column;

	public RsNlComparator(int column)
	{
		this.column = column;
	}

	/**
	 * Compare the eqMod names of the specified type.
	 */
	public int compare(NetworkList ds1, NetworkList ds2)
	{
		if (ds1 == ds2)
			return 0;

		String s1 = NetlistListTableModel.getNlColumn(ds1, column);
		String s2 = NetlistListTableModel.getNlColumn(ds2, column);

		return s1.compareToIgnoreCase(s2);
	}

	public boolean equals(Object ob)
	{
		return false;
	}
}
