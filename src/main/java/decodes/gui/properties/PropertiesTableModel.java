package decodes.gui.properties;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.TreeSet;

import javax.swing.table.AbstractTableModel;

import org.slf4j.LoggerFactory;

import decodes.gui.PropertiesEditDialog;
import decodes.gui.TopFrame;
import decodes.util.DynamicPropertiesOwner;
import decodes.util.PropertiesOwner;
import decodes.util.PropertySpec;
import ilex.util.StringPair;
import ilex.util.TextUtil;

public class PropertiesTableModel extends AbstractTableModel
 {
     private static final org.slf4j.Logger log = LoggerFactory.getLogger(PropertiesTableModel.class);
     private static ResourceBundle genericLabels = PropertiesEditDialog.getGenericLabels();
     /** The properties as a list of StringPair object. */
     private ArrayList<StringPair> props = new ArrayList<StringPair>();
     private PropertiesOwner propertiesOwner;

     /** Column names */
     private static String columnNames[];

     /** The Properties set that we're editing. */
     private Properties origProps;

     /** flag to keep track of changes */
     private boolean changed;

     private HashMap<String, PropertySpec> propHash = null;

     /** Constructs a new table model for the passed Properties set. */
     public PropertiesTableModel(Properties properties)
     {
         columnNames = new String[2];
         columnNames[0] = genericLabels.getString("name");
         columnNames[1] = genericLabels.getString("PropertiesEditDialog.value");
         setProperties(properties);
     }

     public HashMap<String,PropertySpec> getPropHash() {
        return propHash;
     }

     /**
      * Sets a hash of property specs for known properties for this object. Known
      * properties will be shown in the table even if no value is assigned. Can
      * be called multiple times if the property specs change. Subsequent calls
      * will remove any unassigned properties that are no longer spec'ed.
      *
      * @param propHash
      *            the has of specs
      */
     public void setPropHash(HashMap<String, PropertySpec> propHash)
     {
         this.propHash = propHash;
         for (String ucName : propHash.keySet())
         {
             boolean found = false;
             for (StringPair prop : props)
             {
                 if (prop.first.equalsIgnoreCase(ucName))
                 {
                     found = true;
                     break;
                 }
             }
             if (!found)
             {
                 StringPair sp = new StringPair(propHash.get(ucName).getName(), "");
                 props.add(sp);
                 log.trace("Added spec'ed property '{}'", sp.first);
             }
         }
         // Now remove the unassigned props that are no longer spec'ed
         for (Iterator<StringPair> spit = props.iterator(); spit.hasNext();)
         {
             StringPair sp = spit.next();
             if ((sp.second == null || sp.second.length() == 0) // value
                                                                 // unassigned
                 && propHash.get(sp.first.toUpperCase()) == null) // not in spec
                                                                     // hash
             {
                 spit.remove();
                 log.trace("Removed unspec'ed property '{}'", sp.first);
             }
         }
         log.trace("Property table now has {} rows.", props.size());
         this.fireTableDataChanged();
     }

     /** Sets the properties set being edited. */
     public void setProperties(Properties properties)
     {
         origProps = properties;
         changed = false;

         if (origProps == null)
             return;

         props.clear();
         TreeSet<String> names = new TreeSet<String>();
         Enumeration<?> pe = properties.propertyNames();
         while (pe.hasMoreElements())
             names.add((String) pe.nextElement());
         if (propHash != null)
             for (PropertySpec ps : propHash.values())
                 names.add(ps.getName());

         for (String name : names)
         {
             String value = properties.getProperty(name);
             if (value == null)
                 value = "";
             props.add(new StringPair(name, value));
         }
     }

     /** Returns number of rows */
     public int getRowCount()
     {
         return props.size();
     }

     /** Returns number of columns */
     public int getColumnCount()
     {
         return columnNames.length;
     }

     /** Returns column name */
     public String getColumnName(int col)
     {
         return columnNames[col];
     }

     /** Returns a String value at specified row/column */
     public Object getValueAt(int row, int column)
     {
         if (row >= getRowCount())
             return null;
         StringPair sp = props.get(row);
         if (column == 0)
             return sp.first;
         if (sp.first != null
          && (sp.first.toLowerCase().contains("password") || sp.first.toLowerCase().contains("passwd"))
          && !sp.first.equalsIgnoreCase("passwordCheckerClass")
          && sp.second != null && sp.second.length() > 0)
             return "****";
         PropertySpec ps = propHash != null ? propHash.get(sp.first.toUpperCase()) : null;
         if (ps != null && ps.getType().equals(PropertySpec.COLOR))
         {
             if (sp.second.toLowerCase().startsWith("0x"))
             {
                 return new Color(Integer.parseInt(sp.second.substring(2), 16));
             }
             else
             {
                 return sp.second;
             }
         }
         else
         {
             return sp.second;
         }
     }

     /**
      * Return the current value of a given property, or null if it's not set.
      * @param propName the name of the property to retrieve.
      * @return the current value of a given property, or null if it's not set.
      */
     public String getCurrentPropValue(String propName)
     {
        for(StringPair sp : props)
        {
            if (sp.first.equalsIgnoreCase(propName))
            {
                return sp.second;
            }
        }
        return null;
     }

     /** Adds a new property to the model. */
     public void add(StringPair prop)
     {
         if (TextUtil.isAllWhitespace(prop.first))
         {
             TopFrame.instance().showError(genericLabels.getString("PropertiesEditDialog.blankErr"));
             return;
         }

         props.add(prop);
         fireTableRowsInserted(props.size()-1, props.size()-1);
         changed = true;
     }

     /** Returns selected row-property as a StringPair object. */
     public StringPair propAt(int row)
     {
         if (row >= getRowCount())
             return null;
         return props.get(row);
     }

     /** Sets selected row-property from a StringPair object. */
     public void setPropAt(int row, StringPair prop)
     {
         if (row >= getRowCount())
             return;
         if (TextUtil.isAllWhitespace(prop.first))
         {
             TopFrame.instance().showError(genericLabels.getString("PropertiesEditDialog.blankErr"));
             return;
         }
         props.set(row, prop);
         fireTableRowsUpdated(row, row);
         changed = true;
     }

     public void addEmptyProps(String[] propNames)
     {
         int numAdded = 0;
         for (String nm : propNames)
         {
             int row = 0;
             for (; row < getRowCount(); row++)
             {
                 StringPair sp = propAt(row);
                 if (nm.equalsIgnoreCase(sp.first))
                     break;
             }
             if (row >= getRowCount()) // fell through - not found.
             {
                 add(new StringPair(nm, ""));
                 numAdded++;
             }
         }
         if (numAdded > 0)
         {
             fireTableDataChanged();
             changed = true;
         }
     }

     /** Deletes specified row */
     public void deletePropAt(int row)
     {
         if (row >= getRowCount())
             return;
         props.remove(row);
         fireTableDataChanged();
         changed = true;
     }

     /** Returns true if anything has been changed. */
     public boolean hasChanged()
     {
         return changed;
     }

     /** Saves changes from the model back to the java.util.Properties set. */
     public void saveChanges()
     {
         origProps.clear();
         for (int i = 0; i < props.size(); i++)
         {
             StringPair sp = props.get(i);
             if (sp.second != null && sp.second.trim().length() > 0)
                 origProps.setProperty(sp.first, sp.second);
         }
         changed = false;
     }

     /**
      * Used by the RetProcAdvancedPanel class to repaint the table when the new
      * button is pressed.
      */
     public void redrawTable()
     {
         saveChanges();
         fireTableDataChanged();
     }

     /**
     * Call this method before setProperties(). The known properties will be
    * displayed in the table along with tool tips. Editor dialogs for known
    * properties will be tailored to the data type.
    *
    * @param propertiesOwner
    *            The object owning the properties.
    */
    public void setPropertiesOwner(PropertiesOwner propertiesOwner)
    {
        this.propertiesOwner = propertiesOwner;
        if (propertiesOwner == null)
        {
            propHash = null;
            return;
        }
        // For quick access, construct a hash with upper-case names.
        propHash = new HashMap<String, PropertySpec>();
        for (PropertySpec ps : propertiesOwner.getSupportedProps())
        {
            propHash.put(ps.getName().toUpperCase(), ps);
        }
        if (propertiesOwner instanceof DynamicPropertiesOwner)
        {
            DynamicPropertiesOwner dpo = (DynamicPropertiesOwner)propertiesOwner;
            if (dpo.dynamicPropsAllowed())
                for(PropertySpec ps : dpo.getDynamicPropSpecs())
                    propHash.put(ps.getName().toUpperCase(), ps);
        }


        this.setPropHash(propHash);
        fireTableDataChanged();
    }


    /**
     * Return the property value if the name is defined in the table, or null if
    * not.
    *
    * @param name
    *            the name of the property
    * @return the value of the property or null if undefined.
    */
    public String getProperty(String name)
    {
        for (StringPair sp: this.props)
        {
            if (sp != null && sp.first.equalsIgnoreCase(name))
            {
                return sp.second;
            }
        }
        return null;
    }

    public void rmProperty(String name)
    {
        for (int row = 0; row < props.size(); row++)
        {
            StringPair sp = props.get(row);
            if (sp != null && sp.first.equalsIgnoreCase(name))
            {
                deletePropAt(row);
                changed = true;
                return;
            }
        }
    }

    public void setProperty(String name, String value)
    {
        for (int row = 0; row < props.size(); row++)
        {
            StringPair sp = props.get(row);
            if (sp != null && sp.first.equalsIgnoreCase(name))
            {
                sp.second = value;
                setPropAt(row, sp);
                return;
            }
        }
        // Fell through -- this is a new property
        props.add(new StringPair(name, value));
        fireTableRowsInserted(props.size()-1, props.size()-1);
    }

    /**
     * Return the Property Owner in case a downstream component needs to check something.
     * @return The set properties owner
     */
    public PropertiesOwner getPropertiesOwner()
    {
        return this.propertiesOwner;
    }
 }
