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
package decodes.gui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.*;
import java.util.ResourceBundle;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;

import org.opendcs.gui.GuiHelpers;
import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import decodes.db.EngineeringUnit;
import decodes.dbeditor.DbEditorFrame;
import decodes.rledit.EUTableModel;

/** Dialog for selecting a configuration from a list. */
@SuppressWarnings("serial")
public class EUSelectDialog extends GuiDialog
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	private JButton selectButton = null;
	private JTable euTable = null;
    private static String title = "Select Units";
    private EngineeringUnit selection = null;
    private EUTableModel euTableModel = null;

	/**
	  Construct new dialog.
	  @param ctl the owner of this dialog to receive a call-back when a
	  selection has been made.
	*/
    public EUSelectDialog(JFrame parent)
	{
        super(parent, title, true);
		allInit();
	}

	/**
	  Construct new dialog with dialog parent.
	  @param ctl the owner of this dialog to receive a call-back when a
	  selection has been made.
	*/
    public EUSelectDialog(JDialog parent)
	{
        super(parent, title, true);
		allInit();
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
			GuiHelpers.logGuiComponentInit(log, ex);
		}
    }

	/** Initializes GUI components */
    void jbInit() throws Exception
    {
    	ResourceBundle genericLabels = DbEditorFrame.getGenericLabels();
    	ResourceBundle dbeditLabels = DbEditorFrame.getDbeditLabels();

    	// Build button panel for the south
        FlowLayout buttonLayout = new FlowLayout();
        buttonLayout.setHgap(35);
        buttonLayout.setVgap(10);
        JPanel buttonPanel = new JPanel(buttonLayout);
        selectButton = new JButton(genericLabels.getString("select"));
        selectButton.addActionListener(
        	new java.awt.event.ActionListener()
        	{
        		public void actionPerformed(ActionEvent e)
        		{
        			selectPressed();
        		}
        	});
        JButton cancelButton = new JButton(genericLabels.getString("cancel"));
        cancelButton.addActionListener(
        	new java.awt.event.ActionListener()
        	{
        		public void actionPerformed(ActionEvent e)
        		{
        			cancelButton_actionPerformed(e);
        		}
        	});
        buttonPanel.add(selectButton, null);
        buttonPanel.add(cancelButton, null);

        // Build the scroll-pane with the table for the center
    	euTableModel = new EUTableModel();
    	euTable = new SortingListTable(euTableModel,
    		new int[] { 20, 30, 25, 25 });
		euTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		JScrollPane scrollPane = new JScrollPane();
		scrollPane.getViewport().add(euTable, null);

        // Build the main panel
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        getContentPane().add(mainPanel);

        euTable.addMouseListener(
			new MouseAdapter()
			{
				public void mouseClicked(MouseEvent e)
				{
					if (e.getClickCount() == 2)
					{
						selectPressed();
					}
				}
			});

    }

	/**
	  Called when the Select button is pressed.
	  @param e ignored.
	*/
    void selectPressed()
	{
    	int idx = euTable.getSelectedRow();
    	if (idx < 0)
    		selection = null;
    	else
		{
			//Get the correct row from the table model
			int modelrow = euTable.convertRowIndexToModel(idx);
			EUTableModel tablemodel = (EUTableModel)euTable.getModel();
			selection = (EngineeringUnit)tablemodel.getRowObject(modelrow);
		}
		closeDlg();
    }

	/** Closes the dialog */
	void closeDlg()
	{
		setVisible(false);
		dispose();
	}

	/**
	  Called when Cancel the button is pressed.
	  @param e ignored.
	*/
    void cancelButton_actionPerformed(ActionEvent e)
	{
    	selection = null;
		closeDlg();
    }

	/** Returns the selected configuration, or null if none selected. */
	public EngineeringUnit getSelection()
	{
		return selection;
	}
}
