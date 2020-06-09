/**
 * $Id$
 * 
 * Open Source Software.
 * Author: Michael Maloney, Cove Software, LLC
 * 
 * $Log$
 * Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
 * OPENDCS 6.0 Initial Checkin
 *
 * Revision 1.1  2013/02/05 21:07:59  mmaloney
 * Created.
 *
 */
package ilex.util;

/**
 * Use this class when you want to have a module of code with its
 * own log priority that passes through messages to the default
 * Logger instance.
 */
public class PassThruLogger extends Logger
{
	private Logger parent = null;
	
	public PassThruLogger(Logger parent)
	{
		super("");
		this.parent = parent;
	}

	@Override
	public void close()
	{
	}

	@Override
	public void doLog(int priority, String text)
	{
		parent.doLog(priority, text);
	}
}
