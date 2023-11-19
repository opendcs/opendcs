package org.opendcs.utils;

import java.net.MalformedURLException;
import java.net.URL;

public class UrlUtils
{
    public static URL urlIfValid(String url)
    {
        try
        {
            return new URL(url);
        }
        catch (MalformedURLException e)
        {
            return null;
        }
    }

    public static boolean isValidUrl(String url)
    {
        return urlIfValid(url) != null;
    }
}
