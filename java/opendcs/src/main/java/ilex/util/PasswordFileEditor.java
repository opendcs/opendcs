/*
* Where Applicable, Copyright 2025 OpenDCS Consortium and/or its contributors
*
* Licensed under the Apache License, Version 2.0 (the "License"); you may not
* use this file except in compliance with the License. You may obtain a copy
* of the License at
*
*   http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
* License for the specific language governing permissions and limitations
* under the License.
*/
package ilex.util;

import java.io.Console;
import java.io.File;
import java.io.IOException;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;

import ilex.cmdline.*;

/**
This class contains a main which is a stand-alone simple command line
editor for a PasswordFile. The LRGS suite uses this from the editPasswd
script.
*/
public class PasswordFileEditor extends CmdLineProcessor
{
	private static final org.slf4j.Logger log = OpenDcsLoggerFactory.getLogger();
	/** The PasswordFile to edit */
	PasswordFile passwordFile;
	/** True if file has been modified. */
	boolean modified;

	/**
	* Constructor
	* @param filename the file tgo edit
	* @throws Exception on IO error
	*/
	public PasswordFileEditor( String filename ) throws Exception
	{
		super(System.in);
		passwordFile = new PasswordFile(new File(filename));
		modified = false;
		doRead();

		addCmd(
			new CmdLine("read", "   - Read password file into memory")
			{
				public void execute(String[] tokens)
				{
					doRead();
				}
			});
		addCmd(
			new CmdLine("write", "   - Write password file to disk")
			{
				public void execute(String[] tokens)
				{
					doWrite();
				}
			});
		addCmd(
			new CmdLine("adduser", "<username>   - Add new user record")
			{
				public void execute(String[] tokens)
				{
					if (requireTokens(2, tokens))
						doAddUser(tokens[1]);
				}
			});
		addCmd(
			new CmdLine("rmuser", "<username>   - Remove a user record")
			{
				public void execute(String[] tokens)
				{
					if (requireTokens(2, tokens))
						doRmUser(tokens[1]);
				}
			});
		addCmd(
			new CmdLine("passwd", "<username>   - Set new password for user")
			{
				public void execute(String[] tokens)
				{
					if (requireTokens(2, tokens))
						doPasswd(tokens[1]);
				}
			});
		addCmd(
			new CmdLine("show", "<username>   - Show record for user")
			{
				public void execute(String[] tokens)
				{
					if (requireTokens(2, tokens))
						doShow(tokens[1]);
				}
			});
		addCmd(
			new CmdLine("check", "<username>   - Check password for user")
			{
				public void execute(String[] tokens)
				{
					if (requireTokens(2, tokens))
						doCheck(tokens[1]);
				}
			});
		addCmd(
			new CmdLine("addrole", "<username> <role>   - Add a role to user")
			{
				public void execute(String[] tokens)
				{
					if (requireTokens(3, tokens))
						doAddRole(tokens[1], tokens[2]);
				}
			});
		addCmd(
			new CmdLine("rmrole", "<username> <role>   - Remove a role from user")
			{
				public void execute(String[] tokens)
				{
					if (requireTokens(3, tokens))
						doRmRole(tokens[1], tokens[2]);
				}
			});


		addCmd(
			new CmdLine("addprop",
				"<username> <name=value>   - Assigns a property to a user")
			{
				public void execute(String[] tokens)
				{
					if (requireTokens(3, tokens))
						doAddProp(tokens[1], inputLine);
				}
			});

		addCmd(
			new CmdLine("rmprop",
				"<username> <name>   - Removes a property from a user")
			{
				public void execute(String[] tokens)
				{
					if (requireTokens(3, tokens))
						doRmProp(tokens[1], tokens[2]);
				}
			});

		addHelpAndQuitCommands();

		prompt = "> ";
	}

	/**
	* Start editing.
	*/
	void go( ) throws Exception
	{
		processInput();
	}

	private void doRead( )
	{
		try
		{
			if (modified)
			{
				String s = getResponse("Discard changes? ", true);
				if (s == null || (s.charAt(0) != 'y' && s.charAt(0) != 'Y'))
				{
					System.out.println("Read aborted.");
					return;
				}
			}

			passwordFile.read();
			modified = false;
		}
		catch (IOException ex)
		{
			log.atError().setCause(ex).log("Unable to read password file.");
		}
	}

	private void doWrite( )
	{
		try
		{
			passwordFile.write();
			modified = false;
			System.out.println("Wrote " + passwordFile.getFile());
		}
		catch (IOException ex)
		{
			log.atError().setCause(ex).log("Unable to write password file.");
		}
	}

	/**
	* @param user
	*/
	private void doAddUser( String user )
	{
		PasswordFileEntry pfe = passwordFile.getEntryByName(user);
		if (pfe != null)
		{
			System.out.println("A record already exists for user '"
				+ user + "'");
			return;
		}
		try
		{
			pfe = new PasswordFileEntry(user);
			passwordFile.addEntry(pfe);
		}
		catch(AuthException ex)
		{
			log.atError().setCause(ex).log("Unable to add user.");
		}
		if (doPasswd(user))
			modified = true;
		else
			passwordFile.rmEntryByName(user);
	}

	/**
	* @param user
	*/
	private void doRmUser( String user )
	{
		PasswordFileEntry pfe = passwordFile.getEntryByName(user);
		if (pfe == null)
		{
			System.out.println("No such record exists for user '"
				+ user + "'");
			return;
		}
		passwordFile.rmEntryByName(user);
		modified = true;
	}

