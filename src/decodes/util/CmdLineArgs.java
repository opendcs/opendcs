/*
*  $Id$
*
*  $State$
*
*  $Log$
*  Revision 1.15  2019/01/22 19:36:34  mmaloney
*  Added -FL argument to forward the java.logging logger.
*
*  Revision 1.14  2019/01/18 16:10:27  mmaloney
*  dev
*
*  Revision 1.13  2019/01/18 16:01:10  mmaloney
*  dev
*
*  Revision 1.12  2019/01/18 15:58:26  mmaloney
*  dev
*
*  Revision 1.11  2019/01/18 15:43:17  mmaloney
*  dev
*
*  Revision 1.10  2019/01/18 15:06:48  mmaloney
*  dev
*
*  Revision 1.9  2019/01/17 20:13:54  mmaloney
*  dev
*
*  Revision 1.8  2019/01/17 20:00:17  mmaloney
*  dev
*
*  Revision 1.7  2019/01/17 16:03:26  mmaloney
*  debug log forwarding
*
*  Revision 1.6  2019/01/15 19:41:19  mmaloney
*  Remove debug
*
*  Revision 1.5  2019/01/11 14:43:16  mmaloney
*  Move JavaLoggerAdapter to ApplicationSettings
*
*  Revision 1.4  2018/02/05 15:54:34  mmaloney
*  Add fontAdjust feature to increase/decrease font sizes throughout the GUI.
*
*  Revision 1.3  2016/01/27 22:09:12  mmaloney
*  Get rid of error message when "decodes.properties" doesn't exist in a shared implementation for CWMS.
*
*  Revision 1.2  2014/06/27 20:33:49  mmaloney
*  Bug fix: Catch any exception, not just IOException.
*
*  Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
*  OPENDCS 6.0 Initial Checkin
*
*  Revision 1.9  2013/03/28 17:29:09  mmaloney
*  Refactoring for user-customizable decodes properties.
*
*  Revision 1.8  2011/11/18 20:05:25  mmaloney
*  added setDefaultLogFile method.
*
*  Revision 1.7  2009/10/30 20:00:27  mjmaloney
*  Removed -L from CmdLineArgs.java -- This is an Application Specific Arg
*
*  Revision 1.6  2009/10/27 03:51:08  shweta
*  NoCompFilter token  is defined
*
*  Revision 1.2  2009/10/26 07:42:08  shweta
*  code added to disable computation filter
*
*  Revision 1.1  2008/04/04 18:21:07  cvs
*  Added legacy code to repository
*
*  Revision 1.16  2008/01/14 14:56:44  mmaloney
*  dev
*
*  Revision 1.15  2007/12/23 19:45:11  mmaloney
*  dev
*
*  Revision 1.14  2007/12/17 15:35:27  mmaloney
*  added code to CmdLineArgs to load the decodes.properties file
*
*  Revision 1.13  2004/08/27 20:50:29  mjmaloney
*  javadocs
*
*  Revision 1.12  2003/12/23 20:10:18  mjmaloney
*  Mods to support -a (autoinstall) feature on dbimport.
*
*  Revision 1.11  2003/12/10 20:35:03  mjmaloney
*  Modified time-stamping to support usgs-style time zones.
*
*  Revision 1.10  2003/12/07 20:36:52  mjmaloney
*  First working implementation of EDL time stamping.
*
*  Revision 1.9  2003/08/01 19:17:23  mjmaloney
*  CmdLineArgs now takes default log file in constructor.
*
*  Revision 1.8  2003/07/12 16:18:30  mjmaloney
*  Assume properties file is under install-dir if -P not supplied.
*
*  Revision 1.7  2002/04/15 16:58:37  mike
*  Debug args.
*
*  Revision 1.6  2001/11/23 21:18:25  mike
*  dev
*
*  Revision 1.5  2001/10/11 00:34:24  mike
*  Improve start-up code for different types of databases. DatabaseIO is now
*  an abstract class rather than an interface. It has a factory method for
*  creating different database IO instances.
*
*  Revision 1.4  2001/09/24 20:43:27  mike
*  Created FileLogger and added the -l <log-file> argument.
*
*  Revision 1.3  2001/08/24 19:35:13  mike
*  Inherit from the new ilex.cmdline.StdAppSettings (w/o CORBA dependencies).
*
*  Revision 1.2  2001/04/23 17:33:19  mike
*  dev
*
*  Revision 1.1  2001/04/21 20:19:23  mike
*  Added read & write methods to all DatabaseObjects
*
*/
package decodes.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.File;
import java.util.Map;
import java.util.Properties;

