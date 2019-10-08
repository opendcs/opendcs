/*
*	$Id$
*/
package decodes.decoder;

import ilex.var.Variable;

import java.text.DecimalFormat;

import ilex.util.PseudoBinary;
import ilex.var.NoConversionException;

/**
This class handles the parsing of numbers from the raw data. It knows
how to handle string, labarge, hex, and several flavors of pseudo-binary
formats.
*/
public class NumberParser
{
	private char dataType;
	private int pbinaryMask;

	// Constants for datatype:

	/** ASCII data format */
	public static final char ASCII_FMT               = 'a';
	/** ASCII data format that allows numbers of length zero */
	public static final char ASCII_ZERO				 = 'z';
	/** Unsigned Pseudobinary data format */
	public static final char PBINARY_FMT             = 'b'; // Pseudo binary
	/** Signed 2's compliment pseudobinary data format */
	public static final char SIGNED_PBINARY_FMT      = 'i';
	/** Old Labarge data format */
	public static final char LABARGE_FMT             = 'l';
	/** Hex string data format */
	public static final char HEX_FMT                 = 'x';
	/** String data format for non-numeric data */
	public static final char STRING_FMT              = 's';
	/** Campbell Scientific floating point Pseudobinary data format */
	public static final char CAMPBELL_BINARY_FMT     = 'c';
	/** Design Analysis signbit Pseudobinary data format */
	public static final char SIGNBIT_BINARY_FMT      = 'o';
	/** Placeholder for invalid format */
	public static final char UNKNOWN_FMT             = 'u';
	/** Pure binary 2's compliment MSB-first */
	public static final char BIN_SIGNED_MSB          = 'd';
	/** Pure binary Unsigned MSB-first */
	public static final char BIN_UNSIGNED_MSB        = 'e';
	/** Pure binary 2's compliment MSB-first */
	public static final char BIN_SIGNED_LSB          = 'f';
	/** Pure binary Unsigned MSB-first */
	public static final char BIN_UNSIGNED_LSB        = 'g';
	/** Pseudobinary Aton message */
	public static final char PBINARY_ATON 			 = 'n';
	/** Pseudobinary Sontek message */
	public static final char PBINARY_SONTEK			 = 'k';
	
	/** default constructor */
	public NumberParser()
	{
		// setup defaults
		dataType = ASCII_FMT;
		pbinaryMask = 0x3f;		/* low order 6 bits */
	}

	/**
	  Sets the datatype to one of the valid codes.
	  @param  type a valid data type code
	*/
	public void setDataType( char type )
		throws ScriptFormatException
	{
		type = Character.toLowerCase(type);
		if (type != ASCII_FMT
		 && type != ASCII_ZERO
		 && type != PBINARY_FMT
		 && type != SIGNED_PBINARY_FMT
		 && type != LABARGE_FMT
		 && type != HEX_FMT
		 && type != STRING_FMT
		 && type != CAMPBELL_BINARY_FMT
		 && type != SIGNBIT_BINARY_FMT
		 && type != BIN_SIGNED_MSB
		 && type != BIN_UNSIGNED_MSB
		 && type != BIN_SIGNED_LSB
		 && type != BIN_UNSIGNED_LSB
		 && type != PBINARY_ATON
		 && type != PBINARY_SONTEK)
			throw new ScriptFormatException(
				"Unknown field data-type '" + type + "'");
		dataType = type;
	}

	/** @return the data type code */
	public char getDataType() { return dataType; }

	/** 
	  Do not call this method.
	*/
	public void setMask(int m) { pbinaryMask = m; }

	/**
	  Parses an integer value from the passed field data.
	  @param field the raw data extracted from the message.
	  @return integer value
	  @throws FieldParseException on conversion error.
	*/
	public int parseIntValue(byte[] field)
		throws FieldParseException
	{
		try { return parseDataValue(field).getIntValue(); }
		catch(NoConversionException nce)
		{
			throw new FieldParseException("Field requires an integer");
		}
	}

