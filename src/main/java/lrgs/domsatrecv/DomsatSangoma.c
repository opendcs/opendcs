/*
*  $Id$
*
* Copyright Sutron Corporation - All Rights Reserved.
* No part of this file may be duplicated in either hard-copy or electronic
* form without specific written permission.
* 
*  $Log$
*  Revision 1.5  2010/05/04 15:25:24  mjmaloney
*  Added hasCRC check. A142 boards don't include CRC.
*
*  Revision 1.4  2010/01/20 12:50:17  mjmaloney
*  Added fix for Sangoma 5.3 drivers, which now have 64 byte header.
*  The new native module will detect the correct version and adjust
*  accordingly.
*
*  Revision 1.3  2008/11/29 21:08:02  mjmaloney
*  merge with opensrc
*
*  Revision 1.2  2008/06/10 21:39:52  cvs
*  dev
*
*  Revision 1.1  2008/04/04 18:21:12  cvs
*  Added legacy code to repository
*
*  Revision 1.7  2005/12/21 14:17:06  mmaloney
*  LRGS 5.5
*
*  Revision 1.6  2005/12/20 19:58:11  mmaloney
*  5.5 release prep.
*
*  Revision 1.5  2005/09/11 21:40:31  mjmaloney
*  dev
*
*  Revision 1.4  2005/08/21 14:18:03  mjmaloney
*  LRGS 5.1
*
*  Revision 1.3  2005/08/17 22:08:01  mjmaloney
*  dev
*
*  Revision 1.2  2005/08/08 18:09:41  mjmaloney
*  HW Interface complete.
*
*  Revision 1.1  2005/08/08 14:46:32  mjmaloney
*  Created.
*
*/

/*
This file contains the native methods for the DomsatSangoma interface.
*/

#include <string.h>
#include <unistd.h>
#include <errno.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include "DomsatSangoma.h"
#include "sangoma_drv.h"

static int wanpipe_proto;
static char *r_name = "wanpipe1";
static char *i_name = "wp1_chdlc";
static char *i_name_alt = "wp1chdlc";
static char *i_name_alt2 = "w1g1";
static int sock = -1;
static char errbuf[256];
/*static char cbuf[MAX_FRAME_SIZE + sizeof(api_rx_hdr_t) + 2]; */
/* MJM For driver v3.5, api_rx_hdr is now 64 bytes (used to be 16) */
static char cbuf[MAX_FRAME_SIZE + 64 + 2];
static int hasCRC = 1;


JNIEXPORT void JNICALL Java_lrgs_domsatrecv_DomsatSangoma_setInterfaceName
  (JNIEnv *env, jclass cls, jstring ifName)
{
	const char *ncp = (*env)->GetStringUTFChars(env, ifName, NULL);
	i_name = strdup(ncp);
	(*env)->ReleaseStringUTFChars(env, ifName, ncp);
}

/**
Open & bind socket to Sangoma board driver.
*/
JNIEXPORT jint JNICALL Java_lrgs_domsatrecv_DomsatSangoma_initSangoma
  (JNIEnv *env, jclass cls)
{
	/* Determine what kernel version this is so I'll know what the protocol
	 * number to use:
	 *   Before 2.4: AF_WANPIPE=24
	 *   2.4 & later: AF_WANPIPE=25
	*/
	char kversion[20];
	FILE *fp;
	int k1, k2, k3, n;

	fp = fopen("/proc/version", "r");
	if (fp == NULL)
	{
		strcpy(errbuf,"Can't open /proc/version!");
		return -1;
	}
	kversion[0] = '\0';
	fscanf(fp,"%*s %*s %s", kversion);
	fclose(fp);

	n = sscanf(kversion, "%d.%d.%d", &k1, &k2, &k3);
	if (n < 3)
	{
		strcpy(errbuf, "Can't parse kernel version from /proc/version!!\n");
		return -1;
	}

	/* Linux 2.4 & later uses protocol # 25 */
	if (k1 > 2 || (k1 == 2 && k2 >= 4))
		wanpipe_proto = 25;
	else
		wanpipe_proto = 24;

/*printf("Sangoma-init: Linux Kernel %d.%d.%d, using protocol # %d",
k1, k2, k3, wanpipe_proto);
*/

	return 0;
}

/*
Enable the interface. 
*/
JNIEXPORT jint JNICALL Java_lrgs_domsatrecv_DomsatSangoma_enable
  (JNIEnv *env, jclass cls)
{
	struct wan_sockaddr_ll 	sa;

	memset(&sa,0,sizeof(struct wan_sockaddr_ll));
   	sock = socket(wanpipe_proto, SOCK_RAW, 0);
   	if( sock < 0 ) 
	{
		sprintf(errbuf, "Cannot create AF_WANPIPE socket, protocol %d",
			wanpipe_proto);
		return -1;
   	}
  
/*printf("Connecting to router %s, interface %s", r_name, i_name);
*/

	strcpy(sa.sll_device, i_name);
	strcpy(sa.sll_card, r_name);
	sa.sll_protocol = htons(PVC_PROT);
	sa.sll_family = wanpipe_proto;

	if(bind(sock, (struct sockaddr *)&sa, sizeof(struct wan_sockaddr_ll)) < 0)
	{
		sprintf(errbuf, 
			"Failed to bind socket for router %s, interface %s, protocol %d", 
			r_name, i_name, wanpipe_proto);
		strcpy(sa.sll_device, i_name_alt);
		if(bind(sock, (struct sockaddr *)&sa, sizeof(struct wan_sockaddr_ll)) 
			< 0)
		{
			strcpy(sa.sll_device, i_name_alt2);
			if(bind(sock, (struct sockaddr *)&sa, 
				sizeof(struct wan_sockaddr_ll)) < 0)
			{
				sprintf(errbuf, 
"Failed to bind socket for router %s, interfaces %s, %s, or %s, protocol %d", 
					r_name, i_name, i_name_alt, i_name_alt2, wanpipe_proto);
				return -1;
			}

			/* A142 does not include CRC at end of packet */
			hasCRC = 0;
		}
	}

	return 0;
}

