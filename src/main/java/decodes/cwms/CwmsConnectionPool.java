package decodes.cwms;

import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.JMException;
import javax.management.ObjectName;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.TabularData;
import javax.management.openmbean.TabularDataSupport;

import decodes.db.Constants;
import decodes.sql.DbKey;
import decodes.tsdb.BadConnectException;
import decodes.tsdb.DbIoException;
import ilex.util.StringPair;
import ilex.util.TextUtil;
import opendcs.org.opendcs.jmx.ConnectionPoolMXBean;
import opendcs.org.opendcs.jmx.connections.JMXTypes;
import opendcs.util.sql.WrappedConnection;
import usace.cwms.db.dao.ifc.sec.CwmsDbSec;
import usace.cwms.db.dao.util.connection.ConnectionLoginInfo;
import usace.cwms.db.dao.util.connection.CwmsDbConnectionPool;
import usace.cwms.db.dao.util.services.CwmsDbServiceLookup;

public class CwmsConnectionPool implements ConnectionPoolMXBean
{
    private static Logger log = Logger.getLogger(CwmsConnectionPool.class.getName());

    private static HashMap<CwmsConnectionInfo,CwmsConnectionPool> pools = new HashMap<>();
    private HashSet<WrappedConnection> connectionsOut = new HashSet<>();
    private CwmsConnectionInfo info = null;	
	private int connectionsRequested = 0;
	private int connectionsFreed = 0;
	private int unknownConnReturned = 0;
    private static CwmsDbConnectionPool pool = CwmsDbConnectionPool.getInstance();


    public static CwmsConnectionPool getPoolFor(CwmsConnectionInfo info) throws BadConnectException
    {
        CwmsConnectionPool ret = pools.get(info);
        if (ret == null)
        {
            try(Connection conn = pool.getConnection(info.getLoginInfo());)
			{
                ret = new CwmsConnectionPool(info,conn);
                pools.put(info,ret);        
            }
            catch(SQLException ex)
            {
                throw new BadConnectException("Unable to initialize pool.",ex);
            }
            catch(DbIoException ex)
            {
                throw new BadConnectException("Unable to set office context.",ex);
            }
        }
        return ret;
    }


    private CwmsConnectionPool(CwmsConnectionInfo info, Connection conn) throws BadConnectException,SQLException,DbIoException
    {
        this.info = info;
        String officeId = info.getLoginInfo().getUserOfficeId();
        String username = info.getLoginInfo().getUser();
        info.setConnection(conn);
        info.setDbOfficeCode(officeId2code(conn, officeId));

        // MJM 2018-2/21 Force autoCommit on.
        try{ conn.setAutoCommit(true); }
        catch(SQLException ex)
        {
            log.warning("Cannot set SQL AutoCommit to true: " + ex);
        }

        String priv = getOfficePrivileges(conn, info.getLoginInfo().getUrl(), officeId);
        setCtxDbOfficeId(conn, officeId, info.getDbOfficeCode(), priv);


        try
		{
            String name = String.format("CwmsConnectionPool(%s/%s)",info.getLoginInfo().getUrl(),info.getLoginInfo().getUser());
			ManagementFactory.getPlatformMBeanServer()
							 .registerMBean(this, new ObjectName("org.opendcs:type=TimeSeriesDb,name=\""+name+"\""));
		}
		catch(JMException ex)
		{
			log.warning("Unable to register tracking bean " + ex.getLocalizedMessage());
		}

    }    

    @Override
	public int getConnectionsOut()
    {
		return connectionsOut.size();
	}

	@Override
	public int getConnectionsAvailable()
    {
		return 10 - connectionsOut.size();
	}

	@Override
	public String getThreadName()
    {
		return Thread.currentThread().getName();
	}

	@Override
	public int getGetConnCalled()
    {
		return this.connectionsRequested;
	}

	@Override
	public int getFreeConnCalled()
    {
		return this.connectionsFreed;
	}

	@Override
	public int getUnknownReturned()
    {
		return this.unknownConnReturned;
	}

