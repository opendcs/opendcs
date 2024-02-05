package decodes.drgsinfogui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.WindowConstants;

import ilex.util.AsciiUtil;
import ilex.util.Logger;

import decodes.gui.SortingListTable;
import decodes.gui.TopFrame;

/**
 * This class displays the DRGS Receiver GUI Application.
 * It contains all the functions to read and write the
 * DRGS Receiver Identification XML file.
 * 
 */
public class DrgsReceiversListFrame extends TopFrame
{
	public String module = "DrgsReceiversListFrame";
	private DrgsReceiversListTableModel drgsTableModel;
	private SortingListTable drgsListTable;
	//Labels
	private String frameTitle = "DRGS Receivers List";
	private String newButtonLabel = "New";
	private String editButtonLabel = "Edit";
	private String deleteButtonLabel = "Delete";
	private String saveAllButtonLabel = "Save";
	private String quitButtonLabel = "Quit";
	//Columns names
	public String codeColumn = "Code";
	public String locationColumn = "Location";
	public String emailColumn = "E-mail";
	public String descriptionColumn = "Description";
	public String contactColumn = "Contact";
	public String phoneColumn = "Phone #";
	
	public DrgsReceiversListFrame()
	{
		drgsTableModel = new DrgsReceiversListTableModel(this);
		drgsListTable = new SortingListTable(drgsTableModel, 
						new int[] {20, 40, 40, 40, 40, 40});
		drgsListTable.getTableHeader().setReorderingAllowed(false);
		drgsListTable.getSelectionModel().setSelectionMode(
				ListSelectionModel.SINGLE_SELECTION);
		DrgsReceiverDialog.MODIFIED = false;
		//Create Java Swing Components
		jbInit();
		//Default operation is to do nothing when user hits 'X' in
		// upper right to close the window. We will catch the closing
		// event and do the same thing as if user had hit close.
		setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowAdapter()
		{
			public void windowClosing(WindowEvent e)
			{
				doClose();
			}
		});
	}
	
	private void jbInit()
	{
		JPanel mainPanel = new JPanel();
		JScrollPane drgsJScrollPane = new JScrollPane();
		JButton newButton = new JButton(newButtonLabel);
		JButton editButton = new JButton(editButtonLabel);
		JButton deleteButton = new JButton(deleteButtonLabel);
		JButton saveAllButton = new JButton(saveAllButtonLabel);
		JButton quitButton = new JButton(quitButtonLabel);
		
		this.setTitle(frameTitle);
		this.setSize(780,600);
		this.setContentPane(mainPanel);
		newButton.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				newButton_actionPerformed(e);
			}
		});
		editButton.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				editButton_actionPerformed(e);
			}
		});
		deleteButton.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				deleteButton_actionPerformed(e);
			}
		});
		saveAllButton.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				saveAllButton_actionPerformed(e);
			}
		});
		quitButton.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				doClose();
			}
		});
		mainPanel.setLayout(new GridBagLayout());
		drgsJScrollPane.getViewport().add(drgsListTable, null);
		
		mainPanel.add(drgsJScrollPane, new GridBagConstraints(
				0, 0, 1, 4, 1.0, 1.0, GridBagConstraints.CENTER,
				GridBagConstraints.BOTH, new Insets(20, 12, 4, 5), 0, 0));
		mainPanel.add(newButton, new GridBagConstraints(1,
			0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER,
			GridBagConstraints.HORIZONTAL, new Insets(20, 12, 0, 12), 0, 0));
		mainPanel.add(editButton, new GridBagConstraints(1, 1, 1,
						1, 0.0, 0.0, GridBagConstraints.NORTH,
						GridBagConstraints.HORIZONTAL,
						new Insets(5, 12, 2, 12), 16, 0));
		mainPanel.add(deleteButton, new GridBagConstraints(1, 2, 1,
				1, 0.0, 0.0, GridBagConstraints.NORTH,
				GridBagConstraints.HORIZONTAL,
				new Insets(5, 12, 2, 12), 16, 0));
		mainPanel.add(saveAllButton, new GridBagConstraints(1, 3, 1,
				1, 0.0, 0.0, GridBagConstraints.NORTH,
				GridBagConstraints.HORIZONTAL,
				new Insets(5, 12, 2, 12), 16, 0));
		mainPanel.add(quitButton, new GridBagConstraints(0, 4, 2,
				1, 0.0, 0.0, GridBagConstraints.CENTER,
				GridBagConstraints.NONE,
				new Insets(10, 12, 20, 12), 16, 0));
	}
	
	private void newButton_actionPerformed(ActionEvent e)
	{
		DrgsReceiverDialog dlg = new DrgsReceiverDialog(this);
		//Open dialog
		this.launchDialog(dlg);
	}
	
	private void editButton_actionPerformed(ActionEvent e)
	{
		int r = drgsListTable.getSelectedRow();
		if (r == -1)
		{
			String msg = 
				"Select DRGS Receiver record, then press Edit.";
			JOptionPane.showMessageDialog(this,
					msg, "Error!", JOptionPane.ERROR_MESSAGE);
			return;
		}
		int modelRow = drgsListTable.convertRowIndexToModel(r);
		DrgsReceiverIdent dr = drgsTableModel.getDrgsReceiverIdentAt(modelRow);
		//Open Dialog
		if (dr != null)
		{
			DrgsReceiverDialog dlg = new DrgsReceiverDialog(this);
			dlg.fillValues(dr);
			this.launchDialog(dlg);
		}
		else
		{
			Logger.instance().failure(module 
					+ " The select DRGS Receiver rec is null.");
		}
	}
	
	private void deleteButton_actionPerformed(ActionEvent e)
	{
		int r = drgsListTable.getSelectedRow();
		if (r == -1)
		{
			String msg = 
				"Select record, then press Delete.";
			JOptionPane.showMessageDialog(this,
					msg, "Error!", JOptionPane.ERROR_MESSAGE);
			return;
		}
		
		int modelRow = drgsListTable.convertRowIndexToModel(r);
		//Ask user if he is sure about deletion
		int ok = JOptionPane.showConfirmDialog(this,
				"Are you sure you want to delete this record?"
				, "Choose an option",
				JOptionPane.YES_NO_OPTION);

		if (ok == JOptionPane.YES_OPTION)
		{
			//remove drgs record from table model list
			drgsTableModel.deleteDrgsReceiverAt(modelRow);
			save();
		}
	}
	
	private void saveAllButton_actionPerformed(ActionEvent e)
	{
		save();
	}
	
	private boolean save()
	{
		boolean result = true;
		//Get all DRGS records from the table
		Vector<DrgsReceiverIdent> vec = drgsTableModel.getDRGSList();

		if (vec == null || vec.size() == 0)
		{
			return true;
		}
		
		ArrayList<DrgsReceiverIdent> drgsRecvList = new 
											ArrayList<DrgsReceiverIdent>(vec);
		//save DRGS List to XML file
		try
		{
			DrgsReceiverIo.writeDrgsReceiverInfo(drgsRecvList);
			//Save successfull - display info msg
			String msg = "DRGS Information saved to '"
				+ DrgsReceiverIo.drgsRecvXmlFname + "' and html.";
			JOptionPane.showMessageDialog(this,
				AsciiUtil.wrapString(msg, 60), "Info!", 
				JOptionPane.INFORMATION_MESSAGE);
			
			DrgsReceiverDialog.MODIFIED = false;
		}
		catch(IOException ex)
		{
			//Error - display error msg
			showError("Can not write '" + DrgsReceiverIo.drgsRecvXmlFname
				+ " (and HTML): " + ex);
			result = false;
		}
		return result;
	}
	
	private void doClose()
	{
		//if (needToSave)
		if (DrgsReceiverDialog.MODIFIED)//if user changed something
		{
			int r = JOptionPane.showConfirmDialog(this, "Save changes?");
			if (r == JOptionPane.CANCEL_OPTION)
				return;
			else if (r == JOptionPane.YES_OPTION)
			{ 
				if (!save())
					return;
			} else if (r == JOptionPane.NO_OPTION)
			{
			}
		}
		dispose();
		//if (exitOnClose)
			System.exit(0);
	}

	/**
	 * Return the table model used in this frame
	 * @return
	 */
	public DrgsReceiversListTableModel getDrgsTableModel()
	{
		return drgsTableModel;
	}
}