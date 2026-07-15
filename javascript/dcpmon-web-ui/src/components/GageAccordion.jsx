import {
  Accordion,
  AccordionItem,
  AccordionTrigger,
  AccordionContent,
} from "@/components/ui/accordion";
import useDataQuery from "@/hooks/useDataQuery";
import dayjs from "dayjs";
import { useState } from "react";
import { Skeleton } from "./ui/skeleton";

export function GageAccordion({ location, totalHours = 24 }) {
  const [isOpen, setIsOpen] = useState(false);

  const goesDcp = useDataQuery({
    dataParams: { source: "goes", dcpAddress: location?.dcpAddress },
    queryParams: {
      // only fetch when accordion is opened
      enabled: isOpen && !!location?.dcpAddress,
    },
  });

  return (
    <Accordion
      type="single"
      collapsible
      onValueChange={(value) => setIsOpen(value === "item-1")}
    >
      <AccordionItem value="item-1">
        <AccordionTrigger>
          {location?.status === "complete" ? "✅" : "❗"} {location?.dcpAddress}{" "}
          - {location.messagesTotal} / {totalHours}
        </AccordionTrigger>
        <AccordionContent>
          <div className="space-y-2">
            <div>
              <strong>Station:</strong> {location?.stationId}
            </div>
            {goesDcp.isLoading && <Skeleton className="h-6 w-1/2" />}
            {goesDcp.error && (
              <div className="text-red-500">Error loading data</div>
            )}
            {goesDcp.data && (
              <div className="overflow-x-auto rounded border border-gray-200">
                <table className="min-w-full text-sm text-left">
                  <thead className="bg-gray-100 text-gray-700 font-semibold">
                    <tr>
                      <th className="px-4 py-2 whitespace-nowrap">
                        Receive Time
                      </th>
                      <th className="px-4 py-2 whitespace-nowrap">C-Type</th>
                      <th className="px-4 py-2 whitespace-nowrap">ARM</th>
                      <th className="px-4 py-2 whitespace-nowrap">EIRP</th>
                      <th className="px-4 py-2 whitespace-nowrap">Freq</th>
                      <th className="px-4 py-2 whitespace-nowrap">Quality</th>
                      <th className="px-4 py-2 whitespace-nowrap">Channel</th>
                      <th className="px-4 py-2 whitespace-nowrap">Data</th>
                    </tr>
                  </thead>
                  <tbody>
                    {goesDcp.data.data.messages.map((msg, idx) => (
                      <tr key={idx} className="border-t hover:bg-gray-50">
                        <td className="px-4 py-2">
                          {dayjs(msg.receiveTime).format("YYYY-MM-DD HH:mm")}
                        </td>
                        <td className="px-4 py-2">{msg.cType}</td>
                        <td className="px-4 py-2">{msg.arm}</td>
                        <td className="px-4 py-2">{msg.eirp}</td>
                        <td className="px-4 py-2">{msg.frequency}</td>
                        <td className="px-4 py-2">{msg.quality}</td>
                        <td className="px-4 py-2">{msg.channel}</td>
                        <td className="px-4 py-2 whitespace-pre-wrap">
                          <pre className=" max-w-sm overflow-auto">
                            {" "}
                            {msg.data}
                          </pre>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        </AccordionContent>
      </AccordionItem>
    </Accordion>
  );
}
