package decodes.dbeditor;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.ResourceBundle;


import decodes.db.*;
import decodes.gui.*;

@SuppressWarnings("serial")
public class SiteSelectPanel extends JPanel
{
	static ResourceBundle genericLabels = DbEditorFrame.getGenericLabels();
	static ResourceBundle dbeditLabels = DbEditorFrame.getDbeditLabels();
	static String descriptionLabel = genericLabels.getString("description");

	BorderLayout borderLayout1 = new BorderLayout();
	JScrollPane jScrollPane1 = new JScrollPane();
	SiteSelectTableModel model;
	SortingListTable siteTable;
	SiteSelectDialog parentDialog = null;
	SiteListPanel parentPanel = null;

	public SiteSelectPanel()
	{
	    model = new SiteSelectTableModel(this);
		int cc = model.getColumnCount();
		int widths[] = new int[cc];
		for(int i=0; i<cc-1; i++)
			widths[i] = 10;
		widths[cc-1] = 30;
		siteTable = new SortingListTable(model, widths);
		setMultipleSelection(false);

		siteTable.addMouseListener(new MouseAdapter()
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

		try {
		    jbInit();
		}
		catch(Exception ex) {
		    ex.printStackTrace();
		}
	}

	public void setMultipleSelection(boolean ok)
	{
		siteTable.getSelectionModel().setSelectionMode(
			ok ? ListSelectionModel.MULTIPLE_INTERVAL_SELECTION :
			ListSelectionModel.SINGLE_SELECTION);
	}

	private void jbInit() throws Exception
	{
		this.setLayout(borderLayout1);
		jScrollPane1.setPreferredSize(new Dimension(600, 300));
        this.add(jScrollPane1, BorderLayout.CENTER);
		jScrollPane1.getViewport().add(siteTable, null);
	}

	/**
	 * @return the currently-selected site, or null if no site is selected.
	 */
	public Site getSelectedSite()
	{
		int r = siteTable.getSelectedRow();
		if (r == -1)
			return null;
		//Get the correct row from the table model
		int modelrow = siteTable.convertRowIndexToModel(r);
		SiteSelectTableModel tablemodel = (SiteSelectTableModel)siteTable.getModel();
		return tablemodel.getSiteAt(modelrow);
	}

	public void clearSelection()
	{
		siteTable.clearSelection();
	}

	/**
	 * @return all currently-selected sites, or empty array if none.
	 */
	public Site[] getSelectedSites()
	{
		int idx[] = siteTable.getSelectedRows();
		Site ret[] = new Site[idx.length];
		for(int i=0; i<idx.length; i++)
			ret[i] = model.getSiteAt(idx[i]);
		return ret;
	}

	public void refill()
	{
		model.refill();
		invalidate();
		repaint();
	}

	public void addSite(Site site)
	{
		model.addSite(site);
		invalidate();
		repaint();
	}

	public void deleteSelectedSite()
	{
		int r = siteTable.getSelectedRow();
		if (r == -1)
			return;
		model.deleteSiteAt(r);
	}

	public void setParentDialog(SiteSelectDialog parentDialog)
	{
		this.parentDialog = parentDialog;
	}

	public void setParentPanel(SiteListPanel parentPanel)
	{
		this.parentPanel = parentPanel;
	}
	
	public void setSelection(SiteName sn)
	{
		for(int col=0; col < model.getColumnCount(); col++)
		{
			if (sn.getNameType().equalsIgnoreCase(model.getColumnName(col)))
			{
				for(int row=0; row < model.getRowCount(); row++)
				{
					if (sn.getNameValue().equalsIgnoreCase(
						(String)model.getValueAt(row, col)))
					{
						siteTable.clearSelection();
						siteTable.setRowSelectionInterval(row, row);
						return;
					}
				}
			}
		}
	}
}



