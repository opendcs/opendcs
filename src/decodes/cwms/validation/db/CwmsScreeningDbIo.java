/*@lineinfo:filename=CwmsScreeningDbIo*//*@lineinfo:user-code*//*@lineinfo:1^1*/package decodes.cwms.validation.db;

import java.sql.SQLException;
import sqlj.runtime.ref.DefaultContext;
import sqlj.runtime.ConnectionContext;
import java.sql.Connection;

public class CwmsScreeningDbIo
{

  /* connection management */
  protected Connection __onn = null;
  protected javax.sql.DataSource __dataSource = null;
  public void setDataSource(javax.sql.DataSource dataSource) throws SQLException
  { release(); __dataSource = dataSource; }
  public void setDataSourceLocation(String dataSourceLocation) throws SQLException {
    javax.sql.DataSource dataSource;
    try {
      Class cls = Class.forName("javax.naming.InitialContext");
      Object ctx = cls.newInstance();
      java.lang.reflect.Method meth = cls.getMethod("lookup", new Class[]{String.class});
      dataSource = (javax.sql.DataSource) meth.invoke(ctx, new Object[]{"java:comp/env/" + dataSourceLocation});
      setDataSource(dataSource);
    } catch (Exception e) {
      throw new java.sql.SQLException("Error initializing DataSource at " + dataSourceLocation + ": " + e.getMessage());
    }
  }
  public Connection getConnection() throws SQLException
  { 
    if (__onn!=null) return __onn;
     else if (__tx!=null) return __tx.getConnection(); 
     else if (__dataSource!=null) __onn= __dataSource.getConnection(); 
     return __onn; 
   } 
  public void release() throws SQLException
  { if (__tx!=null && __onn!=null) __tx.close(ConnectionContext.KEEP_CONNECTION);
    __onn = null; __tx = null;
    __dataSource = null;
  }

  public void closeConnection(){
    if (__dataSource!=null) {
      try { if (__onn!=null) { __onn.close(); } } catch (java.sql.SQLException e) {}
      try { if (__tx!=null) {__tx.close(); } } catch (java.sql.SQLException e) {}
      __onn=null;
      __tx=null;
    }
  }
  protected DefaultContext __tx = null;
  public void setConnectionContext(DefaultContext ctx) throws SQLException
  { release(); __tx = ctx; }
  public DefaultContext getConnectionContext() throws SQLException
  { if (__tx==null)
    { __tx = (getConnection()==null) ? DefaultContext.getDefaultContext() : new DefaultContext(getConnection()); }
    return __tx;
  };

  /* constructors */
  public CwmsScreeningDbIo() throws SQLException
  { __tx = DefaultContext.getDefaultContext();
 }
  public CwmsScreeningDbIo(DefaultContext c) throws SQLException
  { __tx = c; }
  public CwmsScreeningDbIo(Connection c) throws SQLException
  {__onn = c; __tx = new DefaultContext(c);  }
  public CwmsScreeningDbIo(javax.sql.DataSource ds) throws SQLException { __dataSource = ds; }

  public void updateScreeningIdDesc (
    oracle.sql.CHAR P_SCREENING_ID,
    oracle.sql.CHAR P_SCREENING_ID_DESC,
    oracle.sql.CHAR P_DB_OFFICE_ID)
  throws java.sql.SQLException
  {
 try {
    /*@lineinfo:generated-code*//*@lineinfo:75^5*/

//  ************************************************************
//  #sql [getConnectionContext()] { CALL CWMS_20.CWMS_VT.UPDATE_SCREENING_ID_DESC(
//        :P_SCREENING_ID,
//        :P_SCREENING_ID_DESC,
//        :P_DB_OFFICE_ID)  };
//  ************************************************************

{
  // declare temps
  oracle.jdbc.OraclePreparedStatement __sJT_st = null;
  sqlj.runtime.ref.DefaultContext __sJT_cc = getConnectionContext(); if (__sJT_cc==null) sqlj.runtime.error.RuntimeRefErrors.raise_NULL_CONN_CTX();
  sqlj.runtime.ExecutionContext.OracleContext __sJT_ec = ((__sJT_cc.getExecutionContext()==null) ? sqlj.runtime.ExecutionContext.raiseNullExecCtx() : __sJT_cc.getExecutionContext().getOracleContext());
  try {
   String theSqlTS = "BEGIN CWMS_20.CWMS_VT.UPDATE_SCREENING_ID_DESC(\n       :1 ,\n       :2 ,\n       :3 ) \n; END;";
   __sJT_st = __sJT_ec.prepareOracleStatement(__sJT_cc,"0decodes.cwms.validation.db.CwmsScreeningDbIo",theSqlTS);
   // set IN parameters
   __sJT_st.setCHAR(1,P_SCREENING_ID);
   __sJT_st.setCHAR(2,P_SCREENING_ID_DESC);
   __sJT_st.setCHAR(3,P_DB_OFFICE_ID);
  // execute statement
   __sJT_ec.oracleExecuteUpdate();
  } finally { __sJT_ec.oracleClose(); }
}


//  ************************************************************

/*@lineinfo:user-code*//*@lineinfo:78^24*/
 } catch(java.sql.SQLException _err) {
   try {
      getConnectionContext().getExecutionContext().close();
      closeConnection();
      if (__dataSource==null) throw _err;
    /*@lineinfo:generated-code*//*@lineinfo:84^5*/

//  ************************************************************
//  #sql [getConnectionContext()] { CALL CWMS_20.CWMS_VT.UPDATE_SCREENING_ID_DESC(
//        :P_SCREENING_ID,
//        :P_SCREENING_ID_DESC,
//        :P_DB_OFFICE_ID)  };
//  ************************************************************

{
  // declare temps
  oracle.jdbc.OraclePreparedStatement __sJT_st = null;
  sqlj.runtime.ref.DefaultContext __sJT_cc = getConnectionContext(); if (__sJT_cc==null) sqlj.runtime.error.RuntimeRefErrors.raise_NULL_CONN_CTX();
  sqlj.runtime.ExecutionContext.OracleContext __sJT_ec = ((__sJT_cc.getExecutionContext()==null) ? sqlj.runtime.ExecutionContext.raiseNullExecCtx() : __sJT_cc.getExecutionContext().getOracleContext());
  try {
   String theSqlTS = "BEGIN CWMS_20.CWMS_VT.UPDATE_SCREENING_ID_DESC(\n       :1 ,\n       :2 ,\n       :3 ) \n; END;";
   __sJT_st = __sJT_ec.prepareOracleStatement(__sJT_cc,"1decodes.cwms.validation.db.CwmsScreeningDbIo",theSqlTS);
   // set IN parameters
   __sJT_st.setCHAR(1,P_SCREENING_ID);
   __sJT_st.setCHAR(2,P_SCREENING_ID_DESC);
   __sJT_st.setCHAR(3,P_DB_OFFICE_ID);
  // execute statement
   __sJT_ec.oracleExecuteUpdate();
  } finally { __sJT_ec.oracleClose(); }
}


//  ************************************************************

/*@lineinfo:user-code*//*@lineinfo:87^24*/
   } catch (java.sql.SQLException _err2) { 
     try { getConnectionContext().getExecutionContext().close(); } catch (java.sql.SQLException _sqle) {}
     throw _err; 
  }
 }
  }

