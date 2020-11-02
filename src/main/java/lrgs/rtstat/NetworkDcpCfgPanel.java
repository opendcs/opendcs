/**
 * 
 */
package lrgs.rtstat;

import ilex.net.BasicClient;
import ilex.util.LoadResourceBundle;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.ResourceBundle;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.AbstractTableModel;

import ilex.util.TextUtil;
import lrgs.drgs.DrgsConnectCfg;
import lrgs.drgs.DrgsInputSettings;

import decodes.gui.GuiDialog;
import decodes.gui.SortingListTable;
import decodes.gui.SortingListTableModel;
import decodes.gui.TopFrame;

/**
 * @author mjmaloney
 *
 */
public class NetworkDcpCfgPanel
    extends JPanel
{
	private LrgsConfigDialog parent;
	private JCheckBox enabledCheck;
	private JButton addButton;
	private JButton editButton;
	private JButton deleteButton;
	private JButton testButton;
	private SortingListTable conTable;
	private NetworkDcpTableModel model;
	private boolean wasEnabled = false;
	private static ResourceBundle labels = 
		RtStat.getLabels();
	private static ResourceBundle genericLabels = 
		RtStat.getGenericLabels();


	public NetworkDcpCfgPanel(LrgsConfigDialog parent)
	{
		super(new GridBagLayout());
		this.parent = parent;
		initialize();
	}
	
	private void initialize()
	{
		this.setBorder(
			BorderFactory.createTitledBorder(null, "Network DCPs", 
				TitledBorder.CENTER, TitledBorder.BELOW_TOP, 
				new Font("Dialog", Font.BOLD, 14), new Color(51, 51, 51)));

		// Define the overall layout
		enabledCheck = new JCheckBox(
			labels.getString("LrgsConfigDialog.enableNetworkDcps"));
		this.add(enabledCheck, 
			new GridBagConstraints(0, 0, 2, 1, 0.0, 0.0, 
				GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(10, 10, 10, 0), 0, 0));  
	
		JPanel buttonPanel = new JPanel(new GridBagLayout());
		this.add(buttonPanel, 
			new GridBagConstraints(2, 1, 1, 1, 0.0, 1.0, 
				GridBagConstraints.NORTH, GridBagConstraints.NONE,
				new Insets(5, 5, 5, 5), 0, 0));  
			
		JScrollPane connectionScrollPane = new JScrollPane();
		this.add(connectionScrollPane,
			new GridBagConstraints(0, 1, 2, 1, 1.0, 1.0, 
				GridBagConstraints.WEST, GridBagConstraints.BOTH,
				new Insets(5, 5, 5, 5), 0, 0));

		// Define the add, edit, delete, test buttons
		addButton = new JButton(genericLabels.getString("add"));
		buttonPanel.add(addButton, 
			new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
				GridBagConstraints.NORTH, GridBagConstraints.BOTH,
				new Insets(10, 5, 5, 5), 0, 0));
		addButton.addActionListener(
			new ActionListener() 
			{
				public void actionPerformed(ActionEvent e) 
				{
					addPressed();
				}
			});
		editButton= new JButton(genericLabels.getString("edit"));
		editButton.addActionListener(
			new ActionListener() 
			{
				public void actionPerformed(ActionEvent e) 
				{
					editPressed();
				}
			});
		buttonPanel.add(editButton, 
			new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
				GridBagConstraints.NORTH, GridBagConstraints.BOTH,
				new Insets(5, 5, 5, 5), 0, 0));
		deleteButton= new JButton(genericLabels.getString("delete"));
		deleteButton.addActionListener(
			new ActionListener() 
			{
				public void actionPerformed(ActionEvent e) 
				{
					deletePressed();
				}
			});
		buttonPanel.add(deleteButton, 
			new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0,
				GridBagConstraints.NORTH, GridBagConstraints.BOTH,
				new Insets(5, 5, 5, 5), 0, 0));
		testButton= new JButton(genericLabels.getString("test"));
		testButton.addActionListener(
			new ActionListener() 
			{
				public void actionPerformed(ActionEvent e) 
				{
					testPressed();
				}
			});
		buttonPanel.add(testButton, 
			new GridBagConstraints(0, 3, 1, 1, 0.0, 1.0,
				GridBagConstraints.NORTH, GridBagConstraints.BOTH,
				new Insets(5, 5, 10, 5), 0, 0));

		// Define the scrollpane containing the table
		model = new NetworkDcpTableModel();
		conTable = new SortingListTable(model, model.columnWidths);
		connectionScrollPane.setViewportView(conTable);
	}
	
	private void addPressed()
	{
		DrgsConnectCfg cfg = new DrgsConnectCfg(conTable.getRowCount(), "");
		NetworkDcpDialog dlg = new NetworkDcpDialog(parent);
		dlg.setInfo(cfg);
		parent.launchDialog(dlg);
		if (dlg.okPressed())
			model.add(cfg);
	}

	private void editPressed()
	{
		int idx = conTable.getSelectedRow();
		if (idx == -1)
		{
			parent.showError(labels.getString(
				"LrgsConfigDialog.selectConnEditErr"));
			return;
		}
		NetworkDcpDialog dlg = new NetworkDcpDialog(parent);
		DrgsConnectCfg cfg = (DrgsConnectCfg)model.getRowObject(idx);
		dlg.setInfo(cfg);
		parent.launchDialog(dlg);
		if (dlg.okPressed())
			model.modified();
	}

	private void deletePressed()
	{
		int idx = conTable.getSelectedRow();
		if (idx == -1)
		{
			parent.showError(labels.getString(
				"LrgsConfigDialog.selectDRGSConnDelErr"));
			return;
		}
		
		DrgsConnectCfg cfg = (DrgsConnectCfg)model.getRowObject(idx);
		if( JOptionPane.showConfirmDialog(this,
			LoadResourceBundle.sprintf(
				labels.getString("LrgsConfigDialog.DRGSConnDel"),cfg.name),
				labels.getString("LrgsConfigDialog.confirmDelete"),
					JOptionPane.YES_NO_OPTION)
			== JOptionPane.YES_OPTION)
		{
			model.deleteAt(idx);
		}

	}
	private void testPressed()
	{
		int idx = conTable.getSelectedRow();
		if (idx == -1)
		{
			parent.showError(labels.getString(
				"LrgsConfigDialog.selectConnEditErr"));
			return;
		}
		DrgsConnectCfg cfg = (DrgsConnectCfg)model.getRowObject(idx);
		
		BasicClient myClient = new BasicClient(cfg.host,cfg.msgPort);
		
		LrgsConnectionTest myTester = new LrgsConnectionTest(parent, myClient);
		myTester.startConnect();
	}
	
	public void setContents(DrgsInputSettings settings, boolean isEnabled)
	{
		model.setContents(settings);
		enabledCheck.setSelected(isEnabled);
		wasEnabled = isEnabled;
	}
	
	public void clear()
	{
		model.clear();
		wasEnabled = false;
	}
	
	public boolean hasChanged()
	{
		if (model.modified)
			return true;
		return wasEnabled != enabledCheck.isSelected();
	}
	
	public boolean networkDcpEnable()
	{
		return enabledCheck.isSelected();
	}
	
	public Collection<DrgsConnectCfg> getConnections()
	{
		return model.cons;
	}
}

