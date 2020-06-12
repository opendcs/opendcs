#
#  $Id$
#
#  $Log$
#  Revision 1.1  2008/04/04 18:21:16  cvs
#  Added legacy code to repository
#
#  Revision 1.1  2004/05/04 18:03:57  mjmaloney
#  Moved from statusgui package to here.
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
	TopLevelXio.java \
	StatusXmlTags.java \
	LrgsStatusSnapshotExt.java \
	LrgsStatusSnapshotXio.java \
	ArchiveStatisticsXio.java \
	ProcessXio.java \
	DownLinkXio.java \
	QualityMeasurementXio.java

classes: ${JCLASS}
	@echo finished making ${JCLASS}

install:

# DO NOT DELETE THIS LINE
