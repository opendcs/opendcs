package decodes.syncgui;

import java.io.IOException;
import java.io.InputStream;

/**
This provides the interface to call back after a download has completed
or errored-out.
*/
public interface DownloadReader
{
	/**
	  Called to read the download stream. The concrete method should 
	  read the stream until it is finished or the reader is satisfied.
	  @param relpath the relative path downloaded
	  @param strm InputStream to read from
	*/
	public void readFile(String relpath, InputStream strm)
		throws IOException;

	/**
	  Called when download could not be opened because of an exception.
	  @param ex the exception thrown, most likely will be an instance
	  of IOException or MalformedURLException
	*/
	public void readFailed(String relurl, Exception ex);
	

}
