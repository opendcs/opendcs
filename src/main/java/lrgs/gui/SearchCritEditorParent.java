/*
*  $Id$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:21:14  cvs
*  Added legacy code to repository
*
*  Revision 1.2  2004/08/31 21:08:42  mjmaloney
*  javadoc
*
*  Revision 1.1  2003/06/09 18:57:44  mjmaloney
*  Created.
*
*/
package lrgs.gui;

/**
The SearchCriteriaEditor frame can be started from different places in the
GUI hierarchy. It calls the closing method defined here to let its parent
know that the editor has exited.
*/
public interface SearchCritEditorParent
{
	/** Let parent know we're closing. */
	public void closingSearchCritEditor();
}