  public void assignScreeningId (
    oracle.sql.CHAR P_SCREENING_ID,
    ScreenAssignArray P_SCR_ASSIGN_ARRAY,
    oracle.sql.CHAR P_DB_OFFICE_ID)
  throws java.sql.SQLException
  {
 try {
    /*@lineinfo:generated-code*//*@lineinfo:102^5*/

//  ************************************************************
//  #sql [getConnectionContext()] { CALL CWMS_20.CWMS_VT.ASSIGN_SCREENING_ID(
//        :P_SCREENING_ID,
//        :P_SCR_ASSIGN_ARRAY,
//        :P_DB_OFFICE_ID)  };
//  ************************************************************

{
  // declare temps
  oracle.jdbc.OraclePreparedStatement __sJT_st = null;
  sqlj.runtime.ref.DefaultContext __sJT_cc = getConnectionContext(); if (__sJT_cc==null) sqlj.runtime.error.RuntimeRefErrors.raise_NULL_CONN_CTX();
  sqlj.runtime.ExecutionContext.OracleContext __sJT_ec = ((__sJT_cc.getExecutionContext()==null) ? sqlj.runtime.ExecutionContext.raiseNullExecCtx() : __sJT_cc.getExecutionContext().getOracleContext());
  try {
   String theSqlTS = "BEGIN CWMS_20.CWMS_VT.ASSIGN_SCREENING_ID(\n       :1 ,\n       :2 ,\n       :3 ) \n; END;";
   __sJT_st = __sJT_ec.prepareOracleStatement(__sJT_cc,"2decodes.cwms.validation.db.CwmsScreeningDbIo",theSqlTS);
   // set IN parameters
   __sJT_st.setCHAR(1,P_SCREENING_ID);
   if (P_SCR_ASSIGN_ARRAY==null) __sJT_st.setNull(2,2003,"CWMS_20.SCREEN_ASSIGN_ARRAY"); else __sJT_st.setORAData(2,P_SCR_ASSIGN_ARRAY);
   __sJT_st.setCHAR(3,P_DB_OFFICE_ID);
  // execute statement
   __sJT_ec.oracleExecuteUpdate();
  } finally { __sJT_ec.oracleClose(); }
}


//  ************************************************************

/*@lineinfo:user-code*//*@lineinfo:105^24*/
 } catch(java.sql.SQLException _err) {
   try {
      getConnectionContext().getExecutionContext().close();
      closeConnection();
      if (__dataSource==null) throw _err;
    /*@lineinfo:generated-code*//*@lineinfo:111^5*/

//  ************************************************************
//  #sql [getConnectionContext()] { CALL CWMS_20.CWMS_VT.ASSIGN_SCREENING_ID(
//        :P_SCREENING_ID,
//        :P_SCR_ASSIGN_ARRAY,
//        :P_DB_OFFICE_ID)  };
//  ************************************************************

{
  // declare temps
  oracle.jdbc.OraclePreparedStatement __sJT_st = null;
  sqlj.runtime.ref.DefaultContext __sJT_cc = getConnectionContext(); if (__sJT_cc==null) sqlj.runtime.error.RuntimeRefErrors.raise_NULL_CONN_CTX();
  sqlj.runtime.ExecutionContext.OracleContext __sJT_ec = ((__sJT_cc.getExecutionContext()==null) ? sqlj.runtime.ExecutionContext.raiseNullExecCtx() : __sJT_cc.getExecutionContext().getOracleContext());
  try {
   String theSqlTS = "BEGIN CWMS_20.CWMS_VT.ASSIGN_SCREENING_ID(\n       :1 ,\n       :2 ,\n       :3 ) \n; END;";
   __sJT_st = __sJT_ec.prepareOracleStatement(__sJT_cc,"3decodes.cwms.validation.db.CwmsScreeningDbIo",theSqlTS);
   // set IN parameters
   __sJT_st.setCHAR(1,P_SCREENING_ID);
   if (P_SCR_ASSIGN_ARRAY==null) __sJT_st.setNull(2,2003,"CWMS_20.SCREEN_ASSIGN_ARRAY"); else __sJT_st.setORAData(2,P_SCR_ASSIGN_ARRAY);
   __sJT_st.setCHAR(3,P_DB_OFFICE_ID);
  // execute statement
   __sJT_ec.oracleExecuteUpdate();
  } finally { __sJT_ec.oracleClose(); }
}


//  ************************************************************

/*@lineinfo:user-code*//*@lineinfo:114^24*/
   } catch (java.sql.SQLException _err2) { 
     try { getConnectionContext().getExecutionContext().close(); } catch (java.sql.SQLException _sqle) {}
     throw _err; 
  }
 }
  }

