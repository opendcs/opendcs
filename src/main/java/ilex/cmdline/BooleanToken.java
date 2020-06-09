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
*  Revision 1.3  2004/08/30 14:50:15  mjmaloney
*  Javadocs
*
*  Revision 1.2  2002/08/29 05:58:44  chris
*  Serious javadoc-ification, while I (chris) was trying to figure
*  out how these classes work.
*
*  Revision 1.1  1999/09/30 18:16:41  mike
*  9/30/1999
*
*
*/

package ilex.cmdline;

/**
 * The Implementation of Tokens for boolean command-line arguments
 * (also known as switches or flags).
 */
public class BooleanToken extends Token
{
	/** 
	  	Constructor.
		@param name the switch as it is to appear on the command line.
		@param message message to appear in the "usage" output on error.
    	@param environment_variable ignored
    	@param tokenOptions see values in the TokenOptions class.
		@param def_value default value.
	*/
	public BooleanToken(String name,
		                String message,
                        String environment_variable,
                        int tokenOptions,
		          	    boolean def_value)
	{
		super(name, message, environment_variable, tokenOptions);
		setDefaultValue(new Boolean(def_value));
	}

  	/** @return the empty String.  */
	public String type() {
		return "";
	}

	/** 
	  @return the value of this token, either as specified on the command
	  line or from the default.
	*/
	public boolean getValue() {
		return getValue(0);
	}

	/** 
	  @return for tokens that appear more than once on the command line, this
	  method would return the specified value.
	*/
	public boolean getValue(int i) {
		return ((Boolean) m_values.elementAt(i)).booleanValue();
	}

	public Object toObject(String lexeme) {
		return new Boolean(true);
	}

	/** This returns false, since a boolean switch takes no arguments.  */
	public boolean hasOneOrMoreArgs()
	{
		return false;
	}
}
