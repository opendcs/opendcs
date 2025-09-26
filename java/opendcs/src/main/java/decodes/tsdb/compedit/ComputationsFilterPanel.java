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

import java.awt.*;

import java.util.ArrayList;
import java.util.Objects;
import java.util.ResourceBundle;

import decodes.db.Constants;
import decodes.db.DataType;
import decodes.db.Site;
import decodes.db.SiteName;
import decodes.dbeditor.SiteSelectDialog;
import decodes.sql.DbKey;
import decodes.tsdb.*;
import decodes.tsdb.compedit.computations.ComputationsListPanelTableModel;
import decodes.gui.*;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import opendcs.dai.AlgorithmDAI;
import opendcs.dai.IntervalDAI;
import opendcs.dai.LoadingAppDAI;
import opendcs.dai.TimeSeriesDAI;
import opendcs.dai.TsGroupDAI;

@SuppressWarnings({ "serial", "rawtypes" })
public class ComputationsFilterPanel extends JPanel
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	public JTextField siteText;
	private JButton siteButton;
	public JTextField paramCode;
	private JButton clear;
	public JComboBox intervalBox;
	public JComboBox processBox;
	public JComboBox algorithmBox;
	private JButton refresh;
	private final TimeSeriesDb mydb;
	public DbKey filterSiteId = Constants.undefinedId;
	boolean isDialog;
	ResourceBundle compLabels;
	private String any;
	private TopFrame parentFrame;
	private SiteSelectDialog siteSelectDialog = null;
	private JComboBox groupCombo = null;
	private ArrayList<TsGroup> groupList;
	private JCheckBox hideDisabledCheck = null;
	private boolean filterLowIds = false;
	private final ComputationsListPanelTableModel model;
	private TableRowSorter<ComputationsListPanelTableModel> sorter;
	private final JLabel filterStatusLabel = new JLabel("0/0");

	/**
	 * constructor taking a new database to filter the computations from. if the
	 * database is null then the instance of CAPEdit's database is used
	 *
	 * @param newDb
	 */
	@SuppressWarnings("unchecked")
	ComputationsFilterPanel(TimeSeriesDb newDb, TopFrame parentFrame,
							ComputationsListPanelTableModel model, TableRowSorter<ComputationsListPanelTableModel> sorter,
							boolean filterLowIds)
	{
		this.mydb = Objects.requireNonNull(newDb, "Time Series database must be provided to this panel");
		this.filterLowIds = filterLowIds;
		this.sorter = sorter;
		this.model = model;
		this.parentFrame = parentFrame;
		compLabels = CAPEdit.instance().compeditDescriptions;
		any = compLabels.getString("ComputationsFilterPanel.Any");

		this.setBorder(javax.swing.BorderFactory.createTitledBorder(javax.swing.BorderFactory
			.createLineBorder(java.awt.Color.gray, 2), compLabels.getString("ComputationsFilterPanel.Title"),
			javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
			javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Dialog", java.awt.Font.BOLD,
				12), new java.awt.Color(51, 51, 51)));

		DocumentListener listener = new DocumentListener() {

			@Override
			public void insertUpdate(DocumentEvent e) { setFilterParams(); }

			@Override
			public void removeUpdate(DocumentEvent e) { setFilterParams(); }

			@Override
			public void changedUpdate(DocumentEvent e) { setFilterParams(); }
		};
		siteText = new JTextField();
		siteText.setText(any);
		siteText.setEditable(false);
		siteText.getDocument().addDocumentListener(listener);
		// TODO: This should be a combobox as well
		paramCode = new JTextField();
		paramCode.setText(any);
		paramCode.getDocument().addDocumentListener(listener);
		intervalBox = new JComboBox();
		intervalBox.addItem(any);
		intervalBox.addActionListener(e -> setFilterParams());
		clear = new JButton(CAPEdit.instance().genericDescriptions.getString("clear"));
		clear.addActionListener(e -> clearButtonPressed());
		processBox = new JComboBox();
		processBox.addItem(any);
		processBox.addActionListener(e -> setFilterParams());
		algorithmBox = new JComboBox();
		algorithmBox.addItem(any);
		algorithmBox.addActionListener(e -> setFilterParams());
		refresh = new JButton(compLabels.getString("ComputationsFilterPanel.RefreshButton"));
		siteButton = new JButton(CAPEdit.instance().genericDescriptions.getString("select"));
		siteButton.addActionListener(e -> siteButtonPressed());

		groupCombo = new JComboBox();
		groupCombo.addItem("");
		groupCombo.addActionListener(e -> setFilterParams());

		try (TimeSeriesDAI tsDai = mydb.makeTimeSeriesDAO();
			TsGroupDAI tsGroupDAO = mydb.makeTsGroupDAO();)
		{
			// Loading the TsId cache first provides a drastic performance increase
			tsDai.reloadTsIdCache();
			groupList = tsGroupDAO.getTsGroupList(null);
			for(TsGroup grp : groupList)
				groupCombo.addItem(grp.getGroupId().toString() + " - " + grp.getGroupName());
		}
		catch (DbIoException ex)
		{
			log.atError().setCause(ex).log("Error listing groups.");
		}

		hideDisabledCheck = new JCheckBox(compLabels.getString("ComputationsFilterPanel.hideDisabled"));
		hideDisabledCheck.addChangeListener(e -> setFilterParams());

		this.setLayout(new GridBagLayout());

		this.add(new JLabel(compLabels.getString("ComputationsFilterPanel.SiteLabel")),
			new GridBagConstraints(0, 0, 1, 1, 0, 0,
				GridBagConstraints.EAST, 0, new Insets(4, 10, 0, 0), 0,	0));
		this.add(new JLabel(compLabels.getString("ComputationsFilterPanel.CodeLabel")),
			new GridBagConstraints(0, 1, 1, 1, 0, 0,
				GridBagConstraints.EAST, 0, new Insets(4, 10, 0, 0), 0, 0));
		this.add(new JLabel(compLabels.getString("ComputationsFilterPanel.IntervalLabel")),
			new GridBagConstraints(0, 2, 1, 1, 0, 0,
				GridBagConstraints.EAST, 0, new Insets(4, 10, 0, 0), 0, 0));
		this.add(siteText, new GridBagConstraints(1, 0, 1, 1, 1, 0, GridBagConstraints.CENTER,
			GridBagConstraints.HORIZONTAL, new Insets(4, 4, 0, 0), 0, 0));
		this.add(paramCode, new GridBagConstraints(1, 1, 1, 1, 1, 0, GridBagConstraints.CENTER,
			GridBagConstraints.HORIZONTAL, new Insets(4, 4, 0, 0), 0, 0));
		this.add(siteButton, new GridBagConstraints(2, 0, 1, 1, 0, 0, GridBagConstraints.CENTER,
			GridBagConstraints.HORIZONTAL, new Insets(4, 10, 0, 0), 0, 0));
		this.add(clear, new GridBagConstraints(3, 0, 1, 1, 0, 0, GridBagConstraints.CENTER,
			GridBagConstraints.HORIZONTAL, new Insets(4, 10, 0, 0), 0, 0));

		this.add(intervalBox, new GridBagConstraints(1, 2, 1, 1, 1, 0, GridBagConstraints.CENTER,
			GridBagConstraints.HORIZONTAL, new Insets(4, 4, 0, 0), 0, 0));

		this.add(new JLabel("Uses Group:"),
			new GridBagConstraints(0, 3, 1, 1, 0, 0, GridBagConstraints.EAST, 0, new Insets(4, 10, 10, 0), 0,
				0));
		this.add(groupCombo, new GridBagConstraints(1, 3, 1, 1, 1, 0, GridBagConstraints.CENTER,
			GridBagConstraints.HORIZONTAL, new Insets(4, 4, 10, 0), 0, 0));
		this.add(filterStatusLabel,
			new GridBagConstraints(0, 4, 1, 1, 0, 0, GridBagConstraints.WEST, 0, new Insets(4, 10, 10, 0), 0,
				0));

		this.add(new JLabel(compLabels.getString("ComputationsFilterPanel.ProcessLabel")),
			new GridBagConstraints(5, 0, 1, 1, 0, 0, GridBagConstraints.EAST, 0, new Insets(4, 10, 0, 0), 0,
				0));
		this.add(new JLabel(compLabels.getString("ComputationsFilterPanel.AlgorithmLabel")),
			new GridBagConstraints(5, 1, 1, 1, 0, 0, GridBagConstraints.EAST, 0, new Insets(4, 10, 0, 0), 0,
				0));
		this.add(new JLabel(""), new GridBagConstraints(4, 1, 1, 1, 1, 0, GridBagConstraints.EAST, 0,
			new Insets(4, 10, 0, 0), 0, 0));
		this.add(processBox, new GridBagConstraints(6, 0, 1, 1, 0, 0, GridBagConstraints.CENTER,
			GridBagConstraints.HORIZONTAL, new Insets(4, 4, 0, 10), 0, 0));
		this.add(algorithmBox, new GridBagConstraints(6, 1, 1, 1, 0, 0, GridBagConstraints.CENTER,
			GridBagConstraints.HORIZONTAL, new Insets(4, 4, 0, 10), 0, 0));

		this.add(hideDisabledCheck, new GridBagConstraints(5, 2, 2, 1, 0, 0, GridBagConstraints.CENTER,
				GridBagConstraints.HORIZONTAL, new Insets(4, 4, 0, 10), 0, 0));

		this.add(refresh, new GridBagConstraints(5, 3, 2, 1, 0, 0, GridBagConstraints.CENTER,
			GridBagConstraints.HORIZONTAL, new Insets(4, 4, 10, 10), 0, 0));

		try (IntervalDAI intervalDAO = mydb.makeIntervalDAO();
		 	 LoadingAppDAI loadingAppDao = mydb.makeLoadingAppDAO();
		 	 AlgorithmDAI algoDao = mydb.makeAlgorithmDAO();)
		{
			for (String obj : intervalDAO.getValidIntervalCodes())
				intervalBox.addItem(obj);
			for (CompAppInfo cai : loadingAppDao.listComputationApps(false))
				processBox.addItem(cai.getAppId() + ": " + cai.getAppName());
			for (String algoName : algoDao.listAlgorithmNames())
				algorithmBox.addItem(algoName);
		}
		catch (Exception ex)
		{
			log.atError().setCause(ex).log(compLabels.getString("ComputationsFilterPanel.FillException"));
		}
	}

	private void clearButtonPressed()
	{
		filterSiteId = Constants.undefinedId;
		siteText.setText(any);
	}

	public JButton getRefresh()
	{
		return refresh;
	}

	private void siteButtonPressed()
	{
		if (siteSelectDialog == null)
		{
			siteSelectDialog = new SiteSelectDialog();
			siteSelectDialog.setMultipleSelection(false);
		}
		parentFrame.launchDialog(siteSelectDialog);
		Site site = siteSelectDialog.getSelectedSite();
		if (site != null)
		{
			filterSiteId = site.getId();
			SiteName sn = site.getPreferredName();
			if (sn != null)
				siteText.setText(sn.getNameValue());
		}
	}

	public void setFilterParams()
	{
		CompFilter compFilter = new CompFilter();
		compFilter.setFilterLowIds(filterLowIds);
		compFilter.setSiteId(filterSiteId);
		String x = (String) algorithmBox.getSelectedItem();
		if (!x.equals(any))
		{
			try (AlgorithmDAI algorithmDAO = mydb.makeAlgorithmDAO())
			{
				DbCompAlgorithm algo = algorithmDAO.getAlgorithm(x);
				if (algo != null)
					compFilter.setAlgoId(algo.getId());
			}
			catch (Exception ex)
			{
				log.atWarn().setCause(ex).log("Error setting filter params.");
			}
		}
		else
		{
			compFilter.setAlgoId(Constants.undefinedId);
		}

		x = (String) processBox.getSelectedItem();
		if (!x.equals(any))
		{
			int idx = x.indexOf(':');
			if (idx > 0)
			{
				try
				{
					compFilter.setProcessId(DbKey.createDbKey(Long.parseLong(x.substring(0, idx))));
				}
				catch (NumberFormatException ex)
				{
					log.atTrace().setCause(ex).log("Unable to set Process Filter");
				}
			}
		}
		else
			compFilter.setProcessId(Constants.undefinedId);

		x = paramCode.getText().trim();
		if (x.length() > 0 && !x.equals(any))
		{
			try
			{
				DataType dt = mydb.lookupDataType(x);
				if (dt != null)
					compFilter.setDataTypeId(dt.getId());
			}
			catch (Exception ex)
			{
				log.atWarn().setCause(ex).log("Error setting filter params.");
			}
		}

		x = (String) intervalBox.getSelectedItem();
		if (!x.equals(any))
			compFilter.setIntervalCode(x);
		else
			compFilter.setIntervalCode(null);

		if (groupCombo.getSelectedIndex() > 0)
		{
			TsGroup grp = groupList.get(groupCombo.getSelectedIndex()-1);
			compFilter.setGroupId(grp.getGroupId());
		}

		compFilter.setEnabledOnly(hideDisabledCheck.isSelected());

		updateFilter(compFilter);

	}

	private void updateFilter(CompFilter filter)
	{
		sorter.setRowFilter(new RowFilter<TableModel, Integer>()
		{
			@Override
			public boolean include(Entry<? extends TableModel, ? extends Integer> entry)
			{
				int row = entry.getIdentifier();
				ComputationsListPanelTableModel model = (ComputationsListPanelTableModel) entry.getModel();
				DbComputation comp = model.getCompAt(row);
				return filter.passes(comp);
			}
		});
		updateStatus();
	}

	private void updateStatus()
	{
		int total = model.getRowCount();
		int shown = sorter.getViewRowCount();
		filterStatusLabel.setText(shown + "/" + total);
	}

}