  public void renameScreeningId (
    oracle.sql.CHAR P_SCREENING_ID_OLD,
    oracle.sql.CHAR P_SCREENING_ID_NEW,
    oracle.sql.CHAR P_DB_OFFICE_ID)
  throws java.sql.SQLException
  {
 try {
    /*@lineinfo:generated-code*//*@lineinfo:129^5*/

//  ************************************************************
//  #sql [getConnectionContext()] { CALL CWMS_20.CWMS_VT.RENAME_SCREENING_ID(
//        :P_SCREENING_ID_OLD,
//        :P_SCREENING_ID_NEW,
//        :P_DB_OFFICE_ID)  };
//  ************************************************************

{
  // declare temps
  oracle.jdbc.OraclePreparedStatement __sJT_st = null;
  sqlj.runtime.ref.DefaultContext __sJT_cc = getConnectionContext(); if (__sJT_cc==null) sqlj.runtime.error.RuntimeRefErrors.raise_NULL_CONN_CTX();
  sqlj.runtime.ExecutionContext.OracleContext __sJT_ec = ((__sJT_cc.getExecutionContext()==null) ? sqlj.runtime.ExecutionContext.raiseNullExecCtx() : __sJT_cc.getExecutionContext().getOracleContext());
  try {
   String theSqlTS = "BEGIN CWMS_20.CWMS_VT.RENAME_SCREENING_ID(\n       :1 ,\n       :2 ,\n       :3 ) \n; END;";
   __sJT_st = __sJT_ec.prepareOracleStatement(__sJT_cc,"4decodes.cwms.validation.db.CwmsScreeningDbIo",theSqlTS);
   // set IN parameters
   __sJT_st.setCHAR(1,P_SCREENING_ID_OLD);
   __sJT_st.setCHAR(2,P_SCREENING_ID_NEW);
   __sJT_st.setCHAR(3,P_DB_OFFICE_ID);
  // execute statement
   __sJT_ec.oracleExecuteUpdate();
  } finally { __sJT_ec.oracleClose(); }
}


//  ************************************************************

/*@lineinfo:user-code*//*@lineinfo:132^24*/
 } catch(java.sql.SQLException _err) {
   try {
      getConnectionContext().getExecutionContext().close();
      closeConnection();
      if (__dataSource==null) throw _err;
    /*@lineinfo:generated-code*//*@lineinfo:138^5*/

//  ************************************************************
//  #sql [getConnectionContext()] { CALL CWMS_20.CWMS_VT.RENAME_SCREENING_ID(
//        :P_SCREENING_ID_OLD,
//        :P_SCREENING_ID_NEW,
//        :P_DB_OFFICE_ID)  };
//  ************************************************************

{
  // declare temps
  oracle.jdbc.OraclePreparedStatement __sJT_st = null;
  sqlj.runtime.ref.DefaultContext __sJT_cc = getConnectionContext(); if (__sJT_cc==null) sqlj.runtime.error.RuntimeRefErrors.raise_NULL_CONN_CTX();
  sqlj.runtime.ExecutionContext.OracleContext __sJT_ec = ((__sJT_cc.getExecutionContext()==null) ? sqlj.runtime.ExecutionContext.raiseNullExecCtx() : __sJT_cc.getExecutionContext().getOracleContext());
  try {
   String theSqlTS = "BEGIN CWMS_20.CWMS_VT.RENAME_SCREENING_ID(\n       :1 ,\n       :2 ,\n       :3 ) \n; END;";
   __sJT_st = __sJT_ec.prepareOracleStatement(__sJT_cc,"5decodes.cwms.validation.db.CwmsScreeningDbIo",theSqlTS);
   // set IN parameters
   __sJT_st.setCHAR(1,P_SCREENING_ID_OLD);
   __sJT_st.setCHAR(2,P_SCREENING_ID_NEW);
   __sJT_st.setCHAR(3,P_DB_OFFICE_ID);
  // execute statement
   __sJT_ec.oracleExecuteUpdate();
  } finally { __sJT_ec.oracleClose(); }
}


//  ************************************************************

/*@lineinfo:user-code*//*@lineinfo:141^24*/
   } catch (java.sql.SQLException _err2) { 
     try { getConnectionContext().getExecutionContext().close(); } catch (java.sql.SQLException _sqle) {}
     throw _err; 
  }
 }
  }

