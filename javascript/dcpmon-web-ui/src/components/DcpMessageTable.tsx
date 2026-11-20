import Table from "react-bootstrap/Table";
import type { DcpMessage } from "../types";

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
          {messages.map((message) => (
            <tr key={`${message.receiveTime}-${message.channel ?? "unknown"}`}>
              <td>{new Date(message.receiveTime).toLocaleString()}</td>
              <td>{message.cType ?? "-"}</td>
              <td>{message.arm ?? "-"}</td>
              <td>{message.eirp ?? "-"}</td>
              <td>{message.frequency ?? "-"}</td>
              <td>{message.quality ?? "-"}</td>
              <td>{message.channel ?? "-"}</td>
              <td>
                <pre className="dcpmon-message-data">{message.data}</pre>
              </td>
            </tr>
          ))}
        </tbody>
      </Table>
    </div>
  );
}
