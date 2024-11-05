package decodes.hdb.dbutils;

import java.sql.*;
import java.util.*;

public class Observation	 
{
    private String data_string;
    private String error_message;
    private Logger log = null;
    private DataObject do2 = null;
    private Connection conn = null;
    private boolean cont = true;
    private boolean debug = false;
    private boolean log_all = true;
    private boolean fatal_error = true;
    private boolean rbase_delete = false;
    private DBAccess db = null;

    public Observation(String str, Hashtable _hash, Connection _conn)  
    {
      data_string = str;
      log = Logger.getInstance();
      do2 = new DataObject(_hash);
      conn = _conn;
      db = new DBAccess(conn);
      if ((String)do2.get("error_table") == null) do2.put("error_table","NO");
      if ((String)do2.get("file_type") == null) do2.put("file_type","D");
      if ((String)do2.get("debug") != null && ((String)do2.get("debug")).equals("Y")) debug = true;
      if ((String)do2.get("log_all") != null && ((String)do2.get("log_all")).equals("N")) log_all = false;
      if ((String)do2.get("do_delete") != null && ((String)do2.get("do_delete")).equals("Y")) rbase_delete = true;
    }

    public void process()
    {


      fatal_error = true;
      cont = true;
      try 
      {

       String result = null;
       String query = null;
       String db_oper = " ";

       if (cont) this.parse(do2);       
       if (cont) this.get_sdi();
       if (cont) this.get_interval();
       if (cont) this.validation();
       if (cont && !rbase_delete)   //main continue check from here we can either update, insert or ignore duplicates
                   //  data operation decided by the procedure call
       {
          String proc = "{ call modify_r_base_raw(?,?,to_date(?,'YYYYMONDD HH24:MI')" + do2.get("start_time_offset") 
            + ",?,?,?,?,?,?,?,?,?,?)}";
          CallableStatement stmt = db.getConnection(do2).prepareCall(proc);
          // set all the called procedures input variables from the DataObject
          stmt.setLong(1,Long.parseLong(do2.get("site_datatype_id").toString()));
          stmt.setString(2,(String) do2.get("interval"));
          stmt.setString(3,(String) do2.get("start_date_time"));
          //stmt.setString(4,(String) do2.get("end_date_time"));  let procedure determine end_date_time
          stmt.setFloat(5,Float.parseFloat(do2.get("value").toString()));
          stmt.setLong(6,Long.parseLong(do2.get("agen_id").toString()));
          stmt.setString(7,(String) do2.get("overwrite_flag"));
          stmt.setString(8,(String) do2.get("validation"));
          stmt.setLong(9,Long.parseLong(do2.get("collection_system_id").toString()));
          stmt.setLong(10,Long.parseLong(do2.get("loading_application_id").toString()));
          stmt.setLong(11,Long.parseLong(do2.get("method_id").toString()));
          stmt.setLong(12,Long.parseLong(do2.get("computation_id").toString()));
          stmt.setString(13,"Y");

          stmt.registerOutParameter(4, java.sql.Types.DATE);

          // execute the stored procedure call
          stmt.execute();

          // we are done with the procedure call so close the statement
          stmt.close();
          fatal_error = false;

       }  // end of main continue check continue

       if (cont && rbase_delete)   //  this observation record is to be deleted from r_base if it exists
       {
          String del_sql = "delete from r_base where " +
                 " site_datatype_id =  " + do2.get("site_datatype_id").toString() +
                 " and interval = '" + (String) do2.get("interval") + "' " +
                 " and start_date_time =  to_date('" + do2.get("start_date_time").toString() + "','YYYYMONDD HH24:MI')" +
                 " and agen_id = " + do2.get("agen_id").toString() +
                 " and collection_system_id = " + do2.get("collection_system_id").toString() +
                 " and loading_application_id = " + do2.get("loading_application_id").toString() ; 
          String status = db.performDML(del_sql,do2);
          if (debug) log.debug(this,del_sql);
          if (debug) log.debug(this,status); 
       }

       if (!cont)
       {

           if (log_all) log.debug( this,"  " + data_string + "  :" + db_oper + error_message);
           if ((String)do2.get("error_table") != null && ((String)do2.get("error_table")).equals("Y") && fatal_error) this.error_insert();
       }

      }  // end of try

       catch (Exception e) {if (log_all) log.debug(this,data_string + e.getMessage());}

       finally  //close connection always
       {
         db.closeConnection();
       }

    }  // end of method process


    private void parse(DataObject dobj)
    {

        dobj.put("DATETIME",data_string.substring(0,15).trim());
        dobj.put("START_DATE_TIME",data_string.substring(0,15).trim());
        dobj.put("END_DATE_TIME",data_string.substring(0,15).trim());
        dobj.put("STATION",data_string.substring(16,24).trim());
        dobj.put("PCODE",data_string.substring(25,33).trim());
        dobj.put("VALIDATION",data_string.substring(34,35).trim());
        dobj.put("VALUE",data_string.substring(36).trim());
//        dobj.put("INTERVAL","instant");
        dobj.put("OVERWRITE_FLAG","");

    } // end of method parse 


    private void validation()  // method used to validate the validation code
    {
       String result = null;
       String query = null;
       // validate the validation code if it exists
       if (!((String)do2.get("VALIDATION")).equals("")) 
        {
           query = "select count(*) rec_count from hdb_validation where validation='"+do2.get("VALIDATION")+"'";
           result = db.performQuery(query,do2);
           if (result.startsWith("ERROR")) 
           {
             cont = false;
             error_message = "Validation Check RESULT FAILED" + result;
             if (debug) log.debug( this,"  " + query + "  :" + error_message);
           }

           if (((String)do2.get("rec_count")).equalsIgnoreCase("0")) 
           {
              cont = false;
              error_message = "Invalid Validation Flag: " + (String)do2.get("VALIDATION");
              if (debug) log.debug( this,"  " + data_string + "  :" + error_message);
           }
        }
    } // end of validation method

