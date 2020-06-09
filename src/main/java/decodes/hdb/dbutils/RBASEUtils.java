package decodes.hdb.dbutils;

import java.util.Date;
import java.sql.*;
import java.util.*;
import java.lang.Long;
import java.text.SimpleDateFormat;

public class RBASEUtils 
{
    private Logger log = null;
    private DataObject do2 = null;
    private Connection conn = null;
    private DBAccess db = null;
    private boolean debug = false;
    private String error_message = null;

    public RBASEUtils(DataObject _do, Connection _conn)  
    {
      log = Logger.getInstance();
      do2 = _do;
      conn = _conn;
      db = new DBAccess(conn);
      if ((String)do2.get("debug") != null && ((String)do2.get("debug")).equals("YES")) debug = true;
    }

    public boolean get_all_ids()
    {
      boolean end_result = true;
      boolean temp_result = true;
    
      temp_result = this.get_method_id();
      if (end_result) end_result = temp_result;
    
      temp_result = this.get_computation_id();
      if (end_result) end_result = temp_result;
    
      temp_result = this.get_loading_application_id();
      if (end_result) end_result = temp_result;
    
      temp_result = this.get_collection_system_id();
      if (end_result) end_result = temp_result;
    
      temp_result = this.get_agen_id();
      if (end_result) end_result = temp_result;

      return end_result;

    } // end of get_all_ids method


    public boolean get_method_id()
    {

       String result = null;
       String query = null;
       // get method_id if it doesn't exist
       if ((String)do2.get("method_id") == null || ((String)do2.get("method_id")).length() == 0) 
        {
           if ((String)do2.get("method_name") == null )
             query = "select method_id from hdb_method where method_name = 'unknown'";
           else
             query = "select method_id from hdb_method where method_name = '" + (String)do2.get("method_name") + "'";
 

           result = db.performQuery(query,do2);
           if (result.startsWith("ERROR")) 
           {
             error_message = "Method_id get RESULT FAILED" + result;
             if (debug) log.debug( this,"  " + query + "  :" + error_message);
             return false;
           }

        }

      return true;
    } // end of get_method_id method



    public boolean get_loading_application_id()
    {

       String result = null;
       String query = null;
       // get loading_application_id if it doesn't exist
       if ((String)do2.get("loading_application_id") == null || ((String)do2.get("loading_application_id")).length() == 0) 
        {
           if ((String)do2.get("loading_application_name") == null )
             query = "select loading_application_id from hdb_loading_application where loading_application_name = 'unknown'";
           else
             query = "select loading_application_id from hdb_loading_application where loading_application_name = '" + (String)do2.get("loading_application_name") + "'";
 

           result = db.performQuery(query,do2);
           if (result.startsWith("ERROR")) 
           {
             error_message = "Loading_application_id get RESULT FAILED" + result;
             if (debug) log.debug( this,"  " + query + "  :" + error_message);
             return false;
           }

        }

      return true;
    } // end of get_loading_application_id method



    public boolean get_computation_id()
    {

       String result = null;
       String query = null;
       // get computation_id if it doesn't exist
       if ((String)do2.get("computation_id") == null || ((String)do2.get("computation_id")).length() == 0) 
        {
           if ((String)do2.get("computation_name") == null )
             query = "select computation_id from hdb_computed_datatype where computation_name = 'unknown'";
           else
             query = "select computation_id from hdb_computed_datatype where computation_name = '" + (String)do2.get("computation_name") + "'";
 

           result = db.performQuery(query,do2);
           if (result.startsWith("ERROR")) 
           {
             error_message = "Computation_id get RESULT FAILED" + result;
             if (debug) log.debug( this,"  " + query + "  :" + error_message);
             return false;
           }
        }

      return true;
    } // end of get_computation_id method


    public boolean get_collection_system_id()
    {

       String result = null;
       String query = null;
       // get collection_system_id if it doesn't exist
       if ((String)do2.get("collection_system_id") == null || ((String)do2.get("collection_system_id")).length() == 0) 
        {
           if ((String)do2.get("collection_system_name") == null )
             query = "select collection_system_id from hdb_collection_system where collection_system_name = 'unknown'";
           else
             query = "select collection_system_id from hdb_collection_system where collection_system_name = '" + (String)do2.get("collection_system_name") + "'";
 

           result = db.performQuery(query,do2);
           if (result.startsWith("ERROR")) 
           {
             error_message = "Collection_system_id get RESULT FAILED" + result;
             if (debug) log.debug( this,"  " + query + "  :" + error_message);
             return false;
           }

        }

      return true;
    } // end of get_collection_system_id method



    public boolean get_agen_id()
    {

       String result = null;
       String query = null;
       // get agen_id if it doesn't exist
       if ((String)do2.get("agen_id") == null || ((String)do2.get("agen_id")).length() == 0) 
        {
           if ((String)do2.get("agen_name") == null )
             query = "select agen_id from hdb_agen where agen_name = 'unknown'";
           else
             query = "select agen_id from hdb_agen where agen_name = '" + (String)do2.get("agen_name") + "'";
 

           result = db.performQuery(query,do2);
           if (result.startsWith("ERROR")) 
           {
             error_message = "Agen_id get RESULT FAILED" + result;
             if (debug) log.debug( this,"  " + query + "  :" + error_message);
             return false;
           }

        }

      return true;
    } // end of get_agen_id method


