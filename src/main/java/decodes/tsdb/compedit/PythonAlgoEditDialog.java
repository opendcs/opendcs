/*
 * $Id$
 * 
 * $Log$
 * Revision 1.1  2015/10/26 12:46:05  mmaloney
 * Additions for PythonAlgorithm
 *
 *
 * Open Source Software written by Cove Software, LLC under contract to the
 * U.S. Government.
 * 
 * Copyright 2015 U.S. Army Corps of Engineers Hydrologic Engineering Center
 */
package decodes.tsdb.compedit;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;

import javax.swing.*;

import decodes.tsdb.DbCompAlgorithm;
import decodes.gui.GuiDialog;

@SuppressWarnings("serial")
public class PythonAlgoEditDialog 
	extends GuiDialog
{
	private PythonAlgoEditPanel pythonAlgoEditPanel = null;

	public PythonAlgoEditDialog(JFrame frame)
	{
		super(frame, "Python Algorithm Scripts", true);
		
		try 
		{
			guiInit();
			pack();
		}
		catch(Exception ex) 
		{
			ex.printStackTrace();
		}
		trackChanges("PythonAlgoDialog");
	}

	public void setPythonAlgo(DbCompAlgorithm algo)
	{
		pythonAlgoEditPanel.setPythonAlgo(algo);
	}

	/** Fills in values from the object */
	void fillValues()
	{
		pythonAlgoEditPanel.fillValues();
	}

	/** JBuilder-generated method to initialize the GUI components */
	private void guiInit()
	{
		JPanel dlgPanel = new JPanel(new BorderLayout());
		getContentPane().add(dlgPanel);
		JButton okButton = new JButton("OK");
		okButton.addActionListener(
			new java.awt.event.ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					okPressed();
				}
			});
		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 8));
		buttonPanel.add(okButton);
		dlgPanel.add(buttonPanel, BorderLayout.SOUTH);

		pythonAlgoEditPanel = new PythonAlgoEditPanel(this);
		dlgPanel.add(pythonAlgoEditPanel, BorderLayout.CENTER);
	}
	
	

	/** 
	  Called when OK button is pressed. 
	*/
	void okPressed()
	{
		setVisible(false);
		dispose();
	}

	public void saveToObject(DbCompAlgorithm ob)
	{
		pythonAlgoEditPanel.saveToObject(ob);
	}
}