  public void deleteScreeningId (
    oracle.sql.CHAR P_SCREENING_ID,
    oracle.sql.CHAR P_PARAMETER_ID,
    oracle.sql.CHAR P_PARAMETER_TYPE_ID,
    oracle.sql.CHAR P_DURATION_ID,
    oracle.sql.CHAR P_CASCADE,
    oracle.sql.CHAR P_DB_OFFICE_ID)
  throws java.sql.SQLException
  {
 try {
    /*@lineinfo:generated-code*//*@lineinfo:159^5*/

//  ************************************************************
//  #sql [getConnectionContext()] { CALL CWMS_20.CWMS_VT.DELETE_SCREENING_ID(
//        :P_SCREENING_ID,
//        :P_PARAMETER_ID,
//        :P_PARAMETER_TYPE_ID,
//        :P_DURATION_ID,
//        :P_CASCADE,
//        :P_DB_OFFICE_ID)  };
//  ************************************************************

{
  // declare temps
  oracle.jdbc.OraclePreparedStatement __sJT_st = null;
  sqlj.runtime.ref.DefaultContext __sJT_cc = getConnectionContext(); if (__sJT_cc==null) sqlj.runtime.error.RuntimeRefErrors.raise_NULL_CONN_CTX();
  sqlj.runtime.ExecutionContext.OracleContext __sJT_ec = ((__sJT_cc.getExecutionContext()==null) ? sqlj.runtime.ExecutionContext.raiseNullExecCtx() : __sJT_cc.getExecutionContext().getOracleContext());
  try {
   String theSqlTS = "BEGIN CWMS_20.CWMS_VT.DELETE_SCREENING_ID(\n       :1 ,\n       :2 ,\n       :3 ,\n       :4 ,\n       :5 ,\n       :6 ) \n; END;";
   __sJT_st = __sJT_ec.prepareOracleStatement(__sJT_cc,"6decodes.cwms.validation.db.CwmsScreeningDbIo",theSqlTS);
   // set IN parameters
   __sJT_st.setCHAR(1,P_SCREENING_ID);
   __sJT_st.setCHAR(2,P_PARAMETER_ID);
   __sJT_st.setCHAR(3,P_PARAMETER_TYPE_ID);
   __sJT_st.setCHAR(4,P_DURATION_ID);
   __sJT_st.setCHAR(5,P_CASCADE);
   __sJT_st.setCHAR(6,P_DB_OFFICE_ID);
  // execute statement
   __sJT_ec.oracleExecuteUpdate();
  } finally { __sJT_ec.oracleClose(); }
}


//  ************************************************************

/*@lineinfo:user-code*//*@lineinfo:165^24*/
 } catch(java.sql.SQLException _err) {
   try {
      getConnectionContext().getExecutionContext().close();
      closeConnection();
      if (__dataSource==null) throw _err;
    /*@lineinfo:generated-code*//*@lineinfo:171^5*/

//  ************************************************************
//  #sql [getConnectionContext()] { CALL CWMS_20.CWMS_VT.DELETE_SCREENING_ID(
//        :P_SCREENING_ID,
//        :P_PARAMETER_ID,
//        :P_PARAMETER_TYPE_ID,
//        :P_DURATION_ID,
//        :P_CASCADE,
//        :P_DB_OFFICE_ID)  };
//  ************************************************************

{
  // declare temps
  oracle.jdbc.OraclePreparedStatement __sJT_st = null;
  sqlj.runtime.ref.DefaultContext __sJT_cc = getConnectionContext(); if (__sJT_cc==null) sqlj.runtime.error.RuntimeRefErrors.raise_NULL_CONN_CTX();
  sqlj.runtime.ExecutionContext.OracleContext __sJT_ec = ((__sJT_cc.getExecutionContext()==null) ? sqlj.runtime.ExecutionContext.raiseNullExecCtx() : __sJT_cc.getExecutionContext().getOracleContext());
  try {
   String theSqlTS = "BEGIN CWMS_20.CWMS_VT.DELETE_SCREENING_ID(\n       :1 ,\n       :2 ,\n       :3 ,\n       :4 ,\n       :5 ,\n       :6 ) \n; END;";
   __sJT_st = __sJT_ec.prepareOracleStatement(__sJT_cc,"7decodes.cwms.validation.db.CwmsScreeningDbIo",theSqlTS);
   // set IN parameters
   __sJT_st.setCHAR(1,P_SCREENING_ID);
   __sJT_st.setCHAR(2,P_PARAMETER_ID);
   __sJT_st.setCHAR(3,P_PARAMETER_TYPE_ID);
   __sJT_st.setCHAR(4,P_DURATION_ID);
   __sJT_st.setCHAR(5,P_CASCADE);
   __sJT_st.setCHAR(6,P_DB_OFFICE_ID);
  // execute statement
   __sJT_ec.oracleExecuteUpdate();
  } finally { __sJT_ec.oracleClose(); }
}


//  ************************************************************

/*@lineinfo:user-code*//*@lineinfo:177^24*/
   } catch (java.sql.SQLException _err2) { 
     try { getConnectionContext().getExecutionContext().close(); } catch (java.sql.SQLException _sqle) {}
     throw _err; 
  }
 }
  }

