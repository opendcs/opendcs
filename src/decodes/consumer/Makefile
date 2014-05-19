#
#  $Id$
#
#  $State$
#
#  $Log$
#  Revision 1.1  2008/04/04 18:20:59  cvs
#  Added legacy code to repository
#
#  Revision 1.15  2005/04/25 21:38:08  mjmaloney
#  dev
#
#  Revision 1.14  2004/12/12 18:49:10  mjmaloney
#  Fixed NullPointer bug.
#
#  Revision 1.13  2004/04/27 19:27:48  mjmaloney
#  compile bug fix.
#
#  Revision 1.12  2004/02/19 01:46:42  mjmaloney
#  Added NullFormatter to this package. Used by DCP Monitor app.
#
#  Revision 1.11  2003/05/21 20:00:01  mjmaloney
#  Added TvaFormatter.
#
#  Revision 1.10  2002/10/31 18:53:52  mjmaloney
#  release prep
#
#  Revision 1.9  2002/10/29 02:19:09  mjmaloney
#  Implemented StdmsgFormatter
#
#  Revision 1.8  2001/11/21 18:08:26  mike
#  Created ShefitFormatter.java
#
#  Revision 1.7  2001/11/21 14:12:25  mike
#  Implemented DirectoryConsumer and FileConsumer.
#
#  Revision 1.6  2001/10/26 00:14:52  mike
#  Added StringBufferConsumer
#
#  Revision 1.5  2001/10/05 19:21:00  mike
#  Implemented ShefFormatter
#
#  Revision 1.4  2001/10/05 17:49:52  mike
#  Added HumanReadableFormatter
#
#  Revision 1.3  2001/10/05 01:12:26  mike
#  Created EmitOracleFormatter
#
#  Revision 1.2  2001/10/05 00:52:12  mike
#  Implemented EmitAsciiFormatter
#
#  Revision 1.1  2001/09/19 01:11:52  mike
#  dev
#
#

JSRC = \
	DataConsumer.java \
	DataConsumerException.java \
	DumpFormatter.java \
	OutputFormatter.java \
	OutputFormatterException.java \
	PipeConsumer.java \
	EmitAsciiFormatter.java \
	EmitOracleFormatter.java \
	HumanReadableFormatter.java \
	ShefFormatter.java \
	StringBufferConsumer.java \
	FileConsumer.java \
	DirectoryConsumer.java \
	ShefitFormatter.java \
	StdmsgFormatter.java \
	TransmitMonitorFormatter.java \
	TvaFormatter.java \
	NullFormatter.java \
	FileAppendConsumer.java \
	HydstraFormatter.java \
	HtmlFormatter.java


JCLASS = ${JSRC:.java=.class}

all: ${JCLASS}

include ${DECODESSRCROOT}/Makefile.Common

depends:
	touch make.depend

install:

