package opendcs.dao;

import ilex.util.TextUtil;
import ilex.util.Base64;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.LoggerFactory;

import decodes.sql.DbKey;
import decodes.tsdb.ConstraintException;
import decodes.tsdb.DbAlgoParm;
import decodes.tsdb.DbCompAlgorithm;
import decodes.tsdb.DbCompAlgorithmScript;
import decodes.tsdb.DbIoException;
import decodes.tsdb.NoSuchObjectException;
import decodes.tsdb.ScriptType;
import decodes.tsdb.TsdbDatabaseVersion;
import decodes.tsdb.compedit.AlgorithmInList;

import opendcs.dai.AlgorithmDAI;
import opendcs.util.functional.ThrowingSupplier;

/**
 * Data Access Object for writing/reading Algorithm objects to/from a SQL database
 * @author mmaloney Mike Maloney, Cove Software, LLC
 * @author M. Allan Neilson
 */
public class AlgorithmDAO extends DaoBase implements AlgorithmDAI
{
    public static final org.slf4j.Logger log = LoggerFactory.getLogger(AlgorithmDAO.class);

    // While it *is* possible for this to change during runtime. The cost of
    // a database query is not trivial so we will have a default here, attempt
    // to determine it once per application instance, and except the result for
    // the length of that run.
    public static Integer COMMENT_LENGTH = null;

    public AlgorithmDAO(DatabaseConnectionOwner tsdb)
    {
        super(tsdb, "AlgorithmDao");
        if (COMMENT_LENGTH == null)
        {
            COMMENT_LENGTH = 1000;
            try(DaoBase dao = new DaoBase(tsdb, module))
            {
                dao.doQuery("select cmmnt from cp_algorithm", rs ->
                {
                    ResultSetMetaData rsmd = rs.getMetaData();
                    COMMENT_LENGTH = rsmd.getPrecision(1);
                    log.info("Determined Algorithm comment length to be {}", COMMENT_LENGTH.intValue());
                });
            }
            catch (Exception ex)
            {
                log.atError()
                   .setCause(ex)
                   .log("Unable to query for column meta data.");
            }
        }
    }

    @Override
    public DbKey getAlgorithmId(String name) throws DbIoException, NoSuchObjectException
    {
        String q = "select ALGORITHM_ID from CP_ALGORITHM where ALGORITHM_NAME = ?";
        try
        {
            DbKey ret = getSingleResult(q, rs -> DbKey.createDbKey(rs, 1), name);
            if (ret == null)
            {
                throw new NoSuchObjectException("No algorithm for name '" + name+ "'");
            }
            return ret;
        }
        catch(SQLException ex)
        {
            String msg = "Error getting algorithm ID for name '" + name + "'";
            log.atError()
               .setCause(ex)
               .log(msg);
            throw new DbIoException(msg, ex);
        }
    }

    public DbCompAlgorithm getAlgorithmById(DbKey id) throws DbIoException, NoSuchObjectException
    {
        String q = "select * from CP_ALGORITHM where ALGORITHM_ID = ?";
        try
        {
            DbCompAlgorithm ret = getSingleResult(q, rs ->
            {
                //int id = rs.getInt(1);
                String nm = rs.getString(2);
                String cls = rs.getString(3);
                String cmmt = rs.getString(4);
                DbCompAlgorithm algo = new DbCompAlgorithm(id, nm, cls, cmmt);
                fillAlgorithmSubordinates(algo);
                return algo;
            },
            id);
            if (ret == null)
            {
                throw new NoSuchObjectException("No algorithm with ID=" + id);
            }
            return ret;
        }
        catch(SQLException ex)
        {
            String msg = "Error reading algorithm with ID=" + id;
            log.atWarn()
               .setCause(ex)
               .log(msg);
            throw new DbIoException(msg, ex);
        }
    }

