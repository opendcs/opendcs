package ilex.gui;

import javax.swing.*;
import java.awt.Desktop;
import java.net.URI;



public class Help
{
    static final String HELP_URL = "https://opendcs-env.readthedocs.io/";

    public static void open(){
        try {
            if(  Desktop.isDesktopSupported())
            {
                Desktop.getDesktop().browse(new URI(HELP_URL));
            }
        }
        catch( Throwable e) {
            JOptionPane.showMessageDialog(null, "Error opening "+HELP_URL, "Error", JOptionPane.ERROR_MESSAGE);
        }
    }


}
