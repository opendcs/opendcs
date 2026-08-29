import Table from "react-bootstrap/Table";
import type { DcpMessage, GoesMessage } from "opendcs-dds-api";

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
            // The query is restricted to GOES messages. The generated union
            // cannot narrow on the standard's nested discriminator yet.
            const goesMessage = message as GoesMessage;
            const receiveTime: unknown = goesMessage.receiveTime;
            const receiveTimeText =
              receiveTime instanceof Date
                ? receiveTime.toLocaleString()
                : new Date(String(receiveTime ?? "")).toLocaleString();

            return (
              <tr
                key={`${String(receiveTime)}-${goesMessage.channel ?? index}`}
              >
                <td>{receiveTimeText}</td>
                <td>{goesMessage.cType ?? "-"}</td>
                <td>{goesMessage.arm ?? "-"}</td>
                <td>{goesMessage.eirp ?? "-"}</td>
                <td>{goesMessage.frequency ?? "-"}</td>
                <td>{goesMessage.quality ?? "-"}</td>
                <td>{goesMessage.channel ?? "-"}</td>
                <td>
                  <pre className="dcpmon-message-data">
                    {goesMessage.data ?? ""}
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
