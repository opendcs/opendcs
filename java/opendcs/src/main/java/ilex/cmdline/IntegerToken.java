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
 * The Implementation of Tokens for integer command-line arguments.
 */
public class IntegerToken extends Token
{
	/** 
	  	Constructor.
		@param name the switch as it is to appear on the command line.
		@param message message to appear in the "usage" output on error.
    	@param environment_variable ignored
    	@param tokenOptions see values in the TokenOptions class.
		@param def_value default value.
	*/
	public IntegerToken(String name,
                        String message,
                        String environment_variable,
                        int tokenOptions,
                        int def_value)
	{
		super(name, message, environment_variable, tokenOptions);
		setDefaultValue(Integer.valueOf(def_value));
	}

  	/** @return "<Int>"  */
	public String type() {
		return "<Int>";
	}

	/** 
	  @return the value of this token, either as specified on the command
	  line or from the default.
	*/
	public int getValue() {
		return getValue(0);
	}

	/** 
	  @return for tokens that appear more than once on the command line, this
	  method would return the specified value.
	*/
	public int getValue(int i) {
		return ((Integer) m_values.elementAt(i)).intValue();
	}

	public Object toObject(String lexeme) {
         //System.out.println("Int toObject: "+lexeme);
		return Integer.valueOf(lexeme);
	}
}
