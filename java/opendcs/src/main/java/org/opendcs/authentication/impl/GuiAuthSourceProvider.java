package org.opendcs.authentication.impl;

import org.opendcs.spi.authentication.AuthSource;
import org.opendcs.spi.authentication.AuthSourceProvider;

import ilex.gui.LoginDialog;
import ilex.util.AuthException;

public class GuiAuthSourceProvider implements AuthSourceProvider
{

    @Override
    public String getType()
    {
        return "gui-auth-source";
    }

    @Override
    public AuthSource process(String configurationString) throws AuthException
    {
        AuthSource gau = new LoginDialog(null,configurationString);
        return gau;
    }
}
