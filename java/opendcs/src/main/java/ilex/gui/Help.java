package ilex.gui;

import javax.swing.*;
import java.awt.Desktop;
import java.io.File;
import java.net.URI;
import org.slf4j.LoggerFactory;


public class Help
{
    static final String HELP_URL = "https://opendcs-env.readthedocs.io/";
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(Help.class);

    private static String getUrl()
    {
        String localURL =System.getenv("DECODES_INSTALL_DIR");
        localURL +="/doc/index.html";
        return localURL;
    }

    public static void open(){
        String url = getUrl();
        try {
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
        catch( Throwable e) {
            if(  Desktop.isDesktopSupported()){
                JOptionPane.showMessageDialog(null, "Error opening " + url, "Error", JOptionPane.ERROR_MESSAGE);
            }
            log.error("Error opening '{}' ",HELP_URL,e);
        }
    }


}
