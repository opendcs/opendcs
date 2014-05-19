/*
*  $Id$
*
*  $Source$
*
*  $State$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:21:09  cvs
*  Added legacy code to repository
*
*  Revision 1.6  2004/08/30 15:43:59  mjmaloney
*  Removed import statements for classes within ilex.util.
*
*  Revision 1.5  2004/08/30 14:50:25  mjmaloney
*  Javadocs
*
*  Revision 1.4  2002/05/18 20:02:09  mjmaloney
*  Added prompt to PasswordFileEditor, this is now an option on CmdLineProcessor.
*
*  Revision 1.3  2000/03/13 15:35:01  mike
*  PasswordFileEditor complete.
*
*  Revision 1.2  1999/11/11 16:19:03  mike
*  Add support for help message for each command.
*
*  Revision 1.1  1999/09/21 08:50:17  mike
*  Renamed CmdLineProg to CmdLineProcessor. This is more accurate - There's
*  no reason to require inheritence. A single prog may have many
*  CmdLineProcessors.
*
*/

package ilex.util;

import java.util.Iterator;
import java.util.StringTokenizer;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.EOFException;

/**
* This class is for implementing simple command line programs. A
* typical way to use it is to make your 'main' class inherit from
* this class. The class contains several useful functions for reading
* and parsing command-line input.
*/
public class CmdLineProcessor
{
	protected BufferedReader input;

	/**
	* If skipBlankLines is true (default if you don't change it), then
	* blank lines will not be returned by getLine.
	*/
	public boolean skipBlankLines;

	/**
	* If skipCommentLines is true (default if you don't change it),
	* then comment lines will not be returned by getLine. A comment
	* line is any line that begins with a special character sequence.
	*/
	public boolean skipCommentLines;

	/**
	* "#" by default.
	*/
	public String commentStart;

	/**
	* Prompt sent prior to reading each command.
	* Default is null, meaning that no prompt is issued.
	*/
	public String prompt = null;

	/**
	* Pass the constructor the input stream you want to read commands
	* from. Pass it System.in if you want to read commands from the
	* UNIX standard-input.
	* @param is the InputStream to read commands from
	*/
	public CmdLineProcessor( InputStream is )
	{
		commentStart = "#";
		skipBlankLines = true;
		skipCommentLines = true;
		currentLine = 0;
		input = new BufferedReader(new InputStreamReader(is));
		myCmdList = new CmdLineList();
	}

	/**
	* The no-args constructor will cause standard-input to be read.
	*/
	public CmdLineProcessor( )
	{
		this(System.in);
	}

	/**
	* Get a line from standard input. Use this low-level routine if
	* you want to parse the input yourself.
	* @return next line from InputStream
	*/
	public String getLine( ) throws IOException
	{
		while(true)
		{
			String s = input.readLine();
			currentLine++;
			if (skipCommentLines && s.startsWith(commentStart))
				continue; // Read another line
			if (skipBlankLines)
			{
				int i;
				for(i = 0; i < s.length(); i++)
					if (!Character.isWhitespace(s.charAt(i)))
						break;
				if (i >= s.length())
					continue;  // Read another line
			}
			return s;
		}
	}

	/**
	* This will contain the line number of the last line read,
	* starting with 1.
	*/
	public int currentLine;

	/**
	* Reads a line from standard input and then
	* devides it into white-space separated tokens.
	* It returns an array containing the tokens.
	* Override if you want tokenizing done in a special way.
	* @return array of string tokens
	*/
	public String[] getTokens( ) throws IOException
	{
		String s = getLine();
		StringTokenizer st = new StringTokenizer(s);
		int n = st.countTokens();
		if (n == 0)
			return null;
		String ret[] = new String[n];
		for(int i=0; i<n; i++)
			ret[i] = st.nextToken();
		return ret;
	}

	/**
	* The processInput method will continue to process input lines
	* until an IOException is thrown (usually an EOFException which
	* indicates that the end-of-file was reached). Any other type
	* of IOException will be passed onto the caller.
	* For each line it examines the first token to determine which
	* CmdLine object to execute.
	* Your commands can cause processing to abort by throwing an
	* EOFException. You can use this to implement a 'quit' command.
	* Pass this method a CmdLineList object containing a list of
	* your CmdLine objects. The CmdLineList also should override the
	* 'unrecognizedCmd' method which is called if the keyword is not
	* recognized.
	* Return the number of commands processed.
	* @param cmdlist list of known CmdLine commands.
	* @return total number of commands executed
	  @throws IOException on IO error
	*/
	public int processInput( CmdLineList cmdlist ) throws IOException
	{
		int n = 0;   // Count commands executed
		try
		{
		  Get_Next_Command:
			while(true)
			{
				if (prompt != null)
				{
					System.out.print(prompt);
					System.out.flush();
				}

				String tokens[] = getTokens();
				Iterator it = cmdlist.iterator();
				while(it.hasNext())
				{
					CmdLine cmd = (CmdLine)it.next();
					if (cmd.keyword.equalsIgnoreCase(tokens[0]))
					{
						cmd.execute(tokens);
						n++;
						continue Get_Next_Command;
					}
				}
				cmdlist.unrecognizedCmd(tokens);
			}
		}
		catch(EOFException eof)
		{
			// Fall through and return.
		}
		return n;
	}

	/**
	* The no-args version of processInput uses the internally stored
	* command line list.
	* @return number of commands executed
	  @throws IOException on IO error
	*/
	public int processInput( ) throws IOException
	{
		return processInput(myCmdList);
	}

	/**
	* The CmdLineProcessor object can hold a CmdLineList, which contains
	* a number of CmdLine objects.
	*/
	protected CmdLineList myCmdList;

	/**
	* Adds a command to the internal Command List.
	* @param cmd the CmdLine object
	*/
	public void addCmd( CmdLine cmd )
	{
		myCmdList.add(cmd);
	}

	/**
	* Adds standard help and quit commands to the list. These are
	* useful for test programs.
	*/
	public void addHelpAndQuitCommands( )
	{
		addCmd(
			new CmdLine("quit", "- Quit the program") 
			{
				public void execute(String[] tokens) throws EOFException
				{
					if (isOkToQuit())
						throw new EOFException("All done");
				}
			});
		addCmd(
			new CmdLine("help", "- Print this message")
			{
				public void execute(String[] tokens)
				{
					System.out.println("Valid commands are:");
					for(int i = 0; i<myCmdList.size(); i++)
					{
						CmdLine cl = (CmdLine)myCmdList.elementAt(i);
						System.out.println(cl.keyword + " " + cl.helpmsg);
					}
				}
			});
	}

	/**
	* Returns true if it's OK to quit the program.
	* This method allows application sub-classes to use the standard quit
	* command but gives them a hook whereby they can make sure the program
	* is in a proper state to quit.
	* <p>
	* For example a file-editor application may want to ask the user if it's
	* OK to discard changes.
	* @return true if it's OK to quit the program.
	*/
	protected boolean isOkToQuit( )
	{
		return true;
	}

	/**
	* The errorMsg method is a convenience to the CmdLine object's
	* execute methods. If they encounter syntax errors they should
	* call this method. A sub-class can override this method to handle
	* error messages in any way desired. The default behavior is to
	* print the message to System.err.
	* @param msg the message
	*/
	public void errorMsg( String msg )
	{
		System.err.println(msg);
	}
}
