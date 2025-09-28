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
package decodes.tsdb.groupedit;

import ilex.util.LoadResourceBundle;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.util.ResourceBundle;

import javax.swing.JButton;
import javax.swing.JPanel;

import decodes.util.DecodesSettings;

/**
This panel appears at the bottom of the Ts Definition panels and contains the
'Save', 'Close' and Corrections buttons.
*/
public class TsEntityOpsPanel extends JPanel
{
    JButton saveButton = new JButton();
    JButton closeButton = new JButton();
    JButton evaluateButton = new JButton();

    GridBagLayout gridBagLayout1 = new GridBagLayout();
    TsEntityOpsController myController;


        /**
          Constructor.
          @param ctl the owning panel must implement these methods.
        */
    public TsEntityOpsPanel(TsEntityOpsController ctl)
    {
    	ResourceBundle groupResources = LoadResourceBundle.getLabelDescriptions(
			"decodes/resources/groupedit",
			DecodesSettings.instance().language);
		ResourceBundle genericResources = LoadResourceBundle.getLabelDescriptions(
			"decodes/resources/generic",
			DecodesSettings.instance().language);

        //For internationalization, get the title description
        //from properties file
        saveButton.setText(genericResources.getString("save"));
        closeButton.setText(genericResources.getString("close"));
        evaluateButton.setText(genericResources.getString("evaluate"));

        myController = ctl;


		jbInit();

    }

    /** GUI component initialization. */
	private void jbInit()
	{
		saveButton.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(ActionEvent e) {
				saveButton_actionPerformed(e);
			}
		});
		this.setLayout(gridBagLayout1);
		closeButton.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(ActionEvent e) {
				closeButton_actionPerformed(e);
			}
		});
		evaluateButton.addActionListener(
			new java.awt.event.ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
						evaluateButton_actionPerformed(e);
				}
			});

		this.add(saveButton, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
				new Insets(12, 10, 12, 10), 0, 0));
		this.add(closeButton, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
				new Insets(12, 10, 12, 10), 0, 0));

		// This buttons will show up ONLY in the Ts Definition Panel
		if (myController instanceof TsGroupDefinitionPanel)
		{
			this.add(evaluateButton, new GridBagConstraints(4, 0, 1, 1, 0.0,
				0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
				new Insets(12, 280, 12, 10), 0, 0));
		}
	}

    /**
	 * Called when user presses the 'save' Button.
	 *
	 * @param e
	 *            ignored.
	 */
	void saveButton_actionPerformed(ActionEvent e)
	{
		myController.saveEntity();
	}

	/**
	 * Called when user presses the 'Close' Button.
	 *
	 * @param e
	 *            ignored.
	 */
	void closeButton_actionPerformed(ActionEvent e)
	{
		myController.closeEntity();
	}

	/**
	 * Called when user presses the 'Corrections' Button.
	 *
	 * @param e
	 *            ignored.
	 */
	void evaluateButton_actionPerformed(ActionEvent e)
	{
		myController.evaluateEntity();
	}
}