  public void unassignScreeningId (
    oracle.sql.CHAR P_SCREENING_ID,
    CwmsTsIdArray P_CWMS_TS_ID_ARRAY,
    oracle.sql.CHAR P_UNASSIGN_ALL,
    oracle.sql.CHAR P_DB_OFFICE_ID)
  throws java.sql.SQLException
  {
 try {
    /*@lineinfo:generated-code*//*@lineinfo:193^5*/

//  ************************************************************
//  #sql [getConnectionContext()] { CALL CWMS_20.CWMS_VT.UNASSIGN_SCREENING_ID(
//        :P_SCREENING_ID,
//        :P_CWMS_TS_ID_ARRAY,
//        :P_UNASSIGN_ALL,
//        :P_DB_OFFICE_ID)  };
//  ************************************************************

{
  // declare temps
  oracle.jdbc.OraclePreparedStatement __sJT_st = null;
  sqlj.runtime.ref.DefaultContext __sJT_cc = getConnectionContext(); if (__sJT_cc==null) sqlj.runtime.error.RuntimeRefErrors.raise_NULL_CONN_CTX();
  sqlj.runtime.ExecutionContext.OracleContext __sJT_ec = ((__sJT_cc.getExecutionContext()==null) ? sqlj.runtime.ExecutionContext.raiseNullExecCtx() : __sJT_cc.getExecutionContext().getOracleContext());
  try {
   String theSqlTS = "BEGIN CWMS_20.CWMS_VT.UNASSIGN_SCREENING_ID(\n       :1 ,\n       :2 ,\n       :3 ,\n       :4 ) \n; END;";
   __sJT_st = __sJT_ec.prepareOracleStatement(__sJT_cc,"8decodes.cwms.validation.db.CwmsScreeningDbIo",theSqlTS);
   // set IN parameters
   __sJT_st.setCHAR(1,P_SCREENING_ID);
   if (P_CWMS_TS_ID_ARRAY==null) __sJT_st.setNull(2,2003,"CWMS_20.CWMS_TS_ID_ARRAY"); else __sJT_st.setORAData(2,P_CWMS_TS_ID_ARRAY);
   __sJT_st.setCHAR(3,P_UNASSIGN_ALL);
   __sJT_st.setCHAR(4,P_DB_OFFICE_ID);
  // execute statement
   __sJT_ec.oracleExecuteUpdate();
  } finally { __sJT_ec.oracleClose(); }
}


//  ************************************************************

/*@lineinfo:user-code*//*@lineinfo:197^24*/
 } catch(java.sql.SQLException _err) {
   try {
      getConnectionContext().getExecutionContext().close();
      closeConnection();
      if (__dataSource==null) throw _err;
    /*@lineinfo:generated-code*//*@lineinfo:203^5*/

//  ************************************************************
//  #sql [getConnectionContext()] { CALL CWMS_20.CWMS_VT.UNASSIGN_SCREENING_ID(
//        :P_SCREENING_ID,
//        :P_CWMS_TS_ID_ARRAY,
//        :P_UNASSIGN_ALL,
//        :P_DB_OFFICE_ID)  };
//  ************************************************************

{
  // declare temps
  oracle.jdbc.OraclePreparedStatement __sJT_st = null;
  sqlj.runtime.ref.DefaultContext __sJT_cc = getConnectionContext(); if (__sJT_cc==null) sqlj.runtime.error.RuntimeRefErrors.raise_NULL_CONN_CTX();
  sqlj.runtime.ExecutionContext.OracleContext __sJT_ec = ((__sJT_cc.getExecutionContext()==null) ? sqlj.runtime.ExecutionContext.raiseNullExecCtx() : __sJT_cc.getExecutionContext().getOracleContext());
  try {
   String theSqlTS = "BEGIN CWMS_20.CWMS_VT.UNASSIGN_SCREENING_ID(\n       :1 ,\n       :2 ,\n       :3 ,\n       :4 ) \n; END;";
   __sJT_st = __sJT_ec.prepareOracleStatement(__sJT_cc,"9decodes.cwms.validation.db.CwmsScreeningDbIo",theSqlTS);
   // set IN parameters
   __sJT_st.setCHAR(1,P_SCREENING_ID);
   if (P_CWMS_TS_ID_ARRAY==null) __sJT_st.setNull(2,2003,"CWMS_20.CWMS_TS_ID_ARRAY"); else __sJT_st.setORAData(2,P_CWMS_TS_ID_ARRAY);
   __sJT_st.setCHAR(3,P_UNASSIGN_ALL);
   __sJT_st.setCHAR(4,P_DB_OFFICE_ID);
  // execute statement
   __sJT_ec.oracleExecuteUpdate();
  } finally { __sJT_ec.oracleClose(); }
}


//  ************************************************************

/*@lineinfo:user-code*//*@lineinfo:207^24*/
   } catch (java.sql.SQLException _err2) { 
     try { getConnectionContext().getExecutionContext().close(); } catch (java.sql.SQLException _sqle) {}
     throw _err; 
  }
 }
  }

