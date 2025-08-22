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
package decodes.dbeditor;

import java.awt.*;
import javax.swing.*;

import org.opendcs.gui.GuiHelpers;
import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import java.awt.event.*;
import java.util.ResourceBundle;

/**
 * All of the 'xxxListPanel.java' objects display a ListOpsPanel at the bottom.
 */
public class ListOpsPanel extends JPanel
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	static ResourceBundle genericLabels = DbEditorFrame.getGenericLabels();
	static ResourceBundle dbeditLabels = DbEditorFrame.getDbeditLabels();

	private JButton openButton = new JButton();
	private JButton newButton = new JButton();
	private JButton deleteButton = new JButton();
	private JButton copyButton = new JButton();
	private JButton refreshButton = new JButton();
	private ListOpsController myController;

	/**
	 * Constructor.
	 *
	 * @ctl the parent panel.
	 */
	public ListOpsPanel(ListOpsController ctl)
	{
		myController = ctl;

		try
		{
			jbInit();
		}
		catch (Exception ex)
		{
			GuiHelpers.logGuiComponentInit(log, ex);
		}
	}

	/** GUI component initialization. */
	private void jbInit() throws Exception
	{
		openButton.setText(genericLabels.getString("open"));
		openButton.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				openButton_actionPerformed(e);
			}
		});
		setLayout(new GridBagLayout());
		newButton.setText(genericLabels.getString("new"));
		newButton.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				newButton_actionPerformed(e);
			}
		});
		deleteButton.setText(genericLabels.getString("delete"));
		deleteButton.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				deleteButton_actionPerformed(e);
			}
		});
		copyButton.setText(genericLabels.getString("copy"));
		copyButton.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				copyButton_actionPerformed(e);
			}
		});
		refreshButton.setText(genericLabels.getString("refresh"));
		refreshButton.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				helpButton_actionPerformed(e);
			}
		});
		this.setMinimumSize(new Dimension(571, 50));
		this.setPreferredSize(new Dimension(571, 50));
		this.add(deleteButton, new GridBagConstraints(3, 0, 1, 1, 1.0, 0.0,
			GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
			new Insets(10, 4, 10, 4), 0, 0));
		this.add(openButton, new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0,
			GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
			new Insets(10, 4, 10, 4), 0, 0));
		this.add(newButton, new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0,
			GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
			new Insets(10, 4, 10, 4), 0, 0));
		this.add(copyButton, new GridBagConstraints(2, 0, 1, 1, 1.0, 0.0,
			GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
			new Insets(10, 4, 10, 4), 0, 0));
		this.add(refreshButton, new GridBagConstraints(5, 0, 1, 1, 1.5, 0.0,
			GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(10, 4,
				10, 4), 18, 0));
		refreshButton.setEnabled(false);
	}

	/**
	 * Call with true if the 'Copy' button should be enabled.
	 *
	 * @param yn
	 *            true if the 'Copy' button should be enabled.
	 */
	public void enableCopy(boolean yn)
	{
		copyButton.setEnabled(yn);
	}

	/**
	 * Called when user presses the 'Open' Button.
	 *
	 * @param e
	 *            ignored.
	 */
	void openButton_actionPerformed(ActionEvent e)
	{
		myController.openPressed();
	}

	/**
	 * Called when user presses the 'New' Button.
	 *
	 * @param e
	 *            ignored.
	 */
	void newButton_actionPerformed(ActionEvent e)
	{
		myController.newPressed();
	}

	/**
	 * Called when user presses the 'Copy' Button.
	 *
	 * @param e
	 *            ignored.
	 */
	void copyButton_actionPerformed(ActionEvent e)
	{
		myController.copyPressed();
	}

	/**
	 * Called when user presses the 'Delete' Button.
	 *
	 * @param e
	 *            ignored.
	 */
	void deleteButton_actionPerformed(ActionEvent e)
	{
		myController.deletePressed();
	}

	/**
	 * Called when user presses the 'Help' Button.
	 *
	 * @param e
	 *            ignored.
	 */
	void helpButton_actionPerformed(ActionEvent e)
	{
		myController.refreshPressed();
	}
}
