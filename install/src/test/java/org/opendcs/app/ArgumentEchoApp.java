package org.opendcs.app;
/**
 * A simple application that echos command-line arguments to standard output.
 * also echos system properties.
 * used for testing java launching and argument passing.
 */
public class ArgumentEchoApp
{
    public static void main(String[] args)
    {
        System.out.println("props start");
        System.out.print("args:");
        System.out.println(String.join(",", args));
        for (String key : System.getProperties().stringPropertyNames())
        {
            System.out.println(key + ": " + System.getProperty(key));
        }
    }
}