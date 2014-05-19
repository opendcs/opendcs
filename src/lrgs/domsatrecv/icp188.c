/*
*  $Id$
*/
#include <fcntl.h>
#include <sys/types.h>
#include <time.h>
#include <unistd.h>
#include <sys/ioctl.h>
#include <string.h>

#include <icp188_driver/icp188drv.h>
#include <icp188_driver/icp188ioctl.h>
#include <icp188_driver/icp188mem.h>

struct EXE_HEADER
{
	ushort e_sig;		/* exe file signature  ( 0x5A4D ) */
	ushort e_loi;		/* length of image mod 512 */
	ushort e_sof;		/* size of file in 512 byt increments */
	ushort e_nor;		/* # of relocation items */
	ushort e_soh;		/* size of header in paragraphs */
	ushort e_min_para;      /* min # of paras required above prog */
	ushort e_max_para;      /* max # of paras required above prog */
	ushort e_sso;		/* offset of the SS in load module */
	ushort e_spv;		/* value to be given sp */
	ushort e_chk;		/* checksum of file */
	ushort e_ipv;		/* value for IP */
	ushort e_cso;		/* offset of code seg in load module */
	ushort e_fro;		/* offset of first reloc item in file */
	ushort e_ovn;		/* overlay number */
}
  __attribute__((packed))
 ;

#define BUFSIZE 1024

char icperrbuf[256];

int icp188load(int fd, const char *fname)
/*
* The program must be in the DOS EXE format with origin at 
* 0000:0000.
*
* Return: 0 if program successfully loaded, otherwise -1.
*/
{
	int prog, n;
	struct EXE_HEADER *exe_header;
	struct SHARED_DATA sd;
	char buffer[BUFSIZE];
	long jmpvec;

    /* Make sure driver is in raw mode and reboot ICP processor. */
	if (ioctl(fd, DRIVER_RAW, 0) == -1 || ioctl(fd, REBOOT, 0) == -1)
	{
		strcpy(icperrbuf, "DICPIOCTL");
		return -1;
	}

    /* Attempt to open the named program. */
	if ((prog = open(fname,O_RDONLY)) == -1)
	{
		strcpy(icperrbuf, "DBADINPUTFILE");
		return -1;
	}

    /* Get exe file header. */
	if ((n = read(prog, buffer, BUFSIZE)) < sizeof(struct EXE_HEADER))
	{
		strcpy(icperrbuf, "DBADINPUTFILE");
		close(prog);
		return -1;
	}
	exe_header = (struct EXE_HEADER *)buffer;       

    /* Compute start address from initial code segment and IP. */
	if (ioctl(fd, GET_SHARED_DATA, &sd) == -1)
	{
		strcpy(icperrbuf, "DICPIOCTL");
		close(prog);
		return -1;
	}
	jmpvec = exe_header->e_cso;
	jmpvec = (jmpvec<<16) + exe_header->e_ipv;
	sd.sd_jmpvec = (faddr_t)jmpvec;
	if (ioctl(fd, SET_SHARED_DATA, &sd) == -1)
	{
		strcpy(icperrbuf, "DICPIOCTL");
		close(prog);
		return -1;
	}

    /* 'soh' contains the actual size of the header.  The load
     * module itself contains 1056 spaceholder bytes for the interrupt
     * vector table and the shared-data structure.  Thus we want to
     * skip past the header and the first 1056 bytes of the load module
     * before loading the board starting at address 1056.
     */
	lseek(prog,(long)((exe_header->e_soh << 4) + 1056),0);
	lseek(fd, 1056l, 0);

    /* DownLoad the program in 1K blocks. */
	while((n = read(prog, buffer, BUFSIZE)) > 0)
		write(fd, buffer, n);

    /* Setting this flag will cause the 188 to jump to the start address. */
	ioctl(fd, GET_SHARED_DATA, &sd);
	sd.sd_flag = 1;
	ioctl(fd, SET_SHARED_DATA, &sd);
	close(prog); 

	return 0;
}

int icp188start(int fd)
/*
* This function starts up a previously loaded program on the ICP188x.
* By convention, ICP programs will wait until a non-zero value is seen
* at location 0x442 (called the configure_od flag) before doing anything.  
* This function sets the 'configure_ok' flag at 0x442, causing the program
* to start up.
*
* Return: 0 if successful in setting flag, otherwise -1.
*/
{
	char c;
	long clock;
	struct tm *gmt;

	time(&clock);
	gmt = gmtime(&clock);
	gmt->tm_yday++;

	if (lseek(fd, 0x448l, 0) == -1)
		return -1;
	write(fd, &gmt->tm_year, 2);
	write(fd, &gmt->tm_yday, 2);
	write(fd, &gmt->tm_hour, 2);
	write(fd, &gmt->tm_min,  2);

	if (lseek(fd, 0x442l, 0) == -1)
		return -1;
	c = 1;
	if (write(fd, &c, 1) != 1)
		return -1;

	return 0;
}
