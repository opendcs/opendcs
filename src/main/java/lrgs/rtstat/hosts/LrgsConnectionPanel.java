package lrgs.rtstat.hosts;

import javax.swing.JPanel;
import java.awt.FlowLayout;
import javax.swing.JComboBox;
import javax.swing.JTextField;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

import decodes.util.DecodesVersion;
import ilex.util.AuthException;
import ilex.util.DesEncrypter;
import lrgs.gui.MessageBrowser;
import lrgs.rtstat.RtStatFrame;

import java.awt.Component;
import java.awt.Dimension;
import javax.swing.JButton;

import java.util.Locale;
import java.util.ResourceBundle;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

public class LrgsConnectionPanel extends JPanel {
	private static final ResourceBundle labels = ResourceBundle.getBundle("decodes.resources.rtstat", Locale.getDefault()); //$NON-NLS-1$
	private static String pwk = MessageBrowser.class.toString() + 
		(""+Math.PI).substring(3, 10) + DecodesVersion.class.toString();
	private static final long serialVersionUID = 1L;
	private JComboBox<LrgsConnection> hostCombo;
	private JTextField portField;
	private JTextField usernameField;
	private JTextField passwordField;

	private final LrgsConnectionPanelController controller;

	/**
	 * Create the panel.
	 */
	public LrgsConnectionPanel()
	{
		this(new LrgsConnectionPanelController());
	}

	private LrgsConnectionPanel(LrgsConnectionPanelController controller)
	{
		this.controller = controller;
		setLayout(new FlowLayout(FlowLayout.LEFT, 5, 0));
		
		JPanel panel = new JPanel();
		FlowLayout flowLayout = (FlowLayout) panel.getLayout();
		flowLayout.setAlignment(FlowLayout.LEFT);
		add(panel);
		
		JLabel lblNewLabel = new JLabel(labels.getString("RtStatFrame.host")); //$NON-NLS-1$
		lblNewLabel.setHorizontalAlignment(SwingConstants.LEFT);
		panel.add(lblNewLabel);
		lblNewLabel.setLabelFor(hostCombo);
		
		hostCombo = new JComboBox<>();
		hostCombo.setName("hostCombo");
		hostCombo.setMinimumSize(new Dimension(120, 24));
		hostCombo.setAlignmentX(Component.LEFT_ALIGNMENT);
		hostCombo.setEditable(true);
		hostCombo.addActionListener(e -> changeConnection());
		panel.add(hostCombo);
		
		JPanel panel_1 = new JPanel();
		add(panel_1);
		
		JLabel lblNewLabel_1 = new JLabel(labels.getString("RtStatFrame.port")); //$NON-NLS-1$
		panel_1.add(lblNewLabel_1);
		lblNewLabel_1.setLabelFor(portField);
		
		portField = new JTextField();
		portField.setMaximumSize(new Dimension(32, 2147483647));
		panel_1.add(portField);
		portField.setText("16003");
		portField.setColumns(10);
		
		JPanel panel_2 = new JPanel();
		add(panel_2);
		
		JLabel lblNewLabel_2 = new JLabel(labels.getString("RtStatFrame.user")); //$NON-NLS-1$
		panel_2.add(lblNewLabel_2);
		
		usernameField = new JTextField();
		lblNewLabel_2.setLabelFor(usernameField);
		panel_2.add(usernameField);
		usernameField.setColumns(10);
		
		JPanel panel_3 = new JPanel();
		add(panel_3);
		
		JLabel lblNewLabel_3 = new JLabel(labels.getString("RtStatFrame.password")); //$NON-NLS-1$
		panel_3.add(lblNewLabel_3);
		lblNewLabel_3.setLabelFor(passwordField);
		
		passwordField = new JTextField();
		panel_3.add(passwordField);
		passwordField.setColumns(10);
		
		JPanel panel_4 = new JPanel();
		add(panel_4);
		
		JButton connectButton = new JButton(labels.getString("RtStatFrame.connectButton")); //$NON-NLS-1$
		connectButton.addActionListener(e -> controller.connect());		
		panel_4.add(connectButton);
		
		JButton pauseButton = new JButton(labels.getString("RtStatFrame.pause")); //$NON-NLS-1$
		panel_4.add(pauseButton);

		controller.setView(this);
	}

	private void changeConnection()
	{
		LrgsConnection c = (LrgsConnection)hostCombo.getSelectedItem();
        if (c == LrgsConnection.BLANK)
		{
			portField.setText("16003");
			usernameField.setText("");
			passwordField.setText("");
		}
		else
		{
			portField.setText(""+c.getPort());
			usernameField.setText(c.getUsername());

			String pw = c.getPassword();
			if (pw != null && !pw.isEmpty())
			{		
				try
				{
					DesEncrypter de = new DesEncrypter(pwk);
					String dpw = de.decrypt(pw);
					passwordField.setText(dpw);
				}
				catch (AuthException e)
				{
				}
			}
			else
			{
				passwordField.setText("");
			}
		}
		controller.changeConnection(c);
	}

	public void setModel(LrgsConnectionComboBoxModel model)
	{
		this.hostCombo.setModel(model);
		System.out.println(model.getSize());
	}

}