	/**
	  Parses an value from the passed field data.
	  @param field the raw data extracted from the message.
	  @return Variable holding the value
	  @throws FieldParseException on conversion error.
	*/
	public Variable parseDataValue(byte[] field)
		throws FieldParseException
	{
		if (field == null)
			throw new FieldParseException("Attempt to parse a null field value.");
		switch(dataType)
		{
		case ASCII_FMT:	
		case ASCII_ZERO:
			return parseAsciiDataValue(field);		
			
		case PBINARY_FMT:
		case SIGNED_PBINARY_FMT:
			return parsePBinaryDataValue(field);

		case LABARGE_FMT:
			return parseLabargeDataValue(field);

		case CAMPBELL_BINARY_FMT:
			return parseCampbellBinaryDataValue(field);

		case HEX_FMT:
			return parseHexDataValue(field);

		case STRING_FMT:
			return new Variable(new String(field));

		case SIGNBIT_BINARY_FMT:
			return parseSignBitDataValue(field);

		case BIN_SIGNED_MSB:
		case BIN_UNSIGNED_MSB:
		case BIN_SIGNED_LSB:
		case BIN_UNSIGNED_LSB:
			return parsePureBinary(field);
			
		case PBINARY_ATON:
			return parseAtonString(field);
		
		case PBINARY_SONTEK:
			return parseSontekString(field);
		

		
		default:
			throw new FieldParseException("Unknown data type '" + dataType
				+ "'");
		}
	}
	
	private Variable parseAsciiDataValue(byte[] field)
		throws FieldParseException
	{
		int n = 0;
		int start = 0;
		int decPoints = 0;
		boolean exponential = false;

		// Strip initial WS & keep track of start of data
		while ( n < field.length && field[n] == ' ' )
			n++;
		start = n;

		// Verify that starts with + or - followed by digit or '.'
		if ( n < field.length && (field[n] == '+' || field[n] == '-' ))
			n++;
		
//		//if it is empty field
//		if(field[n] == ',')
//		{
//			String str = "";
//			n++;
//			return new Variable(str);
//		}
		
		if ( n == field.length
		 || ( !Character.isDigit((char)field[n]) && field[n] != '.' ) )
			throw new FieldParseException("no start digit");

		// Keep track of decimal points
		if (field[n] == '.')
		{
			decPoints++;
			n++;
			// Now we must have a digit
			if (n == field.length || !Character.isDigit((char)field[n]))
				throw new FieldParseException(
					"no start digit after decimal point");
		}

		// Skip initial digits
		while ( n < field.length && Character.isDigit((char)field[n]) )
			 n++;

		// There's something after the initial digits?
		if ( n < field.length )
		{
			if ( field[n] == '.' )
			{
				if (decPoints > 0)
					throw new FieldParseException("multiple decimal points");
				n++;
				decPoints++;
			}

			// skip digits after decimal point
			while(n < field.length && Character.isDigit((char)field[n]))
				n++;

			// Something after fractional part? (exponent part)
			if (n < field.length && (field[n] == 'e' || field[n] == 'E'))
			{
				exponential = true;
				n++;
				if (n < field.length && (field[n] == '+' || field[n] == '-' ))
					n++;
				if (n < field.length)
				{
					if (!Character.isDigit((char)field[n]))
						throw new FieldParseException(
							"Invalid exponent field in number");
					while(n<field.length
						&& Character.isDigit((char)field[n]))
						n++;
				}
				else throw new FieldParseException(
					"Expected exponent digits");
			}
		}
		int end = n;

		// The only thing allowed at the end is whitespace.
		while (n < field.length)
			if (Character.isWhitespace((char)field[n]))
				n++;
			else
				throw new FieldParseException(
					"Garbage in field after number");

		String s = new String(field, start, end-start);
		if (s.startsWith("+"))
			s = s.substring(1);

		if (decPoints == 0 && !exponential)
		{
			try 
			{
				long x = Long.parseLong(s); 
				return new Variable(x); 
			}
			catch (Exception e)
			{
				throw new FieldParseException(e.toString());
			}
		}
		else
		{
			try 
			{
				double x = Double.parseDouble(s); 
				return new Variable(x); 
			}
			catch (Exception e)
			{
				throw new FieldParseException(e.toString());
			}
		 }
	}
	
