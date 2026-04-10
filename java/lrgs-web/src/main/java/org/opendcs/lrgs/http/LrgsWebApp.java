package org.opendcs.lrgs.http;

import jakarta.servlet.ServletContext;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.Context;

@ApplicationPath("/")
public class LrgsWebApp extends Application
{
    @Context
    ServletContext servletContext;
    
}
