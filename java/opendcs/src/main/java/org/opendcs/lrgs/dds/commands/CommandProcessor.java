package org.opendcs.lrgs.dds.commands;

import java.io.IOException;
import java.rmi.ServerError;

import org.opendcs.lrgs.dds.DdsSession;

import lrgs.ldds.LddsCommand;
import lrgs.ldds.LddsMessage;

public interface CommandProcessor<T extends LddsCommand>
{
    LddsMessage process(T cmd, DdsSession session) throws IOException;
}
