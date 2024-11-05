package lrgs.drgsrecv;

import java.io.*;

import lrgs.common.DcpAddress;
import decodes.util.Pdt;
import decodes.util.PdtEntry;


public class CompileDrgsStats
{
	public CompileDrgsStats()
	{
	}

	public static void main(String args[])
		throws Exception
	{
		if (args.length < 3)
		{
			System.err.println("Usage: prog file start end [chan list ...]");
			System.exit(0);
		}
		String fn = args[0];
		long starttime = Long.parseLong(args[1]);
		long endtime = Long.parseLong(args[2]);
		int chans[] = null;
		if (args.length > 3)
		{
			chans = new int[args.length-3];
			for(int i=3; i<args.length; i++)
				chans[i-3] = Integer.parseInt(args[i]);
		}

		System.out.println("Input file: " + fn);
		System.out.println("Time Range: " + starttime + " ... " + endtime);
		if (chans == null)
			System.out.println("  Channels: NO FILTERING");
		else
		{
			System.out.print("  Channels:");
			for(int i=0; i < chans.length; i++)
				System.out.print(" " + chans[i]);
			System.out.println("");
		}
		System.out.println("");

		LineNumberReader lnr = new LineNumberReader(new FileReader(fn));
		int n100 = 0;
		int n300 = 0;
		int n1200 = 0;
		int n100AnyErrs = 0;
		int n300AnyErrs = 0;
		int n1200AnyErrs = 0;
		int n100ParErrs = 0;
		int n300ParErrs = 0;
		int n1200ParErrs = 0;
		int n100NoEots = 0;
		int n300NoEots = 0;
		int n1200NoEots = 0;
		double totCompRat0_50 = 0.0;
		double totCompRat51_100 = 0.0;
		double totCompRat101_200 = 0.0;
		double totCompRat201_300 = 0.0;
		double totCompRat301plus = 0.0;
		int n0_50 = 0;
		int n51_100 = 0;
		int n101_200 = 0;
		int n201_300 = 0;
		int n301plus = 0;
		int n100Par1st10 = 0;
		int n300Par1st10 = 0;
		int n1200Par1st10 = 0;
		int n100Par11_25 = 0;
		int n300Par11_25 = 0;
		int n1200Par11_25 = 0;
		int n100Par26_50 = 0;
		int n300Par26_50 = 0;
		int n1200Par26_50 = 0;
		int n100Par50p = 0;
		int n300Par50p = 0;
		int n1200Par50p = 0;
		int nInvalidAddr100 = 0;
		int nInvalidAddr300 = 0;
		int nInvalidAddr1200 = 0;
		int n100dailyAssigns = 0;
		int n300dailyAssigns = 0;
		int n1200dailyAssigns = 0;

		Pdt pdt = Pdt.instance();
		if (!pdt.load(new File("pdts_compressed.txt")))
		{
			pdt = null;
		}
		else
		{
			for(PdtEntry pe : pdt.getEntries())
			{
//				if (pe.active_flag != 'Y' || pe.st_xmit_interval == 0)
				if (pe.st_xmit_interval == 0)
					continue;
				int numDaily = 3600*24 / pe.st_xmit_interval;
				if (isInList(chans, pe.st_channel))
				{
					if (pe.baud == 100) n100dailyAssigns += numDaily;
					if (pe.baud == 300) n300dailyAssigns += numDaily;
					if (pe.baud == 1200) n1200dailyAssigns += numDaily;
				}
			}
		}
		System.out.println("PDT checking will " + (pdt == null ? "NOT" : "")
			+ " be done.");

		String line;
		while((line = lnr.readLine()) != null)
		{
			if (line.startsWith("Addr"))
				continue; // header line
			String[] words = line.split("\t");
			if (words != null && words.length > 0)
			{
				try
				{
					long timestamp = Long.parseLong(words[1]);
					if (timestamp < starttime || timestamp > endtime)
						continue;

					int chan = Integer.parseInt(words[6]);
					if (chans != null && !isInList(chans, chan))
						continue;
					int length = Integer.parseInt(words[2]);
					char type = words[3].charAt(0);
					boolean parErr = words[4].charAt(0) == '?';
					boolean noEot = words[5].charAt(0) != 'Y';
					int baud = Integer.parseInt(words[7]);
//					double compRatio = Double.parseDouble(words[8]);
					int firstParPos = Integer.parseInt(words[11]);

					DcpAddress dcpAddress = new DcpAddress(words[0]);
//					long addr = Long.parseLong(words[0], 16);
					boolean validAddr = 
						(pdt == null || pdt.find(dcpAddress) != null);

					if (baud == 100)
					{
						n100++;
						if (parErr)
						{
							n100ParErrs++;
							if (firstParPos <= 10) n100Par1st10++;
							else if (firstParPos <= 25) n100Par11_25++;
							else if (firstParPos <= 50) n100Par26_50++;
							else n100Par50p++;
						}
						if (noEot) n100NoEots++;
						if (!validAddr) nInvalidAddr100++;
						if (parErr || noEot || !validAddr) n100AnyErrs++;
					}
					else if (baud == 300)
					{
						n300++;
						if (parErr) 
						{
							n300ParErrs++;
							if (firstParPos <= 10) n300Par1st10++;
							else if (firstParPos <= 25) n300Par11_25++;
							else if (firstParPos <= 50) n300Par26_50++;
							else n100Par50p++;
						}
						if (noEot) n300NoEots++;
						if (!validAddr) nInvalidAddr300++;
						if (parErr || noEot || !validAddr) n300AnyErrs++;
					}
					else if (baud == 1200)
					{
						n1200++;
						if (parErr)
						{
							n1200ParErrs++;
							if (firstParPos <= 10) n1200Par1st10++;
							else if (firstParPos <= 25) n1200Par11_25++;
							else if (firstParPos <= 50) n1200Par26_50++;
						}
						if (noEot) n1200NoEots++;
						if (!validAddr) nInvalidAddr1200++;
						if (parErr || noEot || !validAddr) n1200AnyErrs++;
					}
					if (length <= 50)
					{
//						totCompRat0_50 += compRatio;
						n0_50++;
					}
					else if (length <= 100)
					{
//						totCompRat51_100 += compRatio;
						n51_100++;
					}
					else if (length <= 200)
					{
//						totCompRat101_200 += compRatio;
						n101_200++;
					}
					else if (length <= 300)
					{
//						totCompRat201_300 += compRatio;
						n201_300++;
					}
					else
					{
//						totCompRat301plus += compRatio;
						n301plus++;
					}
				}
				catch(Exception ex)
				{
					System.err.println("Error on line " + lnr.getLineNumber()
						+ ": " + ex);
				}
			}
		}
		System.out.println("Statistics for 100 baud messages:");
		System.out.println("              # Messages: " + n100);
		System.out.println("           # Assignments: " + n100dailyAssigns);
		System.out.println("               # ParErrs: " + n100ParErrs);
		System.out.println("               # No-EOTs: " + n100NoEots);
		System.out.println("      #Invalid DCP Addrs: " + nInvalidAddr100);
		System.out.println("          #100b Any-Errs: " + n100AnyErrs);
	if (n100dailyAssigns != 0)
	{
		System.out.println("   % of assigns Received: " +
			(((double)n100 / n100dailyAssigns) * 100.0));
		System.out.println("      % of assigns w/Err: " +
			(((double)n100AnyErrs / n100dailyAssigns) * 100.0));
	}
		System.out.println("      # w/Par Err 0...10: " + n100Par1st10);
		System.out.println("     # w/Par Err 11...25: " + n100Par11_25);
		System.out.println("     # w/Par Err 26...50: " + n100Par26_50);
		System.out.println("     # w/Par Err after50: " + n100Par50p);
		
		System.out.println("");
		System.out.println("Statistics for 300 baud messages:");
		System.out.println("              # Messages: " + n300);
		System.out.println("           # Assignments: " + n300dailyAssigns);
		System.out.println("               # ParErrs: " + n300ParErrs);
		System.out.println("               # No-EOTs: " + n300NoEots);
		System.out.println("      #Invalid DCP Addrs: " + nInvalidAddr300);
		System.out.println("              # Any-Errs: " + n300AnyErrs);
	if (n300dailyAssigns != 0)
	{
		System.out.println("   % of assigns Received: " +
			(((double)n300 / n300dailyAssigns) * 100.0));
		System.out.println("      % of assigns w/Err: " +
			(((double)n300AnyErrs / n300dailyAssigns) * 100.0));
	}
		System.out.println("      # w/Par Err 0...10: " + n300Par1st10);
		System.out.println("     # w/Par Err 11...25: " + n300Par11_25);
		System.out.println("     # w/Par Err 26...50: " + n300Par26_50);
		System.out.println("     # w/Par Err after50: " + n300Par50p);
		
		System.out.println("");
		System.out.println("Statistics for 1200 baud messages:");
		System.out.println("              # Messages: " + n1200);
		System.out.println("           # Assignments: " + n1200dailyAssigns);
		System.out.println("               # ParErrs: " + n1200ParErrs);
		System.out.println("               # No-EOTs: " + n1200NoEots);
		System.out.println("      #Invalid DCP Addrs: " + nInvalidAddr1200);
		System.out.println("         #1200b Any-Errs: " + n1200AnyErrs);
	if (n1200dailyAssigns != 0)
	{
		System.out.println("   % of assigns Received: " +
			(((double)n1200 / n1200dailyAssigns) * 100.0));
		System.out.println("      % of assigns w/Err: " +
			(((double)n1200AnyErrs / n1200dailyAssigns) * 100.0));
	}
		System.out.println("      # w/Par Err 0...10: " + n1200Par1st10);
		System.out.println("     # w/Par Err 11...25: " + n1200Par11_25);
		System.out.println("     # w/Par Err 26...50: " + n1200Par26_50);
		System.out.println("     # w/Par Err after50: " + n1200Par50p);
		
//		System.out.println("");
//		System.out.println("GZIP Compression Ration Statistics");
//		System.out.println("    # & ave-comprat 0...50: " + n0_50 + " "
//			+ (n0_50 == 0 ? 0.0 : (totCompRat0_50 / (double)n0_50)));
//		System.out.println("  # & ave-comprat 51...100: " + n51_100 + " "
//			+ (n51_100 == 0 ? 0.0 : (totCompRat51_100 / (double)n51_100)));
//		System.out.println(" # & ave-comprat 101...200: " + n101_200 + " "
//			+ (n101_200 == 0 ? 0.0 : (totCompRat101_200 / (double)n101_200)));
//		System.out.println(" # & ave-comprat 201...300: " + n201_300 + " "
//			+ (n201_300 == 0 ? 0.0 : (totCompRat201_300 / (double)n201_300)));
//		System.out.println("      # & ave-comprat >300: " + n301plus + " "
//			+ (n301plus == 0 ? 0.0 : (totCompRat301plus / (double)n301plus)));
	}

	static boolean isInList(int chans[], int chan)
	{
		for(int i=0; i<chans.length; i++)
			if (chan == chans[i])
				return true;
		return false;
	}
		
}
