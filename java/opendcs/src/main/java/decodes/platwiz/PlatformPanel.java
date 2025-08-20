/*
*	$Id$
*
*	$Log$
*	Revision 1.1  2008/04/04 18:21:03  cvs
*	Added legacy code to repository
*	
*	Revision 1.6  2008/01/25 18:56:10  mmaloney
*	modified files for internationalization
*	
*	Revision 1.5  2008/01/25 16:16:29  mmaloney
*	modified files for internationalization
*	
*	Revision 1.4  2007/09/11 13:25:04  mmaloney
*	Fixed bug
*	
*	Revision 1.3  2004/09/08 12:24:21  mjmaloney
*	javadoc
*	
*	Revision 1.2  2004/08/27 18:40:58  mjmaloney
*	Platwiz work
*	
*	Revision 1.1  2004/08/25 19:29:20  mjmaloney
*	Added
*	
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
public class PlatformPanel extends JPanel 
	implements WizardPanel
{
	private static ResourceBundle genericLabels = 
							PlatformWizard.getGenericLabels();
	private static ResourceBundle platwizLabels = 
							PlatformWizard.getPlatwizLabels();
	BorderLayout borderLayout1 = new BorderLayout();
	PlatformEditPanel platformEditPanel;

	/** Constructs new platform panel */
	public PlatformPanel() 
	{
		platformEditPanel = new PlatformEditPanel(false);
		try {
			jbInit();
		}
		catch(Exception ex) {
			ex.printStackTrace();
		}
	}

	/** Initializes GUI components. */
	void jbInit() throws Exception {
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
