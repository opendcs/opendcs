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
package decodes.dbeditor.platform;

import java.util.ArrayList;
import java.util.ResourceBundle;
import java.util.Vector;

import javax.swing.table.AbstractTableModel;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import decodes.db.Constants;
import decodes.db.Database;
import decodes.db.DatabaseException;
import decodes.db.Platform;
import decodes.db.Site;
import decodes.db.SiteName;
import decodes.db.TransportMedium;
import decodes.dbeditor.PlatformSelectPanel;
import decodes.gui.TopFrame;
import decodes.util.DecodesSettings;


public class PlatformSelectTableModel extends AbstractTableModel
{
    private static final Logger log = OpenDcsLoggerFactory.getLogger();
    private Database db;
    private PlatformSelectPanel panel;
    private PlatformSelectColumnizer columnizer;    
    private final ArrayList<Platform> platformList = new ArrayList<>();
    String mediumType = null;

    private static ResourceBundle generic = ResourceBundle.getBundle("decodes/resources/generic");    
    private static ResourceBundle dbedit = ResourceBundle.getBundle("decodes/resources/dbedit");
    static String colNamesNoDesig[] = 
    {        
        generic.getString("platform"),
        dbedit.getString("PlatformSelectPanel.agency"),
        dbedit.getString("PlatformSelectPanel.transport"),
        dbedit.getString("PlatformSelectPanel.config"),
        generic.getString("expiration"),
        generic.getString("description")
    };
    static String colNamesDesig[] = 
    {
        generic.getString("platform"),
        "Designator",
        dbedit.getString("PlatformSelectPanel.agency"),
        dbedit.getString("PlatformSelectPanel.transport"),
        dbedit.getString("PlatformSelectPanel.config"),
        generic.getString("expiration"),
        generic.getString("description")
    };

    String columnNames[] = DecodesSettings.instance().platformListDesignatorCol
                         ? colNamesDesig : colNamesNoDesig;

    int columnWidths [] = DecodesSettings.instance().platformListDesignatorCol
                        ? new int[] { 18, 6, 6, 20, 20,10, 30 } : new int[] { 22, 6, 20, 20,10, 33 };


    public PlatformSelectTableModel(PlatformSelectPanel panel, String medTyp, Database db)
    {
        this(panel, db);
        this.mediumType = medTyp;
        for(Platform platform : db.platformList.getPlatformVector())
        {
            // NOTE: Medium Type NULL means display all platforms.
            if (mediumType == null
                    // Direct match for specified TM type
                    || platform.getTransportMedium(mediumType) != null
                    // If any GOES type then only display GOES platforms.
                    || (mediumType.equalsIgnoreCase(Constants.medium_Goes)
                            && (platform.getTransportMedium(Constants.medium_GoesST) != null
                                    || platform.getTransportMedium(Constants.medium_GoesRD) != null))
                    // If MT='Poll', display any polling-type platform.
                    || (mediumType.equalsIgnoreCase("poll")
                            && (platform.getTransportMedium("polled-modem") != null
                                    || platform.getTransportMedium("polled-tcp") != null
                                    || platform.getTransportMedium("incoming-tcp") != null)))
                platformList.add(platform);
        }

    }

    public PlatformSelectTableModel(PlatformSelectPanel panel, Site site, Database db)    
    {
        this(panel, db);
        
        Vector<Platform> fvec = db.platformList.getPlatformVector();
        SiteName usgsName = site.getName(Constants.snt_USGS);
        if (usgsName != null)
        {
            fvec.forEach(p ->
            {
                if ( p.getSite() != null )
                {
                    SiteName sn = p.getSite().getName(Constants.snt_USGS);
                    if(sn != null)
                    {
                        if ( sn.equals(usgsName) )
                        {
                            platformList.add(p);
                        }
                    }

                }
            });
        }
    }

    public PlatformSelectTableModel(PlatformSelectPanel platformSelectPanel, Database db) {
        this(db);
        this.panel = platformSelectPanel;
    }

    public PlatformSelectTableModel(Database db) {
        super();
        this.db = db;
        this.columnizer = new PlatformSelectColumnizer(mediumType);
    }

    public void addPlatform(Platform ob)
    {
        db.platformList.add(ob);
        platformList.add(ob);
        fireTableDataChanged();
    }

    public void deletePlatformAt(int index)
    {
        deletePlatform(platformList.get(index));
    }

    public void deletePlatform(Platform ob)
    {
        platformList.remove(ob);
        try
        {
            if (ob.idIsSet())
            {
                db.platformList.removePlatform(ob);
                db.getDbIo().deletePlatform(ob);
            }
        }
        catch(DatabaseException e)
        {
            TopFrame.instance().showError(e.toString());
        }
        fireTableDataChanged();
    }

