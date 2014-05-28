/**
 * $Id$
 * 
 * $Log$
 * Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
 * OPENDCS 6.0 Initial Checkin
 *
 * 
 * Open Source software by Cove Software, LLC
 */
package decodes.decoder;

import java.util.Date;
import java.util.StringTokenizer;

import ilex.util.ByteUtil;
import ilex.util.Logger;
import ilex.var.Variable;
import decodes.decoder.DataOperations;
import decodes.decoder.DecodedMessage;
import decodes.decoder.DecodesFunction;

/**
 * Ott does Pseudo Binary in a different way. They construct an array
 * of 16-bit binary integers and then bulk-encode as an array of PB
 * chars. Thus the integer boundaries don't line up with PB characters.
 * <p>
 * This function performs the decoding by bulk un-encoding back to the
 * array of 16-bit integers. It then only needs a map between DECODES
 * sensor numbers and the order in which sensors appear in the message.
 * This map is provided by command-line arguments.
 */
public class OttFunction
	extends DecodesFunction 
{
	// Ott represents times relative to Jan 1, 2000.
	public static final long JAN_1_2000 = 946684800000L;
	private Integer[] ordinal2Sensor = null;
	// for decoding state machine
	enum DecodeState { GetInterval, GetNumSamps, GetDataValue,
		GetSampTime1, GetSampTime2 };


	// Required noargs ctor.
	public OttFunction()
	{
	}

	@Override
	public DecodesFunction makeCopy()
	{
		return new OttFunction();
	}

	public String getFuncName() { return "ott"; }

	@Override
	public void setArguments(String sensorList)
	{
		// Argument must be a space, tab, or comma-separated list of
		// DECODES sensor numbers. 'x' means to not process this sensor.
		StringTokenizer st = new StringTokenizer(sensorList, " \t,");
		ordinal2Sensor = new Integer[st.countTokens()];
		for(int i=0; st.hasMoreTokens(); i++)
		{
			String t = st.nextToken();
			try { ordinal2Sensor[i] = Integer.parseInt(t); }
			catch(NumberFormatException ex)
			{
				// null means ignore this sensor.
				ordinal2Sensor[i] = -1;
			}
		}
	}


	@Override
	public void execute(DataOperations dd, DecodedMessage decmsg) 
	{
		// The whole message should be a block of pseudobinary.
		// Get it and bulk decode to an array of bytes. PB data array
		// starts at character 5.
		String strpb = new String(dd.getDataBuffer(), 5, 
			dd.getDataBuffer().length - 5);
		debug("pseudobinary buffer has " + strpb.length() + " characters");

		byte binary[] = pbStr2byteArray(strpb);
		debug("reduced to " + binary.length + " binary bytes.");
		
		int msgTimeSec = ByteUtil.getInt4_LittleEndian(binary, 0);
		Date msgTime = new Date(JAN_1_2000 + (long)msgTimeSec * 1000L);
		decmsg.setMessageTime(msgTime);
		debug("Message time stamp: " + msgTime);

		boolean positive = false;
		int sampleCount = 0, sampleIndex = 0, minutes = 0;
		int timeStamp = 0; // time of sample in seconds since epoch2000
		int aperiodicValue = 0, msgSensorIdx = 0;
		DecodeState state = DecodeState.GetInterval;
		for(int byteIdx = 4; byteIdx < binary.length - 1; byteIdx += 2)
		{
			int int16 = ((int)binary[byteIdx+1]<<8) + ((int)binary[byteIdx] & 0xFF);
			int decodesSensorNum = getDecodesSensorNum(msgSensorIdx);

			switch(state)
			{
			case GetInterval:
				minutes = int16 & 0x7fff;
				positive = (int16 & 0x8000) != 0;
				if (minutes != 0 && decodesSensorNum >= 0 && decmsg != null)
					decmsg.setTimeInterval(decodesSensorNum, minutes * 60);
				state = DecodeState.GetNumSamps;
				break;
			case GetNumSamps:
				sampleCount = int16;
				state = DecodeState.GetDataValue;
				debug("  sensor[" + msgSensorIdx + "] = decodes sensor "
					+ decodesSensorNum + " interval=" + minutes + " minutes "
					+ "will have " + sampleCount + " samples.");
				break;
			case GetDataValue:
				if (positive)
					int16 = int16 & 0xffff;

				if (minutes == 0)
				{
					aperiodicValue = int16;
					state = DecodeState.GetSampTime1;
					break;
				}
				if (decmsg != null && decodesSensorNum >= 0)
				{
					decmsg.addSample(decodesSensorNum, new Variable(int16),
						msgSensorIdx);
					debug("   periodic value " + int16);
				}
				if (++sampleIndex == sampleCount)
				{
					msgSensorIdx++;
					state = DecodeState.GetInterval;
				}
				break;
			case GetSampTime1:
				timeStamp = int16;
				state = DecodeState.GetSampTime2;
				break;
			case GetSampTime2: // Last 2 bytes of little endian sample time
				timeStamp = (int16<<16) + timeStamp;
				Date sampTime = new Date(JAN_1_2000 + (long)timeStamp * 1000L);
				if (decmsg != null && decodesSensorNum >= 0)
				{
					decmsg.addSampleWithTime(decodesSensorNum, 
						new Variable(aperiodicValue), sampTime, 0);
					debug("   aperiodic value " + sampTime + " " + aperiodicValue);
				}
				if (++sampleIndex == sampleCount)
				{
					msgSensorIdx++;
					state = DecodeState.GetInterval;
				}
				else
					state = DecodeState.GetDataValue;
				break;
			}
		}
	}

	private byte[] pbStr2byteArray(String pbStr)
	{
		// We will use 6 out of every 8 bytes. Therefore the length
		// will be 3/4 of the string.
		int outlen = pbStr.length() * 3 / 4;
		if (pbStr.length() % 4 != 0)
			outlen++;
		
		// Initialize output array to all zeros
		byte ret[] = new byte[outlen];
		for(int n = 0; n < outlen; n++) ret[n] = (byte)0;

		int outIdx = 0;
		for(int inIdx = 0; inIdx < pbStr.length(); inIdx++)
		{
			char pbChar = pbStr.charAt(inIdx);
			// Strip off high-order 2 bits.
			int strippedChar = (int)pbChar & 0x3F;
			switch(inIdx % 4)
			{
			case 0:
				ret[outIdx] = (byte)strippedChar;
				break;
			case 1:
				ret[outIdx] = (byte)(ret[outIdx] + ((strippedChar & 0x3) << 6));
				outIdx++;
				ret[outIdx] = (byte)(strippedChar>>2);
				break;
			case 2:
				ret[outIdx] = (byte)(ret[outIdx] + ((strippedChar & 0xF)<<4));
				outIdx++;
				ret[outIdx] = (byte)(strippedChar>>4);
				break;
			case 3:
				ret[outIdx] = (byte)(ret[outIdx] + (strippedChar<<2));
				outIdx++;
				break;
			}
		}
		return ret;
	}

	// Return DECODES Sensor number, or null if this ordinal is undefined
	// or marked to be ignored.
	private int getDecodesSensorNum(int ordinal)
	{
		if (ordinal2Sensor == null || ordinal < 0 || ordinal > ordinal2Sensor.length)
			return -1;
		return ordinal2Sensor[ordinal] == null ? -1 : ordinal2Sensor[ordinal];
	}
	
	private void debug(String msg)
	{
		Logger.instance().debug1("ott: " + msg);
	}
}
