/*
*  $Id$
*
*  This is open-source software written by ILEX Engineering, Inc., under
*  contract to the federal government. 
*  Except for specific contractual terms between ILEX and the federal 
*  government, this source code is provided completely without warranty.
*  For more information contact: info@ilexeng.com
*/
package lrgs.gui;

import java.awt.Dimension;
import java.awt.Point;

import lrgs.common.SearchCriteria;

public interface SearchCriteriaEditorIF
{
	/** Call with true to save when editor closes */
	public void setAutoSave(boolean tf);

	/** Called by parent component */
	public void setParent(SearchCritEditorParent parent);

	public void setSize(Dimension d);

	public void movedTo(Point p);

	/** Launches the editor */
	public void startup(int x, int y);

	public void cleanupBeforeExit();

	/** Fills the search crit with the contents of the GUI controls */
	public void fillSearchCrit(SearchCriteria searchcrit);

	public SearchCriteria getCurrenCriteria();

	public boolean isChanged();

	public void toFront();
}

