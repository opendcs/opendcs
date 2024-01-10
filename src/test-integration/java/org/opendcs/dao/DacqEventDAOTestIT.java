package org.opendcs.dao;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Date;
import java.util.logging.Logger;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.assertj.core.api.ZonedDateTimeAssert;
import org.junit.jupiter.api.Test;
import org.opendcs.fixtures.AppTestBase;
import org.opendcs.fixtures.annotations.ComputationConfigurationRequired;
import org.opendcs.fixtures.annotations.ConfiguredField;
import org.opendcs.fixtures.annotations.TsdbAppRequired;

import decodes.polling.DacqEvent;
import decodes.sql.DbKey;
import decodes.tsdb.ComputationApp;
import decodes.tsdb.TimeSeriesDb;
import opendcs.dai.DacqEventDAI;
import opendcs.dai.LoadingAppDAI;

@ComputationConfigurationRequired("shared/loading-apps.xml")
public class DacqEventDAOTestIT extends AppTestBase
{
    private Logger log = Logger.getLogger(DacqEventDAI.class.getName());

    @ConfiguredField
    private TimeSeriesDb tsDb;

    @Test
    @TsdbAppRequired(app = ComputationApp.class, appName="compproc_regtest")
    public void test_event_logger() throws Exception
    {
        try(DacqEventDAI dacq = tsDb.makeDacqEventDAO();
            LoadingAppDAI loading = tsDb.makeLoadingAppDAO();)
        {
            ZonedDateTime zdtNow = ZonedDateTime.now(ZoneId.of("UTC"));
            DbKey appKey = loading.getComputationApp("compproc_regtest").getId();
            DacqEvent event = new DacqEvent();
            event.setAppId(appKey);
            event.setEventPriority(3);
            event.setEventText("Test message.");
            event.setSubsystem("testing");
            dacq.writeEvent(event);

            ArrayList<DacqEvent> events = new ArrayList<>();
            dacq.readEventsContaining("Test message", events);
            assertFalse(events.isEmpty(), "It does not appear the message was saved.");

            boolean found = false;
            for (DacqEvent e: events)
            {
                if (e.getEventText().equals("Test message.")
                    && e.getSubsystem().equals("testing"))
                {
                    found = true;
                }
            }
            assertTrue(found, "Message was not returned.");

            events = new ArrayList<>();
            Date eventTimePlus = Date.from(zdtNow.plusHours(1).toInstant());
            dacq.readEventsAfter(eventTimePlus, events);
            for(DacqEvent e: events)
            {
                log.info("*****" + e.toString());
            }
            assertTrue(events.size() == 0, "Events were found but shouldn't be.");
        }
    }
}
