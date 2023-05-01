#
#  $Id$
#
#  $Source$
#
#  $State$
#
#  $Log$
#  Revision 1.1  2008/04/04 18:21:10  cvs
#  Added legacy code to repository
#
#  Revision 1.42  2005/12/22 15:22:16  mmaloney
#  Added DesEncrypter
#
#  Revision 1.41  2004/11/10 16:27:10  mjmaloney
#  Added unzip capability
#
#  Revision 1.40  2004/08/30 15:35:16  mjmaloney
#  Renamed BadIndexException to IndexRangeException because of class with ilex.
#  var class.
#
#  Revision 1.39  2004/08/26 14:14:37  mjmaloney
#  Added FileUtil
#
#  Revision 1.38  2004/07/07 14:26:22  mjmaloney
#  Created.
#
#  Revision 1.37  2004/05/06 20:57:10  mjmaloney
#  Implemented QueueLogger to be used by servers that export events.
#
#  Revision 1.36  2004/04/23 19:28:01  mjmaloney
#  Created CaptureStdout
#
#  Revision 1.35  2004/04/02 18:58:16  mjmaloney
#  Created.
#
#  Revision 1.34  2004/03/31 14:28:04  mjmaloney
#  Added new test.
#
#  Revision 1.33  2004/01/27 15:28:48  mjmaloney
#  dev
#
#  Revision 1.32  2003/12/20 00:32:50  mjmaloney
#  Implemented TimeoutInputStream.
#
#  Revision 1.31  2003/09/02 14:37:28  mjmaloney
#  Added TeeLogger. Added more control on msg format to Logger.
#  Added TextUtil.fixedLengthFields method.
#
#  Revision 1.30  2003/05/16 20:12:38  mjmaloney
#  Added EnvExpander. This is preferrable to ShellExpander because
#  it is platform independent.
#
#  Revision 1.29  2003/05/12 12:36:46  mjmaloney
#  Added FailureException & FatalException.
#  All three IlexException sub-classes modified to log messages.
#
#  Revision 1.28  2003/05/08 23:20:15  mjmaloney
#  Added Base64 based on apache source.
#
#  Revision 1.27  2003/03/27 21:17:55  mjmaloney
#  drgs dev
#
#  Revision 1.26  2002/08/26 17:47:32  chris
#  SQL Database I/O development
#
#  Revision 1.25  2001/10/25 14:33:03  mike
#  Added PropertiesUtil
#
#  Revision 1.24  2001/09/24 20:43:04  mike
#  Created FileLogger
#
#  Revision 1.23  2001/07/25 13:15:27  mike
#  dev
#
#  Revision 1.22  2001/06/13 02:01:41  mike
#  dev
#
#  Revision 1.21  2001/05/06 17:48:11  mike
#  Created ArgumentTokenizer
#
#  Revision 1.20  2001/04/13 19:38:41  mike
#  Added logger classes.
#
#  Revision 1.19  2001/01/24 02:17:14  mike
#  Added ErrorException and WarningException for File parsers.
#
#  Revision 1.18  2000/12/18 02:59:30  mike
#  dev
#
#  Revision 1.17  2000/12/15 23:27:56  mike
#  created StringPair
#
#  Revision 1.16  2000/08/31 00:00:02  mike
#  *** empty log message ***
#
#  Revision 1.15  2000/08/30 17:21:04  mike
#  Created ServerLock.java
#
#  Revision 1.14  2000/03/31 16:11:36  mike
#  Created AsciiUtil.java
#
#  Revision 1.13  2000/03/25 22:07:26  mike
#  Added gui stuff.
#
#  Revision 1.12  2000/03/17 22:44:56  mike
#  dev
#
#  Revision 1.11  2000/03/16 14:23:50  mike
#  Added TTYEcho to prompt user for password without echoing.
#
#  Revision 1.10  2000/03/13 15:35:01  mike
#  PasswordFileEditor complete.
#
#  Revision 1.9  2000/03/12 22:41:34  mike
#  Added PasswordFile & PasswordFileEntry classes.
#
#  Revision 1.8  2000/01/07 23:04:51  mike
#  Created
#
#  Revision 1.7  1999/12/01 19:42:53  mike
#  Implemented java install stuff.
#
#  Revision 1.6  1999/11/18 17:04:57  mike
#  Added ShellExpander class to expand tilde & shell variables.
#
#  Revision 1.5  1999/10/20 21:10:04  mike
#  Initial implementation
#
#  Revision 1.4  1999/09/23 12:33:41  mike
#  Initial implementation
#
#  Revision 1.3  1999/09/21 08:50:17  mike
#  Renamed CmdLineProg to CmdLineProcessor. This is more accurate - There's
#  no reason to require inheritence. A single prog may have many
#  CmdLineProcessors.
#
#  Revision 1.2  1999/09/16 14:52:43  mike
#  9/16/1999
#
#  Revision 1.1  1999/09/15 17:10:54  mike
#  *** empty log message ***
#
#

JSRC = FileException.java FileExceptionList.java FileParser.java \
	IDateFormat.java CmdLine.java CmdLineList.java CmdLineProcessor.java \
	ArrayUtil.java TwoDigitYear.java ByteUtil.java ShellExpander.java \
	TextUtil.java PasswordFileEntry.java PasswordFile.java \
	PasswordFileEditor.java TTYEcho.java FileDigest.java RunQueue.java \
	AsciiUtil.java ServerLock.java LockWrapper.java StringPair.java \
	IlexException.java \
	ErrorException.java \
	WarningException.java \
	FailureException.java \
	FatalException.java \
	Logger.java \
	StderrLogger.java \
	NullLogger.java \
	FileLogger.java \
	ArgumentTokenizer.java \
	Counter.java \
	FileCounter.java \
	PropertiesUtil.java \
    Pair.java \
	ServerLockable.java \
	Base64.java \
	EnvExpander.java \
	TeeLogger.java \
	TimeoutInputStream.java \
	ThreadLogger.java \
	FileLoggerTest.java \
	DirectoryMonitorThread.java \
	TestDirectoryMonitor.java \
	CaptureStdout.java \
	IndexRangeException.java \
	QueueLogger.java \
	ProcWaiterThread.java \
	FileUtil.java \
	ZipMonitor.java \
	DesEncrypter.java

JCLASS = ${JSRC:.java=.class}

all: ${JCLASS}

include ${ILEXSRCROOT}/Makefile.Common

depends:
	touch make.depend

install:

