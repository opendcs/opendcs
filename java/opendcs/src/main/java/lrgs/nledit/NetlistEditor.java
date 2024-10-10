package lrgs.nledit;

import ilex.gui.WindowUtility;
import ilex.util.LoadResourceBundle;

import javax.swing.UIManager;

import decodes.util.DecodesSettings;

import java.util.ResourceBundle;

public class NetlistEditor {
    boolean packFrame = false;
	NetlistEditFrame frame;
	private static ResourceBundle labels = null;
	private static ResourceBundle genericLabels = null;
	
    /**Construct the application*/
    public NetlistEditor() {
    	labels = getLabels();
    	genericLabels = getGenericLabels();
    	
        frame = new NetlistEditFrame();
        //Validate frames that have preset sizes
        //Pack frames that have useful preferred size info, e.g. from their layout
        if (packFrame) {
            frame.pack();
        }
        else {
            frame.validate();
        }
		WindowUtility.center(frame).setVisible(true);
    }

	public void load(String filename)
	{
		frame.openFile(filename);
	}

	public static ResourceBundle getLabels() 
	{
		if (labels == null)
		{
			DecodesSettings settings = DecodesSettings.instance();
			//Return the main label descriptions for Net List Editor App
			labels = LoadResourceBundle.getLabelDescriptions(
					"decodes/resources/netlistmaint",
					settings.language);
		}
		return labels;
	}

	public static ResourceBundle getGenericLabels() 
	{
		if (genericLabels == null)
		{
			DecodesSettings settings = DecodesSettings.instance();
			//Load the generic properties file - includes labels that are used
			//in multiple screens
			genericLabels = LoadResourceBundle.getLabelDescriptions(
					"decodes/resources/generic",
					settings.language);
		}
		return genericLabels;
	}
	
    /**Main method*/
    public static void main(String[] args)
	{
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        }
        catch(Exception e) {
            e.printStackTrace();
        }
        //If running standalone needs to load decodes.properties here
        //otherwise it will show only in english
        
        NetlistEditor editor = new NetlistEditor();
		if (args.length > 0)
			editor.load(args[0]);
		editor.frame.isStandAlone = true;
    }
}