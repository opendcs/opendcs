/*
 *  $Id$
 */

package decodes.gui.properties;

import java.awt.*;

import javax.swing.*;
import javax.swing.table.*;

import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.TreeSet;
import java.util.Enumeration;

import javax.swing.border.*;

import decodes.gui.PropertiesEditDialog;
import decodes.gui.PropertiesEditPanel;
import decodes.gui.PropertyEditDialog;
import decodes.gui.TopFrame;
import decodes.gui.PropertiesEditPanel.PropertiesEditController;
import decodes.util.DynamicPropertiesOwner;
import decodes.util.PropertiesOwner;
import decodes.util.PropertySpec;

import java.awt.event.*;

import ilex.util.Logger;
import ilex.util.StringPair;
import ilex.util.TextUtil;

/**
 * A panel that allows you to edit a group of Properties.
 *
 * @see PropertiesEditDialog
 */
@SuppressWarnings("serial")
public class PropertiesEditPanelController implements PropertiesEditController
{
    private PropertiesOwner propertiesOwner;
    private HashMap<String, PropertySpec> propHash = null;
    private PropertiesEditPanel view;
    private PropertiesTableModel model;
    private JDialog ownerDialog;
    private JFrame ownerFrame;
    private boolean changesMade = false;

    public PropertiesEditPanelController(PropertiesTableModel model, PropertiesEditPanel view)
    {
        this.model = model;
        this.view = view;
    }

    /**
     * Populates the panel with the passed set.
    *
    * @param properties
    *            The properties to edit.
    */
    public void setProperties(Properties properties)
    {
        model.setProperties(properties);
        changesMade = false;
    }

    public void setView(PropertiesEditPanel view)
    {
        this.view = view;
    }

    /**
     * Sets the owner dialog (if there is one).
    *
    * @param dlg
    *            the owner dialog
    */
    @Override
    public void setOwnerDialog(JDialog dlg)
    {
        ownerDialog = dlg;
    }

    /**
     * Sets the owner frame.
    *
    * @param frm
    *            the owner frame
    */
    @Override
    public void setOwnerFrame(JFrame frm)
    {
        ownerFrame = frm;
    }


    /**
     * Called when 'Add' button is pressed. Displays a sub-dialog for the user
    * to enter name and value.
    *
    * @param e
    *            ignored.
    */
    public void addButton_actionPerformed(ActionEvent e)
    {
        PropertyEditDialog dlg = null;
        PropertySpec propSpec = null;

        if (propertiesOwner != null
        && (propertiesOwner instanceof DynamicPropertiesOwner)
        && ((DynamicPropertiesOwner)propertiesOwner).dynamicPropsAllowed())
        {
            propSpec = new PropertySpec("", PropertySpec.STRING, "");
            propSpec.setDynamic(true);
        }

        if (ownerDialog != null)
            dlg = new PropertyEditDialog(ownerDialog, "", "", propSpec);
        else if (ownerFrame != null)
            dlg = new PropertyEditDialog(ownerFrame, "", "", propSpec);
        else
            dlg = new PropertyEditDialog(TopFrame.instance(), "", "", propSpec);
        dlg.setLocation(50, 50);
        dlg.setLocationRelativeTo(view);
        dlg.setVisible(true);
        StringPair sp = dlg.getResult();
        if (sp != null)
            model.add(sp);
        else
            return;

        if (propSpec != null && propSpec.isDynamic())
        {
            ((DynamicPropertiesOwner)propertiesOwner).setDynamicPropDescription(
                sp.first, propSpec.getDescription());
            propHash.put(propSpec.getName().toUpperCase(), propSpec);
        }

        changesMade = true;
    }

    /**
     * Called when the 'Edit' button is pressed. Displays a sub-dialog for user
    * to edit the selected name/value.
    *
    * @param e
    *            ignored.
    */
    public void editPressed(int modelRow)
    {

        StringPair sp = model.propAt(modelRow);
        PropertySpec propSpec = null;
        if (propHash != null)
            propSpec = propHash.get(sp.first.toUpperCase());
//System.out.println("Editing prop '" + sp.first + "' isDynamic=" +
//(propSpec != null && propSpec.isDynamic()));

        PropertyEditDialog dlg = null;
        Logger.instance().debug3("Editing propspec=" + propSpec);
        if (ownerDialog != null)
            dlg = new PropertyEditDialog(ownerDialog, sp.first, sp.second, propSpec);
        else if (ownerFrame != null)
            dlg = new PropertyEditDialog(ownerFrame, sp.first, sp.second, propSpec);
        else
            dlg = new PropertyEditDialog(TopFrame.instance(), sp.first, sp.second, propSpec);
        dlg.setLocation(50, 50);
        dlg.setLocationRelativeTo(view);
        dlg.setVisible(true);
        StringPair res = dlg.getResult();
        if (res != null)
        {
            model.setPropAt(modelRow, res);
            changesMade = true;
            saveChanges();
        }

        if (propSpec != null && propSpec.isDynamic())
        {
            ((DynamicPropertiesOwner)propertiesOwner).setDynamicPropDescription(
                propSpec.getName(), propSpec.getDescription());
            propHash.put(propSpec.getName().toUpperCase(), propSpec);
        }
    }

    /**
     * Called when the 'Delete' button is pressed. Removes the selected property
    * from the table.
    *
    * @param e
    *            ignored.
    */
    public void deleteButton_actionPerformed(int modelRow)
    {
        if (modelRow == -1)
            return;
        StringPair sp = model.propAt(modelRow);
        PropertySpec propSpec =
            propHash == null ? null : propHash.get(sp.first.toUpperCase());
        if (propSpec != null)
        {
            propHash.remove(sp.first.toUpperCase());
            if (propSpec.isDynamic())
                ((DynamicPropertiesOwner)propertiesOwner).setDynamicPropDescription(
                    propSpec.getName(), null);

        }

        model.deletePropAt(modelRow);
        changesMade = true;
    }

    /** @return true if anything in the table has been changed. */
    public boolean hasChanged()
    {
        return model.hasChanged();
    }

    /** Saves the changes back to the properties set. */
    public void saveChanges()
    {
        model.saveChanges();
    }

    /**
     * Used by the RetProcAdvancedPanel class to repaint the table when the new
    * button is pressed.
    */
    public void redrawTable()
    {
        model.redrawTable();
    }

    /**
     * Adds empty properties with the passed names, but doesn't overwrite any
    * existing property settings.
    *
    * @param propnames
    *            array of property names.
    */
    public void addEmptyProps(String[] propnames)
    {
        model.addEmptyProps(propnames);
    }

    /**
     * Adds a default property name and value, only if a property with this name
    * doesn't already exist.
    */
    // public void addDefaultProperty(String name, String value)
    // {
    // for(int i=0; i<model.props.size(); i++)
    // if (name.equals(((Property)model.props.elementAt(i)).name))
    // return;
    // model.props.add(new Property(name, value));
    // System.out.println("Added new property: " + name + "=" + value);
    // model.fireTableDataChanged();
    // }




    @Override
    public PropertiesTableModel getModel()
    {
        return this.model;
    }
}
