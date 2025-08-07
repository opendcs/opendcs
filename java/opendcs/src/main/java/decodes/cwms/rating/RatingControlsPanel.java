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
package decodes.cwms.rating;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;

import javax.swing.JButton;
import javax.swing.JPanel;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

/**
Control panel at bottom of CWMS Rating GUI.
*/
public class RatingControlsPanel extends JPanel
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
    JButton refreshButton = new JButton("Refresh");
    JButton exportXmlButton = new JButton("Export XML");
    JButton deleteButton = new JButton("Delete");
    JButton importXmlButton = new JButton("Import XML");
    JButton searchUsgsButton = new JButton("Search USGS");
    RatingController myController;
    GridBagLayout gridBagLayout1 = new GridBagLayout();

    /**
      Constructor.
      @ctl the parent panel.
    */
	public RatingControlsPanel(RatingController ctl)
	{
		myController = ctl;
		try
		{
			jbInit();
		}
		catch (Exception ex)
		{
			log.atError().setCause(ex).log("Unable to initialize GUI elements.");
		}
	}

    /** GUI component initialization. */
	private void jbInit()
    	throws Exception
    {
		refreshButton.addActionListener(
			new java.awt.event.ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					myController.refreshPressed();
				}
			});

		exportXmlButton.addActionListener(
			new java.awt.event.ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					myController.exportXmlPressed();
				}
			});
	
		deleteButton.addActionListener(
			new java.awt.event.ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					myController.deletePressed();
				}
			});

		importXmlButton.addActionListener(
			new java.awt.event.ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					myController.importXmlPressed();
				}
			});
		
		searchUsgsButton.addActionListener(
			new java.awt.event.ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					myController.searchUsgsPressed();
				}
			});
		
		setLayout(gridBagLayout1);
		add(refreshButton,
			new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.NONE,
				new Insets(10, 10, 10, 10), 0, 0));
		add(exportXmlButton,
			new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.NONE,
				new Insets(10, 10, 10, 10), 0, 0));
		add(deleteButton,
			new GridBagConstraints(2, 0, 1, 1, 0.5, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(10, 10, 10, 10), 0, 0));

		add(importXmlButton,
			new GridBagConstraints(3, 0, 1, 1, 0.5, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(10, 10, 10, 10), 0, 0));
		add(searchUsgsButton,
			new GridBagConstraints(4, 0, 1, 1, 0.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.NONE,
				new Insets(10, 10, 10, 10), 0, 0));
	}
}
