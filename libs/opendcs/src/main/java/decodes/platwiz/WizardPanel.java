package decodes.platwiz;

import java.awt.*;
import javax.swing.*;

public interface WizardPanel 
{
	/** Called once at start-up. */
	public void initialize()
		throws PanelException;

	/**
	* A short (couple word) title to be displayed at the top of the panel.
	*/
	public String getPanelTitle();

	/**
	* A 3-line description area is at the top of each panel. This method should
	* return a description of the current panel.
	* @return string
	*/
	public String getDescription();


	/**
	* In some contexts, certain panels should be skipped. By default this
	* method returns false. Implementation should override & check current
	* status, & return a value if appropriate.
	* @return boolean
	*/
	public boolean shouldSkip();

	/**
	* Called just prior to making this panel visible.
	* Implementation should read info in the object & populate fields in the
	* panel.
	*/
	public void activate()
		throws PanelException;

	/**
	* Called just prior to making this panel invisible.
	* Implementation should get info the controls and populate the db
	* objects.
	* @return true if OK to deactivate & move-on.
	*/
	public boolean deactivate()
		throws PanelException;

	/** Called just prior to application shutting down. */
	public void shutdown();
}
