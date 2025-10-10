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
package lrgs.nledit;

import ilex.gui.WindowUtility;
import ilex.util.LoadResourceBundle;

import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

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

    /**Main method
     * @throws UnsupportedLookAndFeelException
     * @throws IllegalAccessException
     * @throws InstantiationException
     * @throws ClassNotFoundException */
    public static void main(String[] args) throws ClassNotFoundException, InstantiationException, IllegalAccessException, UnsupportedLookAndFeelException
	{
		UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

        //If running standalone needs to load decodes.properties here
        //otherwise it will show only in english

        NetlistEditor editor = new NetlistEditor();
		if (args.length > 0)
			editor.load(args[0]);
		editor.frame.isStandAlone = true;
    }
}