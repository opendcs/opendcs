/*
 * $Id$
 *
 * $State$
 *
 * $Log$
 * Revision 1.1  2008/04/04 18:21:09  cvs
 * Added legacy code to repository
 *
 * Revision 1.3  2002/08/26 17:47:32  chris
 * SQL Database I/O development
 *
 */

package ilex;

import java.io.PrintStream;
import java.util.Date;

/**
 * This class is designed to be the base class of a Java program
 * that tests some subset of a library's functionality.
 */

public class Tester
{
  /**
   * This is the OutputStream to which error messages are logged,
   * if there is one.  If not, this will be null.  The default for
   * this is System.err.
   */

    protected PrintStream _log;

  /**
   * Default constructor.
   */

    public Tester()
    {
        _log = System.err;
    }

  /**
   * Construct with a PrintStream to log error messages.  If you
   * want to disable the logging of error messages, construct with
   * log == null.
   */

    public Tester(PrintStream log)
    {
        _log = log;
    }

  /**
   * Call this when the test has failed, and there's no point in
   * continuing.  This prints out the error message, and then
   * exits with a non-zero exit status.
   */

    public void failed(String msg)
    {
        if (_log != null)
            _log.println("Failed:  " + msg);
        System.exit(1);
    }

  /**
   * Call this if the test failed because of catching an exception,
   * and you don't want to print any extra message with the
   * exception's message.
   */

    public void failedWithException(Exception e)
    {
        failedWithException(null, e);
    }

  /**
   * Call this if the test failed because of catching an exception.
   * This prints out the message that's passed in (if it's not
   * null), then prints out the exception's message, and then
   * prints the exception's stack trace.
   */

    public void failedWithException(String msg, Exception e)
    {
        if (_log != null) {
            String m = msg == null ? e.getMessage() :
                       (msg + "\n" + e.getMessage());

		    _log.println("Failed:  " + m);
    		e.printStackTrace(_log);
		}

		System.exit(1);
    }

  /**
   * Check that the argument passed is not null.
   */

    public void checkNotNull(String msg, Object obj)
    {
        if (obj == null) failed(msg + ":  should not be null");
    }

  /**
   * Use this to compare a boolean value with an expected.
   */

    public void compareBool(String msg, boolean expected, boolean got)
    {
        if (expected != got)
            failed(msg + ":  expected " + expected + ", got " + got);
    }

  /**
   * Use this to compare a single int value with an expected value.
   */

    public void compareInt(String msg, int expected, int got)
    {
        if (expected != got)
            failed(msg + ":  expected " + expected + ", got " + got);
    }

  /**
   * Use this to compare a single floating point value with an
   * expected value.
   */

    public void compareDouble(String msg, double expected, double got)
    {
        if (expected != got)
            failed(msg + ":  expected " + expected + ", got " + got);
    }
  /**
   * Use this to compare a single char value with an expected value.
   */

    public void compareChar(String msg, char expected, char got)
    {
        if (expected != got)
            failed(msg + ":  expected '" + expected + "', got '" + got + "'");
    }

  /**
   * Use this to compare a String with an expected value.  If both
   * expected and got are null, this considers them equal.
   */

    public void compareString(String msg, String expected, String got)
    {
        if (expected == null) {
            if (got == null) return;
            failed(msg + ":  expected null, got '" + got + "'");
        }

        if (!expected.equals(got))
            failed(msg + ":  expected '" + expected +
                   "', got '" + got + "'");
    }

  /**
   * Use this to compare a String with an expected value, ignoring case.
   */

    public void compareStringIgnoreCase(String msg, String expected,
                                        String got)
    {
        if (!expected.equalsIgnoreCase(got))
            failed(msg + ":  expected '" + expected +
                   "', got '" + got + "'");

    }

  /**
   * Compare a Date with an expected value.
   */

    public void compareDate(String msg, Date expected, Date got)
    {
        if (expected == null) {
            if (got == null) return;
            failed(msg + ":  expected null, got '" + got + "'");
        }

        if (!expected.equals(got))
            failed(msg + ":  expected " + expected +
                   ", got " + got);
    }
}