  public oracle.sql.NUMBER getScreeningCode (
    oracle.sql.CHAR P_SCREENING_ID,
    oracle.sql.NUMBER P_DB_OFFICE_CODE)
  throws java.sql.SQLException
  {
    oracle.sql.NUMBER __jPt_result=null;
 try {
    /*@lineinfo:generated-code*//*@lineinfo:222^5*/

//  ************************************************************
//  #sql [getConnectionContext()] __jPt_result = { VALUES(CWMS_20.CWMS_VT.GET_SCREENING_CODE(
//        :P_SCREENING_ID,
//        :P_DB_OFFICE_CODE))  };
//  ************************************************************

{
  // declare temps
  oracle.jdbc.OracleCallableStatement __sJT_st = null;
  sqlj.runtime.ref.DefaultContext __sJT_cc = getConnectionContext(); if (__sJT_cc==null) sqlj.runtime.error.RuntimeRefErrors.raise_NULL_CONN_CTX();
  sqlj.runtime.ExecutionContext.OracleContext __sJT_ec = ((__sJT_cc.getExecutionContext()==null) ? sqlj.runtime.ExecutionContext.raiseNullExecCtx() : __sJT_cc.getExecutionContext().getOracleContext());
  try {
   String theSqlTS = "BEGIN :1 := CWMS_20.CWMS_VT.GET_SCREENING_CODE(\n       :2 ,\n       :3 )  \n; END;";
   __sJT_st = __sJT_ec.prepareOracleCall(__sJT_cc,"10decodes.cwms.validation.db.CwmsScreeningDbIo",theSqlTS);
   if (__sJT_ec.isNew())
   {
      __sJT_st.registerOutParameter(1,oracle.jdbc.OracleTypes.NUMBER);
   }
   // set IN parameters
   __sJT_st.setCHAR(2,P_SCREENING_ID);
   __sJT_st.setNUMBER(3,P_DB_OFFICE_CODE);
  // execute statement
   __sJT_ec.oracleExecuteUpdate();
   // retrieve OUT parameters
   __jPt_result = (oracle.sql.NUMBER)__sJT_st.getNUMBER(1);
  } finally { __sJT_ec.oracleClose(); }
}


//  ************************************************************

/*@lineinfo:user-code*//*@lineinfo:224^27*/
 } catch(java.sql.SQLException _err) {
   try {
      getConnectionContext().getExecutionContext().close();
      closeConnection();
      if (__dataSource==null) throw _err;
    /*@lineinfo:generated-code*//*@lineinfo:230^5*/

//  ************************************************************
//  #sql [getConnectionContext()] __jPt_result = { VALUES(CWMS_20.CWMS_VT.GET_SCREENING_CODE(
//        :P_SCREENING_ID,
//        :P_DB_OFFICE_CODE))  };
//  ************************************************************

{
  // declare temps
  oracle.jdbc.OracleCallableStatement __sJT_st = null;
  sqlj.runtime.ref.DefaultContext __sJT_cc = getConnectionContext(); if (__sJT_cc==null) sqlj.runtime.error.RuntimeRefErrors.raise_NULL_CONN_CTX();
  sqlj.runtime.ExecutionContext.OracleContext __sJT_ec = ((__sJT_cc.getExecutionContext()==null) ? sqlj.runtime.ExecutionContext.raiseNullExecCtx() : __sJT_cc.getExecutionContext().getOracleContext());
  try {
   String theSqlTS = "BEGIN :1 := CWMS_20.CWMS_VT.GET_SCREENING_CODE(\n       :2 ,\n       :3 )  \n; END;";
   __sJT_st = __sJT_ec.prepareOracleCall(__sJT_cc,"11decodes.cwms.validation.db.CwmsScreeningDbIo",theSqlTS);
   if (__sJT_ec.isNew())
   {
      __sJT_st.registerOutParameter(1,oracle.jdbc.OracleTypes.NUMBER);
   }
   // set IN parameters
   __sJT_st.setCHAR(2,P_SCREENING_ID);
   __sJT_st.setNUMBER(3,P_DB_OFFICE_CODE);
  // execute statement
   __sJT_ec.oracleExecuteUpdate();
   // retrieve OUT parameters
   __jPt_result = (oracle.sql.NUMBER)__sJT_st.getNUMBER(1);
  } finally { __sJT_ec.oracleClose(); }
}


//  ************************************************************

/*@lineinfo:user-code*//*@lineinfo:232^27*/
   } catch (java.sql.SQLException _err2) { 
     try { getConnectionContext().getExecutionContext().close(); } catch (java.sql.SQLException _sqle) {}
     throw _err; 
  }
 }
    return __jPt_result;
  }

