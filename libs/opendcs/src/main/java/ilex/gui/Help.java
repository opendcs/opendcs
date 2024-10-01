package ilex.gui;

import javax.swing.*;
import java.awt.Desktop;
import java.net.URI;
import org.slf4j.LoggerFactory;

public class Help
{
    static final String HELP_URL = "https://opendcs-env.readthedocs.io/";
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(Help.class);
    public static void open(){
        try {
            if(  Desktop.isDesktopSupported())
            {
                Desktop.getDesktop().browse(new URI(HELP_URL));
            }
        }
        catch( Throwable e) {
            if(  Desktop.isDesktopSupported()){
                JOptionPane.showMessageDialog(null, "Error opening " + HELP_URL, "Error", JOptionPane.ERROR_MESSAGE);
            }
            log.error("Error opening '{}' ",HELP_URL,e);
        }
    }


}
