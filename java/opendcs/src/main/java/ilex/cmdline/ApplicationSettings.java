/*
*  $Id$
*/
package ilex.cmdline; 

import java.util.TimeZone;

import ilex.util.Logger;

/* 
This family of classes provides an object-oriented way to process
command line arguments.
Instantiate an ApplicationSettings object (or sub-class thereof) and
populate it with the various Token objects. Then pass it the String
arguments from the command line.
*/ 
public class ApplicationSettings 
{ 
	private StringToken ltz_arg;//logger timezone
	
	/** Default constructor. */
	public ApplicationSettings() { this(optDoNotIgnoreUnknown); } 
  
	/** 
	  Construct with specified options.
	*/
	public ApplicationSettings(int aApplicationSettingsOptions) { 
		m_argDescriptions = new java.util.Vector(); 
		m_flags = aApplicationSettingsOptions;
		createLTZArg();
	} 
  
 	/**
 	 * 
 	 *
 	 */
 	private void createLTZArg()
 	{
 		ltz_arg = new StringToken("Y", 
				"The log file time-zones", "",
				TokenOptions.optSwitch, 
				"UTC");
 		addToken(ltz_arg);
 	}
 	
	/**
	  Adds a token, representing a particular command-line argument.
	  @param argum the token object.
	*/
	public void addToken(Token argum) 
	{ 
		for (int i = 0; i < m_argDescriptions.size(); i++) 
		{
			Token argDesc = ((Token)m_argDescriptions.elementAt(i)); 

			//Make sure we have no argument clash 
			if (argDesc.name().compareTo(argum.name()) == 0) 
			{
				System.err.print("ApplicationSettings ERROR: option '"); 
				System.err.print(argum.name()); 
				System.err.println("' is used more than once:");
				System.err.println("\t" + argDesc.getMessage());
				System.err.println("\t" + argum.getMessage());
				System.exit(-1); 
			}

			// make sure there is only one that is non switch 
			if (!argDesc.isSwitch() && !argum.isSwitch()) 
			{
				System.err.print("ApplicationSettings ERROR: arguments defined in both '"); 
				System.err.print(argum.name()); 
				System.err.print("' and '"); 
				System.err.println(argDesc.name() + "'"); 
				System.err.println("Arguments should be defined only once."); 
				System.exit(-2); 
			} 
		} 
		m_argDescriptions.addElement(argum); 
	} 

	public void rmToken(Token argum) 
	{
		m_argDescriptions.remove(argum);
	}
  
	/** 
	  Call this from your main with the command line arguments.
	  The args will be matched with the tokens, values extracted and set
	  in the tokens.
	  @param args the command line arguments.
	*/
	public void parseArgs(String[] args) 
		throws IllegalArgumentException 
	{
		String dbg = "Cmd Line Args: ";
		for(String arg : args)
			dbg = dbg + arg + ' ';
		Logger.instance().debug1(dbg);

		m_cmdLineArgs = new StringArrayIterator(args);
		setEnvironmentValues(); 
		parseInternal();
		Logger.instance().setTimeZone(TimeZone.getTimeZone(getLtzArg()));
	} 
  
	/**
	  Prints a usage string explaining the command line syntax and a help
	  message for each token.
	  @param reason The reason why the arguments are invalid.
	*/
	public void printUsage(String reason) 
		throws IllegalArgumentException 
	{
		// Print the first line 
		System.err.print("Error: "); 
		System.err.println(reason + "\n"); 
		// Print the usage line 
		System.err.print("Usage: "); 
		System.err.print(m_programName + " "); 
		for (int i = 0; i < m_argDescriptions.size(); i++) 
		{ 
			Token arg = (Token)m_argDescriptions.elementAt(i); 
			arg.printUsage(System.err); 
		} 
		System.err.println("\n"); 
  
		// Print the explanations 
		for (int i = 0; i < m_argDescriptions.size(); i++) 
		{
			Token arg = (Token)m_argDescriptions.elementAt(i); 
			arg.printUsageExtended(System.err); 
		} 
		throw new IllegalArgumentException(reason); 
	} 
  
