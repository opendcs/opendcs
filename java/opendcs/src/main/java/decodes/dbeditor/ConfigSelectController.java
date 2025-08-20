/*
*  $Id$
*
*  $State$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:21:00  cvs
*  Added legacy code to repository
*
*  Revision 1.3  2004/09/19 20:08:51  mjmaloney
*  javadocs added. Removed unused classes.
*
*  Revision 1.2  2003/08/01 21:05:43  mjmaloney
*  dev
*
*  Revision 1.1  2001/05/03 14:15:55  mike
*  dev
*
*/
package decodes.dbeditor;

/**
Any class that creates a ConfigSelectDialog implements this interface.
This provides a 'call-back' method called from the dialog when a selection
has been made.
*/
public interface ConfigSelectController
{
	/** 
	  Called from the dialog when a selection has been made.
	  @param config the selected configuration.
	*/
	public void selectConfig(decodes.db.PlatformConfig config);
}

