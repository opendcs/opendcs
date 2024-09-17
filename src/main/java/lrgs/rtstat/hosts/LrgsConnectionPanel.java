package lrgs.rtstat.hosts;

import javax.swing.JPanel;
import java.awt.FlowLayout;
import javax.swing.JComboBox;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;

import org.opendcs.gui.GuiConstants;
import org.opendcs.gui.PasswordWithShow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import decodes.util.DecodesVersion;
import lrgs.gui.MessageBrowser;

import java.awt.Component;
import java.awt.Dimension;
import javax.swing.JButton;

import java.util.Date;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Function;

public final class LrgsConnectionPanel extends JPanel
{
    private static final Logger log = LoggerFactory.getLogger(LrgsConnectionPanel.class);
    private static final ResourceBundle labels = ResourceBundle.getBundle("decodes.resources.rtstat", Locale.getDefault()); //$NON-NLS-1$
    public static String pwk = MessageBrowser.class.toString() +
        (""+Math.PI).substring(3, 10) + DecodesVersion.class.toString();
    private static final long serialVersionUID = 1L;
    private JComboBox<LrgsConnection> hostCombo;
    private JTextField portField;
    private JTextField usernameField;
    private JButton pausedButton;
    private PasswordWithShow passwordField;
    private boolean paused = false;

    private final LrgsConnectionPanelController controller;

    /**
     * Create the panel.
     */
    public LrgsConnectionPanel()
    {
        this(new LrgsConnectionPanelController(), true);
    }

    public LrgsConnectionPanel(boolean showPause)
    {
        this(new LrgsConnectionPanelController(), showPause);
    }

    private LrgsConnectionPanel(LrgsConnectionPanelController controller, boolean showPause)
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
        hostCombo.setRenderer(new ConnectionRender());
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

        passwordField = new PasswordWithShow(GuiConstants.DEFAULT_PASSWORD_WITH);
        panel_3.add(passwordField);

        JPanel panel_4 = new JPanel();
        add(panel_4);

        JButton connectButton = new JButton(labels.getString("RtStatFrame.connectButton")); //$NON-NLS-1$
        connectButton.addActionListener(e -> connect());
        panel_4.add(connectButton);

        pausedButton = new JButton(labels.getString("RtStatFrame.pause")); //$NON-NLS-1$
        pausedButton.addActionListener(e -> 
        {
            paused = !paused;
            pause(paused);
        });
        panel_4.add(pausedButton);
        pausedButton.setVisible(showPause);

        controller.setView(this);


        int minWidth = passwordField.getMinimumSize().width + lblNewLabel_3.getMinimumSize().width + 50;
        this.setMinimumSize(new Dimension(minWidth, 40));
    }

    private void connect()
    {
        paused = false;
        pause(false);
        new SwingWorker<LrgsConnection, Void>()
        {
            // TODO. This should actually disable the button until the connection is finished.
            @Override
            protected LrgsConnection doInBackground() throws Exception
            {
                // Always build the connection from the fields, the user may have change a single setting.
                LrgsConnection c = connectionFromFields();
                if (controller.connect(c))
                {
                    return c;
                }
                else
                {
                    return null;
                }
            }

            @Override
            protected void done()
            {
                try
                {
                    LrgsConnection c = get();
                    if (c != null)
                    {
                        ((LrgsConnectionComboBoxModel)hostCombo.getModel())
                            .addOrReplaceConnection(new LrgsConnection(c, new Date()));
                    }
                }
                catch (InterruptedException | ExecutionException ex)
                {
                    log.atError()
                       .setCause(ex)
                       .log("Unable to connect to LRGS.");
                    JOptionPane.showMessageDialog(LrgsConnectionPanel.this,
                                                  "Unable to connect to LRGS.",
                                                  "Error!",
                                                  JOptionPane.ERROR_MESSAGE
                                                  );
                }
            }

        }.execute();

    }

    private void changeConnection()
    {
        LrgsConnection c = (LrgsConnection)hostCombo.getSelectedItem();
        if (c == null)
        {
            return;
        }
        else if (c == LrgsConnection.BLANK)
        {
            portField.setText("16003");
            usernameField.setText("");
            passwordField.setText("");
        }
        else
        {
            portField.setText(""+c.getPort());
            usernameField.setText(c.getUsername());

            String pw = LrgsConnection.decryptPassword(c, LrgsConnectionPanel.pwk);
            passwordField.setText(pw);
        }
        new SwingWorker<Void,Void>()
        {

            @Override
            protected Void doInBackground() throws Exception
            {
                controller.changeConnection(c);
                return null;
            }
        }.execute();
    }

    private void pause(boolean pause)
    {
        if (paused)
        {
            pausedButton.setText(labels.getString("RtStatFrame.resume"));
        }
        else
        {
            pausedButton.setText(labels.getString("RtStatFrame.pause"));
        }
        this.controller.pause(paused);
    }

    public void setModel(LrgsConnectionComboBoxModel model)
    {
        this.hostCombo.setModel(model);
    }

    public LrgsConnection getCurrentConnection()
    {
        return (LrgsConnection)hostCombo.getSelectedItem();
    }

    public void onConnect(Function<LrgsConnection,Boolean> connectCallback)
    {
        this.controller.onConnect(connectCallback);
    }

    public void onPause(Consumer<Boolean> onPause)
    {
        this.controller.onPause(onPause);
    }

    private LrgsConnection connectionFromFields()
    {
        final String hostName = hostCombo.getEditor().getItem().toString().trim();
        final int port = Integer.parseInt(portField.getText());
        final String username = usernameField.getText();
        final String password = LrgsConnection.encryptPassword(passwordField.getText(), LrgsConnectionPanel.pwk);
        return new LrgsConnection(hostName, port, username, password, null);
    }

    public static class ConnectionRender extends JLabel implements ListCellRenderer<LrgsConnection>
    {

        @Override
        public Component getListCellRendererComponent(JList<? extends LrgsConnection> list, LrgsConnection value,
                int index, boolean isSelected, boolean cellHasFocus)
        {
            if (value == LrgsConnection.BLANK)
            {
                setText(" ");
            }
            else
            {
                setText(String.format("%s:%d (%s)", value.getHostName(), value.getPort(), value.getUsername()));
            }

            return this;
        }

    }
}
