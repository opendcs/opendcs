#
#  $Id$
#

# IMPORTANT IMPORTANT IMPORTANT IMPORTANT IMPORTANT IMPORTANT IMPORTANT 
#
#   DO NOT BUILD THE NATIVE LIBRARY ON A LINUX 64-BIT SYSTEM OR IT WON'T
#   WORK WHEN DISTRIBUTED TO A 32-BIT SYSTEM!!!!!
# !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

#
# Define library base name and version/release numbers:
#
LIB_BASE_NAME=libilexjni.so
LIB_MAJOR_VER=6
LIB_MINOR_VER=0
LIB_RELEASE=0

LIB_SO_NAME=$(LIB_BASE_NAME).$(LIB_MAJOR_VER)
TARGETLIB=$(LIB_SO_NAME).$(LIB_MINOR_VER).$(LIB_RELEASE)

#
# list of executable programs:
#
EXECUTABLES=

TARGETS= ${TARGETLIB} 

all: classes headers ${TARGETS}
	@echo '${TARGETS} upto date!!'

include ${PROPTOP}/dcstool/legacy/ilex/Makefile.Common
include ${PROPTOP}/dcstool/legacy/lrgs/Makefile.DependRules

#
# Definitions local to this directory:
#
LOCAL_DEFINES = -L.
LOCAL_INCLUDES= -I${JDKDIR}/include -I${JDKDIR}/include/linux -I${SRCROOT}/java
LOCAL_C_OPTIONS = -fPIC
LOCAL_CPP_OPTIONS =-fPIC

#
# Java package prefix and source files:
#
JPKGPREFIX=ilex.jni.
JSRC = SignalTrapper.java SignalHandler.java Debug.java

#JSRC = SignalTrapper.java SignalHandler.java Debug.java \
#	EventMsg.java EventShmem.java EventContext.java

#JCLASS = ${JSRC:.java=.class}

classes: $(JCLASS)
	@echo made ${JCLASS}

GENHEADERS=SignalTrapper.h Debug.h

#GENHEADERS=SignalTrapper.h Debug.h EventShmem.h EventContext.h

headers: $(GENHEADERS)

#
# Source files
#
CPPFILES= 
CFILES= SignalTrapper.c Debug.c 

#CFILES= SignalTrapper.c Debug.c EventShmem.c EventContext.c

#
# Dependency rules are included after source files
#
#include ${SRCROOT}/Makefile.DependRules


#
# Library rules:
#
${TARGETLIB}: ${ALL_OBJECTS}
	@echo '---------------------------------------------------'
	@echo 'Creating/Updating Library '$@'...'
	${AR} ${LDFLAGS} ${ARFLAGS} -o $@ $(ALL_OBJECTS) -lc
	@echo 'Making local link for build...'
	if [ -f ./${LIB_BASE_NAME} -o -L ./${LIB_BASE_NAME} ] ; then \
 		rm ./${LIB_BASE_NAME}; \
	fi
	ln -s $@ ./${LIB_BASE_NAME}

#	${AR} ${LDFLAGS} ${ARFLAGS} -o $@ $(ALL_OBJECTS) -lilexutil -lc

#
# You must have root permissions to run install. This will need
# to be done once every time any of the version numbers change.
#
install: ${TARGETLIB}
	-rm /usr/lib/${LIB_SO_NAME} /usr/lib/${LIB_BASE_NAME}
	(cd /usr/lib; \
	ln -sf ${TARGETLIB} ${LIB_SO_NAME} ; \
	ln -sf ${TARGETLIB} ${LIB_BASE_NAME} ;)
	${LDCONFIG}

SignalTrapper.o: SignalTrapper.c SignalTrapper.h 

Debug.o: Debug.c Debug.h

EventShmem.o: EventShmem.c EventShmem.h

EventContext.o: EventContext.c EventContext.h

depends:
	touch make.depend

# DO NOT DELETE THIS LINE
