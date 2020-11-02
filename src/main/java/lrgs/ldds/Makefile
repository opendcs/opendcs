#
#  $Id$
#
#  $Source$
#
#  $State$
#
#  $Log$
#  Revision 1.1  2008/04/04 18:21:15  cvs
#  Added legacy code to repository
#
#  Revision 1.19  2005/10/11 17:55:19  mmaloney
#  dev
#
#  Revision 1.18  2005/10/10 20:49:41  mmaloney
#  dev
#
#  Revision 1.17  2005/01/05 19:21:09  mjmaloney
#  Bug fixes & updates.
#
#  Revision 1.16  2004/07/02 18:56:40  mjmaloney
#  Added events distribution capability.
#
#  Revision 1.15  2004/04/23 19:34:15  mjmaloney
#  Use CaptureStdout to redirect uncaught exceptions to the log.
#
#  Revision 1.14  2003/06/18 13:53:54  mjmaloney
#  Added LddsParams.java
#
#  Revision 1.13  2003/05/29 14:27:07  mjmaloney
#  Enhancements for 3.4
#
#  Revision 1.12  2003/05/03 16:48:08  mjmaloney
#  Added logging to LDDS server.
#
#  Revision 1.11  2003/02/05 14:32:31  mjmaloney
#  Added DdsConstants module.
#
#  Revision 1.10  2002/08/05 19:08:30  mjmaloney
#  First implementation of GetDcpMsgBlock
#
#  Revision 1.9  2002/06/18 23:11:04  mjmaloney
#  Added CmdAuthHello.java
#
#  Revision 1.8  2000/02/26 19:09:38  mike
#  Renamed Legacy to Lrgs
#
#  Revision 1.7  2000/01/08 21:53:28  mike
#  generic LddsClient interface
#
#  Revision 1.6  2000/01/07 23:03:09  mike
#  Generic Client Interface
#
#  Revision 1.5  2000/01/07 19:10:00  mike
#  Generalizing client interface
#
#  Revision 1.4  1999/12/01 19:43:03  mike
#  Implemented java install stuff.
#
#  Revision 1.3  1999/11/03 19:23:53  mike
#  Debugging network read problems.
#
#  Revision 1.2  1999/10/15 10:13:55  mike
#  Moved LddsInputStream and LddsMessage to lrgs.common package. Added import
#  statements in the 'Cmd' classes that need these.
#
#  Revision 1.1  1999/10/13 21:29:35  mike
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
JPKGPREFIX=lrgs.ldds.
JSRC = \
	LrgsDcpDataSvr.java LddsClient.java \
	LddsMessage.java LddsUser.java LddsCommand.java CmdHello.java \
	CmdEcho.java CmdGetNextDcpMsg.java CmdGoodbye.java CmdRecvSearchCrit.java \
	CmdSendSearchCrit.java LddsThread.java \
	CmdRecvNetList.java CmdSendNetList.java LddsInputStream.java \
	CmdAuthHello.java ServerError.java ProtocolError.java \
	CmdGetDcpMsgBlock.java LddsLoggerThread.java \
	LddsRequestException.java LddsParams.java \
	CmdGetStatus.java LddsLogForwarder.java CmdGetEvents.java \
	UnknownUserException.java NativeLddsThread.java \
	AuthFailedException.java \
	DdsInternalException.java \
	NoSuchFileException.java \
	StatLogger.java

classes: ${JCLASS}
	@echo finished making ${JCLASS}

install:

# DO NOT DELETE THIS LINE