 //--------------------------------------------------------------------- 
  
 // This functions should be called only once 
 protected void parseNonSwitch() throws IllegalArgumentException { 
  for (int i = 0; i < m_argDescriptions.size(); i++) { 
   Token argDesc = ((Token)m_argDescriptions.elementAt(i)); 
  
   if (!argDesc.parseArgument(m_cmdLineArgs)) continue; 
  
   // here should be the end... 
   if (!m_cmdLineArgs.EOF()) { 
    this.printUsage("too many command line arguments."); 
   } 
   return; 
  } 
  
  String str = "Unexepected argument "; 
  str += m_cmdLineArgs.get(); 
  if (!ignoreUnknownSwitches()) 
   this.printUsage(str); 
 } 
  
	protected void parseInternal() throws IllegalArgumentException 
	{ 
  		// skip the name of the program 
  		m_programName = "program"; 
  
		while (!m_cmdLineArgs.EOF()) 
		{
			try 
			{ 
				String s = m_cmdLineArgs.get();
				if (Token.isASwitch(s)) 
				{
					this.parseSwitch(); 
				}
				else 
				{ 
					this.parseNonSwitch(); 
					break; 
				} 
			}
			catch (Exception ex) 
			{
				String str = ex.getMessage(); 
				if (ex.getClass() == NumberFormatException.class) 
				{ 
					str = str + " "; 
					str = str + " wrong argument type"; 
				} 
				// most likely 'argument expected' 
				this.printUsage(str); 
			} 
			m_cmdLineArgs.moveNext(); 
		} 
  
		for (int i = 0; i < m_argDescriptions.size(); i++) 
		{
			Token argDesc = ((Token)m_argDescriptions.elementAt(i)); 
			if (!argDesc.isUsed() && argDesc.isRequired()) 
			{
				String str; 
				str = "missing required argument: "; 
				str += argDesc.getMessage(); 
				this.printUsage(str); 
			} 
		} 
	} 
  
	protected void parseSwitch() 
		throws IllegalArgumentException 
	{ 
		int i = 0; 
		for (i = 0; i < m_argDescriptions.size(); i++) 
		{
			Token argDesc = ((Token)m_argDescriptions.elementAt(i)); 
			if (argDesc.ParseSwitch(m_cmdLineArgs)) 
				return; 
		}

		// We tried all the tokens and no one recognized 
		if (i >= m_argDescriptions.size()) 
		{
			String str= new String("Unknown option "); 
			str += m_cmdLineArgs.get(); 
			if (!ignoreUnknownSwitches()) 
				throw new IllegalArgumentException(str);
//				this.printUsage(str); 
		}
	}
  
 protected void setEnvironmentValues() { 
  for (int i = 0; i < m_argDescriptions.size(); i++) { 
   Token argDesc = ((Token)m_argDescriptions.elementAt(i)); 
   if (argDesc.hasEnvironmentVariable()) { 
    String str = util.GetEnvVariable(argDesc.getEnvironmentVariable()); 
    if (str.length() != 0) { 
     argDesc.SetValueFromLexeme(str, 0); 
    } 
   } 
  } 
 } 
  
 protected boolean ignoreUnknownSwitches() { 
  return (m_flags & optIgnoreUnknown) != 0; 
 } 
  
 // Data members 
 protected StringArrayIterator m_cmdLineArgs; 
 protected String              m_programName; 
 protected java.util.Vector    m_argDescriptions; // Vector of Argv objects 
 protected int                 m_flags; 


	// MJM Added
	public static final int optIgnoreUnknown = 1;
	public static final int optDoNotIgnoreUnknown = 0;

	/**
	 * Return the Logger Time Zone value
	 * @return
	 */
	public String getLtzArg()
	{
		if (ltz_arg == null)
			return "UTC";

		try
		{
			String r = ltz_arg.getValue();
			if (r == null || r.length() == 0)
				return "UTC";
			return r;
		}
		catch(Exception e) {}
		return "UTC";
	}
} 
