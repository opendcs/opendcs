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
package decodes.dbeditor;

import java.awt.*;
import javax.swing.*;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import java.awt.event.*;
import java.util.ResourceBundle;

import org.opendcs.gui.GuiHelpers;
import org.opendcs.gui.models.LoggingEventListModel;
import org.opendcs.logging.LoggingEvent;
import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

/**
Dialog to show the trace events for decoding.
Used in dbedit, platwiz, and decwiz.
*/
public class TraceDialog extends JDialog 
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	static ResourceBundle genericLabels = DbEditorFrame.getGenericLabels();
	static ResourceBundle dbeditLabels = DbEditorFrame.getDbeditLabels();

	private JPanel panel1 = new JPanel();
	private BorderLayout borderLayout1 = new BorderLayout();
	private JPanel jPanel1 = new JPanel();
	private JButton closeButton = new JButton();
	private JToggleButton autoScroll = new JToggleButton("autoScroll",true);
	private JPanel jPanel2 = new JPanel();
	private FlowLayout flowLayout1 = new FlowLayout();
	private JLabel jLabel1 = new JLabel();
	private JScrollPane eventScrollPane = new JScrollPane();
	private JList<LoggingEvent> eventArea;
	private JLabel errorLabel = new JLabel();
	private String closeText = null;

	/**
	 * Constructor for Dialog parent.
	 * @param owner the owning component
	 * @param modal true if this dialog is to be modal.
	 */
	public TraceDialog(JDialog owner, boolean modal)
	{
		super(owner, 
			dbeditLabels.getString("TraceDialog.title"), modal);
		try 	
		{
			jbInit();
			pack();
		}
		catch(Exception ex) 
		{
			GuiHelpers.logGuiComponentInit(log, ex);
		}
	}
	
	public void setTraceType(String type)
	{
		setTitle(type + " Trace");
		jLabel1.setText("Log Messages from last " + type);
	}

	/**
	 * Constructor for Frame parent.
	 * @param owner the owning component
	 * @param modal true if this dialog is to be modal.
	 */
	public TraceDialog(Frame owner, boolean modal)
	{
		super(owner, 
			dbeditLabels.getString("TraceDialog.title"), modal);
		try 	
		{
			jbInit();
			pack();
		}
		catch(Exception ex) 
		{
			GuiHelpers.logGuiComponentInit(log, ex);
		}
	}

	private void jbInit() throws Exception 
	{
		//this.setSize(new Dimension(900, 400));
		panel1.setLayout(borderLayout1);
		this.setTitle(
			dbeditLabels.getString("TraceDialog.title"));
		closeButton.setText(genericLabels.getString("close"));
		closeButton.addActionListener(e -> closeButton_actionPerformed(e));
		autoScroll.setSelected(true);
		jPanel2.setLayout(flowLayout1);
		jLabel1.setText(
			dbeditLabels.getString("TraceDialog.logMsgs"));
		getContentPane().add(panel1);
		panel1.add(jPanel1, BorderLayout.SOUTH);
		jPanel1.add(closeButton, null);
		jPanel1.add(autoScroll, null);

		errorLabel.setForeground(Color.RED); // Set text color to red for errors
		jPanel1.add(errorLabel, null);

		panel1.add(jPanel2, BorderLayout.NORTH);
		jPanel2.add(jLabel1, null);
		panel1.add(eventScrollPane, BorderLayout.CENTER);
		eventArea = new JList<>(new LoggingEventListModel());
		eventArea.setLayoutOrientation(JList.VERTICAL);
		eventArea.setVisibleRowCount(-1);
		eventArea.getModel().addListDataListener(new ListDataListener()
		{

			@Override
			public void intervalAdded(ListDataEvent e)
			{
				setPosition();
			}

			@Override
			public void intervalRemoved(ListDataEvent e)
			{
				setPosition();
			}

			@Override
			public void contentsChanged(ListDataEvent e)
			{
				setPosition();
			}

			private void setPosition()
			{
				if (autoScroll.isEnabled())
				{
					eventArea.ensureIndexIsVisible(eventArea.getModel().getSize()-1);
				}
			}
		});
		eventScrollPane.getViewport().add(eventArea, null);
		//eventScrollPane.setVerticalScrollBarPolicy(
		//	JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		this.setPreferredSize(new Dimension(370, 680));
	}

	void closeButton_actionPerformed(ActionEvent e) 
	{
		setVisible(false);
	}

	/**
	 * Text which if seen in addText will automatically close the dialog.
	 * @param text exact text to check for.
	 */
	public void setCloseText(String text)
	{
		this.closeText = text;
	}

	/**
	 * Adds text to the dialog. 
	 * This is left in place for close text at the moment, it it otherwise a no-op.
	 * @param text the text.
	 */
	public void addText(String text)
	{
		if (closeText != null && closeText.equals(text))
		{
			this.setVisible(false);
		}
	}

	/**
	 * Clears the text in the scroll pane.
	 */
	public void clear()
	{
	}
}
