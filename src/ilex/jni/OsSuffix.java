/*
* Copyright 2008 Ilex Engineering, Inc. - All Rights Reserved.
* No part of this file may be duplicated in either hard-copy or electronic
* form without specific written permission.
* 
 * 2014 Notice: Cove Software, LLC believes the above copyright notice to be
 * in error. This module was 100% funded by the U.S. Federal Government under
 * contracts requiring that it be Government-Owned. It has been delivered to
 * U.S. Bureau of Reclamation, U.S. Geological Survey, and U.S. Army Corps of
 * Engineers under contract.

*/
package ilex.jni;

import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.IOException;

/**
Determines the correct Shared Object Library suffix to use when loading
a native library.
*/
public class OsSuffix
{
	public static String getOsSuffix()
	{
		String osn = System.getProperty("os.name");
		if (osn == null)
			return "unknown";
		osn = osn.toLowerCase();
		if (osn.startsWith("win"))
			return "win";
		else if (osn.startsWith("sunos"))
			return "sol10";
		try
		{
			Process uname = Runtime.getRuntime().exec("uname -rp");
			InputStreamReader isr = new InputStreamReader(
				uname.getInputStream());
			BufferedReader bis = new BufferedReader(isr);
			String line = bis.readLine();

			// RHEL3 is Kernel version 2.4.xxxxx
			if (line.startsWith("2.4")) 
				return "el3.32";
			int bits = 32;
			String n = System.getProperty("sun.arch.data.model");
			if (n != null && n.contains("64"))
				bits = 64;
			int rhelVersion = line.contains("el5") ? 5 : 4;
			return "el" + rhelVersion + "." + bits;
		}
		catch(IOException ex)
		{
			return "unknown";
		}
	}

	public static void main(String args[])
	{
		System.out.println(getOsSuffix());
	}
}