    @Override
    public ArrayList<DbCompAlgorithm> listAlgorithms()
        throws DbIoException
    {
        ArrayList<DbCompAlgorithm> ret = new ArrayList<DbCompAlgorithm>();
        String q = "select * from CP_ALGORITHM";
        try(PropertiesSqlDao propertiesSqlDao = new PropertiesSqlDao(db))
        {   // TODO: This can be a giant JOIN do it all at once.
            doQuery(q, rs ->
            {
                DbKey id = DbKey.createDbKey(rs, 1);
                String nm = rs.getString(2);
                String cls = rs.getString(3);
                String cmmt = rs.getString(4);
                DbCompAlgorithm algo = new DbCompAlgorithm(id, nm, cls, cmmt);

                ret.add(algo);
            });

            q = "select a.* from CP_ALGO_TS_PARM a, CP_ALGORITHM b "
                + "where a.ALGORITHM_ID = b.ALGORITHM_ID";
            doQuery(q, rs ->
            {
                DbKey algoId = DbKey.createDbKey(rs, 1);
                String role = rs.getString(2);
                String type = rs.getString(3);
                for(DbCompAlgorithm algo : ret)
                {
                    if (algo.getId().equals(algoId))
                    {
                        algo.addParm(new DbAlgoParm(role, type));
                        break;
                    }
                }
            });
            propertiesSqlDao.readPropertiesIntoList("CP_ALGO_PROPERTY", ret, null);

            if (db.getTsdbVersion() >= TsdbDatabaseVersion.VERSION_13)
            {
                // Join with CP_ALGORITHM for CWMS to implicitly filter by db_office_code.
                q = "select a.ALGORITHM_ID, a.SCRIPT_TYPE, a.SCRIPT_DATA "
                  + "from CP_ALGO_SCRIPT a, CP_ALGORITHM b "
                  + "where a.ALGORITHM_ID = b.ALGORITHM_ID "
                  + "order by ALGORITHM_ID, SCRIPT_TYPE, BLOCK_NUM";

                final AtomicReference<DbCompAlgorithm> lastAlgo = new AtomicReference<>(null);
                doQuery(q, rs ->
                {
                    DbKey algoId = DbKey.createDbKey(rs, 1);
                    DbCompAlgorithm algo = null;
                    for(DbCompAlgorithm dca : ret)
                    {
                        if (dca.getId().equals(algoId))
                        {
                            algo = dca;
                            break;
                        }
                    }

                    if (algo == null)
                    {
                        // Shouldn't happen because of the join
                        return;
                    }
                    // We're not going in parallel so wet don't need to worry about lastAlgo getting changed here.
                    if (algo != lastAlgo.get())
                    {
                        algo.clearScripts();
                        lastAlgo.set(algo);
                    }
                    ScriptType scriptType = ScriptType.fromDbChar(rs.getString(2).charAt(0));
                    String scriptData = rs.getString(3);
                    if (scriptData != null)
                    {
                        scriptData = new String(Base64.decodeBase64(scriptData.getBytes()));
                    }
                    DbCompAlgorithmScript script = algo.getScript(scriptType);
                    if (script == null)
                    {
                        script = new DbCompAlgorithmScript(algo, scriptType);
                        algo.putScript(script);
                    }
                    script.addToText(scriptData);
                });
            }

            return ret;
        }
        catch(SQLException ex)
        {
            String msg = "Error reading algorithm list.";
            warning(msg);
            log.atWarn()
               .setCause(ex)
               .log(msg);
            throw new DbIoException(msg, ex);
        }
    }

    /**
     * Return an algorithm with its subordinate meta-data.
     * @param name the unique name of the algorithm
     * @return an algorithm with its subordinate meta-data.
     * @throws NoSuchObjectException if named algorithm doesn't exist.
     * @throws DbIoException on Database IO error.
     */
    public DbCompAlgorithm getAlgorithm(String name) throws DbIoException, NoSuchObjectException
    {
        return getAlgorithmById(getAlgorithmId(name));
    }

    private void fillAlgorithmSubordinates(DbCompAlgorithm algo) throws SQLException
    {
        String q = "select * from CP_ALGO_TS_PARM where ALGORITHM_ID = ?";
        doQuery(q, rs ->
        {
            String role = rs.getString(2);
            String type = rs.getString(3);
            algo.addParm(new DbAlgoParm(role, type));
        },
        algo.getId());

        try(PropertiesSqlDao propertiesSqlDao = new PropertiesSqlDao(db))
        {
            propertiesSqlDao.readProperties("CP_ALGO_PROPERTY", "ALGORITHM_ID",
                                            algo.getId(), algo.getProperties());
        }
        catch (DbIoException ex)
        {
            throw new SQLException("Failed to retrieve properties", ex);
        }

        if (db.getTsdbVersion() >= TsdbDatabaseVersion.VERSION_13)
        {
            algo.clearScripts();
            q = "select SCRIPT_TYPE, SCRIPT_DATA from CP_ALGO_SCRIPT "
              + "where ALGORITHM_ID = ?"
              + " order by SCRIPT_TYPE, BLOCK_NUM";
            doQuery(q, rs ->
            {
                ScriptType scriptType = ScriptType.fromDbChar(rs.getString(1).charAt(0));
                String b64 = rs.getString(2);
                String scriptData = new String(Base64.decodeBase64(b64.getBytes()));
                log.debug("DAO fill subord: b64='{}' scriptData='{}'", b64, scriptData);
                DbCompAlgorithmScript script = algo.getScript(scriptType);
                if (script == null)
                {
                    script = new DbCompAlgorithmScript(algo, scriptType);
                    algo.putScript(script);
                }
                script.addToText(scriptData);
            },
            algo.getId());
        }
    }

