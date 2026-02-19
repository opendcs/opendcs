import type React from "react";
import type { ApiDecodesTimeSeries } from "opendcs-api";
import DataTable from "datatables.net-react";
import DT, { type ConfigColumns } from "datatables.net-bs5";
import { useCallback, useMemo } from "react";

// this isn't a hook, it just has "use" as the name.
// eslint-disable-next-line react-hooks/rules-of-hooks
DataTable.use(DT);

export interface DecodesDataGridProperties {
  output?: ApiDecodesTimeSeries[];
}

const DecodesDataGrid: React.FC<DecodesDataGridProperties> = ({ output }) => {
  const times = useMemo(() => {
    let times: Date[] = [];
    output?.forEach((ts) =>
      ts.values?.forEach((tsv) => {
        if (tsv.time && !times.some((et) => et.valueOf() === tsv.time?.valueOf())) {
          times.push(tsv.time);
        }
      }),
    );
    return times.sort().map((t) => {
      return { time: t };
    });
  }, [output]);

  const lookupData = useCallback(
    (sensorName: string, time: Date): string | undefined => {
      const ts = output?.find((ts) => ts.sensorName === sensorName);
      console.log(`TS found ${JSON.stringify(ts)}`);
      return ts?.values?.find((tsv) => tsv.time?.valueOf() === time.valueOf())?.value;
    },
    [output],
  );

  let dataColumns =
    output?.map((ts) => {
      return {
        data: "blash",
        defaultContent: "-",
        name: `${ts.sensorName}`,
        render: (
          _data: unknown,
          _type: unknown,
          row: { time: Date },
          _meta: unknown,
        ) => {
          console.log(`Looking for ${ts.sensorName} at time ${JSON.stringify(row)}`);
          return lookupData(ts.sensorName!, row.time);
        },
      };
    }) || [];

  const columns: ConfigColumns[] = [{ data: "time" }, ...dataColumns];

  return (
    <DataTable
      /* Using the column length as the key here allows us to reforce the building of the table so 
           the table actually rebuilds and thus renders everything.
        */
      key={columns.length}
      options={{
        paging: false,
        scrollY: "20em",
        scrollCollapse: true,
        info: false,
        search: false,
        searching: false,
        responsive: false,
        deferRender: true,
      }}
      data={times}
      columns={columns}
      className="table table-hover table-striped table-sm tablerow-cursor w-100 border"
    >
      <thead>
        <tr>
          <th key="time">Date/Time</th>
          {output?.map((ts) => {
            return <th key={ts.sensorName}>{ts.sensorName}</th>;
          })}
        </tr>
      </thead>
    </DataTable>
  );
};

export default DecodesDataGrid;
