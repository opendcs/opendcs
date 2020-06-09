/*
*  $Id$
*
*  This is open-source software written by ILEX Engineering, Inc., under
*  contract to the federal government. You are free to copy and use this
*  source code for your own purposes, except that no part of the information
*  contained in this file may be claimed to be proprietary.
*
*  Except for specific contractual terms between ILEX and the federal 
*  government, this source code is provided completely without warranty.
*  For more information contact: info@ilexeng.com
*/

/*
This file contains the native methods for the DomsatFranklin interface.
*/

#include <stdio.h>
#include <stdlib.h>
#include <fcntl.h>
#include <string.h>
#include <unistd.h>
#include <errno.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <lrgs/domsatrecv/DomsatFranklin.h>
#include <sys/ioctl.h>
#include <time.h>
#include <sys/param.h>

#include <icp188_driver/icp188drv.h>
#include <icp188_driver/icp188ioctl.h>
#include <icp188_driver/icp188mem.h>
                                                                                
static char errbuf[256];
#define MSGS_PER_READ (256*7)        /* max # that can be returned in 1 read */
static struct ICPREADREQ icpReadReq; /* ICP Read Request structure */
static int icpReadReqIndex;  /* Current msg being processed in read request */
static int icpReadReqCount;  /* # of messages returned by last call to read() */static int icp188fd;                 /* File descriptor of ICP device driver */
//static struct READ_TIMER_STRUCT icp188rts = { TIMER_ALWAYS_DELAY, HZ/3 };
struct READ_TIMER_STRUCT icp188rts;
static char icpprogname[25];
static char mypktbuf[16000];

int icp188load(int fd, const char *fname);
int icp188start(int fd);
extern char icperrbuf[];
int icp188readNewMessages(void);

/**
Open & bind socket to Franklin board driver.
*/
JNIEXPORT jint JNICALL Java_lrgs_domsatrecv_DomsatSangoma_initFranklin
  (JNIEnv *env, jclass cls)
{
    /* Allocate receive data buffer and pointer table.  Set up the
     * request structure used for issuing read calls.
     */
	icpReadReq.icpr_dsize = MSGS_PER_READ * 256;
	icpReadReq.icpr_offset = (long *)malloc(MSGS_PER_READ * sizeof(long));
	icpReadReq.icpr_data = (unsigned char *)malloc(icpReadReq.icpr_dsize);
	icpReadReq.icpr_status = 0;
	icpReadReq.icpr_nmsg = 0;
	icpReadReq.icpr_nerr = 0;
	icpReadReq.icpr_lastmsg = 0;
	icpReadReqIndex = 0;
	icpReadReqCount = 0;

    /* Open the DOMSAT device specified in the configuration area. */
	if ((icp188fd = open("/dev/icp", O_RDWR)) == -1)
	{
		strcpy(errbuf, "Cannot open ICP188 node '/dev/icp'");
		return -1;
	}
	icp188rts.rts_timermode = TIMER_ALWAYS_DELAY;
	icp188rts.rts_nticks = HZ/3;
	ioctl(icp188fd, READ_TIMER, &icp188rts);

	return 0;
}

/*
Enable the interface. 
*/
JNIEXPORT jint JNICALL Java_lrgs_domsatrecv_DomsatFranklin_enable
  (JNIEnv *env, jclass cls)
{
	icpReadReqIndex = 0;

    /* Download the ICP program. */
	if (icp188load(icp188fd, icpprogname) == -1)
	{
		sprintf(errbuf,"Cannot load '%s' (ICP Program): %s",
			icpprogname, icperrbuf);
		return -1;
	}

	if (ioctl(icp188fd, DRIVER_RESET, (char *)0) == -1)
	{
		strcpy(errbuf, "ICP Ioctl error - could not reset.");
		return -1;
	}

	if (ioctl(icp188fd, DRIVER_DCPMSG, (char *)0) == -1)
	{
		strcpy(errbuf, 
			"ICP Ioctl error - could change mode to DRIVER_DCPMSG.");
		return -1;
	}

	if (icp188start(icp188fd) == -1)
	{
		strcpy(errbuf, 
			"ICP error - could not start embedded program: ");
		strcat(errbuf, icperrbuf);
		return -1;
	}

	return 0;
}

/*
Disable the interface. 
*/
JNIEXPORT jint JNICALL Java_lrgs_domsatrecv_DomsatFranklin_disable
  (JNIEnv *env, jclass cls)
{
	icpReadReqIndex = icpReadReqCount = icpReadReq.icpr_nmsg = 0;
	return 0;
}

