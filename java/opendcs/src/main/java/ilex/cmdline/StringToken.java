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
*  Revision 1.4  2004/08/30 14:50:16  mjmaloney
*  Javadocs
*
*  Revision 1.3  2002/08/29 05:58:44  chris
*  Serious javadoc-ification, while I (chris) was trying to figure
*  out how these classes work.
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

/**
* The Implementation of Tokens for string command-line arguments.
*/
public class StringToken extends Token
{
	/** 
	  	Constructor.
		@param name the switch as it is to appear on the command line.
		@param message message to appear in the "usage" output on error.
    	@param environment_variable ignored
    	@param tokenOptions see values in the TokenOptions class.
		@param def_value default value.
	*/
    public StringToken(String name,
                       String message,
                       String environment_variable,
                       int tokenOptions,
                       String def_value)
    {
        super(name, message, environment_variable, tokenOptions);
        setDefaultValue(def_value);
    }

  /** Returns "<String>"  */
    public String type() {
         return "<String>";
    }

	/** 
	  @return the value of this token, either as specified on the command
	  line or from the default.
	*/
    public String getValue() {
         return getValue(0);
    }

	/** 
	  @return for tokens that appear more than once on the command line, this
	  method would return the specified value.
	*/
    public String getValue(int i) {
         return (String)m_values.elementAt(i);
    }

    public Object toObject(String lexeme) {
        return lexeme;
    }
}
