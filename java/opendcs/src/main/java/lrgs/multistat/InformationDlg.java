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
package lrgs.multistat;

import java.net.*;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.io.FileWriter;

public class InformationDlg extends JDialog
{
	JPanel panel1 = new JPanel();
	BorderLayout borderLayout1 = new BorderLayout();
	JPanel jPanel1 = new JPanel();
	JButton closeButton = new JButton();
	JScrollPane jScrollPane1 = new JScrollPane();
	JEditorPane htmlPane = new JEditorPane();
	char pageSuffix = 'a';

	public InformationDlg(Frame frame, String title, boolean modal)
	{
		super(frame, title, modal);

		jbInit();
		pack();

		this.setSize(new Dimension(400, 800));

	}

	public InformationDlg()
	{
		this(null, "", false);
	}

	private void jbInit()
	{
		panel1.setLayout(borderLayout1);
		closeButton.setText("Close");
		closeButton.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				closeButton_actionPerformed(e);
			}
		});
		this.setTitle("Alarm Information");
		htmlPane.setEditable(false);
		htmlPane.setText("");
		jScrollPane1.setHorizontalScrollBarPolicy(JScrollPane.
			HORIZONTAL_SCROLLBAR_NEVER);
		getContentPane().add(panel1);
		panel1.add(jPanel1, BorderLayout.SOUTH);
		jPanel1.add(closeButton, null);
		panel1.add(jScrollPane1, BorderLayout.CENTER);
		jScrollPane1.getViewport().add(htmlPane, null);
	}

	public void setPage(String url)
	{
		try
		{
			//System.out.println("Setting page to '" + url + "'");
			htmlPane.setPage(new URL(url));
		}
		catch (Exception ex)
		{
			String errFileName = "mserr-" + pageSuffix + ".html";
			pageSuffix = (pageSuffix == 'a' ? 'b' : 'a');
			try
			{
				FileWriter fw = new FileWriter(
					MultiStatConfig.instance().alarmInfoBasePath + errFileName);
				fw.write("<html><body>"
				+ "<h2>Alarm Info Page Not Found</h2>"
				+ "<p>Cannot open page '" + url + "': " + ex
				+ "</body></html>");
				fw.close();
				htmlPane.setPage(
					MultiStatConfig.instance().alarmInfoBaseUrl + errFileName);
			}
			catch(Exception ex2)
			{
				System.err.println("Cannot create error page '" +
					MultiStatConfig.instance().alarmInfoBasePath + errFileName
					+ "': " + ex2);
			}
		}
	}

	void closeButton_actionPerformed(ActionEvent e)
	{
		setVisible(false);
	}
}