    public boolean get_sdi()  // method used to get the SDI
    {

       String query = null;
       String result = null;

       // get the site_datatype_id
       query = "select site_datatype_id from ref_hm_site_pcode "
             + "where hm_filetype = '" + do2.get("file_type")+"' and hm_site_code='"+do2.get("station")
             +"' and hm_pcode='"
             +do2.get("pcode")+"'";
       result = db.performQuery(query,do2);
       if (result.startsWith("ERROR"))
       {
         error_message = "GET SDI DATABASE RESULT FAILED" + result;
         if (debug) log.debug( this,"  " + query + "  :" + error_message);
         return false;
       }

       if (((String)do2.get("site_datatype_id")).length() == 0)
       {
         error_message = "GET_SDI query FAILED" + " Invalid Station, PCODE combination";
         if (debug) log.debug( this,"  " + query + "  :" + error_message);
         return false;
       }

       if (debug) log.debug( this,"  " + query + "  :" + " PASSED SDI CHECK");

      return true;
    } // end of get_sdi method



    public String  get_role_password(String role_name)  // method used to get the 
    {

       String query = null;
       String result = null;
 
       conn = db.getConnection(do2);
       query = "select psswd role_password from role_psswd where upper(role) = '" + role_name + "'";
       result = db.performQuery(query,do2);

       if (debug) log.debug( this,"  " + query + "  :" + " PASSED ROLE PASSWORD GET");


      return (String)do2.get("role_password");
    } // end of get_role_password method



    public String  get_DSS_Date(int minutes)  // method used to get the 
    {

       String query = null;
       String result = null;
 
       conn = db.getConnection(do2);

       Integer t_int = new Integer(minutes);
       query = "select to_char(trunc(to_date('31-DEC-1899')) + "
             + t_int.toString()
             + "/(60*24),'YYYYMONDD HH24:MI') dss_date from dual";

       result = db.performQuery(query,do2);

       if (debug) log.debug( this,"  " + query + "  :" + " PASSED DSS_DATE GET");


      return (String)do2.get("dss_date");
    } // end of get_DSS_Date method


    public Integer  get_DMI_SDI(int modelId, String objectName, String dataName)  // method used to get the
    // observation date from dss that is in minutes since 1900 to a formatted date
    //  that the observation class can handle
    {

       String query = null;
       String result = null;

       conn = db.getConnection(do2);

       Integer t_int = new Integer(modelId);
       Integer ret_sdi = new Integer(-9999);

       query = "select  site_datatype_id site_datatype_id from ref_dmi_data_map where "
             + "model_id = " + t_int.toString()
             + " and object_name = '" + objectName + "'"
             + " and data_name = '" + dataName + "'"  ;


       result = db.performQuery(query,do2);

       if (debug) log.debug( this,"  " + query + "  :" + " PASSED DMI_SDI GET");

      if ((String)do2.get("site_datatype_id") != null && ((String)do2.get("site_datatype_id")).length() != 0) ret_sdi = new Integer ((String )do2.get("site_datatype_id")); 

       return ret_sdi;
 
    } // end of get_DMI_SDI method



    /** Public method getStandardDates performs the database procedure call
        that requests the date passed in to be standardized by the stored procedure

        @author Mark A. Bogner
        @param _sdi  The  SDI for the particular record
        @param _interval  The  SDI's interval for the particular record
        @param _sdt       The start_datetime for the record
        @param _edt       The end date time for the record
        @param _fmt     The string format of the dates
        
        Date: 28-November-2007
    */
    public void getStandardDates(Integer _sdi, String _interval,Date _sdt, Date _edt, String _fmt)
    {
      CallableStatement stmt = null;
        try
        {
            Timestamp temp_sql_date = new Timestamp(_sdt.getTime());

            // initialize the procedure call, set the parameters , etc..
            java.sql.Date sdt = new java.sql.Date(_sdt.getTime());
            java.sql.Date edt = new java.sql.Date(_edt.getTime());
            String proc = "{ call hdb_utilities.standardize_dates(?,?,?,?) }";
            stmt = conn.prepareCall(proc);
            stmt.setLong(1,Long.parseLong(_sdi.toString()));
            stmt.setString(2,_interval);
            stmt.setTimestamp(3,temp_sql_date);
            stmt.setTimestamp(4,temp_sql_date);

            stmt.registerOutParameter(3, java.sql.Types.TIMESTAMP);
            stmt.registerOutParameter(4, java.sql.Types.TIMESTAMP);


            // now execute the procedure call and go get the return timestamp values
            stmt.execute();
            Timestamp result1 = stmt.getTimestamp(3);
            Timestamp result2 = stmt.getTimestamp(4);

	    // now put the results into the DB object
            if ( _fmt == null) _fmt = "dd-MM-yyyy HH:mm";
            SimpleDateFormat sdf = new SimpleDateFormat(_fmt);
            do2.put("SD_SDT",sdf.format(result1));
            do2.put("SD_EDT",sdf.format(result2));

        }
        catch ( SQLException e )
        {
           log.debug(e.toString());
           //e.printStackTrace();
        }
        finally  //close callable statement always
        {
          try
          {
           if (stmt != null) stmt.close();
          }
          catch ( SQLException e )
          {
             log.debug(e.toString());
          }
        }

     } // end of getStandardDates method


