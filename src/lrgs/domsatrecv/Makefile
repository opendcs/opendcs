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
LIB_BASE_NAME=libdomsat.so
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
LOCAL_JFLAGS=-cp ../..:../../../../src

all: classes headers ${LIB_BASE_NAME}
	@echo '${LIB_BASE_NAME} upto date!!'

JSRC=DomsatSangoma.java 
# DomsatFranklin.java

include ${PROPTOP}/dcstool/legacy/lrgs/Makefile.Common
include ${PROPTOP}/dcstool/legacy/lrgs/Makefile.DependRules

#
# Definitions local to this directory:
#
LOCAL_DEFINES =
LOCAL_INCLUDES= -I${JDKDIR}/include -I${JDKDIR}/include/linux -I${SRCROOT}/java
LOCAL_C_OPTIONS= -fPIC
LOCAL_CPP_OPTIONS= -fPIC
#LOCAL_LDFLAGS = -L${SRCROOT}/java/lrgs/internal

#
# Java package prefix and source files:
#
JPKGPREFIX=lrgs.domsatrecv.

classes: $(JCLASS)
	@echo made ${JCLASS}

GENHEADERS=DomsatSangoma.h

headers: $(GENHEADERS)

#
# Source files
#
CPPFILES= 
CFILES=DomsatSangoma.c 
# DomsatFranklin.c icp188.c

#
# Dependency rules are included after source files
#
#include ${SRCROOT}/Makefile.DependRules


#
# Library rules:
#
${LIB_BASE_NAME}: ${ALL_OBJECTS}
	@echo '---------------------------------------------------'
	@echo 'Creating/Updating Library '$@'...'
	${AR} ${ARFLAGS} ${LDFLAGS} -o $@ $(ALL_OBJECTS) -lc

DomsatSangoma.o: DomsatSangoma.c DomsatSangoma.h

# DomsatFranklin.o: DomsatFranklin.c DomsatFranklin.h


#
# Rules for building test executables
#


# DO NOT DELETE THIS LINE
