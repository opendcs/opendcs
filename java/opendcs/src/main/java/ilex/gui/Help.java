package ilex.gui;

import javax.swing.*;
import java.awt.Desktop;
import java.net.URI;
import org.slf4j.LoggerFactory;

import lrgs.gui.LrgsBuild;

public class Help
{
    static final String HELP_URL = "https://opendcs-env.readthedocs.io/";
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(Help.class);

    private static String getUrl()
    {
        return HELP_URL;
        // // "7.0.15-RC02";
        // String version = LrgsBuild.version;
        // Boolean rc = version.toLowerCase().contains("rc");
        // String mainVersion = version.split(".")[0];

        // if(rc)
        //     {
        //       return HELP_URL+ "en/7.0.14/";
        //     }

        // return HELP_URL+ "en/"+version+"/";
        }

    public static void open(){
        String url = getUrl();
        try {
            if(  Desktop.isDesktopSupported())
            {

                Desktop.getDesktop().browse(new URI(url));
            }
        }
        catch( Throwable e) {
            if(  Desktop.isDesktopSupported()){
                JOptionPane.showMessageDialog(null, "Error opening " + url, "Error", JOptionPane.ERROR_MESSAGE);
            }
            log.error("Error opening '{}' ",HELP_URL,e);
        }
    }


}