    private void get_sdi()  // method used to get the SDI
    {

       String query = null;
       String result = null;

       cont = true;
       // check to see if site_datatype_id is already set
       if ((String)do2.get("site_datatype_id") != null && ((String)do2.get("site_datatype_id")).length() != 0) return;

       // get the site_datatype_id
       query = "select site_datatype_id from ref_hm_site_pcode "
             + "where hm_filetype = '" + do2.get("file_type")+"' and hm_site_code='"+do2.get("station")
             +"' and hm_pcode='"
             +do2.get("pcode")+"'";
       result = db.performQuery(query,do2);
       if (result.startsWith("ERROR")) 
       {
         cont = false;
         error_message = "GET SDI DATABASE RESULT FAILED" + result;
         if (debug) log.debug( this,"  " + query + "  :" + error_message);
       }

       if (((String)do2.get("site_datatype_id")).length() == 0)
       {
         cont = false;
         error_message = "GET_SDI query FAILED" + " Invalid Station, PCODE combination";
         if (debug) log.debug( this,"  " + data_string + "  :" + error_message);
       }

       if (debug) log.debug( this,"  " + data_string + "  :" + " PASSED SDI CHECK");

    } // end of get_sdi method

    private void get_interval()
    {
       String query = null;
       String result = null;

       if ((String)do2.get("interval") == null || ((String)do2.get("interval")).length() == 0)
       {
          // get the  interval if the property file did not short cut it
          query = "select decode(interval ,'instant','instant','" + (String)do2.get("def_noninstant")
                  +"') interval from V_VALID_INTERVAL_DATATYPE where site_datatype_id = " 
                  + (String)do2.get("site_datatype_id");
          result = db.performQuery(query,do2);
          if (result.startsWith("ERROR")) 
          {
            cont = false;
            error_message = "get_interval RESULT FAILED" + result;
            if (debug) log.debug( this,"  " + query + "  :" + error_message);
          }


          if (((String)do2.get("interval")).length() == 0)
          {
             do2.put("INTERVAL","instant");  //default to instant if no data exists;
          }

       }  //  end of big if to get interval code get interval code

      // set the end time or start time offset to nothing if its instantaneous otherwise
      do2.put("start_time_offset"," ");
      do2.put("end_time_offset"," ");
      if (((String)do2.get("interval")).equals("hour")) do2.put("start_time_offset"," - 1/24");
      if (((String)do2.get("interval")).equals("day")) do2.put("end_time_offset"," + 1");

      if (debug) log.debug( this,"  " + data_string + "  :" + " PASSED Interval CHECK");

     }  // end of get_interval method


     private void error_insert()  // method used to  put data into an error table
     {
         String dml = null;
         String result = null;
         String temp_sdi = null;
         String temp_interval = null;

         if ((String)do2.get("SITE_DATATYPE_ID") == null || ((String)do2.get("SITE_DATATYPE_ID")).length() == 0) temp_sdi = "null";
         else temp_sdi = (String)do2.get("SITE_DATATYPE_ID") ; 

         if ((String)do2.get("interval") == null || ((String)do2.get("interval")).length() == 0) temp_interval = "null";
         else temp_interval = "'" + (String)do2.get("interval") + "'"; 

         dml = "INSERT INTO R_BASE_ERROR "
                + "(RECORD_ID,SITE_DATATYPE_ID,SITE_CODE,P_CODE,INTERVAL,START_DATE_TIME,END_DATE_TIME,VALUE,AGEN_ID,OVERWRITE_FLAG,DATE_TIME_LOADED,"
                + "VALIDATION,COLLECTION_SYSTEM_ID,LOADING_APPLICATION_ID,METHOD_ID,COMPUTATION_ID,ERROR_MESSAGE,PROCESS_DATE,"
                + "DATA_SOURCE_NAME) "
                + "VALUES ("
                + "r_base_error_sequence.nextval,"
                + temp_sdi + "," 
                + "'" + do2.get("STATION") + "','"+ do2.get("PCODE") +"',"
                + temp_interval + ","
             //   + do2.get("SITE_DATATYPE_ID") + ",'"+ do2.get("INTERVAL") +"',"
                + "to_date('" + do2.get("START_DATE_TIME")+"','YYYYMONDD HH24:MI'),"
                + "to_date('" + do2.get("END_DATE_TIME")+"','YYYYMONDD HH24:MI'),"
                + do2.get("VALUE") + "," + do2.get("AGEN_ID") + ",'" + do2.get("OVERWRITE_FLAG") + "'," 
                + "sysdate," + "'" + do2.get("VALIDATION") + "'"
                + "," + do2.get("COLLECTION_SYSTEM_ID") +  "," + do2.get("LOADING_APPLICATION_ID") 
                + "," + do2.get("METHOD_ID") + "," + do2.get("COMPUTATION_ID")
                + ",'" + error_message + "',sysdate,'" + do2.get("DATA_SOURCE_NAME") + "'"
                + ")";

          if (debug) log.debug(this,dml);
          result = db.performDML(dml,do2);
          if (result.startsWith("ERROR")) 
          {
             cont = false;
             error_message = " INSERT INTO ERROR TABLE FAILED" + result;
             log.debug( this,"  " + data_string + "  :" + error_message);
          }

     }  // end of error_insert method


}  // end of observation class
