/*
*  $Id$
*
*  $Source$
*
*  $State$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:21:08  cvs
*  Added legacy code to repository
*
*  Revision 1.2  2004/08/30 14:50:18  mjmaloney
*  Javadocs
*
*  Revision 1.1  2000/03/29 21:37:52  mike
*  Created Editor and EditorMenuSet to support custom file editors like
*  the LRGS searchcrit and config-file editors.
*
*/
package ilex.gui;

/**
* This interface works with the suite of EditorMenu actions to provide
* action objects appropriate to a customized editor.
*/
public abstract interface Editor
{
	// File menu actions:

	/** Called when new pressed. */
	void newPress( );
	/** Called when open pressed. */
	void openPress( );
	/** Called when save pressed. */
	void savePress( );
	/** Called when new pressed. */
	void saveAsPress( );

	// Edit menu actions:

	/** Called when undo pressed. */
	void undoPress( );
	/** Called when cut pressed. */
	void cutPress( );
	/** Called when copy pressed. */
	void copyPress( );
	/** Called when paste pressed. */
	void pastePress( );
	/** Called when delete pressed. */
	void deletePress( );
}
