/*
* Where Applicable, Copyright 2026 OpenDCS Consortium and/or its contributors
*
* Licensed under the Apache License, Version 2.0 (the "License"); you may not
* use this file except in compliance with the License. You may obtain a copy
* of the License at
*
*   http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
* License for the specific language governing permissions and limitations
* under the License.
*/
package org.opendcs.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.opendcs.fixtures.AppTestBase;
import org.opendcs.fixtures.annotations.ConfiguredField;
import org.opendcs.fixtures.annotations.DecodesConfigurationRequired;
import org.opendcs.fixtures.annotations.EnableIfTsDb;

import decodes.sql.DbKey;
import decodes.tsdb.CompAppInfo;
import decodes.tsdb.DbIoException;
import decodes.tsdb.TimeSeriesDb;
import decodes.tsdb.TimeSeriesIdentifier;
import decodes.tsdb.TsdbCompLock;
import opendcs.dai.ComputationQueueDAI;
import opendcs.dai.LoadingAppDAI;
import opendcs.dai.TimeSeriesDAI;
import opendcs.dao.DaoBase;

@DecodesConfigurationRequired({"shared/test-sites.xml"})
class CwmsComputationQueueDaoTestIT extends AppTestBase
{
    @ConfiguredField
    private TimeSeriesDb tsDb;

    @Test
    @EnableIfTsDb({"CWMS-Oracle"})
    void monitorsAndSafelyClearsCwmsQueues() throws Exception
    {
        String suffix = Long.toUnsignedString(System.nanoTime(), 36);
        CompAppInfo clearApp = newApp("cq-clear-" + suffix);
        CompAppInfo runningApp = newApp("cq-run-" + suffix);
        TimeSeriesIdentifier stage = tsDb.makeTsId(
            "TESTSITE1.Stage.Inst.15Minutes.0.QueueIT");
        TimeSeriesIdentifier flow = tsDb.makeTsId(
            "TESTSITE1.Flow.Inst.15Minutes.0.QueueIT");

        try (LoadingAppDAI appDao = tsDb.makeLoadingAppDAO();
             TimeSeriesDAI timeSeriesDao = tsDb.makeTimeSeriesDAO();
             DaoBase setupDao = new DaoBase(tsDb, "CwmsComputationQueueDaoTestIT"))
        {
            appDao.writeComputationApp(clearApp);
            appDao.writeComputationApp(runningApp);
            DbKey stageCode = timeSeriesDao.createTimeSeries(stage);
            DbKey flowCode = timeSeriesDao.createTimeSeries(flow);

            enqueue(setupDao, clearApp.getAppId(), stageCode);
            enqueue(setupDao, clearApp.getAppId(), stageCode);
            enqueue(setupDao, clearApp.getAppId(), flowCode);
            enqueue(setupDao, runningApp.getAppId(), flowCode);

            try (ComputationQueueDAI queueDao = tsDb.makeComputationQueueDAO())
            {
                Map<DbKey, Long> counts = queueDao.getQueueCounts();
                assertEquals(Long.valueOf(3L), counts.get(clearApp.getAppId()));
                assertEquals(Long.valueOf(1L), counts.get(runningApp.getAppId()));

                List<ComputationQueueDAI.QueueDetail> details =
                    queueDao.getQueueDetails(clearApp.getAppId());
                assertEquals(2, details.size());
                assertEquals(2L, details.get(0).getQueueCount());
                assertTrue(details.get(0).getTimeSeriesId()
                    .equalsIgnoreCase(stage.getUniqueString()));

                assertEquals(3, queueDao.clearQueueIfStopped(clearApp.getAppId()));
                assertNull(queueDao.getQueueCounts().get(clearApp.getAppId()));

                TsdbCompLock lock =
                    appDao.obtainCompProcLock(runningApp, 2224, "queue-dao-it");
                try
                {
                    DbIoException ex = assertThrows(DbIoException.class,
                        () -> queueDao.clearQueueIfStopped(runningApp.getAppId()));
                    assertTrue(ex.getMessage().contains("running or restarting"));
                    assertEquals(Long.valueOf(1L),
                        queueDao.getQueueCounts().get(runningApp.getAppId()));
                }
                finally
                {
                    appDao.releaseCompProcLock(lock);
                }

                assertEquals(1, queueDao.clearQueueIfStopped(runningApp.getAppId()));
                assertFalse(queueDao.getQueueCounts().containsKey(runningApp.getAppId()));
            }
        }
        finally
        {
            cleanup(clearApp);
            cleanup(runningApp);
            deleteTimeSeries(stage);
            deleteTimeSeries(flow);
        }
    }

    private CompAppInfo newApp(String name)
    {
        CompAppInfo app = new CompAppInfo();
        app.setAppName(name);
        return app;
    }

    private void enqueue(DaoBase dao, DbKey appId, DbKey timeSeriesCode)
        throws Exception
    {
        dao.doModify(
            "insert into cp_comp_tasklist "
                + "(record_num, loading_application_id, site_datatype_id, "
                + "date_time_loaded, start_date_time, flags) "
                + "values (cp_comp_tasklistidseq.nextval, ?, ?, sysdate, sysdate, 0)",
            appId, timeSeriesCode);
    }

    private void cleanup(CompAppInfo app) throws Exception
    {
        if (app == null || DbKey.isNull(app.getAppId()))
            return;

        try (DaoBase dao = new DaoBase(tsDb, "CwmsComputationQueueDaoTestIT"))
        {
            dao.doModify("delete from cp_comp_proc_lock where loading_application_id = ?",
                app.getAppId());
            dao.doModify("delete from cp_comp_tasklist where loading_application_id = ?",
                app.getAppId());
            dao.doModify("delete from ref_loading_application_prop "
                + "where loading_application_id = ?", app.getAppId());
            dao.doModify("delete from hdb_loading_application "
                + "where loading_application_id = ?", app.getAppId());
        }
    }

    private void deleteTimeSeries(TimeSeriesIdentifier timeSeries) throws Exception
    {
        try (TimeSeriesDAI timeSeriesDao = tsDb.makeTimeSeriesDAO())
        {
            timeSeriesDao.deleteTimeSeries(timeSeries);
        }
    }
}
