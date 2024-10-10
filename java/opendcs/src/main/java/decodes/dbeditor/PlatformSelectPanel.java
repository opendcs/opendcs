/*
*  $Id$
*/
package decodes.dbeditor;

import java.awt.*;

import javax.swing.*;
import javax.swing.table.*;

import java.awt.event.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Vector;
import java.util.Collections;
import java.util.Comparator;
import java.util.ResourceBundle;

import ilex.util.Logger;
import decodes.gui.*;
import decodes.db.*;
import decodes.dbeditor.platform.PlatformSelectTableModel;
import decodes.util.DecodesSettings;

/**
Displays a sorting-list of Platform objects in the database.
 */
@SuppressWarnings("serial")
public class PlatformSelectPanel extends JPanel
{
	static ResourceBundle genericLabels = DbEditorFrame.getGenericLabels();
	static ResourceBundle dbeditLabels = DbEditorFrame.getDbeditLabels();

	BorderLayout borderLayout1 = new BorderLayout();
	JScrollPane jScrollPane1 = new JScrollPane();
	PlatformSelectTableModel model;
	JTable platformListTable;
	PlatformSelectDialog parentDialog = null;
	PlatformListPanel parentPanel = null;

	public PlatformSelectPanel(String mediumType)
	{
		model = new PlatformSelectTableModel(this, mediumType, Database.getDb());
		platformListTable = new JTable(model);
		platformListTable.setAutoCreateRowSorter(true);
		platformListTable.getSelectionModel().setSelectionMode(
			ListSelectionModel.SINGLE_SELECTION);
		setMultipleSelection(false);
		platformListTable.addMouseListener(new MouseAdapter()
		{
			public void mouseClicked(MouseEvent e)
			{
				if (e.getClickCount() == 2)
				{
					if (parentDialog != null)
						parentDialog.openPressed();
					else if (parentPanel != null)
						parentPanel.openPressed();
				}
			}
		} );
		try
		{
			jbInit();
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
		}
	}

	public void setParentDialog(PlatformSelectDialog dlg)
	{
		parentDialog = dlg;
	}

	public PlatformSelectPanel(final PlatformSelectDialog psd, Site site, String mediumType)
	{
		if ( site == null ) 
			model = new PlatformSelectTableModel(this, mediumType, Database.getDb());
		else
			model = new PlatformSelectTableModel(this, site, Database.getDb());
		platformListTable = new JTable(model);
		platformListTable.setAutoCreateRowSorter(true);
		platformListTable.getSelectionModel().setSelectionMode(
				ListSelectionModel.SINGLE_SELECTION);
		setMultipleSelection(false);
		platformListTable.addMouseListener(new MouseAdapter(){
			public void mouseClicked(MouseEvent e){
				if (e.getClickCount() == 2){
					psd.openPressed();
				}
			}
		} );
		try
		{
			jbInit();
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
		}
	}

	public void setMultipleSelection(boolean ok)
	{
		platformListTable.getSelectionModel().setSelectionMode(
			ok ? ListSelectionModel.MULTIPLE_INTERVAL_SELECTION :
			ListSelectionModel.SINGLE_SELECTION);
	}

	/** Initializes GUI components. */
		private void jbInit() throws Exception
	{
		this.setPreferredSize(new Dimension(800,500));
		this.setLayout(borderLayout1);
		this.add(jScrollPane1, BorderLayout.CENTER);
		jScrollPane1.getViewport().add(platformListTable, null);
	}

	/**
	  After saving, and edit panel will need to replace the old object
	  with the newly modified one. It calls this method to do this.
	  @param oldp the old object
	  @param newp the new object
	 */
	public void replacePlatform(Platform oldp, Platform newp)
	{
		model.replacePlatform(oldp, newp);
	}

	/**
	 * Adds a new platform to the list.
	  @param newp the new object
	 */
	public void addPlatform(Platform newp)
	{
		model.addPlatform(newp);
	}

	/**
	 * @return the currently-selected platform, or null if none selected.
	 */
	public Platform getSelectedPlatform()
	{
		int r = platformListTable.getSelectedRow();
		if (r == -1)
		{
			return null;
		}
		int modelRow = platformListTable.convertRowIndexToModel(r);
		return model.getPlatformAt(modelRow);
	}

	/**
	 * @return all currently-selected platforms, or empty array if none.
	 */
	public Platform[] getSelectedPlatforms()
	{
		int idx[] = platformListTable.getSelectedRows();
		Platform ret[] = new Platform[idx.length];
		for(int i=0; i<idx.length; i++)
		{
			int modelRow = platformListTable.convertRowIndexToModel(idx[i]);
			ret[i] = model.getPlatformAt(modelRow);
		}
		return ret;
	}


	/**
	 * Deletes the specified platform from the list.
	 * @param ob the object to delete.
	 */
	public void deletePlatform(Platform ob)
	{
		model.deletePlatform(ob);
	}

	public void setParentPanel(PlatformListPanel parentPanel)
	{
		this.parentPanel = parentPanel;
	}

	public JTable getPlatformListTable() {
		return platformListTable;
	}

	public void setPlatformListTable(SortingListTable platformListTable) {
		this.platformListTable = platformListTable;
	}
}
