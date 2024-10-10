/*
* $Id$
*/
package lrgs.dqm;

/**
This class defines the interface to the DOMSAT quality monitor. Currently
this is implemented by DapsDqmInterface. In the future the DQM function
may be handled internally.
*/
public interface DqmInterface
{
	/**
	 * Handles a domsat dropout.
	 * @param gapStart the start sequence number of the dropout.
	 * @param numDropped the number dropped.
	 * @param elapsedSec the elapsed number of seconds in the gap.
	 */
	public void domsatDropped(int gapStart, int numDropped, int elapsedSec);
}
