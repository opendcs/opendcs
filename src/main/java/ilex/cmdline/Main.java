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
*  Revision 1.2  2004/08/30 14:50:15  mjmaloney
*  Javadocs
*
*  Revision 1.1  1999/09/30 18:16:41  mike
*  9/30/1999
*
*
*/

package ilex.cmdline;
//import ilex.cmdline.*; 

/**
Test main for ApplicationSettings.
*/
public class Main 
{ 
  // Initialize the applicationSettings and 
  // the tokens objects 
  static ApplicationSettings sm_main = 
                 new ApplicationSettings(); 
  static BooleanToken 
    sm_verbose = new BooleanToken( 
                  "v",   // switch name 
                  "verbose turned on or off", // message 
                  "",    // environment variable 
                  TokenOptions.optSwitch, // options 
                  false);   // default value 
  static IntegerToken 
    sm_lines = new IntegerToken( 
                  "l", 
                  "Number of empty lines to insert", 
                  "", 
                  TokenOptions.optSwitch, 
                  0); 
  static StringToken 
    sm_files =   new StringToken( 
                   "", 
                   "Test cases", 
                   "", 
                   TokenOptions.optArgument| 
                   TokenOptions.optMultiple| 
                 TokenOptions.optRequired, 
                   ""); 
  // Add all the token objects to the 
  // ApplicationSettings object 
  static { 
    sm_main.addToken(sm_verbose); 
    sm_main.addToken(sm_lines); 
    sm_main.addToken(sm_files); 
  } 


	public static void main (String[] args) 
  { 
    try { 
      sm_main.parseArgs(args); 
  
      // Let's see what we 've got 
      System.out.println(sm_verbose.getValue() ? "verbose" : "not verbose"); 
      System.out.println("lines: " + Integer.toString(sm_lines.getValue(0))); 
      for (int i = 0; i < sm_files.NumberOfValues(); i++) 
        System.out.println("tests: " + sm_files.getValue(i)); 
    } catch (Exception ex) { 
		System.out.println("Exception caught: " + ex);
  
    } 
  } 
}
