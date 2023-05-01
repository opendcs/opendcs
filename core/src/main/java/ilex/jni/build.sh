#!/bin/sh
OUTPUT=$1

#
# -m32 builds a 32-bit model regardless of the local architecture.
#
GCCFLAGS=-m32

if [ -z "$OUTPUT" ]; then
	echo "usage: build.sh outputfile"
	exit 1;
fi

cmd='javac -cp ../..:../../../../src SignalTrapper.java SignalHandler.java Debug.java'
echo $cmd
$cmd

cmd='javah -force -jni -o SignalTrapper.h -classpath ../.. ilex.jni.SignalTrapper'
echo $cmd
$cmd

cmd='javah -force -jni -o Debug.h -classpath ../.. ilex.jni.Debug'
echo $cmd
$cmd

cmd="gcc $GCCFLAGS -DLINUX -g -DLINUXPC -Wall -I/native -I/native -fPIC -I/usr/java/jdk/include -I/usr/java/jdk/include/linux -I/java -c SignalTrapper.c"
echo $cmd
$cmd

cmd="gcc $GCCFLAGS -DLINUX -g -DLINUXPC -Wall -I/native -I/native -fPIC -I/usr/java/jdk/include -I/usr/java/jdk/include/linux -I/java -c Debug.c"
echo $cmd
$cmd

cmd="gcc $GCCFLAGS -shared -Wl,-soname,libdomsat.so.6  -L/native/ilexutil -L/native/lrgscommon -L/native/lrgsintern  -o $OUTPUT SignalTrapper.o Debug.o -lc"
echo $cmd
$cmd
