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
package decodes.db;

import java.util.*;
import java.util.stream.Collectors;

import opendcs.dao.CachableDbObject;

import decodes.sql.DbKey;
import java.io.Serializable;


/**
 * An Enum is a named collection of related values, each of which is
 * stored in an EnumValue object.
 * _id is stored in the IdDatabaseObject superclass.
 */
public class DbEnum  extends IdDatabaseObject implements CachableDbObject, Serializable
{
    private static final long serialVersionUID = -7534343805851676281L;

    /** The name of this enumeration. */
    public String enumName;

    /**
      The collection of Enum Values associated with this enum.
      Value-names are converted to lower-case before being stored in the hash.
    */
    private Vector<EnumValue> enumValues;

    /** The default value of the enumeration, or null if no default specified. */
    private String defaultValue;

    /** Description of what this enum is used for */
    private String description = null;

    /**
     * Construct from a name.  The case of the name is preserved.
     * @param name the name of the enumeration
     */
    public DbEnum(String name)
    {
        super(); // sets _id to Constants.undefinedId;

        enumName = name;
        enumValues = new Vector<EnumValue>();
        Database db = Database.getDb();
        if (db != null)
            db.enumList.addEnum(this);
        defaultValue = null;
    }

    /**
     * Used by SQL database to construct enum with unique numeric key.
     * @param id the unique ID
     * @param name the name
     */
    public DbEnum(DbKey id, String name)
    {
        super(id);
        enumName = name;
        enumValues = new Vector<EnumValue>();
        Database.getDb().enumList.addEnum(this);
        defaultValue = null;
    }

    /**
    * This overrides the DatabaseObject method.  This returns "Enum".
    */
    public String getObjectType() {
        return "Enum";
    }

    /**
    * Adds a new value to this enumeration.
    * Typically called at initialization time.
    * The value's name must be unique among all the EnumValues that are
    * part of this Enum.
     * @param v the enum value
     * @param desc the description
     * @param exec (optional) name of executable class
     * @param edit (optional) name of editor class
     * @return EnumValue object created.
    * @throws ValueAlreadyDefinedException if an entry already
    * exists with this value-string in the enumeration.
     */
    public EnumValue addValue(String v, String desc, String exec, String edit)
        throws ValueAlreadyDefinedException
    {
        //v = v.toLowerCase();
        if (findEnumValue(v) != null)
            throw new ValueAlreadyDefinedException(
                "Enum "+enumName+" already has a definition for '"+v+"'");
        EnumValue ret = new EnumValue(this, v, desc, exec, edit);
        enumValues.add(ret);
        return ret;
    }

    public void addValue(EnumValue ev)
    {
        enumValues.add(ev);
    }

    /**
     * Replaces the enumeration value
     * @param v the value
     * @param desc the description
     * @param exec (optional) name of executable class
     * @param edit (optional) name of editor class
     * @return EnumValue object created.
     */
    public EnumValue replaceValue(String v, String desc, String exec, String edit)
    {
        //v = v.toLowerCase();
        EnumValue ev = findEnumValue(v);
        if (ev != null)
            enumValues.remove(ev);
        ev = new EnumValue(this, v, desc, exec, edit);
        enumValues.add(ev);
        return ev;
    }

    /**
      Removes the value if it is present, else does nothing.
     * @param v the value to remove
     */
    public void removeValue(String v)
    {
        EnumValue ev = findEnumValue(v);
        if (ev != null)
            enumValues.remove(ev);
        if (defaultValue != null && defaultValue.equalsIgnoreCase(v))
            defaultValue = null;
    }


    /**
    * Returns an EnumValue corresponding to the given string.  The case of
    * the argument is not significant -- it is converted to lowercase before
    * the search.  This returns null if the value is not found.
     * @param value the value to search for.
     * @return EnumValue object or null if not found.
     */
    public EnumValue findEnumValue(String value)
    {
        for(Iterator<EnumValue> it = enumValues.iterator(); it.hasNext();)
        {
            EnumValue ev = it.next();
            if (value.equalsIgnoreCase(ev.getValue()))
                return ev;
        }
        return null;
    }

    /** @return all of the EnumValues as a Collection. */
    public Collection<EnumValue> values()
    {
        return enumValues;
    }

    /** return an iterator over the list of EnumValues.  */
    public Iterator<EnumValue> iterator()
    {
        return enumValues.iterator();
    }

    /**
     * Return an EnumValue by index.
     * @param index the index
     * @return an EnumValue by index, or null if out of range.
     */
    public EnumValue valueAt(int index)
    {
        if (index < 0 || index >= enumValues.size())
            return null;
        return enumValues.elementAt(index);
    }

    /**
     * @return number of enum values in this enum.
     */
    public int size()
    {
        return enumValues.size();
    }

    /**
      Sort the enum values by each entry's sort order and value name.
    */
    public void sort()
    {
        Collections.sort(enumValues,
            new Comparator<EnumValue>()
            {
                public int compare(EnumValue ev1, EnumValue ev2)
                {
                    int i = ev1.getSortNumber() - ev2.getSortNumber();
                    if (i != 0)
                        return i;
                    return ev1.getValue().compareToIgnoreCase(ev2.getValue());
                }
            });
    }

    /**
      Returns the default EnumValue for this enumeration, or null if none
      was specified.
      To retrieve the complete EnumValue object, pass the returned string
      to findEnumValue.
     * @return the default value or null if none is specified.
     */
    public String getDefault()
    {
        return defaultValue;
    }

    /**
      Sets the default value for this enumeration.
      The passed name must represent an EnumValue object stored within
      this Enum collection.
      @param ev the default enum value
    */
    public void setDefault(String ev) { defaultValue = ev; }

    @Override
    public void prepareForExec()
        throws IncompleteDatabaseException, InvalidDatabaseException
    {
    }

    @Override
    public boolean isPrepared()
    {
        return true;
    }

    /**
    * This overrides the DatabaseObject.read() method, but it does nothing.
    * I/O for this is handled by the EnumList.
    */
    public void read()
        throws DatabaseException
    {
    }

    /**
    * This overrides the DatabaseObject.write() method, but it does nothing.
    * I/O for this is handled by the EnumList.
    */
    public void write()
        throws DatabaseException
    {
    }

    @Override
    public String getUniqueName()
    {
        return enumName;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    @Override
    public DbKey getKey()
    {
        return getId();
    }

    public void clear()
    {
        enumValues.clear();
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(enumName).append("{")
          .append("id=").append(getId().getValue()).append(",")
          .append("description=").append(description).append(",")
          .append("defaultValue=")
            .append(defaultValue != null ? defaultValue : "not set").append(",")
          .append("values=[")
          .append(String.join(",",
                              enumValues.stream()
                                        .map(EnumValue::getFullName)
                                        .collect(Collectors.toList())
                            )
           )
          .append("]}");
        return sb.toString();
    }
}
