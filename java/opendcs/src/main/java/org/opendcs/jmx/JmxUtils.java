package org.opendcs.jmx;

public final class JmxUtils
{
    private JmxUtils()
    {
        /* intentionally empty */
    }

    /**
     * Escape ObjectName safe characters. {@see https://docs.oracle.com/javase/8/docs/api/javax/management/ObjectName.html}
     * Makes no attempt to determine if the name is already safe.
     * @param name Initial Name
     * @return Name with characters escaped
     */
    public static String jmxSafeName(String name)
    {
        return name.replace("\\","\\\\")
                   .replace("?","\\?")
                   .replace("\"","\\\"")
                   .replace("\n", "\\n")
                ;
    }
}
