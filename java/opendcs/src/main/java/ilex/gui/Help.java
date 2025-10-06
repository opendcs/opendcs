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

import javax.swing.*;
import java.awt.Desktop;
import java.io.File;
import java.net.URI;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;


public class Help
{
    static final String HELP_URL = "https://opendcs-env.readthedocs.io/";
    private static final Logger log = OpenDcsLoggerFactory.getLogger();
    private static String getUrl()
    {
        String localURL =System.getProperty("DCSTOOL_HOME");
        localURL +="/doc/index.html";

        File f = new File(localURL);
        if (f.exists())
        {
            return localURL;
        }
        else
        {
            return HELP_URL;
        }
    }

    public static void open()
    {
        String url="";
        try
        {
            url = getUrl();
            if(  Desktop.isDesktopSupported())
            {
                if( url.startsWith("http"))
                {
                    Desktop.getDesktop().browse(new URI(url));
                }
                else
                {
                    File file = new File(url);
                    Desktop.getDesktop().browse(file.toURI());
                }
            }
        }
        catch (Throwable ex)
        {
            log.atError().setCause(ex).log("Error opening '{}' ", url, ex);
            if (Desktop.isDesktopSupported())
            {
                JOptionPane.showMessageDialog(null, "Error opening " + url, "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}