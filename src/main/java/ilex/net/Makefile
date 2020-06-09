#
#  $Id$
#
#  $Source$
#
#  $State$
#
#  $Log$
#  Revision 1.1  2008/04/04 18:21:09  cvs
#  Added legacy code to repository
#
#  Revision 1.8  2004/03/05 14:17:10  mjmaloney
#  Added TestServer
#
#  Revision 1.7  2003/09/18 14:24:52  mjmaloney
#  Created Usbr test server to test the USBR client in the lrgs.lddc package.
#
#  Revision 1.6  2002/12/08 20:22:10  mjmaloney
#  Added DumpClient
#
#  Revision 1.5  2002/10/11 01:27:31  mjmaloney
#  Created TestFileServer
#
#  Revision 1.4  2000/01/23 19:30:01  mike
#  Added BasicClient
#
#  Revision 1.3  1999/12/01 19:42:49  mike
#  Implemented java install stuff.
#
#  Revision 1.2  1999/09/30 18:16:44  mike
#  9/30/1999
#
#  Revision 1.1  1999/09/24 20:18:44  mike
#  9/24/1999
#
#

JSRC = BasicServer.java BasicSvrThread.java BasicClient.java \
	TestFileServer.java TestFileSvrThread.java DumpClient.java \
	TestUsbrServer.java TestUsbrSvrThread.java \
	TestServer.java TestServerThread.java

JCLASS = ${JSRC:.java=.class}

all: ${JCLASS}

include ${ILEXSRCROOT}/Makefile.Common

depends:
	touch make.depend

install:

