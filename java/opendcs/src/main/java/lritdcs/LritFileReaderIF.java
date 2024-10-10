package lritdcs;

import java.io.IOException;

import lrgs.common.DcpMsg;

public interface LritFileReaderIF
{
	/**
	  Loads the specified LRIT file.
	  @throws IOException if IO error.
	  @throws BadMessageException if invalid LRIT file header.
	*/
	public void load()
		throws IOException, BadMessageException;
	
	/**
	  Checks to see if the header is complete and has a valid CRC16 checksum.
	  Returns true if header is complete and valid, false otherwise.
	*/
	public boolean checkHeader();
	
	/**
	  Checks whether the length of the file equals the value stored in the
	  header.
	  @returns true if length is correct, false if incorrect.
	*/
	public boolean checkLength();
	
	/**
	  Checks the CRC at the end of the entire file.
	  @returns true if file is valid, false if not.
	*/
	public boolean checkCRC();
	
	/**
	 * @return next message in file or null if this is the end of file.
	 * @throws IOException
	 */
	public DcpMsg getNextMsg()
		throws IOException;
	
	public void close();



}
