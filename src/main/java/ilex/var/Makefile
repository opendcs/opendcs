#
#  $Id$
#
#  $State$
#
#  $Log$
#  Revision 1.1  2008/04/04 18:21:10  cvs
#  Added legacy code to repository
#
#  Revision 1.2  2001/09/18 00:46:50  mike
#  Working implementation of DateDelegate
#
#  Revision 1.1  2001/05/19 20:05:30  mike
#  Added Makefile and TimedVariable.java
#

JSRC = BadArgumentException.java BadIndexException.java \
	BooleanDelegate.java ByteDelegate.java DelegateVariable.java \
	DoubleDelegate.java IFlags.java IVariable.java LongDelegate.java \
	NVarTest.java NamedVariable.java NamedVariableList.java \
	NoConversionException.java NotAnArrayException.java StringDelegate.java \
	VarTest.java Variable.java VariableException.java VariableType.java \
	TimedVariable.java CharDelegate.java DateDelegate.java 

JCLASS = ${JSRC:.java=.class}

all: ${JCLASS}

include ${ILEXSRCROOT}/Makefile.Common

depends:
	touch make.depend

install:

