/*
*  $Id: PropertiesEditDialog.java,v 1.2 2020/01/31 19:37:27 mmaloney Exp $
*
*  $State: Exp $
*
*  $Log: PropertiesEditDialog.java,v $
*  Revision 1.2  2020/01/31 19:37:27  mmaloney
*  Added isOkPressed() so callers can know if ok or cancel was hit.
*
*  Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
*  OPENDCS 6.0 Initial Checkin
*
*  Revision 1.1  2008/04/04 18:21:03  cvs
*  Added legacy code to repository
*
*  Revision 1.9  2008/01/24 13:57:47  mmaloney
*  modified files for internationalization
*
*  Revision 1.8  2004/08/31 16:30:24  mjmaloney
*  javadocs
*
*  Revision 1.7  2004/08/09 15:07:59  mjmaloney
*  Upgrades to support platform wizard
*
*  Revision 1.6  2004/05/07 13:58:32  mjmaloney
*  Set dialog title.
*
*  Revision 1.5  2001/10/25 12:57:06  mike
*  Config Editor Polishing.
*
*  Revision 1.4  2001/05/04 01:26:56  mike
*  dev
*
*/
package decodes.gui;

import ilex.util.LoadResourceBundle;

import java.awt.*;
import javax.swing.*;

import decodes.util.DecodesSettings;
import decodes.util.PropertiesOwner;

import java.util.Properties;
import java.util.ResourceBundle;
import java.awt.event.*;


/**
Dialog wrapper for a properties edit panel.
@see PropertiesEditPanel
*/
public class PropertiesEditDialog extends JDialog 
{
	private static ResourceBundle genericLabels = null;
    JPanel panel1 = new JPanel();
    JPanel jPanel1 = new JPanel();
    JLabel jLabel1 = new JLabel();
    JTextField entityNameField = new JTextField();
    BorderLayout borderLayout1 = new BorderLayout();
    FlowLayout flowLayout1 = new FlowLayout();
    PropertiesEditPanel propertiesEditPanel;
    JPanel jPanel2 = new JPanel();
    JButton okButton = new JButton();
    JButton cancelButton = new JButton();
    FlowLayout flowLayout2 = new FlowLayout();

	String entityName;
	private boolean okPressed = false;

	/**
	  Constructor.
	  @param entityName name of object that owns the properties.
	  @param properties the properties set to edit.
	*/
    public PropertiesEditDialog(String entityName, Properties properties)
	{
        super(GuiApp.topFrame, "Properties for " + entityName, true);
        genericLabels = getGenericLabels();
        
        this.setTitle(LoadResourceBundle.sprintf(
        		genericLabels.getString("PropertiesEditDialog.title"),
        		entityName));

		this.entityName = entityName;
        propertiesEditPanel = new PropertiesEditPanel(properties);
		propertiesEditPanel.setOwnerDialog(this);

		try {
            jbInit();
            pack();
			entityNameField.requestFocus();
        }
        catch(Exception ex) {
            ex.printStackTrace();
        }
    }
    
	public void setPropertiesOwner(PropertiesOwner propertiesOwner)
	{
		propertiesEditPanel.setPropertiesOwner(propertiesOwner);
	}

	/**
	 * @return resource bundle containing generic labels for the selected
	 * language.
	 */
	public static ResourceBundle getGenericLabels() 
	{
		if (genericLabels == null)
		{
			DecodesSettings settings = DecodesSettings.instance();
			genericLabels = LoadResourceBundle.getLabelDescriptions(
				"decodes/resources/generic", settings.language);
		}
		return genericLabels;
	}
	
	/** Initializes GUI components. */
    void jbInit() throws Exception {
        panel1.setLayout(borderLayout1);
        jPanel1.setLayout(flowLayout1);
        jLabel1.setText(
        	genericLabels.getString("PropertiesEditDialog.propFor"));
        entityNameField.setPreferredSize(new Dimension(120, 21));
        entityNameField.setEditable(false);
        entityNameField.setText(entityName);
        okButton.setText(
        genericLabels.getString("PropertiesEditDialog.OK"));
        okButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                okButton_actionPerformed(e);
            }
        });
        cancelButton.setText(genericLabels.getString("cancel"));
        cancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                cancelButton_actionPerformed(e);
            }
        });
        jPanel2.setLayout(flowLayout2);
        flowLayout2.setHgap(35);
        flowLayout2.setVgap(10);
        panel1.setMinimumSize(new Dimension(300, 120));
        getContentPane().add(panel1);
        panel1.add(jPanel1, BorderLayout.NORTH);
        jPanel1.add(jLabel1, null);
        jPanel1.add(entityNameField, null);
        panel1.add(propertiesEditPanel, BorderLayout.CENTER);
        panel1.add(jPanel2, BorderLayout.SOUTH);
        jPanel2.add(okButton, null);
        jPanel2.add(cancelButton, null);
    }

	/**
	  Called when OK button pressed. 
	  Saves the changes and closes the dialog.
	  param e ignored.
	*/
    void okButton_actionPerformed(ActionEvent e)
	{
		propertiesEditPanel.saveChanges();
		closeDlg();
		okPressed = true;
    }

	/** Closes the dialog.  */
	void closeDlg()
	{
		setVisible(false);
		dispose();
	}

	/**
	  Called when cancel button pressed. 
	  Closes the dialog without save.
	  param e ignored.
	*/
    void cancelButton_actionPerformed(ActionEvent e)
	{
		closeDlg();
    }

	public boolean isOkPressed()
	{
		return okPressed;
	}

}
