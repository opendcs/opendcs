/*
*  $Id$
*  
*  Open Source Software
*  
*  Author: Mike Maloney, Cove Software, LLC
*  
*  $Log$
*  Revision 1.1  2013/02/28 16:44:26  mmaloney
*  New SearchCriteriaEditPanel implementation.
*
*/
package lrgs.gui;

import ilex.util.Logger;

import java.awt.*;

import javax.swing.*;

import java.awt.event.*;
import decodes.gui.TopFrame;
import decodes.util.PdtEntry;

/**
Dialog for selecting one or more platforms.
Used by both Db Editor for import/export and for network list building.
*/
public class PdtSelectDialog extends JDialog
{
	private TopFrame parent = null;
    private PdtSelectPanel selectPanel;
    private PdtEntry[] selections = null;
	private boolean cancelled;

	public PdtSelectDialog(TopFrame owner)
	{
        super(owner, "", true);
        this.parent = owner;
		init();
	}

	private void init()
	{
		setTitle("PDT Selection");
		JPanel mainPanel = new JPanel(new BorderLayout());
		
		selectPanel = new PdtSelectPanel(this);
		mainPanel.add(selectPanel, BorderLayout.CENTER);
		
		JButton reloadButton = new JButton("Reload");
		reloadButton.addActionListener(
			new java.awt.event.ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					reloadPressed();
				}
			});
		JButton selectButton = new JButton("Select");
		selectButton.addActionListener(
			new java.awt.event.ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					selectPressed();
				}
			});
		JButton cancelButton = new JButton("Cancel");
		cancelButton.addActionListener(
			new java.awt.event.ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					cancelPressed();
				}
			});
		
		JPanel buttonPanel = new JPanel(new GridBagLayout());
		buttonPanel.add(reloadButton,
			new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE, 
				new Insets(4, 10, 4, 5), 0, 0));
		buttonPanel.add(selectButton,
			new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE, 
				new Insets(4, 10, 4, 5), 0, 0));
		buttonPanel.add(cancelButton,
			new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE, 
				new Insets(4, 5, 4, 5), 0, 0));

		mainPanel.add(buttonPanel, BorderLayout.SOUTH);
		getContentPane().add(mainPanel);

//		platformSelectPanel.setBorder(
//			BorderFactory.createTitledBorder(
//				BorderFactory.createEtchedBorder(EtchedBorder.RAISED), "Platform Selection"));

		
		
		

		getRootPane().setDefaultButton(selectButton);
        pack();
		cancelled = false;
	}

	protected void cancelPressed()
	{
		selections = null;
		cancelled = true;
		closeDlg();
	}

	protected void selectPressed()
	{
		selections = selectPanel.getSelectedEntries();
		cancelled = false;
		closeDlg();
	}

	protected void reloadPressed()
	{
Logger.instance().info("PdtSelectDialog.reloadPressed");
		selectPanel.reload();
	}
	
	/** Closes dialog. */
	void closeDlg()
	{
		setVisible(false);
		dispose();
	}

	/** 
	  Called with true if multiple selection is to be allowed. 
	  @param ok true if multiple selection is to be allowed.
	*/
	public void setMultipleSelection(boolean ok)
	{
		selectPanel.setMultipleSelection(ok);
	}

	public PdtEntry[] getSelections()
	{
		return selections;
	}

	public boolean isCancelled()
	{
		return cancelled;
	}
}
