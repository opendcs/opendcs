/*
 *  $Id$
 */
package decodes.tsdb.compedit;

import java.awt.*;
import javax.swing.*;
import java.awt.event.*;

import javax.swing.border.*;

import decodes.gui.SortingListTable;
import decodes.tsdb.DbCompAlgorithm;

public class AlgoSelectDialog extends JDialog
{
	private static final long serialVersionUID = 1L;
	private JPanel mainPanel = new JPanel();
	private JButton selectButton = new JButton();
	private JButton cancelButton = new JButton();

	private AlgoSelectPanel algoSelectPanel = new AlgoSelectPanel(this);
	DbCompAlgorithm selectedAlgo = null;
	boolean okPressed = false;

	String selectString;
	String selectAlgoString;

	/**
	 * Construct new dialog.
	 */
	public AlgoSelectDialog(String compName, DbCompAlgorithm selectedAlgo)
	{
		super(CAPEdit.instance().getFrame(),
			CAPEdit.instance().compeditDescriptions
				.getString("AlgoSelectDialog.Title") + " " + compName, true);
		this.selectedAlgo = selectedAlgo;
		fillLabels();
		allInit();
	}

	private void fillLabels()
	{
		selectString = CAPEdit.instance().compeditDescriptions
			.getString("AlgoSelectDialog.Select");
		selectAlgoString = CAPEdit.instance().compeditDescriptions
			.getString("AlgoSelectDialog.SelectBorderTitle");
	}

	private void allInit()
	{
		try
		{
			jbInit();
			getRootPane().setDefaultButton(selectButton);
			pack();
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}

	/** Initializes GUI components */
	void jbInit() throws Exception
	{

		selectButton.setText(selectString);
		selectButton.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				selectButtonPressed();
			}
		});
		cancelButton.setText(CAPEdit.instance().genericDescriptions
			.getString("cancel"));
		cancelButton.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				cancelButtonPressed();
			}
		});

		JPanel buttonPanel = 
			new JPanel(new FlowLayout(FlowLayout.CENTER, 35, 10));
		buttonPanel.add(selectButton, null);
		buttonPanel.add(cancelButton, null);
		
		TitledBorder titledBorder1 = new TitledBorder(BorderFactory.createLineBorder(
			new Color(153, 153, 153), 2), selectAlgoString);
		Border border1 = BorderFactory.createCompoundBorder(titledBorder1,
			BorderFactory.createEmptyBorder(5, 5, 5, 5));
		algoSelectPanel.setBorder(border1);

		mainPanel.setLayout(new BorderLayout());
		mainPanel.add(buttonPanel, BorderLayout.SOUTH);
		mainPanel.add(algoSelectPanel, BorderLayout.CENTER);

		getContentPane().add(mainPanel);
	}

	/**
	 * Called when the Select button is pressed.
	 */
	void selectButtonPressed()
	{
		selectedAlgo = algoSelectPanel.getSelection();
		okPressed = true;
		closeDlg();
	}

	/** Closes the dialog */
	void closeDlg()
	{
		setVisible(false);
		dispose();
	}

	/**
	 * Called when Cancel the button is pressed.
	 */
	void cancelButtonPressed()
	{
		selectedAlgo = null;
		okPressed = false;
		closeDlg();
	}

	/** Returns the selected configuration, or null if none selected. */
	public DbCompAlgorithm getSelectedAlgo()
	{
		return selectedAlgo;
	}

	/** Sets current selection. */
	public void setSelection(String name)
	{
		if (name == null)
		{
			algoSelectPanel.clearSelection();
			selectedAlgo = null;
		}
		else
		{
			algoSelectPanel.setSelection(name);
			selectedAlgo = algoSelectPanel.getSelection();
		}
	}
}

class AlgoSelectPanel extends JPanel
{
	JScrollPane jScrollPane1 = new JScrollPane();
	JTable algoListTable;

	/** Constructor. */
	public AlgoSelectPanel(final AlgoSelectDialog parent)
	{
		this.setLayout(new BorderLayout());
		this.add(jScrollPane1, BorderLayout.CENTER);

		AlgorithmsListPanel alp = CAPEdit.instance().algorithmsListPanel;
		algoListTable = new SortingListTable(alp.algoListTableModel,
			alp.algoListTableModel.columnWidths);
		algoListTable.addMouseListener(
			new MouseAdapter()
			{
				public void mouseClicked(MouseEvent e)
				{
					if (e.getClickCount() == 2)
					{
						parent.selectButtonPressed();
					}
				}
			});


		jScrollPane1.getViewport().add(algoListTable, null);
	}

	/**
	 * @return the currently-selected algorithm, or null if none is selected.
	 */
	public DbCompAlgorithm getSelection()
	{
		int r = algoListTable.getSelectedRow();
		if (r == -1)
		{
			return null;
		}
		int modelRow = algoListTable.convertRowIndexToModel(r);
		AlgorithmsListPanel algorithmsListPanel = CAPEdit.instance().algorithmsListPanel;
		return algorithmsListPanel.algoListTableModel.getRowAlgorithm(modelRow);
	}

	/**
	 * Sets the current selection.
	 * 
	 * @param name
	 *            the name of the selected configuration.
	 */
	public void setSelection(String name)
	{
		AlgorithmsListPanel algorithmsListPanel = CAPEdit.instance().algorithmsListPanel;
		algoListTable = algorithmsListPanel.algoListTable;
		int n = algorithmsListPanel.algoListTableModel.getRowCount();
		for (int i = 0; i < n; i++)
		{
			String algoName = (String)algorithmsListPanel.algoListTableModel.getValueAt(i, 1);
			
			if (name.equalsIgnoreCase(algoName))
			{
				algoListTable.setRowSelectionInterval(i, i);
				Rectangle rect = algoListTable.getCellRect(i, 0, true);
				algoListTable.scrollRectToVisible(rect);
				break;
			}
		}
	}

	/** Clears any selections which have been made. */
	public void clearSelection()
	{
		AlgorithmsListPanel algorithmsListPanel = CAPEdit.instance().algorithmsListPanel;
		algoListTable = algorithmsListPanel.algoListTable;
		algoListTable.clearSelection();
	}
}
