/*
* Where Applicable, Copyright 2025 OpenDCS Consortium and/or its contributors
*
* Licensed under the Apache License, Version 2.0 (the "License"); you may not
* use this file except in compliance with the License. You may obtain a copy
* of the License at
*
*   http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
* License for the specific language governing permissions and limitations
* under the License.
*/
package decodes.tsdb.compedit;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
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
import java.util.Properties;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.table.AbstractTableModel;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import opendcs.dai.TimeSeriesDAI;
import decodes.gui.GuiDialog;
import decodes.gui.SortingListTable;
import decodes.gui.SortingListTableModel;
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
	private static final Logger log = OpenDcsLoggerFactory.getLogger(); 
	SortingListTable paramTable = null;
	PyParamTableModel model = new PyParamTableModel();
	private static TimeSeriesSelectDialog timeSeriesSelectDialog = null;
	private GuiDialog parent = null;
	private Properties initProps = null;

	public PyParamSetDialog(GuiDialog parent)
	{
		super(parent, "Set Time Series Parameters", true);
		this.parent = parent;
		guiInit();
		this.setPreferredSize(new Dimension(600, 250));
		pack();
	}

	public void fillControls(DbCompAlgorithm algo, Properties initProps)
	{
		model.setParms(algo, this.initProps = initProps);
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
				final String msg = "Invalid floating point number '" + vs.trim() + "'.";
				log.atError().setCause(ex).log(msg);
				showError(msg);
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
		tsidStr = tsidStr.trim();

		TimeSeriesDAI tsd = TsdbAppTemplate.theDb.makeTimeSeriesDAO();
		TimeSeriesIdentifier tsid = null;
		if (tsd != null && tsidStr.length() > 0)
		{
			try
			{
				tsid = tsd.getTimeSeriesIdentifier(tsidStr);
			}
			catch (DbIoException ex)
			{
				final String msg = "Cannot access time series database";
				log.atError().setCause(ex).log(msg);
				showError(msg + ": " + ex);
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
					catch (BadTimeSeriesException btse)
					{
						final String msg = "The string '" + tsidStr + "' is not a valid time series "
							+ "identifier in this database.";
						log.atError().setCause(btse).log(msg);
						showError(msg);
						tsid = null;
					}
				}
			}
			finally
			{
				tsd.close();
			}
		}
		else
		{
			tsid = TsdbAppTemplate.theDb.makeEmptyTsId();
			try
			{
				tsid.setUniqueString(tsidStr);
			}
			catch (BadTimeSeriesException ex)
			{
				log.atError().setCause(ex).log("Unable to create new time series.");
			}
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
				TsdbAppTemplate.theDb, true, this);
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
		model.back2props(initProps);
		this.setVisible(false);
	}

	public ArrayList<PyParamSpec> getParamSpecs()
	{
		return model.getParamSpecs();
	}

}

@SuppressWarnings("serial")
class PyParamTableModel extends AbstractTableModel implements SortingListTableModel
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	String colNames[] = { "Role", "Time Series ID", "Value" };

	ArrayList<PyParamSpec> params = new ArrayList<PyParamSpec>();
	NumberFormat numFmt = NumberFormat.getNumberInstance();
	int sortColumn = 0;

	PyParamTableModel()
	{
		numFmt.setMaximumFractionDigits(4);
		numFmt.setGroupingUsed(false);
	}

	public ArrayList<PyParamSpec> getParamSpecs()
	{
		return params;
	}

	public void setParms(DbCompAlgorithm algo, Properties initProps)
	{
		for(Iterator<DbAlgoParm> parmit = algo.getParms(); parmit.hasNext(); )
		{
			DbAlgoParm parm = parmit.next();
			addParm(parm);
		}

		// initProps will contain properties with [rolename].tsid or [rolename].value
		TimeSeriesDAI tsd = TsdbAppTemplate.theDb.makeTimeSeriesDAO();
		try
		{
			ArrayList<String> badProps = new ArrayList<String>();
			for(Object key : initProps.keySet())
			{
				String propName = (String)key;
				String propValue = initProps.getProperty(propName);

				int idx = propName.indexOf('.');
				if (idx > 0 && propName.length() > idx+1)
				{
					String roleName = propName.substring(0, idx);
					String part = propName.substring(idx+1);
					for(PyParamSpec pps : params)
						if (pps.role.equalsIgnoreCase(roleName))
						{
							if (part.equalsIgnoreCase("tsid"))
							{
								pps.tsid = null;
								if (tsd != null)
								{
									try { pps.tsid = tsd.getTimeSeriesIdentifier(propValue); }
									catch (NoSuchObjectException ex)
									{
									}
								}
								if (pps.tsid == null)
								{
									pps.tsid = TsdbAppTemplate.theDb.makeEmptyTsId();
									try { pps.tsid.setUniqueString(propValue); }
									catch (BadTimeSeriesException ex)
									{
										log.atWarn()
										   .setCause(ex)
										   .log("PyParamTableModle.setParms: The string '{}' is  " +
										   	    "not a valid time series identifier in this database.",
												propValue);
										pps.tsid = null;
										badProps.add(propName);
									}
								}
							}
							else if (part.equalsIgnoreCase("value"))
							{
								try { pps.value = Double.parseDouble(propValue.trim()); }
								catch(NumberFormatException ex)
								{
									log.atWarn()
									   .setCause(ex)
									   .log("PyParamTableModle.setParms: invalid value '{}' " +
									   		"-- must be a number.",
											propValue);
									badProps.add(propName);
								}
							}
							else
							{
								log.warn("PyParamTableModle.setParms: unrecognized prop name '{}' -- removed.", propName);
								badProps.add(propName);
							}
						}
				}
			}
			for(String n : badProps)
				initProps.remove(n);

		}
		catch (DbIoException ex)
		{
			log.atWarn().setCause(ex).log("Error in PyParamTableModel.setParms.");
		}
		finally
		{
			if (tsd != null)
				tsd.close();
		}

		sortByColumn(sortColumn);
		this.fireTableDataChanged();
	}

	void back2props(Properties props)
	{
		for(PyParamSpec pps : params)
		{
			if (pps.tsid != null)
				props.setProperty(pps.role + ".tsid", pps.tsid.getUniqueString());
			else
				props.remove(pps.role + ".tsid");
			if (pps.value != null)
				props.setProperty(pps.role + ".value", numFmt.format(pps.value));
			else
				props.remove(pps.role + ".value");
		}
	}

	private void addParm(DbAlgoParm parm)
	{
		for(PyParamSpec pps : params)
			if (parm.getRoleName().equalsIgnoreCase(pps.role))
				return;

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
