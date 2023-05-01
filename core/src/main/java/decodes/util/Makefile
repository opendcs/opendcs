#
#  $Id$
#
#  $Source$
#
#  $State$
#
#  $Log$
#  Revision 1.1  2008/04/04 18:21:07  cvs
#  Added legacy code to repository
#
#  Revision 1.8  2004/08/27 20:50:30  mjmaloney
#  javadocs
#
#  Revision 1.7  2002/06/24 03:41:51  chris
#  Initial changes in support of the SQL database functionality.
#
#  Revision 1.6  2001/09/19 01:11:57  mike
#  dev
#
#  Revision 1.5  2001/09/03 13:24:32  mike
#  Added TimeOfDay.java
#
#  Revision 1.4  2001/04/21 20:19:23  mike
#  Added read & write methods to all DatabaseObjects
#
#  Revision 1.3  2001/04/13 20:16:49  mike
#  Implemented first cut of DecodesSettings
#
#  Revision 1.2  2001/04/06 11:29:46  mike
#  dev
#
#  Revision 1.1  2000/12/21 14:31:28  mike
#  Created.
#
#

JSRC = DecodesVersion.java \
	DecodesException.java \
	SecurityException.java \
	DecodesSettings.java \
	CmdLineArgs.java \
	TimeOfDay.java \
	CmdLineArgs.java

JCLASS = ${JSRC:.java=.class}

all: ${JCLASS}

include ${DECODESSRCROOT}/Makefile.Common

depends:
	touch make.depend

install:

