package covesw.azul.net.mitm;

import ilex.util.ByteUtil;

import java.io.IOException;
import java.io.PrintWriter;

public class MITMLogger
{
	private PrintWriter pw = null;
	private boolean printHex = false;
	private String lastPfx = "";
	private byte buffer[] = new byte[16];
	private int bufidx = 0;
	
	public MITMLogger(String logName, boolean printHex)
		throws IOException
	{
		this.printHex = printHex;
		pw = new PrintWriter(logName);
		System.out.println("New MITMLogger writing to '" + logName + "'");
	}
	
	public synchronized void log(String pfx, byte data[], int length)
	{
		if (!pfx.equals(lastPfx))
			flush();
		lastPfx = pfx;
		for(int dataidx = 0; dataidx < length; dataidx++)
		{
			buffer[bufidx++] = data[dataidx];
			if (bufidx == 16)
				flush();
		}
	}
	
	private void flush()
	{
System.out.println("flush() bufidx=" + bufidx);
		if (bufidx == 0)
			return;
		String s = b2a(lastPfx, buffer, bufidx, true);
System.out.println(s);
		pw.println(s);
		if (printHex)
			pw.println(b2a(lastPfx, buffer, bufidx, false));
		pw.println();
		bufidx = 0;
	}
	
	public synchronized void close()
	{
		if (pw == null)
			return;
		flush();
		pw.close();
		pw = null;
	}
	
	private String b2a(String prefix, byte[] bin, int length, boolean ascii)
	{
		StringBuilder sb = new StringBuilder(prefix + " ");
		int idx;
	
		for(idx = 0; idx < length; idx++)
		{
			sb.append(' '); // spacer
			
			int c = bin[idx] & 0xff;

			if (ascii
			 && c >= (int)' ' && c <= (int)'~' 
			 && c != (int)'\\')
			{
				sb.append(' ');
				sb.append((char)c);
			}
			else /* Use some type of escape sequence. */
			{
				if ("\\\n\r\b\f\t".indexOf(c) != -1)
				{
					sb.append('\\');
					switch((char)c)
					{
					case '\\': sb.append('\\'); break;
					case '\n': sb.append('n'); break;
					case '\r': sb.append('r'); break;
					case '\b': sb.append('b'); break;
					case '\f': sb.append('f'); break;
					case '\t': sb.append('t'); break;
					}
				}
				else
				{
					sb.append(ByteUtil.toHexChar(c>>4));
					sb.append(ByteUtil.toHexChar(c&0xF));
				}
			}
		}
		return new String(sb);
	}

}
