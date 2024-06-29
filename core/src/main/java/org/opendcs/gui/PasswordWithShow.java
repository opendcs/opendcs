package org.opendcs.gui;

import java.awt.FlowLayout;
import java.util.ResourceBundle;

import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JToggleButton;

import ilex.util.LoadResourceBundle;

/**
 * Component that provides a "SHOW/HIDE" toggle for password entry
 * so that users can visually verify their password or  property
 * entry.
 */
public class PasswordWithShow extends JPanel
{
    private static ResourceBundle resources = 
        LoadResourceBundle.getLabelDescriptions("ilex/resources/gui", null);
    final private JPasswordField passwordField;
    final private JToggleButton showPasswordButton;
    private boolean showing = false;
    final private char defaultEchoChar;
    

    /**
     * Create Component with default preferered width of the password field.
     * @param width
     */
    public PasswordWithShow(int width)
    {

        this.setLayout(new BoxLayout(this,BoxLayout.LINE_AXIS));
        passwordField = new JPasswordField(width);
        defaultEchoChar = passwordField.getEchoChar();
        showPasswordButton = new JToggleButton(resources.getString("Password.show"));
        showPasswordButton.setName("showToggle");
        showPasswordButton.addActionListener(e ->
        {
            showing = !showing;
            if (showing)
            {
                passwordField.setEchoChar((char)0);
                showPasswordButton.setText(resources.getString("Password.hide"));
            }
            else
            {
                passwordField.setEchoChar(defaultEchoChar);
                showPasswordButton.setText(resources.getString("Password.show"));
            }
        });
        this.add(passwordField);
        this.add(showPasswordButton);
    }

    /**
     * Set component name.
     * Will set subcomponents will be set with this prefix.
     */
    public void setName(String name)
    {
        super.setName(name);
        passwordField.setName(name + ".showablePassword");
        showPasswordButton.setName(name+".showToggle");
    }

    /**
     * Get the password. Passthrough to the embedded JPassword component.
     * @return
     */
    public char[] getPassword()
    {
        return passwordField.getPassword();
    }

    /**
     * Set the value of the password field.
     * @param text
     */
    public void setText(String text)
    {
        passwordField.setText(text);
    }

    /**
     * Return the password field contents
     * @return
     */
    public String getText()
    {
        return new String(passwordField.getPassword());
    }

    /**
     * Set the enable state of the password field and toggle button.
     * @param enabled
     */
    public void setEnabled(boolean enabled)
    {
        passwordField.setEnabled(enabled);
        showPasswordButton.setEnabled(enabled);
    }
}