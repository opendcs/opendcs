package org.opendcs.fixtures.helpers;
/**
 * A simple application that echos command-line arguments to standard output.
 * also echos system properties.
 * used for testing java launching and argument passing.
 */
public class ArgumentEchoApp
{
    public static void main(String[] args)
    {
        System.out.println("=== Arguments ===");
        for (int i = 0; i < args.length; i++)
        {
            System.out.println("arg[" + i + "]=" + args[i]);
        }

        System.out.println("=== System Properties ===");
        for (String key : System.getProperties().stringPropertyNames())
        {
            System.out.println(key + "=" + System.getProperty(key));
        }
    }
}