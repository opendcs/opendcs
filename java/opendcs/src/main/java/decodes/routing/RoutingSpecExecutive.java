/*
*  $Id$
*
*  $State$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:21:04  cvs
*  Added legacy code to repository
*
*  Revision 1.4  2004/08/30 21:21:46  mjmaloney
*  javadoc
*
*  Revision 1.3  2003/08/01 19:17:23  mjmaloney
*  CmdLineArgs now takes default log file in constructor.
*
*  Revision 1.2  2001/08/24 19:28:18  mike
*  Make compilable -- right now this is just a place-holder class.
*
*  Revision 1.1  2001/07/24 02:16:57  mike
*  dev
*
*
*/
package decodes.routing;

import ilex.cmdline.*;

import decodes.db.ValueNotFoundException;
import decodes.db.InvalidDatabaseException;
import decodes.util.CmdLineArgs;

/**
This class executes one or more routing spec. It provides methods for
starting and stopping individual routing specs, and for retrieving the
status of all currently-running specs.
*/
public class RoutingSpecExecutive
{
	/**
	  Constructor not yet implemented.
	*/
	public RoutingSpecExecutive()
	{
	}

	/**
	Starts the named routing spec in a separate thread.
	@param rsName name of routing spec to start
	@throws ValueNotFoundException if named spec does not exist.
	@throws InvalidDatabaseException if named spec could not be initialized.
	*/
	public void start(String rsName)
		throws ValueNotFoundException, InvalidDatabaseException
	{
	}

	static CmdLineArgs cmdLineArgs = new CmdLineArgs(false, "routing.log");
	static StringToken rsArgs = new StringToken("", "Routing Spec Names", "",
		TokenOptions.optArgument|TokenOptions.optMultiple
		|TokenOptions.optRequired, "");
	static
	{
		cmdLineArgs.addToken(rsArgs);
	}

	/**
	Usage java decodes.decoder.RoutingSpecExec [specname] ...
	<p>
	@param args command line arguments
	Executes routing specifications named on the command line. Each routing
	spec is executed in its own thread.
	*/
	public static void main(String args[])
	{
	}
}
