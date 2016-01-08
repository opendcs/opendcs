//  The DataObject class will be contained in the some package
package decodes.hdb.dbutils;

// include all necessary imports here
import java.io.*;
import java.util.*;


/**  Public class DataObject is intended to hold in a hashtable hash 
     all the data that is pertinent to this instance of this class.
     This class is intented to be utilized to store any type of data
     that the user may need.  Simple key, value pairs, property files,
     and key, Object types can be stored in this object.

      @author  Mark A. Bogner
      @version 1.0
      Date:   03-April-2001

*/
public class DataObject  
{

    // instance object hash stores all the key object pairs for this class
    private Hashtable hash  = null;

/** DataObject Constructor that  takes no input objects

*/
    public DataObject()
    {
       hash  = new Hashtable();

    } // end of constructor


/** DataObject Constructor that takes a hashtable as an input
    and will be used as the basis of the new DataObject
    
    @author Mark A. Bogner
    @version 1.0
    @param _hash  The input Hashtable object that will be used to populat
     the new internal hashtable of this new object

    Date: 17-April-2001

*/
    public DataObject(Hashtable _hash)
    {
       hash  = new Hashtable(_hash);
    } // end of constructor  with an input hashtable


    /** Method put places into the hashtable an entry
        of input parameters _key and _value.  The _key is stored in the
        Hashtable hash in uppercase.  The value object is stored as is.

    @author  Mark A. Bogner
    @version 1.0
    @param _key  The input String variable _key is used as the key in the 
    hastable put function.  The _key input variable is used in uppercase so the
    user can specify this key in either upper or lower case.
    @param _value Input Parameter _value is the object that will be stored in
    the Hastable hash as is.  This object can be of any type.
    Date:   03-April-2001

    */
    public void put(String _key, Object _value) 
    {
    
      // put the uppercase of the _key String and the value into the has table
      hash.put(_key.toUpperCase(), _value);
    }  // end of put method


    /** Method get retrieves a particular hashtable entry based on the 
        input parameter _key.  The Hashtable hash entries all all stored
        in the Hashtable with the key in uppercase so the get method uses
        an uppercase of the input parameter.

    @author  Mark A. Bogner
    @version 1.0
    @param _key  The input variable _key is used as the key in the hastable get
    function.  The _key input variable is used in uppercase so the user can
    specify this key in either upper or lower case.
    @return Returns the Object stored in the class for the specified key
    Date:   03-April-2001

    */
    public Object get(String _key) 
    {
      // return the hashtable object that is stored for the uppercase value of
      // the key
      return hash.get(_key.toUpperCase());
    } // End of get method


    /** Method addPropertyFile adds properties from a file to the existing 
        
     @author  Mark A. Bogner
     @version 1.0
     Date:   03-April-2001
     @param  _propertyFile  The input string that is the file that will be
     loaded into a Properties object and then transfered into the Hashtable
     hash object 

    */
    public void addPropertyFile(String _propertyFile) throws Exception
    {
       //  use a Properties object, load the property file into it,
       //  then get the elements out and put it into the Hashtable hash. 
       Properties prop = new Properties();
       try
       {
         prop.load(new BufferedInputStream(new FileInputStream(_propertyFile)));
         // now add all the properties to the main hashtable hash
         for (Enumeration e = prop.propertyNames() ; e.hasMoreElements() ;) 
         {
               String key = (String) e.nextElement();
               hash.put(key.toUpperCase(),prop.getProperty(key));
         }

       }
       catch (Exception e)
       {
         System.out.println("Unable to open Property File to add properties");
         System.out.println(e.getMessage());
         e.printStackTrace();
       }
    }  // end of method addPropertyFile


    /** Method toString return the contents of the DataObjects hashtable hash by
        utilizing the toString method of a Hashtable object
        
     @author  Mark A. Bogner
     @version 1.0
     @return Returns the String representation of the whole classes hashtable hash
     Date:   03-April-2001

    */
     public String toString()
     {
       // Returns every key/object pair that is in the Hashtable hash by
       // simply using the toString method supplied by Hashtable object

       return hash.toString();
     } // end of method toString


    /** Method getTable returns the DataObjects hashtable hash 
        
     @author  Mark A. Bogner
     @version 1.0
     @return Returns the String representation of the whole classes hashtable hash
     Date:   16-April-2001

    */
     public Hashtable getTable()
     {

       // Returns the hashtable within this class
       return hash;
     }  // end of method getTable


} // end of class DataObject
