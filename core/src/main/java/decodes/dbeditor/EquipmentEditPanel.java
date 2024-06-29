/*
*  $Id$
*/

package decodes.dbeditor;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;
import java.util.Properties;
import java.util.Iterator;
import java.util.Vector;
import java.util.ResourceBundle;

import ilex.gui.Help;
import ilex.util.Logger;
import ilex.util.TextUtil;

import decodes.db.EquipmentModel;
import decodes.db.PlatformConfig;
import decodes.db.Platform;
import decodes.db.Database;
import decodes.db.DatabaseException;
import decodes.db.Constants;
import decodes.gui.*;

/**
Edit Panel for Equipment Model objects.
These are opened from the EquipmentListPanel.
*/
public class EquipmentEditPanel extends DbEditorTab
	implements ChangeTracker, EntityOpsController
{
	static ResourceBundle genericLabels = DbEditorFrame.getGenericLabels();
	static ResourceBundle dbeditLabels = DbEditorFrame.getDbeditLabels();

	String standardPropertyNames[] = { "DataOrder" };
    TitledBorder titledBorder1;
    TitledBorder titledBorder2;
    EntityOpsPanel entityOpsPanel = new EntityOpsPanel(this);
    JPanel jPanel3 = new JPanel();
    JPanel jPanel2 = new JPanel();
    JPanel jPanel1 = new JPanel();
    JTextField companyField = new JTextField();
    EquipmentTypeSelector equipmentTypeSelector = new EquipmentTypeSelector();
    JTextArea descriptionArea = new JTextArea();
    JLabel jLabel4 = new JLabel();
    JLabel jLabel3 = new JLabel();
    JLabel jLabel2 = new JLabel();
    JLabel jLabel1 = new JLabel();
    BorderLayout borderLayout1 = new BorderLayout();
    JTextField modelField = new JTextField();
    JTextField nameField = new JTextField();
    PropertiesEditPanel propertiesEditPanel;
    BorderLayout borderLayout2 = new BorderLayout();
    GridBagLayout gridBagLayout1 = new GridBagLayout();
    GridBagLayout gridBagLayout2 = new GridBagLayout();

	DbEditorFrame parent;
	EquipmentModel theObject, origObject;

	/** No-args constructor for jbuilder */
    public EquipmentEditPanel()
	{
		Properties props = new Properties();
		propertiesEditPanel =
			new PropertiesEditPanel(props);
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
	public EquipmentEditPanel(EquipmentModel ob)
	{
		this();
		setEquipmentModel(ob);
	}

	/**
	  Sets the object to be edited.
	  @param ob The object
	*/
	public void setEquipmentModel(EquipmentModel ob)
	{
		origObject = ob;
		if (ob == null)
		{
			theObject = null;
			disableFields();
		}
		else
		{
			enableFields();
			theObject = origObject.copy();
			setTopObject(origObject);
			propertiesEditPanel.setProperties(theObject.properties);
			fillFields();
		}
	}

	/**
	  Used specifically for import function, make the editing copy of the
	  object look like the imported object, then reinitialize the fields.
	  @param imported The object
	*/
	public void setImportedEquipmentModel(EquipmentModel imported)
	{
		enableFields();
		theObject.copyFrom(imported);
		propertiesEditPanel.setProperties(theObject.properties);
		fillFields();
	}

	/** Called when this panel is activated with a null EM */
	private void disableFields()
	{
    	companyField.setEnabled(false);
    	equipmentTypeSelector.setEnabled(false);
    	descriptionArea.setEnabled(false);
    	modelField.setEnabled(false);
    	nameField.setEnabled(false);
    	propertiesEditPanel.setEnabled(false);
	}

	/** Called when this panel is activated with a non-null EM */
	private void enableFields()
	{
    	companyField.setEnabled(true);
    	equipmentTypeSelector.setEnabled(true);
    	descriptionArea.setEnabled(true);
    	modelField.setEnabled(true);
    	nameField.setEnabled(true);
    	propertiesEditPanel.setEnabled(true);
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

	/**
	  Fills the GUI fields with the values in the equipment model object.
	*/
	private void fillFields()
	{
		nameField.setText(theObject.name);
		descriptionArea.setText(theObject.description);
		companyField.setText(theObject.company);
		modelField.setText(theObject.model);
		equipmentTypeSelector.setSelection(theObject.equipmentType);
	}

	/**
	  Copies data from the GUI fields back into the object.
	  @return the internal temporary EM object.
	*/
	public EquipmentModel getDataFromFields()
	{
		if (theObject == null)
			return null;

		theObject.name = nameField.getText();
		theObject.description = descriptionArea.getText();
		if (TextUtil.isAllWhitespace(theObject.description))
			theObject.description = null;
		theObject.company = companyField.getText();
		if (TextUtil.isAllWhitespace(theObject.company))
			theObject.company = null;
		theObject.model = modelField.getText();
		if (TextUtil.isAllWhitespace(theObject.model))
			theObject.model = null;
		theObject.equipmentType = equipmentTypeSelector.getSelection();
		propertiesEditPanel.saveChanges();
		return theObject;
	}

	/** Initializes GUI components */
    private void jbInit() throws Exception {
        titledBorder1 = new TitledBorder(
			BorderFactory.createLineBorder(
				new Color(153, 153, 153),2),
				genericLabels.getString("description"));
        titledBorder2 = new TitledBorder(
			BorderFactory.createLineBorder(
				new Color(153, 153, 153),2),
				genericLabels.getString("description"));
        this.setLayout(borderLayout2);
        jPanel2.setBorder(titledBorder2);
        jPanel2.setLayout(borderLayout1);
        jPanel1.setLayout(gridBagLayout1);
        jLabel4.setText(
			dbeditLabels.getString("EquipmentEditPanel.modelLabel"));
        jLabel3.setText(
			dbeditLabels.getString("EquipmentEditPanel.companyLabel"));
        jLabel2.setText(genericLabels.getString("typeLabel"));
        jLabel1.setText(genericLabels.getString("nameLabel"));
        jPanel3.setLayout(gridBagLayout2);
        descriptionArea.setPreferredSize(new Dimension(250, 17));
        nameField.setEditable(false);
        this.add(jPanel3, BorderLayout.CENTER);
        jPanel3.add(jPanel1, new GridBagConstraints(0, 0, 1, 1, 1.0, 0.7
            ,GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 58, 0));
        jPanel1.add(jLabel1, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.2
            ,GridBagConstraints.SOUTHEAST, GridBagConstraints.NONE, new Insets(4, 0, 2, 2), 0, 0));
        jPanel1.add(nameField, new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0
            ,GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(4, 2, 2, 20), 0, 0));
        jPanel1.add(companyField, new GridBagConstraints(1, 2, 1, 1, 1.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(2, 2, 2, 20), 0, 0));
        jPanel1.add(equipmentTypeSelector, new GridBagConstraints(1, 1, 1, 1, 1.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(2, 2, 2, 20), 0, 0));
        jPanel1.add(jLabel4, new GridBagConstraints(0, 3, 1, 1, 0.0, 1.0
            ,GridBagConstraints.NORTHEAST, GridBagConstraints.NONE, new Insets(2, 0, 4, 2), 0, 0));
        jPanel1.add(jLabel3, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(2, 0, 2, 2), 0, 0));
        jPanel1.add(jLabel2, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(2, 0, 2, 2), 0, 0));
        jPanel1.add(modelField, new GridBagConstraints(1, 3, 1, 1, 1.0, 0.0
            ,GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, new Insets(2, 2, 4, 20), 0, 0));
        jPanel1.add(jPanel2, new GridBagConstraints(2, 0, 1, 4, 1.0, 1.0
            ,GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(4, 0, 4, 8), 0, 30));
        jPanel2.add(descriptionArea, BorderLayout.CENTER);
        jPanel3.add(propertiesEditPanel, new GridBagConstraints(0, 1, 1, 1, 1.0, 1.3
            ,GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
    }

	/** @return true if the object has been changed */
	public boolean hasChanged()
	{
		getDataFromFields();
		return !theObject.equals(origObject);
	}

	/**
	  Saves the changes back to the database.
	  @return true if successful.
	*/
	public boolean saveChanges()
	{
		getDataFromFields();
		try
		{
			theObject.validate();
			theObject.write();
		}
		catch(DatabaseException e)
		{
			TopFrame.instance().showError(
				dbeditLabels.getString("EquipmentEditPanel.cannotSave") + e);
			return false;
		}

		// Replace origConfig in ConfigList with the modified config.
		Database.getDb().equipmentModelList.remove(origObject);
		Database.getDb().equipmentModelList.add(theObject);


		// Replace EquipmentModel in every object using this config
		if (theObject.equipmentType.equalsIgnoreCase(Constants.eqType_dcp))
		{
			Vector toMod = new Vector();

			for(PlatformConfig pc : Database.getDb().platformConfigList.values())
			{
				if (pc.equipmentModel != null
				 && pc.equipmentModel.name.equalsIgnoreCase(theObject.name))
				{
					// Can't read PC inside this loop because it will
					// throw a ConcurrentModificationException if it
					// results in an EquipmentModel being added to the
					// list.
					toMod.add(pc);
				}
			}

			// Now modify any affected PCs.
			for(Iterator it = toMod.iterator(); it.hasNext(); )
			{
				PlatformConfig pc = (PlatformConfig)it.next();
				try
				{
	    			pc.read();
	    			pc.equipmentModel = theObject;
	    			pc.write();
				}
				catch(DatabaseException e)
				{
					Logger.instance().log(Logger.E_FAILURE,
						"Could not update PlatformConfig '"
						+ pc.makeFileName() + "': " + e);
				}
			}

			toMod.clear();

			for(Iterator it = Database.getDb().platformList.iterator(); it.hasNext(); )
			{
				Platform p = (Platform)it.next();
				PlatformConfig pc = p.getConfig();
				if (pc != null
				 && pc.equipmentModel != null
				 && pc.equipmentModel.name.equalsIgnoreCase(theObject.name))
				{
					toMod.add(p);
				}
			}
			for(Iterator it = toMod.iterator(); it.hasNext(); )
			{
				Platform p = (Platform)it.next();
				try
				{
					// MJM 3/20/02 - Fix for DR 22 - Don't re-read if already in memory.
					if (!p.isComplete())
						p.read();
					PlatformConfig pc = p.getConfig();
					pc.equipmentModel = theObject;
					p.write();
				}
				catch(DatabaseException e)
				{
					Logger.instance().log(Logger.E_FAILURE,
						"Could not update Platform '"
						+ p.makeFileName() + "': " + e);
				}
			}
		}

		// Config List Panel keeps its own vector, replace old with new:
                if (parent != null)
                {
                  EquipmentListPanel lp = parent.getEquipmentListPanel();
                  lp.replace(origObject, theObject);
                }

		// Make a new copy in case user wants to keep editing.
		origObject = theObject;
		theObject = origObject.copy();
		setTopObject(origObject);
		return true;
	}

	/** @return "EquipementModel" */
	public String getEntityName()
	{
		return "EquipmentModel";
	}

	/** Saves changes to database */
	public void commitEntity()
	{
		saveChanges();
	}

	/** Closes the panel */
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
		if (parent != null)
		{
			DbEditorTabbedPane dbtp = parent.getEquipmentTabbedPane();
			dbtp.remove(this);
		}
	}

	/**
	 * Called from File - CloseAll to close this tab, abandoning any changes.
	 */
	public void forceClose()
	{
		DbEditorTabbedPane dbtp = parent.getEquipmentTabbedPane();
		dbtp.remove(this);
	}


	@Override
	public void help()
	{
		Help.open();
	}
}