	@Override
	public TabularData getConnectionsList() throws OpenDataException
    {
		TabularData td = new TabularDataSupport(JMXTypes.CONNECTION_LIST);
        for(WrappedConnection conn: this.connectionsOut)
        {
            td.put(conn.asCompositeData());
        }
        return td;
	}


    public Connection getConnection() throws SQLException
    {
        connectionsRequested++;
        Connection conn = pool.getConnection(info.getLoginInfo());
        WrappedConnection wc = new WrappedConnection(conn,(c)->CwmsDbConnectionPool.close(c),Optional.of(this));
        connectionsOut.add(wc);        
        return wc;
    }

    public void returnConnection(Connection conn) throws SQLException
    {
        if(connectionsOut.contains(conn))
        {
            connectionsFreed++;
            connectionsOut.remove(conn);
            conn.close();

        }
        else
        {
            unknownConnReturned++;
        }     
    }



    private String getOfficePrivileges(Connection conn, String user, String dbOfficeId) throws BadConnectException
	{
		String ret = null;
		try
		{
			ArrayList<StringPair>officePrivileges = determinePrivilegedOfficeIds(conn);
			// MJM 2018-12-05 now determine the highest privilege level that this user has in
			// the specified office ID:
			log.finest("Office Privileges for user '" + user + "'");
			for(StringPair op : officePrivileges)
			{
				if (op == null)
				{
					log.warning("Skipping null privilege string pair!");
					continue;
				}
				if (op.first == null)
				{
					log.warning("Skipping null op.first privilege string pair!");
					continue;
				}
				if (op.second == null)
				{
					log.warning("Skipping null op.first privilege string pair!");
					continue;
				}
				log.finest("Privilege: " + op.first + ":" + op.second);

				String priv = op.second.toLowerCase();
				if (TextUtil.strEqualIgnoreCase(op.first, dbOfficeId) && priv.startsWith("ccp"))
				{
					if (priv.contains("mgr"))
					{
						ret = op.second;
						break;
					}
					else if (priv.contains("proc"))
					{
						if (ret == null || !ret.toLowerCase().contains("mgr"))
							ret = op.second;
					}
					else if (ret == null)
						ret = op.second;
				}
			}
			return ret;
		}
		catch (Exception ex)
		{
            String msg = "Cannot determine privileged office IDs: ";
			log.log(Level.SEVERE,msg,ex);
			throw new BadConnectException(msg,ex);
		}

    }

	/**
	 * Fills in the internal list of privileged office IDs.
	 * @return array of string pairs: officeId,Privilege for that office
	 * @throws SQLException
	 */
	private ArrayList<StringPair> determinePrivilegedOfficeIds(Connection conn) throws SQLException
    {

        CwmsDbSec dbSec = CwmsDbServiceLookup.buildCwmsDb(CwmsDbSec.class, conn);
        ResultSet rs = dbSec.getAssignedPrivGroups(conn, null);

        ArrayList<StringPair> ret = new ArrayList<StringPair>();

        // 4/8/13 phone call with Pete Morris - call with Null. and the columns returned are:
        // username, user_db_office_id, db_office_id, user_group_type, user_group_owner, user_group_id,
        // is_member, user_group_desc
        while(rs != null && rs.next())
        {
            String username = rs.getString(1);
            String db_office_id = rs.getString(2);
            String user_group_id = rs.getString(5);

            log.fine("privilegedOfficeId: username='" + username + "' "
                + "db_office_id='" + db_office_id + "' "
                + "user_group_id='" + user_group_id + "' "
                );

            // We look for groups "CCP Proc", "CCP Mgr", and "CCP Reviewer".
            // Ignore anything else.
            String gid = user_group_id.trim();
            if (!TextUtil.startsWithIgnoreCase(gid, "CCP"))
                continue;

            // See if we have an existing privilege for this office ID.
            int existingIdx = 0;
            String existingPriv = null;
            for(; existingIdx < ret.size(); existingIdx++)
            {
                StringPair sp = ret.get(existingIdx);
                if (sp.first.equalsIgnoreCase(db_office_id))
                {
                    existingPriv = sp.second;
                    break;
                }
            }
            // If we do have an existing privilege, determine whether to keep this
            // one or the existing one (keep the one with more privilege).
            if (existingPriv != null)
            {
                if (existingPriv.toUpperCase().contains("MGR"))
                    continue; // We are already manager in this office. Discard this item.
                else if (gid.toUpperCase().contains("MGR"))
                {
                    // This item is MGR, replace existing one.
                    ret.get(existingIdx).second = gid;
                    continue;
                }
                else if (gid.toUpperCase().contains("PROC"))
                {
                    // This is for PROC, existing must be PROC or Reviewer. Replace.
                    ret.get(existingIdx).second = gid;
                    continue;
                }
            }
            else // this item is first privilege seen for this office.
                ret.add(new StringPair(db_office_id, gid));
        }
        return ret;
    }

