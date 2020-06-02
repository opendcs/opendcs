/*
*  $Id: EUSelectDialog.java,v 1.2 2020/01/31 19:36:48 mmaloney Exp $
*  
*  $Log: EUSelectDialog.java,v $
*  Revision 1.2  2020/01/31 19:36:48  mmaloney
*  Support double-click
*
*  Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
*  OPENDCS 6.0 Initial Checkin
*
*  Revision 1.1  2010/12/09 17:36:00  mmaloney
*  Created
*
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

import decodes.db.EngineeringUnit;
import decodes.dbeditor.DbEditorFrame;
import decodes.rledit.EUTableModel;

/** Dialog for selecting a configuration from a list. */
@SuppressWarnings("serial")
public class EUSelectDialog extends GuiDialog
{
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
			ex.printStackTrace();
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
    		selection = (EngineeringUnit)euTableModel.getRowObject(idx);
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

//	/** Sets current selection. */
//	public void setSelection(String name)
//	{
//		if (name == null)
//			configSelectPanel.clearSelection();
//		else
//			configSelectPanel.setSelection(name);
//	}
}
