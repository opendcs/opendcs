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
*  Revision 1.3  2004/09/22 23:00:23  mjmaloney
*  Solaris port, Release 4.0 preparation
*
*  Revision 1.2  2003/03/27 21:17:55  mjmaloney
*  drgs dev
*
*  Revision 1.1  1999/11/18 17:05:29  mike
*  Initial implementation.
*
*/
#include <signal.h>

#include "SignalTrapper.h"

#define HIGHEST_SIG 128
static int sigseen[HIGHEST_SIG];

void
signalHandler(int sig)
{
	/* printf("Received signal %d, status was %d\n", sig, sigseen[sig]); */
	if (sig >= 0 && sig < HIGHEST_SIG)
		sigseen[sig] = 1;
}


/*
 * Class:     ilex_jni_SignalTrapper
 * Method:    trapSignal
 * Signature: (IZ)V
 */
JNIEXPORT void JNICALL Java_ilex_jni_SignalTrapper_trapSignal
  (JNIEnv *env, jclass cls, jint sig, jboolean yes_no)
{
	if (yes_no)
		signal((int)sig, signalHandler);
	else
		signal((int)sig, SIG_DFL);
}


/*
 * Class:     ilex_jni_SignalTrapper
 * Method:    wasSignalSeen
 * Signature: (I)Z
 */
JNIEXPORT jboolean JNICALL Java_ilex_jni_SignalTrapper_wasSignalSeen
  (JNIEnv *env, jclass cls, jint sig)
{
	return (sigseen[(int)sig] != 0) ? JNI_TRUE : JNI_FALSE;
}


JNIEXPORT void JNICALL Java_ilex_jni_SignalTrapper_resetSignal
  (JNIEnv *env, jclass cls, jint sig)
{
	sigseen[(int)sig] = 0;
}
