
package ilex.util;

/**
The ProcWaiterThread can issue a callback when the process has died.
Implement this interface if you want a callback.
*/
@FunctionalInterface
public interface ProcWaiterCallback
{
	public void procFinished(String procName, Object obj, int exitStatus);
}