	private Variable parsePureBinary(byte[] field)
	{
		// If this is an LSB-First type, put the data in MSB-first order.
		byte f[] = new byte[field.length];
		if (dataType == BIN_SIGNED_LSB || dataType == BIN_UNSIGNED_LSB)
			for(int i=0; i<field.length; i++)
				f[i] = field[field.length-i-1];
		else
			for(int i=0; i<field.length; i++)
				f[i] = field[i];
		
		// Convert it to a long integer
		long lv = (long)f[0];

		// Java will do sign-extension, so if this is an unsigned type,
		// mask out the high-order bits.
		if (dataType == BIN_UNSIGNED_MSB || dataType == BIN_UNSIGNED_LSB)
		{
			lv &= 0xffL;
		}
		
		// shift & copy in remaining bytes
		for (int i=1; i<f.length; i++)
		{
			lv = (lv << 8) & (~0xffL);
			int b = f[i] & 0xff;
			lv = lv | b;
		}
		
		return new Variable(lv);
	}


	private Variable parsePBinaryDataValue(byte[] field)
		throws FieldParseException
	{
		long result = 0;
		int sign = 1;

		if ( dataType == SIGNED_PBINARY_FMT )  // signed integer
		{
			int c = (int)field[0];
			if (c < 0x3F)   // 0x40 is the value of '@'
				throw new FieldParseException("Illegal character '" 
					+ c + "' in pseudo binary data field.");
			result = (long)(c & 0x1f);
			if ((c & 0x20) != 0)
				sign = -1;
		}
		else
		{
			int c = (int)field[0];
			if (c < 0x3F)   // 0x40 is the value of '@'
				throw new FieldParseException("Illegal character '" 
					+ c + "' in pseudo binary data field.");
			result = (long)(c & 0x3f);
		}

		for(int i = 1; i < field.length; i++)
		{
			int c = (int)field[i];
			if (c < 0x3F)   // 0x40 is the value of '@' 3F == ?
				throw new FieldParseException("Illegal character '" 
					+ c + "' in pseudo binary data field.");

			result <<= 6;
			result += (long)(c & 0x3f);
		}

		if (sign == -1)
		{
			// Compute the 2s compliment
			int numbits = field.length * 6 - 1;
			result = (1<<numbits) - result;
			result = -result;
		}

		return new Variable(result);
	}
	

	private Variable parseLabargeDataValue(byte[] field)
		throws FieldParseException
	{
		double val = 0.0;
		int limit;
		double factor1, factor2;

		if ( field.length == 4 )
		{
			limit = 9;
			factor1 = 1.0;
			factor2 = 10.0;
		}
		else
		{
			limit = 15;
			factor1 = .01952941;
			factor2 = 16.0;
		}
		for( int n = 0; n < field.length; n++ )
		{
			int ival = field[n] & 0x3f;
			if ( ival > limit )
				throw new FieldParseException("Labarge value out of limit");

			val = val + ( factor1 * ival );
			factor1 *= factor2;
		}
		return new Variable(val);
	}


	private Variable parseCampbellBinaryDataValue(byte[] field)
		throws FieldParseException
	{
		try
		{
			int n = 0;
			double factor1;
			double val = 0.0;
	
			if ( (field[n] & 0x08) != 0 )
				factor1 = -1;
			else
				factor1 = 1;
			if ( (field[n] & 0x04)	!= 0 )
				factor1 *= .01;
			if ( (field[n] & 0x02) != 0	)
				factor1 *= .1;
			if ( (field[n] & 0x01)	!= 0 )
				val = 4096;
			else
				val = 0;
			n++;
	
			int ival = ( field[n] & 0x3f );
			val += ( (double) ival) * 64;
			n++;
			ival = ( field[n] & 0x3f );
			val += ( float)ival;
			val *= factor1;
	
			return new Variable(val);
		}
		catch(Exception ex)
		{
			throw new FieldParseException("Error parsing BC field: " + ex);
		}
	}

	private Variable parseHexDataValue(byte[] field)
		throws FieldParseException
	{
		String data = new String(field);
		data = data.trim().toLowerCase();

		long result = 0L;
		for(int i = 0; i < data.length(); i++)
		{
			result <<= 4;
			char c = data.charAt(i);
			if (Character.isDigit(c))
				result += ((int)c - (int)'0');
			else if ((int)c >= (int)'a' && (int)c <= (int)'f')
				result += ((int)c - (int)'a');
			else
				throw new FieldParseException("Illegal character in hex field");
		}

		return new Variable(result);
	}

