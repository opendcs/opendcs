package lrgs.drgsrecv;

/**
Constants and methods for working with DAMS-NT protocol
*/
public class DamsNt
{
	public static final int PARITY_ERR     = 0x01;
	public static final int BINARY_MSG     = 0x02;
	public static final int BIT_ERRORS     = 0x04;
	public static final int NO_EOT         = 0x08;
	public static final int CARRIER_TIMES  = 0x10;
	public static final int EXTENDED_QUAL  = 0x20;

	public static final int ANY_ERROR =
		(PARITY_ERR + BIT_ERRORS + NO_EOT);
}
