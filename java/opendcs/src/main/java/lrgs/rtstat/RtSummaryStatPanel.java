/*
* Where Applicable, Copyright 2025 OpenDCS Consortium and/or its contributors
*
* Licensed under the Apache License, Version 2.0 (the "License"); you may not
* use this file except in compliance with the License. You may obtain a copy
* of the License at
*
*   http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
* License for the specific language governing permissions and limitations
* under the License.
*/
package lrgs.rtstat;

import java.io.*;
import java.net.*;

import java.awt.*;
import javax.swing.*;
import javax.swing.text.html.*;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import javax.swing.text.DefaultCaret;

/**
This is the HTML panel in which the summary status snapshot is displayed.
*/
public class RtSummaryStatPanel extends JPanel
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	BorderLayout borderLayout1 = new BorderLayout();
	JScrollPane scrollPane = new JScrollPane();
	JEditorPane htmlPanel = new JEditorPane();
	JEditorPane displayed;

	public RtSummaryStatPanel()
	{
		jbInit();
		htmlPanel.setDoubleBuffered(true);

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
				log.atError().setCause(ex).log("Cannot read '{}'", url);
			}
		}
		setContent(url);
	}
}
