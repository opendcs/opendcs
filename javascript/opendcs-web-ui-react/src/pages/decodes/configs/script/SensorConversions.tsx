import type React from "react";
import DataTable, { type DataTableRef } from "datatables.net-react";
import DT from "datatables.net-bs5";
import {
  ApiConfigRef,
  ApiConfigSensor,
  type ApiConfigScript,
  type ApiConfigScriptSensor,
} from "opendcs-api";
import { useRef } from "react";
import { useUnits } from "../../../../contexts/data/UnitsContext";
// this isn't a hook, it just has "use" as the name.
// eslint-disable-next-line react-hooks/rules-of-hooks
DataTable.use(DT);

export interface SensorConversionProperties {
  scriptSensors: ApiConfigScriptSensor[];
  configSensors: { [k: number]: ApiConfigSensor };
}

const SensorConversion: React.FC<SensorConversionProperties> = ({
  scriptSensors,
  configSensors,
}) => {
  const table = useRef<DataTableRef>(null);
  const units = useUnits();

  const columns = [
    { data: "sensorNumber", defaultContent: "" },
    {
      data: null,
      render: (data: ApiConfigScriptSensor, type: string, _row: unknown) => {
        if (type === "display") {
          return configSensors[data.sensorNumber!].sensorName;
        } else {
          return data;
        }
      },
    },
    {
      data: null,
      render: (data: ApiConfigScriptSensor, type: string, _row: unknown) => {
        if (type === "display") {
          const unitAbbr: string | undefined = data.unitConverter?.ucId
            ? units.units[data.unitConverter.ucId]?.abbr
            : undefined;
          return unitAbbr || "not defined";
        } else {
          return data;
        }
      },
    },
    { data: "unitConverter.algorithm", defaultContent: "" },
    { data: "unitConverter.a", defaultContent: "" },
    { data: "unitConverter.b", defaultContent: "" },
    { data: "unitConverter.c", defaultContent: "" },
    { data: "unitConverter.d", defaultContent: "" },
    { data: "e", defaultContent: "" },
    { data: "f", defaultContent: "" },
  ];

  return (
    <DataTable
      ref={table}
      data={scriptSensors}
      className="table table-hover table-striped tablerow-cursor w-100 border"
      columns={columns}
      options={{
        search: false,
        searching: false,
        paging: false,
        info: false,
        ordering: {
          handler: false,
          indicators: false,
        },
      }}
    >
      <thead>
        <tr>
          <td>#</td>
          <td>Name</td>
          <td>Units</td>
          <td>Algorithm</td>
          <td>A</td>
          <td>B</td>
          <td>C</td>
          <td>D</td>
          <td>E</td>
          <td>F</td>
        </tr>
      </thead>
    </DataTable>
  );
};

export default SensorConversion;
