/**
 * $Id$
 * 
 * $Log$
 *
 * Copyright 2014 Cove Software, LLC
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package opendcs.dbupdate;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class SQLReader
{
	private String path = null;
	private ArrayList<String> listOfQueries = new ArrayList<String>();
	private StringBuilder queryLine = new StringBuilder();
	
	public SQLReader(String path)
	{
		this.path = path;
	}
	
	private void subtractOne()
	{
		if (queryLine.length() > 0)
			queryLine.setLength(queryLine.length() - 1);
	}
	
	private int endChar()
	{
		return queryLine.length() > 0 ? queryLine.charAt(queryLine.length()-1) : 0;
	}
	
	/*
	 * @param path Path to the SQL file
	 * 
	 * @return List of query strings
	 */
	public ArrayList<String> createQueries()
		throws IOException
	{
		listOfQueries = new ArrayList<String>();
		queryLine.setLength(0);
		FileReader fr = new FileReader(new File(path));
		BufferedReader br = new BufferedReader(fr);
		
		boolean inComment = false;
		String commentStart = null;

		int c = 0, prev = 0;
		while((c = br.read()) != -1)
		{
			if (inComment)
			{
				if (c == '-' && prev == '-' && commentStart.equals("--"))
					inComment = false;
				else if (prev == '*' && c == '/' && commentStart.equals("/*"))
					inComment = false;
				// Also, newlines terminate -- comments
				else if ((c == '\n' || c == '\r') && commentStart.equals("--"))
					inComment = false;
			}
			else
			{
				if (c == '-' && prev == '-')
				{
					inComment = true;
					subtractOne();
					commentStart = "--";
				}
				else if (prev == '/' && c == '*')
				{
					inComment = true;
					subtractOne();
					commentStart = "/*";
				}
				else
				{
					// Collapse contiguous whitespace & line breaks to a single space.
					if (Character.isWhitespace(c))
					{
						if (endChar() != ' ' && queryLine.length() > 0)
							queryLine.append(' ');
					}
					else if (c == ';')
					{
						if (queryLine.length() > 0)
						{
							listOfQueries.add(queryLine.toString());
							queryLine.setLength(0);
						}
					}
					else // plain old char
						queryLine.append((char)c);
				}
			}
			prev = c;
		}
		br.close();
		return listOfQueries;
	}
	
	public static void main(String args[])
		throws Exception
	{
		SQLReader sr = new SQLReader(args[0]);
		ArrayList<String> qs = sr.createQueries();
		int n = 0;
		for(String q : qs)
			System.out.println("" + (++n) + ": " + q);
	}
}
