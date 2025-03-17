package org.opendcs.jmx;

public final class JmxUtils
{
    private JmxUtils()
    {
        /* intentionally empty */
    }


    /**
     * Escape unsafe characters in a {@link javax.management.ObjectName} string.
     * Makes no attempt to determine if the name is already safe.
     * @param name Initial Name
     * @return Name with characters escaped
     * @see <a href="https://docs.oracle.com/javase/8/docs/api/javax/management/ObjectName.html">ObjectName (Java Platform SE 8)</a>
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
