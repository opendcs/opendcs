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
import java.util.ResourceBundle;

import org.opendcs.gui.GuiHelpers;
import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import ilex.util.LoadResourceBundle;

import decodes.gui.*;
import decodes.util.DecodesSettings;
import decodes.db.*;

/**
Displays a sorting-list of Platform objects in the database.
 */
public class PlatformListPanel extends JPanel implements ListOpsController
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	static ResourceBundle dbeditLabels =  
		LoadResourceBundle.getLabelDescriptions(
                "decodes/resources/dbedit",
				DecodesSettings.instance().language
			);

	ListOpsPanel listOpsPanel;
	JLabel jLabelTitle = new JLabel();
	PlatformSelectPanel platformSelectPanel;
	int newIndex = 1;

	/** Constructor. */
	public PlatformListPanel() {
		listOpsPanel = new ListOpsPanel(this);
		listOpsPanel.enableCopy(true);
		try 
		{
			jbInit();
		} catch (Exception ex) 
		{
			GuiHelpers.logGuiComponentInit(log, ex);
		}
	}

	/** Initializes GUI components. */
	private void jbInit() throws Exception 
	{
		platformSelectPanel = new PlatformSelectPanel(this::openPressed,null, null);
		this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		jLabelTitle.setHorizontalAlignment(SwingConstants.CENTER);
		jLabelTitle.setText(dbeditLabels.getString("PlatformListPanel.title"));
		this.add(jLabelTitle);
		this.add(platformSelectPanel, BorderLayout.CENTER); // The JTable
		this.add(listOpsPanel);
	}


	/** @return type of entity that this panel edits. */
	public String getEntityType() { return "Platform"; }

	/** Called when the 'Delete' button is pressed. */
	public void deletePressed() {
		Platform ob = platformSelectPanel.getSelectedPlatform();
		if (ob == null)
		{
			TopFrame.instance().showError(
					dbeditLabels.getString("PlatformListPanel.selectDelete"));
		}
	}

	/** Called when the 'Help' button is pressed. */
	public void refreshPressed()
	{
	}



	/**
	 * Called if a new platform is abandoned before it was ever saved.
	 * @param p the platform.
	 */
	void abandonNewPlatform(Platform p)
	{
		throw new UnsupportedOperationException("Unimplemented method 'abandonNewPlatform'");
	}

	@Override
	public void openPressed()
	{
		throw new UnsupportedOperationException("Unimplemented method 'openPressed'");
	}

	@Override
	public void newPressed()
	{
		throw new UnsupportedOperationException("Unimplemented method 'newPressed'");
	}

	@Override
	public void copyPressed() 
	{
		throw new UnsupportedOperationException("Unimplemented method 'copyPressed'");
	}
}
