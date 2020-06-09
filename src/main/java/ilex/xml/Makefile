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
#  Revision 1.10  2004/04/26 20:02:26  mjmaloney
#  Dev.
#
#  Revision 1.9  2003/11/11 02:44:50  mjmaloney
#  Added ElementIgnorer.
#
#  Revision 1.8  2003/03/27 13:52:01  mjmaloney
#  Created DomHelper
#
#  Revision 1.7  2003/03/26 20:16:37  mjmaloney
#  Added DomTest
#
#  Revision 1.6  2001/04/23 12:51:54  mike
#  Added LoggerErrorHandler.
#
#  Revision 1.5  2001/04/09 20:46:49  mike
#  Created StringBufferErrorHandler
#
#  Revision 1.4  2001/01/03 02:55:05  mike
#  dev
#
#  Revision 1.3  2000/12/31 15:56:56  mike
#  dev
#
#  Revision 1.2  2000/12/23 21:33:16  mike
#  Created XmlObjectWriter and XmlOutputStream
#
#  Revision 1.1  2000/12/21 21:30:11  mike
#  Created
#
#

JSRC = TaggedDoubleOwner.java TaggedDoubleSetter.java TaggedLongOwner.java \
	TaggedLongSetter.java TaggedStringOwner.java TaggedStringSetter.java \
	TaggedBooleanSetter.java TaggedBooleanOwner.java \
	XmlHierarchyParser.java XmlObjectParser.java \
	XmlObjectWriter.java XmlOutputStream.java PrintStreamErrorHandler.java \
	StringBufferErrorHandler.java LoggerErrorHandler.java \
	DomTest.java \
	DomHelper.java \
	ElementIgnorer.java \
	XmlUtils.java

JCLASS = ${JSRC:.java=.class}

all: ${JCLASS}

include ${ILEXSRCROOT}/Makefile.Common

depends:
	touch make.depend

install:

