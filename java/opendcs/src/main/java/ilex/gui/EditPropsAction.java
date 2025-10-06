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
package ilex.gui;

import javax.swing.text.JTextComponent;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import java.util.ResourceBundle;
import java.util.Vector;
import java.util.Properties;
import java.util.Enumeration;
import java.util.HashMap;
import java.awt.*;
import java.io.IOException;
import java.awt.event.*;
import javax.swing.*;

/**
Action for editing properties
This class was designed for the LRGS GUI. The application's properties are
kept in a big Properties set. Each screen wants to provide an editor screen
of just its properties, identified by a prefix.
Example, the MessageBrowser just shows propeties that start with the
prefixes "General" or "MessageBrowser".
*/
public class EditPropsAction extends AbstractAction
{
	private static ResourceBundle labels = MenuFrame.getLabels();
	/** The owning frame */
	JFrame parent;
	/**  prefixes to edit */
	String[] prefixes;
	/** appears in the dialog */
	String screenname;
	static HashMap<String, JComponent> editors = new HashMap<>();

	/**
	* Registers an editor for a particular property.
	* Example: if this is a boolean property, the editor might be a combo
	* box with values "true" and "false".
	* @param propName the property name
	* @param editor the GUI editor component
	*/
	public static void registerEditor( String propName, JComponent editor )
	{
		editors.put(propName.toLowerCase(), editor);
	}

	/**
	* Constructor.
	* @param parent the owning frame
	* @param screenname appears in the dialog
	* @param prefixes list of prefixes to include in the dialog
	*/
	public EditPropsAction( JFrame parent, String screenname, String[] prefixes )
	{
		//super("Properties");
		super(labels.getString("EditPropsAction.properties"));
		this.parent = parent;
		this.screenname = screenname;
		if (prefixes == null)
			this.prefixes = null;
		else
		{
			this.prefixes = new String[prefixes.length];
			for(int i=0; i<prefixes.length; i++)
				this.prefixes[i] = prefixes[i].toLowerCase();
		}
	}

	/**
	* Called when the action is triggered, usually by menu selection.
	* @param e ignored
	*/
	public void actionPerformed( ActionEvent e )
	{
		// Construct a vector of all the property names to edit.
		// That is - all the names that start with one of my prefixes.
		Vector propnames = new Vector();
		Properties props = GuiApp.getProperties();
		for(Enumeration it = props.keys(); it != null && it.hasMoreElements(); )
		{
			String name = (String)it.nextElement();
			if (prefixes == null)
				propnames.add(name);
			else
				for(int i=0; i<prefixes.length; i++)
					if (name.toLowerCase().startsWith(prefixes[i]))
					{
						propnames.add(name);
						break;
					}
		}
		java.util.Collections.sort(propnames);
		PropEditDialog dlg = new PropEditDialog(parent, screenname, propnames,
			editors);
		dlg.setVisible(true);
	}
}

/**
Non-public Dialog class to implement the properties editors.
*/
class PropEditDialog extends JDialog
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	private static ResourceBundle labels = MenuFrame.getLabels();
	Vector propnames;
	JButton okButton, cancelButton, saveButton;
	JComponent[] editField;

	/**
	* Constructor
	* @param parent the owning frame
	* @param screenname appears in the dialog
	* @param propnames property names to include in the dialog
	* @param editors HashMap of GUI components
	*/
	PropEditDialog( JFrame parent, String screenname,
			Vector propnames, HashMap<String, JComponent> editors )
	{
		super(parent,
				labels.getString("EditPropsAction.editProperties"), true);
		this.propnames = propnames;
		setSize(550, 300);
		Container contpane = getContentPane();
		contpane.setLayout(new BorderLayout());
		JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER));
		p.add(new JLabel(labels.getString("EditPropsAction.propertiesFor")
				+screenname));
		contpane.add(p, BorderLayout.NORTH);
		p = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 4));
		p.add(okButton = new JButton(labels.getString("EditPropsAction.ok")));
		okButton.addActionListener(
			new ActionListener()
			{
				public void actionPerformed(ActionEvent av)
				{
					doOK();
				}
			});
		p.add(saveButton = new JButton(
				labels.getString("EditPropsAction.save")));
		saveButton.addActionListener(
			new ActionListener()
			{
				public void actionPerformed(ActionEvent av)
				{
					doSave();
				}
			});
		p.add(cancelButton = new JButton(
				labels.getString("EditPropsAction.cancel")));
		cancelButton.addActionListener(
			new ActionListener()
			{
				public void actionPerformed(ActionEvent av)
				{
					doCancel();
				}
			});
		contpane.add(p, BorderLayout.SOUTH);

		JPanel editpanel = new JPanel(new GridLayout(propnames.size(), 2, 2, 2));

		editField = new JComponent[propnames.size()];
		Properties properties = GuiApp.getProperties();
		for(int i=0; i<propnames.size(); i++)
		{
			String name = (String)propnames.elementAt(i);
			p = new JPanel(new FlowLayout(FlowLayout.RIGHT));
			p.add(new JLabel(name+": ", SwingConstants.RIGHT));
			editpanel.add(p);

			editField[i] = (JComponent)editors.get(name.toLowerCase());
			if (editField[i] == null)
				editField[i] = new JTextField(20);

			if (editField[i] instanceof JTextComponent)
			{
				((JTextComponent)editField[i]).setText(
					properties.getProperty(name));
				((JTextComponent)editField[i]).setCaretPosition(0);
			}
			else if (editField[i] instanceof JComboBox)
				((JComboBox)editField[i]).setSelectedItem(
					properties.getProperty(name));

			p = new JPanel(new FlowLayout(FlowLayout.LEFT));
			p.add(editField[i]);
			editpanel.add(p);
		}

		contpane.add(new JScrollPane(editpanel), BorderLayout.CENTER);

	}

	/** Called when OK pressed */
	private void doOK( )
	{
		copyPropertyValuesBack();
		closeDlg();
	}

	/** Called when Cancel pressed */
	private void doCancel( )
	{
		closeDlg();
	}

	/** Closes the dialog */
	private void closeDlg( )
	{
		setVisible(false);
		dispose();
	}

	/** Copies values back from dialog into the Properties set. */
	private void copyPropertyValuesBack( )
	{
		Properties properties = GuiApp.getProperties();
		for(int i=0; i<propnames.size(); i++)
		{
			String name = (String)propnames.elementAt(i);
			String value = "";

			if (editField[i] instanceof JTextComponent)
				value = ((JTextComponent)editField[i]).getText();
			else if (editField[i] instanceof JComboBox)
			{
				value = (String)((JComboBox)editField[i]).getSelectedItem();
				if (value == null)
					value = "";
			}

			properties.setProperty(name, value);
		}
	}

	/** Saves the changes. */
	private void doSave( )
	{
		copyPropertyValuesBack();
		try
		{
			GuiApp.saveProperties();
		}
		catch (IOException ioe)
		{
			String msg = labels.getString(
				"EditPropsAction.cannotSaveProp") + ioe.toString();
			log.atError().setCause(ioe).log(msg);
			JOptionPane.showMessageDialog(this, msg,
				"Error!", JOptionPane.ERROR_MESSAGE);
		}
	}
}