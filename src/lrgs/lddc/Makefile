#
#  $Id$
#
#  $Source$
#
#  $State$
#
#  $Log$
#  Revision 1.1  2008/04/04 18:21:14  cvs
#  Added legacy code to repository
#
#  Revision 1.6  2003/09/18 14:22:50  mjmaloney
#  Created.
#
#  Revision 1.5  2002/11/20 22:28:14  mjmaloney
#  Added RtSocketStream utility.
#
#  Revision 1.4  2000/03/31 16:09:32  mike
#  Created GetDcpMessages
#
#  Revision 1.3  2000/01/19 14:34:49  mike
#  Debug messages to detect garbage collection.
#
#  Revision 1.2  1999/12/01 19:43:02  mike
#  Implemented java install stuff.
#
#  Revision 1.1  1999/10/13 21:29:33  mike
#  Initial implementation
#
#

all: classes
	@echo upto date!!

include ${SRCROOT}/Makefile.Common
include ${SRCROOT}/Makefile.DependRules

#
# Java package prefix and source files:
#
JPKGPREFIX=lrgs.lddc.
JSRC = \
	Client.java \
	BgClient.java \
	GetDcpMessages.java \
	RtSocketStream.java \
	UsbrDrotFeed.java

classes: ${JCLASS}
	@echo finished making ${JCLASS}

install:

# DO NOT DELETE THIS LINE