import ilex.cmdline.*;
import ilex.util.EnvExpander;
import ilex.util.JavaLoggerAdapter;
import ilex.util.Logger;
import ilex.util.FileLogger;

/**
Extends the ilex.cmdline.StdAppSettings class to handle arguments that
are common to most DECODES programs.
*/
public class CmdLineArgs
	extends StdAppSettings
{
	// Add DECODES-specific setting declarations here...

	/** Log file argument (-l) */
	protected StringToken log_arg;
	/** properties file argument (-P) */
	private String propFile;
	/** Application Define argument (-D) */
	private StringToken define_arg;
	private BooleanToken forwardLogArg;
	


	//No filter option token.
	public StringToken NoCompFilterToken;
	
	/** Properties set explicitly on the command line. */
	private Properties cmdLineProps;

	/** default constructor */
    public CmdLineArgs()
	{
		this(true, "util.log");
	}

	/** 
	  Explicit constructor.
	  @param isNetworkApp True if this is a network-aware application
	  @param defaultLogFile Initialize log file name
	*/
    public CmdLineArgs(boolean isNetworkApp, String defaultLogFile)
	{
		super(isNetworkApp);

		// Construct DECODES-specific setting & call addToken for each one.
		log_arg = new StringToken(
			"l", "log-file", "", TokenOptions.optSwitch, defaultLogFile);
		define_arg = new StringToken(
			"D", "Env-Define", "", 
			TokenOptions.optSwitch|TokenOptions.optMultiple, "");
//		NoCompFilterToken = new StringToken("L", 
//				"Disable Computation List filter (default=on)", "",
//				TokenOptions.optSwitch, "true");
//		addToken(NoCompFilterToken);
		forwardLogArg = new BooleanToken("FL", "Forward javax.logging logger to application log.", "",
			TokenOptions.optSwitch, false);
		addToken(log_arg);
		addToken(define_arg);
		addToken(forwardLogArg);
		cmdLineProps = new Properties();

    }

	/** @return log file name, either default, or as specified in argumnet */
	public String getLogFile()
	{
		String s = log_arg.getValue();
		if (s == null || s.length() == 0)
			return null;
		return s;
	}
	
	public void setDefaultLogFile(String f)
	{
		log_arg.setDefaultValue(f);
	}
	
	
	
//	/** @return log Nofiltertoken option value, either default(true), or as specified in argument */
//	public String getNoCompFilterToken()
//	{
//		String s = NoCompFilterToken.getValue();
//		if (s == null || s.length() == 0)
//			return null;
//		return s;
//	}
//

	/**
	  Parses the command line argument and fills in internal variables.
	  @param args the arguments.
	*/
	public void parseArgs(String args[])
	{
		super.parseArgs(args);

		// If log-file specified, open it.
		String fn = getLogFile();
		if (fn != null && fn.length() > 0)
		{
			String procname = Logger.instance().getProcName();
			try { Logger.setLogger(new FileLogger(procname, fn)); }
			catch(IOException e)
			{
				System.err.println("Cannot open log file '" + fn + "': " + e);
				System.exit(1);
			}
		}

		/*
		  Legacy script compatibility:
			Old scripts supply -P arg for decodes.properties file.
			New ones put DECODES_INSTALL_DIR in system props, and assume that
			the properties file resides there.
		
			if -P arg supplied use it, else look in install dir.
		*/
		propFile = super.getPropertiesFile();
		if (propFile == null || propFile.length() == 0)
		{
			// Get DECODES_INSTALL_DIR from system properties & look there.
			String installDir = System.getProperty("DECODES_INSTALL_DIR");			
			if (installDir != null)
				propFile = installDir + File.separator + "decodes.properties";
		}

		//Load the decodes.properties
		DecodesSettings settings = DecodesSettings.instance();
		File userProps = new File(EnvExpander.expand("$DCSTOOL_USERDIR/user.properties"));
		if (!settings.isLoaded())
		{
			Properties props = new Properties();
			try
			{
				FileInputStream fis = new FileInputStream(propFile);
				props.load(fis);
				fis.close();
			}
			catch(Exception e)
			{
				// MJM if user props exists, this is not an error.
				if (!userProps.canRead())
					Logger.instance().failure(
						"CmdLineArgs:parseArgs " +
						"Cannot open DECODES Properties File '"+propFile+"': "+e);
			}
			settings.loadFromProperties(props);
		}
		
		// Userdir is needed to support multi-user installations under unix/linux.
		// If the property is not set, just copy DCSTOOL_HOME.
		// That is, assume this is a single-user or windows installation.
		String userDir = System.getProperty("DCSTOOL_USERDIR");
		if (userDir == null)
			System.setProperty("DCSTOOL_USERDIR", System.getProperty("DCSTOOL_HOME"));
		
		if (!settings.isToolkitOwner())
		{
			Properties props = new Properties();
			try
			{
				FileInputStream fis = new FileInputStream(
					EnvExpander.expand("$DCSTOOL_USERDIR/user.properties"));
				props.load(fis);
				fis.close();
				settings.loadFromUserProperties(props);
			}
			catch(IOException e)
			{
				Logger.instance().debug1(
				"CmdLineArgs:parseArgs " +
				"Cannot open User Properties File '"+propFile+"': "+e);
			}
		}
		
		// Set debug level.
		int dl = getDebugLevel();
		if (dl > 0)
			Logger.instance().setMinLogPriority(
				dl == 1 ? Logger.E_DEBUG1 :
				dl == 2 ? Logger.E_DEBUG2 : Logger.E_DEBUG3);

		/*
		  The user can set system properties on the command line with multiple
		  -Dname=value arguments. The following puts each setting into
		  System.properties so that they are available globally.
	
		  Each 'name' is converted to upper case before putting in the
		  properties set. So retrieve the property by upper-case name only.
		*/
		for(int i=0; i<define_arg.NumberOfValues(); i++)
		{
			String arg = define_arg.getValue(i).trim();
			if (arg == null || arg.length() == 0)
				continue;
			int idx = arg.indexOf('=');
			if (idx == -1 || arg.length() <= idx+1)
			{
				System.err.println("Invalid define '" + arg + "' -- should be "
					+ "in the form name=value.");
				System.exit(1);
			}
			String name = arg.substring(0,idx).trim();
			String value = arg.substring(idx+1).trim();
			if (name.length() == 0 || value.length() == 0)
			{
				System.err.println("Invalid define name='" + name
					+ "', value='" + value + "'");
				System.exit(1);
			}
			System.setProperty(name.toUpperCase(), value);
			cmdLineProps.setProperty(name, value);
		}
		
		if (DecodesSettings.instance().fontAdjust != 0)
		{
			for (Map.Entry<Object, Object> entry : javax.swing.UIManager.getDefaults().entrySet()) 
			{
			    Object key = entry.getKey();
			    Object value = javax.swing.UIManager.get(key);
			    if (value != null && value instanceof javax.swing.plaf.FontUIResource)
			    {
			        javax.swing.plaf.FontUIResource fr=(javax.swing.plaf.FontUIResource)value;
			        javax.swing.plaf.FontUIResource f = new javax.swing.plaf.FontUIResource(fr.getFamily(), 
			        	fr.getStyle(), fr.getSize() + DecodesSettings.instance().fontAdjust);
			        javax.swing.UIManager.put(key, f);
			    }
			}
		}
		
		// This will forward log messages for the CWMS JOOQ Interface to the Ilex Logger.
		Logger.instance().debug1("Forwarding javax.logging to ilex log.");
		JavaLoggerAdapter.initialize(Logger.instance(), forwardLogArg.getValue(), "", 
			"usace", "cwmsdb", "rma", "hec", "wcds", "com.rma",
			"org.jooq", "usace.cwms.db.jooq.util");
	}

	/** @return DECODES Properties file name */
	public String getPropertiesFile()
	{
		return propFile;
	}

	public Properties getCmdLineProps() { return cmdLineProps; }
}