class NetworkDcpTableModel 
	extends AbstractTableModel 
	implements SortingListTableModel
{
	private static ResourceBundle labels = 
		RtStat.getLabels();
	
	String columnNames[] = null;
	int columnWidths[] = { 10, 30, 30, 15, 15};
	ArrayList<DrgsConnectCfg> cons;
	boolean modified = false;
	int sortColumn = 0;
	
	public NetworkDcpTableModel()
	{
		columnNames = new String[5];
		columnNames[0] = labels.getString("LrgsConfigDialog.useColumn");
		columnNames[1] = labels.getString("LrgsConfigDialog.nameColumn");
		columnNames[2] = labels.getString("LrgsConfigDialog.hostIPAddrColumn");
		columnNames[3] = labels.getString("LrgsConfigDialog.msgPortColumn");
		columnNames[4] = labels.getString("LrgsConfigDialog.pollPeriodColumn");
	
		cons = new ArrayList<DrgsConnectCfg>();
	}
	
	public void setContents(DrgsInputSettings settings)
	{
		cons.clear();
		for(DrgsConnectCfg con : settings.connections)
			cons.add(new DrgsConnectCfg(con));
		Collections.sort(cons);
	}
	
	public void clear()
	{
		cons.clear();
	}
	
	public void add(DrgsConnectCfg cfg)
	{
		cons.add(cfg);
		modified();
	}
	
	public void modified()
	{
		super.fireTableDataChanged();
		modified = true;
	}
	
	public void deleteAt(int idx)
	{
		if (idx < 0 || idx >= cons.size())
			return;
		cons.remove(idx);
		for(int i=idx; i<cons.size(); i++)
			cons.get(i).connectNum = i;
		modified();
	}
	
	public int getRowCount()
	{
		return cons.size();
	}
	
	public Object getRowObject(int r)
	{
		if (r >=0 && r < cons.size())
			return cons.get(r);
		return null;
	}
	
	public String getColumnName(int col)
	{
		return columnNames[col];
	}
	
	public Object getValueAt(int r, int c)
	{
		DrgsConnectCfg cfg = (DrgsConnectCfg)getRowObject(r);
		if (cfg == null)
			return "";
	
		switch(c)
		{
		case 0: return "" + cfg.msgEnabled;
		case 1: return cfg.name == null ? "" : cfg.name;
		case 2: return cfg.host == null ? "" : cfg.host;
		case 3: return "" + cfg.msgPort;
		case 4: return "" + cfg.pollingPeriod;
		default: return "";
		}
	}
	
	public int getColumnCount() 
	{
		return columnNames.length;
	}

	public void sortByColumn(int c)
	{
		sortColumn = c;
		Collections.sort(cons, new DCCComparator(sortColumn));
	}
}

class DCCComparator implements Comparator
{
	int sortColumn;
	public DCCComparator(int sortColumn)
	{
		this.sortColumn = sortColumn;
	}

	public int compare(Object o1, Object o2)
	{
		if (o1 == o2)
			return 0;
		DrgsConnectCfg dcc1 = (DrgsConnectCfg)o1;
		DrgsConnectCfg dcc2 = (DrgsConnectCfg)o2;
		switch(sortColumn)
		{
		case 0: 
			// sort true to the front.
			int r = -((""+dcc1.msgEnabled).compareTo(""+dcc2.msgEnabled));
			if (r != 0)
				return r;
			// fall through to name sort...
		case 1: 
			return TextUtil.strCompareIgnoreCase(dcc1.name, dcc2.name);
		case 2:
			return TextUtil.strCompareIgnoreCase(dcc1.host, dcc2.host);
		case 3:
			return dcc1.msgPort - dcc2.msgPort;
		case 4: 
		default:
			return dcc1.pollingPeriod - dcc2.pollingPeriod;
		}
	}
}
