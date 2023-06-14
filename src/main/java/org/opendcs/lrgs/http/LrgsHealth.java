package org.opendcs.lrgs.http;

import io.javalin.http.Context;
import io.javalin.http.HttpCode;

public class LrgsHealth {
    public static void get(Context ctx) {
        ctx.status(HttpCode.BAD_REQUEST);
    }
}