/*
Disable the interface. 
*/
JNIEXPORT jint JNICALL Java_lrgs_domsatrecv_DomsatSangoma_disable
  (JNIEnv *env, jclass cls)
{
	if (sock != -1)
		close(sock);
	sock = -1;
	return 0;
}

/**
Read a packet from the board.
Return length on success, 0 on timeout (no data available currently),
       -1 on a recoverable error, -2 on non-recoverable error
*/
JNIEXPORT jint JNICALL Java_lrgs_domsatrecv_DomsatSangoma_readPacket
  (JNIEnv *env, jclass cls, jbyteArray jbuf)
{
	fd_set readfds;
	struct timeval tv;
	int r;
	int pktlen;
	api_rx_hdr_t *hdrp;
	unsigned char *pbuf;
	int hdr_size = sizeof(api_rx_hdr_t);

	if (sock == -1)
	{
		strcpy(errbuf, "Socket not open.");
		return -2;
	}

	FD_ZERO(&readfds);
	FD_SET(sock, &readfds);

	tv.tv_sec = (long)5;
	tv.tv_usec = 0L;

	r = select(sock+1, &readfds, NULL, NULL, &tv);
	if (r < 0)
	{
		sprintf(errbuf, "select error, errno=%d", errno);
		return -2;
	}
	else if (r == 0)
	{
		/* timeout */
		return 0;
	}
	else if (!FD_ISSET(sock, &readfds))
	{
		strcpy(errbuf, "Data detected on wrong file descriptor -- ignored");
		return -1;
	}

	r = recv(sock, cbuf, sizeof(cbuf), 0);
	if (r < 0)
	{
		sprintf(errbuf, "Error %d from recv()", r);
		return -1;
	}

	/* Sangoma 3.5 driver has 64 byte header.  Earlier versions had 16.
	 * Determine which this is and set hdr size appropriately.
	 * The new driver will null fill bytes 16 ... 63.
	 * Look for nulls in the position that my start pattern should be.
	 * HDLC Framing should always be 01 00 10 00 (hex).
	 */
	if (r > 64
	 && (int)cbuf[16] == 0x00 && (int)cbuf[17] == 0x00
	 && (int)cbuf[18] == 0x00 && (int)cbuf[19] == 0x00)
		hdr_size = 64;

	// don't count wanpipe header or CRC
//	if ((pktlen = r - sizeof(api_rx_hdr_t) - 2) <= 0)
	pktlen = r - hdr_size;
	if (hasCRC)
		pktlen -= 2;

	if (pktlen <= 0)
	{
		sprintf(errbuf, 
			"Short (%d byte inc header+crc) frame received (hdr=%d) -- ignored",
			r, hdr_size);
		return 0;
	}

	/* check for errors in the error flag */
	hdrp = (api_rx_hdr_t*)cbuf;
	if (hdrp->error_flag != (unsigned char)0)
	{
		sprintf(errbuf, "Bad frame received, error code=0x%02X -- skipped", 
			hdrp->error_flag);
		return 0;
	}

//	pbuf = cbuf+sizeof(api_rx_hdr_t);
	pbuf = cbuf+hdr_size;

/*
{ 
printf(
"native: len=%d  hdr=%d %02X %02X %02X %02X (48) %02X %02X %02X %02X ... %02X %02X\n",
pktlen,hdr_size,
(int)pbuf[0], (int)pbuf[1], (int)pbuf[2], (int)pbuf[3],
(int)pbuf[48], (int)pbuf[49], (int)pbuf[50], (int)pbuf[51],
(int)pbuf[pktlen-2], (int)pbuf[pktlen-1]);
}
*/
	// Else we have a good frame to process...
	(*env)->SetByteArrayRegion(env, jbuf, 0, pktlen, pbuf);
	return pktlen;
}

/**
Close the socket.
*/
JNIEXPORT void JNICALL Java_lrgs_domsatrecv_DomsatSangoma_closeSangoma
  (JNIEnv *env, jclass cls)
{
	if (sock != -1)
		close(sock);
	sock = -1;
}

/*
Place the last error message in the passed buffer.
*/
JNIEXPORT void JNICALL Java_lrgs_domsatrecv_DomsatSangoma_getErrorMsg
  (JNIEnv *env, jclass cls, jbyteArray buf)
{
	(*env)->SetByteArrayRegion(env, buf, (jsize)0, strlen(errbuf)+1, errbuf);
}