  public void storeScreeningCriteria (
    oracle.sql.CHAR P_SCREENING_ID,
    oracle.sql.CHAR P_UNIT_ID,
    ScreenCritArray P_SCREEN_CRIT_ARRAY,
    oracle.sql.CHAR P_RATE_CHANGE_DISP_INTERVAL_ID,
    ScreenControlType P_SCREENING_CONTROL,
    oracle.sql.CHAR P_STORE_RULE,
    oracle.sql.CHAR P_IGNORE_NULLS,
    oracle.sql.CHAR P_DB_OFFICE_ID)
  throws java.sql.SQLException
  {
 try {
    /*@lineinfo:generated-code*//*@lineinfo:253^5*/

//  ************************************************************
//  #sql [getConnectionContext()] { CALL CWMS_20.CWMS_VT.STORE_SCREENING_CRITERIA(
//        :P_SCREENING_ID,
//        :P_UNIT_ID,
//        :P_SCREEN_CRIT_ARRAY,
//        :P_RATE_CHANGE_DISP_INTERVAL_ID,
//        :P_SCREENING_CONTROL,
//        :P_STORE_RULE,
//        :P_IGNORE_NULLS,
//        :P_DB_OFFICE_ID)  };
//  ************************************************************

{
  // declare temps
  oracle.jdbc.OraclePreparedStatement __sJT_st = null;
  sqlj.runtime.ref.DefaultContext __sJT_cc = getConnectionContext(); if (__sJT_cc==null) sqlj.runtime.error.RuntimeRefErrors.raise_NULL_CONN_CTX();
  sqlj.runtime.ExecutionContext.OracleContext __sJT_ec = ((__sJT_cc.getExecutionContext()==null) ? sqlj.runtime.ExecutionContext.raiseNullExecCtx() : __sJT_cc.getExecutionContext().getOracleContext());
  try {
   String theSqlTS = "BEGIN CWMS_20.CWMS_VT.STORE_SCREENING_CRITERIA(\n       :1 ,\n       :2 ,\n       :3 ,\n       :4 ,\n       :5 ,\n       :6 ,\n       :7 ,\n       :8 ) \n; END;";
   __sJT_st = __sJT_ec.prepareOracleStatement(__sJT_cc,"12decodes.cwms.validation.db.CwmsScreeningDbIo",theSqlTS);
   // set IN parameters
   __sJT_st.setCHAR(1,P_SCREENING_ID);
   __sJT_st.setCHAR(2,P_UNIT_ID);
   if (P_SCREEN_CRIT_ARRAY==null) __sJT_st.setNull(3,2003,"CWMS_20.SCREEN_CRIT_ARRAY"); else __sJT_st.setORAData(3,P_SCREEN_CRIT_ARRAY);
   __sJT_st.setCHAR(4,P_RATE_CHANGE_DISP_INTERVAL_ID);
   if (P_SCREENING_CONTROL==null) __sJT_st.setNull(5,2002,"CWMS_20.SCREENING_CONTROL_T"); else __sJT_st.setORAData(5,P_SCREENING_CONTROL);
   __sJT_st.setCHAR(6,P_STORE_RULE);
   __sJT_st.setCHAR(7,P_IGNORE_NULLS);
   __sJT_st.setCHAR(8,P_DB_OFFICE_ID);
  // execute statement
   __sJT_ec.oracleExecuteUpdate();
  } finally { __sJT_ec.oracleClose(); }
}


//  ************************************************************

/*@lineinfo:user-code*//*@lineinfo:261^24*/
 } catch(java.sql.SQLException _err) {
   try {
      getConnectionContext().getExecutionContext().close();
      closeConnection();
      if (__dataSource==null) throw _err;
    /*@lineinfo:generated-code*//*@lineinfo:267^5*/

//  ************************************************************
//  #sql [getConnectionContext()] { CALL CWMS_20.CWMS_VT.STORE_SCREENING_CRITERIA(
//        :P_SCREENING_ID,
//        :P_UNIT_ID,
//        :P_SCREEN_CRIT_ARRAY,
//        :P_RATE_CHANGE_DISP_INTERVAL_ID,
//        :P_SCREENING_CONTROL,
//        :P_STORE_RULE,
//        :P_IGNORE_NULLS,
//        :P_DB_OFFICE_ID)  };
//  ************************************************************

{
  // declare temps
  oracle.jdbc.OraclePreparedStatement __sJT_st = null;
  sqlj.runtime.ref.DefaultContext __sJT_cc = getConnectionContext(); if (__sJT_cc==null) sqlj.runtime.error.RuntimeRefErrors.raise_NULL_CONN_CTX();
  sqlj.runtime.ExecutionContext.OracleContext __sJT_ec = ((__sJT_cc.getExecutionContext()==null) ? sqlj.runtime.ExecutionContext.raiseNullExecCtx() : __sJT_cc.getExecutionContext().getOracleContext());
  try {
   String theSqlTS = "BEGIN CWMS_20.CWMS_VT.STORE_SCREENING_CRITERIA(\n       :1 ,\n       :2 ,\n       :3 ,\n       :4 ,\n       :5 ,\n       :6 ,\n       :7 ,\n       :8 ) \n; END;";
   __sJT_st = __sJT_ec.prepareOracleStatement(__sJT_cc,"13decodes.cwms.validation.db.CwmsScreeningDbIo",theSqlTS);
   // set IN parameters
   __sJT_st.setCHAR(1,P_SCREENING_ID);
   __sJT_st.setCHAR(2,P_UNIT_ID);
   if (P_SCREEN_CRIT_ARRAY==null) __sJT_st.setNull(3,2003,"CWMS_20.SCREEN_CRIT_ARRAY"); else __sJT_st.setORAData(3,P_SCREEN_CRIT_ARRAY);
   __sJT_st.setCHAR(4,P_RATE_CHANGE_DISP_INTERVAL_ID);
   if (P_SCREENING_CONTROL==null) __sJT_st.setNull(5,2002,"CWMS_20.SCREENING_CONTROL_T"); else __sJT_st.setORAData(5,P_SCREENING_CONTROL);
   __sJT_st.setCHAR(6,P_STORE_RULE);
   __sJT_st.setCHAR(7,P_IGNORE_NULLS);
   __sJT_st.setCHAR(8,P_DB_OFFICE_ID);
  // execute statement
   __sJT_ec.oracleExecuteUpdate();
  } finally { __sJT_ec.oracleClose(); }
}


//  ************************************************************

/*@lineinfo:user-code*//*@lineinfo:275^24*/
   } catch (java.sql.SQLException _err2) { 
     try { getConnectionContext().getExecutionContext().close(); } catch (java.sql.SQLException _sqle) {}
     throw _err; 
  }
 }
  }