/**
Read a packet from the board.
Return length on success, 0 on timeout (no data available currently),
       -1 on a recoverable error, -2 on non-recoverable error
For Franklin board, a packet will always hold a complete message.
*/
JNIEXPORT jint JNICALL Java_lrgs_domsatrecv_DomsatFranklin_readPacket
  (JNIEnv *env, jclass cls, jbyteArray jbuf)
{
	/*
	* The icp read request structure acts as a cache. If it's empty
	* attempt to read the next block of messages.
	*/
	if (icpReadReqIndex >= icpReadReqCount)
	{
		if (icp188readNewMessages() == -1)
			return -1;
	}

	/* If messages present in cache, return next one. */
	if (icpReadReqIndex < icpReadReqCount)
	{
		int idx = icpReadReqIndex++;
		long offset = icpReadReq.icpr_offset[idx];
		struct ICP188_MSG_HEADER *mhp = 
			(struct ICP188_MSG_HEADER *)(icpReadReq.icpr_data + offset);
		struct DCPMSG_DATA *mdp = 
			(struct DCPMSG_DATA *)(icpReadReq.icpr_data + offset +sizeof(*mhp));

		/* validate bounds of message within buffer. */
		if (offset < 0 || offset > icpReadReq.icpr_dsize)
		{
			sprintf(errbuf,
				"ICP Bounding Error: offset=%ld, i=%d, n=%d",
					offset, idx, icpReadReqCount);
			icpReadReqIndex = icpReadReqCount = 0;
			return -1;
		}

		/* Treat msgs with errors just like missing msgs. */
		if (mhp->m_flag & 0xbf00)	/* ANY_ERROR */
		{
			sprintf(errbuf,
				"ICP Message Error: offset=%ld, i=%d, flag=0x%X",
					offset, idx, mhp->m_flag);
			return -1;
		}

		/* Do sanity checks on message size. */
		if (mhp->m_size > 15787)
		{
			sprintf(errbuf,"Bad message size (%d): Skipped", mhp->m_size);
			return -1;
		}
		if (idx < icpReadReqCount-1) // Not last msg in batch?
		{
			int k;	  // distance from this msg to next
			k = icpReadReq.icpr_offset[idx+1] 
			  - icpReadReq.icpr_offset[idx] 
			  - sizeof(*mhp)
			  - mhp->m_size;

			if (k < 0 || k > 1)
			{
				int should = icpReadReq.icpr_offset[idx+1] 
			  		- icpReadReq.icpr_offset[idx] 
			  		- sizeof(*mhp);

				sprintf(errbuf, 
				"Bad msg size: m_size=%d, should=%d, idx=%d, offset[%d]=%ld, "
					"offset[%d]=%ld, k=%d, nmsg=%d, %d msgs skipped",
				    mhp->m_size, should, idx, 
					idx, icpReadReq.icpr_offset[idx], 
					idx+1, icpReadReq.icpr_offset[idx+1], k, 
					icpReadReqCount, icpReadReqCount - idx);
				icpReadReqIndex = icpReadReqCount = 0;
				return -1;
			}
		}

		/*
		 * Need to make this entire message look like a single HDLC packet.
		 */
		mypktbuf[0] = 0;	 		/* don't care */
		mypktbuf[1] = 0;	 		/* don't care */
		mypktbuf[2] = 0;	 		/* don't care */
		mypktbuf[3] = 0;	 		/* don't care */
		mypktbuf[4] = (char)0;		/* clear the more bit at 0x10 */
		mypktbuf[5] = (char)((mhp->m_num >> 8) & 0xff);
		mypktbuf[6] = (char)(mhp->m_num & 0xff);
		mypktbuf[7] = (char)1;		/* 1st pkt of msg is always # 1 */
	
		memcpy(mypktbuf+8, mdp, mhp->m_size);
	
		(*env)->SetByteArrayRegion(env, jbuf, 0, mhp->m_size + 8, mypktbuf);
		return mhp->m_size + 8;
	}
	return 0;
}

/**
Close the socket.
*/
JNIEXPORT void JNICALL Java_lrgs_domsatrecv_DomsatFranklin_closeFranklin
  (JNIEnv *env, jclass cls)
{
	close(icp188fd);
}

/*
Place the last error message in the passed buffer.
*/
JNIEXPORT void JNICALL Java_lrgs_domsatrecv_DomsatFranklin_getErrorMsg
  (JNIEnv *env, jclass cls, jbyteArray buf)
{
	(*env)->SetByteArrayRegion(env, buf, (jsize)0, strlen(errbuf)+1, errbuf);
}


/* 
* Call ICP driver to get a batch of messages. (ICP will
* return 0 after timeout period if no messages are received.
*/
int icp188readNewMessages(void)
{
	icpReadReqIndex = icpReadReqCount = 0;
	icpReadReqCount = read(icp188fd, &icpReadReq, MSGS_PER_READ);
	if (icpReadReqCount < 0)
	{ 	
		strcpy(errbuf, "ICP188 read error");

		return -1;
	}
	else if (icpReadReqCount > 0)
	{
		return 0;
	}

	/* else icpReadReqCount == 0, check for other status indicators */

    /* 
	* Check for status information change from the ICP driver.  Send events
    * as appropriate.
    */
	if (icpReadReq.icpr_status & ERR_HOST_TOO_SLOW)
	{
		strcpy(errbuf, "ICP Error: Host too slow.");
		return -1;
	}

	if (icpReadReq.icpr_status & ERR_FIXED_OOP)
	{
   		strcpy(errbuf, "ICP Error: Fixed ICP out-of-phase.");
		return -1;
	}

	if (icpReadReq.icpr_status & ERR_OVERRUN)
	{
		strcpy(errbuf, "ICP Error: Receiver overrun.");
		return -1;
	}

	if (icpReadReq.icpr_status & ERR_CRC)
	{
	   strcpy(errbuf, "ICP Error: CRC error(s).");
		return -1;
	}

	return 0;
}

