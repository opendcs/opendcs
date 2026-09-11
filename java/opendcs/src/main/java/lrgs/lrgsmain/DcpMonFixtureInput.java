package lrgs.lrgsmain;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.opendcs.lrgs.dao.MsgArchive;
import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import decodes.util.Pdt;
import decodes.util.PdtEntry;
import lrgs.archive.XmlMsgArchive;
import lrgs.common.DcpAddress;
import lrgs.common.DcpMsg;
import lrgs.common.DcpMsgFlag;

/**
 * Deterministic, opt-in GOES input used by the local Docker Compose demo.
 * It is inert unless explicitly configured as a loadable LRGS input.
 */
public final class DcpMonFixtureInput implements LoadableLrgsInputInterface
{
    private static final Logger log = OpenDcsLoggerFactory.getLogger();
    private static final String COMPLETE = "CE1F40D4";
    private static final String PARTIAL = "CE1F2532";
    private static final String PARITY = "CE000001";
    private static final String MISSING = "CE000002";
    private static final String UNKNOWN = "CE000003";

    private MsgArchive archive;
    private String interfaceName = "dcpmon-fixtures";
    private int slot;
    private boolean enabled;
    private boolean loaded;

    @Override
    public void initLrgsInput()
    {
        addHourlySchedule(COMPLETE);
        addHourlySchedule(PARTIAL);
        addHourlySchedule(PARITY);
        addHourlySchedule(MISSING);
    }

    @Override
    public synchronized void enableLrgsInput(boolean enable)
    {
        enabled = enable;
        if (!enable || loaded)
            return;
        seed(COMPLETE, 24, false, false, true);
        seed(PARTIAL, 12, false, false, true);
        seed(PARITY, 24, true, true, false);
        seed(UNKNOWN, 3, false, false, true);
        if (archive instanceof XmlMsgArchive xmlArchive)
            xmlArchive.checkpoint();
        loaded = true;
        log.info("Loaded deterministic DCPMon fixture messages");
    }

    private void seed(
        String address, int count, boolean parity, boolean lowBattery, boolean gpsSynced)
    {
        long now = System.currentTimeMillis();
        long latestTransmit = now / (60L * 60L * 1000L) * (60L * 60L * 1000L) + 5_000L;
        if (latestTransmit > now)
            latestTransmit -= 60L * 60L * 1000L;
        for (int index = 0; index < count; index++)
        {
            Date transmitTime = new Date(
                latestTransmit - (long)(count - index - 1) * 60L * 60L * 1000L);
            char arm = parity && index == count - 1 ? '?' : 'G';
            String payload = (gpsSynced ? "\"" : "") + (lowBattery && index == count - 1
                ? " STAGE 12.34 V LOW BATTERY " : " STAGE 12.34 ");
            byte[] bytes = goesMessage(address, transmitTime, arm, payload);
            DcpMsg message = new DcpMsg(
                DcpMsgFlag.MSG_PRESENT | DcpMsgFlag.SRC_DOMSAT
                    | DcpMsgFlag.MSG_TYPE_GOES_ST | DcpMsgFlag.MSG_NO_SEQNUM,
                bytes, bytes.length, 0);
            message.setDcpAddress(new DcpAddress(address));
            message.setXmitTime(transmitTime);
            message.setDomsatTime(transmitTime);
            archive.archiveMsg(message, this);
        }
    }

    private static byte[] goesMessage(String address, Date time, char arm, String payload)
    {
        SimpleDateFormat format = new SimpleDateFormat("yyDDDHHmmss");
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        String header = address + format.format(time) + arm + "45+0NN162W00"
            + String.format("%05d", payload.length());
        return (header + payload).getBytes(StandardCharsets.US_ASCII);
    }

    private static void addHourlySchedule(String address)
    {
        PdtEntry entry = new PdtEntry();
        entry.dcpAddress = new DcpAddress(address);
        entry.st_channel = 162;
        entry.st_first_xmit_sod = 0;
        entry.st_xmit_interval = 3600;
        entry.st_xmit_window = 60;
        Pdt.instance().put(entry);
    }

    @Override public int getType() { return DL_DOMSAT; }
    @Override public void setSlot(int slot) { this.slot = slot; }
    @Override public int getSlot() { return slot; }
    @Override public String getInputName() { return "GOES:" + interfaceName; }
    @Override public void shutdownLrgsInput() { enabled = false; }
    @Override public boolean hasBER() { return false; }
    @Override public String getBER() { return ""; }
    @Override public boolean hasSequenceNums() { return false; }
    @Override public int getStatusCode() { return enabled ? DL_ACTIVE : DL_DISABLED; }
    @Override public String getStatus() { return enabled ? "Active" : "Disabled"; }
    @Override public int getDataSourceId() { return slot; }
    @Override public boolean getsAPRMessages() { return false; }
    @Override public String getGroup() { return "DCPMon fixtures"; }
    @Override public void setInterfaceName(String name) { interfaceName = name; }
    @Override public void setConfigParam(String name, String value) { }
    @Override public void setMsgArchive(MsgArchive archive) { this.archive = archive; }
}
