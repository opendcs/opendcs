package decodes.tsdb;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;
import org.slf4j.event.Level;

public abstract class ProgressListener
{
	private static final Logger PROGRESS_LOGGER = OpenDcsLoggerFactory.getLogger();

	public abstract void onProgress(String message, Level logLevel, Throwable cause);

	protected void logEvent(String message, Level logLevel, Throwable cause)
	{
		PROGRESS_LOGGER.atLevel(logLevel).setCause(cause).log(message);
	}

	public static final class LoggingProgressListener extends ProgressListener
	{
		@Override
		public void onProgress(String message, Level logLevel, Throwable cause)
		{
			logEvent(message, logLevel, cause);
		}
	}
}
