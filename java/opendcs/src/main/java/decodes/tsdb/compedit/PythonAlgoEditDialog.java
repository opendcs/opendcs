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
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;

import javax.swing.*;

import decodes.tsdb.DbCompAlgorithm;
import decodes.gui.GuiDialog;

@SuppressWarnings("serial")
public class PythonAlgoEditDialog extends GuiDialog
{
	private PythonAlgoEditPanel pythonAlgoEditPanel = null;

	public PythonAlgoEditDialog(JFrame frame)
	{
		super(frame, "Python Algorithm Scripts", true);

		guiInit();
		pack();

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