package ilex.test;

import ilex.util.Logger;
import ilex.util.StderrLogger;

class LoggerTester
{
  /**
   * The main function.
   */

    public static void main(String[] args)
    {
        Logger.setLogger(new StderrLogger("LoggerTester"));

        Logger.instance().log(Logger.E_WARNING,
            "You shouldn't eat too much red meat");

    }
}