    @Override
    public void writeAlgorithm(DbCompAlgorithm algo) throws DbIoException
    {
        final boolean isNew = ((ThrowingSupplier<Boolean,DbIoException>) (() ->
        {
            // Could be import from XML to overwrite existing algorithm.
            if (!DbKey.isNull(algo.getId()))
            {
                return false;
            }
            try
            {
                DbKey id = getAlgorithmId(algo.getName());
                algo.setId(id);
                return false;
            }
            catch(NoSuchObjectException ex)
            {
                return true;
            }
        })).get();

        try
        {
            inTransaction(dao ->
            {
                String q;
                final ArrayList<Object> parameters = new ArrayList<>();
                final DbKey id = ((ThrowingSupplier<DbKey,Exception>) () -> {
                    if (isNew)
                    {
                        DbKey tmp = dao.getKey("CP_ALGORITHM");
                        algo.setId(tmp);
                    }
                    return algo.getId();
                }).get();
                if (DbKey.isNull(id))
                {
                    log.error("Algorithm DB Key is still null.");
                }
                String comment = algo.getComment();
                if (comment.length() > COMMENT_LENGTH)
                {
                    comment = comment.substring(0, COMMENT_LENGTH.intValue());
                }
                if (isNew)
                {
                    q = "INSERT INTO CP_ALGORITHM(algorithm_id, algorithm_name, exec_class, cmmnt) VALUES(?,?,?,?)";
                    parameters.add(id);
                    parameters.add(algo.getName());
                    parameters.add(algo.getExecClass());
                    parameters.add(comment);
                }
                else // update
                {
                    q = "UPDATE CP_ALGORITHM "
                    + "SET ALGORITHM_NAME = ?"
                    + ", EXEC_CLASS = ?"
                    + ", CMMNT = ?"
                    + " WHERE ALGORITHM_ID = ?";
                    parameters.add(algo.getName());
                    parameters.add(algo.getExecClass());
                    parameters.add(comment);
                    parameters.add(id);
                }

                dao.doModify(q, parameters.toArray(new Object[0]));
                if (!isNew)
                {
                    // Delete & re-add parameters
                    q = "DELETE FROM CP_ALGO_TS_PARM WHERE ALGORITHM_ID = ?";
                    doModify(q, id);
                }
                for(Iterator<DbAlgoParm> it = algo.getParms(); it.hasNext(); )
                {
                    DbAlgoParm dap = it.next();
                    q = "INSERT INTO CP_ALGO_TS_PARM VALUES (?,?,?)";
                    doModify(q, id, dap.getRoleName(), dap.getParmType());
                }

                try(PropertiesSqlDao propertiesSqlDao = new PropertiesSqlDao(db))
                {
                    propertiesSqlDao.inTransactionOf(dao);
                    propertiesSqlDao.writeProperties("CP_ALGO_PROPERTY", "ALGORITHM_ID",
                            id, algo.getProperties());
                }

                if (db.getTsdbVersion() >= TsdbDatabaseVersion.VERSION_13)
                {
                    q = "DELETE FROM CP_ALGO_SCRIPT WHERE ALGORITHM_ID = ?";
                    dao.doModify(q, id);
                    log.debug("AlgorithmDAO.writeAlgo algorithm has {} scripts.", algo.getScripts().size());
                    for(DbCompAlgorithmScript script : algo.getScripts())
                    {
                        String text = script.getText();
                        if (text == null || text.length() == 0)
                        {
                            continue;
                        }
                        // Have to convert to Base64 to preserve quotes, newlines, etc.
                        String b64 = new String(Base64.encodeBase64(text.getBytes()));
                        log.debug("AlgorithmDAO.writeAlgo script {} text '{}' b64={}", script.getScriptType(), text, b64);
                        int blockNum = 1;
                        while(b64 != null)
                        {
                            String block = b64.length() < 4000 ? b64 : b64.substring(0, 4000);
                            b64 = (block == b64) ? null : b64.substring(4000);
                            q = "INSERT INTO CP_ALGO_SCRIPT VALUES(?,?,?,?)";
                            dao.doModify(q, id, script.getScriptType().getDbChar(), blockNum++, block);
                        }
                    }
                }

                q = "UPDATE CP_COMPUTATION SET DATE_TIME_LOADED = ? WHERE ALGORITHM_ID = ?";
                dao.doModify(q, new Date(), id);
            });
        }
        catch (Exception ex)
        {
            throw new DbIoException("Unable to save algorithm.", ex);
        }
    }

