/*
*  $Id$
*
*  $Source$
*
*  $State$
*
*  $Log$
*  Revision 1.2  2008/11/21 16:19:39  mjmaloney
*  Removed.
*
*  Revision 1.2  2008/11/15 18:47:33  mmaloney
*  Working build of the decodes.jar file.
*
*  Revision 1.1  2008/11/15 01:12:11  mmaloney
*  Copied from separate ilex tree
*
*  Revision 1.11  2004/08/30 14:50:17  mjmaloney
*  Javadocs
*
*  Revision 1.10  2002/05/03 18:54:50  mjmaloney
*  For ClientAppSettings, added constructor for NOT including the CORBA
*  options. This is used by non-corba apps like the MessageBrowser.
*  for gui/EditProperties, implemented register method that causes pull-
*  down menus to be displayed rather than JTextField.
*
*  Revision 1.9  2001/02/04 21:22:02  mike
*  Added -D <url> argument to specify LRGS API Directory Service.
*
*  Revision 1.8  2000/10/21 21:02:45  mike
*  Added -d<level> argument for debug level.
*
*  Revision 1.7  2000/05/19 16:40:42  mike
*  Button behavior updates (not finished)
*
*  Revision 1.6  2000/05/19 16:26:34  mike
*  Changed all instances of "NamingContext" to "NamingContextExt" for
*  compatibility with the latest version of JacORB.
*
*  Revision 1.5  2000/04/04 19:07:25  mike
*  dev
*
*  Revision 1.4  2000/03/30 22:54:04  mike
*  dev
*
*  Revision 1.3  2000/03/24 02:16:06  mike
*  dev
*
*  Revision 1.2  2000/03/18 21:36:00  mike
*  Support for common arguments -L -C -H and -S in both client and server.
*
*  Revision 1.1  2000/03/18 20:00:13  mike
*  dev
*
*
*/
package ilex.corba;

import java.net.InetAddress;
import org.omg.CosNaming.*;
import ilex.cmdline.*;

/**
* This class provides a convenient method of supporting command line arguments
* for finding an initial service.
* We can find the initial service in one of three ways:
* <ol>
* <li>The command line contains an URL that contains the IOR of the service.
* <li>The command line contains an URL to a local name context. The local
* name context contains an entry for the service.
* <li>The command line contains an URL to a central name context and a system
* name. We lookup the system name in the central context to obtain the
* local context. Then we lookup the service.
* </ol>
* <p>
* The command line arguments that support this are:
* <ul>
* <li>[-L URL]      - Local Naming Context to lookup EventAdmin.service
* <li>[-C URL]      - Central Naming Context to lookup LRGS systems.
* <li>[-H hostname] - Name of system to look up in Central name context.
* <li>[-S URL]      - Direct URL to initial service.
* <li>[-P filename] - Name of properties file for this application.
* </ul>
* <p>
* To use this class, first create a ClientAppSettings object. Then add
* any app-specific args. Take care not to clash with the arg letters listed
* above.
* <p>
* Initialize the ORB and call ClientUtil.setORB to make a singleton instance
* available. Then call parseArgs(args), passing it the String array of
* command line arguments.
* Finally, call findInitialService(), which should result in your initial
* service being located through one of the
* means described above. If not, an IllegalArgumentException will be thrown
* detailing the problems.
*/
public class ClientAppSettings extends ApplicationSettings
{
//	public StringToken localNC_arg;
//	public StringToken centralNC_arg;
	public StringToken host_arg;
//	public StringToken service_arg;
	public StringToken properties_arg;
	public IntegerToken debuglevel_arg;
//	public StringToken directory_arg;

