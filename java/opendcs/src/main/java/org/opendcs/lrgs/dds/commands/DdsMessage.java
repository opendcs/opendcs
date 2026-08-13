package org.opendcs.lrgs.dds.commands;

import lrgs.ldds.LddsMessage;

public enum DdsMessage {
    HELLO('a');

    private final char id;

    DdsMessage(char id)
    {
        this.id = id;
    }

    public char getId()
    {
        return id;
    }

    public DdsMessage from(LddsMessage msg)
    {
        return null;
    }
}
