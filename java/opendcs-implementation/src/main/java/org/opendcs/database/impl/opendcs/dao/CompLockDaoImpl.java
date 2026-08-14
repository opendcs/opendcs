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
    private final CompLockMapper lockMapper = CompLockMapper.withPrefix(null);

    private static final ST SELECT = new ST("""
        select loading_application_id, pid, hostname, heartbeat, cur_status
          from cp_comp_proc_lock
          <if(where)> <where> <endif>
        order by loading_application_id asc
        <if(limit)> <limit> <endif>
    """);

    @Override
    public void releaseLock(DataTransaction tx, TsdbCompLock lock) throws OpenDcsDataException
    {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'releaseLock'");
    }

    @Override
    public Optional<LockBusyException> checkLock(DataTransaction tx, TsdbCompLock lock) throws OpenDcsDataException
    {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'checkLock'");
    }

    @Override
    public List<TsdbCompLock> getAll(DataTransaction tx, int limit, int offset) throws OpenDcsDataException
    {
    
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getAll'");
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
        var existing = getLock(tx, appInfo.getAppId());
        if (existing.isPresent())
        {
            var lock = existing.get();
            if (lock.getPID() == pid)
            {
                return 
            }
        }
    }
    
}
