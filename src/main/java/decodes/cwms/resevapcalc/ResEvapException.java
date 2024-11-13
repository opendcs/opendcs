/* 
 * Copyright (c) 2018
 * United States Army Corps of Engineers - Hydrologic Engineering Center (USACE/HEC)
 * All Rights Reserved.  USACE PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval from HEC
 */
package usace.rowcps.computation.resevap;

/**
 * ResEvapException is used to communicate errors occurring in the
 * use of the java ResEvap computations 
 *
 * @author  Richard Rachiele
 * @version 1.1 June  2015
 * @see HecMath
 */

public class ResEvapException extends java.lang.Exception
{
    /**
     * Constructs an <code>ResEvapException</code> with no detail  message.
     */
    public ResEvapException()
    {
        super();
    }

    /**
     * Constructs a new <code>ResEvapException</code> and copies the
     * message from <code>e</code>.<p>
     *
     * @param   e   a <code>java.lang.Exception</code>.
     */
    public ResEvapException(Exception e)
    {
        super(e.toString());
    }

    /**
     * Constructs an <code>ResEvapException</code> with the specified
     * detail message.<p>
     *
     * @param   message   the detail message.
     */
    public ResEvapException(String message)
    {
        super(message);
    }

}
