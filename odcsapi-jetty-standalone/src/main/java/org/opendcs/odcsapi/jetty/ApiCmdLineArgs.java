/*
 *  Copyright 2023 OpenDCS Consortium
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.opendcs.odcsapi.jetty;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.opendcs.odcsapi.start.StartException;

public class ApiCmdLineArgs
{
	private String context = "odcsapi";
	private int httpPort = -1;
	
	private int httpsPort = -1;
	private String keyStorePath;
	private String keyStorePassword;
	
	private String decodesPropFile = "$DCSTOOL_USERDIR/user.properties";
	private boolean secureMode = false;
	private String corsFile = "$DCSTOOL_HOME/opendcs_api_cors.cfg";
	
	private enum State { IDLE, EXPECT_CONTEXT, EXPECT_HTTPPORT,  EXPECT_HTTPSPORT, EXPECT_KEYSTOREPATH, EXPECT_KEYSTOREPASSWORD, EXPECT_PROPFILE, EXPECT_CORS };
	private State state = State.IDLE;
	private String splitArg = null;
	
	public void parseArgs(String[] args)
			throws StartException
	{
		state = State.IDLE;
		splitArg = null;
		System.out.println("ALL Args: " + args.toString());
		for(String arg : args)
		{
			System.out.println("First Arg: " + arg);
			parseArg(arg);
			System.out.println("Arg after parse: " + arg);
			if (splitArg != null)
			{
				System.out.println("Split Arg Before Parse: " + splitArg);
				parseArg(splitArg);
				splitArg = null;
			}
		}
		if (state != State.IDLE)
			throw new StartException("Incomplete arguments. Parser left in state " + state.toString());
	}
	
	private void parseArg(String arg)
		throws StartException
	{
		switch(state)
		{
			case IDLE:
				System.out.println("IDLE: Arg: " + arg);
				if (arg.startsWith("-cors"))
				{
					System.out.println("-cors argument found.");
					state = State.EXPECT_CORS;
					System.out.println("Arg: " + arg);
					if (arg.length() > 5)
						splitArg = arg.substring(5);
					System.out.println("Split Arg: " + splitArg);
				}
				else if (arg.startsWith("-c"))
				{
					System.out.println("-c argument found.");
					state = State.EXPECT_CONTEXT;
					System.out.println("Arg: " + arg);
					if (arg.length() > 2)
						splitArg = arg.substring(2);
					System.out.println("Split Arg: " + splitArg);
				}
				else if (arg.startsWith("-p"))
				{
					state = State.EXPECT_HTTPPORT;
					if (arg.length() > 2)
						splitArg = arg.substring(2);
				}
				else if (arg.startsWith("-sp"))
				{
					state = State.EXPECT_HTTPSPORT;
					if (arg.length() > 3)
						splitArg = arg.substring(3);
				}
				else if (arg.startsWith("-key"))
				{
					state = State.EXPECT_KEYSTOREPATH;
					if (arg.length() > 4)
						splitArg = arg.substring(4);
				}
				else if (arg.startsWith("-kp"))
				{
					state = State.EXPECT_KEYSTOREPASSWORD;
					if (arg.length() > 3)
						splitArg = arg.substring(3);
				}
				else if (arg.startsWith("-P"))
				{
					state = State.EXPECT_PROPFILE;
					if (arg.length() > 2)
						splitArg = arg.substring(2);
				}
				else if (arg.startsWith("-s"))
					secureMode = true;
				else
					throw new StartException("Unknown argument '" + arg + "'. Cannot start.");
				break;
			case EXPECT_CONTEXT:
			{
				String t = arg.trim();
				for(int idx = 0; idx < t.length(); idx++)
				{
					char c = t.charAt(idx);
					if (!Character.isLetterOrDigit(c) && c != '_' && c != '-')
						throw new StartException("Illegal character '" + c + " at position "
							+ idx + " of context argument. Cannot start.");
				}
				context = t;
				state = State.IDLE;
				break;
			}
			case EXPECT_HTTPPORT:
				try { httpPort = Integer.parseInt(arg.trim()); }
				catch(NumberFormatException ex)
				{
					throw new StartException("Invalid httpPort argument '" + arg 
						+ "' after -p. Must be integer.");
				}
				state = State.IDLE;
				break;
			case EXPECT_HTTPSPORT:
				try { httpsPort = Integer.parseInt(arg.trim()); }
				catch(NumberFormatException ex)
				{
					throw new StartException("Invalid httpsPort argument '" + arg 
						+ "' after -sp. Must be integer.");
				}
				state = State.IDLE;
				break;
			case EXPECT_KEYSTOREPATH:
				keyStorePath = arg.trim();
				Path pth = Paths.get(keyStorePath);
				boolean kspExists = Files.exists(pth);
				if (!kspExists)
				{
					throw new StartException("Invalid keyStorePath file '" + arg 
							+ "' after -key. This file does not exist.");
				}
				state = State.IDLE;
				break;
			case EXPECT_KEYSTOREPASSWORD:
				keyStorePassword = arg.trim();
				if (keyStorePassword.length() <= 0)
				{
					throw new StartException("You must enter a keystore password after -kp.");
				}
				state = State.IDLE;
				break;
			case EXPECT_CORS:
				System.out.println("Cors File!!!");
				System.out.println("Whole Arg: " + arg);
				System.out.println("Split Arg: " + splitArg);
				corsFile = arg.trim();
				System.out.println("Cors File: " + corsFile);
				Path corsPath = Paths.get(corsFile);
				boolean fExists = Files.exists(corsPath);
				if (!fExists)
				{
					throw new StartException("Invalid cors file '" + arg 
							+ "' after -cors. This file does not exist.");
				}
				state = State.IDLE;
				break;
			case EXPECT_PROPFILE:
				decodesPropFile = arg.trim();
				state = State.IDLE;
				break;
			default:
				throw new StartException("Command line state parser confused. Cannot start.");
		}
	}

	public String getContext()
	{
		return context;
	}

	public void setContext(String context)
	{
		this.context = context;
	}

	public int getHttpPort()
	{
		return httpPort;
	}

	public void setHttpPort(int port)
	{
		this.httpPort = httpPort;
	}

	public int getHttpsPort()
	{
		return httpsPort;
	}

	public void setHttpsPort(int port)
	{
		this.httpsPort = httpsPort;
	}

	public String getKeyStorePath()
	{
		return keyStorePath;
	}

	public void setKeyStorePath(String keyStorePath)
	{
		this.keyStorePath = keyStorePath;
	}

	public String getKeyStorePassword()
	{
		return keyStorePassword;
	}

	public void setKeyStorePassword(String keyStorePassword)
	{
		this.keyStorePassword = keyStorePassword;
	}
	
	public String getDecodesPropFile()
	{
		return decodesPropFile;
	}

	public void setDecodesPropFile(String decodesPropFile)
	{
		this.decodesPropFile = decodesPropFile;
	}

	public String getCorsFile()
	{
		return corsFile;
	}

	public void setCorsFile(String corsFile)
	{
		this.corsFile = corsFile;
	}
	
	public boolean isSecureMode()
	{
		return secureMode;
	}

}
