#
#  $Id$
#
#  $Source$
#
#  $State$
#
#  $Log$
#  Revision 1.1  2008/04/04 18:21:12  cvs
#  Added legacy code to repository
#
#  Revision 1.10  2005/03/07 21:35:25  mjmaloney
#  dev
#
#  Revision 1.9  2003/04/05 17:57:22  mjmaloney
#  Added DcpMsgFlag
#
#  Revision 1.8  2000/03/31 16:10:26  mike
#  Added LrgsErrorCode
#
#  Revision 1.7  2000/03/19 22:02:08  mike
#  Created ConfigEntry and ConfigSet classes.
#
#  Revision 1.6  2000/01/07 23:05:05  mike
#  Generic ldds client interface
#
#  Revision 1.5  1999/12/01 19:42:59  mike
#  Implemented java install stuff.
#
#  Revision 1.4  1999/10/15 10:12:54  mike
#  Moved LddsInputStream to common from package lrgs.ldds. Clients will need
#  to use this class so it should be in the common package.
#
#  Revision 1.3  1999/10/15 10:05:01  mike
#  Moved to common from package ldds. Clients will need this class too.
#
#  Revision 1.2  1999/09/27 20:17:39  mike
#  9/27/1999
#
#  Revision 1.1  1999/09/24 20:18:45  mike
#  9/24/1999
#
#

JSRC = DcpAddress.java DcpNameMapper.java NetlistReader.java \
	NetworkList.java NetworkListItem.java SearchCriteria.java \
	SearchCritTest.java DcpMsgIndex.java DcpMsg.java DcpMsgSource.java \
	DcpMsgRetriever.java ConfigEntry.java ConfigSet.java LrgsErrorCode.java \
	DcpMsgFlag.java \
	ArchiveException.java \
	ArchiveUnavailableException.java \
	NoSuchMessageException.java \
	SearchSyntaxException.java \
	SearchTimeoutException.java \
	EndOfArchiveException.java \
	UntilReachedException.java

JCLASS = ${JSRC:.java=.class}

all: ${JCLASS}

include ${SRCROOT}/Makefile.Common

install:

depends:

