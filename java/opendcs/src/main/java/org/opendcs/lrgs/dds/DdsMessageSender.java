package org.opendcs.lrgs.dds;

import java.io.IOException;

import lrgs.ldds.LddsMessage;

public interface DdsMessageSender
{
    /**
      Send a message to the client.
      @param msg the message to send
    */
    void send(LddsMessage msg) throws IOException;
}
