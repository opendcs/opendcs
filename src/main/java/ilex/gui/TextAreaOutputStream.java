package ilex.gui;

import java.io.IOException;
import java.io.OutputStream;

import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

public class TextAreaOutputStream 
	extends OutputStream
{
	private final JTextArea textArea;

	public TextAreaOutputStream(final JTextArea textArea)
	{
		this.textArea = textArea;
	}

	@Override
	public void flush()
	{
	}

	@Override
	public void close()
	{
	}

	@Override
	public void write(final int b)
		throws IOException
	{
		if (b == '\r')
			return; // CR not necessary in text area, only NL.

		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				textArea.append("" + (char) b);
			}
		});
	}
}