	// constructs settings with or without std CORBA args.
	/**
	* Constructor.
	* @param useCorba true if this is a CORBA application
	*/
	public ClientAppSettings( boolean useCorba )
	{
		super();
		if (useCorba)
		{
//			localNC_arg = new StringToken("L", "URL to Local Name Context", "", 
//				TokenOptions.optSwitch, "");
//			addToken(localNC_arg);

//			centralNC_arg = new StringToken("C", "URL to Central Name Context",
//				"", TokenOptions.optSwitch, "");
//			addToken(centralNC_arg);

//			service_arg = new StringToken("S", "URL direct to Initial Service",
//				"", TokenOptions.optSwitch, "");
//			addToken(service_arg);

//			directory_arg = new StringToken("D", "URL to Directory Service",
//				"", TokenOptions.optSwitch, "");
//			addToken(directory_arg);
		}
		host_arg = new StringToken("H", "Host Name", "", 
			TokenOptions.optSwitch, "");
		addToken(host_arg);

		properties_arg = new StringToken("P", 
			"Name (or path) of properties file","",TokenOptions.optSwitch, "");
		addToken(properties_arg);

		debuglevel_arg = new IntegerToken("d", "Debug level (0-3)", "",
			TokenOptions.optSwitch, 0);
		addToken(debuglevel_arg);
	}

	
	/** Default constructor. */
	public ClientAppSettings( ) 
	{
		this(true);
	}

	/**
	* Interprets command line arguments to find the initial service.
	* Passing null for the name will cause this method to interpret arguments
	* and resolve name servers, but it will return null.
	* @param name name of initial service
	* @param kind type of initial service
	* @return CORBA Object reference of initial service
	*/
//	public org.omg.CORBA.Object findInitialService( String name, String kind ) throws IllegalArgumentException
//	{
//		org.omg.CORBA.ORB orb = ClientUtil.getORB();
//		if (orb == null)
//			throw new IllegalArgumentException(
//				"ORB must be defined in ClientUtil prior to findInitialService");
//
//		org.omg.CORBA.Object ret = null;
//
//		String localUrl = localNC_arg.getValue();
//		String centralUrl = centralNC_arg.getValue();
//		String host = host_arg.getValue();
//		String serviceUrl = service_arg.getValue();
//
//		try
//		{
//			// If -S option used, attempt to connect directly to service.
//			if (serviceUrl.length() > 0)
//			{
//				ret = ClientUtil.url2object(orb, serviceUrl);
//			}
//			// Else need to look up service from names contexts
//			else
//			{
//				// if a local name context was specified, get it.
//				if (localUrl.length() > 0)
//				{
//					ClientUtil.setLocalNamingContext(localUrl);
//				}
//
//				// Else if central context & host was specified, find local ctx
//				if (centralUrl.length() > 0)
//				{
//					ClientUtil.setCentralNamingContext(centralUrl);
//					if (host.length() > 0)
//					{
//						ClientUtil.setLocalNamingContext(
//							NamingContextExtHelper.narrow(
//							ClientUtil.lookupCentral(host, "context")));
//					}
//				}
//
//				// If a lookup name was supplied, find the initial servant.
//				if (name != null)
//					ret = ClientUtil.lookupLocal(name, kind);
//			}
//		}
//		catch(IllegalArgumentException e) { throw e; }
//		catch(Exception e)
//		{
//			throw new IllegalArgumentException(e.toString());
//		}
//
//		return ret;
//	}

	/**
	* Returns the host name specified on the command line
	* arguments. Returns the local host name if none was
	* specified.
	* @return host name from command line args.
	*/
	public String getHostName( )
	{
		try
		{
			String r = host_arg.getValue();
			if (r == null || r.length() == 0)
				return InetAddress.getLocalHost().getHostName();
			return r;
		}
		catch(Exception e) {}
		return null;
	}

	/**
	* Returns the name of the properties file specified on the command
	* line or null if none was.
	* @return name of properties file from command line, or null if none specified.
	*/
	public String getPropertiesFile( )
	{
		String r = properties_arg.getValue();
		if (r != null && r.length() > 0)
			return r;
		return null;
	}

	/**
	* @return debug level from command line args.
	*/
	public int getDebugLevel( )
	{
		return debuglevel_arg.getValue();
	}
}
