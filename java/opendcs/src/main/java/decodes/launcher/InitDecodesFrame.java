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

/*
 * This is open-source software written by Sutron Corporation under
 * contract to the federal government. Anyone is free to copy and use this
 * source code for any purpos, except that no part of the information
 * contained in this file may be claimed to be proprietary.
 *
 * Except for specific contractual terms between Sutron and the federal 
 * government, this source code is provided completely without warranty.
 */
package decodes.launcher;

import ilex.util.EnvExpander;

import java.awt.*;
import java.util.ResourceBundle;
import javax.swing.*;

import org.opendcs.gui.GuiHelpers;
import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import decodes.util.ResourceFactory;

public class InitDecodesFrame extends JFrame 
{
    private static final Logger log = OpenDcsLoggerFactory.getLogger();
	private static ResourceBundle labels;
    JPanel logoPanel = new JPanel();
    BorderLayout borderLayout1 = new BorderLayout();
    JPanel jPanel1 = new JPanel();
    JLabel versionLabel = new JLabel();
    JPanel jPanel2 = new JPanel();
    GridLayout gridLayout1 = new GridLayout();
    JLabel jLabel1 = new JLabel();
    JLabel jLabel2 = new JLabel();
    JPanel emptyPanele = new JPanel();
    JPanel emptyPanelw = new JPanel();

    public InitDecodesFrame(ResourceBundle labels)
	{
    	this.labels = labels;
        try 
        {
            jbInit();
        	versionLabel.setText(ResourceFactory.instance().startTag());
			ImageIcon logo = new ImageIcon(
				EnvExpander.expand("$DECODES_INSTALL_DIR/icons/gears.png"));
			logoPanel.add(new JLabel(logo));
			this.setSize(440, 300);
        }
        catch(Exception ex) 
        {
            GuiHelpers.logGuiComponentInit(log, ex);
        }
    }
    private void jbInit() throws Exception {
        this.getContentPane().setLayout(borderLayout1);
        versionLabel.setText("DECODES, Ver. ");
        jPanel2.setLayout(gridLayout1);
        gridLayout1.setRows(2);
        gridLayout1.setColumns(1);
        jLabel1.setFont(new java.awt.Font("Dialog", 2, 24));
        jLabel1.setHorizontalAlignment(SwingConstants.CENTER);
        jLabel1.setText(
        		labels.getString("InitDecodesFrame.loadDecodesMsg"));
        jLabel2.setHorizontalAlignment(SwingConstants.CENTER);
        jLabel2.setText(
        		labels.getString("InitDecodesFrame.pleaseWaitMsg"));
        logoPanel.setPreferredSize(new Dimension(400, 60));//
        jPanel2.setBorder(BorderFactory.createLoweredBevelBorder());
		logoPanel.setBackground(Color.white);
        this.getContentPane().add(logoPanel, BorderLayout.SOUTH);
		jPanel1.setBackground(Color.white);
        this.getContentPane().add(jPanel1, BorderLayout.NORTH);
        jPanel1.add(versionLabel, null);
        this.getContentPane().add(jPanel2, BorderLayout.CENTER);
		emptyPanele.add(new JLabel(" "));
		emptyPanele.setBackground(Color.white);
        this.getContentPane().add(emptyPanele, BorderLayout.EAST);
		emptyPanelw.add(new JLabel(" "));
		emptyPanelw.setBackground(Color.white);
        this.getContentPane().add(emptyPanelw, BorderLayout.WEST);
		

        jPanel2.add(jLabel1, null);
        jPanel2.add(jLabel2, null);
    }
}
