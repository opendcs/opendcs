package decodes.polling;

import java.io.IOException;
import java.io.Writer;
import java.util.Date;

public class PollSessionLogger
{
	private char lastOp = '\0';
	private Writer out;
	private String lineSep = System.getProperty("line.separator");
	
	public PollSessionLogger(Writer out, String sitename)
	{
		this.out = out;
		try
		{
			out.write("Session with station " + sitename + " starting at " + new Date()
				+ lineSep + lineSep);
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public synchronized void sent(String sent)
	{
		try
		{
			if (lastOp != 'S')
				out.write(lineSep + "SENT:" + lineSep);
			out.write(sent);
			lastOp = 'S';
		}
		catch (IOException ex)
		{
		}
			
	}
	
	public synchronized void received(char c)
	{
		try
		{
			if (lastOp != 'R')
				out.write(lineSep + "RECV:" + lineSep);
			out.write(c);
			out.flush();
			lastOp = 'R';
		}
		catch (IOException ex)
		{
		}
	}
	
	public void close()
	{
		try { out.close(); } catch(Exception ex) {}
	}

}
