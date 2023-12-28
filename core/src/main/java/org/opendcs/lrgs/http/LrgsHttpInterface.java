package org.opendcs.lrgs.http;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import ilex.xml.XmlOutputStream;
import io.javalin.Javalin;
import io.javalin.http.ContentType;
import io.javalin.http.HttpCode;
import lrgs.archive.MsgArchive;
import lrgs.lrgsmain.LoadableLrgsInputInterface;
import lrgs.lrgsmain.LrgsInputException;
import lrgs.lrgsmain.LrgsInputInterface;
import lrgs.lrgsmain.LrgsMain;
import lrgs.lrgsmon.DetailReportGenerator;
import lrgs.statusxml.LrgsStatusSnapshotExt;

public class LrgsHttpInterface implements LoadableLrgsInputInterface
{
    private static final String TYPE = "HTTP";

    private Javalin app;
    private int slot;
    private int port = 7000;
    private String interfaceName;
    private MsgArchive archive;
    private LrgsMain lrgs;

    @Override
    public int getType()
    {
        return 2000;
    }

    @Override
    public void setSlot(int slot) {
        this.slot = slot;
    }

    @Override
    public int getSlot() {
        return slot;
    }

    @Override
    public String getInputName()
    {
        return TYPE+":"+port;
    }

    @Override
    public void initLrgsInput() throws LrgsInputException
    {
        app = Javalin.create()
            .get("/health", ctx -> LrgsHealth.get(ctx, archive, lrgs))
            .get("/status", ctx -> {
                LrgsStatusSnapshotExt status = lrgs.getStatusProvider().getStatusSnapshot();
                DetailReportGenerator gen = lrgs.getReportGenerator();
                try(ByteArrayOutputStream bos = new ByteArrayOutputStream();)
                {
                    XmlOutputStream xos = new XmlOutputStream(bos, "html");
                        gen.writeReport(xos, status.hostname, status, 10);
                        ctx.status(HttpCode.OK)
                           .result(bos.toByteArray())
                           .contentType(ContentType.TEXT_HTML);
                }
                catch (Exception ex)
                {
                    ctx.status(HttpCode.SERVICE_UNAVAILABLE).json("Cannot generate report.");
                }
            })
            ;
    }

    @Override
    public void shutdownLrgsInput()
    {
        app.stop();
    }

    @Override
    public void enableLrgsInput(boolean enabled)
    {
        app.start(port);
    }

    @Override
    public boolean hasBER()
    {
        return false;
    }

    @Override
    public String getBER()
    {
        throw new UnsupportedOperationException("Unimplemented method 'getBER'");
    }

    @Override
    public boolean hasSequenceNums() {
        return false;
    }

    @Override
    public int getStatusCode()
    {
        return LrgsInputInterface.DL_ACTIVE;
    }

    @Override
    public String getStatus()
    {
        return "Active";
    }

    @Override
    public int getDataSourceId()
    {
        return 2000;
    }

    @Override
    public boolean getsAPRMessages() {
        return false;
    }

    @Override
    public String getGroup()
    {
        return null;
    }

    @Override
    public void setInterfaceName(String name)
    {
        this.interfaceName = name;
    }

    @Override
    public void setConfigParam(String name, String value)
    {
        if(name.equalsIgnoreCase("port"))
        {
            this.port = Integer.valueOf(value);
        }
    }

    @Override
    public void setMsgArchive(MsgArchive archive)
    {
        this.archive = archive;
    }

    @Override
    public void setLrgsMain(LrgsMain lrgsMain)
    {
        this.lrgs = lrgsMain;
    }
}
