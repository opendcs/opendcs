package org.opendcs.lrgs.http;

import javax.servlet.ServletContext;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;

@ApplicationPath("/")
public class LrgsWebApp extends Application
{
    @Context
    ServletContext servletContext;
    
}
