import Table from "react-bootstrap/Table";
import type { DcpMessage } from "opendcs-dds-api";

type DcpMessageTableProps = {
  messages: DcpMessage[];
};

export function DcpMessageTable({ messages }: DcpMessageTableProps) {
  return (
    <div className="table-responsive">
      <Table striped hover size="sm" className="align-middle mb-0">
        <thead>
          <tr>
            <th>Receive Time</th>
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
          {messages.map((message, index) => {
            if (message.messageType !== "GOES") return null;
            const receiveTime: unknown = message.receiveTime;
            const receiveTimeText =
              receiveTime instanceof Date
                ? receiveTime.toLocaleString()
                : new Date(String(receiveTime ?? "")).toLocaleString();

            return (
              <tr
                key={`${String(receiveTime)}-${message.channel ?? index}`}
              >
                <td>{receiveTimeText}</td>
                <td>{message.cType ?? "-"}</td>
                <td>{message.arm ?? "-"}</td>
                <td>{message.eirp ?? "-"}</td>
                <td>{message.frequency ?? "-"}</td>
                <td>{message.quality ?? "-"}</td>
                <td>{message.channel ?? "-"}</td>
                <td>
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
