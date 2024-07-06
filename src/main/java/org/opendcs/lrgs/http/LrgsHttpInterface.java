package org.opendcs.lrgs.http;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;

import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.servlet.ServletContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ilex.xml.XmlOutputStream;
import lrgs.archive.MsgArchive;
import lrgs.lrgsmain.LoadableLrgsInputInterface;
import lrgs.lrgsmain.LrgsInputException;
import lrgs.lrgsmain.LrgsInputInterface;
import lrgs.lrgsmain.LrgsMain;
import lrgs.lrgsmon.DetailReportGenerator;
import lrgs.statusxml.LrgsStatusSnapshotExt;

public class LrgsHttpInterface implements LoadableLrgsInputInterface
{
    private static Logger log = LoggerFactory.getLogger(LrgsHttpInterface.class);
    private static final String TYPE = "HTTP";

    private org.eclipse.jetty.server.Server server = null;
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
    public void setSlot(int slot)
    {
        this.slot = slot;
    }

    @Override
    public int getSlot()
    {
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
        server = new org.eclipse.jetty.server.Server();
		ServletContextHandler ctx = new ServletContextHandler(ServletContextHandler.SESSIONS);
		ctx.setContextPath("/");
		server.setHandler(ctx);
        ServletHolder serHol = ctx.addServlet(ServletContainer.class, "/*");
		serHol.setInitOrder(1);
		serHol.setInitParameter("jersey.config.server.provider.packages", "org.opendcs.lrgs.http");

        server.setAttribute("lrgs", this.lrgs);
        server.setAttribute("archive", this.archive);

        ServerConnector connector = new ServerConnector(server);
        connector.setPort(this.port);
        server.addConnector(connector);

        /*
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
            */
    }

    @Override
    public void shutdownLrgsInput()
    {
        try
        {
            server.stop();
        }
        catch (Exception ex)
        {
            log.atError()
               .setCause(ex)
               .log("Unable to stop Jetty Server");
        }
    }

    @Override
    public void enableLrgsInput(boolean enabled)
    {
        try
        {
            server.setAttribute("lrgs", this.lrgs);
            server.setAttribute("archive", this.archive);
            server.start();
        }
        catch (Exception ex)
        {
            log.atError()
               .setCause(ex)
               .log("Unable to stop Jetty Server");
        }
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
        if (server != null)
        {            
            server.setAttribute("archive", this.archive);
        }
    }

    @Override
    public void setLrgsMain(LrgsMain lrgsMain)
    {
        this.lrgs = lrgsMain;
        if (server != null)
        {
            server.setAttribute("lrgs", this.lrgs);
        }
    }
}
