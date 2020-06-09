#
#  $Id$
#
#  $Log$
#  Revision 1.1  2008/04/04 18:21:13  cvs
#  Added legacy code to repository
#
#  Revision 1.6  2003/06/18 11:40:30  mjmaloney
#  3.4 release prep
#
#  Revision 1.5  2003/06/17 17:14:23  mjmaloney
#  This file becomes a generic template to be installed.
#
#  Revision 1.4  2003/06/17 16:48:25  mjmaloney
#  3.4 release prep
#
#  Revision 1.3  2003/04/11 20:14:42  mjmaloney
#  dev
#
#  Revision 1.2  2003/04/09 19:38:03  mjmaloney
#  impl
#
#  Revision 1.1  2003/03/27 16:58:50  mjmaloney
#  drgs development.
#
#

all: classes 
	@echo upto date!!

include ${SRCROOT}/Makefile.Common
include ${SRCROOT}/Makefile.DependRules

#
# Definitions local to this directory:
#
LOCAL_DEFINES =
LOCAL_INCLUDES= -I${JDKDIR}/include -I${JDKDIR}/include/linux -I${SRCROOT}/java
LOCAL_C_OPTIONS = 
LOCAL_CPP_OPTIONS =


#
# Java package prefix and source files:
#
JPKGPREFIX=lrgs.drgs.
JSRC = \
	DrgsConnectCfg.java \
	DrgsEvent.java \
	BadConfigException.java \
	DrgsInputSettings.java \
	DrgsCmdLineArgs.java \
	DrgsInput.java \
	DrgsConnection.java \
	DrgsMsgThread.java \
	DrgsEvtThread.java

classes: ${JCLASS}
	@echo finished making ${JCLASS}

#
# Must have root permission to install!
#
install: all
	cp drgsconf.xml $(LRGSHOME)


# DO NOT DELETE THIS LINE
