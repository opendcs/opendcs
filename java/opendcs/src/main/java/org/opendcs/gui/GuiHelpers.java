package org.opendcs.gui;

import org.slf4j.Logger;

public class GuiHelpers
{
    private GuiHelpers()
    {
    }

    public static void logGuiComponentInit(Logger log, Throwable cause)
    {
        log.atError().setCause(cause).log("Unable to initialize GUI elements.");
    }
}