  public void createScreeningId (
    oracle.sql.CHAR P_SCREENING_ID,
    oracle.sql.CHAR P_SCREENING_ID_DESC,
    oracle.sql.CHAR P_PARAMETER_ID,
    oracle.sql.CHAR P_PARAMETER_TYPE_ID,
    oracle.sql.CHAR P_DURATION_ID,
    oracle.sql.CHAR P_DB_OFFICE_ID)
  throws java.sql.SQLException
  {
 try {
    /*@lineinfo:generated-code*//*@lineinfo:293^5*/

//  ************************************************************
//  #sql [getConnectionContext()] { CALL CWMS_20.CWMS_VT.CREATE_SCREENING_ID(
//        :P_SCREENING_ID,
//        :P_SCREENING_ID_DESC,
//        :P_PARAMETER_ID,
//        :P_PARAMETER_TYPE_ID,
//        :P_DURATION_ID,
//        :P_DB_OFFICE_ID)  };
//  ************************************************************

{
  // declare temps
  oracle.jdbc.OraclePreparedStatement __sJT_st = null;
  sqlj.runtime.ref.DefaultContext __sJT_cc = getConnectionContext(); if (__sJT_cc==null) sqlj.runtime.error.RuntimeRefErrors.raise_NULL_CONN_CTX();
  sqlj.runtime.ExecutionContext.OracleContext __sJT_ec = ((__sJT_cc.getExecutionContext()==null) ? sqlj.runtime.ExecutionContext.raiseNullExecCtx() : __sJT_cc.getExecutionContext().getOracleContext());
  try {
   String theSqlTS = "BEGIN CWMS_20.CWMS_VT.CREATE_SCREENING_ID(\n       :1 ,\n       :2 ,\n       :3 ,\n       :4 ,\n       :5 ,\n       :6 ) \n; END;";
   __sJT_st = __sJT_ec.prepareOracleStatement(__sJT_cc,"14decodes.cwms.validation.db.CwmsScreeningDbIo",theSqlTS);
   // set IN parameters
   __sJT_st.setCHAR(1,P_SCREENING_ID);
   __sJT_st.setCHAR(2,P_SCREENING_ID_DESC);
   __sJT_st.setCHAR(3,P_PARAMETER_ID);
   __sJT_st.setCHAR(4,P_PARAMETER_TYPE_ID);
   __sJT_st.setCHAR(5,P_DURATION_ID);
   __sJT_st.setCHAR(6,P_DB_OFFICE_ID);
  // execute statement
   __sJT_ec.oracleExecuteUpdate();
  } finally { __sJT_ec.oracleClose(); }
}


//  ************************************************************

/*@lineinfo:user-code*//*@lineinfo:299^24*/
 } catch(java.sql.SQLException _err) {
   try {
      getConnectionContext().getExecutionContext().close();
      closeConnection();
      if (__dataSource==null) throw _err;
    /*@lineinfo:generated-code*//*@lineinfo:305^5*/

//  ************************************************************
//  #sql [getConnectionContext()] { CALL CWMS_20.CWMS_VT.CREATE_SCREENING_ID(
//        :P_SCREENING_ID,
//        :P_SCREENING_ID_DESC,
//        :P_PARAMETER_ID,
//        :P_PARAMETER_TYPE_ID,
//        :P_DURATION_ID,
//        :P_DB_OFFICE_ID)  };
//  ************************************************************

{
  // declare temps
  oracle.jdbc.OraclePreparedStatement __sJT_st = null;
  sqlj.runtime.ref.DefaultContext __sJT_cc = getConnectionContext(); if (__sJT_cc==null) sqlj.runtime.error.RuntimeRefErrors.raise_NULL_CONN_CTX();
  sqlj.runtime.ExecutionContext.OracleContext __sJT_ec = ((__sJT_cc.getExecutionContext()==null) ? sqlj.runtime.ExecutionContext.raiseNullExecCtx() : __sJT_cc.getExecutionContext().getOracleContext());
  try {
   String theSqlTS = "BEGIN CWMS_20.CWMS_VT.CREATE_SCREENING_ID(\n       :1 ,\n       :2 ,\n       :3 ,\n       :4 ,\n       :5 ,\n       :6 ) \n; END;";
   __sJT_st = __sJT_ec.prepareOracleStatement(__sJT_cc,"15decodes.cwms.validation.db.CwmsScreeningDbIo",theSqlTS);
   // set IN parameters
   __sJT_st.setCHAR(1,P_SCREENING_ID);
   __sJT_st.setCHAR(2,P_SCREENING_ID_DESC);
   __sJT_st.setCHAR(3,P_PARAMETER_ID);
   __sJT_st.setCHAR(4,P_PARAMETER_TYPE_ID);
   __sJT_st.setCHAR(5,P_DURATION_ID);
   __sJT_st.setCHAR(6,P_DB_OFFICE_ID);
  // execute statement
   __sJT_ec.oracleExecuteUpdate();
  } finally { __sJT_ec.oracleClose(); }
}


//  ************************************************************

/*@lineinfo:user-code*//*@lineinfo:311^24*/
   } catch (java.sql.SQLException _err2) { 
     try { getConnectionContext().getExecutionContext().close(); } catch (java.sql.SQLException _sqle) {}
     throw _err; 
  }
 }
  }
}/*@lineinfo:generated-code*/