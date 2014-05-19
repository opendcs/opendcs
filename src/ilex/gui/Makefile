#
#  $Id$
#
#  $Source$
#
#  $State$
#
#  $Log$
#  Revision 1.1  2008/04/04 18:21:09  cvs
#  Added legacy code to repository
#
#  Revision 1.11  2004/11/16 21:01:59  mjmaloney
#  Created.
#
#  Revision 1.10  2004/11/10 22:28:59  mjmaloney
#  dev
#
#  Revision 1.9  2004/11/10 16:27:34  mjmaloney
#  dev
#
#  Revision 1.8  2000/06/07 20:49:43  mike
#  Added missing classes.
#
#  Revision 1.7  2000/05/19 20:38:13  mike
#  Compile SingleClickButton
#
#  Revision 1.6  2000/04/04 19:46:11  mike
#  Added ExtensionFileFilter
#
#  Revision 1.5  2000/03/29 21:42:19  mike
#  dev
#
#  Revision 1.4  2000/03/29 21:41:52  mike
#  Added Editor & EditorMenuSet
#
#  Revision 1.3  2000/03/28 16:46:24  mike
#  Added LoginDialog
#
#  Revision 1.2  2000/03/25 22:07:24  mike
#  Added gui stuff.
#
#  Revision 1.1  2000/01/26 18:32:40  mike
#  Added makefile support for ilex.gui package.
#
#

JSRC = FrameDisplayer.java DynamicLabel.java EditPropsAction.java \
	GuiApp.java MenuFrame.java LoginDialog.java \
	Editor.java EditorMenuSet.java ExtensionFileFilter.java \
	SingleClickButton.java AllFiles_FileFilter.java \
	BackgroundCorbaUpdateThread.java FrameDisplayer.java \
	FileDownloadDialog.java MultFileDownloadDialog.java \
	EventsPanel.java EventsPanelQueueThread.java

JCLASS = ${JSRC:.java=.class}

all: ${JCLASS}

include ${ILEXSRCROOT}/Makefile.Common

depends:
	touch make.depend

install:

