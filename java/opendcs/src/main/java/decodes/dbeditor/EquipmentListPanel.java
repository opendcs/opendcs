/*
*  $Id$
*/
package decodes.dbeditor;

import java.awt.*;
import javax.swing.*;
import java.util.ResourceBundle;

import ilex.util.LoadResourceBundle;
import decodes.gui.*;
import decodes.db.EquipmentModel;
import decodes.db.Database;
import decodes.db.Constants;

/**
Panel that shows a list of equipement model objects.
*/
@SuppressWarnings("serial")
public class EquipmentListPanel extends JPanel
	implements ListOpsController
{
	static ResourceBundle genericLabels = DbEditorFrame.getGenericLabels();
	static ResourceBundle dbeditLabels = DbEditorFrame.getDbeditLabels();

    BorderLayout borderLayout1 = new BorderLayout();
    ListOpsPanel listOpsPanel = new ListOpsPanel(this);
   JLabel jLabel1 = new JLabel();
    EquipmentModelSelectPanel equipmentModelSelectPanel =
		new EquipmentModelSelectPanel();
	DbEditorFrame parent;

	/** Constructor. */
    public EquipmentListPanel() 
	{
        try {
            jbInit();
        }
        catch(Exception ex) {
            ex.printStackTrace();
        }
    }

	/**
	  Sets the parent frame object. Each list panel needs to know this.
	  @param parent the DbEditorFrame
	*/
	void setParent(DbEditorFrame parent)
	{
		this.parent = parent;
	}

	/** Initializes GUI components. */
    private void jbInit() throws Exception {
        this.setLayout(borderLayout1);
        jLabel1.setHorizontalAlignment(SwingConstants.CENTER);
        jLabel1.setText(
			dbeditLabels.getString("EquipmentListPanel.title"));
        this.add(listOpsPanel, BorderLayout.SOUTH);
        this.add(jLabel1, BorderLayout.NORTH);
        this.add(equipmentModelSelectPanel, BorderLayout.CENTER);
    }

	/** @return type of entity that this panel edits. */
	public String getEntityType() 
	{
		return dbeditLabels.getString("ListPanel.equipementModelEntity");
	}

	/** Called when the 'Open' button is pressed. */
	public void openPressed()
	{
		EquipmentModel ob = equipmentModelSelectPanel.getSelectedEquipmentModel();
		if (ob == null)
			TopFrame.instance().showError(
				LoadResourceBundle.sprintf(
					dbeditLabels.getString("ListPanel.selectOpen"), getEntityType()));
		else
		    doOpen(ob);
	}

	/** Called when the 'New' button is pressed. */
	public void newPressed()
	{
	    String newName = JOptionPane.showInputDialog(
	    		LoadResourceBundle.sprintf(
	    				dbeditLabels.getString("ListPanel.enterNewName"), 
	    				getEntityType()));
		if (newName == null)
			return;

		if (Database.getDb().equipmentModelList.get(newName) != null)
		{
			TopFrame.instance().showError(LoadResourceBundle.sprintf(
				dbeditLabels.getString("ListPanel.alreadyExists"), 
				getEntityType()));
			return;
		}

		EquipmentModel ob = new EquipmentModel(newName);
		ob.company = "";
	    ob.model = "";
	    ob.description = "";
		equipmentModelSelectPanel.addEquipmentModel(ob);
		doOpen(ob);
	}

	/** Called when the 'Copy' button is pressed. */
	public void copyPressed()
	{
		EquipmentModel ob = equipmentModelSelectPanel.getSelectedEquipmentModel();
		if (ob == null)
		{
			TopFrame.instance().showError(
				LoadResourceBundle.sprintf(
					dbeditLabels.getString("ListPanel.selectCopy"), getEntityType()));
			return;
		}

	    String newName = JOptionPane.showInputDialog(
			dbeditLabels.getString("ListPanel.enterCopyName"));
		if (newName == null)
			return;

		if (Database.getDb().equipmentModelList.get(newName) != null)
		{
			TopFrame.instance().showError(
				LoadResourceBundle.sprintf(
					dbeditLabels.getString("ListPanel.alreadyExists"), 
					getEntityType()));
			return;
		}

		EquipmentModel newOb = ob.copy();
		newOb.name = newName;
		newOb.forceSetId(Constants.undefinedId);
		equipmentModelSelectPanel.addEquipmentModel(newOb);
		doOpen(newOb);
	}

	/** Called when the 'Delete' button is pressed. */
	public void deletePressed()
	{
		EquipmentModel ob = equipmentModelSelectPanel.getSelectedEquipmentModel();
		if (ob == null)
		{
			TopFrame.instance().showError(
				LoadResourceBundle.sprintf(
					dbeditLabels.getString("ListPanel.selectDelete"), getEntityType()));
			return;
		}

		DbEditorTabbedPane dbtp = parent.getEquipmentTabbedPane();
		DbEditorTab tab = dbtp.findEditorFor(ob);
		if (tab != null)
		{
			TopFrame.instance().showError(
				LoadResourceBundle.sprintf(
					dbeditLabels.getString("ListPanel.beingEdited"), getEntityType()));
			return;
		}
		int r = JOptionPane.showConfirmDialog(this,
			LoadResourceBundle.sprintf(
				dbeditLabels.getString("ListPanel.confirmDeleteMsg"), getEntityType()),
			dbeditLabels.getString("ListPanel.confirmDeleteTitle"),
			JOptionPane.YES_NO_OPTION);
		if (r == JOptionPane.YES_OPTION)
			equipmentModelSelectPanel.deleteSelectedEquipmentModel();
	}

	/** Called when the 'Help' button is pressed. */
	public void refreshPressed()
	{
	}

	/**
	  Opens an object in an Edit Panel.
	  @param ob the object to be edited.
	  @return the EquipmentEditPanel opened.
	*/
	public EquipmentEditPanel doOpen(EquipmentModel ob)
	{
		DbEditorTabbedPane dbtp = parent.getEquipmentTabbedPane();
		DbEditorTab tab = dbtp.findEditorFor(ob);
		if (tab != null)
		{
			dbtp.setSelectedComponent(tab);
			return (EquipmentEditPanel)tab;
		}
		else
		{
			EquipmentEditPanel newTab = new EquipmentEditPanel(ob);
			newTab.setParent(parent);
			dbtp.add(ob.name, newTab);
			dbtp.setSelectedComponent(newTab);
			return newTab;
		}
	}

	/**
	  After saving, and edit panel will need to replace the old object
	  with the newly modified one. It calls this method to do this.
	  @param oldOb the old object
	  @param newOb the new object
	*/
	public void replace(EquipmentModel oldOb, EquipmentModel newOb)
	{
		equipmentModelSelectPanel.replace(oldOb, newOb);
	}
}
