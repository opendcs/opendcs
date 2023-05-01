// class Logger will be contained within the some package
package decodes.hdb.dbutils;

// do all the imports here
import java.io.*;
import java.util.*;
import java.text.*;

/** Public class Logger is a utility class designed to let multiple classes
    write  log messages to a specific file or to a user specified file. The
    debug method is synchronized so that there is no deadlock issue with
    multiple users attempting to write to the file at the same time.  The 
    Logger class writes out the user supplied classname, date and time 
    (currently format is hard coded) and the message

    @author Mark A. Bogner 

    Date: 18-April-2001

*/
public class Logger
{

  private static PrintWriter defOut = null;
  private static PrintWriter out    = null;
  private        String className = "";
  private        SimpleDateFormat  sdf = new SimpleDateFormat ("yyyy.MM.dd hh:mm:ss a z");
  private static Logger logger = null; 

/*
  static
  {
     try
     {
     defOut = new PrintWriter(new FileOutputStream("log_file.out",true));
     } 
     catch (FileNotFoundException e)
     {
     System.out.println("File not Found");
     }
  } // end of static initializer

  // empty constructor
  public Logger()
  {
   this(null);
  }
*/

 
/** This constructor is used to log the class that calls it inorder
    that the class name will appear in the log file

    @param obj The class that calls it -- usually one would initialize
    the logger like this Logger log = new Logger(this);
*/
 /*
  public Logger (Object obj)
  {
    if (obj != null) className = obj.getClass().getName();
    if (out == null) out = defOut;
  }
*/

 
/** This constructor is used to log the class that calls it inorder
    that the class name will appear in the user specified log file

    @param obj The class that calls it -- usually one would initialize
    the logger like this Logger log = new Logger(this);
    @param _file  The new file that the user wants the log messages to go to
*/

  private Logger(String _file)
  {
     try
     {
     // now redirect the output to a new supplied log file
     out = new PrintWriter(new FileOutputStream(_file,true));
     }
     catch (Exception e)
     {
     System.out.println(e.toString());
     }
  }

  public static Logger createInstance(String _file)
  {
    logger = new Logger(_file);
    return logger;
  }

  public static Logger getInstance()
  {
    if (logger == null) logger = new Logger("log_file.out");
    return logger;
  }


  /**  Public method debug is provided for the user to  write the input variable
       msg to the Logger classes output FileOutputStream.  This method will 
       write out the class name, the date and time and the supplied message.

       @author Mark A. Bogner
       @param msg  The message that is to be written to the log file
    
       Date: 18-April-2001
  */
  public synchronized void debug(String msg)
  {

     // get the current date and time and then format using the SimpleDateFormat
     Date currentTime_1 = new Date();
     String dateString = sdf.format(currentTime_1);

     // write the classname, the formated date and the message then flush the stream
     out.print(className + ":" + dateString + "-- " + msg + "\n");
     out.flush();

  } // end of method debug

  public synchronized void debug(Object _obj, String msg)
  {

     // get the current date and time and then format using the SimpleDateFormat
     Date currentTime_1 = new Date();
     String dateString = sdf.format(currentTime_1);

     // write the classname, the formated date and the message then flush the stream
     out.print(_obj.getClass().getName() + ":" + dateString + "-- " + msg + "\n");
     out.flush();

  } // end of method debug




   /** public method setDTFormat allows the user to redefine the loggers 
    date/time format if they prefer another

    @author Mark A. Bogner
    @param _fmt The supplied SimpleDateFormat the user prefers to use
    
    Date: 18-April-2001

   */
   public void  setDTFormat(String _fmt)
   {
      sdf = new SimpleDateFormat (_fmt);

   } // end of method setDTFormat

   /**  Public method finalize is provided for the user to close the optional
     file if wanted and to allow java to close the file if there is an abnormal
     termination


    
     Date: 18-April-2001
   */
   public void finalize()
   {
     // if the out file is not the default filename then close the out file
     if (out != null && defOut != out) out.close();
   }

} // end of class Logger
