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
package lrgs.rtstat.hosts;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

import javax.swing.AbstractListModel;
import javax.swing.ComboBoxModel;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

public class LrgsConnectionComboBoxModel extends AbstractListModel<LrgsConnection> implements ComboBoxModel<LrgsConnection>
{
    private static final Logger log = OpenDcsLoggerFactory.getLogger();

    private final List<LrgsConnection> hosts = new ArrayList<>();
    private final File lddsConnectionFile;
    private int selectedIndex = -1;
    private Comparator<LrgsConnection> sorter = (left,right) ->
    {
        final Date leftDate = left.getLastUsed();
        final Date rightDate = right.getLastUsed();
        if (left.getHostName() == null || left.getHostName().isEmpty())
        {
            return -1;
        }

        if (leftDate != null && rightDate != null)
        {
            return -1*Long.compare(leftDate.getTime(), rightDate.getTime()); // we're sorting backwards.
        }

        return left.getHostName().compareTo(right.getHostName());
    };

    public LrgsConnectionComboBoxModel(File lddsConnectionFile)
    {
        this.lddsConnectionFile = lddsConnectionFile;
        hosts.add(LrgsConnection.BLANK);
        try (FileInputStream fis = new FileInputStream(lddsConnectionFile);)
        {
            Properties props = new Properties();
            props.load(fis);
            for (String key: props.stringPropertyNames())
            {
                hosts.add(LrgsConnection.fromDdsFile(key, props.getProperty(key)));
            }
            Collections.sort(hosts, sorter);
		}
		catch(IOException ioe)
		{
			log.info("No previously recorded connections");
		}

    }

    @Override
    public int getSize()
    {
        synchronized(hosts)
        {
            return hosts.size();
        }
    }

    @Override
    public LrgsConnection getElementAt(int index)
    {
        synchronized(hosts)
        {
            return hosts.get(index);
        }
    }

    public void addOrReplaceConnection(LrgsConnection c)
    {
        Objects.requireNonNull(c, "Cannot add or replace a null connection");
        if (c == LrgsConnection.BLANK)
        {
            return; // we don't update the blank connection
        }
        final String hostname = c.getHostName();
        if (hostname == null || hostname.trim().isEmpty())
        {
            return;
        }
        synchronized (hosts)
        {
            for (int i = 0; i < hosts.size(); i++)
            {
                LrgsConnection host = hosts.get(i);
                if (host.equals(c))
                {
                    return; // no changes required.
                }
                else if (host.getHostName().equals(c.getHostName()) && !host.equals(c))
                {
                    hosts.set(i, c);
                    Collections.sort(hosts, sorter);
                    setSelectedItem(c);
                    fireContentsChanged(c, 0, hosts.size());
                    writeToDisk();
                    return;
                }
            }
            hosts.add(c);
            Collections.sort(hosts, sorter);
            setSelectedItem(c); // if we're adding it's because of a successful new connection.
            fireIntervalAdded(c, getSize(), getSize());
            writeToDisk();
        }
    }

    private void writeToDisk()
    {
        Properties props = new Properties();
        synchronized(hosts)
        {
            for (LrgsConnection c: hosts)
            {
                if (c != LrgsConnection.BLANK)
                {
                    props.put(c.getHostName(), c.toPropertyEntry());
                }
            }
        }
        try (FileOutputStream fos = new FileOutputStream(lddsConnectionFile))
        {
            props.store(fos, "Recent LRGS Connections.");
        }
        catch (IOException ex)
        {
            log.atError()
               .setCause(ex)
               .log("Unable to write LddsConnections to {}", lddsConnectionFile);
        }
    }

    @Override
    public void setSelectedItem(Object item)
    {
        synchronized(hosts)
        {
            selectedIndex = hosts.indexOf(item);
        }
    }

    @Override
    public Object getSelectedItem()
    {
        synchronized (hosts)
        {
            return selectedIndex >= 0 ? hosts.get(selectedIndex) : null;
        }
    }
}
