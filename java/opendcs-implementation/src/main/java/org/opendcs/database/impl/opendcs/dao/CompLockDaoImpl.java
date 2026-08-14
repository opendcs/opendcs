/*
 *  Copyright 2026 OpenDCS Consortium and its Contributors
 *
 *  Licensed under the Apache License, Version 2.0 (the "License")
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.opendcs.database.impl.opendcs.dao;

import static org.opendcs.utils.sql.SqlQueries.LIMIT_CLAUSE;
import static org.opendcs.utils.sql.SqlQueries.addLimitOffset;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.jdbi.v3.core.Handle;
import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.OpenDcsDataException;
import org.opendcs.database.dai.CompLockDao;
import org.opendcs.database.impl.opendcs.jdbi.mapper.apps.CompLockMapper;
import org.opendcs.utils.FailableResult;
import org.opendcs.utils.sql.GenericColumns;
import org.opendcs.utils.sql.SqlErrorMessages;
import org.stringtemplate.v4.ST;

import decodes.sql.DbKey;
import decodes.tsdb.CompAppInfo;
import decodes.tsdb.LockBusyException;
import decodes.tsdb.TsdbCompLock;

public final class CompLockDaoImpl implements CompLockDao
{
    private static final ST SELECT = new ST("""
        select loading_application_id, pid, hostname, heartbeat, cur_status
          from cp_comp_proc_lock
          <if(where)> <where> <endif>
        order by loading_application_id asc
        <if(limit)> <limit> <endif>
    """);

    private final CompLockMapper lockMapper = CompLockMapper.withPrefix(null);

    @Override
    public void releaseLock(DataTransaction tx, TsdbCompLock lock) throws OpenDcsDataException
    {
        var handle = tx.connection(Handle.class)
                       .orElseThrow(() -> new OpenDcsDataException(SqlErrorMessages.NO_JDBI_HANDLE));
        try (var delete = handle.createUpdate("delete from cp_comp_proc_lock where loading_application_id = :id"))
        {
            delete.bind(GenericColumns.ID.column(), lock.getAppId()).execute();
        }
    }

    @Override
    public Optional<LockBusyException> checkLock(DataTransaction tx, TsdbCompLock lock) throws OpenDcsDataException
    {
        var tlock = getLock(tx, lock.getAppId()).orElse(null);
        if (tlock != null)
        {
            if (lock.getPID() != tlock.getPID()
                || !lock.getHost().equalsIgnoreCase(tlock.getHost()))
            {
                return Optional.of(new LockBusyException(
                    "Lock for app ID " + lock.getAppId()
                    + " has been stolen by PID " + tlock.getPID()
                    + " on host '" + tlock.getHost() + "'"
                    + ", my PID=" + lock.getPID()
                    + ", my host='" + lock.getHost() + "'"));
            }
            lock.setHeartbeat(new Date());
            saveLock(tx, lock);
        }
        else
        {
            return Optional.of(new LockBusyException("Lock for app ID " + lock.getAppId() + " has been deleted."));
        }
        return Optional.empty();
    }

    @Override
    public List<TsdbCompLock> getAll(DataTransaction tx, int limit, int offset) throws OpenDcsDataException
    {
        var handle = tx.connection(Handle.class)
                       .orElseThrow(() -> new OpenDcsDataException(SqlErrorMessages.NO_JDBI_HANDLE));
        try (var select = handle.createQuery(SELECT.add(LIMIT_CLAUSE, addLimitOffset(limit, offset)).render()))
        {
            if (limit >= 0)
            {
                select.bind(LIMIT_CLAUSE, limit);
            }
            if (offset >= 0)
            {
                select.bind("offset", offset);
            }
            return select.map(lockMapper).list();
        }
    }

    @Override
    public Optional<TsdbCompLock> getLock(DataTransaction tx, DbKey appId) throws OpenDcsDataException
    {
        var handle = tx.connection(Handle.class)
                       .orElseThrow(() -> new OpenDcsDataException(SqlErrorMessages.NO_JDBI_HANDLE));
        try (var select = handle.createQuery(SELECT.add("where", " loading_application_id = :id").render()))
        {
            return select.bind(GenericColumns.ID.column(), appId)
                         .map(lockMapper)
                         .findOne();
        }
    }

    @Override
    public FailableResult<TsdbCompLock, LockBusyException> obtainLock(DataTransaction tx, CompAppInfo appInfo, int pid,
            String host) throws OpenDcsDataException
    {
        var lock = getLock(tx, appInfo.getAppId()).orElse(null);
        if (lock != null)
        {
            if (lock.getPID() == pid)
            {
                var check = checkLock(tx, lock);
                if (check.isEmpty())
                {
                    return FailableResult.success(lock);
                }
                else
                {
                    return FailableResult.failure(check.get());
                }
            }
            else if (!lock.isStale())
            {
                String msg =
                        "Cannot obtain lock for app ID " + appInfo.getAppId()
                        + ". Currently owned by PID " + lock.getPID()
                        + " on host '" + lock.getHost() + "'";
                return FailableResult.failure( new LockBusyException(msg));
            }

            releaseLock(tx, lock);
        }


        
        return FailableResult.success(saveLock(tx,new TsdbCompLock(appInfo.getAppId(), pid, host, new Date(), "Starting")));        
    }
    

    private TsdbCompLock saveLock(DataTransaction tx, TsdbCompLock lock) throws OpenDcsDataException
    {
        var handle = tx.connection(Handle.class)
                       .orElseThrow(() -> new OpenDcsDataException(SqlErrorMessages.NO_JDBI_HANDLE));
        try (var merge = handle.createUpdate("""
                merge into cp_comp_proc_lock lock
                using (:loading_application_id loading_application_id, :pid pid, :hostname hostname,
                       :heartbeat heartbeat, :cur_status cur_status <dual>) input
                on (lock.loading_application_id = input.loading_application_id)
                when matched then
                    update set pid = input.pid, hostname = input.hostname, heartbeat = input.heartbeat
                            cur_status = input.cur_status
                when not matched then
                insert(loading_application_id, pid, hostname, heartbeat, cur_status)
                values(input.loading_application_id, input.pid, input.hostname, input.heartbeat, input.cur_status)
                """))
        {
            merge.bind(CompLockMapper.Columns.APP_ID.column(), lock.getAppId())
                  .bind(CompLockMapper.Columns.PID.column(), lock.getPID())
                  .bind(CompLockMapper.Columns.HOSTNAME.column(), lock.getHost())
                  .bindByType(CompLockMapper.Columns.HEARTBEAT.column(), lock.getHeartbeat(), Date.class)
                  .bind(CompLockMapper.Columns.STATUS.column(), lock.getStatus())
                  .execute();
            return getLock(tx, lock.getAppId())
                    .orElseThrow(() -> new OpenDcsDataException("Unable to retrieve lock we just saved."));
        }
    }
}
