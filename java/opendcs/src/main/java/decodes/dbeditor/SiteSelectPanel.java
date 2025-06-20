package decodes.dbeditor;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.TableRowSorter;

import org.opendcs.gui.SearchPanel;

import java.util.Collections;
import java.util.ResourceBundle;

import decodes.db.*;
import decodes.util.DecodesSettings;

@SuppressWarnings("serial")
public class SiteSelectPanel extends JPanel
{
	static ResourceBundle genericLabels = DbEditorFrame.getGenericLabels();
	static ResourceBundle dbeditLabels = DbEditorFrame.getDbeditLabels();
	static String descriptionLabel = genericLabels.getString("description");

	BorderLayout borderLayout1 = new BorderLayout();
	JScrollPane jScrollPane1 = new JScrollPane();
	SiteSelectTableModel model;
	private TableRowSorter<SiteSelectTableModel> sorter;
	private JTable siteTable;     
	SiteSelectDialog parentDialog = null;
	SiteListPanel parentPanel = null;
	private JTextField filterField;
	private JLabel filterStatusLabel = new JLabel("0/0");

	public SiteSelectPanel()
	{
	    model = new SiteSelectTableModel(this);
		siteTable = new JTable(model);
		sorter = new TableRowSorter<>(model);
        siteTable.setRowSorter(sorter);
		
		sorter.setSortKeys(
            Collections.singletonList(
                new RowSorter.SortKey(getDefaultSortingColumnIndex(), SortOrder.ASCENDING)
            )
        );
        sorter.sort();
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

	// MJM 20080707 - Initial sorting should be by preferred name type.
	private int getDefaultSortingColumnIndex()
	{
		String pref = DecodesSettings.instance().siteNameTypePreference;
        int initialCol = 0;
		if (pref != null)
		{
			for(int i=0; i<model.getColumnCount(); i++)
				if (model.getColumnName(i).equalsIgnoreCase(pref))
				{
					initialCol = i;
					break;
				}
		}
		return initialCol;
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
		SearchPanel searchPanel = new SearchPanel(sorter, model);
		this.add(searchPanel,BorderLayout.NORTH);
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



