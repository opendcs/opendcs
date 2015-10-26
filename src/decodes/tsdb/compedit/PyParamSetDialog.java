package decodes.tsdb.compedit;

import ilex.util.LoadResourceBundle;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.table.AbstractTableModel;

import opendcs.dai.TimeSeriesDAI;
import decodes.gui.GuiDialog;
import decodes.gui.SortingListTable;
import decodes.gui.SortingListTableModel;
import decodes.gui.TopFrame;
import decodes.tsdb.BadTimeSeriesException;
import decodes.tsdb.DbAlgoParm;
import decodes.tsdb.DbCompAlgorithm;
import decodes.tsdb.DbIoException;
import decodes.tsdb.NoSuchObjectException;
import decodes.tsdb.TimeSeriesIdentifier;
import decodes.tsdb.TsdbAppTemplate;
import decodes.tsdb.groupedit.TimeSeriesSelectDialog;

@SuppressWarnings("serial")
public class PyParamSetDialog extends GuiDialog
{
	SortingListTable paramTable = null;
	PyParamTableModel model = new PyParamTableModel();
	private static TimeSeriesSelectDialog timeSeriesSelectDialog = null;
	private GuiDialog parent = null;
	
	public PyParamSetDialog(GuiDialog parent)
	{
		super(parent, "Set Time Series Parameters", true);
		this.parent = parent;
		guiInit();
		this.setPreferredSize(new Dimension(600, 250));
		pack();
	}
	
	public void fillControls(DbCompAlgorithm algo)
	{
		model.setParms(algo);
	}
	
	private void guiInit()
	{
		JPanel mainPanel = new JPanel(new BorderLayout());
		getContentPane().add(mainPanel);
		
		JPanel southButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 3, 10));
		JButton okButton = new JButton("OK");
		okButton.addActionListener(
			new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					okPressed();
				}
			});
		southButtonPanel.add(okButton);
		mainPanel.add(southButtonPanel, BorderLayout.SOUTH);
		
		JScrollPane sp = new JScrollPane();
		paramTable = new SortingListTable(model, new int[]{20, 60, 20});
		sp.getViewport().add(paramTable);
		mainPanel.add(sp, BorderLayout.CENTER);
		
		JPanel eastButtonPanel = new JPanel(new GridBagLayout());
		mainPanel.add(eastButtonPanel, BorderLayout.EAST);
		JButton selectTsButton = new JButton("Select Time Series");
		eastButtonPanel.add(selectTsButton,
			new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
				new Insets(4, 3, 2, 3), 0, 0));
		selectTsButton.addActionListener(
			new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					selectTsPressed();
				}
			});
		JButton typeTsButton = new JButton("Type TS ID");
		eastButtonPanel.add(typeTsButton,
			new GridBagConstraints(0, 1, 1, 1, 1.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
				new Insets(2, 3, 2, 3), 0, 0));
		typeTsButton.addActionListener(
			new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					typeTsIdPressed();
				}
			});
		JButton setValueButton = new JButton("Set Value");
		eastButtonPanel.add(setValueButton,
			new GridBagConstraints(0, 2, 1, 1, 1.0, 1.0,
				GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL,
				new Insets(2, 3, 2, 3), 0, 0));
		setValueButton.addActionListener(
			new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					setValuePressed();
				}
			});


	}

	protected void setValuePressed()
	{
		int row = paramTable.getSelectedRow();
		if (row == -1)
		{
			showError("Select param, then press Set Value.");
			return;
		}
		PyParamSpec pps = (PyParamSpec)model.getRowObject(row);


		String vs = JOptionPane.showInputDialog("Enter Time Series Value: ",
			pps.value == null ? "" : model.numFmt.format(pps.value));
		if (vs == null)
			return;
		if (vs.trim().length() == 0)
			model.setTsValueAt(row, null);
		else
			try
			{
				model.setTsValueAt(row, Double.parseDouble(vs.trim()));
			}
			catch(NumberFormatException ex)
			{
				showError("Invalide floating point number '" + vs.trim() + "'.");
				return;
			}
	}

	protected void typeTsIdPressed()
	{
		int row = paramTable.getSelectedRow();
		if (row == -1)
		{
			showError("Select param, then press Type Time Series ID.");
			return;
		}

	    String tsidStr = JOptionPane.showInputDialog(
	    	"Enter Time Series Identifier: ");
		if (tsidStr == null)
			return;
		
		TimeSeriesDAI tsd = TsdbAppTemplate.theDb.makeTimeSeriesDAO();
		TimeSeriesIdentifier tsid = null;
		try
		{
			tsid = tsd.getTimeSeriesIdentifier(tsidStr);
		}
		catch (DbIoException ex)
		{
			showError("Cannot access time series database: " + ex);
			tsid = null;
		}
		catch (NoSuchObjectException ex)
		{
			int r = JOptionPane.showConfirmDialog(null, 
				"Time Series '" + tsidStr + "' is not found in your database. Proceed?", 
				"No such time series", JOptionPane.OK_CANCEL_OPTION);
			if (r == JOptionPane.OK_OPTION)
			{
				tsid = TsdbAppTemplate.theDb.makeEmptyTsId();
				try
				{
					tsid.setUniqueString(tsidStr);
				}
				catch (BadTimeSeriesException e)
				{
					showError("The string '" + tsidStr + "' is not a valid time series "
						+ "identifier in this database.");
					tsid = null;
				}
			}
		}
		finally
		{
			tsd.close();
		}
		
		model.setTsidAt(row, tsid);
	}



	protected void selectTsPressed()
	{
		int row = paramTable.getSelectedRow();
		if (row == -1)
		{
			showError("Select param, then press Select Time Series.");
			return;
		}
		PyParamSpec pps = (PyParamSpec)model.getRowObject(row);
		
		if (timeSeriesSelectDialog == null)
		{
			timeSeriesSelectDialog = new TimeSeriesSelectDialog(
				TsdbAppTemplate.theDb, true);
			timeSeriesSelectDialog.setMultipleSelection(false);
		}
		if (pps.tsid != null)
			timeSeriesSelectDialog.setSelectedTS(pps.tsid);
		parent.launchDialog(timeSeriesSelectDialog);
		TimeSeriesIdentifier ts[] = timeSeriesSelectDialog.getSelectedDataDescriptors();
		// Should be either zero or 1 in the return value
		model.setTsidAt(row, ts.length > 0 ? ts[0] : null);
	}

	protected void okPressed()
	{
		System.out.println("OK Pressed");
		this.setVisible(false);
	}

}

