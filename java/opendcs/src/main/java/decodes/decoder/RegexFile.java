package decodes.decoder;

import ilex.util.FileUtil;

import java.io.Console;
import java.io.File;
import java.io.FileReader;
import java.io.LineNumberReader;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class RegexFile
{
	/**
	 * Pattern string for standard self-describing ascii message.
	 * Groups:
	 * <ul>
	 *   <li>1: Entire sensor block</li>
	 *   <li>2: sensor label</li>
	 *   <li>3: minute offset</li>
	 *   <li>4: minute interval</li>
	 *   <li>5: sensor block</li>
	 *   <li>6: sensor value (multiple times per block)</li>
	 * </ul>
	 */
	static String asciiSelfDesc = 
		"[^:]*(:(\\w+)\\s+(\\d+)\\s*#(\\d+)((?:\\s*([+-]?\\d*\\.?\\d+))+))+";
	
	
	public static void main(String[] args)
		throws Exception
	{
		boolean multiline = false;
		boolean asd = false;
		String filename = null;
		for(String arg : args)
			if (arg.equals("m"))
				multiline = true;
			else if (arg.equals("asd"))
				asd = true;
			else if (arg.startsWith("file:"))
				filename = arg.substring(5);
		
		Pattern pattern;

		Console console = System.console();
		if (asd)
		{
			pattern = Pattern.compile(asciiSelfDesc);
		}
		else
		{
			pattern = 
				multiline 
				? Pattern.compile(console.readLine("%nEnter your regex: "),
					Pattern.MULTILINE)
				: Pattern.compile(console.readLine("%nEnter your regex: "));
		}

		if (filename == null)
			filename = console.readLine("Enter name of input file: ");
		
		File file = new File(filename);
		Matcher matcher = null;
		boolean found = false;
		if (asd)
		{
			LineNumberReader lnr = new LineNumberReader(
				new FileReader(file));
			String line;
			found = false;
			while((line = lnr.readLine()) != null)
			{
				System.out.println(line);
				matcher = pattern.matcher(line.substring(37));
				while (matcher.find()) 
				{
					System.out.println("found text '" + matcher.group()
						+ "' from idx: " + matcher.start() + "," + matcher.end());
					for(int idx=0; idx < matcher.groupCount()+1; idx++)
						System.out.println("group[" + idx + "] '" + matcher.group(idx) + "'");
					found = true;
				}
				if(!found)
				{
					System.out.println("No match found.");
				}
				System.out.println("=========");
			}
		}
		else
		{
			String contents = FileUtil.getFileContents(file);
			matcher = pattern.matcher(contents);
			while (matcher.find()) 
			{
				console.format("I found the text \"%s\" starting at " +
				   "index %d and ending at index %d.%n",
					matcher.group(), matcher.start(), matcher.end());
				for(int idx=0; idx < matcher.groupCount()+1; idx++)
					console.format("group[%d]: '%s'\n", idx, matcher.group(idx));
				found = true;
			}
			if(!found)
			{
				console.format("No match found.%n");
			}
		}
	}
}

