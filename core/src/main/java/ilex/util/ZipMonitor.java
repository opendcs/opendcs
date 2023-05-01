package ilex.util;

import java.io.IOException;

/**
ZipMonitor is an interface used by the FileUtil.unzip() method.
In the future it may also be used for a FileUntil.zip() method.
It allows your class to monitor the progress of the unzipping, perhaps
for display in a status dialog.
*/
public interface ZipMonitor
{
	/**
	  Sets the status.
	  @param status the status
	*/
	public void setZipStatus(String status);

	/**
	  Sets the number of entries in the zip file. Can be used to set the
	  dimension of a progress bar, etc.
	  @param num the number of entries in the zip file
	*/
	public void setNumZipEntries(int num);

	/**
	  Sets the current progress, that is, the number of entries thus far
	  unzipped. The monitor may throw IOException if it wants to abort the 
	  unzip process.
	  @param num current progress.
	*/
	public void setZipProgress(int num)
		throws IOException;

	/**
	  Called if the zipping or unzipping was aborted due to some error.
	  @param ex the Exception which caused the abort.
	*/
	public void zipFailed(Exception ex);

	/** Called when the transfer is completed successfully. */
	public void zipComplete();
}
