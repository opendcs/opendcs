#
#  $Id$
#
#  $State$
#
#  $Log$
#  Revision 1.1  2008/04/04 18:21:01  cvs
#  Added legacy code to repository
#
#  Revision 1.15  2004/01/05 01:49:02  mjmaloney
#  Debug.
#
#  Revision 1.14  2003/11/29 17:40:38  mjmaloney
#  Several modifications to the way time-stamping is done. These were necessary
#  to support EDL files. Time stamping is now done all in one pass. All time/date
#  operators are now supported.
#
#  Revision 1.13  2003/11/15 20:24:45  mjmaloney
#  Move FormatTokenizer to top for faster compile.
#
#  Revision 1.12  2001/09/19 01:11:55  mike
#  dev
#
#  Revision 1.11  2001/08/24 19:44:40  mike
#  Moved PM stuff to datasource
#
#  Revision 1.10  2001/08/13 01:30:00  mike
#  dev
#
#  Revision 1.9  2001/07/01 18:41:14  mike
#  Moved DataSource stuff to separate 'datasource' package
#
#  Revision 1.8  2001/06/05 15:17:21  mike
#  dev
#
#  Revision 1.7  2001/05/30 23:52:38  mike
#  First compiled version of FieldOperation
#
#  Revision 1.6  2001/05/21 18:01:27  mike
#  dev
#
#  Revision 1.5  2001/05/21 13:38:50  mike
#  dev
#
#  Revision 1.4  2001/05/06 22:53:03  mike
#  dev
#
#  Revision 1.3  2001/05/05 17:46:33  mike
#  dev
#
#  Revision 1.2  2001/05/04 21:16:12  mike
#  dev
#
#  Revision 1.1  2001/04/19 10:10:00  mike
#  dev
#
#

JSRC = \
	FormatStatementTokenizer.java \
	CheckOperation.java \
	DataOperations.java \
	DecodedMessage.java \
	DecoderException.java \
	DecodesOperationGroup.java \
	DecodesOperation.java \
	EndOfDataException.java \
	FieldOperation.java \
	FieldParseException.java \
	FormatSelectOperation.java \
	NumberParser.java \
	PositionOperation.java \
	RecordedTime.java \
	ScanOperation.java \
	ScriptException.java \
	ScriptFormatException.java \
	Sensor.java \
	SkipCharactersOperation.java \
	SkipLinesOperation.java \
	SwitchFormatException.java \
	TimeSeries.java \
	TimeTruncateOperation.java \
	RecordedTimeStamp.java \
	RecordedTimeStampTest.java \
	WhiteSpaceSkipOperation.java 

JCLASS = ${JSRC:.java=.class}

all: ${JCLASS}

include ${DECODESSRCROOT}/Makefile.Common

depends:
	touch make.depend

install:

