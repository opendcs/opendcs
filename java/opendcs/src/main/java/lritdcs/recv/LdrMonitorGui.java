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
package lritdcs.recv;

import ilex.gui.WindowUtility;

import javax.swing.UIManager;

public class LdrMonitorGui
{
    boolean packFrame = false;

    //Construct the application
    public LdrMonitorGui()
        throws Exception
    {
        LdrMonitorFrame frame = new LdrMonitorFrame();
        //Validate frames that have preset sizes
        //Pack frames that have useful preferred size info, e.g. from their layout
        if (packFrame)
        {
            frame.pack();
        }
        else
        {
            frame.validate();
        }
        WindowUtility.center(frame);

        LritDcsRecvConfig cfg = LritDcsRecvConfig.instance();
        cfg.setPropFile("ldr.conf");
        cfg.load();
        frame.init();
        frame.setVisible(true);
    }

    //Main method
    public static void main(String[] args) throws Exception
    {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        new LdrMonitorGui();
    }
}
