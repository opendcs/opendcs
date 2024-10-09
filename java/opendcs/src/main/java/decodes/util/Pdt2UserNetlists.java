package decodes.util;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import ilex.util.EnvExpander;
import lrgs.common.NetworkList;
import lrgs.common.NetworkListItem;

public class Pdt2UserNetlists
{

	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		if (args.length != 2)
		{
			System.err.println("usage: decj Pdt2UserNetlists <pdtFileName> <netlistDirName>");
			System.exit(1);
		}
		String filename = EnvExpander.expand(args[0]);
		
		Pdt pdt = new Pdt();
		if (!pdt.load(new File(filename)))
		{
			System.err.println("Cannot open pdt file '" + filename + "'");
			System.err.println("usage: decj Pdt2UserNetlists <pdtFileName> <netlistDirName>");
			System.exit(1);
		}
		
		String nlDirName = EnvExpander.expand(args[1]);
		File nlDir = new File(nlDirName);
		if (!nlDir.isDirectory())
		{
			System.err.println("Cannot netlist dirctory '" + nlDirName + "' does not exist.");
			System.err.println("usage: decj Pdt2UserNetlists <pdtFileName> <netlistDirName>");
			System.exit(1);
		}
			
		HashMap<String, NetworkList> agencyNetlistMap = new HashMap<String, NetworkList>();
		for(PdtEntry pdtEntry : pdt.getEntries())
		{
			NetworkList nl = agencyNetlistMap.get(pdtEntry.agency);
			if (nl == null)
			{
				nl = new NetworkList();
				nl.file = new File(nlDirName, pdtEntry.agency + ".nl");
				agencyNetlistMap.put(pdtEntry.agency, nl);
			}
			StringBuilder name = new StringBuilder(pdtEntry.getDescription());
			for(int i=0; i<name.length(); i++)
				if (name.charAt(i) == ' ')
					name.setCharAt(i, '_');
				else if (name.charAt(i) == ',')
					name.deleteCharAt(i);
			
			NetworkListItem nli = new NetworkListItem(
				pdtEntry.dcpAddress, name.toString(), pdtEntry.getDescription());
			nl.add(nli);
		}

		for(NetworkList nl: agencyNetlistMap.values())
			try { nl.saveFile(); }
			catch(IOException ex)
			{
				System.err.println("Cannot save netlist '" + nl.file.getPath() + "': " + ex);
			}
	}

}
