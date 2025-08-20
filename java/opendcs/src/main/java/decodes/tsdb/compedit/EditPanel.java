/*
*  $Id$
*
*  This is open-source software written by ILEX Engineering, Inc., under
*  contract to the federal government. You are free to copy and use this
*  source code for your own purposes, except that no part of the information
*  contained in this file may be claimed to be proprietary.
*
*  Except for specific contractual terms between ILEX and the federal 
*  government, this source code is provided completely without warranty.
*  For more information contact: info@ilexeng.com
*/
package decodes.tsdb.compedit;

import java.awt.*;
import java.awt.event.*;

import javax.swing.JButton;
import javax.swing.JPanel;

import decodes.gui.TopFrame;

@SuppressWarnings("serial")
public abstract class EditPanel extends JPanel 
{
	protected abstract void doCommit();

	protected abstract void doClose();
	
	protected TopFrame topFrame = null;

	protected JPanel getButtonPanel() 
	{
		JPanel buttonPanel = new JPanel();
		GridBagConstraints closeConstraints = new GridBagConstraints();
		closeConstraints.gridx = 1;
		closeConstraints.insets = new java.awt.Insets(0,6,6,0);
		closeConstraints.weightx = 1.0D;
		closeConstraints.anchor = java.awt.GridBagConstraints.WEST;
		closeConstraints.gridy = 0;
		GridBagConstraints commitConstraints = new GridBagConstraints();
		commitConstraints.gridx = 0;
		commitConstraints.insets = new java.awt.Insets(0,6,6,0);
		commitConstraints.gridy = 0;
		buttonPanel = new JPanel();
		buttonPanel.setLayout(new GridBagLayout());
//		buttonPanel.setPreferredSize(new java.awt.Dimension(430,37));
		buttonPanel.add(getCommitButton(), commitConstraints);
		buttonPanel.add(getCloseButton(), closeConstraints);
		return buttonPanel;
	}
	
	protected JButton getCommitButton() 
	{
		JButton commitButton = new JButton();
		commitButton.setText(CAPEdit.instance().genericDescriptions
        		.getString("commit"));
		commitButton.setName("commitButton");
//		commitButton.setPreferredSize(new java.awt.Dimension(100,25));
		commitButton.addActionListener(
			new java.awt.event.ActionListener()
			{
				public void actionPerformed(ActionEvent e) {
					doCommit();
				}
			});
		
		return commitButton;
	}

	protected JButton getCloseButton() 
	{
		JButton closeButton = new JButton();
		closeButton.setText(CAPEdit.instance().genericDescriptions
        		.getString("close"));
//		closeButton.setPreferredSize(new java.awt.Dimension(100,25));
		closeButton.setName("closeButton");
		closeButton.addActionListener(
			new java.awt.event.ActionListener()
			{
				public void actionPerformed(ActionEvent e) {
					doClose();
				}
			});

		return closeButton;
	}

	protected void showError(String msg)
	{
		if (msg.toLowerCase().contains("insufficient priv"))
			topFrame.showError("Insufficient privilege to write to database.");
		else
			topFrame.showError(msg);
	}

	public void setTopFrame(TopFrame topFrame)
	{
		this.topFrame = topFrame;
	}
}
