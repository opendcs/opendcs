import Table from "react-bootstrap/Table";
import type { DcpMessage, DcpTransmission } from "opendcs-dds-api";
import type { DisplaySettings } from "../displaySettings";
import { TimestampDisplay } from "./TimestampDisplay";

type DcpMessageTableProps = {
  messages: DcpMessage[];
  transmissionSlots?: DcpTransmission[];
  displaySettings: DisplaySettings;
};

export function DcpMessageTable({
  messages,
  transmissionSlots = [],
  displaySettings,
}: DcpMessageTableProps) {
  const slottedMessages = new Set(
    transmissionSlots.flatMap((slot) =>
      slot.message?.messageType === "GOES"
        ? [`${String(slot.message.transmitTime)}-${slot.message.channel}`]
        : [],
    ),
  );
  const rows: Array<{ expectedTime?: Date; message?: DcpMessage }> = [
    ...transmissionSlots.map((slot) => ({
      expectedTime: slot.expectedTime,
      message: slot.message,
    })),
    ...messages
      .filter(
        (message) =>
          message.messageType !== "GOES" ||
          !slottedMessages.has(
            `${String(message.transmitTime)}-${message.channel}`,
          ),
      )
      .map((message) => ({ message })),
  ];

  return (
    <div className="table-responsive dcpmon-message-table">
      <Table striped hover size="sm" className="align-middle mb-0">
        <thead>
          <tr>
            <th>
              Transmit Time ({displaySettings.timeZone.toUpperCase() === "UTC"
                ? "GMT"
                : displaySettings.timeZone})
            </th>
            <th>C-Type</th>
            <th>ARM</th>
            <th>EIRP</th>
            <th>Freq</th>
            <th>Quality</th>
            <th>Channel</th>
            <th>Data</th>
          </tr>
        </thead>
        <tbody>
          {rows.map(({ expectedTime, message }, index) => {
            if (!message) {
              return (
                <tr
                  className="dcpmon-missing-transmission"
                  key={`missing-${String(expectedTime)}-${index}`}
                >
                  <td data-label="Transmit time">
                    <TimestampDisplay
                      value={expectedTime}
                      settings={displaySettings}
                    />
                  </td>
                  <td className="dcpmon-missing-label" colSpan={7}>
                    Missing transmission
                  </td>
                </tr>
              );
            }
            if (message.messageType !== "GOES") return null;
            const transmitTime = message.transmitTime;

            return (
              <tr
                key={`${String(transmitTime)}-${message.channel ?? index}`}
              >
                <td data-label="Transmit time">
                  <TimestampDisplay
                    value={transmitTime}
                    settings={displaySettings}
                  />
                </td>
                <td data-label="C-Type">{message.cType ?? "-"}</td>
                <td data-label="ARM">{message.arm ?? "-"}</td>
                <td data-label="EIRP">{message.eirp ?? "-"}</td>
                <td data-label="Freq">{message.frequency ?? "-"}</td>
                <td data-label="Quality">{message.quality ?? "-"}</td>
                <td data-label="Channel">{message.channel ?? "-"}</td>
                <td data-label="Data">
                  <pre className="dcpmon-message-data">
                    {message.data ?? ""}
                  </pre>
                </td>
              </tr>
            );
          })}
        </tbody>
      </Table>
    </div>
  );
}