    public void replacePlatform(Platform oldp, Platform newp)
    {
        db.platformList.removePlatform(oldp);
        if (!platformList.remove(oldp))
        {
            log.trace("oldp was not in list.");
        }
        addPlatform(newp);
        fireTableDataChanged();
    }

    public int getColumnCount() { return columnNames.length; }

    public String getColumnName(int col)
    {
        return columnNames[col];
    }

    public boolean isCellEditable(int modelRow, int c) { return false; }

    public int getRowCount()
    {
        return platformList.size();
    }

    public Object getValueAt(int modelRow, int c)
    {
        Platform p = getPlatformAt(modelRow);
        return this.columnizer.getColumn(p, c);
    }

    public Platform getPlatformAt(int modelRow)
    {
        return platformList.get(modelRow);
    }

    public Object getRowObject(int modelRow)
    {
        return platformList.get(modelRow);
    }

    /**
     * Since this model allows the user to configured additional fields,
     * for the filters we need to be able to lookup the colum index at runtime.
     * @param columnName
     * @return
     */
    public int getColumnFor(String columnName)
    {
        for (int i = 0; i < columnNames.length; i++)
        {
            if (columnNames[i].equals(columnName))
            {
                return i;
            }
        }
        return -1;
    }

    /**
    Helper class to retrieve platform fields by column number. Used for
    displaying values in the table and for sorting.
    */
    public static final class PlatformSelectColumnizer
    {
        private boolean desig = DecodesSettings.instance().platformListDesignatorCol;
        private String mediumType = null;
        private boolean isGOES = false;
        private boolean isPoll = false;

        public PlatformSelectColumnizer(String mediumType)
        {
            this.mediumType = mediumType;
            if (mediumType != null)
            {
                isGOES = mediumType.equalsIgnoreCase(Constants.medium_Goes)
                        || mediumType.equalsIgnoreCase(Constants.medium_GoesST)
                        || mediumType.equalsIgnoreCase(Constants.medium_GoesRD);
                isPoll = mediumType.equalsIgnoreCase("poll")
                        || mediumType.equalsIgnoreCase("polled-modem")
                        || mediumType.equalsIgnoreCase("polled-tcp")
                        || mediumType.equalsIgnoreCase("incoming-tcp");
            }
        }

        public Object getColumn(Platform p, int c)
        {
            switch(c)
            {
                case 0: // Site + Designator
                {
                    if (p.getSite() == null)
                        return "";
                    Site site = p.getSite();
                    SiteName sn = site.getPreferredName();
                    if ( sn == null )
                        return "";
                    String r = sn.getNameValue();
                    if ( r == null ) 
                        return "";
                    String d = p.getPlatformDesignator();
                    if (d != null)
                        r = r + "-" + d;
                    return r;
                }
                case 1: // Desig or Agency
                    if (desig)
                        return p.getPlatformDesignator() == null ? "" : p.getPlatformDesignator();
                    else
                        return p.agency == null ? "" : p.agency;
                case 2: // Agency or Transport-ID
                {
                    if (desig)
                        return p.agency == null ? "" : p.agency;
                    else
                        return getTM(p);
                }
                case 3: // Transport-ID or Config
                {
                    if (desig)
                        return getTM(p);
                    else
                        return p.getConfigName();
                }
                case 4: // Config or Expiration
                {
                    if (desig)
                    {
                        return p.getConfigName();
                    }
                    else if (p.expiration == null)
                    {
                        return null;
                    }
                    else
                    {
                        return decodes.db.Constants.defaultDateFormat.format(p.expiration);
                    }
                }
                case 5: // Expiration or Description
                {
                    if (desig)
                    {
                        if (p.expiration == null)
                        {
                            return null;
                        }
                        else
                        {
                            return decodes.db.Constants.defaultDateFormat.format(p.expiration);
                        }
                    }
                    else
                    {
                        return p.description == null ? "" : p.description;
                    }
                }
                case 6: // desig must be true. return description
                    return p.description == null ? "" : p.description;
                default:
                    return "";
            }
        }

        private String getTM(Platform p)
        {
            if (mediumType == null)
            {
                return p.getPreferredTransportId();                
            }
            TransportMedium tm = p.getTransportMedium(mediumType);
            if (tm != null)
            {
                return tm.getMediumId();
            }

            // If  GOES type display any GOES TM.
            if (isGOES && ((tm = p.getTransportMedium(Constants.medium_GoesST)) != null
                          || (tm = p.getTransportMedium(Constants.medium_GoesRD)) != null))
            {
                return tm.getMediumId();
            }

            if (isPoll && ((tm = p.getTransportMedium(Constants.medium_PolledModem)) != null
                            || (tm = p.getTransportMedium(Constants.medium_PolledTcp)) != null
                            || (tm = p.getTransportMedium("incoming-tcp")) != null))
            {
                return tm.getMediumId();
            }

            return p.getPreferredTransportId();
        }
    }

}
