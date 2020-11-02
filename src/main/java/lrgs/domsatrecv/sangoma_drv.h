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
*
*  $Log$
*  Revision 1.1  2008/04/04 18:21:13  cvs
*  Added legacy code to repository
*
*  Revision 1.1  2005/08/08 18:09:41  mjmaloney
*  HW Interface complete.
*
*/


/*
This file contains a minimal set of definitions necessary to receive packets
from the Sangoma driver. It is extracted from the include files provided
by Sangoma.
*/

struct wan_sockaddr_ll
{
	unsigned short	sll_family;
	unsigned short	sll_protocol;
	int		sll_ifindex;
	unsigned short	sll_hatype;
	unsigned char	sll_pkttype;
	unsigned char	sll_halen;
	unsigned char	sll_addr[8];
	unsigned char   sll_device[14];
	unsigned char 	sll_card[14];

	unsigned int	sll_active_ch;
	unsigned char	sll_prot;
	unsigned char	sll_prot_opt;
	unsigned short  sll_mult_cnt;
	unsigned char	sll_seven_bit_hdlc;
};

#define PVC_PROT 0x17

#ifndef	PACKED
#define	PACKED	__attribute__((packed))
#endif

typedef struct {
	unsigned char	error_flag	PACKED;
	unsigned short	time_stamp	PACKED;
	unsigned char	reserved[13]	PACKED;
} api_rx_hdr_t;

/* MUST agree with size of packetbuf in DomsatSangome.java */
#define MAX_FRAME_SIZE 512
