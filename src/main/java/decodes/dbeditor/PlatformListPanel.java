/*
*  $Id$
*/
package decodes.dbeditor;

import java.awt.*;
import javax.swing.*;
import javax.swing.table.*;
import java.util.Vector;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.ResourceBundle;

import ilex.util.LoadResourceBundle;
import ilex.util.Logger;

import decodes.gui.*;
import decodes.db.*;

/**
Displays a sorting-list of Platform objects in the database.
*/
public class PlatformListPanel extends JPanel
	implements ListOpsController
{
	static ResourceBundle genericLabels = DbEditorFrame.getGenericLabels();
	static ResourceBundle dbeditLabels = DbEditorFrame.getDbeditLabels();

    BorderLayout borderLayout1 = new BorderLayout();
    ListOpsPanel listOpsPanel;
    JLabel jLabel1 = new JLabel();
	PlatformSelectPanel platformSelectPanel = new PlatformSelectPanel(null);
    DbEditorFrame parent;
	int newIndex = 1;

	/** Constructor. */
    public PlatformListPanel()
	{
		parent = null;
    	listOpsPanel = new ListOpsPanel(this);
		listOpsPanel.enableCopy(true);
		platformSelectPanel.setParentPanel(this);
        try
		{
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
    private void jbInit() throws Exception
	{
        this.setLayout(borderLayout1);
        jLabel1.setHorizontalAlignment(SwingConstants.CENTER);
        jLabel1.setText(
			dbeditLabels.getString("PlatformListPanel.title"));
        this.add(listOpsPanel, BorderLayout.SOUTH);
        this.add(jLabel1, BorderLayout.NORTH);
		this.add(platformSelectPanel, BorderLayout.CENTER);
    }

	/** @return type of entity that this panel edits. */
	public String getEntityType() { return "Platform"; }

	public Platform getSelectedPlatform()
	{
		return platformSelectPanel.getSelectedPlatform();
	}

	/** Called when the 'Open' button is pressed. */
	public void openPressed()
	{
		Platform p = platformSelectPanel.getSelectedPlatform();
		if (p == null)
			TopFrame.instance().showError(
				dbeditLabels.getString("PlatformListPanel.selectOpen"));
		else
		{
			try
			{
				p.read();
				doOpen(p);
			}
			catch(Exception ex)
			{
				TopFrame.instance().showError(
					LoadResourceBundle.sprintf(
						dbeditLabels.getString("PlatformListPanel.cannotOpen"),
						p.makeFileName(), ex.toString()));
			}
		}
	}

	/** Called when the 'New' button is pressed. */
	public void newPressed()
	{
		doNew();
	}

	public PlatformEditPanel doNew()
	{
		Platform ob = new Platform();
		platformSelectPanel.addPlatform(ob);
		return doOpen(ob);
	}

	/** Called when the 'Copy' button is pressed. */
	public void copyPressed()
	{
		Platform p = platformSelectPanel.getSelectedPlatform();
		if (p == null)
			TopFrame.instance().showError(
				dbeditLabels.getString("PlatformListPanel.selectCopy"));
		else
		{
			if (!p.isComplete())
			{
				try { p.read(); }
				catch (DatabaseException e)
				{
					TopFrame.instance().showError(
						LoadResourceBundle.sprintf(
						 dbeditLabels.getString("PlatformListPanel.cannotRead"),
						 p.makeFileName(), e.toString()));
					return;
				}
			}

			Platform newOb = p.noIdCopy();
			platformSelectPanel.addPlatform(newOb);
			doOpen(newOb);
		}
	}


	/** Called when the 'Delete' button is pressed. */
	public void deletePressed()
	{
		Platform ob = platformSelectPanel.getSelectedPlatform();
		if (ob == null)
			TopFrame.instance().showError(
				dbeditLabels.getString("PlatformListPanel.selectDelete"));
		else
		{
			DbEditorTabbedPane platformsTabbedPane =
				parent.getPlatformsTabbedPane();
			DbEditorTab tab = platformsTabbedPane.findEditorFor(ob);
			if (tab != null)
			{
				TopFrame.instance().showError(
					dbeditLabels.getString("PlatformListPanel.platformEdited"));
				return;
			}
			int ok = JOptionPane.showConfirmDialog(this,
				dbeditLabels.getString("PlatformListPanel.confirmDelete"),
				dbeditLabels.getString("PlatformListPanel.confirmDeleteTitle"),
				JOptionPane.YES_NO_OPTION);
			if (ok == JOptionPane.YES_OPTION)
			{
				platformSelectPanel.deletePlatform(ob);
				try
				{
					Database.getDb().platformList.write();
				}
				catch(DatabaseException e)
				{
					TopFrame.instance().showError(
					  dbeditLabels.getString("PlatformListPanel.errorDeleting")
					  + e);
				}
			}
		}
	}

	/** Called when the 'Help' button is pressed. */
	public void refreshPressed()
	{
	}

	/**
	  Opens a PlatformEditPanel for the passed platform.
	  @param ob the object to be edited.
	  @return the PlatformEditPanel opened.
	 */
	public PlatformEditPanel doOpen(Platform ob)
	{
		DbEditorTabbedPane tp = parent.getPlatformsTabbedPane();
		DbEditorTab tab = tp.findEditorFor(ob);
		if (tab != null)
		{
			tp.setSelectedComponent(tab);
			return (PlatformEditPanel)tab;
		}
		else
		{
			PlatformEditPanel newTab = new PlatformEditPanel(ob);
			newTab.setParent(parent);
			String title = ob.getPreferredTransportId();
			if (title.equals("unknown"))
				title = "New DCP " + (newIndex++);
			tp.add(title, newTab);
			tp.setSelectedComponent(newTab);
			return newTab;
		}
	}

	/**
	  After saving, and edit panel will need to replace the old object
	  with the newly modified one. It calls this method to do this.
	  @param oldp the old object
	  @param newp the new object
	*/
	public void replacePlatform(Platform oldp, Platform newp)
	{
		platformSelectPanel.replacePlatform(oldp, newp);
	}

	/** Resorts the list by the current column selection. */
	public void reSort()
	{
		platformSelectPanel.reSort();
	}

	/**
	 * Called if a new platform is abandoned before it was ever saved.
	 * @param p the platform.
	 */
	void abandonNewPlatform(Platform p)
	{
		platformSelectPanel.deletePlatform(p);
	}
}
