package decodes.syncgui;

import java.awt.*;
import javax.swing.*;


/**
Top level panel with some descriptive info about the hub.
*/
public class TopPanel extends JPanel 
{
	BorderLayout borderLayout1 = new BorderLayout();
	JPanel jPanel1 = new JPanel();
	JLabel jLabel1 = new JLabel();
	FlowLayout flowLayout1 = new FlowLayout();
	JPanel jPanel2 = new JPanel();
	JLabel jLabel2 = new JLabel();
	JTextField hubHomeField = new JTextField();
	JLabel jLabel3 = new JLabel();
	JTextField hubNameField = new JTextField();
	JLabel jLabel4 = new JLabel();
	JTextField numDistrictsField = new JTextField();
	GridBagLayout gridBagLayout1 = new GridBagLayout();

	public TopPanel() {
		try {
			jbInit();
		}
		catch(Exception ex) {
			ex.printStackTrace();
		}
		SyncConfig sc = SyncConfig.instance();
		hubHomeField.setText(sc.getHubHome());
		hubNameField.setText(sc.getHubName());
		numDistrictsField.setText("" + sc.districts.size());
	}

	void jbInit() throws Exception {
		this.setLayout(borderLayout1);
		jLabel1.setText("DECODES Database Hub");
		jPanel1.setLayout(flowLayout1);
		flowLayout1.setVgap(10);
		jPanel2.setLayout(gridBagLayout1);
		jLabel2.setText("Hub Home URL:");
		hubHomeField.setDoubleBuffered(false);
		hubHomeField.setEditable(false);
		hubHomeField.setText("unknown");
		jLabel3.setText("Hub Name:");
		hubNameField.setEditable(false);
		hubNameField.setText("unknown");
		jLabel4.setText("Number of Districts:");
		numDistrictsField.setEditable(false);
		numDistrictsField.setSelectionStart(11);
		numDistrictsField.setText("2");
		this.add(jPanel1, BorderLayout.NORTH);
		jPanel1.add(jLabel1, null);
		this.add(jPanel2, BorderLayout.CENTER);
		jPanel2.add(hubHomeField,	 new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0
						,GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, new Insets(10, 3, 3, 45), 0, 0));
		jPanel2.add(hubNameField,	 new GridBagConstraints(1, 1, 1, 1, 1.0, 0.0
						,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(3, 3, 3, 45), 0, 0));
		jPanel2.add(numDistrictsField,	 new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0
						,GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(3, 3, 3, 0), 33, 0));
		jPanel2.add(jLabel2,	 new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
						,GridBagConstraints.NORTHEAST, GridBagConstraints.NONE, new Insets(10, 35, 3, 3), 0, 0));
		jPanel2.add(jLabel3,	 new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0
						,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(3, 35, 3, 0), 0, 0));
		jPanel2.add(jLabel4,		new GridBagConstraints(0, 2, 1, 1, 0.0, 1.0
						,GridBagConstraints.NORTHEAST, GridBagConstraints.NONE, new Insets(3, 35, 3, 3), 0, 0));
	}
}
