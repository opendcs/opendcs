#!/bin/sh
OUTPUT=$1

# -m32 builds for 32-bit architecture
#GCCFLAGS=-m32

if [ -z "$OUTPUT" ]; then
    echo "usage: build.sh outputfile"
    exit 1;
fi

cmd='javac DomsatSangoma.java'
echo $cmd
$cmd

cmd="javah -force -jni -o DomsatSangoma.h -classpath ../.. lrgs.domsatrecv.DomsatSangoma"
echo $cmd
$cmd

cmd="gcc $GCCFLAGS -DLINUX   -g -DLINUXPC -Wall -I/native -I/native  -fPIC -I/usr/java/latest/include -I/usr/java/latest/include/linux -I/java  -c DomsatSangoma.c"
echo $cmd
$cmd

cmd="gcc $GCCFLAGS -shared -Wl,-soname,libdomsat.so.6  -L/native/ilexutil -L/native/lrgscommon -L/native/lrgsintern  -o $OUTPUT DomsatSangoma.o -lc"
echo $cmd
$cmd
