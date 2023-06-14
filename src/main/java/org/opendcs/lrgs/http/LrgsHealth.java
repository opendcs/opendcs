package org.opendcs.lrgs.http;

import io.javalin.http.Context;
import io.javalin.http.HttpCode;
import lrgs.archive.MsgArchive;
import lrgs.lrgsmain.LrgsMain;

public class LrgsHealth
{
    /**
     * Simple LRGS status report for simple health checks.
     *
     * @param ctx Javalin Archive.
     * @param archive Lrgs MsgArchive to pull stats.
     * @param lrgs LrgsMain handle to get more information.
     */
    public static void get(Context ctx, MsgArchive archive, LrgsMain lrgs)
    {
        if (lrgs.getDdsServer().statusProvider.getStatusSnapshot().isUsable)
        {
            ctx.status(HttpCode.OK).json("Active");
        }
        else
        {
            ctx.status(HttpCode.SERVICE_UNAVAILABLE).json("Inactive");
        }
    }
}
