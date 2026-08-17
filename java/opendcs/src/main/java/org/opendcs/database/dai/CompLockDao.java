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
package org.opendcs.database.dai;

import java.util.List;
import java.util.Optional;

import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.OpenDcsDao;
import org.opendcs.database.api.OpenDcsDataException;
import org.opendcs.util.Result;

import decodes.sql.DbKey;
import decodes.tsdb.CompAppInfo;
import decodes.tsdb.LockBusyException;
import decodes.tsdb.TsdbCompLock;

public interface CompLockDao extends OpenDcsDao
{

    /**
     * Retrieve the current lock, if any, for the given app id.
     * @param tx
     * @param appId
     * @return
     * @throws OpenDcsDataException
     */
    Optional<TsdbCompLock> getLock(DataTransaction tx, DbKey appId) throws OpenDcsDataException;

    /**
     * Obtain a specific lock. If the lock already exists, and is not for the same pid and is not stale
     * LockBusyException will be returned instead of the lock.
     *
     * Otherwise the Lock is returned and now owned by that instance.
     *
     * @param tx
     * @param appInfo
     * @param pid
     * @param host
     * @return
     * @throws OpenDcsDataException
     */
    Result<TsdbCompLock,LockBusyException> obtainLock(DataTransaction tx, CompAppInfo appInfo, int pid, String host) throws OpenDcsDataException;

    /**
     * Release the given lock.
     * @param tx
     * @param lock
     * @throws OpenDcsDataException
     */
    void releaseLock(DataTransaction tx, TsdbCompLock lock) throws OpenDcsDataException;

    /**
     * Check if given Lock (App) is already set. If so, update the heartbeat time and status.
     *
     * @param tx
     * @param lock
     * @return The updated Lock, or the reason the attempt at the lock failed. Returned lock is always
     * updated from the database regardless of weather or not the heartbeat or status was updated.
     * @throws OpenDcsDataException
     */
    Result<TsdbCompLock,LockBusyException> checkLock(DataTransaction tx, TsdbCompLock lock) throws OpenDcsDataException;

    /**
     * Retrieve all current app locks.
     *
     * @param tx
     * @param limit
     * @param offset
     * @return
     * @throws OpenDcsDataException
     */
    List<TsdbCompLock> getAll(DataTransaction tx, int limit, int offset) throws OpenDcsDataException;
}
