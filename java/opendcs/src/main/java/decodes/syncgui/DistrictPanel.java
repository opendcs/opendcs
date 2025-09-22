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
package decodes.syncgui;

import java.awt.*;
import javax.swing.*;

import org.opendcs.gui.GuiHelpers;
import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

/**
Panel shown in right side of GUI when a district is selected.
*/
public class DistrictPanel extends JPanel
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	BorderLayout borderLayout1 = new BorderLayout();
	JPanel jPanel1 = new JPanel();
	FlowLayout flowLayout1 = new FlowLayout();
	JLabel jLabel1 = new JLabel();
	JTextField districtNameField = new JTextField();
	JPanel jPanel2 = new JPanel();
	JPanel jPanel3 = new JPanel();
	JLabel jLabel2 = new JLabel();
	JLabel jLabel3 = new JLabel();
	JLabel jLabel4 = new JLabel();
	JTextField hostField = new JTextField();
	JTextField userField = new JTextField();
	JTextField directoryField = new JTextField();
	GridBagLayout gridBagLayout1 = new GridBagLayout();
	GridBagLayout gridBagLayout2 = new GridBagLayout();

	District district = null;

	public DistrictPanel()
	{
		try
		{
			jbInit();
		}
		catch (Exception ex)
		{
			GuiHelpers.logGuiComponentInit(log, ex);
		}
	}

	void jbInit() throws Exception {
		this.setLayout(borderLayout1);
		jPanel1.setLayout(flowLayout1);
		jLabel1.setText("District: ");
		districtNameField.setPreferredSize(new Dimension(102, 23));
		districtNameField.setEditable(false);
		districtNameField.setText("MVM");
		jPanel2.setLayout(gridBagLayout2);
		jPanel3.setLayout(gridBagLayout1);
		jLabel2.setText("Host:");
		jLabel3.setText("User:");
		jLabel4.setText("Directory:");
		hostField.setEditable(false);
		hostField.setText("decodes.mvm.usace.army.mil");
		userField.setEnabled(false);
		userField.setText("decodes");
		directoryField.setEditable(false);
		directoryField.setText("/home/decodes/DECODES/edit-db");
		this.add(jPanel1, BorderLayout.NORTH);
		jPanel1.add(jLabel1, null);
		jPanel1.add(districtNameField, null);
		this.add(jPanel2, BorderLayout.CENTER);
		jPanel2.add(jPanel3,	new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0
						,GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(3, 2, 0, 3), 87, 17));
		jPanel3.add(jLabel2,		new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
						,GridBagConstraints.SOUTHEAST, GridBagConstraints.NONE, new Insets(12, 15, 4, 2), 0, 0));
		jPanel3.add(hostField,		new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0
						,GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(12, 0, 4, 50), 0, 0));
		jPanel3.add(jLabel3,		new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0
						,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(4, 15, 4, 2), 0, 0));
		jPanel3.add(jLabel4,		new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0
						,GridBagConstraints.NORTHEAST, GridBagConstraints.NONE, new Insets(4, 30, 10, 2), 0, 0));
		jPanel3.add(userField,	 new GridBagConstraints(1, 1, 1, 1, 1.0, 0.0
						,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(4, 0, 4, 120), 0, 0));
		jPanel3.add(directoryField,	 new GridBagConstraints(1, 2, 1, 1, 1.0, 0.0
						,GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, new Insets(4, 0, 10, 50), 0, 0));
	}

	/**
	  Sets the district to be shown in this panel.
	*/
	public void setDistrict(District dist)
	{
		this.district = dist;
		districtNameField.setText(district.getName());
		hostField.setText(district.getHost());
		userField.setText(district.getUser());
		directoryField.setText(district.getDesc());
	}
}