    @Override
    public void deleteAlgorithm(DbKey id) throws DbIoException, ConstraintException
    {
        try
        {
            inTransaction(dao ->
            {
                String q = "select count(*) from CP_COMPUTATION "
                    + "where ALGORITHM_ID = ?";
                int count = getSingleResult(q, rs -> rs.getInt(1), id);

                if (count > 0)
                {
                        throw new ConstraintException(
                            "Cannot delete algorithm with ID=" + id
                            + " because " + count + " computations rely on it.");
                }

                q = "delete from CP_ALGO_TS_PARM where ALGORITHM_ID = ?";
                dao.doModify(q, id);

                try(PropertiesSqlDao propertiesSqlDao = new PropertiesSqlDao(db))
                {
                    propertiesSqlDao.inTransactionOf(dao);
                    propertiesSqlDao.deleteProperties("CP_ALGO_PROPERTY", "ALGORITHM_ID", id);
                }

                q = "delete from CP_ALGO_SCRIPT where ALGORITHM_ID = ?";
                dao.doModify(q, id);

                q = "delete from CP_ALGORITHM where ALGORITHM_ID = ?";
                doModify(q, id);
            });
        }
        catch(Exception ex)
        {
            String msg = "Error deleting algorithm with ID=" + id;
            log.atWarn()
               .setCause(ex)
               .log(msg);
            throw new DbIoException(msg, ex);
        }
    }

    @Override
    public void close()
    {
        super.close();
    }

    @Override
    public ArrayList<String> listAlgorithmNames() throws DbIoException
    {
        String q = "select ALGORITHM_NAME from CP_ALGORITHM";
        try
        {
            ArrayList<String> ret = new ArrayList<String>();
            ret.addAll(getResults(q, rs -> rs.getString(1)));
            return ret;
        }
        catch(SQLException ex)
        {
            String msg = "Error listing algorithms";
            log.atWarn()
               .setCause(ex)
               .log(msg);
            throw new DbIoException(msg, ex);
        }
    }

    @Override
    public ArrayList<AlgorithmInList> listAlgorithmsForGui() throws DbIoException
    {
        String q = "select algorithm_id, algorithm_name, "
            + "exec_class, cmmnt "
            + "from cp_algorithm";
        try
        {
            ArrayList<AlgorithmInList> ret = new ArrayList<AlgorithmInList>();
            doQuery(q, rs ->
            {
                ret.add(new AlgorithmInList(DbKey.createDbKey(rs, 1), rs.getString(2),
                    rs.getString(3), 0, TextUtil.getFirstLine(rs.getString(4))));
            });
            q = "select a.algorithm_id, count(1) as CompsUsingAlgo "
              + "from cp_algorithm a, cp_computation b "
              + "where a.algorithm_id = b.algorithm_id "
              + "group by a.algorithm_id";
            doQuery(q, rs ->
            {
                DbKey algoId = DbKey.createDbKey(rs, 1);
                int numCompsUsing = rs.getInt(2);
                for(AlgorithmInList ail : ret)
                {
                    if (ail.getAlgorithmId().equals(algoId))
                    {
                        ail.setNumCompsUsing(numCompsUsing);
                        break;
                    }
                }
            });

            return ret;
        }
        catch(SQLException ex)
        {
            String msg = "Error listing algorithms for GUI";
            log.atWarn()
               .setCause(ex)
               .log(msg);
            throw new DbIoException(msg, ex);
        }
    }
}
