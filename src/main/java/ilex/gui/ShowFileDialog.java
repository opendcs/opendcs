package ilex.gui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * This dialog shows the contents of a file in a scrolling window.
 */
public class ShowFileDialog
    extends JDialog
{
    private JPanel mainPanel = new JPanel();
    private JScrollPane scrollPane = new JScrollPane();
    private JTextArea fileArea = new JTextArea();
    private JPanel buttonPanel = new JPanel();
    private JButton closeButton = new JButton();

    /**
     * Constructor.
     * @param frame parent frame or null if none.
     * @param fileName name of file
     * @param modal true if this should be model.
     */
    public ShowFileDialog(Frame frame, String fileName, boolean modal)
    {
        super(frame, fileName, modal);
        jbInit();
        this.setSize(600,280);
        WindowUtility.center(this);
    }

    /**
     * GUI component initialization
     * @throws Exception
     */
    private void jbInit()
    {
        fileArea.setLineWrap(true);
        fileArea.setWrapStyleWord(true);
        fileArea.setEditable(false);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.
            HORIZONTAL_SCROLLBAR_NEVER);
        closeButton.setText("Close");
        closeButton.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                closeDlg();
            }
        });

        mainPanel.setLayout(new BorderLayout());
        getContentPane().add(mainPanel);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        buttonPanel.setLayout(new FlowLayout());
        buttonPanel.add(closeButton, null);
        scrollPane.getViewport().add(fileArea, null);
    }

    /** Closes the dialog. */
    void closeDlg()
    {
        setVisible(false);
    }
    
    public void setFile(File file)
    {
        try
        {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;
            while((line = br.readLine()) != null)
            {
                fileArea.append(line + "\n");
            }
            br.close();
        }
        catch(IOException ex)
        {
            fileArea.append("Cannot read '" + file.getPath() + "':\n" + ex);
        }
    }
}
