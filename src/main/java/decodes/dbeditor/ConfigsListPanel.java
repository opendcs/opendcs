/*
*  $Id$
*
*  $Log$
*  Revision 1.3  2008/11/20 18:49:21  mjmaloney
*  merge from usgs mods
*
*/
package decodes.dbeditor;

import java.awt.*;
import javax.swing.*;
import java.util.ResourceBundle;

import ilex.util.TextUtil;
import ilex.util.LoadResourceBundle;

import decodes.gui.*;
import decodes.db.PlatformConfig;
import decodes.db.Database;
import decodes.db.DatabaseException;
import decodes.db.EquipmentModel;
import decodes.util.DecodesSettings;

/**
Panel containing a sortable list of Platform Configurations.
*/
@SuppressWarnings("serial")
public class ConfigsListPanel extends JPanel
	implements ListOpsController
{
	static ResourceBundle genericLabels = DbEditorFrame.getGenericLabels();
	static ResourceBundle dbeditLabels = DbEditorFrame.getDbeditLabels();

    BorderLayout borderLayout1 = new BorderLayout();
	ConfigSelectPanel configSelectPanel;
    ListOpsPanel listOpsPanel;
	JLabel jLabel1 = new JLabel();
	DbEditorFrame parent;

	/** Constructor. */
    public ConfigsListPanel()
	{
        try {
			configSelectPanel = new ConfigSelectPanel();
		    listOpsPanel = new ListOpsPanel(this);
            jbInit();
        }
        catch(Exception ex) 
		{
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
        jLabel1.setText(dbeditLabels.getString("ConfigsListPanel.title"));
        this.add(configSelectPanel, BorderLayout.CENTER);
        this.add(listOpsPanel, BorderLayout.SOUTH);
        this.add(jLabel1, BorderLayout.NORTH);
    }

	//================ from ListOpsController ======================

	/** @return type of entity that this panel edits. */
	public String getEntityType() 
	{ 
		return dbeditLabels.getString("ListPanel.platformConfigEntity"); 
	}

	/** Called when the 'Open' button is pressed. */
	public void openPressed()
	{
		PlatformConfig pc = configSelectPanel.getSelection();
		if (pc == null)
			DbEditorFrame.instance().showError(
				dbeditLabels.getString("ConfigsListPanel.openSelect"));
		else
		{
		    doOpen(pc);
		}
	}

	/** Called when the 'New' button is pressed. */
	public void newPressed()
	{
		String newName = null;
		DecodesSettings settings = DecodesSettings.instance();
		PlatformConfig ob = null;
		EquipmentModel em = null;
		String originator = settings.decodesConfigOwner;
		if ( originator == null || originator.trim().equals("") )
		{
			newName = JOptionPane.showInputDialog(
				DbEditorFrame.instance(),
				dbeditLabels.getString("ConfigsListPanel.enterName"));
			if (newName == null)
				return;
			newName = newName.trim();
			if (!TextUtil.containsNoWhitespace(newName))
			{
				DbEditorFrame.instance().showError(
				dbeditLabels.getString("ConfigsListPanel.noSpaces"));
				return;
			}
			if (Database.getDb().platformConfigList.get(newName) != null)
			{
				DbEditorFrame.instance().showError(
					dbeditLabels.getString("ConfigsListPanel.alreadyExists"));
				return;
			}
			ob = new PlatformConfig(newName);
			configSelectPanel.addConfig(ob);
		} else {
			originator=originator.trim();
			EquipmentModelSelectDialog dlg = new EquipmentModelSelectDialog();
			TopFrame.instance().launchDialog(dlg);
			em = dlg.getSelectedEquipmentModel();
			if (em != null)
			{
				String modelName = em.getName();
				if ( modelName != null ) {
					modelName = modelName.trim();
					try {
						ob = 
						Database.getDb().getDbIo().newPlatformConfig(ob, modelName, originator);
					} catch(DatabaseException ex)
					{
						ob = new PlatformConfig("Unknown");
					}
					ob.equipmentModel = em;
					configSelectPanel.refill();
				}
				else
					return;
			}
			else
				return;
		}
		
		doOpen(ob);
	}

	/** Called when the 'Copy' button is pressed. */
	public void copyPressed()
	{
		PlatformConfig pc = configSelectPanel.getSelection();
		if (pc == null)
		{
			DbEditorFrame.instance().showError(
				LoadResourceBundle.sprintf(
					dbeditLabels.getString("ListPanel.selectCopy"), 
					getEntityType()));
			return;
		}
		try { pc.read(); }
		catch(DatabaseException ex)
		{
			DbEditorFrame.instance().showError(
				dbeditLabels.getString("ConfigsListPanel.cannotRead") + ex);
		}

		String newName = null;
		DecodesSettings settings = DecodesSettings.instance();
		PlatformConfig ob = pc.noIdCopy();
		ob.clearId();
		ob.numPlatformsUsing = 0;
		EquipmentModel em = ob.getEquipmentModel();
		String originator = settings.decodesConfigOwner;
		if ( originator == null || originator.trim().equals("") ) 
		{
	    	newName = JOptionPane.showInputDialog(
	    		DbEditorFrame.instance(),
				dbeditLabels.getString("ListPanel.enterCopyName"));
			if (newName == null)
				return;

			if (Database.getDb().platformConfigList.get(newName) != null)
			{
				DbEditorFrame.instance().showError(
					LoadResourceBundle.sprintf(
						dbeditLabels.getString("ListPanel.alreadyExists"), 
						getEntityType()));
				return;
			}
			ob.configName = newName;
			configSelectPanel.addConfig(ob);
		}
		else
		{
			originator=originator.trim();
			newName = " ";
			if ( em != null ) {
				String modelName = em.getName();
				if ( modelName != null ) { 
					modelName = modelName.trim();
					try {
						ob = 
						Database.getDb().getDbIo().newPlatformConfig(ob, modelName, originator);
						newName = ob.getName();
					} catch(DatabaseException ex)
					{
						newName = "Unknown";
						ob.configName = newName;
					}
				}
			}
	    	newName = JOptionPane.showInputDialog(
					dbeditLabels.getString("ListPanel.enterCopyName"), newName);
			if (newName == null)
				return;
			configSelectPanel.refill();
		}

		doOpen(ob);
	}

	/** Called when the 'Delete' button is pressed. */
	public void deletePressed()
	{
		PlatformConfig pc = configSelectPanel.getSelection();
		if (pc == null)
		{
			DbEditorFrame.instance().showError(
				LoadResourceBundle.sprintf(
					dbeditLabels.getString("ListPanel.selectDelete"), getEntityType()));
			return;
		}

		pc.numPlatformsUsing = Database.getDb().platformList.countUsing(pc);
		if (pc.numPlatformsUsing > 0)
		{
			DbEditorFrame.instance().showError(
				LoadResourceBundle.sprintf(
					dbeditLabels.getString("ConfigsListPanel.usedBy"),
					pc.numPlatformsUsing));
			return;
		}

		DbEditorTabbedPane configsTabbedPane = parent.getSitesTabbedPane();
		DbEditorTab tab = configsTabbedPane.findEditorFor(pc);
		if (tab != null)
		{
			DbEditorFrame.instance().showError(
				dbeditLabels.getString("ConfigsListPanel.beingEdited"));
			return;
		}
		int r = JOptionPane.showConfirmDialog(this,
			dbeditLabels.getString("ConfigsListPanel.confirmDeleteMsg"),
			dbeditLabels.getString("ConfigsListPanel.confirmDeleteTitle"),
			JOptionPane.YES_NO_OPTION);
		if (r == JOptionPane.YES_OPTION)
			configSelectPanel.deleteSelection();
	}

	/** Called when the 'Help' button is pressed. */
	public void refreshPressed()
	{
		configSelectPanel.refill();
	}

	/**
	  Opens a new platform configuration in an Edit Panel.
	  @param pc the object to be edited.
	  @return the editor panel.
	*/
	public ConfigEditPanel doOpen(PlatformConfig pc)
	{
		DbEditorTabbedPane dbtp = parent.getConfigsTabbedPane();
		DbEditorTab tab = dbtp.findEditorFor(pc);
		if (tab != null)
		{
			dbtp.setSelectedComponent(tab);
			return (ConfigEditPanel)tab;
		}
		else
		{
			try { pc.read(); }
			catch(DatabaseException ex)
			{
				if (pc.lastReadTime.getTime() != 0L)
					parent.showError(
						LoadResourceBundle.sprintf(
							dbeditLabels.getString("ConfigsListPanel.readError"),
							pc.configName)
						+ ex);
			}
			ConfigEditPanel newTab = new ConfigEditPanel(pc);
			newTab.setParent(parent);
			String title = pc.makeFileName();
			dbtp.add(title, newTab);
			dbtp.setSelectedComponent(newTab);
			return newTab;
		}
	}

	/**
	  After saving, and edit panel will need to replace the old object
	  with the newly modified one. It calls this method to do this.
	  @param oldPc the old object
	  @param newPc the new object
	*/
	public void replace(PlatformConfig oldPc, PlatformConfig newPc)
	{
		configSelectPanel.replace(oldPc, newPc);
	}
}

