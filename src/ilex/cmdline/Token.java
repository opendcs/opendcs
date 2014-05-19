/*
*  $Id$
*
*  $Source$
*
*  $State$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:21:08  cvs
*  Added legacy code to repository
*
*  Revision 1.10  2007/02/19 23:02:03  mmaloney
*  Access to default.
*
*  Revision 1.9  2007/02/19 15:33:17  mmaloney
*  Allow caller to set default.
*
*  Revision 1.8  2004/11/29 15:43:52  mjmaloney
*  Improved error output & usage strings.
*
*  Revision 1.7  2004/08/30 14:50:16  mjmaloney
*  Javadocs
*
*  Revision 1.6  2002/08/31 12:16:42  mjmaloney
*  Got rid of assert() statements for JDK 1.4
*
*  Revision 1.5  2002/08/29 05:58:44  chris
*  Serious javadoc-ification, while I (chris) was trying to figure
*  out how these classes work.
*
*  Revision 1.4  2002/05/03 18:54:50  mjmaloney
*  For ClientAppSettings, added constructor for NOT including the CORBA
*  options. This is used by non-corba apps like the MessageBrowser.
*  for gui/EditProperties, implemented register method that causes pull-
*  down menus to be displayed rather than JTextField.
*
*  Revision 1.3  2000/03/18 21:35:29  mike
*  dev
*
*  Revision 1.2  1999/12/01 12:15:58  mike
*  12/1/1999
*
*  Revision 1.1  1999/09/30 18:16:41  mike
*  9/30/1999
*
*
*/
package ilex.cmdline;

import java.io.*;
import java.util.*;

/**
Base class for each type of token.
Each Token object encapsulates one command line argument or switch.
Most of the parsing work is done here in the subclass.
*/
public abstract class Token
{
	protected String m_name;
	protected String m_message;
	protected int m_flags;
	protected String m_env_variable;
	protected Vector m_values;
	protected Object m_defaultValue;
	protected boolean m_firstTime;

	private String explicitType = null;

	/** 
	  The name is the string that appears on the command line.
	  Example, the name for  "-p portnum" would be "p".
	  @return the name
	*/
	public String name() {return m_name;};

	/**
	  @return number of values entered for this token on the command line.
	*/
	public int NumberOfValues() {
		return m_values.size();
	}

	public String extendedName() 
	{
		if (isSwitch()) {
			return "-" + name();
		} else {
			return name();
		}
	}

	//------------------------------------------------------------
	// Subclasses should implement these
	//------------------------------------------------------------

	public abstract Object toObject(String lexeme);


  /**
   * This returns true if the option takes one or more arguments.
   * All but bool (where merely the appearence of the flag
   * signifies the existence) return true;
   */

	public boolean hasOneOrMoreArgs() {
        return true;
    }

	/**
	  Sets the explicit representation for type string.
	  Used to improve the "usage" message. Instead of just saying <String>
	  you can have it say, e.g. <filename>
	  @param type the type to set
	*/
	public void setType(String type)
	{
		explicitType = type;
	}

	/**
	  @return the symbolic type for use in usage messages.
	*/
	public String getType()
	{
		if (explicitType != null)
			return "<" + explicitType + ">";
		return type();
	}

	public abstract String type();

	// Also implement these two methods
	// <Type> getValue(int i);
	// <Type> getValue();

	protected Token(
		String a_name,
		String a_message,
		String a_environment_variable,
		int aTokenOptions  // of type TokenOptions
		) {
		m_name = a_name;
		m_message = a_message;
		m_env_variable = a_environment_variable;
		m_flags = aTokenOptions;
		m_firstTime = true;
		m_values = new Vector(1);
	};

	//--------------------------------------------------------------
	// These methods are used by the ApplicationSettings class
	// Thast' why they are protected
	//--------------------------------------------------------------

