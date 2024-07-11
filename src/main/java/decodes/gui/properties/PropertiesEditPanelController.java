/*
 *  $Id$
 */

package decodes.gui.properties;

import javax.swing.*;
import java.util.Properties;

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

/**
 * A panel that allows you to edit a group of Properties.
 *
 * @see PropertiesEditDialog
 */
public class PropertiesEditPanelController implements PropertiesEditController
{
    private PropertiesOwner propertiesOwner;
    private PropertiesEditPanel view;
    private PropertiesTableModel model;
    private JDialog ownerDialog;
    private JFrame ownerFrame;

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

        dlg = getDialog("", "", propSpec);
        dlg.setVisible(true);
        StringPair sp = dlg.getResult();
        if (sp != null)
        {
            model.add(sp);
        }
        else
        {
            return;
        }

        if (propSpec != null && propSpec.isDynamic())
        {
            ((DynamicPropertiesOwner)propertiesOwner)
                .setDynamicPropDescription(sp.first, propSpec.getDescription());
            model.getPropHash().put(propSpec.getName().toUpperCase(), propSpec);
        }
    }

    /**
     * Called when the 'Edit' button is pressed. Displays a sub-dialog for user
    * to edit the selected name/value.
    *
    * @param modelRow which row to edit.
    */
    public void editPressed(int modelRow)
    {

        StringPair sp = model.propAt(modelRow);
        PropertySpec propSpec = null;
        if (model.getPropHash() != null)
        {
            propSpec = model.getPropHash().get(sp.first.toUpperCase());
        }

        PropertyEditDialog dlg = null;
        Logger.instance().debug3("Editing propspec=" + propSpec);
        dlg = getDialog(sp.first, sp.second, propSpec);
        dlg.setVisible(true);
        StringPair res = dlg.getResult();
        if (res != null)
        {
            model.setPropAt(modelRow, res);
            saveChanges();
        }

        if (propSpec != null && propSpec.isDynamic())
        {
            ((DynamicPropertiesOwner)propertiesOwner)
                .setDynamicPropDescription(propSpec.getName(), propSpec.getDescription());
            model.getPropHash().put(propSpec.getName().toUpperCase(), propSpec);
        }
    }

    private PropertyEditDialog getDialog(String property, String value, PropertySpec propertySpec)
    {
        PropertyEditDialog dlg;
        if (ownerDialog != null)
        {
            dlg = new PropertyEditDialog(ownerDialog, property, value, propertySpec);
        }
        else if (ownerFrame != null)
        {
            dlg = new PropertyEditDialog(ownerFrame, property, value, propertySpec);
        }
        else
        {
            dlg = new PropertyEditDialog(TopFrame.instance(), property, value, propertySpec);
        }
        dlg.setLocation(50, 50);
        dlg.setLocationRelativeTo(view);
        return dlg;
    }

    /**
     * Called when the 'Delete' button is pressed. Removes the selected property
    * from the table.
    *
    * @param modelRow which property to delete.
    */
    public void deleteButton_actionPerformed(int modelRow)
    {
        if (modelRow == -1)
            return;
        StringPair sp = model.propAt(modelRow);
        PropertySpec propSpec =
            model.getPropHash() == null ? null : model.getPropHash().get(sp.first.toUpperCase());
        if (propSpec != null)
        {
            model.getPropHash().remove(sp.first.toUpperCase());
            if (propSpec.isDynamic())
            {
                ((DynamicPropertiesOwner)propertiesOwner)
                    .setDynamicPropDescription(propSpec.getName(), null);
            }
        }

        model.deletePropAt(modelRow);
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

    @Override
    public PropertiesTableModel getModel()
    {
        return this.model;
    }
}