    /**
	 * Converts a CWMS String Office ID to the numeric office Code.
	 * @param con the Connection
	 * @param officeId the String office ID
	 * @return the office code as a DbKey or Constants.undefinedId if no match.
	 */
	private DbKey officeId2code(Connection con, String officeId) throws SQLException
	{
		String q = "select cwms_util.get_office_code(?) from dual";
		try(PreparedStatement stmt = con.prepareStatement(q);)
		{
			stmt.setString(1, officeId);
			log.finest(q);
			try(ResultSet rs = stmt.executeQuery();)
			{
				if(rs.next())
				{
					return DbKey.createDbKey(rs, 1);
				}
				else
				{
					log.warning("Error getting office code for id " + officeId);
					return Constants.undefinedId;
				}
			}
		}
	}

    /**
	 * Reset the VPD context variable with user specified office Id
	 * @throws DbIoException
	 */
	public static void setCtxDbOfficeId(Connection conn, String dbOfficeId,
                                        DbKey dbOfficeCode, String dbOfficePrivilege)
        throws DbIoException
    {
        String errMsg = null;
        PreparedStatement storeProcStmt = null;
        CallableStatement testStmt = null;

        try
        {
            String q = null;
            int privLevel =
                dbOfficeId == null ? 0 :
                dbOfficePrivilege.toUpperCase().contains("MGR") ? 1 :
                dbOfficePrivilege.toUpperCase().contains("PROC") ? 2 : 3;
            q =
                "begin cwms_ccp_vpd.set_ccp_session_ctx(" +
                ":1 /* office code */, :2 /* priv level*/, :3 /* officeId */); end;";
            storeProcStmt  = conn.prepareCall(q);
            storeProcStmt.setInt(1, (int)dbOfficeCode.getValue());
            storeProcStmt.setInt(2, privLevel);
            storeProcStmt.setString(3, dbOfficeId);
    //			Logger.instance().debug2("Executing '" + q + "' with "
    //				+ "dbOfficeCode=" + dbOfficeCode
    //				+ ", privLevel=" + privLevel
    //				+ ", dbOfficeId='" + dbOfficeId + "'");
            storeProcStmt.execute();
    //			conn.commit();

            q = "{ ? = call cwms_ccp_vpd.get_pred_session_office_code_v(?, ?) }";
            testStmt = conn.prepareCall(q);
            testStmt.registerOutParameter(1, Types.VARCHAR);
            testStmt.setString(2, "CC");
            testStmt.setString(3, "PLATFORMCONFIG");
    //			Logger.instance().debug2("Calling '" + q + "' with "
    //				+ "schema=CCP and table=PLATFORMCONFIG");
            testStmt.execute();
        }
        catch (SQLException ex)
        {
            errMsg = "Error setting VPD context for '" + dbOfficeId + "': " + ex;
            log.severe(errMsg);
            System.err.println(errMsg);
            ex.printStackTrace(System.err);
            throw new DbIoException(errMsg);
        }
        finally
        {
            if (storeProcStmt != null)
            {
                try { storeProcStmt.close(); }
                catch(Exception ex) {}
            }
            if (testStmt != null)
            {
                try { testStmt.close(); }
                catch(Exception ex) {}
            }
        }
    }

}
