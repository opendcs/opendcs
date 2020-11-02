package decodes.routing;

/**
 * Lists possible run states for a schedule entry executive.
 * @author mmaloney Mike Maloney, Cove Software, LLC
 */
public enum RunState
{
	/** Transient state after creation while parsing configuration, etc. */
	initializing,
	/** A one-time or periodic task is waiting for the time it is to execute. */
	waiting,
	/** Routing spec is executing. */
	running,
	/** A one-time routing spec has completed.  */
	complete,
	/** The daemon executing this task has been shut down. */
	shutdown
}
