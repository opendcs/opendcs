package decodes.polling;

public enum PollingThreadState
{
	Waiting, // Waiting to run
	Running, // Currently running
	Success, // Poll completed successfully
	Failed   // Poll failed
}