    /** Public method merge_r_base_update performs the   update of the cp_historic_computations table
	when there is a partial calulation.  This method does a merge statement since we don't
	need multiple records in that table.  We only want a basic seed record that says this 
	record is a candidate for one final calculation in the future.

        @author Mark A. Bogner  27-DEC-2007
        @param _sdi  The  SDI for the particular record
        @param _interval  The  SDI's interval for the particular record
        @param _sdt       The start_datetime for the record
        @param _edt       The end date time for the record
        @param _fmt     The string format of the dates
        @param _mrid	The model_run_id of input record (0 for real data) 
        @param _fmt     The table selector of the record (either R_ or M_; model or real indicator) 
        
        Date: 17-December-2007
    */
    public void merge_cp_hist_calc(Integer _ldapp, Integer _sdi, String _interval,Date _sdt, Date _edt, String _fmt,
      Integer _mrid, String _tbl)
    {

       String query = null;
       String result = null;

       conn = db.getConnection(do2);
       if ( _fmt == null) _fmt = "dd-MM-yyyy HH:mm";
       SimpleDateFormat sdf = new SimpleDateFormat(_fmt);

	query = "merge into cp_historic_computations cp using ( select " +
		_ldapp + " ldapp," + _sdi + " sdi, '" + _interval + "' interval, " + 
		_mrid + " mrid, '" + _tbl + "' tbl, " +
                " to_date('" + sdf.format(_sdt) + "','dd-MM-yyyy HH24:mi') sdt, " +
                " to_date('" + sdf.format(_edt) + "','dd-MM-yyyy HH24:mi') edt " + " from dual ) nvr " +
		"on (cp.loading_application_id = nvr.ldapp and cp.site_datatype_id = nvr.sdi and " +
                " cp.interval = nvr.interval and cp.start_date_time = nvr.sdt and " +
		"cp.end_date_time = nvr.edt) when matched then update set ready_for_delete = null " +
		"when not matched then insert " + 
		"(loading_application_id,site_datatype_id,interval,start_date_time,end_date_time,table_selector," + 
		"model_run_id,date_time_loaded) " +
		"values (nvr.ldapp,nvr.sdi,nvr.interval,nvr.sdt,nvr.edt,nvr.tbl,nvr.mrid,sysdate)";

       result = db.performDML(query,do2);
       if (result.startsWith("ERROR"))
	{
           System.out.println("RBASEUtils:merge_cp_hist_calc: " + result);
	   System.out.println(query + "  " + _sdt + "  " + _edt);
	}

     } // end of  merge_r_base_update method



    public Integer get_external_data_sdi()

    /** Public method get_external_data_sdi performs the database query to get the Site_datatype_id (SDI)
        of the external data based on the entries in the data object passed into the constructor of this class
        method used to get the SDI from the external data source tables
        this procedure assumes the the data source, site, and parameter_code has already been established
        so all that is necessary is to go to the external mapping tables and get the sdi
        the data object entries needed to be populated are: DATA_SOURCE, SITE_CODE, and PARAMETER_CODE 
   
        @author Mark A. Bogner  Sutron Corporation  07-April-2011
        
        Date: 07-April-2011
    */
    {

       String query = null;
       String result = null;
       Integer ret_sdi = new Integer(-9999);

       // get the site_datatype_id
       query = "select edm.hdb_site_datatype_id site_datatype_id from hdb_ext_data_source eds , ref_ext_site_data_map edm "
             + "where eds.ext_data_source_name = '" + do2.get("data_source")
	     +"' and edm.primary_site_code='"+do2.get("site_code")
             +"' and edm.primary_data_code='" +do2.get("parameter_code")+"'"
	     +" and eds.ext_data_source_id = edm.ext_data_source_id"
             +" and edm.hdb_interval_name = '" + do2.get("sample_interval") + "'";
       result = db.performQuery(query,do2);
       if (result.startsWith("ERROR"))
       {
         error_message = "GET EXTERNAL SDI DATABASE RESULT FAILED" + result;
         if (debug) log.debug( this,"  " + query + "  :" + error_message);
         return ret_sdi;
       }


       if (debug) log.debug( this,"  " + query + "  :" + " PASSED EXTERNAL SDI GET");

      if ((String)do2.get("site_datatype_id") != null && ((String)do2.get("site_datatype_id")).length() != 0) ret_sdi = new Integer ((String )do2.get("site_datatype_id")); 

       return ret_sdi;

    } // end of get_external_sdi method



}  // end of RBASEUtils class


