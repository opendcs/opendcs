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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.util.ResourceBundle;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;

import org.opendcs.gui.GuiHelpers;
import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import decodes.db.RoutingSpec;
import decodes.gui.GuiDialog;

@SuppressWarnings("serial")
public class RoutingSpecSelectDialog extends GuiDialog
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	static ResourceBundle genericLabels = DbEditorFrame.getGenericLabels();
	static ResourceBundle dbeditLabels = DbEditorFrame.getDbeditLabels();

	private JButton selectButton = new JButton();
	private JButton cancelButton = new JButton();

	private JPanel listWrapperPanel = new JPanel(new BorderLayout());
	private TitledBorder titledBorder1;
	private RoutingSpecSelectPanel rsSelectPanel = new RoutingSpecSelectPanel();
	private RoutingSpec routingSpec = null;
	private boolean cancelled = false;

	public RoutingSpecSelectDialog(JFrame owner)
	{
		super(owner, "", true);
		rsSelectPanel.setParentDialog(this);
		init();
	}

	private void init()
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

	/** Initialize GUI components. */
	void jbInit() throws Exception
	{
		this.setModal(true);
		this.setTitle(dbeditLabels.getString("RoutingSpecListPanel.title"));
		titledBorder1 = new TitledBorder(BorderFactory.createLineBorder(
			new Color(153, 153, 153), 2),
			dbeditLabels.getString("RoutingSpecListPanel.title"));
		Border border1 = BorderFactory.createCompoundBorder(titledBorder1,
			BorderFactory.createEmptyBorder(5, 5, 5, 5));

		JPanel mainPanel = new JPanel(new BorderLayout());
		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
		selectButton.setText(genericLabels.getString("select"));
		selectButton.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				openPressed();
			}
		});
		cancelButton.setText(genericLabels.getString("cancel"));
		cancelButton.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				cancelPressed();
			}
		});
		buttonPanel.add(selectButton, null);
		buttonPanel.add(cancelButton, null);
		mainPanel.add(buttonPanel, BorderLayout.SOUTH);

		listWrapperPanel.setBorder(border1);
		getContentPane().add(mainPanel);
		mainPanel.add(listWrapperPanel, BorderLayout.CENTER);
		listWrapperPanel.add(rsSelectPanel, BorderLayout.CENTER);
	}

	/**
	 * Called when a double click on the selection
	 */
	void openPressed()
	{
		routingSpec = rsSelectPanel.getSelection();
		closeDlg();
	}

	/** Closes dialog. */
	void closeDlg()
	{
		setVisible(false);
		dispose();
	}

	/**
	 * Called when Cancel button is pressed.
	 */
	void cancelPressed()
	{
		routingSpec = null;
		closeDlg();
		cancelled = true;
	}

	/** @return selected (single) site, or null if Cancel was pressed. */
	public RoutingSpec getSelection()
	{
		return routingSpec;
	}

	public void clearSelection()
	{
		rsSelectPanel.clearSelection();
	}

	public boolean isCancelled()
	{
		return cancelled;
	}
}
