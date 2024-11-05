package decodes.decwiz;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;

import decodes.gui.TopFrame;
import decodes.db.Database;

public class DecWizFrame extends TopFrame
{
    private JPanel contentPane;
    private BorderLayout borderLayout1 = new BorderLayout();
    private JPanel southPanel = new JPanel();
    private GridBagLayout gridBagLayout1 = new GridBagLayout();
    private JButton backButton = new JButton();
    private JButton nextButton = new JButton();
    private JButton quitButton = new JButton();
    private JPanel panelContainer = new JPanel();
    private BorderLayout borderLayout2 = new BorderLayout();
    private JPanel titlePanel = new JPanel();
    private JLabel titleLabel = new JLabel();
    private FlowLayout flowLayout1 = new FlowLayout();

    private DecWizPanel thePanels[] = new DecWizPanel[3];
    private int curPanel = -1;
    private boolean exitOnClose = true;

    public DecWizFrame()
    {
        try
        {
            setDefaultCloseOperation(EXIT_ON_CLOSE);
            jbInit();
        }
        catch (Exception exception)
        {
            exception.printStackTrace();
        }
    }

    /**
     * Component initialization.
     *
     * @throws java.lang.Exception
     */
    private void jbInit() throws Exception
    {
        contentPane = (JPanel) getContentPane();
        contentPane.setLayout(borderLayout1);
        setSize(new Dimension(548, 491));
        setPreferredSize(new Dimension(700, 700));
        setTitle("Decoding Wizard");
        southPanel.setLayout(gridBagLayout1);
        backButton.setPreferredSize(new Dimension(100, 27));
        backButton.setText("<< Back");
        backButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                backButton_actionPerformed(e);
            }
        });
        nextButton.setPreferredSize(new Dimension(100, 27));
        nextButton.setText("Next >>");
        nextButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                nextButton_actionPerformed(e);
            }
        });
        quitButton.setPreferredSize(new Dimension(100, 27));
        quitButton.setText("Quit");
        quitButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                quitButton_actionPerformed(e);
            }
        });
        panelContainer.setLayout(borderLayout2);
        panelContainer.setPreferredSize(new Dimension(560, 540));
        panelContainer.setBorder(BorderFactory.createLoweredBevelBorder());
        titleLabel.setFont(new java.awt.Font("Dialog", Font.BOLD, 15));
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        titleLabel.setText("The Panel Title");
        titlePanel.setLayout(flowLayout1);
        flowLayout1.setVgap(15);
        contentPane.add(southPanel, java.awt.BorderLayout.SOUTH);
        southPanel.add(backButton, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
            , GridBagConstraints.WEST, GridBagConstraints.NONE,
            new Insets(10, 10, 10, 10), 0, 0));
        southPanel.add(nextButton, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0
            , GridBagConstraints.WEST, GridBagConstraints.NONE,
            new Insets(10, 0, 10, 0), 0, 0));
        southPanel.add(quitButton, new GridBagConstraints(2, 0, 1, 1, 1.0, 0.0
            , GridBagConstraints.EAST, GridBagConstraints.NONE,
            new Insets(10, 0, 10, 10), 0, 0));
        contentPane.add(panelContainer, java.awt.BorderLayout.CENTER);
        contentPane.add(titlePanel, java.awt.BorderLayout.NORTH);
        titlePanel.add(titleLabel);
    }

    public void backButton_actionPerformed(ActionEvent e)
    {
        switchPanel(curPanel - 1);
    }

    public void nextButton_actionPerformed(ActionEvent e)
    {
        if (curPanel == thePanels.length - 1)
            switchPanel(0);
        else
            switchPanel(curPanel + 1);
    }

    public void quitButton_actionPerformed(ActionEvent e)
    {
        if (exitOnClose)    {
            Database db = Database.getDb();
            db.getDbIo().close();
            System.exit(0);
        }
        else
            dispose();
    }

    public void createPanels()
    {
        thePanels[0] = new FileIdPanel();
        thePanels[1] = new DecodePanel();
        thePanels[2] = new SavePanel();
        curPanel = -1;
    }

    public FileIdPanel getFileIdPanel()
    {
        return (FileIdPanel)thePanels[0];
    }

    public DecodePanel getDecodePanel()
    {
        return (DecodePanel)thePanels[1];
    }

    public SavePanel getSavePanel()
    {
        return (SavePanel)thePanels[2];
    }

    /**
      Called on Next or Previous buttons, switch from one panel to another.
      Call the current panel's deactivate button to see if it's ok to leave.
      Then switch and call the new panel's activate button.
      @param n the number of the panel to switch to
     */
    public void switchPanel(int n)
    {
        if (curPanel != -1)
        {
            JPanel jp = (JPanel) thePanels[curPanel];
            try
            {
                if (!((DecWizPanel) jp).deactivate())
                {
                    return;
                }
            }
            catch (Exception ex)
            {
                System.err.println("Exception in deactivate: " + ex);
                ex.printStackTrace();
            }
            panelContainer.setVisible(false);
            panelContainer.remove(jp);
        }
        curPanel = n;
        JPanel jp = (JPanel) thePanels[curPanel];
        try
        {
            ((DecWizPanel) jp).activate();
        }
        catch (Exception ex)
        {
            System.err.println("Exception in activate: " + ex);
            ex.printStackTrace();
        }
        panelContainer.add(jp);

        titleLabel.setText(thePanels[curPanel].getTitle());
        panelContainer.setVisible(true);
        backButton.setEnabled(curPanel > 0);
        nextButton.setText((curPanel == thePanels.length - 1) ? "Start Over" :
                           "Next >>");
    }
}
