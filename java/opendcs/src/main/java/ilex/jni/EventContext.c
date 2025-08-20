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
*  Revision 1.2  2000/03/31 16:12:21  mike
*  dev
*
*  Revision 1.1  2000/03/21 17:27:48  mike
*  created
*
*
*/
#include "EventContext.h"
#include <ilexutil/event_shm.h>

/*
 * Class:     ilex_jni_EventContext
 * Method:    freeNativeContext
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_ilex_jni_EventContext_freeNativeContext
  (JNIEnv *env, jclass cls, jlong ctx)
{
	struct EventContext *ecp = (struct EventContext *)(unsigned long)ctx;
	unregisterSenderBySlot(ecp->e_q, ecp->e_senderSlot);
	destructEventContext(ecp);
}

/*
 * Class:     ilex_jni_EventContext
 * Method:    newNativeContext
 * Signature: (JLjava/lang/String;)J
 */
JNIEXPORT jlong JNICALL Java_ilex_jni_EventContext_newNativeContext
  (JNIEnv *env, jclass cls, jlong defaultctx, jstring name)
{
	struct EventContext *ecp, *ret;
	const char *namecp;

	ecp = (struct EventContext *)(unsigned long)defaultctx;
	namecp = (*env)->GetStringUTFChars(env, name, NULL);
	ret = newEventContext(ecp->e_q, ecp->e_ipc_key, ecp->e_shm_id, 
		ecp->e_sem_id, namecp);
	(*env)->ReleaseStringUTFChars(env, name, namecp);
	return (jlong)(unsigned long)ret;
}


/*
 * Class:     ilex_jni_EventContext
 * Method:    nextNativeEvent
 * Signature: (JLilex/jni/EventMsg;)Z
 */
JNIEXPORT jboolean JNICALL Java_ilex_jni_EventContext_nextNativeEvent
  (JNIEnv *env, jclass cls, jlong ctx, jobject event)
{
	static int init;
	int len;
	const struct EventSender *esp;
	static jfieldID fid_priority, fid_time, fid_errno, fid_msgnum,
		fid_sender, fid_data;
	jstring str;
	jbyteArray bytes;
	struct EventContext *ecp = (struct EventContext *)(unsigned long)ctx;
	const struct EventMsg *emp = getNextEvent(ecp);

	if (emp == NULL)
		return JNI_FALSE;

	if (!init)
	{
		jclass evcls;

		init = 1;

		evcls = (*env)->GetObjectClass(env, event);

		fid_priority = (*env)->GetFieldID(env, evcls, "priority", "S");
		fid_time = (*env)->GetFieldID(env, evcls, "time", "I");
		fid_errno = (*env)->GetFieldID(env, evcls, "errno", "S");
		fid_msgnum = (*env)->GetFieldID(env, evcls, "msgnum", "I");
		fid_sender = (*env)->GetFieldID(env, evcls, "sender", 
			"Ljava/lang/String;");
		fid_data = (*env)->GetFieldID(env, evcls, "data", "[B");
	}
	(*env)->SetShortField(env, event, fid_priority, (jshort)emp->e_priority);
	(*env)->SetIntField(env, event, fid_time, (jint)emp->e_time);
	(*env)->SetShortField(env, event, fid_errno, (jshort)emp->e_errno);
	(*env)->SetIntField(env, event, fid_msgnum, (jint)emp->e_app_msgnum);

	esp = getEventSender(ecp, emp);
	str = (*env)->NewStringUTF(env, esp->es_name);
	(*env)->SetObjectField(env, event, fid_sender, str);

	len = emp->e_textLength;
	if (len > 0 && (emp->e_text[len-1]=='\n' || emp->e_text[len-1]=='\0'))
		len--;
	bytes = (*env)->NewByteArray(env, len);
	(*env)->SetByteArrayRegion(env, bytes, (jsize)0, (jsize)len,
		(jbyte *)emp->e_text);

/*
* printf("allocated new byte[] of length %d, should be len %d, pointer=0x%lx\n",
* (int)(*env)->GetArrayLength(env, bytes), emp->e_textLength,
* (long)bytes);
*/

	(*env)->SetObjectField(env, event, fid_data, bytes);

	return JNI_TRUE;
}

/*
 * Class:     ilex_jni_EventContext
 * Method:    setNativeBacklog
 * Signature: (JI)V
 */
JNIEXPORT void JNICALL Java_ilex_jni_EventContext_setNativeBacklog
  (JNIEnv *env, jclass cls, jlong ctx, jint n)
{
	struct EventContext *ecp = (struct EventContext *)(unsigned long)ctx;
	setBacklog(ecp, (int)n);
}
