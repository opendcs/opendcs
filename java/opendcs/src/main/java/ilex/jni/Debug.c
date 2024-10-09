/*
*  $Id$
*
*  $Source$
*
*  $State$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:21:09  cvs
*  Added legacy code to repository
*
*  Revision 1.1  2000/01/21 13:27:47  mike
*  Created
*
*
*/
#include "Debug.h"

#include <stdio.h>
#include <fcntl.h>
#include <unistd.h>

/*
 * Class:     ilex_jni_Debug
 * Method:    checkFD
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_ilex_jni_Debug_checkFD
  (JNIEnv *env, jclass cls)
{
	char buf[80];
	int fd = open("/tmp/checkfd",O_WRONLY|O_CREAT,0666);
	if (fd == -1)
		return -1;
	sprintf(buf,"checkfd: %d\n", fd);
	write(fd, buf, strlen(buf));
	close(fd);
	return fd;
}
