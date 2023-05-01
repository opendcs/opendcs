#
#  $Id$
#
#  $Source$
#
#  $State$
#
#  $Log$
#  Revision 1.1  2008/04/04 18:21:13  cvs
#  Added legacy code to repository
#
#  Revision 1.16  2004/07/06 18:42:49  mjmaloney
#  dev
#
#  Revision 1.15  2004/07/05 19:10:43  mjmaloney
#  DECODES 6.2 release prep
#
#  Revision 1.14  2003/12/14 22:25:59  mjmaloney
#  Initial working editor with file IO but no CORBA.
#
#  Revision 1.13  2003/06/09 15:44:38  mjmaloney
#  Prep for LRGS 3.4 release.
#
#  Revision 1.12  2002/05/06 20:45:05  mjmaloney
#  Created NewUserDialog and added it to UserAccounts.
#
#  Revision 1.11  2002/04/30 22:12:17  mjmaloney
#  Added JComboBox for selected properties in MessageBrowser.
#
#  Revision 1.10  2001/03/06 15:12:29  mike
#  *** empty log message ***
#
#  Revision 1.9  2001/02/23 03:04:32  mike
#  *** empty log message ***
#
#  Revision 1.8  2000/06/07 18:49:40  mike
#  Added PopupDisplayer
#
#  Revision 1.7  2000/06/07 18:47:04  mike
#  Added ConfigHandler.java
#
#  Revision 1.6  2000/05/02 15:43:21  mike
#  Make wider to accomodate low-res screens.
#
#  Revision 1.5  2000/04/04 19:44:17  mike
#  Obsolete
#
#  Revision 1.4  2000/03/30 01:48:00  mike
#  dev
#
#  Revision 1.3  2000/03/27 22:18:06  mike
#  Added new GUI classes.
#
#  Revision 1.2  2000/03/25 22:08:40  mike
#  Added gui stuff.
#
#  Revision 1.1  2000/01/26 22:45:23  mike
#  Created
#
#

all: classes
	@echo upto date!!

include ${SRCROOT}/Makefile.Common
include ${SRCROOT}/Makefile.DependRules

#
# Java package prefix and source files:
#
JPKGPREFIX=lrgs.gui.
JSRC = RealTimeStatus.java SearchCriteriaEditor.java \
	MessageBrowser.java \
	LrgsSystemFinder.java UrlLrgsSystemFinder.java \
	DirectoryLrgsSystemFinder.java GuiClientAppSettings.java \
	PopupDisplayer.java LrgsGuiException.java \
	EventDisplay.java \
	LrgsSelectDialog.java LrgsServices.java \
	LrgsControl.java UserAccounts.java MessageOutput.java \
	ControlPanel.java LrgsAccess.java LrgsHostSelector.java \
	GeneralProperties.java LrgsAdmin.java LrgsApp.java \
	DcpMsgOutputMonitor.java DcpMsgOutputThread.java ConfigHandler.java \
	DecodesInterface.java NewUserDialog.java SearchCritEditorParent.java \
	LrgsConfigEditFrame.java NetlistDialog.java NBSiteDialog.java \
	NetlistFinder.java


classes: ${JCLASS}
	@echo finished making ${JCLASS}

install:

# DO NOT DELETE THIS LINE
