/**
 * 
 */
package lrgs.networkdcp;

/**
 * @author mjmaloney
 *
 */
public enum NetworkDcpState
{
	Waiting,    // Waiting for next poll time.
	Running,    // Currently being polled
	Dead        // Has been removed from config: delete from list.
}
