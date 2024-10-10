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
*  Revision 1.1  2000/03/21 17:27:15  mike
*  EventShmem added
*
*
*/

#include "EventShmem.h"
#include <ilexutil/event_shm.h>

/*
 * Class:     ilex_jni_EventShmem
 * Method:    attachNative
 * Signature: (I)J
 */
JNIEXPORT jlong JNICALL Java_ilex_jni_EventShmem_attachNative
  (JNIEnv *env, jclass cls, jint ipckey)
{
	struct EventContext *ecp = attachEventShmem((int)ipckey, "EventShmem");
	return (jlong)(unsigned long)ecp;
}

/*
 * Class:     ilex_jni_EventShmem
 * Method:    detachNative
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_ilex_jni_EventShmem_detachNative
  (JNIEnv *env, jclass cls, jlong ctx)
{
	struct EventContext *ecp = (struct EventContext *)(unsigned long)ctx;
	detachEventShmem(ecp);
}