class PyParamTableModel extends AbstractTableModel
	implements SortingListTableModel
{
	String colNames[] = { "Role", "Time Series ID", "Value" };

	ArrayList<PyParamSpec> params = new ArrayList<PyParamSpec>();
	NumberFormat numFmt = NumberFormat.getNumberInstance();
	int sortColumn = 0;

	PyParamTableModel()
	{
		numFmt.setMaximumFractionDigits(4);
		numFmt.setGroupingUsed(false);
	}
	
	public void setParms(DbCompAlgorithm algo)
	{
		for(Iterator<DbAlgoParm> parmit = algo.getParms(); parmit.hasNext(); )
		{
			DbAlgoParm parm = parmit.next();
			addParm(parm);
		}
		sortByColumn(sortColumn);
System.out.println("after setParms, params.size()=" + params.size());
		this.fireTableDataChanged();
	}
	
	private void addParm(DbAlgoParm parm)
	{
		for(PyParamSpec pps : params)
			if (parm.getRoleName().equalsIgnoreCase(pps.role))
				return;
System.out.println("Adding parm to model: " + parm.getRoleName());

		params.add(new PyParamSpec(parm.getParmType().toLowerCase().startsWith("i"),
			parm.getRoleName()));
	}

	@Override
	public int getRowCount()
	{
		return params.size();
	}

	@Override
	public int getColumnCount()
	{
		return colNames.length;
	}
	
	@Override
	public String getColumnName(int c)
	{
		return colNames[c];
	}


	@Override
	public Object getValueAt(int rowIndex, int columnIndex)
	{
		if (rowIndex < 0 || rowIndex >= params.size())
			return null;
		PyParamSpec pps = params.get(rowIndex);
		return
			columnIndex == 0 ? pps.role :
			columnIndex == 1 ? (pps.tsid == null ? "" : pps.tsid.getUniqueString()) :
			columnIndex == 2 ? (pps.value == null ? "" : numFmt.format(pps.value)) : 
			""; 
	}

	@Override
	public void sortByColumn(final int column)
	{
		Collections.sort(params,
			new Comparator<PyParamSpec>()
			{
				private int compareRole(PyParamSpec o1, PyParamSpec o2)
				{
					if (o1.isInput != o2.isInput)
						return o1.isInput ? -1 : 1;
					else return o1.role.compareTo(o2.role);
				}
				
				@Override
				public int compare(PyParamSpec o1, PyParamSpec o2)
				{
					if (column == 0)
						return compareRole(o1, o2);
					else if (column == 1)
					{
						if (o1.tsid == null && o2.tsid == null)
							return compareRole(o1, o2);
						else if (o1.tsid == null)
							return 1;
						else if (o2.tsid == null)
							return -1;
						else
							return (o1.tsid.getUniqueString().compareTo(
								o2.tsid.getUniqueString()));
					}
					if (o1.value == null && o2.value == null)
						return compareRole(o1, o2);
					else if (o1.value != null)
						return -1;
					else if (o2.value != null)
						return 1;
					else return o1.value < o2.value ? -1 :
						o1.value > o2.value ? 1 : 0;
				}
			});
		sortColumn = column;
	}

	@Override
	public Object getRowObject(int row)
	{
		return params.get(row);
	}
	
	public void setTsidAt(int row, TimeSeriesIdentifier tsid)
	{
		params.get(row).tsid = tsid;
		this.fireTableRowsUpdated(row, row);
	}
	
	public void setTsValueAt(int row, Double value)
	{
		params.get(row).value = value;
		this.fireTableRowsUpdated(row, row);
	}


	
}
