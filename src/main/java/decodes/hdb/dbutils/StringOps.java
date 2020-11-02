package decodes.hdb.dbutils;


// import all necessary classes
import java.net.URL;
import java.net.URLConnection;
import java.io.*;
import java.net.*;

  /** Public class StringOps is the class that contains any methods that can be
     generally used to perform utility methods on String variables

  */

public class StringOps
{
  /* Public class StringOps is the class that contains any methods that can be
     generally used to perform utility methods on String variables

     Author:  Mark Bogner
     Date:    08-January-2001 

     Modified By     |   Date   |   Reason  

  */    

  /** Public method removeEnclosures removes the input String encl from the 
     beginning and end of the specified input String inString if the enclosures
     already exist.

   @param inString  The input string that will be used to remove the enclosures from
   @param encl The enclosure that the input string is enclosed by
   @return Returns a string that has the enclosures removed if they existed.
   */

  // method removeEnclosures (
  public static String removeEnclosures (String inString, String encl) 
  {
  /* Public method removeEnclosures removes the input String encl from the 
     beginning and end of the specified input String inString if the enclosures
     already exist.

     Author:  Mark Bogner
     Date:    08-January-2001 

     Modified By     |   Date   |   Reason  

  */    

   // if the enclosure is null or the empty string just return the input String
   if (encl == null | encl.equals("") ) return inString;
   if ( inString == null ) return inString;

   // if the input string doesn't have the enclosures then send it back the way 
   // it is
   if (!inString.startsWith(encl) || !inString.endsWith(encl)) return inString;

   // otherwise return the inString with the enclosures removed 
   return inString.substring(encl.length(),inString.length()-encl.length());

  }  // end of method removeEnclosures	

  /** Public method addEnclosures adds the input String encl to the beginning
     and end of the specified input String inString if the enclosures do not
     already exist.

   @param inString  The input string that will have enclosures added to it. 
   @param encl The enclosure that the input string should be enclosed by
   @return Returns a string that has the enclosures added if they didn't already exist.
  */

  // method addEnclosures (
  public static String addEnclosures (String inString, String encl) 
  {
  /* Public method addEnclosures adds the input String encl to the beginning
     and end of the specified input String inString if the enclosures do not
     already exist.

     Author:  Mark Bogner
     Date:    08-January-2001 

     Modified By     |   Date   |   Reason  

  */    

   //  if the enclosure is the empty string just return the input String
   if (encl == null | encl.equals("") ) return inString;
   if ( inString == null ) return inString;

   //  if the input string already has the enclosures then send if back the way 
   //  it is
   if (inString.startsWith(encl) && inString.endsWith(encl)) return inString;

   // otherwise return the inString with the enclosures around it
   return encl + inString + encl;

  }  // end of method addEnclosures	


  public String sendEmail(String _addr, String _from, String _subject, String _message)
      {

      try
      {

         // Establish a network connection for sending mail
         URL u = new URL("mailto:" + _addr);      // Create a mailto: URL
         URLConnection c = u.openConnection(); // Create a URLConnection for it
         c.setDoInput(false);                  // Specify no input from this URL
         c.setDoOutput(true);                  // Specify we'll do output
         c.connect();                          // Connect to mail host
         PrintWriter out =                     // Get output stream to mail host
           new PrintWriter(new OutputStreamWriter(c.getOutputStream()));

         // Write out mail headers.  Get user name of current process as from source.
         out.println("From: \"" + _from + "\" <" +
                     System.getProperty("user.name") + "@" +
                     InetAddress.getLocalHost().getHostName() + ">");
         out.println("To: " + _addr);
         out.println("Subject: " + _subject);
         out.println();  // blank line to end the list of headers

         // Now send the message.
         out.println(_message);

         // Close the stream to terminate the message
         out.flush();
         out.close();

         // Tell the caller it was successfully sent.
         return "Email Message sent to :"+ _addr;
      }
      catch (Exception e)
      {
         return "Error while sending admin email:" + e.getMessage();
      }

   }  // end of sendEmail method



}  // end of class StringOps
