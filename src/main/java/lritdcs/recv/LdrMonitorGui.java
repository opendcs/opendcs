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
    public static void main(String[] args) 
    {
        try 
        {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            new LdrMonitorGui();
        }
        catch(Exception e) 
        {
            e.printStackTrace();
        }
    }
}