	/**
	* @param user
	* @return true if password OK.
	*/
	private boolean doPasswd( String user )
	{
		PasswordFileEntry pfe = passwordFile.getEntryByName(user);
		if (pfe == null)
		{
			System.out.println("No such user '" + user + "'");
			return false;
		}
		String s1 = getResponse("Enter Password: ",false);
		String s2 = getResponse("Re-enter Password: ",false);
		if (s1.compareTo(s2) != 0)
		{
			System.out.println("Passwords do not match, record unchanged.");
			return false;
		}
		pfe.setPassword(s1);
		modified = true;
		return true;
	}

	/**
	* @param user
	*/
	private void doShow( String user )
	{
		PasswordFileEntry pfe = passwordFile.getEntryByName(user);
		if (pfe == null)
		{
			System.out.println("No such user '" + user + "'");
			return;
		}
		System.out.println(pfe.toString());
		System.out.println("Properties:");
		for(Object key : pfe.getProperties().keySet())
			System.out.println("   " + key + "=" + pfe.getProperties().getProperty((String)key));
	}

	/**
	* @param user
	*/
	private void doCheck( String user )
	{
		PasswordFileEntry pfe = passwordFile.getEntryByName(user);
		if (pfe == null)
		{
			System.out.println("No such user '" + user + "'");
			return;
		}
		String s = getResponse("Enter Password: ", false);
		if (pfe.matchesPassword(s))
			System.out.println("Passwords match.");
		else
			System.out.println("Passwords are different.");
	}

	/**
	* @param user
	* @param role
	*/
	private void doAddRole( String user, String role )
	{
		PasswordFileEntry pfe = passwordFile.getEntryByName(user);
		if (pfe == null)
		{
			System.out.println("No such user '" + user + "'");
			return;
		}
		if (pfe.isRoleAssigned(role))
		{
			System.out.println("Role '" + role + "' already assigned.");
			return;
		}
		pfe.assignRole(role);
		modified = true;
	}

	/**
	* @param user
	* @param role
	*/
	private void doRmRole( String user, String role )
	{
		PasswordFileEntry pfe = passwordFile.getEntryByName(user);
		if (pfe == null)
		{
			System.out.println("No such user '" + user + "'");
			return;
		}
		if (!pfe.isRoleAssigned(role))
		{
			System.out.println("Role '" + role +"' is not currently assigned.");
			return;
		}
		pfe.removeRole(role);
		modified = true;
	}

	/**
	* @param user
	* @param assignment
	*/
	private void doAddProp( String user, String inputLine )
	{
		PasswordFileEntry pfe = passwordFile.getEntryByName(user);
		if (pfe == null)
		{
			System.out.println("No such user '" + user + "'");
			return;
		}

		// User name has to be the 2nd arg.
		int idx = inputLine.indexOf(user);
		idx += user.length();
		while(Character.isWhitespace(inputLine.charAt(idx)))
			idx++;
		String assignment = inputLine.substring(idx);
		idx = assignment.indexOf('=');
		if (idx == -1)
		{
			System.out.println("Property assignment must be: name=value");
			return;
		}
		pfe.setProperty(assignment.substring(0, idx),
			assignment.substring(idx+1));
		modified = true;
	}

	/**
	* @param user
	* @param propname
	*/
	private void doRmProp( String user, String propname)
	{
		PasswordFileEntry pfe = passwordFile.getEntryByName(user);
		if (pfe == null)
		{
			System.out.println("No such user '" + user + "'");
			return;
		}
		pfe.rmProperty(propname);
		modified = true;
	}

	/**
	* @return true if OK to quit.
	*/
	protected boolean isOkToQuit( )
	{
		if (modified)
		{
			String s = getResponse("Discard changes (y/n): ", true);
			if (s != null && (s.charAt(0) == 'y' || s.charAt(0) == 'Y'))
				return true;
			else
				return false;
		}
		else
			return true;
	}

	/**
	* @param prompt
	* @param echo
	* @return the response
	*/
	private String getResponse(String prompt, boolean echo)
	{
		Console console = System.console();
		if (console != null)
		{
			if (echo) {
				return console.readLine(prompt);
			} else {
				char[] pwd = console.readPassword(prompt);
				if (pwd == null) return null;
				return new String(pwd);
			}
		}
		else
		{
			// console is null (non-interactive env)
			System.out.print(prompt);
			System.out.flush();
			try
			{
				return input.readLine();
			}
			catch (IOException ex)
			{
				log.atError().setCause(ex).log("Error reading input");
			return null;
			}
		}
	}

	// Main Method ===============================================
	static ApplicationSettings settings = new ApplicationSettings();
	static StringToken filearg = new StringToken("f","Password File Name","",TokenOptions.optSwitch,"");
	static
	{
		settings.addToken(filearg);
	}

	/**
	* Usage java ilex.util.PasswordFileEditor -f <file>
	* @param args the arguments
	* @throws Exception on any error, printing stack trace to stdout.
	*/
	public static void main( String[] args ) throws Exception
	{
		settings.parseArgs(args);     // Parse command line args.

		String filestr = filearg.getValue();
		PasswordFileEditor pfe = new PasswordFileEditor(filestr);
		pfe.go();
	}
}