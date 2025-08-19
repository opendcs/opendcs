package lrgs.rtstat;

import java.io.*;
import java.net.*;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.text.html.*;
import javax.swing.text.DefaultCaret;

import ilex.util.EnvExpander;
import org.slf4j.LoggerFactory;

/**
This is the HTML panel in which the status snapshot is displayed.
*/
public class RtStatPanel
	extends JPanel
{
	private final static org.slf4j.Logger log = LoggerFactory.getLogger(RtStatPanel.class);
	BorderLayout borderLayout1 = new BorderLayout();
	TitledBorder titledBorder1;
	TitledBorder titledBorder2;
	JScrollPane scrollPane = new JScrollPane();
	JEditorPane htmlPanel = new JEditorPane();

	public RtStatPanel()
	{
		try
		{
			jbInit();
			htmlPanel.setDoubleBuffered(true);
		}
		catch (Exception ex)
		{
			log.error("Exception in RtStatPanel()",ex);
		}
		HTMLEditorKit hek = (HTMLEditorKit)htmlPanel.getEditorKit();
		HTMLDocument doc = (HTMLDocument)hek.createDefaultDocument();

		/*
	      Kludge to set the parsers document base so that it will be able
		  to find the icons sub directory.
		*/
		try 
		{ 
			String path = 
				EnvExpander.expand("$DECODES_INSTALL_DIR") + "/base.html";
			File base = new File(path);
			FileWriter fw = new FileWriter(base);
			fw.write("<html><body>dummy lrgs status page</body></html>\n");
			fw.close();

			StringBuffer sb = new StringBuffer("file://" + path);
			if (sb.charAt(7) != '/')
				sb.insert(7, '/');
			for(int i=7; i<sb.length(); i++)
				if (sb.charAt(i) == '\\')
					sb.setCharAt(i, '/');
			String url = sb.toString();
			doc.setBase(new URL(url));
		}
		catch(Exception ex)
		{
			log.error("Cannot set base url: " , ex);
		}

		// Tell the panel not to update the scroll-pane position based on the
		// current caret position. Otherwise, we see a flicker when the contents
		// of the html panel are rewritten.
		((DefaultCaret)htmlPanel.getCaret()).setUpdatePolicy(
			DefaultCaret.NEVER_UPDATE);
	}

	void jbInit()
		throws Exception
	{
		titledBorder1 = new TitledBorder(BorderFactory.createEtchedBorder(Color.
			white, new Color(165, 163, 151)), "Archive Statistics");
		titledBorder2 = new TitledBorder(BorderFactory.createEtchedBorder(Color.
			white, new Color(165, 163, 151)),
			"Hourly Data Collection Statistics");
		this.setLayout(borderLayout1);
		scrollPane.setHorizontalScrollBarPolicy(JScrollPane.
			HORIZONTAL_SCROLLBAR_NEVER);
		htmlPanel.setEditable(false);
		htmlPanel.setText("");
		htmlPanel.setContentType("text/html");
		this.add(scrollPane, BorderLayout.CENTER);
		scrollPane.getViewport().add(htmlPanel, null);
	}

	public void setPage(String surl)
	{
		try
		{
			StringBuffer sb = new StringBuffer();
			URL url = new URL(surl);
			InputStream is = url.openStream();
			BufferedReader br = new BufferedReader(new InputStreamReader(is));
			String ln;
			while ( (ln = br.readLine()) != null)
			{
				sb.append(ln);
				sb.append("\n");
			}
			String str = sb.toString();
			br.close();
			is.close();
			htmlPanel.setText(str);
		}
		catch (Exception ex)
		{
			log.error("Error in setPage ",ex);
		}
	}

	/**
	 * Updates the on-screen status.
	 * @param htmlstat the status as a block of HTML.
	 */
	public void updateStatus(String htmlstat)
	{
		htmlPanel.setText(htmlstat);
	}
}
