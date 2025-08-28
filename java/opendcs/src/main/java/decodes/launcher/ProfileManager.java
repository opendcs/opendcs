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
package decodes.launcher;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import decodes.tsdb.TsdbAppTemplate;
import lrgs.gui.DecodesInterface;

public class ProfileManager extends TsdbAppTemplate
{
    private static final Logger log = OpenDcsLoggerFactory.getLogger();
    public static String module = "ProfileManager";
    private ProfileManagerFrame pmFrame = null;
    private boolean exitOnClose = true;


    public ProfileManager()
    {
        super("gui.log");
        this.noExitAfterRunApp = true;
    }

    @Override
    protected void runApp() throws Exception
    {
        pmFrame = new ProfileManagerFrame();
        pmFrame.setVisible(true);
        pmFrame.addWindowListener(new WindowAdapter()
        {
            public void windowClosing(WindowEvent e)
            {
                close();
            }
        });
    }

    public void setExitOnClose(boolean exitOnClose)
    {
        this.exitOnClose = exitOnClose;
    }


    protected void close()
    {
        if (pmFrame != null)
		{
            pmFrame.dispose();
		}
        if (exitOnClose)
		{
            System.exit(0);
		}
    }

    public static void main(String[] args)
    {
        DecodesInterface.setGUI(true);
        ProfileManager guiApp = new ProfileManager();
        try
        {
            guiApp.setExitOnClose(true);
            guiApp.execute(args);
        }
        catch (Exception ex)
        {
            log.atError().setCause(ex).log("Can not initialize.");
        }
    }

    public ProfileManagerFrame getFrame()
    {
        return pmFrame;
    }

    /** Have to overload execute() -- we don't want to connect to DB. */
    public void execute(String args[])
        throws Exception
    {
        addCustomArgs(cmdLineArgs);
        parseArgs(args);
        runApp();
    }
}
