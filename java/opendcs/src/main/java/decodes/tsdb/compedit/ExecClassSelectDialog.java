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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingWorker;
import javax.swing.border.TitledBorder;
import javax.swing.table.TableRowSorter;

import org.opendcs.gui.GuiHelpers;
import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import decodes.tsdb.DbCompAlgorithm;
import decodes.tsdb.NoSuchObjectException;
import decodes.tsdb.TimeSeriesDb;
import decodes.tsdb.compedit.algotab.ExecClassTableModel;

/**
 * Dialog to select an equipment model by name.
 */
@SuppressWarnings("serial")
public class ExecClassSelectDialog extends JDialog
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	private JButton selectButton = new JButton("Select");
	private JButton cancelButton = new JButton("Cancel");
	private DbCompAlgorithm selection;
	private boolean _cancelled;
	private final ExecClassTableModel model;
	private final JTable tab;//b = new JTable(model);

	/** No args constructor for JBuilder */
	public ExecClassSelectDialog(JFrame theFrame, TimeSeriesDb tsDb)
	{
		super(theFrame, "Select Executable Class", true);
		_cancelled = false;
		model = new ExecClassTableModel(tsDb);
		tab = new JTable(model);
		try
		{
			guiInit();
			pack();
			getRootPane().setDefaultButton(selectButton);
		}
		catch (Exception ex)
		{
			GuiHelpers.logGuiComponentInit(log, ex);
		}
	}

	void guiInit() throws Exception
	{
		JPanel thePanel = new JPanel(new BorderLayout());
		thePanel.setBorder(new TitledBorder("Select Executable Class"));
		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 25, 8));
		selectButton.addActionListener(e -> selectPressed());
		buttonPanel.add(selectButton);
		cancelButton.addActionListener(e -> cancelPressed());
		buttonPanel.add(cancelButton);

		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setPreferredSize(new Dimension(600, 400));
		scrollPane.getViewport().add(tab, null);
		tab.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		tab.setRowSorter(new TableRowSorter<>(model));
		tab.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				if (e.getClickCount() == 2)
				{
					selectPressed();
				}
			}
		} );


		thePanel.add(scrollPane, BorderLayout.CENTER);
		thePanel.add(buttonPanel, BorderLayout.SOUTH);
		getContentPane().add(thePanel);
	}

	public void load()
		throws NoSuchObjectException
	{
		new SwingWorker<Void,Void>()
		{
			@Override
			protected Void doInBackground() throws Exception
			{
				model.load();
				return null;
			}

		}.execute();
	}

	public void setSelection(String selection)
	{
		if (selection != null)
		{
			int modelRow = model.indexOf(selection);
			if (modelRow != -1)
			{
				int tableRow = tab.convertRowIndexToView(modelRow);;
				tab.setRowSelectionInterval(tableRow, tableRow);
				this.selection = model.getAlgoAt(modelRow);
			}
		}
		else
		{
			this.selection = null;
		}

	}

	public DbCompAlgorithm getSelection()
	{
		return selection;
	}

	void selectPressed()
	{
		_cancelled = false;
		int idx = tab.getSelectedRow();
		if (idx < 0)
			_cancelled = true;
		else
		{
			int modelRow = tab.convertRowIndexToModel(idx);
			selection = model.getAlgoAt(modelRow);
		}


		closeDlg();
	}

	/** Closes the dialog. */
	void closeDlg()
	{
		setVisible(false);
		dispose();
	}

	void cancelPressed()
	{
		_cancelled = true;
		closeDlg();
	}

	/** @return true if cancel was pressed. */
	public boolean wasCancelled()
	{
		return _cancelled;
	}
}