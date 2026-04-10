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
package decodes.platwiz;

import java.awt.*;
import java.util.ResourceBundle;

import javax.swing.*;

import decodes.db.Platform;
import decodes.dbeditor.PlatformEditPanel;

/** 
Platform Wizard panel for platform info. 
This is a thin layer around the decodes.dbedit.PlatformEditPanel.
*/
public class PlatformPanel extends JPanel implements WizardPanel
{
	private static ResourceBundle platwizLabels = 
							PlatformWizard.getPlatwizLabels();
	BorderLayout borderLayout1 = new BorderLayout();
	PlatformEditPanel platformEditPanel;

	/** Constructs new platform panel */
	public PlatformPanel() 
	{
		platformEditPanel = new PlatformEditPanel(false);
		jbInit();
	}

	/** Initializes GUI components. */
	void jbInit() 
	{
		this.setLayout(borderLayout1);
		this.add(platformEditPanel, BorderLayout.CENTER);
	}

	// ------- From Wizard Panel Interface -------

	/** Called once at start-up. */
	public void initialize()
		throws PanelException
	{
	}

	/** @return title of this panel */
	public String getPanelTitle()
	{
		return platwizLabels.getString("PlatformPanel.title");
	}

	/** @return description of this panel */
	public String getDescription()
	{
		return platwizLabels.getString("PlatformPanel.description");
	}

	/** @return false -- never skip this panel. */
	public boolean shouldSkip() { return false; }

	/** Called when this panel becomes the active one. */
	public void activate()
		throws PanelException
	{
		Platform p = PlatformWizard.instance().getPlatform();
		platformEditPanel.setPlatform(p);
	}

	/** 
	  Called when this panel is deactivated.
	  @return true if information is valid.
	*/
	public boolean deactivate()
		throws PanelException
	{
		Platform p = platformEditPanel.getDataFromFields();
		PlatformWizard.instance().setPlatform(p);
		return true;
	}

	/** Called when application exits. */
	public void shutdown()
	{
	}
}
