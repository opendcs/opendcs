package ilex.util;

import java.io.FileInputStream;

public class ParityCheck
{
	private static final boolean oddPar[] = {
	/* 00 */ false, /* 01 */ true, /* 02 */ true, /* 03 */ false,
	/* 04 */ true, /* 05 */ false, /* 06 */ false, /* 07 */ true,
	/* 08 */ true, /* 09 */ false, /* 0A */ false, /* 0B */ true,
	/* 0C */ false, /* 0D */ true, /* 0E */ true, /* 0F */ false,
	/* 10 */ true, /* 11 */ false, /* 12 */ false, /* 13 */ true,
	/* 14 */ false, /* 15 */ true, /* 16 */ true, /* 17 */ false,
	/* 18 */ false, /* 19 */ true, /* 1A */ true, /* 1B */ false,
	/* 1C */ true, /* 1D */ false, /* 1E */ false, /* 1F */ true,
	/* 20 */ true, /* 21 */ false, /* 22 */ false, /* 23 */ true,
	/* 24 */ false, /* 25 */ true, /* 26 */ true, /* 27 */ false,
	/* 28 */ false, /* 29 */ true, /* 2A */ true, /* 2B */ false,
	/* 2C */ true, /* 2D */ false, /* 2E */ false, /* 2F */ true,
	/* 30 */ false, /* 31 */ true, /* 32 */ true, /* 33 */ false,
	/* 34 */ true, /* 35 */ false, /* 36 */ false, /* 37 */ true,
	/* 38 */ true, /* 39 */ false, /* 3A */ false, /* 3B */ true,
	/* 3C */ false, /* 3D */ true, /* 3E */ true, /* 3F */ false,
	/* 40 */ true, /* 41 */ false, /* 42 */ false, /* 43 */ true,
	/* 44 */ false, /* 45 */ true, /* 46 */ true, /* 47 */ false,
	/* 48 */ false, /* 49 */ true, /* 4A */ true, /* 4B */ false,
	/* 4C */ true, /* 4D */ false, /* 4E */ false, /* 4F */ true,
	/* 50 */ false, /* 51 */ true, /* 52 */ true, /* 53 */ false,
	/* 54 */ true, /* 55 */ false, /* 56 */ false, /* 57 */ true,
	/* 58 */ true, /* 59 */ false, /* 5A */ false, /* 5B */ true,
	/* 5C */ false, /* 5D */ true, /* 5E */ true, /* 5F */ false,
	/* 60 */ false, /* 61 */ true, /* 62 */ true, /* 63 */ false,
	/* 64 */ true, /* 65 */ false, /* 66 */ false, /* 67 */ true,
	/* 68 */ true, /* 69 */ false, /* 6A */ false, /* 6B */ true,
	/* 6C */ false, /* 6D */ true, /* 6E */ true, /* 6F */ false,
	/* 70 */ true, /* 71 */ false, /* 72 */ false, /* 73 */ true,
	/* 74 */ false, /* 75 */ true, /* 76 */ true, /* 77 */ false,
	/* 78 */ false, /* 79 */ true, /* 7A */ true, /* 7B */ false,
	/* 7C */ true, /* 7D */ false, /* 7E */ false, /* 7F */ true,
	/* 80 */ true, /* 81 */ false, /* 82 */ false, /* 83 */ true,
	/* 84 */ false, /* 85 */ true, /* 86 */ true, /* 87 */ false,
	/* 88 */ false, /* 89 */ true, /* 8A */ true, /* 8B */ false,
	/* 8C */ true, /* 8D */ false, /* 8E */ false, /* 8F */ true,
	/* 90 */ false, /* 91 */ true, /* 92 */ true, /* 93 */ false,
	/* 94 */ true, /* 95 */ false, /* 96 */ false, /* 97 */ true,
	/* 98 */ true, /* 99 */ false, /* 9A */ false, /* 9B */ true,
	/* 9C */ false, /* 9D */ true, /* 9E */ true, /* 9F */ false,
	/* A0 */ false, /* A1 */ true, /* A2 */ true, /* A3 */ false,
	/* A4 */ true, /* A5 */ false, /* A6 */ false, /* A7 */ true,
	/* A8 */ true, /* A9 */ false, /* AA */ false, /* AB */ true,
	/* AC */ false, /* AD */ true, /* AE */ true, /* AF */ false,
	/* B0 */ true, /* B1 */ false, /* B2 */ false, /* B3 */ true,
	/* B4 */ false, /* B5 */ true, /* B6 */ true, /* B7 */ false,
	/* B8 */ false, /* B9 */ true, /* BA */ true, /* BB */ false,
	/* BC */ true, /* BD */ false, /* BE */ false, /* BF */ true,
	/* C0 */ false, /* C1 */ true, /* C2 */ true, /* C3 */ false,
	/* C4 */ true, /* C5 */ false, /* C6 */ false, /* C7 */ true,
	/* C8 */ true, /* C9 */ false, /* CA */ false, /* CB */ true,
	/* CC */ false, /* CD */ true, /* CE */ true, /* CF */ false,
	/* D0 */ true, /* D1 */ false, /* D2 */ false, /* D3 */ true,
	/* D4 */ false, /* D5 */ true, /* D6 */ true, /* D7 */ false,
	/* D8 */ false, /* D9 */ true, /* DA */ true, /* DB */ false,
	/* DC */ true, /* DD */ false, /* DE */ false, /* DF */ true,
	/* E0 */ true, /* E1 */ false, /* E2 */ false, /* E3 */ true,
	/* E4 */ false, /* E5 */ true, /* E6 */ true, /* E7 */ false,
	/* E8 */ false, /* E9 */ true, /* EA */ true, /* EB */ false,
	/* EC */ true, /* ED */ false, /* EE */ false, /* EF */ true,
	/* F0 */ false, /* F1 */ true, /* F2 */ true, /* F3 */ false,
	/* F4 */ true, /* F5 */ false, /* F6 */ false, /* F7 */ true,
	/* F8 */ true, /* F9 */ false, /* FA */ false, /* FB */ true,
	/* FC */ false, /* FD */ true, /* FE */ true, /* FF */ false
	};

	public static boolean isOddParity(int b)
	{
		return oddPar[b&0xff];
	}

	public static boolean isEvenParity(int b)
	{
		return !isOddParity(b);
	}
	
	/**
	 * Usage: decj ilex.util.ParityCheck [e|o] FileName
	 * @param args
	 */
	public static void main(String args[])
		throws Exception
	{
		if (args.length != 2)
		{
			System.err.println("Args should be [e|o] filename");
			System.exit(1);
		}
		boolean odd = true;
		if (args[0].startsWith("o"))
			odd = true;
		else if (args[0].startsWith("e"))
			odd = false;
		else 
		{
			System.err.println("parity arg should be 'o' or 'e'");
			System.exit(1);
		}
		FileInputStream fis = new FileInputStream(args[1]);
		int numErrs = 0;
		int c;
		while((c = fis.read()) != -1)
		{
			if ((odd && !isOddParity(c))
			 || (!odd && !isEvenParity(c)))
			{
				numErrs++;
				System.out.print('$');
			}
			else
				System.out.print((char)(c & 0x7f));
		}
		System.out.println("");
		System.out.println("Total of " + numErrs + " parity errors.");
		System.out.println("Parity errors in above text have been replaced with '$'.");
	}
}
