/**
 * 
 */
package lrgs.rtstat;

import lrgs.lrgsmain.LrgsConfig;

/**
 * Interface for extra custom config panels.
 */
public interface LrgsConfigPanel
{
	/** The label to use in the tabbed pane. */
	public String getLabel();
	
	/** Fill controls with values from the passed configuration */
	public void fillFields(LrgsConfig cfg);
	
	/** @return true if anything was changed on this panel. */
	public boolean hasChanged();
	
	/** Save changes back to the config object. */
	public void saveChanges();
}
