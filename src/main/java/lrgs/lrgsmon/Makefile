#
#  $Id$
#
#  $Log$
#  Revision 1.1  2008/04/04 18:21:15  cvs
#  Added legacy code to repository
#
#  Revision 1.4  2004/06/04 17:32:16  mjmaloney
#  Completed LRGSmon application.
#
#  Revision 1.3  2004/06/02 21:31:51  mjmaloney
#  dev
#
#  Revision 1.2  2004/06/01 15:26:36  mjmaloney
#  Created.
#
#  Revision 1.1  2004/05/04 14:20:21  mjmaloney
#  Created - all functional code commented out for now.
#
#

all: classes
	@echo upto date!!

include ${SRCROOT}/Makefile.Common
include ${SRCROOT}/Makefile.DependRules

#
# Java package prefix and source files:
#
JPKGPREFIX=lrgs.ldds.
JSRC = \
	LrgsMonitor.java \
	LrgsMonThread.java \
	LrgsMonCmdLineArgs.java \
	ThreadBase.java \
	DetailReportGenerator.java \
	SummaryReportGenerator.java \


classes: ${JCLASS}
	@echo finished making ${JCLASS}

install:

# DO NOT DELETE THIS LINE