  /**
   * This handles parsing the command-line options after matching a switch.
   * It parses as many command line arguments as apply to this
   * switch and then returns just before the first one it does not
   * recognize.
   */
	protected boolean ParseSwitch(StringArrayIterator cmdLineArgs)
		throws IllegalArgumentException
	{
		if (this.isArgument())
			return false;
		if ((this.isUsed()) && (!this.allowsMultipleValues()))
			return false;
		if (cmdLineArgs.get().substring(1).indexOf(name()) != 0)
			return false;

		// after the match what remains e.g. if we are -t and
		// argument is -tom then rest == 'om'
		String lexeme = cmdLineArgs.get().substring(1 + name().length());
		if (lexeme.length() == 0) {
			// the "-t foo" or "-t" case
			if (this.hasOneOrMoreArgs()) {
				// move to the "foo"
				cmdLineArgs.moveNext();
				if (!cmdLineArgs.EOF())
				 lexeme = cmdLineArgs.get();
				else {
				 String str = new String("Argument expected for option ");
				 str += this.extendedName();
				 throw new IllegalArgumentException(str);
				}
			}
		} else {
			// "-tfoo" case
			if (!this.hasOneOrMoreArgs()) {
				String str = new String("No Argument expected for option ");
				str += this.name();
				throw new IllegalArgumentException(str);
			}
		}

		this.AddValueFromLexeme(lexeme);
		this.setUsed();

				    /*
				     * If you comment out these lines then
				     * "-l 1 2 3" will be permitted. Now this should be
				     * "-l 1 -l 2 -l 3"
				     *
				    if (allowsMultipleValues()) {
				  cmdLineArgs.moveNext();
				  // if it supports multiple parse more arguments
				  while ((!cmdLineArgs.EOF()) &&
				      (!isASwitch(cmdLineArgs.get()))) {
				   this.AddValueFromLexeme(cmdLineArgs.get());
				   cmdLineArgs.moveNext();
				  }
				  cmdLineArgs.movePrevious();
				    }
				    */

		return true;
	}

	protected boolean parseArgument(StringArrayIterator cmdLineArgs) {
		if (isSwitch()) return false;
		if ((isUsed()) &&
			(!allowsMultipleValues())) return false;

		// if it supports multiple parse more arguments
		while ((!cmdLineArgs.EOF()) &&
				  (!isASwitch(cmdLineArgs.get()))) {
			this.AddValueFromLexeme(cmdLineArgs.get());
			this.setUsed();
			cmdLineArgs.moveNext();
			if (!allowsMultipleValues()) break;
		}

		return true;
	}

	protected void printUsage(java.io.PrintStream str) {
		if (!this.isRequired()) str.print( "[");
		str.print(this.extendedName() + " ");
		str.print(this.getType());
		if (this.allowsMultipleValues()) str.print(" ...");
		if (!this.isRequired()) str.print( "]");
		str.print(" ");
	}

	protected void printUsageExtended(java.io.PrintStream str) {
		str.print("\t");
		str.print(this.extendedName() + " ");
		str.print("'" + this.m_message + "' ");
		if (hasEnvironmentVariable()) {
			str.print(" Environment: $" + this.m_env_variable);
		}
		String dflt = this.getDefaultValue();
		if (!this.isRequired() && dflt != null && dflt.length()>0) {
			str.print(" Default: ");
			str.print(dflt);
		}
		str.println();
	}

	protected boolean hasEnvironmentVariable()
	{
		return this.m_env_variable.compareTo("") != 0;
	}

	protected String getEnvironmentVariable() {
		return this.m_env_variable;
	}

	protected boolean isRequired() {
		return (m_flags & TokenOptions.optRequired) ==  TokenOptions.optRequired;
	};

	protected boolean isSwitch() {
		return !isArgument();
	};

	protected boolean isArgument() {
		return (m_flags & TokenOptions.optArgument) == TokenOptions.optArgument;
	};

	protected boolean allowsMultipleValues() {
		return (m_flags & TokenOptions.optMultiple) == TokenOptions.optMultiple;
	};

	protected boolean isUsed() {
		return (m_flags & TokenOptions.optAlreadyUsed) == TokenOptions.optAlreadyUsed;
	};

	protected void setUsed() {m_flags |= TokenOptions.optAlreadyUsed;};

	protected void AddValueFromLexeme(String lexeme)
	{
		if (m_firstTime)
		{
			SetValueFromLexeme(lexeme, 0);
		}
		else
		{
	        //util.assert(this.allowsMultipleValues(), "");
			m_values.addElement(toObject(lexeme));
		}
		m_firstTime = false;
	}

	protected void SetValueFromLexeme(String lexeme, int i)
	{
		m_values.setSize(java.lang.Math.max(m_values.size(), i + 1));
		m_values.setElementAt(toObject(lexeme), i);
	}

	protected String getDefaultValue() {
		return m_defaultValue == null ? "(null)" : m_defaultValue.toString();
	}

	public void setDefaultValue(Object obj) {
		m_defaultValue = obj;
		m_values.setSize(java.lang.Math.max(m_values.size(),1));
		m_values.setElementAt(obj, 0);
	}

	protected static boolean isASwitch(String arg) {
		return (arg.charAt(0) == '-');
	}

	public String getMessage() { return m_message; }

}
