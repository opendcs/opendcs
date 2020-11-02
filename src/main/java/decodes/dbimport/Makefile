#
#  $Id$
#
#  $State$
#
#  $Log$
#  Revision 1.1  2008/04/04 18:21:01  cvs
#  Added legacy code to repository
#
#  Revision 1.5  2002/12/08 20:21:01  mjmaloney
#  Updates
#
#  Revision 1.4  2002/05/07 21:10:39  mjmaloney
#  Created LrgsNetlist utility. Also see scripts/nl2lrgs.
#
#  Revision 1.3  2001/11/24 18:28:57  mike
#  First working dbimport.
#
#  Revision 1.2  2001/11/23 19:15:42  mike
#  Working PlatformExport utility.
#
#  Revision 1.1  2001/06/24 22:01:29  mike
#  created.
#
#

JSRC = EmitImport.java \
	PlatformExport.java \
	DbImport.java \
	LrgsNetlist.java \
	DbExport.java

JCLASS = ${JSRC:.java=.class}

all: ${JCLASS}

include ${DECODESSRCROOT}/Makefile.Common

depends:
	touch make.depend

install:

