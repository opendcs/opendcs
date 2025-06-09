package lrgs.ldds;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import lrgs.common.ArchiveException;

public class CmdStartTls extends LddsCommand
{
    private final String data;

    public CmdStartTls(byte[] msgData)
    {
        this.data = new String(msgData, StandardCharsets.UTF_8);
    }

    @Override
    public String cmdType()
    {
        return "CmdStartTls";
    }

    @Override
    public int execute(LddsThread ldds) throws ArchiveException, IOException
    {
        ldds.startTls();
        return 0;
    }

    @Override
    public char getCommandCode()
    {
        return LddsMessage.IdStartTls;
    }

}
