package ilex.util;

import ilex.var.Variable;
import decodes.decoder.FieldParseException;

public class PseudoBinary
{
	/**
	 * Convert double to 3-char pseudobinary. 
	 * @param v value to encode
	 * @param rightDigits number of decimal points to encode
	 * @return 3-char pb string
	 */
	public static String encodePB(double v, int rightDigits)
	{
		for(int i=0; i<rightDigits; i++)
			v *= 10;
		int iv = (int)v;
		return encodePB(iv);
	}

	/**
	 * Convert int to 3-char pseudobinary.
	 * The integer can be a signed number in the range (-131072 ... 131071)
	 * or an unsigned number in the range (0 ... 262143)
	 * @param v value to encode
	 * @param rightDigits number of decimal points to encode
	 * @return 3-char pb string
	 */
	public static String encodePB(int iv)
	{
		StringBuilder sb = new StringBuilder();
		int c1 = ((iv>>12) & 0x3f) + 64;
		if (c1 == 127) c1 = 63;
		int c2 = ((iv>> 6) & 0x3f) + 64;
		if (c2 == 127) c2 = 63;
		int c3 = (iv & 0x3f) + 64;
		if (c3 == 127) c3 = 63;
		sb.append((char)c1);
		sb.append((char)c2);
		sb.append((char)c3);
		String ret = sb.toString();
		return ret;
	}

	/**
	 * Convert pseudobinary string to integer.
	 * There are two flavors: signed (range -2^17 ... (2^17)-1)
	 * and unsigned (range 0 ... (2^18)-1)
	 * @param pb the string
	 * @param signed true if the value can be negative.
	 * @return resulting integer.
	 */
//	public static int decodePB(String pb, boolean signed)
//	{
//		int len = pb.length();
//		if (len > 3) len = 3;
//		int v = 0;
//		for (int i=0; i<len; i++)
//		{
//			char c = pb.charAt(i);
//			int pbv = ((int)c & 0x3f);
////System.out.println("Iteration[" + i + "] char '" + c + "' adding pbv=" + pbv);
//			v = (v<<6) + pbv;
////System.out.println("Result=" + v);
//		}
//		if (signed && (v & 0x20000) != 0)
//		{
////System.out.println("Before OR'ing sign bits v=" + v);
//			v |= 0xfffc0000;
//		}
//		return v;
//	}
	
	public static int decodePB(String pb, boolean signed)
	{
		return decodePB(pb, signed, 0, pb.length());
	}

	public static int decodePB(String pb, boolean signed, int offset, int length)
	{
		if (length > offset + pb.length())
			length = pb.length() - offset;
		
		int result = 0;
		int sign = 1;

		byte[] field = new byte[length];
		for(int i = 0; i<length; i++ )
			field[i] = (byte)pb.charAt(offset + i);
		
		if (signed)  // signed integer
		{
			int c = (int)field[0];
			
			result = (c & 0x1f);
			if ((c & 0x20) != 0)
				sign = -1;
		}
		else
		{
			int c = (int)field[0];
			result = (c & 0x3f);
		}

		for(int i = 1; i < field.length; i++)
		{
			int c = (int)field[i];
			
//System.out.println("field: " + field[i]);
			result <<= 6;
			result += (c & 0x3f);
		}

		if (sign == -1)
		{
			// Compute the 2s compliment
			int numbits = field.length * 6 - 1;
			result = (1<<numbits) - result;
			result = -result;
		}

		return result;
	}
	
	public static void main(String args[])
	{
		char c = args[0].charAt(0);
		if (Character.isDigit(c) || c == '-')
		{
			if (args[0].contains("."))
				System.out.println("" + encodePB(Double.parseDouble(args[0]), 2));
			else
				System.out.println("" + encodePB(Integer.parseInt(args[0])));
		}
		else
		{
			System.out.println("Signed: " + decodePB(args[0], true));
			System.out.println("Unsigned: " + decodePB(args[0], false));
//			System.out.println("Binary: " + Integer.toBinaryString(decodePB(args[0], true)));
//			System.out.println("Hex: " + Integer.toHexString(decodePB(args[0], true)));
//			System.out.println("Binary us: " + Integer.toBinaryString(decodePB(args[0], false)));
//			System.out.println("Hex us: " + Integer.toHexString(decodePB(args[0], false)));
			System.out.println("Again hex: " + Integer.parseInt(args[0], 16));
		}
	}
}
