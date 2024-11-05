/*
* $Id$
*/
package lrgs.rtstat;

import java.io.*;
import java.net.*;

import java.awt.*;
import javax.swing.*;
import javax.swing.text.html.*;
import javax.swing.text.DefaultCaret;

/**
This is the HTML panel in which the summary status snapshot is displayed.
*/
public class RtSummaryStatPanel
	extends JPanel
{
	BorderLayout borderLayout1 = new BorderLayout();
	JScrollPane scrollPane = new JScrollPane();
	JEditorPane htmlPanel = new JEditorPane();
	JEditorPane displayed;

	public RtSummaryStatPanel()
	{
		try
		{
			jbInit();
			htmlPanel.setDoubleBuffered(true);
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		HTMLEditorKit hek = (HTMLEditorKit)htmlPanel.getEditorKit();
		HTMLDocument doc = (HTMLDocument)hek.createDefaultDocument();

		// Tell the panel not to update the scroll-pane position based on the
		// current caret position. Otherwise, we see a flicker when the contents
		// of the html panel are rewritten.
		String version = System.getProperty("java.version");
		if (version.charAt(2) != '4')
		{
			((DefaultCaret)htmlPanel.getCaret()).setUpdatePolicy(
				DefaultCaret.NEVER_UPDATE);
		}
	}

	void jbInit()
		throws Exception
	{
		this.setLayout(borderLayout1);
		scrollPane.setHorizontalScrollBarPolicy(JScrollPane.
			HORIZONTAL_SCROLLBAR_NEVER);
		htmlPanel.setEditable(false);
		htmlPanel.setText("");
		htmlPanel.setContentType("text/html");
		this.add(scrollPane, BorderLayout.CENTER);
		scrollPane.getViewport().add(htmlPanel, null);
	}

	private void setContent(String surl)
		throws IOException
	{
		StringBuilder sb = new StringBuilder();
		URL url = new URL(surl);
		InputStream is = url.openStream();
		BufferedReader br = new BufferedReader(new InputStreamReader(is));
		String ln;
		while ( (ln = br.readLine()) != null)
		{
			sb.append(ln);
			sb.append("\n");
		}
		br.close();
		is.close();
		String str = sb.toString();
		htmlPanel.setText(str);
	}

	/**
	 * Updates the on-screen status.
	 * @param htmlstat the status as a block of HTML.
	 */
	public void updateStatus(String url)
		throws IOException
	{
		if (url != null && url.length() > 0)
		{
			try 
			{
				htmlPanel.setPage(url);
			}
			catch(Exception ex)
			{
				System.err.println("Cannot read '" + url + "': " + ex);
			}
		}
		setContent(url);
	}
}