	/**
	  Design analysis and Telonics binary format.
	  Extract integer value. If sign bit is set, multiply by -1.
	*/
	private Variable parseSignBitDataValue(byte[] field)
		throws FieldParseException
	{
		double factor1;

		if (((int)field[0] & 0x20) != 0)
			factor1 = -1;
		else
			factor1 = 1;

		int ival = (field[0] & 0x1f);
		for(int i=1; i < field.length; i++)
		{
			ival <<= 6;
			ival += (field[i] & 0x3f);
		}

		return new Variable((double)ival * factor1);
	}
	
	private Variable parseAtonString(byte[] field)
			throws FieldParseException
	{
		String result = "";
		int fieldIndex = 0;
		DecimalFormat dFormat = new DecimalFormat("0.000");
		//first 10 characters are type and id 
		for(fieldIndex=0; fieldIndex<11;fieldIndex++)
		{
			if(fieldIndex>2)
				result += (char)field[fieldIndex];
		}
		//if(field[charCount]=='-')
			result += "\n";
		String strResult = "";
		for(fieldIndex = 12; fieldIndex < 24; fieldIndex++)
		{
			String temp = "";
			temp += (char)field[fieldIndex];
			temp += (char)field[++fieldIndex];

			int number = PseudoBinary.decodePB(temp, false);
			strResult = number+"";
			if(strResult.length()<2)
				strResult = "0" + strResult;
			
			result += strResult + " ";
		}
		
		for(fieldIndex = 24; fieldIndex < 30; fieldIndex++)
		{
			String temp = "";
			temp += (char)field[fieldIndex];
			temp += (char)field[++fieldIndex];
			temp += (char)field[++fieldIndex];

			int number = PseudoBinary.decodePB(temp, false);
			strResult = number+"";
			String zeros = "";
			if(strResult.length()<9)
				for(int i = strResult.length(); i<8; i++)
					zeros += "0";
			strResult = zeros + strResult;
			if(fieldIndex!=29)
				result += strResult + " ";
			else
				result += strResult;
		}
		
		for(fieldIndex = 30; fieldIndex < 51; fieldIndex++)
		{
			String temp = "";
			temp += (char)field[fieldIndex];
			temp += (char)field[++fieldIndex];
			if(fieldIndex+1==34 || fieldIndex+1==47 || fieldIndex+1==50)
				temp += (char)field[++fieldIndex];
			int number = PseudoBinary.decodePB(temp, true);
			double dnum;
			
			if(fieldIndex == 31 || fieldIndex == 34 || fieldIndex == 36 || fieldIndex == 38 || fieldIndex == 40)
			{
				dnum = number*0.1;
				strResult = dFormat.format(dnum);
				
				//remove extra digits or fill in with blanks
				if(fieldIndex==34 && strResult.length()<7)
				{
					strResult = String.format("%"+(7-strResult.length())+"s", "")+strResult;
				}
				else if(fieldIndex==34 && strResult.length()>7)
				{
					strResult = strResult.substring(0,7);
				}
				else if(strResult.length()<7)
				{
					strResult = String.format("%"+(7-strResult.length())+"s", "")+strResult;
				}
				else if(strResult.length()>7)
				{
					strResult = strResult.substring(0,7);
				}
				//remove trailing zeros or decimal point
				if(strResult.contains("."))
				{
					if( strResult.charAt(strResult.length()-1)=='0')
					{
						strResult = strResult.replace("0", "");
						strResult = " " + strResult;
					}
					if(strResult.indexOf('.')==(strResult.length()-1))
					{
						strResult = strResult.replace(".","");
						strResult = " " + strResult;
					}
				}
			}
			else if(fieldIndex == 42)
			{
				dnum = number*0.001;
				strResult = dFormat.format(dnum);
				//remove extra digits or fill in with blanks
				if(strResult.length()<8)
				{
					strResult = String.format("%"+(8-strResult.length())+"s", "")+strResult;
				}
				else if(strResult.length()>8)
				{
					strResult = strResult.substring(0,8);
				}
			}
			else if(fieldIndex == 44)
			{
				DecimalFormat df = new DecimalFormat("0.00");
				dnum = number*0.01;
				strResult = df.format(dnum);
				
				//remove extra digits or fill in with blanks
				if(strResult.length()<7)
				{
					strResult = String.format("%"+(7-strResult.length())+"s", "")+strResult;	
				}
				else if(strResult.length()>7)
				{
					strResult = strResult.substring(0,6);
				}
			}
			else 
			{
				dnum = number;
				strResult = dnum+"";
				//remove extra digits or fill in with blanks
				if(strResult.length()<6)
				{
					strResult = String.format("%"+(6-strResult.length())+"s", "")+strResult;
				}
				else if(strResult.length()>6)
				{
					strResult = strResult.substring(0,6);
				}
				//remove trailing zeros or decimal point
				if(strResult.contains("."))
				{
					if( strResult.charAt(strResult.length()-1)=='0')
					{
						strResult = strResult.replace("0", "");
						strResult = " " + strResult;
					}
					if(strResult.indexOf('.')==(strResult.length()-1))
					{
						strResult = strResult.replace(".","");
						strResult = " " + strResult;
					}
				}
			}
			result += strResult;// + " ";
		}
		result = result + "\n";
		
		//processing data
		for(fieldIndex = 52; fieldIndex < field.length-1; fieldIndex++)
		{
			for(int i = 0; i < 6; i++)
			{
				if(fieldIndex>field.length-2)
					break;
				String temp = "";
				
				temp += (char)field[fieldIndex++];
				temp += (char)field[fieldIndex++];
				
				//the first three values in each bin/line are 
				//represented by three bytes of pseudo-binary
				if(i==0 || i==1 || i==2 && fieldIndex < field.length-1)
				{
					temp += (char)field[fieldIndex++];
				}
				int number = PseudoBinary.decodePB(temp, true);
				
				double dnum = 0.000;
				String blank = "";
				if(i==0 || i==1 || i==2)
				{
					dnum = number*0.001;
					strResult = dnum + "";
					strResult = dFormat.format(dnum);
					
					for(int j = strResult.length(); j<8; j++)
						blank += " ";
					strResult = blank + strResult; 
				}
				else 
				{
					dnum = number;
					strResult = dnum + "";
					if(strResult.equalsIgnoreCase("0.0"))
						strResult = "0.000";
					for(int j = strResult.length(); j<6; j++)
						blank += " ";
					strResult = blank + strResult; 
				}				
				result += strResult;// + " ";
			}
			result = result + "\n";
		}
		return new Variable(result);
	}

		
	private Variable parseSontekString(byte[] field)
			throws FieldParseException
	{
//		String result = "";
		StringBuilder result = new StringBuilder();
		int pos = 0;
//		DecimalFormat dFormat = new DecimalFormat("0.000");
		//first 10 characters are type and id 
		for(pos=0; pos<11;pos++)
		{
			if(pos>2)
				result.append((char)field[pos]);
		}
		
		//skip flag
		pos++;
		
		result.append("\n");
		String intString = "";
		
		String pbChar = "";
		String blank = "";
		
		//first two bytes of the first header
		pbChar += (char)field[pos];
		pbChar += (char)field[++pos];

		int n = (int)PseudoBinary.decodePB(pbChar, false);
		intString = n+"";
		for(int j = intString.length(); j<4; j++)
			blank += " ";
		intString = blank + intString; 
		result.append(intString);
		pbChar = "";
		
		//not encoded chars of the first header
		for(int i = 0; i < 5; i++)
		{
			pbChar += (char)field[++pos];
		}
		result.append("       " + pbChar);
		
		//the rest of the first header
		for(pos = 19; pos < 41; pos++)
		{
			String temp = "";
			temp += (char)field[pos];
			temp += (char)field[++pos];
			
			int number = (int)PseudoBinary.decodePB(temp, false);
			intString = number+"";
			blank="";
			if(pos==20)
				for(int j = intString.length(); j<12; j++)
					blank += " ";
			if(pos==22)
				for(int j = intString.length(); j<5; j++)
					blank += " ";
			if(pos==24||pos==26||pos==28||pos==30||pos==32)
				for(int j = intString.length(); j<3; j++)
					blank += " ";
			if(pos>32)
				for(int j = intString.length(); j<2; j++)
					blank += " ";
			intString = blank + intString; 
			result.append(intString);
		}
		result.append("\n");
		
		//skip header flag and process second header
		for(pos = 42; pos < 52; pos++)
		{
			String temp = "";
			temp += (char)field[pos];
			temp += (char)field[++pos];
			
			int number = (int)PseudoBinary.decodePB(temp, false);
			
			intString = number+"";
			blank="";
			
			for(int j = intString.length(); j<6; j++)
				blank += " ";
			intString = blank + intString; 
			result.append(intString);
		}
		result.append("\n");
		
		//skip flag and process third header
		for(pos = 53; pos < 80; pos++)
		{
			String temp = "";
			temp += (char)field[pos];
			temp += (char)field[++pos];
			if(pos>59 && pos<66 || pos>71)
				temp+= (char)field[++pos];
			int number=0;
			if(pos == 56 || pos == 58)
				number = PseudoBinary.decodePB(temp, true);
			else
				number = PseudoBinary.decodePB(temp, false);
			intString = number+"";
			blank="";
			
			for(int j = intString.length(); j<7; j++)
				blank += " ";
			intString = blank + intString; 
			result.append(intString);
		}
		
		result.append("\n");
		
		//skip flag and process fourth header
		for(pos = 81; pos < 113; pos++)
		{
			String temp = "";
			temp += (char)field[pos];
			temp += (char)field[++pos];
			
			int number = PseudoBinary.decodePB(temp, false);
			intString = number+"";
			blank="";
			
			for(int j = intString.length(); j<4; j++)
				blank += " ";
			intString = blank + intString; 
			result.append(intString);
		}
		
		//data processing 
		boolean firstCell = true;
		int velocityLen = 2;
		for(pos = 113; pos < field.length-2; pos++) // incr here gobbles the plus sign
		{
			result.append("\n");
			
			if (firstCell)
			{
				// Count # of chars to the next '+'
				int idx = 0;
				for(; idx < 17 && (char)field[pos + ++idx] != '+'; );
//					System.out.println("\t" + (char)field[pos + idx]);
//				System.out.println("celLen = " + idx);
				if (idx == 14)
					velocityLen = 2;
				else if (idx == 16)
					velocityLen = 3;
				firstCell = false;
			}
//System.out.println("velocityLen=" + velocityLen);
			
			// Cell #
			String bin = "";
			bin += (char)field[++pos];
			
			int b = PseudoBinary.decodePB(bin, false);
			
			bin = b +"";
			if(bin.length()==1)
				bin = "  " + bin;
			else
				bin = " " + bin;
			result.append(bin);
			
			// Each cell has: velocity 1, velocity 2, std dev 1, std dev 2, amp 1, amp 2
			
			// Get the 2 velocity measurements
			for(int i = 0; i < 2; i++)
			{
				if(pos>field.length-3)
					break;

				String pb = "";
				for(int x = 0; x<velocityLen; x++)
					pb = pb + (char)field[++pos];
				int vel = PseudoBinary.decodePB(pb, true);
				String vels = "" + vel;
				for(int x = 0; x < 7 - vels.length(); x++)
					result.append(' ');
				result.append(vels);
			}

			// get the 2 std devs and 2 amplitudes (aka echo intensity).
			for(int i = 0; i<4; i++)
			{
				if(pos>field.length-3)
					break;
				
				String temp = "";
				temp += (char)field[++pos];
				temp += (char)field[++pos];
				
				int number = PseudoBinary.decodePB(temp, true);
				intString = number+"";
				blank="";
				if(i<2)
				for(int j = intString.length(); j<7; j++)
					blank += " ";
				else
					for(int j = intString.length(); j<4; j++)
						blank += " ";
				intString = blank + intString;
				result.append(intString);
			}
		}
		return new Variable(result.toString());
	} 

	
	// Test main
	public static void main(String args[])
		throws Exception
	{
		NumberParser np = new NumberParser();
		np.setDataType(BIN_SIGNED_MSB);
		byte[] field = args[0].getBytes();
		Variable v = np.parseDataValue(field);
		System.out.println("BIN_SIGNED_MSB Parsed '" + args[0] + "' to: " + v);

		np.setDataType(BIN_UNSIGNED_MSB);
		v = np.parseDataValue(field);
		System.out.println("BIN_UNSIGNED_MSB Parsed '" + args[0] + "' to: " + v);
		
		np.setDataType(BIN_SIGNED_LSB);
		v = np.parseDataValue(field);
		System.out.println("BIN_SIGNED_LSB Parsed '" + args[0] + "' to: " + v);

		np.setDataType(BIN_UNSIGNED_LSB);
		v = np.parseDataValue(field);
		System.out.println("BIN_UNSIGNED_LSB Parsed '" + args[0] + "' to: " + v);
	}
}

