package org.opendcs.dao;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;

import org.junit.jupiter.api.Test;
import org.opendcs.fixtures.AppTestBase;
import org.opendcs.fixtures.annotations.ConfiguredField;
import org.opendcs.fixtures.annotations.EnableIfDaoSupported;

import decodes.dcpmon.XmitMediumType;
import decodes.tsdb.TimeSeriesDb;
import lrgs.common.DcpAddress;
import lrgs.common.DcpMsg;
import opendcs.dai.XmitRecordDAI;
import opendcs.dao.XmitRecordDAO;

public class XmitDaoTestIT extends AppTestBase
{
    @ConfiguredField
    private TimeSeriesDb db;

    @Test
    @EnableIfDaoSupported({XmitRecordDAO.class})
    public void test_xmit_records() throws Exception
    {
        final DcpMsg msg = new DcpMsg();
        final String msgAddr = "TESTADDR";
        final Date msgDate = new Date(ZonedDateTime.of(2023, 12, 21, 14, 20, 0, 0, ZoneId.of("UTC"))
                                                   .toEpochSecond());

        msg.setData(new String(msgAddr+"a test message").getBytes());
        msg.setBattVolt(5.0);
        msg.setDcpAddress(new DcpAddress(msgAddr));

        msg.setXmitTime(msgDate);
        try (XmitRecordDAI xmit = db.makeXmitRecordDao(5);)
        {
            Date t = xmit.getLastLocalRecvTime();
            assertNull(t, "Initial last recieve time should be null.");
            xmit.saveDcpTranmission(msg);
            final DcpMsg msgOut = xmit.findDcpTranmission(XmitMediumType.GOES, msgAddr, msgDate);
            assertNotNull(msgOut, "messaged did not round trip.");
            assertArrayEquals(msg.getData(),msgOut.getData(),"Returned message is not the same.");
        }
    } 
}
