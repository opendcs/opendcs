package decodes.test;

import java.io.IOException;
import java.util.ArrayList;

public class JvmTest
{
	public static void main(String args[]) throws Exception
	{
		JvmTest jvmTest = new JvmTest();
		jvmTest.run(args);
	}

	private void run(String[] args) throws Exception
	{
		ArrayList<String> argl = new ArrayList<String>();
		System.out.print("Args:");
		for(int idx=0; idx<args.length; idx++)
		{
			argl.add(args[idx]);
			System.out.print("'" + args[idx] + "' ");
		}
		System.out.println();
		
		ProcessBuilder pb = new ProcessBuilder();
		pb.command(argl);
		
		System.out.println("Starting...");
		Process child = pb.start();
		child.waitFor();
		System.out.println("JvmTest - Child finished.");
	}
	
}
