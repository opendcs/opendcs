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
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

public abstract class ListPanel extends JPanel
{
	protected abstract void doOpen();
	protected abstract void doNew();
	protected abstract void doCopy();
	protected abstract void doDelete();

	protected JButton getOpenButton() 
	{
		JButton openButton = new JButton();
		openButton.setText(CAPEdit.instance().genericDescriptions
        		.getString("edit"));
		openButton.setName("openButton");
		openButton.setPreferredSize(new java.awt.Dimension(100, 25));
		openButton.addActionListener(
			new java.awt.event.ActionListener()
			{
				public void actionPerformed(ActionEvent e) {
					doOpen();
				}
			});
		
		return openButton;
	}

	protected JButton getNewButton() 
	{
		JButton newButton = new JButton();
		newButton.setText(CAPEdit.instance().genericDescriptions
        		.getString("new"));
		newButton.setPreferredSize(new java.awt.Dimension(100, 25));
		newButton.setName("newButton");
		newButton.addActionListener(
			new java.awt.event.ActionListener()
			{
				public void actionPerformed(ActionEvent e) {
					doNew();
				}
			});

		return newButton;
	}

	protected JButton getCopyButton() 
	{
		JButton copyButton = new JButton();
		copyButton.setText(CAPEdit.instance().genericDescriptions
        		.getString("copy"));
		copyButton.setPreferredSize(new java.awt.Dimension(100, 25));
		copyButton.setName("copyButton");
		copyButton.addActionListener(
			new java.awt.event.ActionListener()
			{
				public void actionPerformed(ActionEvent e) {
					doCopy();
				}
			});
		
		return copyButton;
	}

	protected JButton getDeleteButton() 
	{
		JButton deleteButton = new JButton();
		deleteButton.setText(CAPEdit.instance().genericDescriptions
        		.getString("delete"));
		deleteButton.setPreferredSize(new java.awt.Dimension(100, 25));
		deleteButton.setName("deleteButton");
		deleteButton.addActionListener(
			new java.awt.event.ActionListener()
			{
				public void actionPerformed(ActionEvent e) {
					doDelete();
				}
			});
		
		return deleteButton;
	}

	protected JPanel getButtonPanel() 
	{
		GridBagConstraints deleteConstraints = new GridBagConstraints();
		deleteConstraints.insets = new java.awt.Insets(10, 6, 10, 6);
		deleteConstraints.gridy = 0;
		deleteConstraints.weightx = 1.0D;
		deleteConstraints.anchor = java.awt.GridBagConstraints.WEST;
		deleteConstraints.gridx = 3;
		GridBagConstraints copyConstraints = new GridBagConstraints();
		copyConstraints.gridx = 2;
		copyConstraints.insets = new java.awt.Insets(10, 6, 10, 6);
		copyConstraints.gridy = 0;
		GridBagConstraints newConstraints = new GridBagConstraints();
		newConstraints.gridx = 1;
		newConstraints.insets = new java.awt.Insets(10, 6, 10, 6);
		newConstraints.gridy = 0;
		GridBagConstraints openConstraints = new GridBagConstraints();
		openConstraints.gridx = 0;
		openConstraints.insets = new java.awt.Insets(10, 6, 10, 6);
		openConstraints.gridy = 0;
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new GridBagLayout());
		buttonPanel.add(getOpenButton(), openConstraints);
		buttonPanel.add(getNewButton(), newConstraints);
		buttonPanel.add(getCopyButton(), copyConstraints);
		buttonPanel.add(getDeleteButton(), deleteConstraints);
		return buttonPanel;
	}

	protected void showError(String msg)
	{
		CAPEdit.instance().getFrame().showError(msg);
	}
}
